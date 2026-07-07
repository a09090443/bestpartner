<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { ToolSettingFieldSchema, ToolSettingSchema } from '../../../types/toolSchema'

const props = defineProps<{
  schema: ToolSettingSchema
  modelValue: Record<string, unknown>
}>()
const emit = defineEmits<{ 'update:modelValue': [value: Record<string, unknown>] }>()

const fields = computed(() => Object.entries(props.schema))

/**
 * array 欄位的本地原始輸入字串（欄位名 -> raw 字串）。
 * array input 的 :value 綁定此 buffer 而非從 model 反推，
 * 否則使用者每打一個逗號會被 split→filter→join 還原吃掉（尾端逗號消失）。
 */
const arrayBuffers = reactive<Record<string, string>>({})

/** 將 array 欄位原始字串切割為字串陣列：split→trim→濾空段。注意：值本身不可含逗號 */
function parseArray(raw: string): string[] {
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s !== '')
}

function arraysEqual(a: string[], b: string[]): boolean {
  return a.length === b.length && a.every((v, i) => v === b[i])
}

/**
 * 依 schema 與 modelValue 同步 array 欄位 buffer。
 * 避免同步迴圈：僅在 buffer 尚未建立，或其解析結果與 model 陣列「不等價」（＝外部變更，
 * 如切換工具）時才以 model 重建 buffer；使用者自行 emit 造成的 model 變更解析後等價，
 * 故不覆寫正在編輯的原始字串（保留尾端逗號等中間狀態）。
 */
function syncArrayBuffers() {
  for (const [name, field] of Object.entries(props.schema)) {
    if (field.type !== 'array') continue
    const v = props.modelValue[name]
    const modelArr = Array.isArray(v) ? (v as string[]) : []
    if (!(name in arrayBuffers) || !arraysEqual(parseArray(arrayBuffers[name]), modelArr)) {
      arrayBuffers[name] = modelArr.join(', ')
    }
  }
}

watch([() => props.schema, () => props.modelValue], syncArrayBuffers, { immediate: true })

/**
 * 依欄位描述決定 input type：sensitive 優先 password，數字 number，其餘（含 array）text。
 * boolean 已由 template 的 v-if 分流至 checkbox，不會進入此函式。
 */
function inputType(field: ToolSettingFieldSchema): string {
  if (field.sensitive) return 'password'
  if (field.type === 'integer' || field.type === 'number') return 'number'
  return 'text'
}

/** 以不可變方式合併回 modelValue；空值欄位移除該鍵 */
function updateField(name: string, value: unknown) {
  const next = { ...props.modelValue }
  if (value === undefined || value === '') delete next[name]
  else next[name] = value
  emit('update:modelValue', next)
}

/** input 顯示值：array 綁本地 buffer（不從 model 反推），其餘型別原樣顯示 */
function inputValue(name: string): string | number {
  if (props.schema[name].type === 'array') return arrayBuffers[name] ?? ''
  const v = props.modelValue[name]
  return (v as string | number | undefined) ?? ''
}

function onInput(name: string, event: Event) {
  const el = event.target as HTMLInputElement
  const field = props.schema[name]
  let value: unknown
  if (field.type === 'integer') {
    // integer 欄位截斷小數，僅保留整數（後端對應 Int/Long；Math.trunc 趨零截斷，負數亦適用）
    value = el.value === '' ? undefined : Math.trunc(Number(el.value))
  } else if (field.type === 'number') {
    value = el.value === '' ? undefined : Number(el.value)
  } else if (field.type === 'array') {
    // 先更新本地 buffer（保留原始輸入），再 emit 切割後的字串陣列；空則刪鍵
    arrayBuffers[name] = el.value
    const parts = parseArray(el.value)
    value = parts.length === 0 ? undefined : parts
  } else {
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
      <!-- input 直接包在 label 內建立關聯，多實例共存不需產生唯一 id -->
      <label :data-test="`label-${name}`">
        <span class="label-text">
          {{ name }}<span v-if="field.required" class="required">*</span>
          <span v-if="field.description" class="hint">{{ field.description }}</span>
        </span>
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
          :step="field.type === 'integer' ? '1' : undefined"
          :autocomplete="field.sensitive ? 'new-password' : undefined"
          :placeholder="field.type === 'array' ? '以逗號分隔（值不可含逗號）' : undefined"
          :value="inputValue(name)"
          class="text-input"
          @input="onInput(name, $event)"
        />
      </label>
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
  display: flex;
  flex-direction: column;
  gap: 4px;
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
