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

select.text-input option {
  background: var(--wf-input, #0f0f13);
  color: var(--wf-text, #e7e7ec);
}
</style>
