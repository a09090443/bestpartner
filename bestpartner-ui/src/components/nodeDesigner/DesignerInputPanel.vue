<script setup lang="ts">
import { computed } from 'vue'
import { IN_TOOL } from '../../constants/handles'
import { getNodeTypeMeta } from '../../constants/nodeTypes'
import type { NodeType } from '../../types/workflow'
import type { UpstreamSource } from '../../types/nodeDesigner'

const props = defineProps<{
  type: NodeType
  sources: UpstreamSource[]
}>()

const meta = computed(() => getNodeTypeMeta(props.type))

/** 本節點是否根本沒有輸入埠（TRIGGER / SKILL） */
const hasNoInputPorts = computed(() => (meta.value?.inputs.length ?? 0) === 0)

/**
 * in:tool 是能力掛載埠、不是資料流（見 constants/handles.ts），
 * 故與資料輸入分開呈現：能力來源只列節點名，不顯示 JSON。
 */
const dataSources = computed(() => props.sources.filter((s) => s.targetHandle !== IN_TOOL))
const capabilitySources = computed(() => props.sources.filter((s) => s.targetHandle === IN_TOOL))

/** 已連上游、但都還沒有任何執行輸出 */
const notExecuted = computed(
  () => dataSources.value.length > 0 && dataSources.value.every((s) => s.output === undefined),
)

/** 無輸入埠時的說明文字，依型別給出「為什麼沒有輸入」的具體原因 */
const noPortHint = computed(() =>
  props.type === 'SKILL'
    ? '這是能力提供節點，它只作為 LLM 助手的工具來源，沒有輸入資料。'
    : '這是觸發節點，它啟動整個流程，因此沒有輸入資料。',
)

function formatJson(value: unknown): string {
  return JSON.stringify(value, null, 2)
}
</script>

<template>
  <section class="designer-pane" data-test="node-designer-input-panel">
    <header class="pane-header">
      <span class="pane-title">輸入</span>
      <span class="pane-note">最近一次執行的資料</span>
    </header>

    <!-- ① 無輸入埠 -->
    <div v-if="hasNoInputPorts" class="empty dashed" data-test="node-designer-input-empty">
      {{ noPortHint }}
    </div>

    <!-- ② 有埠但沒有上游 -->
    <div
      v-else-if="dataSources.length === 0 && capabilitySources.length === 0"
      class="empty"
      data-test="node-designer-input-empty"
    >
      尚未連接上游節點。
    </div>

    <template v-else>
      <!-- ③ 有上游但尚未執行 -->
      <div v-if="notExecuted" class="empty" data-test="node-designer-input-empty">
        尚未執行。執行流程後可在此檢視實際輸入資料。
      </div>

      <!-- ④ 有輸出 -->
      <div
        v-for="source in dataSources.filter((s) => s.output !== undefined)"
        :key="source.nodeId + source.targetHandle"
        class="source-block"
      >
        <div class="source-name">來自 {{ source.name }}</div>
        <pre class="json" data-test="node-designer-input-json">{{ formatJson(source.output) }}</pre>
      </div>

      <div v-if="capabilitySources.length > 0" class="capability-block">
        <div class="pane-subtitle">已掛載能力</div>
        <div class="chips" data-test="node-designer-capability-list">
          <span v-for="source in capabilitySources" :key="source.nodeId" class="chip">
            {{ source.name }}
          </span>
        </div>
        <p class="capability-hint">能力節點不參與資料流，由 LLM 自主決定何時呼叫。</p>
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
  border-right: 1px solid var(--wf-border);
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

.pane-note {
  font-size: 10.5px;
  color: var(--wf-text-3);
}

.pane-subtitle {
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--wf-text-3);
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

.source-block {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.source-name {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--wf-text-2);
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

.capability-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px solid var(--wf-border);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  padding: 3px 9px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--wf-text);
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 999px;
}

.capability-hint {
  margin: 0;
  font-size: 11px;
  line-height: 1.5;
  color: var(--wf-text-3);
}
</style>
