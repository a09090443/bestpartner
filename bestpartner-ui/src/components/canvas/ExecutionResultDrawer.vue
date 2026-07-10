<script setup lang="ts">
import { useExecutionStore } from '../../stores/execution'

const store = useExecutionStore()
</script>

<template>
  <div v-if="store.finalStatus" class="result-drawer" data-test="result-drawer">
    <div class="drawer-header">
      <span class="drawer-title">執行結果</span>
      <span class="status" :class="`status-${store.finalStatus.toLowerCase()}`">{{ store.finalStatus }}</span>
      <button type="button" class="close-btn" data-test="result-close" @click="store.reset()">✕</button>
    </div>
    <p v-if="store.errorMessage" class="error-text">{{ store.errorMessage }}</p>
    <pre v-if="store.finalOutput" class="output-json">{{ JSON.stringify(store.finalOutput, null, 2) }}</pre>
  </div>
</template>

<style scoped>
.result-drawer {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  max-height: 40%;
  overflow: auto;
  padding: 12px 16px;
  background: var(--wf-surface-2, #17171c);
  border-top: 1px solid var(--wf-border, #29292f);
  z-index: 10;
}
.drawer-header { display: flex; align-items: center; gap: 10px; }
.drawer-title { font-weight: 700; font-size: 13px; color: var(--wf-text, #e7e7ec); }
.status { font-size: 11px; font-weight: 700; }
.status-success { color: #67c23a; }
.status-failed { color: #f56c6c; }
.status-cancelled { color: #e6a23c; }
.close-btn { margin-left: auto; background: none; border: none; color: var(--wf-text-dim, #8a8a95); cursor: pointer; }
.error-text { color: #f56c6c; font-size: 12px; }
.output-json { font-size: 12px; color: var(--wf-text, #e7e7ec); white-space: pre-wrap; }
</style>
