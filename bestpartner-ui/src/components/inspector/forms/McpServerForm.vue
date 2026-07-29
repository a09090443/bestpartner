<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useNodeOptions } from '../../../composables/useNodeOptions'
import type { Option } from '../../../types/options'
import { parseJsonObjectField } from '../../../utils/json'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const options = ref<Option[]>([])
const mcpId = ref<string>((props.config.mcpId as string) ?? '')
// 後端契約欄位為 userSettingId（McpServerNodeConfig）；mcpSettingId 為舊版欄位名，讀取時相容
const userSettingId = ref<string>(
  ((props.config.userSettingId as string) ?? (props.config.mcpSettingId as string)) ?? '',
)
// MCP 工具清單為執行期才能發現，後端無查詢端點，v1 以文字輸入
const toolName = ref<string>((props.config.toolName as string) ?? '')
const outputKey = ref<string>((props.config.outputKey as string) ?? '')
// 呼叫參數（JsonObject）：以美化縮排的 JSON 文字編輯，方便閱讀多行參數
const argumentsText = ref<string>(
  props.config.arguments ? JSON.stringify(props.config.arguments, null, 2) : '',
)

onMounted(async () => {
  try {
    options.value = await useNodeOptions().loadMcpOptions()
  } catch {
    options.value = []
  }
})

/** 以不可變方式合併回 config，保留未知鍵值；舊欄位 mcpSettingId 一律移除（後端嚴格驗證會拒絕未知鍵） */
function emitConfig() {
  const next: Record<string, unknown> = { ...props.config, mcpId: mcpId.value }
  delete next.mcpSettingId
  if (userSettingId.value) next.userSettingId = userSettingId.value
  else delete next.userSettingId
  // 啟用必填欄位；空值仍刪鍵（DRAFT 允許缺席，啟用時後端才擋）
  if (toolName.value) next.toolName = toolName.value
  else delete next.toolName
  if (outputKey.value) next.outputKey = outputKey.value
  else delete next.outputKey
  applyArguments(next)
  emit('update:config', next)
}

/**
 * 將 arguments 文字合併回 config：僅合法 JSON「物件」才寫入，否則刪鍵。
 * 非物件（陣列/字串/數字等）與壞 JSON 一律不寫入，避免後端 JsonObject 反序列化失敗。
 */
function applyArguments(next: Record<string, unknown>) {
  const args = parseJsonObjectField(argumentsText.value)
  if (args !== null) next.arguments = args
  else delete next.arguments
}
</script>

<template>
  <div class="form">
    <div class="field">
      <label>MCP 伺服器</label>
      <select v-model="mcpId" data-test="mcp-select" class="text-input" @change="emitConfig">
        <option value="">請選擇</option>
        <option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>
    <div class="field">
      <label>工具名稱（必填，MCP 伺服器提供的 tool name）</label>
      <input v-model="toolName" data-test="tool-name" class="text-input" @input="emitConfig" />
    </div>
    <div class="field">
      <label>MCP 設定 ID（選填，執行期使用者設定）</label>
      <input
        v-model="userSettingId"
        data-test="mcp-setting-id"
        class="text-input"
        @input="emitConfig"
      />
    </div>
    <div class="field">
      <label>輸出鍵名（選填）</label>
      <input v-model="outputKey" data-test="output-key" class="text-input" @input="emitConfig" />
    </div>
    <div class="field">
      <!-- v-pre 避免 {{變數}} 被 Vue 當作插值運算式 -->
      <label v-pre>呼叫參數（選填，JSON 物件，值支援 {{變數}} 插值）</label>
      <textarea
        v-model="argumentsText"
        data-test="arguments"
        class="text-input"
        rows="3"
        @input="emitConfig"
      />
    </div>
  </div>
</template>

<!-- 樣式全部沿用 styles/wf-form.css 的共用規則，本元件無獨有樣式 -->
