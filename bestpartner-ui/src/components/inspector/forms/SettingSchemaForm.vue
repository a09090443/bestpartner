<script setup lang="ts">
import { computed } from 'vue'
import type { ToolSettingFieldSchema, ToolSettingSchema } from '../../../types/toolSchema'

const props = defineProps<{
  schema: ToolSettingSchema
  modelValue: Record<string, unknown>
}>()
const emit = defineEmits<{ 'update:modelValue': [value: Record<string, unknown>] }>()

const fields = computed(() => Object.entries(props.schema))

/** 依欄位描述決定 input type：sensitive 優先 password，數字 number，布林 checkbox，其餘（含 array）text */
function inputType(field: ToolSettingFieldSchema): string {
  if (field.sensitive) return 'password'
  if (field.type === 'integer' || field.type === 'number') return 'number'
  if (field.type === 'boolean') return 'checkbox'
  return 'text'
}

/** 以不可變方式合併回 modelValue；空值欄位移除該鍵 */
function updateField(name: string, value: unknown) {
  const next = { ...props.modelValue }
  if (value === undefined || value === '') delete next[name]
  else next[name] = value
  emit('update:modelValue', next)
}

function onInput(name: string, event: Event) {
  const el = event.target as HTMLInputElement
  const field = props.schema[name]
  let value: unknown
  if (field.type === 'integer' || field.type === 'number') {
    value = el.value === '' ? undefined : Number(el.value)
  } else {
    // string 與 array（v1 以原樣字串傳遞）皆為字串
    value = el.value
  }
  updateField(name, value)
}

function onCheckbox(name: string, event: Event) {
  updateField(name, (event.target as HTMLInputElement).checked)
}
</script>

<template>
  <div class="form">
    <div v-for="[name, field] in fields" :key="name" class="field">
      <label :data-test="`label-${name}`">
        {{ name }}<span v-if="field.required" class="required">*</span>
        <span v-if="field.description" class="hint">{{ field.description }}</span>
      </label>
      <input
        v-if="field.type === 'boolean'"
        :data-test="`field-${name}`"
        type="checkbox"
        :checked="Boolean(modelValue[name])"
        class="text-input checkbox"
        @change="onCheckbox(name, $event)"
      />
      <input
        v-else
        :data-test="`field-${name}`"
        :type="inputType(field)"
        :value="(modelValue[name] as string | number | undefined) ?? ''"
        class="text-input"
        @input="onInput(name, $event)"
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

.required {
  color: var(--wf-accent, #ff6a54);
  margin-left: 2px;
}

.hint {
  margin-left: 6px;
  opacity: 0.75;
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

.text-input.checkbox {
  width: auto;
  align-self: flex-start;
}
</style>
