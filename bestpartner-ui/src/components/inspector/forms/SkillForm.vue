<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useNodeOptions } from '../../../composables/useNodeOptions'
import { useConfigSync } from '../../../composables/useConfigSync'
import type { Option } from '../../../types/options'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const options = ref<Option[]>([])
const skillId = ref<string>((props.config.skillId as string) ?? '')

// 外部（Node Designer / Inspector 的另一個實例）改動同一節點 config 時同步本地 ref
const { markSelfEmit } = useConfigSync(
  () => props.config,
  (cfg) => {
    skillId.value = (cfg.skillId as string) ?? ''
  },
)

onMounted(async () => {
  try {
    options.value = await useNodeOptions().loadSkillOptions()
  } catch {
    options.value = []
  }
})

/** 以不可變方式合併回 config，保留未知鍵值 */
function emitConfig() {
  const next: Record<string, unknown> = { ...props.config, skillId: skillId.value }
  markSelfEmit(next)
  emit('update:config', next)
}
</script>

<template>
  <div class="form">
    <div class="field">
      <label>Skill</label>
      <select v-model="skillId" data-test="skill-select" class="text-input" @change="emitConfig">
        <option value="">請選擇</option>
        <option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>
    <p class="hint" data-test="skill-hint">
      將本節點的輸出連接到 LLM 助手節點的「工具」輸入埠（in:tool），Skill 即掛載為該 LLM 可用能力。
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
</style>
