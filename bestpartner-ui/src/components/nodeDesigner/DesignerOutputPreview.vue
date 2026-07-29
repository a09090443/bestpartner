<script setup lang="ts">
/**
 * 節點輸出的「結構預覽」：把最外層的鍵與其值型別列成表，
 * 讓使用者不必讀完整段 JSON 就知道下游能引用哪些欄位。
 */
import { computed } from 'vue'

const props = defineProps<{
  /** 節點輸出；undefined 代表尚未執行 */
  value?: unknown
  /** 節點 key，用於組出可複製的 {{nodeKey.field}} 引用字串 */
  nodeId: string
}>()

interface FieldRow {
  key: string
  kind: string
  preview: string
  /** 下游可直接貼用的插值字串；在 script 內組好，避免模板出現巢狀 {{ }} 導致編譯失敗 */
  ref: string
}

/** 值的型別標示：陣列標出長度，物件標出鍵數，其餘為原始型別名 */
function kindOf(value: unknown): string {
  if (value === null) return 'null'
  if (Array.isArray(value)) return `array(${value.length})`
  if (typeof value === 'object') return `object(${Object.keys(value as object).length})`
  return typeof value
}

/** 單行摘要：字串過長截斷，物件/陣列以精簡 JSON 呈現 */
function previewOf(value: unknown): string {
  const text = typeof value === 'string' ? value : JSON.stringify(value)
  if (text === undefined) return ''
  return text.length > 60 ? `${text.slice(0, 60)}…` : text
}

/** 只展開最外層：輸出本身不是純物件時（陣列或純量）以單一 value 列表示 */
const rows = computed<FieldRow[]>(() => {
  const value = props.value
  if (value === undefined) return []
  const refOf = (key: string) => `{{${props.nodeId}.${key}}}`
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    return [{ key: 'value', kind: kindOf(value), preview: previewOf(value), ref: refOf('value') }]
  }
  return Object.entries(value as Record<string, unknown>).map(([key, v]) => ({
    key,
    kind: kindOf(v),
    preview: previewOf(v),
    ref: refOf(key),
  }))
})
</script>

<template>
  <div class="preview" data-test="node-designer-output-preview">
    <div v-for="row in rows" :key="row.key" class="row">
      <div class="row-head">
        <code class="ref">{{ row.ref }}</code>
        <span class="kind">{{ row.kind }}</span>
      </div>
      <div v-if="row.preview" class="row-preview">{{ row.preview }}</div>
    </div>
  </div>
</template>

<style scoped>
.preview {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.row {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 7px 9px;
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 9px;
}

.row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ref {
  font-family: var(--wf-font-mono);
  font-size: 11px;
  color: var(--wf-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kind {
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--wf-text-3);
}

.row-preview {
  font-size: 11.5px;
  color: var(--wf-text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
