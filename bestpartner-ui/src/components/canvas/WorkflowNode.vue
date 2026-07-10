<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { getNodeTypeMeta } from '../../constants/nodeTypes'
import { useExecutionStore } from '../../stores/execution'
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

const executionStore = useExecutionStore()

/** 依 execution store 的執行狀態（以 nodeKey = props.id 查詢）掛上對應色彩 class */
const execClass = computed(() => {
  const status = executionStore.nodeStates[props.id]?.status
  return status ? `is-exec-${status.toLowerCase()}` : ''
})

const meta = computed(() => getNodeTypeMeta(props.data.type))
const label = computed(() => meta.value?.label ?? props.data.type)
const displayName = computed(() => props.data.name?.trim() || label.value)
const color = computed(() => meta.value?.color ?? '#909399')
const icon = computed(() => meta.value?.icon ?? '⬜')
const isTrigger = computed(() => meta.value?.isTrigger ?? false)

/** hex 色碼轉 rgba（供選取外環半透明色） */
function hexToRgba(hex: string, alpha: number): string {
  const value = hex.replace('#', '')
  const r = parseInt(value.slice(0, 2), 16)
  const g = parseInt(value.slice(2, 4), 16)
  const b = parseInt(value.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/** 型別色以 CSS 變數下放：選取外環／輸出埠邊色／icon box 皆取用 */
const nodeStyle = computed(() => ({
  '--node-color': color.value,
  '--node-ring': hexToRgba(color.value, 0.55),
  '--node-tint': hexToRgba(color.value, 0.16),
}))

/** 副標（IBM Plex Mono）：config 首個原始值，否則型別小寫 */
const subtitle = computed(() => {
  const config = props.data.config ?? {}
  const first = Object.values(config).find(
    (v) => (typeof v === 'string' && v.trim() !== '') || typeof v === 'number',
  )
  if (first !== undefined) return String(first)
  return props.data.type.toLowerCase()
})

// 埠依 meta 資料驅動渲染。單埠節點退回預設 in:main / out:main。
const inputs = computed(() => meta.value?.inputs ?? [{ id: 'in:main' }])
const outputs = computed(() => meta.value?.outputs ?? [{ id: 'out:main' }])

/** 多埠時沿該側邊緣平均分布：第 index 個埠（共 total 個）的垂直位置（%） */
function portTop(index: number, total: number): string {
  return `${((index + 1) / (total + 1)) * 100}%`
}
</script>

<template>
  <div
    class="workflow-node"
    :class="[{ 'is-trigger': isTrigger }, execClass]"
    :style="nodeStyle"
  >
    <!-- 輸入埠（左緣）；TRIGGER 的 inputs 為空故不渲染 -->
    <Handle
      v-for="(port, i) in inputs"
      :key="port.id"
      :id="port.id"
      type="target"
      :position="Position.Left"
      :style="{ top: portTop(i, inputs.length) }"
    />

    <div class="node-icon-box">{{ icon }}</div>
    <div class="node-info">
      <div class="node-type-label">{{ label }}</div>
      <div class="node-name">{{ displayName }}</div>
      <div class="node-subtitle" data-test="node-subtitle">{{ subtitle }}</div>
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
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 170px;
  max-width: 240px;
  padding: 10px 14px 10px 11px;
  border: 1.5px solid var(--wf-border-2, #31313c);
  border-radius: 14px;
  background: var(--wf-card, #1e1e26);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
  font-size: 13px;
  transition: border-color 0.12s, box-shadow 0.12s;
}

/* 觸發節點以型別色淡淡標示（未選取時） */
.workflow-node.is-trigger {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35), 0 0 0 1px var(--node-tint);
}

.node-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  font-size: 17px;
  background: var(--node-tint, rgba(144, 147, 153, 0.16));
  border: 1px solid var(--node-color, #909399);
  border-radius: 10px;
}

.node-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-type-label {
  font-size: 9.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--node-color, #909399);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-name {
  font-size: 13.5px;
  font-weight: 700;
  color: #eff0f4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-subtitle {
  font-family: var(--wf-font-mono, ui-monospace, monospace);
  font-size: 10.5px;
  color: var(--wf-text-dim, #8a8a95);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 多輸出埠的文字標示，貼齊右緣埠旁 */
.port-label {
  position: absolute;
  right: 10px;
  transform: translateY(-50%);
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--wf-text-dim, #8a8a95);
  pointer-events: none;
  white-space: nowrap;
}
</style>
