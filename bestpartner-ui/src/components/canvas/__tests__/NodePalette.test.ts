import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import NodePalette from '../NodePalette.vue'
import { DRAG_NODE_TYPE_KEY } from '../dragKeys'
import { NODE_CATEGORIES } from '../../../constants/nodeTypes'

describe('NodePalette', () => {
  it('應列出 13 種可拖曳的節點型別', () => {
    const wrapper = mount(NodePalette)
    const items = wrapper.findAll('[data-test^="palette-item-"]')
    expect(items).toHaveLength(13)
  })

  it('應顯示各型別的 label（例如 TRIGGER → 觸發）', () => {
    const wrapper = mount(NodePalette)
    expect(wrapper.text()).toContain('觸發')
    expect(wrapper.text()).toContain('LLM 助手')
  })

  it('dragstart 應於 dataTransfer 設定 nodeType', async () => {
    const wrapper = mount(NodePalette)
    const setData = vi.fn()
    const item = wrapper.find('[data-test="palette-item-TRIGGER"]')

    await item.trigger('dragstart', {
      dataTransfer: { setData, effectAllowed: '' },
    })

    expect(setData).toHaveBeenCalledWith(DRAG_NODE_TYPE_KEY, 'TRIGGER')
  })

  it('應依 category 分組顯示，順序 Trigger → Action → AI → Logic', () => {
    const wrapper = mount(NodePalette)
    const groups = wrapper.findAll('[data-test^="palette-group-"]')
    expect(groups.map((g) => g.attributes('data-test'))).toEqual(
      NODE_CATEGORIES.map((c) => `palette-group-${c}`),
    )
  })

  it('搜尋應以 label 過濾（大小寫不敏感），空組不顯示', async () => {
    const wrapper = mount(NodePalette)
    await wrapper.find('[data-test="palette-search"]').setValue('llm')

    const items = wrapper.findAll('[data-test^="palette-item-"]')
    expect(items).toHaveLength(1)
    expect(items[0].attributes('data-test')).toBe('palette-item-LLM_ASSISTANT')

    // 僅剩 AI 分組
    const groups = wrapper.findAll('[data-test^="palette-group-"]')
    expect(groups.map((g) => g.attributes('data-test'))).toEqual(['palette-group-AI'])
  })

  it('搜尋無結果時顯示空狀態文字', async () => {
    const wrapper = mount(NodePalette)
    await wrapper.find('[data-test="palette-search"]').setValue('不存在的節點')
    expect(wrapper.findAll('[data-test^="palette-item-"]')).toHaveLength(0)
    expect(wrapper.text()).toContain('找不到符合的節點')
  })
})
