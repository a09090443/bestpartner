<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { getTool, saveToolSetting, updateToolSetting } from '../../../api/tool'
import { useNodeOptions } from '../../../composables/useNodeOptions'
import type { Option } from '../../../types/options'
import type { ToolSettingSchema } from '../../../types/toolSchema'
import { parseJsonObjectField } from '../../../utils/json'
import SettingSchemaForm from './SettingSchemaForm.vue'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const options = ref<Option[]>([])
const toolId = ref<string>((props.config.toolId as string) ?? '')
const toolSettingId = ref<string>((props.config.toolSettingId as string) ?? '')
// 呼叫參數（JsonObject）：以美化縮排的 JSON 文字編輯，方便閱讀多行參數
const argumentsText = ref<string>(
  props.config.arguments ? JSON.stringify(props.config.arguments, null, 2) : '',
)

// 工具設定建立區塊狀態
const settingSchema = ref<ToolSettingSchema | null>(null)
const settingValues = ref<Record<string, unknown>>({})
const alias = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const isCreating = ref(false)
const expanded = ref(true)

/** schema 非 null 且至少有一個欄位才顯示建立區塊 */
const hasSchema = computed(
  () => !!settingSchema.value && Object.keys(settingSchema.value).length > 0,
)
/** 給 SettingSchemaForm 的非空 schema（hasSchema 保證非 null） */
const schemaForForm = computed<ToolSettingSchema>(() => settingSchema.value ?? {})

/** schema 中任一 required 欄位在 settingValues 缺席或為空（空字串/undefined/空陣列）即為 true */
const missingRequired = computed(() =>
  Object.entries(schemaForForm.value).some(([name, field]) => {
    if (!field.required) return false
    // boolean 的「必填」語意本就模糊：未勾選(false) 也是合法值，且初始鍵不存在時
    // 難以區分「未觸碰」與「刻意 false」。故 required boolean 不視為缺漏、不擋建立按鈕；
    // 僅對 string/integer/number/array 的 required 缺漏 disable。
    if (field.type === 'boolean') return false
    const v = settingValues.value[name]
    // 空陣列分支僅為防禦外部注入；UI 上 array 空輸入 emit undefined，走 undefined 路徑
    return v === undefined || v === '' || (Array.isArray(v) && v.length === 0)
  }),
)

onMounted(async () => {
  try {
    options.value = await useNodeOptions().loadToolOptions()
  } catch {
    options.value = []
  }
})

// 切換工具（含初始已有 toolId）時載入 schema，並重置區塊狀態
watch(
  toolId,
  async (id, oldId) => {
    settingSchema.value = null
    settingValues.value = {}
    alias.value = ''
    errorMsg.value = ''
    successMsg.value = ''
    expanded.value = true
    // settingId 語意上綁定工具，跨工具沿用必然無效；僅在使用者「實際切換」時清空，
    // 初始 immediate 載入（oldId 為 undefined）須保留自 config 還原的 toolSettingId
    if (oldId !== undefined && toolSettingId.value) {
      toolSettingId.value = ''
      emitConfig()
    }
    if (!id) return
    try {
      const tool = await getTool(id)
      // 期間使用者可能又切換了工具，避免過期回應覆寫
      if (toolId.value !== id) return
      settingSchema.value = tool.settingSchema ?? null
    } catch {
      // 舊請求晚到的失敗不得覆寫新工具已載入的 schema
      if (toolId.value !== id) return
      // getTool 失敗視同無 schema，不擋原有手動輸入 toolSettingId 的功能
      settingSchema.value = null
    }
  },
  { immediate: true },
)

/** 建立工具設定：saveToolSetting 取得 settingId 後寫入 settingContent，成功即自動帶入 toolSettingId */
async function createSetting() {
  if (!alias.value.trim() || isCreating.value) return
  // 快照建立當下的工具與設定值：pending 期間使用者可能切換工具（watch 會重置狀態），
  // 每個 await 之後以 toolId 比對快照，一旦切換即中止且不寫任何 UI 狀態
  const id = toolId.value
  const values = { ...settingValues.value }
  errorMsg.value = ''
  successMsg.value = ''
  isCreating.value = true
  try {
    const result = await saveToolSetting(id, alias.value.trim())
    if (toolId.value !== id) return
    const settingId = result.settingId
    if (!settingId) {
      // fail-fast：後端未回傳 settingId 時不得繼續寫入設定內容
      errorMsg.value = '建立失敗：後端未回傳 settingId'
      return
    }
    await updateToolSetting(settingId, JSON.stringify(values))
    if (toolId.value !== id) return
    toolSettingId.value = settingId
    emitConfig()
    successMsg.value = `已建立工具設定並帶入 ID：${settingId}`
    expanded.value = false
  } catch (e) {
    if (toolId.value !== id) return
    errorMsg.value = e instanceof Error ? e.message : '建立工具設定失敗'
  } finally {
    isCreating.value = false
  }
}

/** 手動編輯 toolSettingId：先前建立成功的提示（含帶入 ID）已與現值不符，須清除 */
function onToolSettingIdInput() {
  successMsg.value = ''
  emitConfig()
}

/** 以不可變方式合併回 config，保留未知鍵值 */
function emitConfig() {
  const next: Record<string, unknown> = { ...props.config, toolId: toolId.value }
  if (toolSettingId.value) next.toolSettingId = toolSettingId.value
  else delete next.toolSettingId
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
      <label>工具</label>
      <select v-model="toolId" data-test="tool-select" class="text-input" @change="emitConfig">
        <option value="">請選擇</option>
        <option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>
    <div class="field">
      <label>工具設定 ID（選填，執行期使用者設定）</label>
      <input
        v-model="toolSettingId"
        data-test="tool-setting-id"
        class="text-input"
        @input="onToolSettingIdInput"
      />
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
    <div v-if="hasSchema" class="setting-section" data-test="setting-section">
      <button
        type="button"
        class="section-toggle"
        data-test="setting-toggle"
        @click="expanded = !expanded"
      >
        {{ expanded ? '▾' : '▸' }} 建立工具設定
      </button>
      <div v-if="expanded" class="section-body">
        <div class="field">
          <label>設定別名（alias）</label>
          <input v-model="alias" data-test="setting-alias" class="text-input" />
        </div>
        <SettingSchemaForm v-model="settingValues" :schema="schemaForForm" />
        <button
          type="button"
          class="create-btn"
          data-test="setting-create"
          :disabled="!alias.trim() || missingRequired || isCreating"
          @click="createSetting"
        >
          {{ isCreating ? '建立中…' : '建立設定' }}
        </button>
      </div>
      <p v-if="errorMsg" class="setting-error" data-test="setting-error">{{ errorMsg }}</p>
      <p v-if="successMsg" class="setting-success" data-test="setting-success">{{ successMsg }}</p>
    </div>
  </div>
</template>

<!-- .form / .field / .field label / .text-input 由 styles/wf-form.css 共用 -->
<style scoped>
.setting-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  border: 1px solid var(--wf-border);
  border-radius: 8px;
}

.section-toggle {
  font-size: 12px;
  text-align: left;
  color: var(--wf-accent);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}

.section-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.create-btn {
  align-self: flex-start;
  padding: 6px 12px;
  font-size: 12.5px;
  font-family: inherit;
  color: var(--wf-text);
  background: var(--wf-input);
  border: 1px solid var(--wf-accent);
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.15s;
}

.create-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.setting-error {
  margin: 0;
  font-size: 11.5px;
  color: var(--wf-accent);
}

.setting-success {
  margin: 0;
  font-size: 11.5px;
  color: var(--wf-text-2);
}
</style>
