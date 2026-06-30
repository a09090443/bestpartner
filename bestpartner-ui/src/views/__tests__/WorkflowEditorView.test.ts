import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import type { WorkflowDTO } from '../../types/workflow'

// factory 內引用的 mock 一律以 vi.hoisted 定義，避免 TDZ
const {
  setNodesMock,
  setEdgesMock,
  toObjectMock,
  routeParams,
  confirmMock,
  successMock,
  errorMock,
} = vi.hoisted(() => ({
  setNodesMock: vi.fn(),
  setEdgesMock: vi.fn(),
  toObjectMock: vi.fn(() => ({ nodes: [], edges: [] })),
  routeParams: { id: 'wf-1' } as { id?: string },
  confirmMock: vi.fn(() => Promise.resolve('confirm')),
  successMock: vi.fn(),
  errorMock: vi.fn(),
}))

// ---- mock api/workflow（保留真實 store 以取得 WorkflowVersionConflictError） ----
vi.mock('../../api/workflow', () => ({
  get: vi.fn(),
  save: vi.fn(),
}))

// ---- mock Vue Flow ----
vi.mock('@vue-flow/core', () => ({
  VueFlow: { name: 'VueFlow', template: '<div class="vue-flow-stub" />' },
  Handle: { name: 'Handle', template: '<div />' },
  Position: { Left: 'left', Right: 'right' },
  useVueFlow: () => ({
    onConnect: vi.fn(),
    onNodeClick: vi.fn(),
    onPaneClick: vi.fn(),
    onNodesChange: vi.fn(),
    onEdgesChange: vi.fn(),
    addEdges: vi.fn(),
    addNodes: vi.fn(),
    screenToFlowCoordinate: vi.fn(() => ({ x: 0, y: 0 })),
    setNodes: setNodesMock,
    setEdges: setEdgesMock,
    toObject: toObjectMock,
  }),
}))

// ---- mock vue-router ----
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: routeParams }),
  useRouter: () => ({ push: vi.fn() }),
  onBeforeRouteLeave: vi.fn(),
}))

// ---- mock element-plus 訊息元件 ----
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: { success: successMock, error: errorMock },
    ElMessageBox: { confirm: confirmMock },
  }
})

import * as workflowApi from '../../api/workflow'
import WorkflowEditorView from '../WorkflowEditorView.vue'
import ElementPlus from 'element-plus'

const loaded: WorkflowDTO = {
  id: 'wf-1',
  name: '流程',
  status: 'DRAFT',
  version: 2,
  nodes: [{ nodeKey: 'n1', type: 'TRIGGER', name: '開始', positionX: 0, positionY: 0, config: {} }],
  edges: [],
}

function mountEditor() {
  return mount(WorkflowEditorView, {
    global: { plugins: [createPinia(), ElementPlus] },
  })
}

describe('WorkflowEditorView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routeParams.id = 'wf-1'
    vi.clearAllMocks()
    toObjectMock.mockReturnValue({ nodes: [], edges: [] })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('進入帶 :id 路由應呼叫 store.load 並把節點推入畫布', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)

    mountEditor()
    await flushPromises()

    expect(workflowApi.get).toHaveBeenCalledWith('wf-1')
    expect(setNodesMock).toHaveBeenCalled()
    const pushedNodes = setNodesMock.mock.calls.at(-1)![0]
    expect(pushedNodes[0].id).toBe('n1')
  })

  it('點存檔成功應呼叫 store.save 並顯示成功提示', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    vi.mocked(workflowApi.save).mockResolvedValueOnce({ ...loaded, version: 3 })

    const wrapper = mountEditor()
    await flushPromises()

    await wrapper.find('[data-test="save-button"]').trigger('click')
    await flushPromises()

    expect(workflowApi.save).toHaveBeenCalled()
    expect(successMock).toHaveBeenCalled()
  })

  it('存檔遇版本衝突應彈出重新載入對話框，確認後再次 load', async () => {
    vi.mocked(workflowApi.get).mockResolvedValue(loaded)
    vi.mocked(workflowApi.save).mockRejectedValueOnce({
      response: { data: { code: 400, message: '工作流程已被其他作業修改，請重新載入' } },
    })

    const wrapper = mountEditor()
    await flushPromises()
    expect(workflowApi.get).toHaveBeenCalledTimes(1)

    await wrapper.find('[data-test="save-button"]').trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalled()
    // 確認後重新載入
    expect(workflowApi.get).toHaveBeenCalledTimes(2)
  })
})
