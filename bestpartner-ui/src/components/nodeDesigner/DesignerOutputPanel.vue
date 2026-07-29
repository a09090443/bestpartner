<script setup lang="ts">
import { computed } from 'vue'
import DesignerOutputPreview from './DesignerOutputPreview.vue'
import { getNodeTypeMeta } from '../../constants/nodeTypes'
import type { NodeType } from '../../types/workflow'
import type { NodeRunState } from '../../stores/execution'

const props = defineProps<{
  nodeId: string
  type: NodeType
  /** 該節點最近一次執行的狀態；未執行為 undefined */
  run?: NodeRunState
}>()

const meta = computed(() => getNodeTypeMeta(props.type))

/**
 * 不會有輸出可看的兩種情形：
 * 1. 沒有輸出埠（OUTPUT，流程終點）
 * 2. SKILL —— 它**有** out:main 埠，但那是掛載到 LLM `in:tool` 的能力埠、不是資料流；
 *    後端沒有 SkillExecutor，此節點不落主遍歷、不產生執行紀錄。
 *    ⚠️ 只靠埠數判斷會漏掉 SKILL，讓 UI 錯誤顯示「執行流程後可在此檢視實際輸出資料」。
 */
const hasNoOutput = computed(() => (meta.value?.outputs.length ?? 0) === 0 || props.type === 'SKILL')

const output = computed(() => props.run?.output)

const statusText = computed(() => {
  if (!props.run) return null
  const duration = props.run.durationMs != null ? `（${props.run.durationMs} ms）` : ''
  return `${props.run.status}${duration}`
})

function formatJson(value: unknown): string {
  return JSON.stringify(value, null, 2)
}
</script>

<template>
  <section class="designer-pane" data-test="node-designer-output-panel">
    <header class="pane-header">
      <span class="pane-title">輸出</span>
      <span v-if="statusText" class="status" data-test="node-designer-output-status">
        {{ statusText }}
      </span>
      <span v-else class="pane-note">最近一次執行的資料</span>
    </header>

    <div v-if="props.run?.error" class="error-box" data-test="node-designer-output-error">
      {{ props.run.error }}
    </div>

    <div v-if="hasNoOutput" class="empty dashed" data-test="node-designer-output-empty">
      {{
        props.type === 'SKILL'
          ? '這是能力提供節點，它不會產生自己的輸出。'
          : '這是流程的終點，沒有輸出埠可再連接下游節點。'
      }}
    </div>

    <div v-else-if="output === undefined" class="empty" data-test="node-designer-output-empty">
      尚未執行。執行流程後可在此檢視實際輸出資料。
    </div>

    <template v-else>
      <div class="block">
        <div class="pane-subtitle">可引用欄位</div>
        <DesignerOutputPreview :value="output" :node-id="props.nodeId" />
      </div>

      <div class="block">
        <div class="pane-subtitle">原始 JSON</div>
        <pre class="json" data-test="node-designer-output-json">{{ formatJson(output) }}</pre>
      </div>
    </template>
  </section>
</template>

<style scoped>
.designer-pane {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  padding: 14px;
  background: var(--wf-panel-2);
  border-left: 1px solid var(--wf-border);
  overflow-y: auto;
}

.pane-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.pane-title {
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--wf-text-3);
}

.pane-note,
.status {
  font-size: 10.5px;
  color: var(--wf-text-3);
}

.status {
  font-family: var(--wf-font-mono);
}

.pane-subtitle {
  margin-bottom: 6px;
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--wf-text-3);
}

.block {
  min-width: 0;
}

.empty {
  padding: 14px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--wf-text-2);
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 10px;
}

.empty.dashed {
  background: transparent;
  border-style: dashed;
}

.error-box {
  padding: 9px 11px;
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--wf-danger);
  background: var(--wf-danger-bg);
  border: 1px solid var(--wf-danger-border);
  border-radius: 9px;
  word-break: break-word;
}

.json {
  margin: 0;
  padding: 10px;
  font-family: var(--wf-font-mono);
  font-size: 11.5px;
  line-height: 1.55;
  color: var(--wf-text);
  background: var(--wf-input);
  border: 1px solid var(--wf-border);
  border-radius: 10px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
