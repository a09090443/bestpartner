<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const template = ref<string>((props.config.template as string) ?? '')
// mappings 以每行 key=value 編輯
const mappingsText = ref<string>(
  Object.entries((props.config.mappings as Record<string, string>) ?? {})
    .map(([k, v]) => `${k}=${v}`)
    .join('\n'),
)

function parseMappings(text: string): Record<string, string> {
  const result: Record<string, string> = {}
  text.split('\n').forEach((line) => {
    const idx = line.indexOf('=')
    if (idx > 0) result[line.slice(0, idx).trim()] = line.slice(idx + 1).trim()
  })
  return result
}

function emitConfig() {
  const next: Record<string, unknown> = { ...props.config }
  if (template.value) next.template = template.value
  else delete next.template
  const mappings = parseMappings(mappingsText.value)
  if (Object.keys(mappings).length > 0) next.mappings = mappings
  else delete next.mappings
  emit('update:config', next)
}
</script>

<template>
  <div class="form">
    <div class="field">
      <!-- v-pre 避免 {{變數}} 被 Vue 當插值 -->
      <label v-pre>輸出模板（支援 {{nodeKey.path}} 插值，結果放 result 鍵）</label>
      <textarea
        v-model="template"
        data-test="output-template"
        class="text-input"
        rows="4"
        @input="emitConfig"
      />
    </div>
    <div class="field">
      <label>欄位對映（選填，每行 key=value，value 支援插值）</label>
      <textarea
        v-model="mappingsText"
        data-test="output-mappings"
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
