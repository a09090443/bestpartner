<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const knowledgeId = ref<string>((props.config.knowledgeId as string) ?? '')
const topK = ref<number>((props.config.topK as number) ?? 4)

function emitConfig() {
  emit('update:config', { ...props.config, knowledgeId: knowledgeId.value, topK: topK.value })
}
</script>

<template>
  <div class="form">
    <div class="field">
      <label>知識庫 ID</label>
      <input
        v-model="knowledgeId"
        data-test="knowledge-id"
        class="text-input"
        @input="emitConfig"
      />
    </div>
    <div class="field">
      <label>取回筆數（topK）</label>
      <input
        v-model.number="topK"
        data-test="topk"
        type="number"
        min="1"
        class="text-input"
        @input="emitConfig"
      />
    </div>
  </div>
</template>

<style scoped>
.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
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

.text-input {
  width: 100%;
  box-sizing: border-box;
  padding: 4px 6px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-family: inherit;
}
</style>
