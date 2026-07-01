import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const loadMcpOptions = vi.fn()
vi.mock('../../../../composables/useNodeOptions', () => ({
  useNodeOptions: () => ({ loadMcpOptions, loadLlmOptions: vi.fn(), loadToolOptions: vi.fn() }),
}))

import McpServerForm from '../McpServerForm.vue'

describe('McpServerForm', () => {
  beforeEach(() => {
    loadMcpOptions.mockReset()
    loadMcpOptions.mockResolvedValue([
      { value: 'm1', label: '天氣' },
      { value: 'm2', label: '檔案' },
    ])
  })

  it('掛載後渲染 MCP 下拉選項', async () => {
    const wrapper = mount(McpServerForm, { props: { config: {} } })
    await flushPromises()
    expect(wrapper.findAll('[data-test="mcp-select"] option').length).toBe(3)
    expect(wrapper.text()).toContain('天氣')
  })

  it('選取 mcpId 應 emit update:config 並保留未知鍵值', async () => {
    const wrapper = mount(McpServerForm, { props: { config: { foo: 1 } } })
    await flushPromises()
    await wrapper.find('[data-test="mcp-select"]').setValue('m2')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ foo: 1, mcpId: 'm2' })
  })
})
