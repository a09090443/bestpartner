<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useNodeOptions } from '../../../composables/useNodeOptions'
import type { Option } from '../../../types/options'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const llmOptions = ref<Option[]>([])
const toolOptions = ref<Option[]>([])
const mcpOptions = ref<Option[]>([])
const knowledgeOptions = ref<Option[]>([])
const skillOptions = ref<Option[]>([])

const llmId = ref<string>((props.config.llmId as string) ?? '')
const systemPrompt = ref<string>((props.config.systemPrompt as string) ?? '')
const userPrompt = ref<string>((props.config.userPrompt as string) ?? '')
const enableMemory = ref<boolean>((props.config.enableMemory as boolean) ?? false)
const memoryId = ref<string>((props.config.memoryId as string) ?? '')
const toolIds = ref<string[]>((props.config.toolIds as string[]) ?? [])
const toolSettingIdsText = ref<string>(((props.config.toolSettingIds as string[]) ?? []).join(', '))
const mcpIds = ref<string[]>((props.config.mcpIds as string[]) ?? [])
const mcpSettingIdsText = ref<string>(((props.config.mcpSettingIds as string[]) ?? []).join(', '))
const skillIds = ref<string[]>((props.config.skillIds as string[]) ?? [])
const knowledgeId = ref<string>((props.config.knowledgeId as string) ?? '')
const filesText = ref<string>(((props.config.files as string[]) ?? []).join(', '))
const responseFormat = ref<string>((props.config.responseFormat as string) ?? 'TEXT')
const outputSchemaText = ref<string>(
  props.config.outputSchema ? JSON.stringify(props.config.outputSchema) : '',
)
const outputKey = ref<string>((props.config.outputKey as string) ?? '')

onMounted(async () => {
  const loaders = useNodeOptions()
  const load = async (fn: () => Promise<Option[]>) => {
    try {
      return await fn()
    } catch {
      return []
    }
  }
  ;[
    llmOptions.value,
    toolOptions.value,
    mcpOptions.value,
    knowledgeOptions.value,
    skillOptions.value,
  ] = await Promise.all([
    load(loaders.loadLlmOptions),
    load(loaders.loadToolOptions),
    load(loaders.loadMcpOptions),
    load(loaders.loadKnowledgeOptions),
    load(loaders.loadSkillOptions),
  ])
})

/** 逗號分隔字串 -> 修剪後的非空片段陣列 */
function splitCsv(text: string): string[] {
  return text
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

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
  setOrDelete('toolIds', [...toolIds.value], toolIds.value.length > 0)
  const toolSettingIds = splitCsv(toolSettingIdsText.value)
  setOrDelete('toolSettingIds', toolSettingIds, toolSettingIds.length > 0)
  setOrDelete('mcpIds', [...mcpIds.value], mcpIds.value.length > 0)
  const mcpSettingIds = splitCsv(mcpSettingIdsText.value)
  setOrDelete('mcpSettingIds', mcpSettingIds, mcpSettingIds.length > 0)
  setOrDelete('skillIds', [...skillIds.value], skillIds.value.length > 0)
  setOrDelete('knowledgeId', knowledgeId.value, !!knowledgeId.value)
  const files = splitCsv(filesText.value)
  setOrDelete('files', files, files.length > 0)

  const isJson = responseFormat.value === 'JSON'
  setOrDelete('responseFormat', 'JSON', isJson)
  let schema: unknown = null
  if (isJson && outputSchemaText.value.trim()) {
    try {
      schema = JSON.parse(outputSchemaText.value)
    } catch {
      schema = null
    }
  }
  setOrDelete('outputSchema', schema, schema !== null)

  setOrDelete('outputKey', outputKey.value, !!outputKey.value)

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
      <label>工具（可複選）</label>
      <select
        v-model="toolIds"
        data-test="tool-ids"
        class="text-input"
        multiple
        @change="emitConfig"
      >
        <option v-for="opt in toolOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>
    <div class="field">
      <label>工具設定 ID（選填，逗號分隔；需 API key 的工具用此欄）</label>
      <input
        v-model="toolSettingIdsText"
        data-test="tool-setting-ids"
        class="text-input"
        @input="emitConfig"
      />
    </div>
    <div class="field">
      <label>MCP 伺服器（可複選）</label>
      <select
        v-model="mcpIds"
        data-test="mcp-ids"
        class="text-input"
        multiple
        @change="emitConfig"
      >
        <option v-for="opt in mcpOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>
    <div class="field">
      <label>MCP 設定 ID（選填，逗號分隔，執行期使用者設定）</label>
      <input
        v-model="mcpSettingIdsText"
        data-test="mcp-setting-ids"
        class="text-input"
        @input="emitConfig"
      />
    </div>
    <div class="field">
      <label>Skill（可複選）</label>
      <select
        v-model="skillIds"
        data-test="skill-ids"
        class="text-input"
        multiple
        @change="emitConfig"
      >
        <option v-for="opt in skillOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>
    <div class="field">
      <label>知識庫（選填，RAG 自動增強）</label>
      <select
        v-model="knowledgeId"
        data-test="knowledge-id"
        class="text-input"
        @change="emitConfig"
      >
        <option value="">不使用</option>
        <option v-for="opt in knowledgeOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>
    <div class="field">
      <label>附加檔案（選填，逗號分隔已上傳檔名；支援插值）</label>
      <input v-model="filesText" data-test="files" class="text-input" @input="emitConfig" />
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

select.text-input[multiple] {
  min-height: 72px;
}
</style>
