<script setup lang="ts">
import { ref } from 'vue'
import { useConfigSync } from '../../../composables/useConfigSync'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

/**
 * 觸發類型選項。值對齊後端 `enumerate/TriggerType`（MANUAL / WEBHOOK / CRON）。
 *
 * ⚠️ 目前只有 MANUAL 可用：`TriggerExecutor` 不區分類型，且引擎寫執行紀錄時固定記為 MANUAL
 * （`WorkflowEngine` 建立 execution 時寫死）。WEBHOOK / CRON 尚未有觸發來源實作，
 * 因此列出但停用，避免使用者選到一個存得起來、卻永遠不會被觸發的設定。
 * 後端支援後把 `disabled` 拿掉即可。
 */
const TRIGGER_TYPES: { value: string; label: string; disabled?: boolean }[] = [
  { value: 'MANUAL', label: '手動觸發（Manual）' },
  { value: 'WEBHOOK', label: 'Webhook（即將推出）', disabled: true },
  { value: 'CRON', label: '排程（Cron，即將推出）', disabled: true },
]

const triggerType = ref<string>((props.config.triggerType as string) ?? '')

// 外部（Node Designer / Inspector 的另一個實例）改動同一節點 config 時同步本地 ref
const { markSelfEmit } = useConfigSync(
  () => props.config,
  (cfg) => {
    triggerType.value = (cfg.triggerType as string) ?? ''
  },
)

/** 以不可變方式合併回 config，保留未知鍵值（如既有的 inputSchema） */
function emitConfig() {
  const next: Record<string, unknown> = { ...props.config }
  if (triggerType.value) next.triggerType = triggerType.value
  else delete next.triggerType
  markSelfEmit(next)
  emit('update:config', next)
}
</script>

<template>
  <div class="form">
    <div class="field">
      <label>觸發類型（必填）</label>
      <select
        v-model="triggerType"
        data-test="trigger-type"
        class="text-input"
        @change="emitConfig"
      >
        <option value="">請選擇</option>
        <option
          v-for="opt in TRIGGER_TYPES"
          :key="opt.value"
          :value="opt.value"
          :disabled="opt.disabled"
        >
          {{ opt.label }}
        </option>
      </select>
    </div>
    <p class="hint" data-test="trigger-hint">
      觸發節點是流程的起點，每張流程至少要有一個才能啟用。手動執行時，
      帶入的資料會成為本節點的輸出，下游可用
      <code>&#123;&#123;觸發節點key.欄位&#125;&#125;</code> 引用。
    </p>
  </div>
</template>

<!-- .form / .field / .field label / .text-input 由 styles/wf-form.css 共用 -->
<style scoped>
.hint {
  margin: 0;
  padding: 8px 10px;
  font-size: 11px;
  line-height: 1.5;
  color: var(--wf-text-2);
  background: var(--wf-input);
  border: 1px dashed var(--wf-border);
  border-radius: 8px;
}

.hint code {
  font-family: var(--wf-font-mono);
  font-size: 10.5px;
  color: var(--wf-text);
}
</style>
