<script setup lang="ts">
import { NODE_TYPE_METAS } from '../../constants/nodeTypes'
import type { NodeType } from '../../types/workflow'
import { DRAG_NODE_TYPE_KEY } from './dragKeys'

function handleDragStart(event: DragEvent, type: NodeType) {
  if (!event.dataTransfer) return
  event.dataTransfer.setData(DRAG_NODE_TYPE_KEY, type)
  event.dataTransfer.effectAllowed = 'move'
}
</script>

<template>
  <div class="node-palette">
    <div class="palette-title">節點</div>
    <div
      v-for="meta in NODE_TYPE_METAS"
      :key="meta.type"
      class="palette-item"
      :data-test="`palette-item-${meta.type}`"
      draggable="true"
      @dragstart="handleDragStart($event, meta.type)"
    >
      <span class="palette-icon" :style="{ color: meta.color }">{{ meta.icon }}</span>
      <span class="palette-label">{{ meta.label }}</span>
    </div>
  </div>
</template>

<style scoped>
.node-palette {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  width: 160px;
  border-right: 1px solid #e4e7ed;
  background: #fafafa;
  overflow-y: auto;
}

.palette-title {
  font-weight: 600;
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fff;
  cursor: grab;
  user-select: none;
}

.palette-item:hover {
  border-color: #c0c4cc;
}

.palette-icon {
  font-size: 16px;
}

.palette-label {
  font-size: 13px;
  color: #303133;
}
</style>
