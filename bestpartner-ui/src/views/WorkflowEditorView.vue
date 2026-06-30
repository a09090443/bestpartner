<script setup lang="ts">
import { markRaw } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import type { Connection, Node, NodeTypesObject } from '@vue-flow/core'
import NodePalette from '../components/canvas/NodePalette.vue'
import WorkflowNode from '../components/canvas/WorkflowNode.vue'
import { DRAG_NODE_TYPE_KEY } from '../components/canvas/dragKeys'
import { generateNodeKey } from '../composables/useNodeKey'
import { getNodeTypeMeta } from '../constants/nodeTypes'
import type { NodeType } from '../types/workflow'

// 單一 Vue Flow 自訂節點型別，語意型別放於 node.data.type
const nodeTypes = { workflow: markRaw(WorkflowNode) } as unknown as NodeTypesObject

const {
  onConnect,
  addEdges,
  addNodes,
  screenToFlowCoordinate,
} = useVueFlow()

// 連線：使用者拉線時加入 edge
onConnect((connection: Connection) => {
  addEdges([{ ...connection, id: `e-${connection.source}-${connection.target}` }])
})

function onDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

// 從 NodePalette 拖放：依放下座標建立新節點
function onDrop(event: DragEvent) {
  event.preventDefault()
  const type = event.dataTransfer?.getData(DRAG_NODE_TYPE_KEY) as NodeType | undefined
  if (!type) return

  const position = screenToFlowCoordinate({ x: event.clientX, y: event.clientY })
  const meta = getNodeTypeMeta(type)
  const nodeKey = generateNodeKey()

  const newNode: Node = {
    id: nodeKey,
    type: 'workflow',
    position,
    data: { name: meta?.label ?? type, type, config: {} },
  }
  addNodes([newNode])
}
</script>

<template>
  <div class="editor-layout">
    <NodePalette />

    <div class="canvas" @drop="onDrop" @dragover="onDragOver">
      <VueFlow :node-types="nodeTypes" delete-key-code="Delete" fit-view-on-init />
    </div>

    <div class="inspector">
      <div class="inspector-title">屬性</div>
      <div class="inspector-empty">選取節點以編輯（M5）</div>
    </div>
  </div>
</template>

<style scoped>
.editor-layout {
  display: flex;
  height: 100vh;
  width: 100%;
}

.canvas {
  flex: 1;
  height: 100%;
  position: relative;
}

.inspector {
  width: 280px;
  border-left: 1px solid #e4e7ed;
  background: #fafafa;
  padding: 12px;
}

.inspector-title {
  font-weight: 600;
  color: #909399;
  font-size: 12px;
  margin-bottom: 8px;
}

.inspector-empty {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
