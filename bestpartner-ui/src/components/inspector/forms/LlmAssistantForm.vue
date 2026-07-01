<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useNodeOptions } from '../../../composables/useNodeOptions'
import type { Option } from '../../../types/options'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const options = ref<Option[]>([])
const llmId = ref<string>((props.config.llmId as string) ?? '')
const systemPrompt = ref<string>((props.config.systemPrompt as string) ?? '')

onMounted(async () => {
  try {
    options.value = await useNodeOptions().loadLlmOptions()
  } catch {
    options.value = []
  }
})

/** 以不可變方式合併回 config，保留未知鍵值 */
function emitConfig() {
  const next: Record<string, unknown> = { ...props.config, llmId: llmId.value }
  if (systemPrompt.value) next.systemPrompt = systemPrompt.value
  else delete next.systemPrompt
  emit('update:config', next)
}
</script>

<template>
  <div class="form">
    <div class="field">
      <label>LLM 設定</label>
      <select v-model="llmId" data-test="llm-select" class="text-input" @change="emitConfig">
        <option value="">請選擇</option>
        <option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>
    <div class="field">
      <label>系統提示（選填）</label>
      <textarea
        v-model="systemPrompt"
        data-test="system-prompt"
        class="text-input"
        rows="3"
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
