<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useNodeOptions } from '../../../composables/useNodeOptions'
import type { Option } from '../../../types/options'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const options = ref<Option[]>([])
const skillId = ref<string>((props.config.skillId as string) ?? '')

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
