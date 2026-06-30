<script setup lang="ts">
import JsonConfigEditor from './JsonConfigEditor.vue'
import { getNodeTypeMeta } from '../../constants/nodeTypes'
import type { FlowNode } from '../../composables/useWorkflowSync'

const props = defineProps<{
  selectedNode?: FlowNode | null
  workflowName: string
  workflowDescription?: string
}>()

const emit = defineEmits<{
  'update:node-name': [name: string]
  'update:node-config': [config: Record<string, unknown>]
  'update:workflow-name': [name: string]
  'update:workflow-description': [description: string]
  'config-validity': [valid: boolean]
}>()

function nodeTypeLabel(node: FlowNode): string {
  return getNodeTypeMeta(node.data.type)?.label ?? node.data.type
}
</script>

<template>
  <div class="inspector-panel">
    <div class="inspector-title">屬性</div>

    <!-- 已選取節點：編輯名稱與 config -->
    <template v-if="props.selectedNode">
      <div class="field">
        <label>型別</label>
        <div class="readonly">{{ nodeTypeLabel(props.selectedNode) }}</div>
      </div>
      <div class="field">
        <label>名稱</label>
        <input
          class="text-input"
          data-test="node-name-input"
          :value="props.selectedNode.data.name"
          @input="emit('update:node-name', ($event.target as HTMLInputElement).value)"
        />
      </div>
      <div class="field">
        <label>設定（config）</label>
        <JsonConfigEditor
          :model-value="props.selectedNode.data.config"
          @update:model-value="emit('update:node-config', $event)"
          @validity-change="emit('config-validity', $event)"
        />
      </div>
    </template>

    <!-- 未選取：編輯 workflow meta -->
    <template v-else>
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
    </template>
  </div>
</template>

<style scoped>
.inspector-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inspector-title {
  font-weight: 600;
  color: #909399;
  font-size: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field label {
  font-size: 12px;
  color: #606266;
}

.readonly {
  font-size: 13px;
  color: #303133;
}

.text-input {
  width: 100%;
  box-sizing: border-box;
  padding: 4px 6px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-family: inherit;
}
</style>
