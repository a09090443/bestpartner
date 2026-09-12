<script setup lang="ts">
/**
 * 插值運算式編輯器：把 `{{nodeKey.field}}` 顯示成人類看得懂的色塊（「LLM 助手 › reply」），
 * 但 **v-model 的值永遠是原始模板字串**——儲存格式不變，後端無感。
 *
 * 為什麼不用 textarea：textarea 只能顯示純文字，無法把機器識別碼換成可讀名稱。
 * 為什麼不引 CodeMirror：為單一欄位加約 200KB 依賴不划算，本元件約 120 行即可滿足需求。
 *
 * ⚠️ contenteditable 的兩個經典地雷，處理方式如下：
 * 1. **中文輸入法**：組字期間若重繪 DOM 會吃掉字。故 `isComposing` 期間完全不 emit、不重繪，
 *    等 `compositionend` 再同步一次。
 * 2. **外部值回寫造成游標跳動**：只有當外部傳入的值與「本元件最後讀出的值」不同時才重繪
 *    （`lastEmitted` 比對），使用者自己打字造成的 v-model 更新不會觸發重繪。
 */
import { onMounted, ref, watch } from 'vue'
import {
  ZWSP,
  formatRefLabel,
  isKnownRef,
  parseTemplate,
  serializeNodes,
} from '../../utils/expression'

const props = defineProps<{
  modelValue: string
  /** nodeKey → 顯示名稱；查不到的引用會標示為未知 */
  refLabels?: Record<string, string>
  placeholder?: string
  /** 內容區最小高度（以行計），預設 4 */
  rows?: number
}>()

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const editorEl = ref<HTMLElement | null>(null)
const isComposing = ref(false)
/** 本元件最後一次 emit 出去的值，用來判斷外部變更是否需要重繪 */
let lastEmitted = ''

const labels = () => props.refLabels ?? {}

/** 依模板字串重建 DOM（唯一會動 DOM 結構的地方） */
function render() {
  const el = editorEl.value
  if (!el) return
  el.textContent = ''
  for (const seg of parseTemplate(props.modelValue)) {
    if (seg.kind === 'text') {
      // 文字內的換行交給 white-space: pre-wrap 呈現
      el.appendChild(document.createTextNode(seg.value))
    } else {
      el.appendChild(createToken(seg.value, seg.path))
    }
  }
}

/** 建立不可分割的引用色塊；刪除時整塊消失（contenteditable=false） */
function createToken(raw: string, path: string): HTMLElement {
  const span = document.createElement('span')
  span.className = isKnownRef(path, labels()) ? 'expr-token' : 'expr-token is-unknown'
  span.setAttribute('contenteditable', 'false')
  span.setAttribute('data-ref', raw)
  span.setAttribute('data-test', `expr-token-${path}`)
  span.textContent = formatRefLabel(path, labels())
  span.title = isKnownRef(path, labels()) ? raw : `${raw}（找不到對應節點）`
  return span
}

function readAndEmit() {
  const el = editorEl.value
  if (!el) return
  const value = serializeNodes(el)
  lastEmitted = value
  emit('update:modelValue', value)
}

function onInput() {
  // 組字中不回報，避免中途的注音／拼音被當成正式內容寫進 config
  if (isComposing.value) return
  readAndEmit()
}

function onCompositionEnd() {
  isComposing.value = false
  readAndEmit()
}

/** 貼上一律轉純文字，避免把外部 HTML 結構帶進編輯區 */
function onPaste(event: ClipboardEvent) {
  event.preventDefault()
  const text = event.clipboardData?.getData('text/plain') ?? ''
  insertNode(document.createTextNode(text))
  readAndEmit()
}

/** Enter 插入 <br>，避免不同瀏覽器自行包 <div>/<p> 造成結構不一致 */
function onKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.shiftKey) return
  event.preventDefault()
  insertNode(document.createElement('br'))
  // 換行後補一個 ZWSP，否則游標可能停在 <br> 之前看不出換行
  insertNode(document.createTextNode(ZWSP))
  readAndEmit()
}

/** 在游標處插入節點；取不到選取範圍（如尚未聚焦）時附加到結尾 */
function insertNode(node: Node) {
  const el = editorEl.value
  if (!el) return
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0 || !el.contains(sel.anchorNode)) {
    el.appendChild(node)
    return
  }
  const range = sel.getRangeAt(0)
  range.deleteContents()
  range.insertNode(node)
  range.setStartAfter(node)
  range.collapse(true)
  sel.removeAllRanges()
  sel.addRange(range)
}

/** 供父層呼叫：於游標處插入一個引用色塊 */
function insertRef(raw: string, path: string) {
  editorEl.value?.focus()
  insertNode(createToken(raw, path))
  // token 後補 ZWSP，讓游標有落腳處（否則色塊在結尾時無法接著打字）
  insertNode(document.createTextNode(ZWSP))
  readAndEmit()
}

defineExpose({ insertRef })

watch(
  () => props.modelValue,
  (value) => {
    // 只有外部變更（非本元件 emit 的回流）才重繪，否則會打斷輸入與游標
    if (value === lastEmitted) return
    if (isComposing.value) return
    render()
  },
)

// refLabels 變動（如上游節點改名）時重繪，讓色塊文字跟著更新
watch(
  () => props.refLabels,
  () => {
    if (!isComposing.value) render()
  },
  { deep: true },
)

onMounted(render)
</script>

<template>
  <div
    ref="editorEl"
    class="expr-editor"
    :class="{ 'is-empty': !props.modelValue }"
    contenteditable="true"
    role="textbox"
    aria-multiline="true"
    :data-placeholder="props.placeholder ?? ''"
    :style="{ minHeight: `${(props.rows ?? 4) * 1.6}em` }"
    @input="onInput"
    @paste="onPaste"
    @keydown="onKeydown"
    @compositionstart="isComposing = true"
    @compositionend="onCompositionEnd"
  />
</template>

<style scoped>
.expr-editor {
  width: 100%;
  box-sizing: border-box;
  padding: 6px 8px;
  font-size: 12.5px;
  line-height: 1.6;
  font-family: inherit;
  color: var(--wf-text);
  background: var(--wf-input);
  border: 1px solid var(--wf-border);
  border-radius: 8px;
  outline: none;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-y: auto;
  transition: border-color 0.15s;
}

.expr-editor:focus {
  border-color: var(--wf-accent);
}

.expr-editor.is-empty::before {
  content: attr(data-placeholder);
  color: var(--wf-text-3);
  pointer-events: none;
}

/* 引用色塊：整塊不可編輯，刪除時一次消失 */
.expr-editor :deep(.expr-token) {
  display: inline-block;
  margin: 0 1px;
  padding: 0 6px;
  font-family: var(--wf-font-mono);
  font-size: 11px;
  line-height: 1.65;
  color: var(--wf-accent);
  background: color-mix(in srgb, var(--wf-accent) 12%, transparent);
  border: 1px solid color-mix(in srgb, var(--wf-accent) 32%, transparent);
  border-radius: 5px;
  user-select: none;
  cursor: default;
}

/* 找不到對應節點（節點被刪除，或手打錯字）——這正是 `{{llm_reply}}` 那類錯誤的樣子 */
.expr-editor :deep(.expr-token.is-unknown) {
  color: var(--wf-danger);
  background: var(--wf-danger-bg);
  border-color: var(--wf-danger-border);
  border-style: dashed;
}
</style>
