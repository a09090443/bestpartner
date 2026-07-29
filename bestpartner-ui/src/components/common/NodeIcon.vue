<script setup lang="ts">
import { computed } from 'vue'
import { getNodeIconPaths } from '../../constants/nodeIcons'
import type { NodeType } from '../../types/workflow'

const props = defineProps<{
  type: NodeType
  /** 邊長（px），預設 18 */
  size?: number
}>()

const px = computed(() => props.size ?? 18)
const paths = computed(() => getNodeIconPaths(props.type))
</script>

<template>
  <!-- 刻意不寫 <style>：stroke 用 currentColor，顏色完全由父層 color 決定 -->
  <svg
    :width="px"
    :height="px"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.9"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    :data-icon="type"
  >
    <path v-for="(d, i) in paths" :key="i" :d="d" />
  </svg>
</template>
