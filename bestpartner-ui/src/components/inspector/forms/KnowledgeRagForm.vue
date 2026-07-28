<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useNodeOptions } from '../../../composables/useNodeOptions'
import type { Option } from '../../../types/options'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const options = ref<Option[]>([])
const embeddingOptions = ref<Option[]>([])
const knowledgeId = ref<string>((props.config.knowledgeId as string) ?? '')
const embeddingModelId = ref<string>((props.config.embeddingModelId as string) ?? '')
const query = ref<string>((props.config.query as string) ?? '')
// v-model.number 清空時值為空字串，故型別為 number | ''；新節點無 topK 時預設顯示 4
const topK = ref<number | ''>((props.config.topK as number) ?? 4)
const minScore = ref<number | ''>((props.config.minScore as number) ?? '')
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
  ;[options.value, embeddingOptions.value] = await Promise.all([
    load(loaders.loadKnowledgeOptions),
    load(loaders.loadEmbeddingOptions),
  ])
})

/** 以不可變方式合併回 config，保留未知鍵值；新欄位空值刪鍵（DRAFT 允許缺席，啟用時後端才擋） */
function emitConfig() {
  const next: Record<string, unknown> = {
    ...props.config,
    knowledgeId: knowledgeId.value,
  }
  // topK 清空時為空字串，送出 topK:"" 會令後端 strictJson 解 Int? 失敗；空值刪鍵
  if (typeof topK.value === 'number') next.topK = topK.value
  else delete next.topK
  if (embeddingModelId.value) next.embeddingModelId = embeddingModelId.value
  else delete next.embeddingModelId
  if (query.value) next.query = query.value
  else delete next.query
  if (typeof minScore.value === 'number') next.minScore = minScore.value
  else delete next.minScore
  if (outputKey.value) next.outputKey = outputKey.value
  else delete next.outputKey
  emit('update:config', next)
}
</script>

<template>
  <div class="form">
    <div class="field">
      <label>知識庫</label>
      <select
        v-model="knowledgeId"
        data-test="knowledge-id"
        class="text-input"
        @change="emitConfig"
      >
        <option value="">請選擇</option>
        <option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>
    <div class="field">
      <label>Embedding 模型（pipeline 檢索用；作為 LLM 外掛時由知識庫本身設定決定，可留空）</label>
      <select
        v-model="embeddingModelId"
        data-test="embedding-model-select"
        class="text-input"
        @change="emitConfig"
      >
        <option value="">請選擇</option>
        <option v-for="opt in embeddingOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>
    <div class="field">
      <label>檢索語句（pipeline 檢索必填，支援變數插值；作為 LLM 外掛時免填，由 LLM 問題自動帶入）</label>
      <textarea v-model="query" data-test="query" class="text-input" rows="3" @input="emitConfig" />
    </div>
    <div class="field">
      <label>取回筆數（topK）</label>
      <input
        v-model.number="topK"
        data-test="topk"
        type="number"
        min="1"
        class="text-input"
        @input="emitConfig"
      />
    </div>
    <div class="field">
      <label>相似度下限（選填，0–1）</label>
      <input
        v-model.number="minScore"
        data-test="min-score"
        type="number"
        min="0"
        max="1"
        step="0.05"
        class="text-input"
        @input="emitConfig"
      />
    </div>
    <div class="field">
      <label>輸出鍵名（選填）</label>
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
