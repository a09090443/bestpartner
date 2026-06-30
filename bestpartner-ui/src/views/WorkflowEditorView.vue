<script setup lang="ts">
import { markRaw, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import type { Connection, Node, NodeTypesObject } from '@vue-flow/core'
import { ElMessage, ElMessageBox } from 'element-plus'
import NodePalette from '../components/canvas/NodePalette.vue'
import WorkflowNode from '../components/canvas/WorkflowNode.vue'
import InspectorPanel from '../components/inspector/InspectorPanel.vue'
import { DRAG_NODE_TYPE_KEY } from '../components/canvas/dragKeys'
import { generateNodeKey } from '../composables/useNodeKey'
import { getNodeTypeMeta } from '../constants/nodeTypes'
import { useWorkflowStore, WorkflowVersionConflictError } from '../stores/workflow'
import type { FlowNode, FlowEdge } from '../composables/useWorkflowSync'
import type { NodeType } from '../types/workflow'

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
  screenToFlowCoordinate,
  setNodes,
  setEdges,
  toObject,
} = useVueFlow()

const selectedNode = ref<FlowNode | null>(null)
const configValid = ref(true)

function currentId(): string | undefined {
  const idParam = route.params.id
  return Array.isArray(idParam) ? idParam[0] : idParam
}

async function loadIntoCanvas(id: string) {
  await store.load(id)
  if (store.current) {
    setNodes(store.current.nodes as unknown as Node[])
    setEdges(store.current.edges)
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

// 畫布變更 → 標記未存
onNodesChange(() => store.setDirty(true))
onEdgesChange(() => store.setDirty(true))

onConnect((connection: Connection) => {
  addEdges([{ ...connection, id: `e-${connection.source}-${connection.target}` }])
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

// Inspector：流程 meta 編輯
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

async function handleSave() {
  if (!configValid.value) {
    ElMessage.error('節點設定 JSON 格式錯誤，請修正後再存檔')
    return
  }
  const { nodes, edges } = toObject()
  try {
    await store.save(nodes as unknown as FlowNode[], edges as unknown as FlowEdge[])
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
    const message = err instanceof Error ? err.message : '存檔失敗'
    ElMessage.error(message)
  }
}
</script>

<template>
  <div class="editor-layout">
    <NodePalette />

    <div class="center">
      <div class="toolbar">
        <span class="wf-name">{{ store.current?.name ?? '未命名流程' }}</span>
        <span class="wf-meta">
          <el-tag size="small">{{ store.current?.status ?? 'DRAFT' }}</el-tag>
          <span class="version">v{{ store.current?.version ?? '-' }}</span>
          <el-tag v-if="store.dirty" size="small" type="warning">未存</el-tag>
        </span>
        <el-button type="primary" data-test="save-button" @click="handleSave">存檔</el-button>
      </div>

      <div class="canvas" @drop="onDrop" @dragover="onDragOver">
        <VueFlow :node-types="nodeTypes" delete-key-code="Delete" fit-view-on-init />
      </div>
    </div>

    <div class="inspector">
      <InspectorPanel
        :selected-node="selectedNode"
        :workflow-name="store.current?.name ?? ''"
        :workflow-description="store.current?.description"
        @update:node-name="onNodeNameUpdate"
        @update:node-config="onNodeConfigUpdate"
        @update:workflow-name="onWorkflowNameUpdate"
        @update:workflow-description="onWorkflowDescriptionUpdate"
        @config-validity="configValid = $event"
      />
    </div>
  </div>
</template>

<style scoped>
.editor-layout {
  display: flex;
  height: 100vh;
  width: 100%;
}

.center {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid #e4e7ed;
}

.wf-name {
  font-weight: 600;
}

.wf-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  color: #909399;
  font-size: 13px;
}

.canvas {
  flex: 1;
  position: relative;
}

.inspector {
  width: 280px;
  border-left: 1px solid #e4e7ed;
  background: #fafafa;
  padding: 12px;
  overflow-y: auto;
}
</style>
