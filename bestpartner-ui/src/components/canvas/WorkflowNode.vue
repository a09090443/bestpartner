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
</script>

<template>
  <div class="workflow-node" :class="{ 'is-trigger': isTrigger }" :style="{ borderColor: color }">
    <!-- 觸發節點無輸入 handle -->
    <Handle v-if="!isTrigger" type="target" :position="Position.Left" />

    <div class="node-header" :style="{ backgroundColor: color }">
      <span class="node-icon">{{ icon }}</span>
      <span class="node-type-label">{{ label }}</span>
    </div>
    <div class="node-body">
      <span class="node-name">{{ displayName }}</span>
    </div>

    <Handle type="source" :position="Position.Right" />
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
</style>
