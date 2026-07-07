<script setup lang="ts">
import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, onBeforeRouteLeave } from 'vue-router'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import type { Connection, Node, NodeTypesObject } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { ElMessage, ElMessageBox } from 'element-plus'
import NodePalette from '../components/canvas/NodePalette.vue'
import WorkflowNode from '../components/canvas/WorkflowNode.vue'
import InspectorPanel from '../components/inspector/InspectorPanel.vue'
import { DRAG_NODE_TYPE_KEY } from '../components/canvas/dragKeys'
import { generateNodeKey } from '../composables/useNodeKey'
import { getNodeTypeMeta } from '../constants/nodeTypes'
import { flowToSaveRequest, makeEdgeId } from '../composables/useWorkflowSync'
import { layoutGraph } from '../composables/useCanvasLayout'
import { validateGraph } from '../composables/useGraphValidation'
import { useWorkflowStore, WorkflowVersionConflictError } from '../stores/workflow'
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
const store = useWorkflowStore()

const {
  onConnect,
  onNodeClick,
  onPaneClick,
  onNodesChange,
  onEdgesChange,
  addEdges,
  addNodes,
  removeNodes,
  screenToFlowCoordinate,
  setNodes,
  setEdges,
  toObject,
  fitView,
  nodes: flowNodesRef,
  edges: flowEdgesRef,
} = useVueFlow()

const selectedNode = ref<FlowNode | null>(null)
const configValid = ref(true)

// 各 NodeType 必填欄位清單（後端 NodeConfig 契約）：供 Inspector 即時提示與存檔前驗證共用。
// 載入失敗時維持空物件，Inspector 不顯示提示、存檔驗證則自行重試（見 handleSave）。
const requiredFields = ref<NodeRequiredFields>({})

// 載入既有流程期間為 true：此時 setNodes/setEdges 會觸發 onNodesChange/onEdgesChange，
// 但那是程式化還原、非使用者編輯，不應標記為未存。
const hydrating = ref(false)

// Inspector overview 統計：取畫布的響應式 nodes/edges 計算（測試 mock 可能未提供，需防禦）
const nodeCount = computed(() => flowNodesRef?.value?.length ?? 0)
const connectionCount = computed(() => flowEdgesRef?.value?.length ?? 0)
const triggerCount = computed(
  () =>
    (flowNodesRef?.value ?? []).filter(
      (n) => (n.data as { type?: NodeType } | undefined)?.type === 'TRIGGER',
    ).length,
)

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
  return (type && getNodeTypeMeta(type)?.color) || '#909399'
}

function currentId(): string | undefined {
  const idParam = route.params.id
  return Array.isArray(idParam) ? idParam[0] : idParam
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
  }
})

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
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnloadHandler))

onConnect((connection: Connection) => {
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
    data: { name: meta?.label ?? type, type, config: {} },
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

// 整理版面：以 dagre 重排節點位置，更新畫布並置中檢視
function handleTidyUp() {
  const obj = toObject()
  const laidOut = layoutGraph(
    obj.nodes as unknown as FlowNode[],
    obj.edges as unknown as FlowEdge[],
  )
  setNodes(laidOut as unknown as Node[])
  store.setDirty(true)
  nextTick(() => fitView())
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
  <div class="wf-editor editor-layout">
    <!-- 頂部工具列 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="logo-box" aria-hidden="true">⚡</div>
        <span class="breadcrumb">Personal <span class="breadcrumb-sep">/</span></span>
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
        <button type="button" class="btn primary" data-test="save-button" @click="handleSave">
          存檔
        </button>
      </div>
    </div>

    <div class="editor-body">
      <NodePalette />

      <div class="canvas" @drop="onDrop" @dragover="onDragOver">
        <VueFlow
          :node-types="nodeTypes"
          :default-edge-options="EDGE_DEFAULTS"
          delete-key-code="Delete"
          fit-view-on-init
        >
          <Background variant="dots" :gap="20" :size="1.4" color="#2b2b34" />
          <MiniMap :node-color="minimapNodeColor" pannable zoomable />
          <Controls position="bottom-left" />
        </VueFlow>
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
          @update:node-name="onNodeNameUpdate"
          @update:node-config="onNodeConfigUpdate"
          @update:workflow-name="onWorkflowNameUpdate"
          @update:workflow-description="onWorkflowDescriptionUpdate"
          @duplicate-node="onDuplicateNode"
          @delete-node="onDeleteNode"
          @config-validity="configValid = $event"
        />
      </div>
    </div>
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
  background: var(--wf-surface-2);
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

.breadcrumb {
  font-size: 12.5px;
  color: var(--wf-text-dim);
  white-space: nowrap;
}

.breadcrumb-sep {
  color: var(--wf-text-mute);
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
  color: var(--wf-text-mute);
  white-space: nowrap;
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
  color: var(--wf-text-dim);
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
  color: var(--wf-text-mute);
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
  color: var(--wf-text-dim);
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
  border: 1px solid var(--wf-border-2);
}

.btn.ghost:hover {
  border-color: var(--wf-text-mute);
}

.btn.primary {
  color: #fff;
  background: var(--wf-accent);
  border: 1px solid var(--wf-accent);
}

.btn.primary:hover {
  filter: brightness(1.08);
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

.inspector {
  width: 322px;
  flex-shrink: 0;
  border-left: 1px solid var(--wf-border);
  background: var(--wf-surface);
  overflow-y: auto;
}

/* ---- 連線：深色預設，hover / 選取轉 accent ---- */
.canvas :deep(.vue-flow__edge-path) {
  stroke: var(--wf-edge);
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

/* ---- Controls 深色覆寫 ---- */
.canvas :deep(.vue-flow__controls) {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.4);
  border-radius: 10px;
  overflow: hidden;
}

.canvas :deep(.vue-flow__controls-button) {
  background: var(--wf-surface-2);
  border-bottom: 1px solid var(--wf-border);
  fill: var(--wf-text-dim);
}

.canvas :deep(.vue-flow__controls-button:hover) {
  background: var(--wf-card);
  fill: var(--wf-text);
}

/* ---- MiniMap 深色覆寫 ---- */
.canvas :deep(.vue-flow__minimap) {
  background: var(--wf-surface-2);
  border: 1px solid var(--wf-border);
  border-radius: 10px;
}

.canvas :deep(.vue-flow__minimap-mask) {
  fill: rgba(19, 19, 22, 0.55);
}
</style>
