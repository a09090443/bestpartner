<script setup lang="ts">
import { computed, markRaw, watch } from 'vue'
import JsonConfigEditor from './JsonConfigEditor.vue'
import LlmAssistantForm from './forms/LlmAssistantForm.vue'
import ToolForm from './forms/ToolForm.vue'
import McpServerForm from './forms/McpServerForm.vue'
import KnowledgeRagForm from './forms/KnowledgeRagForm.vue'
import { NODE_CATEGORIES, getNodeTypeMeta, getNodesByCategory } from '../../constants/nodeTypes'
import { missingRequiredForNode } from '../../utils/nodeRequiredFields'
import type { FlowNode } from '../../composables/useWorkflowSync'
import type { WorkflowStatus } from '../../types/workflow'
import type { NodeRequiredFields } from '../../api/workflow'

const props = defineProps<{
  selectedNode?: FlowNode | null
  workflowName: string
  workflowDescription?: string
  /** overview 統計卡數字，由父層計算傳入 */
  nodeCount?: number
  connectionCount?: number
  triggerCount?: number
  workflowStatus?: WorkflowStatus
  /** 各 NodeType 必填欄位清單（後端契約）；未提供時不顯示必填缺漏提示 */
  requiredFields?: NodeRequiredFields
}>()

const emit = defineEmits<{
  'update:node-name': [name: string]
  'update:node-config': [config: Record<string, unknown>]
  'update:workflow-name': [name: string]
  'update:workflow-description': [description: string]
  'config-validity': [valid: boolean]
  'duplicate-node': []
  'delete-node': []
}>()

/** 型別化表單分派表；其餘型別退回 JsonConfigEditor */
const TYPED_FORMS = markRaw({
  LLM_ASSISTANT: LlmAssistantForm,
  TOOL: ToolForm,
  MCP_SERVER: McpServerForm,
  KNOWLEDGE_RAG: KnowledgeRagForm,
})

const typedForm = computed(() => {
  const type = props.selectedNode?.data.type
  return (type && TYPED_FORMS[type as keyof typeof TYPED_FORMS]) || null
})

// 型別化表單為結構化輸入，永遠有效；選中時通知 config 有效，清除先前 JSON 無效狀態
watch(
  typedForm,
  (form) => {
    if (form) emit('config-validity', true)
  },
  { immediate: true },
)

const selectedMeta = computed(() =>
  props.selectedNode ? getNodeTypeMeta(props.selectedNode.data.type) : undefined,
)

function nodeTypeLabel(node: FlowNode): string {
  return getNodeTypeMeta(node.data.type)?.label ?? node.data.type
}

/** hex 轉 16% 透明底色（型別色 icon box 用） */
function tint(color: string): string {
  return `${color}29`
}

/** overview 圖例：每分類取該分類第一個型別的代表色 */
const legend = computed(() =>
  NODE_CATEGORIES.map((category) => ({
    category,
    color: getNodesByCategory(category)[0]?.color ?? '#909399',
  })),
)

function onFormConfig(config: Record<string, unknown>) {
  emit('update:node-config', config)
}

/** 選中節點的必填欄位缺漏（依後端契約即時計算，不需等存檔） */
const missingRequired = computed(() => {
  if (!props.selectedNode || !props.requiredFields) return []
  return missingRequiredForNode(
    props.selectedNode.data.type,
    props.selectedNode.data.config ?? {},
    props.requiredFields,
  )
})
</script>

<template>
  <div class="inspector-panel">
    <!-- 已選取節點：header + Parameters + 操作列 -->
    <template v-if="props.selectedNode">
      <div class="node-header">
        <span
          class="node-icon-box"
          :style="{
            backgroundColor: tint(selectedMeta?.color ?? '#909399'),
            borderColor: selectedMeta?.color ?? '#909399',
          }"
        >{{ selectedMeta?.icon ?? '⬜' }}</span>
        <div class="node-header-info">
          <div class="node-type-label" :style="{ color: selectedMeta?.color ?? '#909399' }">
            {{ nodeTypeLabel(props.selectedNode) }}
          </div>
          <input
            class="text-input name-input"
            data-test="node-name-input"
            :value="props.selectedNode.data.name"
            @input="emit('update:node-name', ($event.target as HTMLInputElement).value)"
          />
        </div>
      </div>

      <div
        v-if="missingRequired.length > 0"
        class="required-warning"
        data-test="required-field-warning"
      >
        缺少必填欄位：{{ missingRequired.join('、') }}
      </div>

      <div class="section">
        <div class="section-title">Parameters</div>
        <!-- 型別化表單；其餘型別退回 JSON 編輯器 -->
        <component
          :is="typedForm"
          v-if="typedForm"
          :key="props.selectedNode.id"
          :config="props.selectedNode.data.config ?? {}"
          @update:config="onFormConfig"
        />
        <JsonConfigEditor
          v-else
          :model-value="props.selectedNode.data.config"
          @update:model-value="emit('update:node-config', $event)"
          @validity-change="emit('config-validity', $event)"
        />
      </div>

      <div class="node-actions">
        <button
          type="button"
          class="action-btn"
          data-test="duplicate-node-button"
          @click="emit('duplicate-node')"
        >
          Duplicate
        </button>
        <button
          type="button"
          class="action-btn danger"
          data-test="delete-node-button"
          @click="emit('delete-node')"
        >
          Delete
        </button>
      </div>
    </template>

    <!-- 未選取：Workflow overview（meta 編輯 + 統計卡 + 圖例） -->
    <template v-else>
      <div class="section-title">Workflow overview</div>
      <p class="overview-hint">點選畫布上的節點以編輯其參數；自左側面板拖曳節點以新增。</p>

      <div class="field">
        <label>流程名稱</label>
        <input
          class="text-input"
          data-test="workflow-name-input"
          :value="props.workflowName"
          @input="emit('update:workflow-name', ($event.target as HTMLInputElement).value)"
        />
      </div>
      <div class="field">
        <label>描述</label>
        <textarea
          class="text-input"
          data-test="workflow-description-input"
          :value="props.workflowDescription"
          rows="3"
          @input="emit('update:workflow-description', ($event.target as HTMLTextAreaElement).value)"
        />
      </div>

      <div class="stat-grid">
        <div class="stat-card" data-test="stat-nodes">
          <div class="stat-value">{{ props.nodeCount ?? 0 }}</div>
          <div class="stat-label">Nodes</div>
        </div>
        <div class="stat-card" data-test="stat-connections">
          <div class="stat-value">{{ props.connectionCount ?? 0 }}</div>
          <div class="stat-label">Connections</div>
        </div>
        <div class="stat-card" data-test="stat-triggers">
          <div class="stat-value">{{ props.triggerCount ?? 0 }}</div>
          <div class="stat-label">Triggers</div>
        </div>
        <div class="stat-card" data-test="stat-status">
          <div class="stat-value status">{{ props.workflowStatus ?? 'DRAFT' }}</div>
          <div class="stat-label">Status</div>
        </div>
      </div>

      <div class="section">
        <div class="section-title">Node types</div>
        <div class="legend">
          <div
            v-for="item in legend"
            :key="item.category"
            class="legend-item"
            :data-test="`legend-${item.category}`"
          >
            <span class="legend-dot" :style="{ backgroundColor: item.color }" />
            <span class="legend-label">{{ item.category }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.inspector-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 100%;
  box-sizing: border-box;
  padding: 14px;
  background: var(--wf-surface, #17171c);
  color: var(--wf-text, #e7e7ec);
}

/* ---- 已選節點 header ---- */
.node-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--wf-border, #29292f);
}

.node-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  font-size: 17px;
  border: 1px solid;
  border-radius: 10px;
}

.node-header-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.node-type-label {
  font-size: 9.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.name-input {
  font-weight: 700;
}

/* ---- 必填欄位缺漏提示 ---- */
.required-warning {
  padding: 8px 10px;
  font-size: 11.5px;
  line-height: 1.5;
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.12);
  border: 1px solid rgba(230, 162, 60, 0.35);
  border-radius: 8px;
}

/* ---- 區段 ---- */
.section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--wf-text-mute, #5c5c67);
}

.overview-hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--wf-text-dim, #8a8a95);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field label {
  font-size: 11px;
  color: var(--wf-text-dim, #8a8a95);
}

.text-input {
  width: 100%;
  box-sizing: border-box;
  padding: 6px 8px;
  font-size: 12.5px;
  font-family: inherit;
  color: var(--wf-text, #e7e7ec);
  background: var(--wf-input, #0f0f13);
  border: 1px solid var(--wf-border, #29292f);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.15s;
}

.text-input:focus {
  border-color: var(--wf-accent, #ff6a54);
}

/* ---- 統計卡 2×2 ---- */
.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  background: var(--wf-card, #1e1e26);
  border: 1px solid var(--wf-border, #29292f);
  border-radius: 10px;
}

.stat-value {
  font-size: 18px;
  font-weight: 800;
  color: var(--wf-text, #e7e7ec);
}

.stat-value.status {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--wf-success, #3ecf8e);
}

.stat-label {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--wf-text-mute, #5c5c67);
}

/* ---- 圖例 ---- */
.legend {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-label {
  font-size: 12px;
  color: var(--wf-text-dim, #8a8a95);
}

/* ---- 底部操作列 ---- */
.node-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--wf-border, #29292f);
}

.action-btn {
  flex: 1;
  padding: 7px 0;
  font-size: 12px;
  font-weight: 700;
  font-family: inherit;
  color: var(--wf-text, #e7e7ec);
  background: var(--wf-card, #1e1e26);
  border: 1px solid var(--wf-border-2, #31313c);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.12s, color 0.12s;
}

.action-btn:hover {
  border-color: var(--wf-text-mute, #5c5c67);
}

.action-btn.danger {
  color: #f56c6c;
}

.action-btn.danger:hover {
  border-color: #f56c6c;
}
</style>
