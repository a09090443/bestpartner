<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { getNodeTypeMeta } from '../../constants/nodeTypes'
import type { NodeType } from '../../types/workflow'

interface NodeData {
  name?: string
  type: NodeType
  config?: Record<string, unknown>
}

const props = defineProps<{
  id: string
  data: NodeData
}>()

const meta = computed(() => getNodeTypeMeta(props.data.type))
const label = computed(() => meta.value?.label ?? props.data.type)
const displayName = computed(() => props.data.name?.trim() || label.value)
const color = computed(() => meta.value?.color ?? '#909399')
const icon = computed(() => meta.value?.icon ?? '⬜')
const isTrigger = computed(() => meta.value?.isTrigger ?? false)

// 埠依 meta 資料驅動渲染。單埠節點退回預設 in:main / out:main。
const inputs = computed(() => meta.value?.inputs ?? [{ id: 'in:main' }])
const outputs = computed(() => meta.value?.outputs ?? [{ id: 'out:main' }])

/** 多埠時沿該側邊緣平均分布：第 index 個埠（共 total 個）的垂直位置（%） */
function portTop(index: number, total: number): string {
  return `${((index + 1) / (total + 1)) * 100}%`
}
</script>

<template>
  <div class="workflow-node" :class="{ 'is-trigger': isTrigger }" :style="{ borderColor: color }">
    <!-- 輸入埠（左緣）；TRIGGER 的 inputs 為空故不渲染 -->
    <Handle
      v-for="(port, i) in inputs"
      :key="port.id"
      :id="port.id"
      type="target"
      :position="Position.Left"
      :style="{ top: portTop(i, inputs.length) }"
    />

    <div class="node-header" :style="{ backgroundColor: color }">
      <span class="node-icon">{{ icon }}</span>
      <span class="node-type-label">{{ label }}</span>
    </div>
    <div class="node-body">
      <span class="node-name">{{ displayName }}</span>
    </div>

    <!-- 輸出埠（右緣）；CONDITION / LOOP 為多埠並顯示 label -->
    <template v-for="(port, i) in outputs" :key="port.id">
      <Handle
        :id="port.id"
        type="source"
        :position="Position.Right"
        :style="{ top: portTop(i, outputs.length) }"
      />
      <span
        v-if="port.label"
        class="port-label"
        :style="{ top: portTop(i, outputs.length) }"
      >{{ port.label }}</span>
    </template>
  </div>
</template>

<style scoped>
.workflow-node {
  min-width: 140px;
  border: 2px solid #909399;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
  font-size: 13px;
}

.workflow-node.is-trigger {
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.25);
}

.node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  color: #fff;
  font-weight: 600;
}

.node-icon {
  font-size: 14px;
}

.node-body {
  padding: 8px;
}

.node-name {
  color: #303133;
  word-break: break-all;
}

/* 多輸出埠的文字標示，貼齊右緣埠旁 */
.port-label {
  position: absolute;
  right: 8px;
  transform: translateY(-50%);
  font-size: 10px;
  color: #909399;
  pointer-events: none;
  white-space: nowrap;
}
</style>
