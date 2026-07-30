import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import type { WorkflowDTO } from '../../types/workflow'

// factory 內引用的 mock 一律以 vi.hoisted 定義，避免 TDZ
const {
  setNodesMock,
  setEdgesMock,
  toObjectMock,
  fitViewMock,
  zoomInMock,
  zoomOutMock,
  addNodesMock,
  flowNodes,
  doubleClick,
  routeParams,
  confirmMock,
  successMock,
  errorMock,
  warningMock,
  executeWorkflowMock,
  abortMock,
} = vi.hoisted(() => ({
  setNodesMock: vi.fn(),
  setEdgesMock: vi.fn(),
  toObjectMock: vi.fn((): { nodes: unknown[]; edges: unknown[] } => ({ nodes: [], edges: [] })),
  fitViewMock: vi.fn(),
  zoomInMock: vi.fn(),
  zoomOutMock: vi.fn(),
  addNodesMock: vi.fn(),
  // 畫布節點陣列：測試可直接塞值，模擬 Vue Flow 內部持有的節點（Node Designer 需要它才找得到節點）
  flowNodes: { value: [] as Array<Record<string, unknown>> },
  // 攔下 view 註冊的雙擊 handler，測試可直接觸發（VueFlow 本體已被 stub，沒有真的雙擊事件）
  doubleClick: { handler: null as null | ((payload: { node: { id: string } }) => void) },
  routeParams: { id: 'wf-1' } as { id?: string },
  confirmMock: vi.fn(() => Promise.resolve('confirm')),
  successMock: vi.fn(),
  errorMock: vi.fn(),
  warningMock: vi.fn(),
  executeWorkflowMock: vi.fn(),
  abortMock: vi.fn(),
}))

// ---- mock api/workflow（保留真實 store 以取得 WorkflowVersionConflictError） ----
vi.mock('../../api/workflow', () => ({
  get: vi.fn(),
  save: vi.fn(),
  getNodeRequiredFields: vi.fn().mockResolvedValue({}),
}))

// ---- mock Vue Flow ----
vi.mock('@vue-flow/core', () => ({
  VueFlow: { name: 'VueFlow', template: '<div class="vue-flow-stub" />' },
  Handle: { name: 'Handle', template: '<div />' },
  Position: { Left: 'left', Right: 'right' },
  useVueFlow: () => ({
    onConnect: vi.fn(),
    onNodeClick: vi.fn(),
    onNodeDoubleClick: (cb: (payload: { node: { id: string } }) => void) => {
      doubleClick.handler = cb
    },
    onPaneClick: vi.fn(),
    onNodesChange: vi.fn(),
    onEdgesChange: vi.fn(),
    onPaneReady: vi.fn(),
    addEdges: vi.fn(),
    addNodes: addNodesMock,
    removeNodes: vi.fn(),
    screenToFlowCoordinate: vi.fn(() => ({ x: 0, y: 0 })),
    setNodes: setNodesMock,
    setEdges: setEdgesMock,
    setViewport: vi.fn(),
    toObject: toObjectMock,
    fitView: fitViewMock,
    zoomIn: zoomInMock,
    zoomOut: zoomOutMock,
    viewport: { value: { x: 0, y: 0, zoom: 1 } },
    nodes: flowNodes,
    edges: { value: [] },
  }),
}))

// ---- mock vue-router ----
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: routeParams }),
  useRouter: () => ({ push: vi.fn() }),
  onBeforeRouteLeave: vi.fn(),
}))

// ---- mock 執行 SSE api（避免真實 fetch；斷言 options 是否帶 triggerNodeKey） ----
vi.mock('../../api/workflowExecution', () => ({
  executeWorkflow: executeWorkflowMock,
}))

// ---- mock element-plus 訊息元件 ----
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: { success: successMock, error: errorMock, warning: warningMock },
    ElMessageBox: { confirm: confirmMock },
  }
})

import * as workflowApi from '../../api/workflow'
import WorkflowEditorView from '../WorkflowEditorView.vue'
import ElementPlus from 'element-plus'
import { EDITOR_THEME_KEY, __resetEditorThemeForTest } from '../../composables/useEditorTheme'

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
    flowNodes.value = []
    doubleClick.handler = null
    localStorage.clear()
    __resetEditorThemeForTest()
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

  it('點整理版面應以 dagre 重排節點（x 遞增）並更新畫布、標記未存', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    toObjectMock.mockReturnValue({
      nodes: [
        { id: 'a', type: 'workflow', position: { x: 500, y: 0 }, data: { type: 'TOOL', config: {} } },
        { id: 'b', type: 'workflow', position: { x: 0, y: 0 }, data: { type: 'TOOL', config: {} } },
      ],
      edges: [{ id: 'e-a-b', source: 'a', target: 'b' }],
    })

    const wrapper = mountEditor()
    await flushPromises()

    await wrapper.find('[data-test="tidy-button"]').trigger('click')

    expect(setNodesMock).toHaveBeenCalled()
    const laidOut = setNodesMock.mock.calls.at(-1)![0] as Array<{
      id: string
      position: { x: number }
    }>
    const ax = laidOut.find((n) => n.id === 'a')!.position.x
    const bx = laidOut.find((n) => n.id === 'b')!.position.x
    expect(ax).toBeLessThan(bx)
    expect(fitViewMock).toHaveBeenCalled()
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

  it('工具列名稱 inline input 應改寫 store.current.name 並標記未存', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)

    const wrapper = mountEditor()
    await flushPromises()

    const input = wrapper.find('[data-test="toolbar-name-input"]')
    expect((input.element as HTMLInputElement).value).toBe('流程')

    await input.setValue('新流程名')

    const { useWorkflowStore } = await import('../../stores/workflow')
    const store = useWorkflowStore()
    expect(store.current?.name).toBe('新流程名')
    expect(store.dirty).toBe(true)
    // dirty 徽章顯示未存
    expect(wrapper.find('[data-test="dirty-badge"]').exists()).toBe(true)
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

  it('預設為淺色主題，點主題切換鈕翻轉 data-wf-theme 並寫入 localStorage', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)

    const wrapper = mountEditor()
    await flushPromises()

    const root = wrapper.find('.wf-editor')
    expect(root.attributes('data-wf-theme')).toBe('light')

    await wrapper.find('[data-test="theme-toggle"]').trigger('click')
    expect(root.attributes('data-wf-theme')).toBe('dark')
    expect(localStorage.getItem(EDITOR_THEME_KEY)).toBe('dark')

    await wrapper.find('[data-test="theme-toggle"]').trigger('click')
    expect(root.attributes('data-wf-theme')).toBe('light')
    expect(localStorage.getItem(EDITOR_THEME_KEY)).toBe('light')
  })

  it('卸載編輯器後移除 <html class="dark">，避免污染 Login / WorkflowList', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)

    const wrapper = mountEditor()
    await flushPromises()

    await wrapper.find('[data-test="theme-toggle"]').trigger('click')
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    wrapper.unmount()
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('自訂 zoom bar 的按鈕接上 Vue Flow 的縮放 API 並顯示目前縮放百分比', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)

    const wrapper = mountEditor()
    await flushPromises()

    expect(wrapper.find('[data-test="zoom-value"]').text()).toBe('100%')

    await wrapper.find('[data-test="zoom-in"]').trigger('click')
    expect(zoomInMock).toHaveBeenCalled()

    await wrapper.find('[data-test="zoom-out"]').trigger('click')
    expect(zoomOutMock).toHaveBeenCalled()

    fitViewMock.mockClear()
    await wrapper.find('[data-test="zoom-fit"]').trigger('click')
    expect(fitViewMock).toHaveBeenCalled()
  })

  /**
   * TRIGGER 是每張流程的必要節點，triggerType 是它唯一的必填欄位，
   * 而目前唯一可用值就是 MANUAL（WEBHOOK / CRON 未實作）。
   * 不帶預設值會讓每個使用者每張流程都撞到「缺少必填欄位：觸發類型」。
   */
  it('自 palette 拖入 TRIGGER 應帶入預設 config（triggerType: MANUAL）', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    const wrapper = mountEditor()
    await flushPromises()
    addNodesMock.mockClear()

    const dataTransfer = { getData: vi.fn(() => 'TRIGGER'), dropEffect: '' }
    await wrapper.find('.canvas').trigger('drop', { dataTransfer })

    expect(addNodesMock).toHaveBeenCalledTimes(1)
    const added = addNodesMock.mock.calls[0][0][0]
    expect(added.data.type).toBe('TRIGGER')
    expect(added.data.config).toEqual({ triggerType: 'MANUAL' })
  })

  it('無 defaultConfig 的型別維持空 config，且各節點不共用同一個 config 物件', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    const wrapper = mountEditor()
    await flushPromises()
    addNodesMock.mockClear()

    await wrapper
      .find('.canvas')
      .trigger('drop', { dataTransfer: { getData: vi.fn(() => 'TOOL'), dropEffect: '' } })
    expect(addNodesMock.mock.calls[0][0][0].data.config).toEqual({})

    // 連拖兩個 TRIGGER，兩者的 config 必須是各自獨立的物件（否則改一個會動到另一個）
    await wrapper
      .find('.canvas')
      .trigger('drop', { dataTransfer: { getData: vi.fn(() => 'TRIGGER'), dropEffect: '' } })
    await wrapper
      .find('.canvas')
      .trigger('drop', { dataTransfer: { getData: vi.fn(() => 'TRIGGER'), dropEffect: '' } })
    const first = addNodesMock.mock.calls[1][0][0].data.config
    const second = addNodesMock.mock.calls[2][0][0].data.config
    expect(first).toEqual(second)
    expect(first).not.toBe(second)
  })

  it('雙擊節點開啟 Node Designer，關閉後收起', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    flowNodes.value = [
      { id: 'n1', type: 'workflow', position: { x: 0, y: 0 }, data: { type: 'TOOL', name: '工具節點', config: {} } },
    ]

    const wrapper = mountEditor()
    await flushPromises()
    expect(wrapper.find('[data-test="node-designer-modal"]').exists()).toBe(false)

    doubleClick.handler!({ node: { id: 'n1' } })
    await flushPromises()
    expect(wrapper.find('[data-test="node-designer-modal"]').exists()).toBe(true)
    // 開啟同時也選取該節點，Inspector 與 modal 編輯的是同一份 config
    expect(wrapper.find('[data-test="node-name-input"]').exists()).toBe(true)

    await wrapper.find('[data-test="node-designer-close"]').trigger('click')
    expect(wrapper.find('[data-test="node-designer-modal"]').exists()).toBe(false)
  })
})

describe('WorkflowEditorView — 指定觸發點執行', () => {
  /** 建構畫布節點（Vue Flow 內部形狀），供 triggerNodes computed 讀取 */
  function flowNode(id: string, type: string, name?: string) {
    return { id, type: 'workflow', position: { x: 0, y: 0 }, data: { type, name, config: {} } }
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    routeParams.id = 'wf-1'
    vi.clearAllMocks()
    toObjectMock.mockReturnValue({ nodes: [], edges: [] })
    flowNodes.value = []
    localStorage.clear()
    __resetEditorThemeForTest()
    executeWorkflowMock.mockReturnValue(abortMock)
  })

  afterEach(() => vi.clearAllMocks())

  /** 取出 executeWorkflow 收到的第 5 個引數（ExecuteOptions） */
  const optionsOfLastCall = () => executeWorkflowMock.mock.calls.at(-1)![4]

  it('單一觸發點時點執行直接以該 nodeKey 開跑，不顯示選擇器', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    flowNodes.value = [flowNode('t1', 'TRIGGER', '客服進線'), flowNode('a', 'TOOL')]

    const wrapper = mountEditor()
    await flushPromises()

    await wrapper.find('[data-test="run-button"]').trigger('click')

    expect(wrapper.find('[data-test="trigger-picker"]').exists()).toBe(false)
    expect(executeWorkflowMock).toHaveBeenCalledTimes(1)
    expect(optionsOfLastCall()).toEqual({ triggerNodeKey: 't1' })
  })

  it('多觸發點時點執行先顯示選擇器且尚未開跑', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    flowNodes.value = [flowNode('t1', 'TRIGGER', '客服進線'), flowNode('t2', 'TRIGGER', '每日報表')]

    const wrapper = mountEditor()
    await flushPromises()

    await wrapper.find('[data-test="run-button"]').trigger('click')

    expect(wrapper.find('[data-test="trigger-picker"]').exists()).toBe(true)
    expect(executeWorkflowMock).not.toHaveBeenCalled()
    // 選項以節點顯示名稱呈現
    expect(wrapper.find('[data-test="trigger-option-t1"]').text()).toContain('客服進線')
    expect(wrapper.find('[data-test="trigger-option-t2"]').text()).toContain('每日報表')
  })

  it('選擇某個觸發點後以該 key 開跑，其餘觸發點預先標為 SKIPPED', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    flowNodes.value = [
      flowNode('t1', 'TRIGGER', '客服進線'),
      flowNode('t2', 'TRIGGER', '每日報表'),
      flowNode('t3', 'TRIGGER', '批次匯入'),
    ]

    const wrapper = mountEditor()
    await flushPromises()
    await wrapper.find('[data-test="run-button"]').trigger('click')

    await wrapper.find('[data-test="trigger-option-t2"]').trigger('click')

    expect(optionsOfLastCall()).toEqual({ triggerNodeKey: 't2' })
    // 選完即關閉選擇器
    expect(wrapper.find('[data-test="trigger-picker"]').exists()).toBe(false)

    const { useExecutionStore } = await import('../../stores/execution')
    const executionStore = useExecutionStore()
    expect(executionStore.triggerNodeKey).toBe('t2')
    expect(executionStore.nodeStates['t1'].status).toBe('SKIPPED')
    expect(executionStore.nodeStates['t3'].status).toBe('SKIPPED')
    expect(executionStore.nodeStates['t2']).toBeUndefined()
  })

  it('無觸發節點時點執行顯示提示且不開跑', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    flowNodes.value = [flowNode('a', 'TOOL')]

    const wrapper = mountEditor()
    await flushPromises()

    await wrapper.find('[data-test="run-button"]').trigger('click')

    expect(warningMock).toHaveBeenCalled()
    expect(executeWorkflowMock).not.toHaveBeenCalled()
    expect(wrapper.find('[data-test="trigger-picker"]').exists()).toBe(false)
  })

  it('選擇器可取消，取消後不開跑', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    flowNodes.value = [flowNode('t1', 'TRIGGER'), flowNode('t2', 'TRIGGER')]

    const wrapper = mountEditor()
    await flushPromises()
    await wrapper.find('[data-test="run-button"]').trigger('click')

    await wrapper.find('[data-test="trigger-picker-cancel"]').trigger('click')

    expect(wrapper.find('[data-test="trigger-picker"]').exists()).toBe(false)
    expect(executeWorkflowMock).not.toHaveBeenCalled()
  })

  it('執行中點按鈕走停止（abort），不重新開跑', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    flowNodes.value = [flowNode('t1', 'TRIGGER')]

    const wrapper = mountEditor()
    await flushPromises()
    await wrapper.find('[data-test="run-button"]').trigger('click')
    expect(executeWorkflowMock).toHaveBeenCalledTimes(1)

    await wrapper.find('[data-test="run-button"]').trigger('click')

    expect(abortMock).toHaveBeenCalled()
    expect(executeWorkflowMock).toHaveBeenCalledTimes(1)
  })

  it('未存檔（dirty）時點執行維持原提示且不開跑', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    flowNodes.value = [flowNode('t1', 'TRIGGER')]

    const wrapper = mountEditor()
    await flushPromises()
    // 改名造成未存變更
    await wrapper.find('[data-test="toolbar-name-input"]').setValue('改過的名字')

    await wrapper.find('[data-test="run-button"]').trigger('click')

    expect(warningMock).toHaveBeenCalledWith('有未存變更，請先存檔再執行')
    expect(executeWorkflowMock).not.toHaveBeenCalled()
  })
})
