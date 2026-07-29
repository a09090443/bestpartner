<script setup lang="ts">
import { computed, inject } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import NodeIcon from '../common/NodeIcon.vue'
import { OPEN_DESIGNER_KEY } from './designerInjection'
import { NODE_FALLBACK_COLOR, getNodeTypeMeta } from '../../constants/nodeTypes'
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

// 預設 no-op：單獨 mount 節點的測試不必補 provide（見 designerInjection.ts）
const openDesigner = inject(OPEN_DESIGNER_KEY, () => {})

const execStatus = computed(() => executionStore.nodeStates[props.id]?.status)

/** 依 execution store 的執行狀態（以 nodeKey = props.id 查詢）掛上對應色彩 class */
const execClass = computed(() =>
  execStatus.value ? `is-exec-${execStatus.value.toLowerCase()}` : '',
)

/** 執行成功的節點顯示勾勾徽章（設計稿以形狀而非顏色區分狀態，避免與型別色混淆） */
const isDone = computed(() => execStatus.value === 'SUCCESS')

const meta = computed(() => getNodeTypeMeta(props.data.type))
const label = computed(() => meta.value?.label ?? props.data.type)
const displayName = computed(() => props.data.name?.trim() || label.value)
const color = computed(() => meta.value?.color ?? NODE_FALLBACK_COLOR)
const isTrigger = computed(() => meta.value?.isTrigger ?? false)

/**
 * 型別色以 CSS 變數下放：選取外環／輸出埠邊色／icon box 皆取用。
 * 衍生色（--node-tint / --node-ring / 前景色）改由 CSS color-mix 就地計算，
 * 因為它們需隨主題調整，JS 端拿不到當下主題。
 * ⚠️ `--node-color` 鍵名有測試依賴（WorkflowNode.test.ts 斷言其值），勿更名。
 */
const nodeStyle = computed(() => ({
  '--node-color': color.value,
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
    :data-node-type="data.type"
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

    <div class="node-icon-box"><NodeIcon :type="data.type" :size="18" /></div>
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

    <!-- 開啟全屏節點編輯頁；.stop 同時擋掉選取與 Vue Flow 的拖曳起手式 -->
    <button
      type="button"
      class="node-open-btn"
      data-test="node-open-designer"
      aria-label="開啟節點編輯頁"
      title="開啟節點編輯頁"
      @click.stop="openDesigner(props.id)"
      @mousedown.stop
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2.2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path d="M15 3h6v6M21 3l-9 9M10 5H6a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-4" />
      </svg>
    </button>

    <!-- 執行完成徽章 -->
    <span v-if="isDone" class="node-done-badge" data-test="node-done-badge" aria-label="執行成功">
      <svg
        width="12"
        height="12"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="3"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path d="M20 6 9 17l-5-5" />
      </svg>
    </span>
  </div>
</template>

<style scoped>
.workflow-node {
  /* 型別色的衍生色：在此就地計算，才能跟著主題（--node-fg-mix）變動。
     --node-ring 供 workflow-theme.css 的 .vue-flow__node.selected 規則取用。 */
  --node-tint: color-mix(in srgb, var(--node-color) 13%, transparent);
  --node-ring: color-mix(in srgb, var(--node-color) 35%, transparent);
  --node-fg: color-mix(in srgb, var(--node-color) var(--node-fg-mix), #000);

  /* 固定尺寸，與 constants/canvas.ts 的 NODE_BOX 對齊——dagre 自動排版與預設縮放都依賴這組數字，
     改動時三處必須同步（否則整理版面會重疊、預設視野會失準）。 */
  position: relative;
  box-sizing: border-box;
  width: 214px;
  min-height: 76px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 13px;
  border: 1.5px solid var(--wf-border);
  border-radius: 14px;
  background: var(--wf-card);
  box-shadow: var(--wf-shadow);
  font-size: 13px;
  transition: border-color 0.12s, box-shadow 0.12s;
}

/* 觸發節點以型別色淡淡標示（未選取時） */
.workflow-node.is-trigger {
  box-shadow: var(--wf-shadow), 0 0 0 1px var(--node-tint);
}

.node-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  color: var(--node-fg);
  background: var(--node-tint);
  border: 1px solid color-mix(in srgb, var(--node-color) 28%, transparent);
  border-radius: 10px;
}

.node-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  /* 讓出右上角開啟鈕的位置，避免長名稱壓到按鈕下方 */
  padding-right: 20px;
}

/* 右上角開啟鈕：未選取時淡化，hover / 選取時全亮 */
.node-open-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  color: var(--wf-text-3);
  background: var(--wf-card-2);
  border: 1px solid var(--wf-border);
  border-radius: 7px;
  opacity: 0.55;
  cursor: pointer;
  transition: opacity 0.12s, color 0.12s;
}

.node-open-btn svg {
  width: 11px;
  height: 11px;
}

.workflow-node:hover .node-open-btn,
.node-open-btn:hover {
  opacity: 1;
  color: var(--wf-text);
}

.node-type-label {
  font-size: 9.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--node-fg);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-name {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--wf-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-subtitle {
  font-family: var(--wf-font-mono);
  font-size: 10.5px;
  color: var(--wf-text-3);
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
  color: var(--wf-text-3);
  pointer-events: none;
  white-space: nowrap;
}

/* 執行完成徽章：綠色圓形勾勾。放左上角而非右上角——右上角已被開啟鈕佔用，
   兩者相距不足會疊在一起。 */
.node-done-badge {
  position: absolute;
  top: -7px;
  left: -7px;
  z-index: 4;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #fff;
  background: var(--wf-success);
  border-radius: 50%;
  box-shadow: 0 2px 8px color-mix(in srgb, var(--wf-success) 45%, transparent);
  pointer-events: none;
}
</style>
