import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'

// 捕捉元件註冊的 callback（畫布變更、離頁守衛）
const h = vi.hoisted(() => {
  const captured: { nodesChange?: (changes?: unknown) => void; leave?: () => unknown } = {}
  return {
    captured,
    toObjectMock: vi.fn(() => ({ nodes: [] as unknown[], edges: [] as unknown[] })),
    routeParams: { id: undefined } as { id?: string },
    confirmMock: vi.fn(() => Promise.resolve('confirm')),
    successMock: vi.fn(),
    errorMock: vi.fn(),
    warningMock: vi.fn(),
    onBeforeRouteLeaveMock: vi.fn((cb: () => unknown) => {
      captured.leave = cb
    }),
    onNodesChangeMock: vi.fn((cb: (changes?: unknown) => void) => {
      captured.nodesChange = cb
    }),
  }
})

vi.mock('../../api/workflow', () => ({
  get: vi.fn(),
  save: vi.fn(),
  getNodeRequiredFields: vi.fn().mockResolvedValue({}),
}))

vi.mock('@vue-flow/core', () => ({
  VueFlow: { name: 'VueFlow', template: '<div />' },
  Handle: { name: 'Handle', template: '<div />' },
  Position: { Left: 'left', Right: 'right' },
  useVueFlow: () => ({
    onConnect: vi.fn(),
    onNodeClick: vi.fn(),
    onPaneClick: vi.fn(),
    onNodesChange: h.onNodesChangeMock,
    onEdgesChange: vi.fn(),
    onPaneReady: vi.fn(),
    addEdges: vi.fn(),
    addNodes: vi.fn(),
    removeNodes: vi.fn(),
    screenToFlowCoordinate: vi.fn(() => ({ x: 0, y: 0 })),
    setNodes: vi.fn(),
    setEdges: vi.fn(),
    setViewport: vi.fn(),
    toObject: h.toObjectMock,
    fitView: vi.fn(),
    nodes: { value: [] },
    edges: { value: [] },
  }),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: h.routeParams }),
  useRouter: () => ({ push: vi.fn() }),
  onBeforeRouteLeave: h.onBeforeRouteLeaveMock,
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: { success: h.successMock, error: h.errorMock, warning: h.warningMock },
    ElMessageBox: { confirm: h.confirmMock },
  }
})

import * as workflowApi from '../../api/workflow'
import WorkflowEditorView from '../WorkflowEditorView.vue'
import { useWorkflowStore } from '../../stores/workflow'
import ElementPlus from 'element-plus'

function mountEditor() {
  return mount(WorkflowEditorView, { global: { plugins: [createPinia(), ElementPlus] } })
}

describe('WorkflowEditorView — dirty 追蹤與離頁攔截', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    h.routeParams.id = undefined
    h.captured.nodesChange = undefined
    h.captured.leave = undefined
    h.toObjectMock.mockReturnValue({ nodes: [], edges: [] })
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('節點 position/add 變更應使 store.dirty 變 true', async () => {
    mountEditor()
    await flushPromises()
    const store = useWorkflowStore()
    expect(store.dirty).toBe(false)

    h.captured.nodesChange?.([{ type: 'position' }])
    expect(store.dirty).toBe(true)
  })

  it('僅 dimensions/select 變更不應標記 dirty（尺寸量測、選取非使用者編輯）', async () => {
    mountEditor()
    await flushPromises()
    const store = useWorkflowStore()
    expect(store.dirty).toBe(false)

    h.captured.nodesChange?.([{ type: 'dimensions' }])
    expect(store.dirty).toBe(false)

    h.captured.nodesChange?.([{ type: 'select' }])
    expect(store.dirty).toBe(false)
  })

  it('dirty 為 true 時離頁守衛應觸發確認框', async () => {
    mountEditor()
    await flushPromises()
    const store = useWorkflowStore()
    store.setDirty(true)

    expect(h.captured.leave).toBeTypeOf('function')
    await h.captured.leave?.()
    expect(h.confirmMock).toHaveBeenCalled()
  })

  it('存檔前 validateGraph 有錯應擋下、不呼叫 store.save 並顯示錯誤', async () => {
    // toObject 回傳含環的畫布（a→b→a）
    h.toObjectMock.mockReturnValue({
      nodes: [
        { id: 'a', type: 'workflow', position: { x: 0, y: 0 }, data: { name: 'A', type: 'TOOL', config: {} } },
        { id: 'b', type: 'workflow', position: { x: 0, y: 0 }, data: { name: 'B', type: 'TOOL', config: {} } },
      ],
      edges: [
        { source: 'a', target: 'b' },
        { source: 'b', target: 'a' },
      ],
    })

    const wrapper = mountEditor()
    await flushPromises()

    await wrapper.find('[data-test="save-button"]').trigger('click')
    await flushPromises()

    expect(workflowApi.save).not.toHaveBeenCalled()
    expect(h.errorMock).toHaveBeenCalled()
  })

  it('ACTIVE 流程缺少必填欄位應擋存檔（升級為 error）', async () => {
    vi.mocked(workflowApi.getNodeRequiredFields).mockResolvedValue({ LLM_ASSISTANT: ['llmId'] })
    h.toObjectMock.mockReturnValue({
      nodes: [
        {
          id: 'a',
          type: 'workflow',
          position: { x: 0, y: 0 },
          data: { name: 'A', type: 'LLM_ASSISTANT', config: {} },
        },
      ],
      edges: [],
    })

    const wrapper = mountEditor()
    await flushPromises()
    const store = useWorkflowStore()
    store.current!.status = 'ACTIVE'

    await wrapper.find('[data-test="save-button"]').trigger('click')
    await flushPromises()

    expect(workflowApi.save).not.toHaveBeenCalled()
    expect(h.errorMock).toHaveBeenCalled()
    // ACTIVE 擋存訊息為聚合格式（節點 key 清單 + 引導），非單筆原始 message
    expect(h.errorMock.mock.calls.at(-1)?.[0]).toContain('可先停用')
  })

  it('ACTIVE 流程多節點缺必填時應聚合訊息並附引導提示，而非只顯示第一筆', async () => {
    vi.mocked(workflowApi.getNodeRequiredFields).mockResolvedValue({ LLM_ASSISTANT: ['llmId'] })
    h.toObjectMock.mockReturnValue({
      nodes: [
        {
          id: 'a',
          type: 'workflow',
          position: { x: 0, y: 0 },
          data: { name: 'A', type: 'LLM_ASSISTANT', config: {} },
        },
        {
          id: 'b',
          type: 'workflow',
          position: { x: 0, y: 0 },
          data: { name: 'B', type: 'LLM_ASSISTANT', config: {} },
        },
      ],
      edges: [],
    })

    const wrapper = mountEditor()
    await flushPromises()
    const store = useWorkflowStore()
    store.current!.status = 'ACTIVE'

    await wrapper.find('[data-test="save-button"]').trigger('click')
    await flushPromises()

    expect(workflowApi.save).not.toHaveBeenCalled()
    const message = h.errorMock.mock.calls.at(-1)?.[0] as string
    expect(message).toContain('2 個節點缺必填')
    expect(message).toContain('a')
    expect(message).toContain('b')
    expect(message).toContain('可先停用')
  })

  it('畫布快照於 await getNodeRequiredFields 之後才擷取，等待期間的編輯不遺失', async () => {
    const oldCanvas = {
      nodes: [{ id: 'old', type: 'workflow', position: { x: 0, y: 0 }, data: { name: 'O', type: 'TOOL', config: {} } }],
      edges: [],
    }
    const newCanvas = {
      nodes: [{ id: 'new', type: 'workflow', position: { x: 0, y: 0 }, data: { name: 'N', type: 'TOOL', config: {} } }],
      edges: [],
    }
    h.toObjectMock.mockReturnValue(oldCanvas)
    // 第一次（onMounted 預載）不動畫布；第二次（handleSave 的 await）期間才把畫布換成新版，
    // 模擬使用者在必填清單查詢等待視窗內編輯了畫布
    vi.mocked(workflowApi.getNodeRequiredFields)
      .mockImplementationOnce(async () => ({}))
      .mockImplementationOnce(async () => {
        h.toObjectMock.mockReturnValue(newCanvas)
        return {}
      })
    vi.mocked(workflowApi.save).mockResolvedValueOnce({
      id: 'w1', name: 'flow', status: 'DRAFT', version: 2, nodes: [], edges: [],
    })

    const wrapper = mountEditor()
    await flushPromises()

    await wrapper.find('[data-test="save-button"]').trigger('click')
    await flushPromises()

    expect(workflowApi.save).toHaveBeenCalled()
    // store.save 會把畫布轉為 WorkflowSaveRequestDTO 再送出，flow node 的 id 對應 nodeKey。
    // 若快照在 await 前擷取，會送出 oldCanvas（nodeKey 'old'）；修正後應為 await 後的 newCanvas（'new'）
    const req = vi.mocked(workflowApi.save).mock.calls[0][0] as { nodes: Array<{ nodeKey: string }> }
    expect(req.nodes[0].nodeKey).toBe('new')
  })

  it('DRAFT 流程缺少必填欄位僅提示 warning、不擋存檔', async () => {
    vi.mocked(workflowApi.getNodeRequiredFields).mockResolvedValue({ LLM_ASSISTANT: ['llmId'] })
    vi.mocked(workflowApi.save).mockResolvedValueOnce({
      id: 'w1',
      name: 'flow',
      status: 'DRAFT',
      version: 2,
      nodes: [],
      edges: [],
    })
    h.toObjectMock.mockReturnValue({
      nodes: [
        {
          id: 'a',
          type: 'workflow',
          position: { x: 0, y: 0 },
          data: { name: 'A', type: 'LLM_ASSISTANT', config: {} },
        },
      ],
      edges: [],
    })

    const wrapper = mountEditor()
    await flushPromises()
    const store = useWorkflowStore()
    expect(store.current?.status).toBe('DRAFT')

    await wrapper.find('[data-test="save-button"]').trigger('click')
    await flushPromises()

    expect(workflowApi.save).toHaveBeenCalled()
    expect(h.warningMock).toHaveBeenCalled()
  })
})
