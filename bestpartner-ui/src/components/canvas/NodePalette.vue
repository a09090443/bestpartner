<script setup lang="ts">
import { computed, ref } from 'vue'
import { NODE_CATEGORIES, getNodesByCategory } from '../../constants/nodeTypes'
import type { NodeCategory, NodeTypeMeta } from '../../constants/nodeTypes'
import type { NodeType } from '../../types/workflow'
import { DRAG_NODE_TYPE_KEY } from './dragKeys'

const search = ref('')

interface PaletteGroup {
  category: NodeCategory
  items: NodeTypeMeta[]
}

/** 依 category 分組並套用搜尋過濾（label 大小寫不敏感）；空組不顯示 */
const groups = computed<PaletteGroup[]>(() => {
  const keyword = search.value.trim().toLowerCase()
  return NODE_CATEGORIES.map((category) => ({
    category,
    items: getNodesByCategory(category).filter(
      (meta) => !keyword || meta.label.toLowerCase().includes(keyword),
    ),
  })).filter((group) => group.items.length > 0)
})

/** 型別色轉 16% 透明度背景（色碼皆為 6 位 hex，直接附加 alpha） */
function tint(color: string): string {
  return `${color}29`
}

function handleDragStart(event: DragEvent, type: NodeType) {
  if (!event.dataTransfer) return
  event.dataTransfer.setData(DRAG_NODE_TYPE_KEY, type)
  event.dataTransfer.effectAllowed = 'move'
}
</script>

<template>
  <div class="node-palette">
    <div class="palette-title">ADD NODE</div>

    <div class="search-box">
      <span class="search-icon" aria-hidden="true">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <circle cx="11" cy="11" r="7" />
          <line x1="21" y1="21" x2="16.5" y2="16.5" />
        </svg>
      </span>
      <input
        v-model="search"
        class="search-input"
        data-test="palette-search"
        type="text"
        placeholder="搜尋節點…"
      />
    </div>

    <div
      v-for="group in groups"
      :key="group.category"
      class="palette-group"
      :data-test="`palette-group-${group.category}`"
    >
      <div class="group-title">{{ group.category }}</div>
      <div
        v-for="meta in group.items"
        :key="meta.type"
        class="palette-item"
        :data-test="`palette-item-${meta.type}`"
        draggable="true"
        @dragstart="handleDragStart($event, meta.type)"
      >
        <span
          class="palette-icon"
          :style="{ backgroundColor: tint(meta.color), borderColor: meta.color, color: meta.color }"
        >{{ meta.icon }}</span>
        <span class="palette-label">{{ meta.label }}</span>
        <span class="palette-add" aria-hidden="true">+</span>
      </div>
    </div>

    <p v-if="groups.length === 0" class="palette-empty">找不到符合的節點</p>
  </div>
</template>

<style scoped>
.node-palette {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 12px;
  width: 250px;
  box-sizing: border-box;
  flex-shrink: 0;
  border-right: 1px solid var(--wf-border, #29292f);
  background: var(--wf-surface, #17171c);
  overflow-y: auto;
}

.palette-title {
  font-weight: 700;
  color: var(--wf-text-dim, #8a8a95);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 9px;
  display: flex;
  color: var(--wf-text-mute, #5c5c67);
  pointer-events: none;
}

.search-input {
  width: 100%;
  box-sizing: border-box;
  padding: 7px 10px 7px 28px;
  font-size: 12.5px;
  font-family: inherit;
  color: var(--wf-text, #e7e7ec);
  background: var(--wf-input, #0f0f13);
  border: 1px solid var(--wf-border, #29292f);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.15s;
}

.search-input::placeholder {
  color: var(--wf-text-mute, #5c5c67);
}

.search-input:focus {
  border-color: var(--wf-accent, #ff6a54);
}

.palette-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.group-title {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--wf-text-mute, #5c5c67);
  margin: 6px 0 2px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 8px;
  border-radius: 8px;
  cursor: grab;
  user-select: none;
  transition: background-color 0.12s;
}

.palette-item:hover {
  background: #212128;
}

.palette-item:hover .palette-add {
  opacity: 1;
}

.palette-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  font-size: 13px;
  border: 1px solid;
  border-radius: 7px;
}

.palette-label {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--wf-text, #e7e7ec);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.palette-add {
  font-size: 14px;
  color: var(--wf-text-mute, #5c5c67);
  opacity: 0;
  transition: opacity 0.12s;
}

.palette-empty {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--wf-text-mute, #5c5c67);
}
</style>
