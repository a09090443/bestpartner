import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'

// 捕捉元件註冊的 callback（畫布變更、離頁守衛）
const h = vi.hoisted(() => {
  const captured: { nodesChange?: () => void; leave?: () => unknown } = {}
  return {
    captured,
    toObjectMock: vi.fn(() => ({ nodes: [] as unknown[], edges: [] as unknown[] })),
    routeParams: { id: undefined } as { id?: string },
    confirmMock: vi.fn(() => Promise.resolve('confirm')),
    successMock: vi.fn(),
    errorMock: vi.fn(),
    onBeforeRouteLeaveMock: vi.fn((cb: () => unknown) => {
      captured.leave = cb
    }),
    onNodesChangeMock: vi.fn((cb: () => void) => {
      captured.nodesChange = cb
    }),
  }
})

vi.mock('../../api/workflow', () => ({ get: vi.fn(), save: vi.fn() }))

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
    addEdges: vi.fn(),
    addNodes: vi.fn(),
    screenToFlowCoordinate: vi.fn(() => ({ x: 0, y: 0 })),
    setNodes: vi.fn(),
    setEdges: vi.fn(),
    toObject: h.toObjectMock,
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
    ElMessage: { success: h.successMock, error: h.errorMock },
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

  it('畫布變更應使 store.dirty 變 true', async () => {
    mountEditor()
    await flushPromises()
    const store = useWorkflowStore()
    expect(store.dirty).toBe(false)

    h.captured.nodesChange?.()
    expect(store.dirty).toBe(true)
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
})
