<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useNodeOptions } from '../../../composables/useNodeOptions'
import type { Option } from '../../../types/options'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const options = ref<Option[]>([])
const toolId = ref<string>((props.config.toolId as string) ?? '')

onMounted(async () => {
  try {
    options.value = await useNodeOptions().loadToolOptions()
  } catch {
    options.value = []
  }
})

function emitConfig() {
  emit('update:config', { ...props.config, toolId: toolId.value })
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
