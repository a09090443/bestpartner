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
    <table v-if="mode === 'kv'" class="kv-table">
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
        </tr>
      </tbody>
    </table>
    <p v-if="mode === 'kv' && Object.keys(modelValue).length === 0" class="empty">無設定欄位</p>

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
  color: #409eff;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.kv-table {
  width: 100%;
  border-collapse: collapse;
}

.kv-key {
  padding: 4px;
  color: #606266;
  font-size: 13px;
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
  padding: 4px 6px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.raw-input {
  width: 100%;
  box-sizing: border-box;
  font-family: monospace;
  font-size: 12px;
  padding: 6px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.json-error {
  color: #f56c6c;
  font-size: 12px;
  margin: 0;
}

.empty {
  color: #c0c4cc;
  font-size: 12px;
  margin: 0;
}
</style>
