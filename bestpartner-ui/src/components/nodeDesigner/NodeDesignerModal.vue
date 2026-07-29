<script setup lang="ts">
/**
 * 節點全屏編輯頁（Node Designer）。
 *
 * ⚠️ 刻意不用 <Teleport>：`--wf-*` token 定義在 .wf-editor 上、styles/wf-form.css 的規則
 * 也以 `.wf-editor ` 前綴，teleport 到 body 兩者會同時失效。
 * 本元件作為 .wf-editor 子節點以 position: fixed 覆蓋全屏（祖先皆無 transform，安全）。
 *
 * 本元件為 dumb component：不自行取畫布或執行資料，全部由 WorkflowEditorView 以 prop 傳入。
 */
import { computed, onBeforeUnmount, onMounted } from 'vue'
import DesignerInputPanel from './DesignerInputPanel.vue'
import DesignerParamsPanel from './DesignerParamsPanel.vue'
import DesignerOutputPanel from './DesignerOutputPanel.vue'
import NodeIcon from '../common/NodeIcon.vue'
import { NODE_FALLBACK_COLOR, getNodeTypeMeta } from '../../constants/nodeTypes'
import type { NodeType } from '../../types/workflow'
import type { UpstreamSource } from '../../types/nodeDesigner'
import type { NodeRequiredFields } from '../../api/workflow'
import type { NodeRunState } from '../../stores/execution'

const props = defineProps<{
  nodeId: string
  type: NodeType
  name: string
  config: Record<string, unknown>
  sources: UpstreamSource[]
  run?: NodeRunState
  requiredFields?: NodeRequiredFields
  promptBoundNodeKeys?: string[]
}>()

const emit = defineEmits<{
  close: []
  'update:node-name': [name: string]
  'update:node-config': [config: Record<string, unknown>]
  'config-validity': [valid: boolean]
  'duplicate-node': []
  'delete-node': []
}>()

const meta = computed(() => getNodeTypeMeta(props.type))
const color = computed(() => meta.value?.color ?? NODE_FALLBACK_COLOR)

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') emit('close')
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="designer-overlay" data-test="node-designer-modal" @click="emit('close')">
    <div class="designer-card" @click.stop>
      <header class="designer-header">
        <span class="header-icon" :style="{ '--node-color': color }">
          <NodeIcon :type="props.type" :size="22" />
        </span>
        <div class="header-info">
          <div class="header-meta">
            <span class="type-label" :style="{ '--node-color': color }">{{ meta?.label }}</span>
            <span class="category-pill">{{ meta?.category }}</span>
          </div>
          <input
            class="header-name"
            data-test="node-designer-name-input"
            :value="props.name"
            @input="emit('update:node-name', ($event.target as HTMLInputElement).value)"
          />
        </div>
        <button
          type="button"
          class="close-btn"
          data-test="node-designer-close"
          aria-label="關閉節點編輯頁"
          @click="emit('close')"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linecap="round"
            aria-hidden="true"
          >
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
      </header>

      <div class="designer-body">
        <DesignerInputPanel :type="props.type" :sources="props.sources" />
        <DesignerParamsPanel
          :node-id="props.nodeId"
          :type="props.type"
          :config="props.config"
          :required-fields="props.requiredFields"
          :prompt-bound-node-keys="props.promptBoundNodeKeys"
          :run="props.run"
          @update:config="emit('update:node-config', $event)"
          @config-validity="emit('config-validity', $event)"
          @duplicate-node="emit('duplicate-node')"
          @delete-node="emit('delete-node')"
        />
        <DesignerOutputPanel :node-id="props.nodeId" :type="props.type" :run="props.run" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.designer-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px;
  background: rgba(18, 20, 28, 0.55);
  backdrop-filter: blur(3px);
  animation: wf-fade 0.16s ease-out;
}

.designer-card {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  max-width: 1180px;
  max-height: 760px;
  background: var(--wf-panel);
  border: 1px solid var(--wf-border);
  border-radius: 18px;
  box-shadow: var(--wf-shadow-lg);
  overflow: hidden;
  animation: wf-in 0.2s cubic-bezier(0.2, 0.8, 0.3, 1);
}

/* ---- header ---- */
.designer-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  padding: 14px 16px;
  border-bottom: 1px solid var(--wf-border);
}

.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  color: color-mix(in srgb, var(--node-color) var(--node-fg-mix), #000);
  background: color-mix(in srgb, var(--node-color) 13%, transparent);
  border: 1px solid color-mix(in srgb, var(--node-color) 28%, transparent);
  border-radius: 12px;
}

.header-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-label {
  font-size: 9.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: color-mix(in srgb, var(--node-color) var(--node-fg-mix), #000);
}

.category-pill {
  padding: 1px 8px;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--wf-text-3);
  background: var(--wf-card-2);
  border: 1px solid var(--wf-border);
  border-radius: 999px;
}

/* 名稱同樣採 inline 編輯（與 Inspector 一致） */
.header-name {
  width: 100%;
  box-sizing: border-box;
  padding: 2px 6px;
  margin-left: -6px;
  font-size: 17px;
  font-weight: 800;
  font-family: inherit;
  color: var(--wf-text);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 7px;
  outline: none;
  transition: border-color 0.12s, background 0.12s;
}

.header-name:hover {
  border-color: var(--wf-border);
}

.header-name:focus {
  background: var(--wf-input);
  border-color: var(--wf-accent);
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  padding: 0;
  color: var(--wf-text-2);
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 10px;
  cursor: pointer;
  transition: color 0.12s, border-color 0.12s;
}

.close-btn svg {
  width: 15px;
  height: 15px;
}

.close-btn:hover {
  color: var(--wf-text);
  border-color: var(--wf-text-3);
}

/* ---- 三欄 ---- */
.designer-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1.15fr 1.05fr;
}

@keyframes wf-fade {
  from {
    opacity: 0;
  }
}

@keyframes wf-in {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.985);
  }
}
</style>
