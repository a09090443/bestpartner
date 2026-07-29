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
  background: var(--wf-panel);
  border-top: 1px solid var(--wf-border);
  z-index: 10;
}
.drawer-header { display: flex; align-items: center; gap: 10px; }
.drawer-title { font-weight: 700; font-size: 13px; color: var(--wf-text); }
.status { font-size: 11px; font-weight: 700; }
.status-success { color: var(--wf-exec-success); }
.status-failed { color: var(--wf-exec-failed); }
.status-cancelled { color: var(--wf-exec-cancelled); }
.close-btn { margin-left: auto; background: none; border: none; color: var(--wf-text-2); cursor: pointer; }
.error-text { color: var(--wf-danger); font-size: 12px; }
.output-json { font-size: 12px; color: var(--wf-text); white-space: pre-wrap; }
</style>
