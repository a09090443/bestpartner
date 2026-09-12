<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: Record<string, unknown>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, unknown>]
  'validity-change': [valid: boolean]
}>()

type Mode = 'kv' | 'raw'
const mode = ref<Mode>('kv')

// raw 模式的文字與錯誤
const rawText = ref(JSON.stringify(props.modelValue, null, 2))
const jsonError = ref('')

// 外部 modelValue 變動時，同步 raw 文字（非 raw 編輯期間）
watch(
  () => props.modelValue,
  (val) => {
    if (mode.value !== 'raw') {
      rawText.value = JSON.stringify(val, null, 2)
    }
  },
)

function toggleMode() {
  if (mode.value === 'kv') {
    rawText.value = JSON.stringify(props.modelValue, null, 2)
    jsonError.value = ''
    mode.value = 'raw'
  } else {
    mode.value = 'kv'
    jsonError.value = ''
  }
}

/** key-value 模式：將單一欄位字串嘗試解析為 JSON 值，失敗則維持字串 */
function parseScalar(raw: string): unknown {
  try {
    return JSON.parse(raw)
  } catch {
    return raw
  }
}

function onKvInput(key: string, rawValue: string) {
  const next = { ...props.modelValue, [key]: parseScalar(rawValue) }
  emit('update:modelValue', next)
}

function valueToString(value: unknown): string {
  if (typeof value === 'string') return value
  return JSON.stringify(value)
}

// ---- 新增欄位 ----
// 表格模式原本只能編輯「已存在的鍵」，config 為空時整個面板無路可走，
// 逼使用者切到 JSON 模式手打——對沒有型別化表單的節點是硬門檻。
const newKey = ref('')
const keyError = ref('')

/** 鍵名需非空且未重複；不做字元限制（後端 config 的鍵由各 NodeConfig 契約決定） */
function addField() {
  const key = newKey.value.trim()
  if (!key) {
    keyError.value = '請輸入欄位名稱'
    return
  }
  if (Object.prototype.hasOwnProperty.call(props.modelValue, key)) {
    keyError.value = `欄位「${key}」已存在`
    return
  }
  keyError.value = ''
  newKey.value = ''
  // 新鍵先給空字串，讓它出現在表格中供接著填值
  emit('update:modelValue', { ...props.modelValue, [key]: '' })
}

function removeField(key: string) {
  const next = { ...props.modelValue }
  delete next[key]
  emit('update:modelValue', next)
}

function onRawInput(text: string) {
  rawText.value = text
  try {
    const parsed = JSON.parse(text)
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      throw new Error('config 必須為物件')
    }
    jsonError.value = ''
    emit('validity-change', true)
    emit('update:modelValue', parsed as Record<string, unknown>)
  } catch (err) {
    jsonError.value = err instanceof Error ? err.message : 'JSON 格式錯誤'
    emit('validity-change', false)
  }
}
</script>

<template>
  <div class="json-config-editor">
    <div class="toolbar">
      <button type="button" class="mode-btn" data-test="mode-toggle" @click="toggleMode">
        {{ mode === 'kv' ? '切換到 JSON 模式' : '切換到表格模式' }}
      </button>
    </div>

    <!-- key-value 表格模式 -->
    <template v-if="mode === 'kv'">
      <table v-if="Object.keys(modelValue).length > 0" class="kv-table">
        <tbody>
          <tr v-for="(value, key) in modelValue" :key="key">
            <td class="kv-key">{{ key }}</td>
            <td class="kv-value">
              <input
                class="kv-input"
                :data-test="`value-input-${key}`"
                :value="valueToString(value)"
                @input="onKvInput(String(key), ($event.target as HTMLInputElement).value)"
              />
            </td>
            <td class="kv-actions">
              <button
                type="button"
                class="remove-btn"
                :data-test="`remove-field-${key}`"
                :aria-label="`移除欄位 ${key}`"
                title="移除此欄位"
                @click="removeField(String(key))"
              >
                ✕
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">無設定欄位</p>

      <div class="add-field">
        <input
          v-model="newKey"
          class="kv-input"
          data-test="new-field-key"
          placeholder="欄位名稱"
          @keyup.enter="addField"
        />
        <button type="button" class="add-btn" data-test="add-field" @click="addField">新增欄位</button>
      </div>
      <p v-if="keyError" class="field-error" data-test="new-field-error">{{ keyError }}</p>
    </template>

    <!-- raw JSON 模式 -->
    <div v-else class="raw-mode">
      <textarea
        class="raw-input"
        data-test="raw-input"
        :value="rawText"
        rows="8"
        @input="onRawInput(($event.target as HTMLTextAreaElement).value)"
      />
      <p v-if="jsonError" class="json-error" data-test="json-error">{{ jsonError }}</p>
    </div>
  </div>
</template>

<style scoped>
.json-config-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mode-btn {
  font-size: 12px;
  color: var(--wf-accent);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}

.kv-table {
  width: 100%;
  border-collapse: collapse;
}

.kv-key {
  padding: 4px;
  color: var(--wf-text-2);
  font-size: 12.5px;
  font-family: var(--wf-font-mono);
  white-space: nowrap;
  vertical-align: top;
}

.kv-value {
  padding: 4px;
  width: 100%;
}

.kv-input {
  width: 100%;
  box-sizing: border-box;
  padding: 5px 8px;
  font-size: 12.5px;
  color: var(--wf-text);
  background: var(--wf-input);
  border: 1px solid var(--wf-border);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.15s;
}

.kv-input:focus {
  border-color: var(--wf-accent);
}

.kv-actions {
  padding: 4px 0 4px 4px;
  vertical-align: top;
}

.remove-btn {
  padding: 4px 6px;
  font-size: 11px;
  line-height: 1;
  color: var(--wf-text-3);
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.12s, background 0.12s;
}

.remove-btn:hover {
  color: var(--wf-danger);
  background: var(--wf-danger-bg);
}

/* 新增欄位列：讓 config 為空的節點也能在表格模式建立欄位，不必切到 JSON 模式手打 */
.add-field {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.add-btn {
  flex-shrink: 0;
  padding: 5px 12px;
  font-size: 12px;
  font-weight: 700;
  font-family: inherit;
  white-space: nowrap;
  color: var(--wf-text);
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.12s;
}

.add-btn:hover {
  border-color: var(--wf-text-3);
}

.field-error {
  margin: 6px 0 0;
  font-size: 11.5px;
  color: var(--wf-danger);
}

.raw-input {
  width: 100%;
  box-sizing: border-box;
  font-family: var(--wf-font-mono);
  font-size: 12px;
  padding: 8px;
  color: var(--wf-text);
  background: var(--wf-input);
  border: 1px solid var(--wf-border);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.15s;
}

.raw-input:focus {
  border-color: var(--wf-accent);
}

.json-error {
  color: var(--wf-danger);
  font-size: 12px;
  margin: 0;
}

.empty {
  color: var(--wf-text-3);
  font-size: 12px;
  margin: 0;
}
</style>
