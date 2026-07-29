<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const prompt = ref<string>((props.config.prompt as string) ?? '')
const outputKey = ref<string>((props.config.outputKey as string) ?? '')

/** 以不可變方式合併回 config，保留未知鍵值；空值鍵一律移除，維持 config 精簡 */
function emitConfig() {
  const next: Record<string, unknown> = { ...props.config }

  const setOrDelete = (key: string, value: unknown, keep: boolean) => {
    if (keep) next[key] = value
    else delete next[key]
  }

  setOrDelete('prompt', prompt.value, !!prompt.value)
  setOrDelete('outputKey', outputKey.value, !!outputKey.value)

  emit('update:config', next)
}
</script>

<template>
  <div class="form">
    <div class="field">
      <label>提示詞（必填，支援變數插值引用上游輸出）</label>
      <textarea
        v-model="prompt"
        data-test="prompt-text"
        class="text-input"
        rows="6"
        @input="emitConfig"
      />
    </div>
    <p class="hint" data-test="prompt-hint">
      本節點輸出需連到 LLM 助手節點的「提示」輸入埠（in:prompt）作為該次推論的提問；
      未連接將無法啟用流程。多個提示節點可接到同一顆 LLM，搭配條件判斷分支即可切換不同對話。
    </p>
    <div class="field">
      <label>輸出鍵名（選填，預設 prompt）</label>
      <input v-model="outputKey" data-test="output-key" class="text-input" @input="emitConfig" />
    </div>
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
</style>
