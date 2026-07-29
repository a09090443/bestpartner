<script setup lang="ts">
/**
 * Node Designer 中欄：Parameters / Settings / Docs 三個分頁。
 *
 * Parameters 與 Inspector 共用同一份分派表（inspector/typedForms.ts），
 * 因此兩處編輯的是同一份 node.data.config，不會有兩套行為。
 */
import { computed, ref } from 'vue'
import JsonConfigEditor from '../inspector/JsonConfigEditor.vue'
import { resolveTypedForm, buildTypedFormProps } from '../inspector/typedForms'
import { getNodeTypeMeta } from '../../constants/nodeTypes'
import { getNodeDoc } from '../../constants/nodeDocs'
import { missingRequiredForNode, formatMissingFields } from '../../utils/nodeRequiredFields'
import type { NodeType } from '../../types/workflow'
import type { NodeRequiredFields } from '../../api/workflow'
import type { NodeRunState } from '../../stores/execution'

const props = defineProps<{
  nodeId: string
  type: NodeType
  config: Record<string, unknown>
  requiredFields?: NodeRequiredFields
  promptBoundNodeKeys?: string[]
  run?: NodeRunState
}>()

const emit = defineEmits<{
  'update:config': [config: Record<string, unknown>]
  'config-validity': [valid: boolean]
  'duplicate-node': []
  'delete-node': []
}>()

type TabKey = 'parameters' | 'settings' | 'docs'
const TABS: { key: TabKey; label: string }[] = [
  { key: 'parameters', label: 'Parameters' },
  { key: 'settings', label: 'Settings' },
  { key: 'docs', label: 'Docs' },
]

const activeTab = ref<TabKey>('parameters')

const meta = computed(() => getNodeTypeMeta(props.type))
const doc = computed(() => getNodeDoc(props.type))

const typedForm = computed(() => resolveTypedForm(props.type))
const typedFormProps = computed(() =>
  buildTypedFormProps({
    type: props.type,
    nodeId: props.nodeId,
    config: props.config,
    promptBoundNodeKeys: props.promptBoundNodeKeys,
  }),
)

/** 必填缺漏（與 Inspector 同一套判斷，直接重用 utils） */
const missingText = computed(() => {
  if (!props.requiredFields) return ''
  return formatMissingFields(
    missingRequiredForNode(props.type, props.config, props.requiredFields),
  )
})

const copied = ref(false)

/** 複製 nodeKey：clipboard 不可用（非安全來源、測試環境）時靜默略過，不干擾編輯 */
async function copyNodeKey() {
  try {
    await navigator.clipboard?.writeText(props.nodeId)
    copied.value = true
    setTimeout(() => (copied.value = false), 1500)
  } catch {
    /* 忽略：複製失敗不影響節點編輯 */
  }
}
</script>

<template>
  <section class="designer-pane">
    <div class="tabs" role="tablist">
      <button
        v-for="tab in TABS"
        :key="tab.key"
        type="button"
        role="tab"
        class="tab"
        :class="{ active: activeTab === tab.key }"
        :aria-selected="activeTab === tab.key"
        :data-test="`node-designer-tab-${tab.key}`"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Parameters：與 Inspector 同一份表單、同一份 config -->
    <div v-if="activeTab === 'parameters'" class="tab-body" data-test="node-designer-parameters">
      <div v-if="missingText" class="warn">缺少必填欄位：{{ missingText }}</div>
      <component
        :is="typedForm"
        v-if="typedForm"
        v-bind="typedFormProps"
        @update:config="emit('update:config', $event)"
      />
      <JsonConfigEditor
        v-else
        :model-value="props.config"
        @update:model-value="emit('update:config', $event)"
        @validity-change="emit('config-validity', $event)"
      />
    </div>

    <!-- Settings：唯讀 metadata + 本次執行摘要 + 節點操作（本專案無 per-node 設定資料模型） -->
    <div v-else-if="activeTab === 'settings'" class="tab-body" data-test="node-designer-settings">
      <div class="block">
        <div class="block-title">節點資訊</div>
        <div class="meta-row">
          <span class="meta-label">節點 key</span>
          <code class="meta-value">{{ props.nodeId }}</code>
          <button
            type="button"
            class="mini-btn"
            data-test="node-designer-copy-key"
            @click="copyNodeKey"
          >
            {{ copied ? '已複製' : '複製' }}
          </button>
        </div>
        <div class="meta-row">
          <span class="meta-label">型別</span>
          <span class="meta-value">{{ meta?.label }}（{{ props.type }}）</span>
        </div>
        <div class="meta-row">
          <span class="meta-label">分類</span>
          <span class="meta-value">{{ meta?.category }}</span>
        </div>
      </div>

      <div class="block">
        <div class="block-title">必填檢核</div>
        <p v-if="missingText" class="warn">缺少必填欄位：{{ missingText }}</p>
        <p v-else class="ok">必填欄位皆已填寫。</p>
      </div>

      <div class="block">
        <div class="block-title">本次執行</div>
        <p v-if="!props.run" class="hint">尚未執行。</p>
        <template v-else>
          <p class="hint">
            狀態：{{ props.run.status
            }}<span v-if="props.run.durationMs != null">（{{ props.run.durationMs }} ms）</span>
          </p>
          <p v-if="props.run.error" class="warn">{{ props.run.error }}</p>
        </template>
      </div>

      <div class="actions">
        <button
          type="button"
          class="action-btn"
          data-test="node-designer-duplicate"
          @click="emit('duplicate-node')"
        >
          複製節點
        </button>
        <button
          type="button"
          class="action-btn danger"
          data-test="node-designer-delete"
          @click="emit('delete-node')"
        >
          刪除節點
        </button>
      </div>
    </div>

    <!-- Docs：說明文字寫在 constants/nodeDocs.ts；連接埠清單直接讀 meta，零維護 -->
    <div v-else class="tab-body" data-test="node-designer-docs">
      <p class="summary">{{ doc.summary }}</p>

      <div class="block">
        <div class="block-title">行為要點</div>
        <ul class="points">
          <li v-for="point in doc.points" :key="point">{{ point }}</li>
        </ul>
      </div>

      <div class="block">
        <div class="block-title">連接埠</div>
        <div class="port-group">
          <span class="port-role">輸入</span>
          <div v-if="(meta?.inputs.length ?? 0) === 0" class="port-none">無</div>
          <div v-else class="chips">
            <span v-for="port in meta?.inputs" :key="port.id" class="chip">
              {{ port.label ?? '輸入' }} · <code>{{ port.id }}</code>
            </span>
          </div>
        </div>
        <div class="port-group">
          <span class="port-role">輸出</span>
          <div v-if="(meta?.outputs.length ?? 0) === 0" class="port-none">無</div>
          <div v-else class="chips">
            <span v-for="port in meta?.outputs" :key="port.id" class="chip">
              {{ port.label ?? '輸出' }} · <code>{{ port.id }}</code>
            </span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.designer-pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--wf-panel);
  overflow: hidden;
}

.tabs {
  display: flex;
  gap: 2px;
  padding: 10px 14px 0;
  border-bottom: 1px solid var(--wf-border);
}

.tab {
  padding: 7px 13px;
  font-size: 12px;
  font-weight: 700;
  font-family: inherit;
  color: var(--wf-text-2);
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color 0.12s, border-color 0.12s;
}

.tab:hover {
  color: var(--wf-text);
}

.tab.active {
  color: var(--wf-text);
  border-bottom-color: var(--wf-accent);
}

.tab-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 14px;
  overflow-y: auto;
}

.block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.block-title {
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--wf-text-3);
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.meta-label {
  width: 60px;
  flex-shrink: 0;
  color: var(--wf-text-3);
}

.meta-value {
  flex: 1;
  min-width: 0;
  color: var(--wf-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

code.meta-value {
  font-family: var(--wf-font-mono);
  font-size: 11.5px;
}

.mini-btn {
  flex-shrink: 0;
  padding: 3px 9px;
  font-size: 11px;
  font-weight: 700;
  font-family: inherit;
  color: var(--wf-text-2);
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 7px;
  cursor: pointer;
}

.mini-btn:hover {
  color: var(--wf-text);
  border-color: var(--wf-text-3);
}

.summary,
.hint,
.ok {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--wf-text-2);
}

.ok {
  color: var(--wf-success);
}

.warn {
  margin: 0;
  padding: 8px 10px;
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--wf-warning);
  background: var(--wf-warning-bg);
  border: 1px solid var(--wf-warning-border);
  border-radius: 8px;
}

.points {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--wf-text-2);
}

.port-group {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.port-role {
  width: 32px;
  flex-shrink: 0;
  padding-top: 3px;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--wf-text-3);
}

.port-none {
  font-size: 12px;
  color: var(--wf-text-3);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  padding: 3px 9px;
  font-size: 11.5px;
  color: var(--wf-text-2);
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 999px;
}

.chip code {
  font-family: var(--wf-font-mono);
  font-size: 10.5px;
  color: var(--wf-text-3);
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--wf-border);
}

.action-btn {
  flex: 1;
  padding: 8px 0;
  font-size: 12px;
  font-weight: 700;
  font-family: inherit;
  color: var(--wf-text);
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.12s;
}

.action-btn:hover {
  border-color: var(--wf-text-3);
}

.action-btn.danger {
  flex: 0 0 auto;
  padding: 8px 16px;
  color: var(--wf-danger);
  background: var(--wf-danger-bg);
  border-color: var(--wf-danger-border);
}

.action-btn.danger:hover {
  border-color: var(--wf-danger);
}
</style>
