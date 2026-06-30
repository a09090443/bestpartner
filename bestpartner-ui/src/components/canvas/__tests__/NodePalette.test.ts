import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import NodePalette from '../NodePalette.vue'
import { DRAG_NODE_TYPE_KEY } from '../dragKeys'

describe('NodePalette', () => {
  it('應列出 10 種可拖曳的節點型別', () => {
    const wrapper = mount(NodePalette)
    const items = wrapper.findAll('[data-test^="palette-item-"]')
    expect(items).toHaveLength(10)
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
})
