<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useNodeOptions } from '../../../composables/useNodeOptions'
import type { Option } from '../../../types/options'
import { parseJsonObjectField } from '../../../utils/json'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const llmOptions = ref<Option[]>([])

const llmId = ref<string>((props.config.llmId as string) ?? '')
const systemPrompt = ref<string>((props.config.systemPrompt as string) ?? '')
const userPrompt = ref<string>((props.config.userPrompt as string) ?? '')
const enableMemory = ref<boolean>((props.config.enableMemory as boolean) ?? false)
const memoryId = ref<string>((props.config.memoryId as string) ?? '')
const responseFormat = ref<string>((props.config.responseFormat as string) ?? 'TEXT')
const outputSchemaText = ref<string>(
  props.config.outputSchema ? JSON.stringify(props.config.outputSchema) : '',
)
const outputKey = ref<string>((props.config.outputKey as string) ?? '')

/** 已移除的欄位：工具/MCP/Skill/知識庫/檔案改由獨立節點連接到 in:tool 埠處理 */
const DEPRECATED_KEYS = [
  'toolIds',
  'toolSettingIds',
  'mcpIds',
  'mcpSettingIds',
  'skillIds',
  'knowledgeId',
  'files',
]

onMounted(async () => {
  const loaders = useNodeOptions()
  try {
    llmOptions.value = await loaders.loadLlmOptions()
  } catch {
    llmOptions.value = []
  }
})

/** 以不可變方式合併回 config，保留未知鍵值；空值鍵一律移除，維持 config 精簡 */
function emitConfig() {
  const next: Record<string, unknown> = { ...props.config, llmId: llmId.value }

  const setOrDelete = (key: string, value: unknown, keep: boolean) => {
    if (keep) next[key] = value
    else delete next[key]
  }

  setOrDelete('systemPrompt', systemPrompt.value, !!systemPrompt.value)
  setOrDelete('userPrompt', userPrompt.value, !!userPrompt.value)
  setOrDelete('enableMemory', true, enableMemory.value)
  setOrDelete('memoryId', memoryId.value, enableMemory.value && !!memoryId.value)

  const isJson = responseFormat.value === 'JSON'
  setOrDelete('responseFormat', 'JSON', isJson)
  // 僅合法 JSON「物件」才寫入；非物件與壞 JSON 一律不寫，避免後端 JsonObject 反序列化失敗
  const schema = isJson ? parseJsonObjectField(outputSchemaText.value) : null
  setOrDelete('outputSchema', schema, schema !== null)

  setOrDelete('outputKey', outputKey.value, !!outputKey.value)

  // 清除已移除欄位的殘留鍵，避免後端嚴格 JSON（ignoreUnknownKeys=false）解析失敗
  for (const key of DEPRECATED_KEYS) delete next[key]

  emit('update:config', next)
}
</script>

<template>
  <div class="form">
    <div class="field">
      <label>LLM 設定</label>
      <select v-model="llmId" data-test="llm-select" class="text-input" @change="emitConfig">
        <option value="">請選擇</option>
        <option v-for="opt in llmOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>
    <p class="hint" data-test="capability-hint">
      工具、MCP、Skill 改以獨立節點連接到本節點的「工具」輸入埠（in:tool），由 LLM 自主決定何時呼叫。
    </p>
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
    <div class="field">
      <label>使用者提示（選填，支援變數插值引用上游輸出）</label>
      <textarea
        v-model="userPrompt"
        data-test="user-prompt"
        class="text-input"
        rows="3"
        @input="emitConfig"
      />
    </div>
    <div class="field field-row">
      <label>
        <input
          v-model="enableMemory"
          data-test="enable-memory"
          type="checkbox"
          @change="emitConfig"
        />
        啟用 Memory
      </label>
    </div>
    <div v-if="enableMemory" class="field">
      <label>Memory ID（選填，留空則單次執行內共享；支援插值）</label>
      <input v-model="memoryId" data-test="memory-id" class="text-input" @input="emitConfig" />
    </div>
    <div class="field">
      <label>回應格式</label>
      <select
        v-model="responseFormat"
        data-test="response-format"
        class="text-input"
        @change="emitConfig"
      >
        <option value="TEXT">純文字（TEXT）</option>
        <option value="JSON">結構化 JSON</option>
      </select>
    </div>
    <div v-if="responseFormat === 'JSON'" class="field">
      <label>輸出 JSON Schema</label>
      <textarea
        v-model="outputSchemaText"
        data-test="output-schema"
        class="text-input"
        rows="4"
        @input="emitConfig"
      />
    </div>
    <div class="field">
      <label>輸出鍵名（選填，預設 reply）</label>
      <input v-model="outputKey" data-test="output-key" class="text-input" @input="emitConfig" />
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

.field-row label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.hint {
  margin: 0;
  padding: 8px 10px;
  font-size: 11px;
  line-height: 1.5;
  color: var(--wf-text-dim, #8a8a95);
  background: var(--wf-input, #0f0f13);
  border: 1px dashed var(--wf-border, #29292f);
  border-radius: 8px;
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
