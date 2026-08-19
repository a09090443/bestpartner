<script setup lang="ts">
import { computed, ref } from 'vue'
import ExpressionEditor from '../../common/ExpressionEditor.vue'
import { useConfigSync } from '../../../composables/useConfigSync'
import type { UpstreamRef } from '../../../constants/nodeOutputKeys'
import { refFieldPath } from '../../../utils/expression'

const props = defineProps<{
  config: Record<string, unknown>
  /**
   * 圖上其他節點的可引用欄位建議（由編輯器依畫布計算後傳入）。
   * 沒有這個，使用者得自己去別處挖出節點的 nanoid 才拼得出插值字串。
   */
  upstreamRefs?: UpstreamRef[]
  /**
   * nodeKey → 顯示名稱。讓模板裡的 `{{Mr7gjxEL.reply}}` 能渲染成「LLM 助手 › reply」，
   * 查不到的引用則標成紅色虛線，當場看出打錯或節點已被刪。
   */
  refLabels?: Record<string, string>
}>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const template = ref<string>((props.config.template as string) ?? '')
// mappings 以每行 key=value 編輯
// mappings 以每行 key=value 編輯（mappingsToText 為函式宣告，hoisting 使其可在此使用）
const mappingsText = ref<string>(
  mappingsToText(props.config.mappings as Record<string, string> | undefined),
)

const templateEl = ref<InstanceType<typeof ExpressionEditor> | null>(null)

/** mappings 物件 → 每行 `key=value` 的編輯文字（初始化與外部同步共用） */
function mappingsToText(mappings: Record<string, string> | undefined): string {
  return Object.entries(mappings ?? {})
    .map(([k, v]) => `${k}=${v}`)
    .join('\n')
}

// 外部（Node Designer / Inspector 的另一個實例）改動同一節點 config 時同步本地 ref。
// 自身 emit 造成的回流已由 useConfigSync 以物件參考比對略過，
// 故 contenteditable 的 template 不會在使用者打字（含中文組字）期間被重灌。
const { markSelfEmit } = useConfigSync(
  () => props.config,
  (cfg) => {
    template.value = (cfg.template as string) ?? ''
    mappingsText.value = mappingsToText(cfg.mappings as Record<string, string> | undefined)
  },
)

function parseMappings(text: string): Record<string, string> {
  const result: Record<string, string> = {}
  text.split('\n').forEach((line) => {
    const idx = line.indexOf('=')
    if (idx > 0) result[line.slice(0, idx).trim()] = line.slice(idx + 1).trim()
  })
  return result
}

/** template 與 mappings 擇一即可；兩者皆空才是未滿足必填 */
const bothEmpty = computed(
  () => !template.value.trim() && Object.keys(parseMappings(mappingsText.value)).length === 0,
)

const hasSuggestions = computed(() => (props.upstreamRefs ?? []).some((r) => r.refs.length > 0))

function emitConfig() {
  const next: Record<string, unknown> = { ...props.config }
  if (template.value) next.template = template.value
  else delete next.template
  const mappings = parseMappings(mappingsText.value)
  if (Object.keys(mappings).length > 0) next.mappings = mappings
  else delete next.mappings
  markSelfEmit(next)
  emit('update:config', next)
}

/** 由完整引用字串取出括號內的路徑，如 `{{abc.reply}}` → `abc.reply` */
function pathOf(ref: string): string {
  return ref.replace(/^\{\{\s*|\s*\}\}$/g, '')
}

/** chip 上顯示的欄位名（路徑去掉節點 key 那一段） */
function fieldOf(ref: string): string {
  return refFieldPath(pathOf(ref)) || pathOf(ref)
}

/** 於游標處插入引用色塊；實際的 DOM 操作與值回報交給 ExpressionEditor */
function insertRef(ref: string) {
  templateEl.value?.insertRef(ref, pathOf(ref))
}

/** ExpressionEditor 回報的永遠是原始模板字串，直接寫回 config */
function onTemplateUpdate(value: string) {
  template.value = value
  emitConfig()
}
</script>

<template>
  <div class="form">
    <div class="field">
      <!-- v-pre 避免 {{變數}} 被 Vue 當插值 -->
      <label v-pre>輸出模板（與欄位對映擇一必填，可引用上游輸出，結果放 result 鍵）</label>
      <ExpressionEditor
        ref="templateEl"
        :model-value="template"
        :ref-labels="props.refLabels"
        :rows="4"
        placeholder="可直接打字，或點下方色塊插入上游節點的輸出"
        data-test="output-template"
        @update:model-value="onTemplateUpdate"
      />
    </div>

    <!-- 可引用欄位：點一下插入，省去自行查節點 key -->
    <div v-if="hasSuggestions" class="refs" data-test="output-ref-suggestions">
      <div class="refs-title">可引用的上游輸出（點擊插入模板）</div>
      <div v-for="src in props.upstreamRefs" :key="src.nodeId" class="ref-group">
        <!-- 顯示格式與編輯區的色塊一致，讓「看到的」與「插進去的」對得起來；
             原始的 {{nodeKey.field}} 只放在 title，需要時才查得到 -->
        <button
          v-for="r in src.refs"
          :key="r"
          type="button"
          class="ref-chip"
          :data-test="`output-ref-${r}`"
          :title="`插入 ${r}`"
          @click="insertRef(r)"
        >
          {{ src.name }} › {{ fieldOf(r) }}
        </button>
      </div>
    </div>

    <div class="field">
      <label>欄位對映（與輸出模板擇一必填，每行 key=value，value 支援插值）</label>
      <textarea
        v-model="mappingsText"
        data-test="output-mappings"
        class="text-input"
        rows="3"
        @input="emitConfig"
      />
    </div>

    <p v-if="bothEmpty" class="both-empty" data-test="output-both-empty">
      輸出模板與欄位對映至少要填一個，否則流程執行到本節點會失敗。
    </p>
  </div>
</template>

<!-- .form / .field / .field label / .text-input 沿用 styles/wf-form.css -->
<style scoped>
.refs {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 9px 10px;
  background: var(--wf-input);
  border: 1px dashed var(--wf-border);
  border-radius: 8px;
}

.refs-title {
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--wf-text-3);
}

.ref-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
}

.ref-chip {
  padding: 2px 8px;
  font-family: var(--wf-font-mono);
  font-size: 10.5px;
  color: var(--wf-text);
  background: var(--wf-card);
  border: 1px solid var(--wf-border);
  border-radius: 999px;
  cursor: pointer;
  transition: border-color 0.12s, color 0.12s;
}

.ref-chip:hover {
  color: var(--wf-accent);
  border-color: var(--wf-accent);
}

.both-empty {
  margin: 0;
  padding: 8px 10px;
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--wf-warning);
  background: var(--wf-warning-bg);
  border: 1px solid var(--wf-warning-border);
  border-radius: 8px;
}
</style>
