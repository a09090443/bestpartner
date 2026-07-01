import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const loadToolOptions = vi.fn()
vi.mock('../../../../composables/useNodeOptions', () => ({
  useNodeOptions: () => ({ loadToolOptions, loadLlmOptions: vi.fn(), loadMcpOptions: vi.fn() }),
}))

import ToolForm from '../ToolForm.vue'

describe('ToolForm', () => {
  beforeEach(() => {
    loadToolOptions.mockReset()
    loadToolOptions.mockResolvedValue([
      { value: 't1', label: 'Google' },
      { value: 't2', label: 'Tavily' },
    ])
  })

  it('掛載後渲染工具下拉選項', async () => {
    const wrapper = mount(ToolForm, { props: { config: {} } })
    await flushPromises()
    expect(wrapper.findAll('[data-test="tool-select"] option').length).toBe(3)
    expect(wrapper.text()).toContain('Tavily')
  })

  it('選取 toolId 應 emit update:config 並保留未知鍵值', async () => {
    const wrapper = mount(ToolForm, { props: { config: { foo: 1 } } })
    await flushPromises()
    await wrapper.find('[data-test="tool-select"]').setValue('t1')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ foo: 1, toolId: 't1' })
  })
})
