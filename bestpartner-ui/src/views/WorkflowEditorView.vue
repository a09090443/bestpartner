<script setup lang="ts">
import {
  computed,
  markRaw,
  nextTick,
  onBeforeUnmount,
  onMounted,
  provide,
  ref,
  watch,
} from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import type { Connection, Node, NodeTypesObject } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { MiniMap } from '@vue-flow/minimap'
import { ElMessage, ElMessageBox } from 'element-plus'
import NodePalette from '../components/canvas/NodePalette.vue'
import WorkflowNode from '../components/canvas/WorkflowNode.vue'
import ExecutionResultDrawer from '../components/canvas/ExecutionResultDrawer.vue'
import InspectorPanel from '../components/inspector/InspectorPanel.vue'
import { DRAG_NODE_TYPE_KEY } from '../components/canvas/dragKeys'
import { OPEN_DESIGNER_KEY, RUN_FROM_TRIGGER_KEY } from '../components/canvas/designerInjection'
import NodeDesignerModal from '../components/nodeDesigner/NodeDesignerModal.vue'
import { generateNodeKey } from '../composables/useNodeKey'
import { useEditorTheme } from '../composables/useEditorTheme'
import { NODE_FALLBACK_COLOR, getNodeTypeMeta } from '../constants/nodeTypes'
import { flowToSaveRequest, makeEdgeId } from '../composables/useWorkflowSync'
import { layoutGraph } from '../composables/useCanvasLayout'
import { computeFitZoom } from '../composables/useDefaultZoom'
import { validateGraph, CAPABILITY_SOURCE_TYPES } from '../composables/useGraphValidation'
import { IN_MAIN, IN_PROMPT, IN_TOOL } from '../constants/handles'
import type { UpstreamSource } from '../types/nodeDesigner'
import { buildUpstreamRef, type UpstreamRef } from '../constants/nodeOutputKeys'
import { useWorkflowStore, WorkflowVersionConflictError } from '../stores/workflow'
import { useExecutionStore } from '../stores/execution'
import { useAuthStore } from '../stores/auth'
import { extractApiMessage } from '../api/http'
import { getNodeRequiredFields } from '../api/workflow'
import type { FlowNode, FlowEdge } from '../composables/useWorkflowSync'
import type { NodeType } from '../types/workflow'
import type { NodeRequiredFields } from '../api/workflow'
import type { GraphValidationError } from '../composables/useGraphValidation'
import '../styles/workflow-theme.css'

// 單一 Vue Flow 自訂節點型別，語意型別放於 node.data.type
const nodeTypes = { workflow: markRaw(WorkflowNode) } as unknown as NodeTypesObject

const route = useRoute()
const router = useRouter()
const store = useWorkflowStore()
const executionStore = useExecutionStore()
const authStore = useAuthStore()

// 編輯器深／淺主題（預設淺色，偏好存 localStorage；離開本頁時會清掉 <html class="dark">）
const { theme, isDark, toggleTheme } = useEditorTheme()

const {
  onConnect,
  onNodeClick,
  onNodeDoubleClick,
  onPaneClick,
  onNodesChange,
  onEdgesChange,
  onPaneReady,
  addEdges,
  addNodes,
  removeNodes,
  screenToFlowCoordinate,
  setNodes,
  setEdges,
  setViewport,
  toObject,
  fitView,
  zoomIn,
  zoomOut,
  viewport,
  nodes: flowNodesRef,
  edges: flowEdgesRef,
} = useVueFlow()

// 畫布容器 DOM：量測可視寬度以計算「容納約 5 個節點」的預設縮放
const canvasRef = ref<HTMLElement | null>(null)

const selectedNode = ref<FlowNode | null>(null)
const configValid = ref(true)

/**
 * Node Designer 全屏編輯頁鎖定的節點 key（null 為關閉）。
 * 與 selectedNode / configValid 同樣屬「畫布互動狀態」，一律留在 view 的 ref 不進 store；
 * 且 modal 需要的 nodes/edges 只有此處的 useVueFlow() 拿得到。
 */
const designerNodeId = ref<string | null>(null)

/**
 * 開啟指定節點的編輯頁，並同步選取該節點——名稱／設定的更新一律走既有的
 * selectedNode handler，避免出現第二套寫入路徑；關閉 modal 後 Inspector 也已對齊。
 */
function openDesigner(nodeId?: string) {
  if (!nodeId) return
  const node = (flowNodesRef?.value ?? []).find((n) => n.id === nodeId)
  if (node) selectedNode.value = node as unknown as FlowNode
  designerNodeId.value = nodeId
}

function closeDesigner() {
  designerNodeId.value = null
}

/** modal 綁定的節點（節點被刪除時自動失效，template 的 v-if 隨即收起 modal） */
const designerNode = computed<FlowNode | null>(() => {
  if (!designerNodeId.value) return null
  const node = (flowNodesRef?.value ?? []).find((n) => n.id === designerNodeId.value)
  return (node as unknown as FlowNode) ?? null
})

/** modal 的上游輸入來源：畫布 edges ＋ execution store 的節點輸出（只有 view 拿得到 edges） */
const designerSources = computed<UpstreamSource[]>(() => {
  const targetId = designerNodeId.value
  if (!targetId) return []
  const nodes = flowNodesRef?.value ?? []
  return (flowEdgesRef?.value ?? [])
    .filter((edge) => edge.target === targetId)
    .map((edge) => {
      const from = nodes.find((n) => n.id === edge.source)
      const data = from?.data as { name?: string; type?: NodeType } | undefined
      return {
        nodeId: edge.source,
        name: data?.name?.trim() || getNodeTypeMeta(data?.type as NodeType)?.label || edge.source,
        targetHandle: edge.targetHandle ?? IN_MAIN,
        output: executionStore.nodeStates[edge.source]?.output,
      }
    })
})

/** 節點卡右上角的開啟鈕拿不到父層 listener，改以 provide/inject 下放 */
provide(OPEN_DESIGNER_KEY, openDesigner)
/** TRIGGER 節點卡的「從此觸發點執行」鈕同理（函式宣告有 hoisting，可在定義前 provide） */
provide(RUN_FROM_TRIGGER_KEY, runFromTrigger)

onNodeDoubleClick(({ node }) => openDesigner(node.id))

/** 從 modal 刪除節點：先關閉再刪，避免 modal 綁著已不存在的節點 */
function onDesignerDelete() {
  closeDesigner()
  onDeleteNode()
}

// 登出流程進行中：讓 onBeforeRouteLeave 略過未存確認（登出已自行確認過，避免二次跳窗）
const loggingOut = ref(false)

// 各 NodeType 必填欄位清單（後端 NodeConfig 契約）：供 Inspector 即時提示與存檔前驗證共用。
// 載入失敗時維持空物件，Inspector 不顯示提示、存檔驗證則自行重試（見 handleSave）。
const requiredFields = ref<NodeRequiredFields>({})

// 載入既有流程期間為 true：此時 setNodes/setEdges 會觸發 onNodesChange/onEdgesChange，
// 但那是程式化還原、非使用者編輯，不應標記為未存。
const hydrating = ref(false)

// Inspector overview 統計：取畫布的響應式 nodes/edges 計算（測試 mock 可能未提供，需防禦）
const nodeCount = computed(() => flowNodesRef?.value?.length ?? 0)
const connectionCount = computed(() => flowEdgesRef?.value?.length ?? 0)
// 已有提示詞節點連入 in:prompt 埠的 LLM 節點：供 Inspector 標示 userPrompt 已被上游取代
const promptBoundNodeKeys = computed(() =>
  (flowEdgesRef?.value ?? []).filter((e) => e.targetHandle === IN_PROMPT).map((e) => e.target),
)
/**
 * 供 OUTPUT 表單使用的「可引用上游輸出」建議。
 *
 * 列出圖上所有會產生輸出的節點（排除自己、以及不產生輸出的 SKILL / OUTPUT）——
 * 引擎的 ExecutionContext 持有所有已執行節點的輸出，`{{任一nodeKey.欄位}}` 都引用得到，
 * 不限直接上游。已執行過的節點以實際輸出鍵為準，未執行則用型別預設鍵（見 nodeOutputKeys.ts）。
 */
const upstreamRefs = computed<UpstreamRef[]>(() => {
  const selfId = selectedNode.value?.id
  return (flowNodesRef?.value ?? [])
    .filter((n) => {
      const type = (n.data as { type?: NodeType } | undefined)?.type
      return n.id !== selfId && type !== 'SKILL' && type !== 'OUTPUT'
    })
    .map((n) => {
      const data = n.data as
        | { name?: string; type?: NodeType; config?: Record<string, unknown> }
        | undefined
      const type = data?.type as NodeType
      return buildUpstreamRef({
        nodeId: n.id,
        name: data?.name?.trim() || getNodeTypeMeta(type)?.label || n.id,
        type,
        config: data?.config,
        executedOutput: executionStore.nodeStates[n.id]?.output,
      })
    })
    .filter((r) => r.refs.length > 0)
})

/**
 * nodeKey → 人類可讀名稱，供 OUTPUT 的運算式編輯器把 `{{Mr7gjxEL.reply}}`
 * 渲染成「LLM 助手 › reply」。
 *
 * 涵蓋**圖上全部節點**（不只有輸出的那些）——查不到才標成未知引用，
 * 若這裡漏掉某節點，使用者會看到一個其實正確的引用被誤標成紅色。
 * 另補上引擎的內建值：`__input__`（TriggerExecutor.INPUT_KEY）與 LOOP 的迭代別名。
 */
const refLabels = computed<Record<string, string>>(() => {
  const labels: Record<string, string> = { __input__: '啟動資料（執行時傳入）' }
  for (const n of flowNodesRef?.value ?? []) {
    const data = n.data as
      | { name?: string; type?: NodeType; config?: Record<string, unknown> }
      | undefined
    const type = data?.type as NodeType
    labels[n.id] = data?.name?.trim() || getNodeTypeMeta(type)?.label || n.id
    // LOOP 子圖內可用 `{{item}}` 引用當前迭代項，別名可由 itemAlias 覆寫
    if (type === 'LOOP') {
      const alias = data?.config?.itemAlias
      const key = typeof alias === 'string' && alias.trim() ? alias.trim() : 'item'
      labels[key] = `${labels[n.id]} 的迭代項`
    }
  }
  return labels
})

/**
 * 畫布上所有 TRIGGER 節點（key ＝ nodeKey，label ＝ 顯示名稱）。
 * 同一畫布可有多個觸發點，各自獨立執行；選擇器與 Inspector 統計共用此來源。
 */
const triggerNodes = computed(() =>
  (flowNodesRef?.value ?? [])
    .filter((n) => (n.data as { type?: NodeType } | undefined)?.type === 'TRIGGER')
    .map((n) => ({
      key: n.id,
      label: (n.data as { name?: string } | undefined)?.name?.trim() || n.id,
    })),
)

const triggerCount = computed(() => triggerNodes.value.length)

/** 多觸發點時，執行前的觸發點選擇面板是否開啟 */
const triggerPickerVisible = ref(false)

// zoom bar 顯示的縮放百分比（測試 mock 可能未提供 viewport，需防禦）
const zoomPercent = computed(() => Math.round((viewport?.value?.zoom ?? 1) * 100))

const isActive = computed(() => store.current?.status === 'ACTIVE')

// edge 視覺與互動預設：smoothstep 路由、加粗線、加大可點擊範圍（interactionWidth）。
// 這些僅為畫布呈現用，存檔時 flowToSaveRequest 不會帶入這些欄位。
const EDGE_DEFAULTS = {
  type: 'smoothstep',
  interactionWidth: 28,
  style: { strokeWidth: 2.5 },
}

function decorateEdge<T extends object>(edge: T): T & typeof EDGE_DEFAULTS {
  return { ...EDGE_DEFAULTS, ...edge }
}

/** MiniMap 節點著色：沿用節點型別代表色 */
function minimapNodeColor(node: Node): string {
  const type = (node.data as { type?: NodeType } | undefined)?.type
  return (type && getNodeTypeMeta(type)?.color) || NODE_FALLBACK_COLOR
}

function currentId(): string | undefined {
  const idParam = route.params.id
  return Array.isArray(idParam) ? idParam[0] : idParam
}

/**
 * 執行期間讓「資料已流過」的連線顯示流動虛線（樣式見 workflow-theme.css 的 .is-flowing）。
 * 判準：來源節點已完成，且目標節點正在跑或已完成 —— 亦即這條線上的資料確實傳遞過。
 * 直接改 edge 物件的 class（Vue Flow 的 edges 是響應式陣列），不另建 computed，
 * 因為 :default-edge-options 只在建立時套用一次。
 */
watch(
  () => [executionStore.running, executionStore.nodeStates] as const,
  () => {
    const states = executionStore.nodeStates
    for (const edge of flowEdgesRef?.value ?? []) {
      const from = states[edge.source]?.status
      const to = states[edge.target]?.status
      const flowing =
        executionStore.running && from === 'SUCCESS' && (to === 'RUNNING' || to === 'SUCCESS')
      edge.class = flowing ? 'is-flowing' : ''
    }
  },
  { deep: true },
)

/** 依畫布可視寬度計算「容納約 5 個節點」的預設縮放；量不到寬度時退回固定值 */
function defaultCanvasZoom(): number {
  return computeFitZoom({ canvasWidth: canvasRef.value?.clientWidth ?? 0 })
}

/**
 * 套用預設檢視，讓各情境的初始視野都以「約 5 個節點寬」為基準：
 * 有節點時 fitView 但以預設縮放為上限（少數節點不被放到過大）；
 * 空白畫布則定位到預設縮放並留左上空白，方便放置第一個節點。
 * 僅改動 viewport、不變動節點，故不會標記未存。
 */
function applyDefaultView() {
  const zoom = defaultCanvasZoom()
  const hasNodes = (flowNodesRef?.value?.length ?? 0) > 0
  if (hasNodes) {
    fitView({ maxZoom: zoom, padding: 0.2 })
  } else {
    setViewport({ x: 80, y: 80, zoom })
  }
}

async function loadIntoCanvas(id: string) {
  hydrating.value = true
  try {
    await store.load(id)
    if (store.current) {
      setNodes(store.current.nodes as unknown as Node[])
      setEdges(store.current.edges.map(decorateEdge))
    }
    // 等畫布套用完 setNodes/setEdges 觸發的變更事件後，再重置為已存狀態
    await nextTick()
    store.setDirty(false)
    // 節點就位後套預設視野（pane 若尚未 ready，onPaneReady 會再補一次）
    applyDefaultView()
  } finally {
    hydrating.value = false
  }
}

onMounted(async () => {
  const id = currentId()
  if (id) {
    await loadIntoCanvas(id)
  } else {
    store.createNew()
    await nextTick()
    applyDefaultView()
  }
})

// pane 初始化後套用預設檢視（涵蓋首次載入既有流程與空白流程）
onPaneReady(() => applyDefaultView())

// 預先載入必填欄位清單，供 Inspector 即時提示；失敗時靜默忽略（不影響畫布可用性）
onMounted(async () => {
  try {
    requiredFields.value = await getNodeRequiredFields()
  } catch {
    // 忽略：Inspector 提示退化為不顯示，handleSave 存檔時會再各自嘗試一次
  }
})

// 僅「使用者編輯」類變更才標記未存：
// 節點取 add/remove/position（排除 dimensions 尺寸量測、select 選取）；
// 連線取 add/remove。並在 hydrating 期間一律忽略（程式化還原）。
const DIRTYING_NODE_CHANGES = new Set(['add', 'remove', 'position'])
const DIRTYING_EDGE_CHANGES = new Set(['add', 'remove'])

function markDirtyFromChanges(
  changes: ReadonlyArray<{ type?: string }> | undefined,
  dirtying: Set<string>,
) {
  if (hydrating.value) return
  if (changes?.some((c) => c.type != null && dirtying.has(c.type))) {
    store.setDirty(true)
  }
}

onNodesChange((changes) => markDirtyFromChanges(changes, DIRTYING_NODE_CHANGES))
onEdgesChange((changes) => markDirtyFromChanges(changes, DIRTYING_EDGE_CHANGES))

// 離頁攔截：有未存變更時提示確認
onBeforeRouteLeave(async () => {
  if (loggingOut.value) return true
  if (!store.dirty) return true
  try {
    await ElMessageBox.confirm('有未存變更，確定要離開嗎？', '尚未存檔', {
      type: 'warning',
      confirmButtonText: '離開',
      cancelButtonText: '留下',
    })
    return true
  } catch {
    return false
  }
})

// 重新整理/關閉分頁時的原生提示
function beforeUnloadHandler(event: BeforeUnloadEvent) {
  if (store.dirty) {
    event.preventDefault()
    event.returnValue = ''
  }
}
onMounted(() => window.addEventListener('beforeunload', beforeUnloadHandler))
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnloadHandler)
  executionStore.stop()
  executionStore.reset()
})

onConnect((connection: Connection) => {
  // Agent 模式相容性：LLM 工具埠（in:tool）只接受 TOOL / MCP_SERVER / SKILL / KNOWLEDGE_RAG 來源
  if (connection.targetHandle === IN_TOOL) {
    const sourceType = (flowNodesRef?.value ?? []).find((n) => n.id === connection.source)?.data
      ?.type as NodeType | undefined
    if (!sourceType || !CAPABILITY_SOURCE_TYPES.has(sourceType)) {
      ElMessage.warning('僅工具、MCP、Skill、知識庫節點可連到 LLM 的工具埠')
      return
    }
  }
  // 提示埠（in:prompt）只接受 PROMPT 來源；此埠為一般資料流，提供該次推論的提問內容
  if (connection.targetHandle === IN_PROMPT) {
    const sourceType = (flowNodesRef?.value ?? []).find((n) => n.id === connection.source)?.data
      ?.type as NodeType | undefined
    if (sourceType !== 'PROMPT') {
      ElMessage.warning('僅提示詞節點可連到 LLM 的提示埠')
      return
    }
  }
  const id = makeEdgeId(
    connection.source,
    connection.target,
    connection.sourceHandle,
    connection.targetHandle,
  )
  addEdges([decorateEdge({ ...connection, id })])
  store.setDirty(true)
})

onNodeClick(({ node }) => {
  selectedNode.value = node as unknown as FlowNode
})

onPaneClick(() => {
  selectedNode.value = null
})

function onDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
}

function onDrop(event: DragEvent) {
  event.preventDefault()
  const type = event.dataTransfer?.getData(DRAG_NODE_TYPE_KEY) as NodeType | undefined
  if (!type) return

  const position = screenToFlowCoordinate({ x: event.clientX, y: event.clientY })
  const meta = getNodeTypeMeta(type)
  const newNode: Node = {
    id: generateNodeKey(),
    type: 'workflow',
    position,
    // defaultConfig 必須展開複製，否則同型別的多個節點會共用 nodeTypes.ts 上的同一個物件
    data: { name: meta?.label ?? type, type, config: { ...(meta?.defaultConfig ?? {}) } },
  }
  addNodes([newNode])
  store.setDirty(true)
}

// Inspector：節點屬性編輯
function onNodeNameUpdate(name: string) {
  if (selectedNode.value) {
    selectedNode.value.data.name = name
    store.setDirty(true)
  }
}

function onNodeConfigUpdate(config: Record<string, unknown>) {
  if (selectedNode.value) {
    selectedNode.value.data.config = config
    store.setDirty(true)
  }
}

// Inspector：複製選取節點（偏移放置，config 淺拷貝避免共用參考）
function onDuplicateNode() {
  const node = selectedNode.value
  if (!node) return
  const copy: Node = {
    id: generateNodeKey(),
    type: 'workflow',
    position: { x: node.position.x + 48, y: node.position.y + 48 },
    data: { ...node.data, config: { ...(node.data.config ?? {}) } },
  }
  addNodes([copy])
  store.setDirty(true)
}

// Inspector：刪除選取節點
function onDeleteNode() {
  const node = selectedNode.value
  if (!node) return
  removeNodes([node.id])
  selectedNode.value = null
  store.setDirty(true)
}

// 流程 meta 編輯（工具列 inline input 與 Inspector overview 共用）
function onWorkflowNameUpdate(name: string) {
  if (store.current) {
    store.current.name = name
    store.setDirty(true)
  }
}

function onWorkflowDescriptionUpdate(description: string) {
  if (store.current) {
    store.current.description = description
    store.setDirty(true)
  }
}

// Active 切換：已存檔（有 id）才呼叫後端 switchStatus
async function handleActiveToggle(value: string | number | boolean) {
  const active = value === true
  if (!store.current) return
  const id = store.current.id
  if (!id) {
    ElMessage.warning('請先存檔後再啟用流程')
    return
  }
  try {
    await store.switchStatus(id, active)
    store.current.status = active ? 'ACTIVE' : 'INACTIVE'
    ElMessage.success(active ? '流程已啟用' : '流程已停用')
  } catch (err) {
    ElMessage.error(extractApiMessage(err) ?? '切換狀態失敗')
  }
}

/**
 * 執行前守衛：需先存檔（有 id 且無未存變更）。通過回傳 workflowId，否則提示並回傳 null。
 * 訊息與檢查順序刻意與抽出前的 handleRun 一致，避免既有行為漂移。
 */
function ensureRunnable(): string | null {
  const id = store.current?.id
  if (!id) {
    ElMessage.warning('請先存檔後再執行')
    return null
  }
  if (store.dirty) {
    ElMessage.warning('有未存變更，請先存檔再執行')
    return null
  }
  return id
}

/**
 * 以指定觸發點啟動執行；工具列選擇器與 TRIGGER 節點卡兩條入口共用。
 *
 * 未選中的觸發點預先標為 SKIPPED——後端對 SKIPPED 節點不發 SSE 事件，
 * 不預標的話畫布上會完全無狀態，使用者分不清「刻意沒跑」與「還沒跑到」。
 * 只標觸發點本身、不推算下游（見 stores/execution.ts 的 StartOptions.skipNodeKeys）。
 */
function runFromTrigger(triggerNodeKey: string) {
  const id = ensureRunnable()
  if (!id) return
  triggerPickerVisible.value = false
  const skipNodeKeys = triggerNodes.value.map((t) => t.key).filter((k) => k !== triggerNodeKey)
  executionStore.start(id, { triggerNodeKey, skipNodeKeys })
}

// 執行/停止：執行中按鈕轉為停止；否則依觸發點數量決定直接跑或先選擇入口
async function handleRun() {
  if (executionStore.running) {
    executionStore.stop()
    return
  }
  if (!ensureRunnable()) return
  const triggers = triggerNodes.value
  if (triggers.length === 0) {
    ElMessage.warning('流程尚無觸發節點，請先加入觸發節點')
    return
  }
  // 單一觸發點直接跑（仍明確帶上 triggerNodeKey，使 UI 永遠是「個別執行」語義）
  if (triggers.length === 1) {
    runFromTrigger(triggers[0].key)
    return
  }
  triggerPickerVisible.value = true
}

// 登出：路由守衛會把「已登入卻訪問 /login」導回首頁，故必須先清 token 再導航。
// 但先清 token 會與未存變更的離頁確認衝突（選「留下」時 token 已沒了），
// 因此這裡自行做未存確認，並以 loggingOut 讓 onBeforeRouteLeave 略過、避免二次確認。
async function handleLogout() {
  if (store.dirty) {
    try {
      await ElMessageBox.confirm('有未存變更，確定要登出嗎？', '尚未存檔', {
        type: 'warning',
        confirmButtonText: '登出',
        cancelButtonText: '取消',
      })
    } catch {
      // 使用者取消
      return
    }
  }
  loggingOut.value = true
  authStore.logout()
  router.push('/login')
}

/**
 * 返回流程列表。未存變更的確認一律交給 onBeforeRouteLeave 統一處理，
 * 此處不重複跳窗（避免與登出流程一樣出現兩層確認）。
 */
function handleBack() {
  router.push('/')
}

// 整理版面：以 dagre 重排節點位置，更新畫布並置中檢視
function handleTidyUp() {
  const obj = toObject()
  const laidOut = layoutGraph(
    obj.nodes as unknown as FlowNode[],
    obj.edges as unknown as FlowEdge[],
  )
  setNodes(laidOut as unknown as Node[])
  store.setDirty(true)
  nextTick(() => fitView({ maxZoom: defaultCanvasZoom(), padding: 0.2 }))
}

/**
 * ACTIVE 流程存檔遇必填缺漏時的擋存訊息：聚合所有缺漏節點的 key（避免只顯示第一筆），
 * 訊息過長時最多列出前 5 個並以「等」收尾；並附一句引導，提示可先停用以暫存半成品。
 */
function buildActiveRequiredFieldMessage(missingResults: GraphValidationError[]): string {
  const keys = missingResults.map((r) => r.key).filter((k): k is string => Boolean(k))
  const shown = keys.slice(0, 5)
  const suffix = keys.length > shown.length ? ' 等' : ''
  return `有 ${keys.length} 個節點缺必填：${shown.join('、')}${suffix}。可先停用（Active off）以暫存半成品`
}

async function handleSave() {
  if (!configValid.value) {
    ElMessage.error('節點設定 JSON 格式錯誤，請修正後再存檔')
    return
  }
  // 必填欄位清單取自後端契約；查詢失敗則降級跳過必填檢查，不阻斷既有存檔流程
  let latestRequiredFields: NodeRequiredFields | undefined
  try {
    latestRequiredFields = await getNodeRequiredFields()
    requiredFields.value = latestRequiredFields
  } catch {
    latestRequiredFields = undefined
  }

  // 快照於 await 之後才擷取：確保驗證與存檔用的是「等待期間可能已被編輯」的最新畫布，
  // 避免首次存檔（快取未預載、await 為真實網路等待）期間的編輯遺失
  const flowNodes = toObject().nodes as unknown as FlowNode[]
  const flowEdges = toObject().edges as unknown as FlowEdge[]

  // 存檔前先跑與後端同義的輕量驗證。error 擋存檔；warning 提示但放行（啟用時再擋）
  const req = flowToSaveRequest(flowNodes, flowEdges, {
    name: store.current?.name ?? '未命名流程',
  })

  const results = validateGraph(req.nodes, req.edges, latestRequiredFields)
  const blockingError = results.find((r) => r.severity === 'error')
  if (blockingError) {
    ElMessage.error(blockingError.message)
    return
  }

  // ACTIVE 流程：必填缺漏比照後端行為升級為擋存錯誤；聚合多節點訊息，避免只提示第一筆
  if (isActive.value) {
    const missingRequired = results.filter((r) => r.type === 'REQUIRED_FIELD_MISSING')
    if (missingRequired.length > 0) {
      ElMessage.error(buildActiveRequiredFieldMessage(missingRequired))
      return
    }
  }

  const warning = results.find((r) => r.severity === 'warning')
  if (warning) {
    ElMessage.warning(warning.message)
  }

  try {
    await store.save(flowNodes, flowEdges)
    ElMessage.success('已存檔')
  } catch (err) {
    if (err instanceof WorkflowVersionConflictError) {
      try {
        await ElMessageBox.confirm(
          '此流程已被其他作業修改。重新載入將捨棄目前未存變更，是否繼續？',
          '版本衝突',
          { type: 'warning', confirmButtonText: '重新載入', cancelButtonText: '取消' },
        )
      } catch {
        return
      }
      const id = currentId()
      if (id) await loadIntoCanvas(id)
      return
    }
    ElMessage.error(extractApiMessage(err) ?? '存檔失敗')
  }
}
</script>

<template>
  <div class="wf-editor editor-layout" :data-wf-theme="theme">
    <!-- 頂部工具列 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <button
          type="button"
          class="icon-btn"
          data-test="back-button"
          aria-label="返回流程列表"
          title="返回流程列表"
          @click="handleBack"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M15 5l-7 7 7 7" />
          </svg>
        </button>
        <div class="logo-box" aria-hidden="true">⚡</div>
        <span class="breadcrumb-box">
          <button
            type="button"
            class="breadcrumb breadcrumb-link"
            data-test="breadcrumb-list"
            @click="handleBack"
          >
            Personal
          </button>
          <span class="breadcrumb-sep" aria-hidden="true">/</span>
        </span>
        <input
          class="wf-name-input"
          data-test="toolbar-name-input"
          :value="store.current?.name ?? ''"
          placeholder="未命名流程"
          @input="onWorkflowNameUpdate(($event.target as HTMLInputElement).value)"
        />
        <span v-if="store.dirty" class="save-badge dirty" data-test="dirty-badge">● 未存</span>
        <span v-else class="save-badge saved" data-test="saved-badge">● Saved</span>
        <span class="version">v{{ store.current?.version ?? '-' }}</span>
      </div>

      <div class="toolbar-right">
        <button
          type="button"
          class="icon-btn"
          data-test="theme-toggle"
          :aria-label="isDark ? '切換為淺色主題' : '切換為深色主題'"
          :title="isDark ? '切換為淺色主題' : '切換為深色主題'"
          @click="toggleTheme"
        >
          <!-- 深色時顯示太陽（點了會變亮），淺色時顯示月亮 -->
          <svg
            v-if="isDark"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.9"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <circle cx="12" cy="12" r="4.2" />
            <path
              d="M12 2.5v2.2M12 19.3v2.2M2.5 12h2.2M19.3 12h2.2M5.4 5.4l1.6 1.6M17 17l1.6 1.6M18.6 5.4 17 7M7 17l-1.6 1.6"
            />
          </svg>
          <svg
            v-else
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.9"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M20.5 13.4A8.5 8.5 0 1 1 10.6 3.5a6.8 6.8 0 0 0 9.9 9.9z" />
          </svg>
        </button>

        <div class="tabs">
          <button type="button" class="tab active">Editor</button>
          <button type="button" class="tab" disabled title="即將推出">Executions</button>
        </div>

        <div class="active-toggle">
          <span class="toggle-label">Active</span>
          <el-switch
            data-test="active-toggle"
            :model-value="isActive"
            size="small"
            @change="handleActiveToggle"
          />
        </div>

        <button type="button" class="btn ghost" data-test="tidy-button" @click="handleTidyUp">
          整理版面
        </button>
        <button type="button" class="btn ghost" data-test="save-button" @click="handleSave">
          存檔
        </button>
        <!-- 執行是工具列唯一的 accent 實心按鈕（設計稿的主要行動） -->
        <button
          type="button"
          class="btn primary run-btn"
          :class="{ 'is-running': executionStore.running }"
          data-test="run-button"
          @click="handleRun"
        >
          <span v-if="executionStore.running" class="run-spinner" aria-hidden="true" />
          <svg
            v-else
            class="run-glyph"
            viewBox="0 0 24 24"
            fill="currentColor"
            aria-hidden="true"
          >
            <path d="M8 5.6v12.8L19 12z" />
          </svg>
          {{ executionStore.running ? '停止' : '執行' }}
        </button>
        <button
          type="button"
          class="btn ghost logout"
          data-test="logout-button"
          @click="handleLogout"
        >
          登出
        </button>
      </div>
    </div>

    <div class="editor-body">
      <NodePalette />

      <div class="canvas" ref="canvasRef" @drop="onDrop" @dragover="onDragOver">
        <VueFlow
          :node-types="nodeTypes"
          :default-edge-options="EDGE_DEFAULTS"
          delete-key-code="Delete"
        >
          <!-- 點色不走 color prop：該 prop 會變成 <circle fill> presentation attribute，
               無法用 CSS 變數。改由下方 :deep(.vue-flow__background circle) 的 fill 覆寫，
               才能跟著主題切換。 -->
          <Background variant="dots" :gap="20" :size="1.4" />
          <MiniMap :node-color="minimapNodeColor" pannable zoomable />
        </VueFlow>

        <!-- 自訂 zoom bar（取代 @vue-flow/controls 的預設樣式，才能與設計稿一致並跟著主題切換） -->
        <div class="zoom-bar" data-test="zoom-bar">
          <button type="button" class="zoom-btn" data-test="zoom-out" aria-label="縮小" @click="zoomOut()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
              <path d="M5 12h14" />
            </svg>
          </button>
          <span class="zoom-value" data-test="zoom-value">{{ zoomPercent }}%</span>
          <button type="button" class="zoom-btn" data-test="zoom-in" aria-label="放大" @click="zoomIn()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
              <path d="M12 5v14M5 12h14" />
            </svg>
          </button>
          <span class="zoom-divider" />
          <button type="button" class="zoom-btn" data-test="zoom-fit" aria-label="適應視窗" @click="fitView({ padding: 0.2 })">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M4 9V5a1 1 0 0 1 1-1h4M15 4h4a1 1 0 0 1 1 1v4M20 15v4a1 1 0 0 1-1 1h-4M9 20H5a1 1 0 0 1-1-1v-4" />
            </svg>
          </button>
        </div>
        <!--
          多觸發點時的入口選擇面板。刻意不用 <Teleport>／el-dialog 預設 teleport：
          --wf-* token 與 wf-form.css 的 `.wf-editor ` 前綴規則都綁在 .wf-editor 上，
          teleport 到 body 兩者會同時失效（同 NodeDesignerModal 的取捨）。
        -->
        <div
          v-if="triggerPickerVisible"
          class="trigger-picker-backdrop"
          @click.self="triggerPickerVisible = false"
        >
          <div class="trigger-picker" data-test="trigger-picker">
            <div class="trigger-picker-title">選擇要執行的觸發點</div>
            <div class="trigger-picker-hint">只會執行該觸發點的流程，其餘觸發點不啟動</div>
            <button
              v-for="t in triggerNodes"
              :key="t.key"
              type="button"
              class="trigger-option"
              :data-test="`trigger-option-${t.key}`"
              @click="runFromTrigger(t.key)"
            >
              <svg class="trigger-option-glyph" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M8 5.6v12.8L19 12z" />
              </svg>
              <span class="trigger-option-label">{{ t.label }}</span>
            </button>
            <button
              type="button"
              class="btn ghost trigger-picker-cancel"
              data-test="trigger-picker-cancel"
              @click="triggerPickerVisible = false"
            >
              取消
            </button>
          </div>
        </div>
        <ExecutionResultDrawer />
      </div>

      <div class="inspector">
        <InspectorPanel
          :selected-node="selectedNode"
          :workflow-name="store.current?.name ?? ''"
          :workflow-description="store.current?.description"
          :node-count="nodeCount"
          :connection-count="connectionCount"
          :trigger-count="triggerCount"
          :workflow-status="store.current?.status"
          :required-fields="requiredFields"
          :prompt-bound-node-keys="promptBoundNodeKeys"
          :upstream-refs="upstreamRefs"
          :ref-labels="refLabels"
          @update:node-name="onNodeNameUpdate"
          @update:node-config="onNodeConfigUpdate"
          @update:workflow-name="onWorkflowNameUpdate"
          @update:workflow-description="onWorkflowDescriptionUpdate"
          @duplicate-node="onDuplicateNode"
          @delete-node="onDeleteNode"
          @config-validity="configValid = $event"
          @open-designer="openDesigner(selectedNode?.id)"
        />
      </div>
    </div>

    <!-- 節點全屏編輯頁。刻意留在 .wf-editor 內（不 teleport），否則主題 token 與
         wf-form.css 的 `.wf-editor ` 前綴規則都會失效——說明見該元件註解。 -->
    <NodeDesignerModal
      v-if="designerNode"
      :key="designerNode.id"
      :node-id="designerNode.id"
      :type="designerNode.data.type"
      :name="designerNode.data.name ?? ''"
      :config="designerNode.data.config ?? {}"
      :sources="designerSources"
      :run="executionStore.nodeStates[designerNode.id]"
      :required-fields="requiredFields"
      :prompt-bound-node-keys="promptBoundNodeKeys"
      @close="closeDesigner"
      @update:node-name="onNodeNameUpdate"
      @update:node-config="onNodeConfigUpdate"
      @config-validity="configValid = $event"
      @duplicate-node="onDuplicateNode"
      @delete-node="onDesignerDelete"
    />
  </div>
</template>

<style scoped>
.editor-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
}

/* ---- 工具列 ---- */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 54px;
  flex-shrink: 0;
  padding: 0 14px;
  box-sizing: border-box;
  background: var(--wf-panel);
  border-bottom: 1px solid var(--wf-border);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.logo-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  font-size: 14px;
  background: var(--wf-accent);
  border-radius: 8px;
}

.breadcrumb-box {
  display: flex;
  align-items: center;
  min-width: 0;
}

.breadcrumb {
  font-size: 12.5px;
  color: var(--wf-text-2);
  white-space: nowrap;
}

.breadcrumb-link {
  padding: 0;
  font-family: inherit;
  background: transparent;
  border: 0;
  cursor: pointer;
  transition: color 0.12s;
}

.breadcrumb-link:hover {
  color: var(--wf-text);
  text-decoration: underline;
}

.breadcrumb-sep {
  color: var(--wf-line);
  margin-left: 2px;
}

.wf-name-input {
  min-width: 120px;
  max-width: 280px;
  padding: 5px 8px;
  font-size: 13.5px;
  font-weight: 700;
  font-family: inherit;
  color: var(--wf-text);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  outline: none;
  transition: border-color 0.15s, background-color 0.15s;
}

.wf-name-input:hover {
  border-color: var(--wf-border);
}

.wf-name-input:focus {
  border-color: var(--wf-accent);
  background: var(--wf-input);
}

.save-badge {
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.save-badge.saved {
  color: var(--wf-success);
}

.save-badge.dirty {
  color: var(--wf-accent);
}

.version {
  font-size: 11px;
  font-family: var(--wf-font-mono);
  color: var(--wf-text-3);
  white-space: nowrap;
}

/* 方形圖示按鈕（主題切換） */
.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  padding: 0;
  color: var(--wf-text-2);
  background: var(--wf-card-2);
  border: 1px solid var(--wf-border);
  border-radius: 9px;
  cursor: pointer;
  transition: background-color 0.12s, color 0.12s;
}

.icon-btn:hover {
  color: var(--wf-text);
  background: var(--wf-hover);
}

.tabs {
  display: flex;
  gap: 2px;
  padding: 3px;
  background: var(--wf-input);
  border: 1px solid var(--wf-border);
  border-radius: 9px;
}

.tab {
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 700;
  font-family: inherit;
  color: var(--wf-text-2);
  background: transparent;
  border: none;
  border-radius: 7px;
  cursor: pointer;
}

.tab.active {
  color: var(--wf-text);
  background: var(--wf-card);
}

.tab:disabled {
  color: var(--wf-text-3);
  cursor: not-allowed;
}

.active-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
}

.toggle-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--wf-text-2);
}

.btn {
  padding: 7px 14px;
  font-size: 12.5px;
  font-weight: 700;
  font-family: inherit;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition: filter 0.12s, border-color 0.12s;
}

.btn.ghost {
  color: var(--wf-text);
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
}

.btn.ghost:hover {
  border-color: var(--wf-text-3);
}

.btn.primary {
  color: var(--wf-on-accent);
  background: var(--wf-accent);
  border: 1px solid var(--wf-accent);
}

.btn.primary:hover {
  filter: brightness(1.08);
}

/* 執行鈕：圖示 + 文字並排；執行中換成旋轉指示器 */
.run-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.run-glyph {
  width: 13px;
  height: 13px;
}

.run-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid color-mix(in srgb, var(--wf-on-accent) 35%, transparent);
  border-top-color: var(--wf-on-accent);
  border-radius: 50%;
  animation: run-spin 0.7s linear infinite;
}

@keyframes run-spin {
  to {
    transform: rotate(360deg);
  }
}

.btn.ghost.logout {
  margin-left: 4px;
  color: var(--wf-text-2);
}

.btn.ghost.logout:hover {
  color: var(--wf-text);
}

/* ---- 主體三欄 ---- */
.editor-body {
  flex: 1;
  min-height: 0;
  display: flex;
}

.canvas {
  flex: 1;
  position: relative;
  min-width: 0;
}

.canvas :deep(.vue-flow) {
  background: var(--wf-bg);
}

/* 點陣格線：Background 的 color prop 產生的是 <circle fill> presentation attribute，
   優先權低於任何 CSS 規則，故以 fill 屬性覆寫即可跟隨主題 */
.canvas :deep(.vue-flow__background circle) {
  fill: var(--wf-dot);
}

.inspector {
  width: 322px;
  flex-shrink: 0;
  border-left: 1px solid var(--wf-border);
  background: var(--wf-panel);
  overflow-y: auto;
}

/* ---- 連線：深色預設，hover / 選取轉 accent ---- */
.canvas :deep(.vue-flow__edge-path) {
  stroke: var(--wf-line);
  transition: stroke 0.15s, stroke-width 0.15s;
}

.canvas :deep(.vue-flow__edge:hover .vue-flow__edge-path) {
  stroke: var(--wf-accent);
  stroke-width: 3.5;
}

.canvas :deep(.vue-flow__edge.selected .vue-flow__edge-path) {
  stroke: var(--wf-accent);
  stroke-width: 4;
}

/* ---- 自訂 zoom bar（左下角，浮在畫布之上） ---- */
.zoom-bar {
  position: absolute;
  left: 14px;
  bottom: 14px;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px;
  background: var(--wf-panel);
  border: 1px solid var(--wf-border);
  border-radius: 11px;
  box-shadow: var(--wf-shadow);
}

.zoom-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  color: var(--wf-text-2);
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}

.zoom-btn svg {
  width: 15px;
  height: 15px;
}

.zoom-btn:hover {
  color: var(--wf-text);
  background: var(--wf-hover);
}

.zoom-value {
  min-width: 42px;
  text-align: center;
  font-family: var(--wf-font-mono);
  font-size: 11px;
  color: var(--wf-text-2);
  user-select: none;
}

.zoom-divider {
  width: 1px;
  height: 16px;
  margin: 0 3px;
  background: var(--wf-border);
}

/* ---- MiniMap 主題覆寫 ---- */
.canvas :deep(.vue-flow__minimap) {
  background: var(--wf-panel);
  border: 1px solid var(--wf-border);
  border-radius: 11px;
  overflow: hidden;
}

.canvas :deep(.vue-flow__minimap-mask) {
  fill: var(--wf-minimap-mask);
}

/* ---- 多觸發點的入口選擇面板（浮在畫布之上，留在 .wf-editor 作用域內） ---- */
.trigger-picker-backdrop {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--wf-bg) 62%, transparent);
}

.trigger-picker {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: min(320px, 84%);
  padding: 16px;
  background: var(--wf-panel);
  border: 1px solid var(--wf-border);
  border-radius: 12px;
  box-shadow: var(--wf-shadow);
}

.trigger-picker-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--wf-text);
}

.trigger-picker-hint {
  margin-bottom: 4px;
  font-size: 11.5px;
  color: var(--wf-text-3);
}

.trigger-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 11px;
  font-size: 12.5px;
  color: var(--wf-text);
  text-align: left;
  background: var(--wf-card-2);
  border: 1px solid var(--wf-border);
  border-radius: 9px;
  cursor: pointer;
  transition: border-color 0.12s, background 0.12s;
}

.trigger-option:hover {
  background: color-mix(in srgb, var(--wf-accent) 12%, var(--wf-card-2));
  border-color: var(--wf-accent);
}

.trigger-option-glyph {
  width: 11px;
  height: 11px;
  flex-shrink: 0;
  color: var(--wf-accent);
}

.trigger-option-label {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.trigger-picker-cancel {
  margin-top: 4px;
  align-self: flex-end;
}
</style>
