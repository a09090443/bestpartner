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

  it('編輯設定 ID 應 emit update:config 帶入 userSettingId（後端契約欄位）', async () => {
    const wrapper = mount(McpServerForm, { props: { config: { mcpId: 'm1' } } })
    await flushPromises()
    await wrapper.find('[data-test="mcp-setting-id"]').setValue('ms-7')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ mcpId: 'm1', userSettingId: 'ms-7' })
  })

  it('清空設定 ID 應自 config 移除 userSettingId 鍵', async () => {
    const wrapper = mount(McpServerForm, { props: { config: { mcpId: 'm1', userSettingId: 'ms-7' } } })
    await flushPromises()
    await wrapper.find('[data-test="mcp-setting-id"]').setValue('')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ mcpId: 'm1' })
  })

  it('編輯工具名稱應 emit update:config 帶入 toolName（啟用必填欄位）', async () => {
    const wrapper = mount(McpServerForm, { props: { config: { mcpId: 'm1' } } })
    await flushPromises()
    await wrapper.find('[data-test="tool-name"]').setValue('get_weather')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ mcpId: 'm1', toolName: 'get_weather' })
  })

  it('toolName 以既有 config 值初始化', () => {
    const wrapper = mount(McpServerForm, { props: { config: { mcpId: 'm1', toolName: 'read_file' } } })
    expect((wrapper.find('[data-test="tool-name"]').element as HTMLInputElement).value).toBe('read_file')
  })

  it('清空工具名稱應自 config 移除 toolName 鍵', async () => {
    const wrapper = mount(McpServerForm, { props: { config: { mcpId: 'm1', toolName: 'read_file' } } })
    await flushPromises()
    await wrapper.find('[data-test="tool-name"]').setValue('')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ mcpId: 'm1' })
  })

  it('編輯輸出鍵名應 emit update:config 帶入 outputKey，清空則移除', async () => {
    const wrapper = mount(McpServerForm, { props: { config: { mcpId: 'm1' } } })
    await flushPromises()
    await wrapper.find('[data-test="output-key"]').setValue('mcpResult')
    let emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ mcpId: 'm1', outputKey: 'mcpResult' })
    await wrapper.find('[data-test="output-key"]').setValue('')
    emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ mcpId: 'm1' })
  })

  it('舊欄位 mcpSettingId 讀取相容且 emit 時遷移為 userSettingId', async () => {
    const wrapper = mount(McpServerForm, { props: { config: { mcpId: 'm1', mcpSettingId: 'ms-legacy' } } })
    await flushPromises()
    // 舊值還原到輸入框
    expect((wrapper.find('[data-test="mcp-setting-id"]').element as HTMLInputElement).value).toBe('ms-legacy')
    // 任一編輯後 emit 的 config 不再含舊鍵（後端嚴格驗證會拒絕未知鍵）
    await wrapper.find('[data-test="mcp-select"]').setValue('m2')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ mcpId: 'm2', userSettingId: 'ms-legacy' })
  })

  describe('arguments 呼叫參數欄位', () => {
    it('輸入合法 JSON 物件 → emit 帶 parse 後物件', async () => {
      const wrapper = mount(McpServerForm, { props: { config: { mcpId: 'm1' } } })
      await flushPromises()
      await wrapper.find('[data-test="arguments"]').setValue('{"city": "{{input}}"}')
      const emitted = wrapper.emitted('update:config')!
      expect(emitted[emitted.length - 1][0]).toEqual({
        mcpId: 'm1',
        arguments: { city: '{{input}}' },
      })
    })

    it('清空 arguments → 自 config 刪鍵', async () => {
      const wrapper = mount(McpServerForm, {
        props: { config: { mcpId: 'm1', arguments: { a: 1 } } },
      })
      await flushPromises()
      await wrapper.find('[data-test="arguments"]').setValue('')
      const emitted = wrapper.emitted('update:config')!
      expect(emitted[emitted.length - 1][0]).toEqual({ mcpId: 'm1' })
    })

    it('輸入非法 JSON → 不寫入 arguments 鍵（不讓壞 JSON 進 config）', async () => {
      const wrapper = mount(McpServerForm, { props: { config: { mcpId: 'm1' } } })
      await flushPromises()
      await wrapper.find('[data-test="arguments"]').setValue('{ bad json')
      const emitted = wrapper.emitted('update:config')!
      const last = emitted[emitted.length - 1][0] as Record<string, unknown>
      expect(last).toEqual({ mcpId: 'm1' })
      expect(last).not.toHaveProperty('arguments')
    })

    it('由既有 config.arguments 初始化為格式化 JSON 文字', async () => {
      const wrapper = mount(McpServerForm, {
        props: { config: { mcpId: 'm1', arguments: { a: 1 } } },
      })
      await flushPromises()
      expect((wrapper.find('[data-test="arguments"]').element as HTMLTextAreaElement).value).toBe(
        JSON.stringify({ a: 1 }, null, 2),
      )
    })

    it('輸入合法但非物件的 JSON（如陣列）→ 不寫入 arguments 鍵', async () => {
      const wrapper = mount(McpServerForm, { props: { config: { mcpId: 'm1' } } })
      await flushPromises()
      await wrapper.find('[data-test="arguments"]').setValue('[1,2]')
      const emitted = wrapper.emitted('update:config')!
      const last = emitted[emitted.length - 1][0] as Record<string, unknown>
      expect(last).toEqual({ mcpId: 'm1' })
      expect(last).not.toHaveProperty('arguments')
    })
  })
})
