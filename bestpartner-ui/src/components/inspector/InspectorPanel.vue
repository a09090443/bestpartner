<script setup lang="ts">
import { computed, watch } from 'vue'
import JsonConfigEditor from './JsonConfigEditor.vue'
import NodeIcon from '../common/NodeIcon.vue'
import { resolveTypedForm, buildTypedFormProps } from './typedForms'
import {
  NODE_CATEGORIES,
  NODE_FALLBACK_COLOR,
  getNodeTypeMeta,
  getNodesByCategory,
} from '../../constants/nodeTypes'
import { missingRequiredForNode, formatMissingFields } from '../../utils/nodeRequiredFields'
import { useExecutionStore } from '../../stores/execution'
import type { FlowNode } from '../../composables/useWorkflowSync'
import type { WorkflowStatus } from '../../types/workflow'
import type { NodeRequiredFields } from '../../api/workflow'
import type { UpstreamRef } from '../../constants/nodeOutputKeys'

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
  /** 已有 PROMPT 節點連到其 in:prompt 埠的 LLM 節點 key 清單（由編輯器依畫布 edges 計算） */
  promptBoundNodeKeys?: string[]
  /** 圖上可供 OUTPUT 引用的上游輸出建議（由編輯器依畫布節點計算） */
  upstreamRefs?: UpstreamRef[]
  /** nodeKey → 顯示名稱，供 OUTPUT 的運算式編輯器渲染可讀色塊 */
  refLabels?: Record<string, string>
}>()

const emit = defineEmits<{
  'update:node-name': [name: string]
  'update:node-config': [config: Record<string, unknown>]
  'update:workflow-name': [name: string]
  'update:workflow-description': [description: string]
  'config-validity': [valid: boolean]
  'duplicate-node': []
  'delete-node': []
  /** 請父層開啟該節點的全屏編輯頁（Node Designer）；modal 狀態由編輯器持有 */
  'open-designer': []
}>()

/** 型別化表單分派（與 Node Designer 共用，見 typedForms.ts）；其餘型別退回 JsonConfigEditor */
const typedForm = computed(() => resolveTypedForm(props.selectedNode?.data.type))

const typedFormProps = computed(() =>
  buildTypedFormProps({
    type: props.selectedNode?.data.type,
    nodeId: props.selectedNode?.id,
    config: props.selectedNode?.data.config,
    promptBoundNodeKeys: props.promptBoundNodeKeys,
    upstreamRefs: props.upstreamRefs,
    refLabels: props.refLabels,
  }),
)

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

/** overview 圖例：每分類取該分類第一個型別的代表色 */
const legend = computed(() =>
  NODE_CATEGORIES.map((category) => ({
    category,
    color: getNodesByCategory(category)[0]?.color ?? NODE_FALLBACK_COLOR,
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

/** 缺漏欄位的中文化顯示文字（複合字樣轉為「A 或 B（擇一）」、已知鍵轉中文標籤） */
const missingRequiredText = computed(() => formatMissingFields(missingRequired.value))

/**
 * 提示語意：ACTIVE workflow 存檔時必填缺漏會被升級為擋存 error（見 WorkflowEditorView.handleSave），
 * 故此處同步改為紅色 error 樣式；DRAFT / INACTIVE 僅為 warning 提示，維持橘色。
 */
const requiredWarningSeverity = computed<'error' | 'warning'>(() =>
  props.workflowStatus === 'ACTIVE' ? 'error' : 'warning',
)

// 選中節點的本次執行狀態（依 nodeKey 從 execution store 讀取，見 FlowNode.id）
const executionStore = useExecutionStore()
const nodeRun = computed(() =>
  props.selectedNode ? executionStore.nodeStates[props.selectedNode.id] : undefined,
)
</script>

<template>
  <div class="inspector-panel">
    <!-- 已選取節點：header + Parameters + 操作列 -->
    <template v-if="props.selectedNode">
      <div class="node-header">
        <span
          class="node-icon-box"
          :style="{ '--node-color': selectedMeta?.color ?? NODE_FALLBACK_COLOR }"
        >
          <NodeIcon :type="props.selectedNode.data.type" :size="20" />
        </span>
        <div class="node-header-info">
          <div
            class="node-type-label"
            :style="{ '--node-color': selectedMeta?.color ?? NODE_FALLBACK_COLOR }"
          >
            {{ nodeTypeLabel(props.selectedNode) }}
          </div>
          <input
            class="name-input"
            data-test="node-name-input"
            :value="props.selectedNode.data.name"
            @input="emit('update:node-name', ($event.target as HTMLInputElement).value)"
          />
        </div>
      </div>

      <button
        type="button"
        class="designer-btn"
        data-test="open-node-designer-button"
        @click="emit('open-designer')"
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M15 3h6v6M21 3l-8 8M10 5H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-5" />
        </svg>
        開啟節點編輯頁
      </button>

      <div
        v-if="missingRequired.length > 0"
        class="required-warning"
        :class="`required-warning--${requiredWarningSeverity}`"
        :data-severity="requiredWarningSeverity"
        data-test="required-field-warning"
      >
        缺少必填欄位：{{ missingRequiredText }}
      </div>

      <div class="section">
        <div class="section-title">Parameters</div>
        <!-- 型別化表單；其餘型別退回 JSON 編輯器 -->
        <component
          :is="typedForm"
          v-if="typedForm"
          :key="props.selectedNode.id"
          v-bind="typedFormProps"
          @update:config="onFormConfig"
        />
        <JsonConfigEditor
          v-else
          :model-value="props.selectedNode.data.config"
          @update:model-value="emit('update:node-config', $event)"
          @validity-change="emit('config-validity', $event)"
        />
      </div>

      <div v-if="nodeRun" class="section exec-section" data-test="node-exec-section">
        <div class="section-title">本次執行</div>
        <p>狀態：{{ nodeRun.status }}<span v-if="nodeRun.durationMs != null">（{{ nodeRun.durationMs }} ms）</span></p>
        <p v-if="nodeRun.error" class="error-text">{{ nodeRun.error }}</p>
        <pre v-if="nodeRun.output" class="output-json">{{ JSON.stringify(nodeRun.output, null, 2) }}</pre>
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
  background: var(--wf-panel);
  color: var(--wf-text);
}

/* ---- 已選節點 header ---- */
.node-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--wf-border);
}

.node-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  color: color-mix(in srgb, var(--node-color) var(--node-fg-mix), #000);
  background: color-mix(in srgb, var(--node-color) 13%, transparent);
  border: 1px solid color-mix(in srgb, var(--node-color) 28%, transparent);
  border-radius: 11px;
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
  color: color-mix(in srgb, var(--node-color) var(--node-fg-mix), #000);
}

/* 名稱採 inline 編輯：平時看起來像純文字，hover 才浮出邊框、focus 才上底色 */
.name-input {
  width: 100%;
  box-sizing: border-box;
  padding: 3px 6px;
  margin-left: -6px;
  font-size: 15px;
  font-weight: 700;
  font-family: inherit;
  color: var(--wf-text);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 7px;
  outline: none;
  transition: border-color 0.12s, background 0.12s;
}

.name-input:hover {
  border-color: var(--wf-border);
}

.name-input:focus {
  background: var(--wf-input);
  border-color: var(--wf-accent);
}

/* ---- 開啟 Node Designer ---- */
.designer-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  padding: 9px 0;
  font-size: 12.5px;
  font-weight: 700;
  font-family: inherit;
  color: var(--wf-text);
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 9px;
  cursor: pointer;
  transition: border-color 0.12s, background 0.12s;
}

.designer-btn svg {
  width: 14px;
  height: 14px;
  color: var(--wf-text-2);
}

.designer-btn:hover {
  background: var(--wf-hover);
  border-color: var(--wf-text-3);
}

/* ---- 必填欄位缺漏提示 ---- */
.required-warning {
  padding: 8px 10px;
  font-size: 11.5px;
  line-height: 1.5;
  border-radius: 8px;
  border: 1px solid transparent;
}

/* DRAFT / INACTIVE：可存檔但未來啟用時會被擋，橘色 warning 語意 */
.required-warning--warning {
  color: var(--wf-warning);
  background: var(--wf-warning-bg);
  border-color: var(--wf-warning-border);
}

/* ACTIVE：存檔當下即被擋，紅色 error 語意 */
.required-warning--error {
  color: var(--wf-danger);
  background: var(--wf-danger-bg);
  border-color: var(--wf-danger-border);
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
  color: var(--wf-text-3);
}

.overview-hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--wf-text-2);
}

/* .field / .field label / .text-input 由 styles/wf-form.css 共用 */

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
  padding: 11px 13px;
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 11px;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.15;
  color: var(--wf-text);
}

.stat-value.status {
  font-size: 13px;
  line-height: 1.9;
  letter-spacing: 0.08em;
  color: var(--wf-success);
}

.stat-label {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--wf-text-3);
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
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.legend-label {
  font-size: 12px;
  color: var(--wf-text-2);
}

/* ---- 本次執行 ---- */
.exec-section p {
  margin: 0;
  font-size: 12px;
  color: var(--wf-text-2);
}

.exec-section .error-text {
  color: var(--wf-danger);
}

.exec-section .output-json {
  margin: 0;
  font-size: 11.5px;
  color: var(--wf-text);
  background: var(--wf-input);
  border: 1px solid var(--wf-border);
  border-radius: 8px;
  padding: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ---- 底部操作列 ---- */
.node-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--wf-border);
}

.action-btn {
  flex: 1;
  padding: 7px 0;
  font-size: 12px;
  font-weight: 700;
  font-family: inherit;
  color: var(--wf-text);
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.12s, color 0.12s;
}

.action-btn:hover {
  border-color: var(--wf-text-3);
}

/* Delete 不佔滿寬（Duplicate flex:1 吃掉剩餘空間），並以 danger 底色與之區隔 */
.action-btn.danger {
  flex: 0 0 auto;
  padding: 7px 14px;
  color: var(--wf-danger);
  background: var(--wf-danger-bg);
  border-color: var(--wf-danger-border);
}

.action-btn.danger:hover {
  border-color: var(--wf-danger);
}
</style>
