import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const loadLlmOptions = vi.fn()
vi.mock('../../../../composables/useNodeOptions', () => ({
  useNodeOptions: () => ({ loadLlmOptions, loadToolOptions: vi.fn(), loadMcpOptions: vi.fn() }),
}))

import LlmAssistantForm from '../LlmAssistantForm.vue'

describe('LlmAssistantForm', () => {
  beforeEach(() => {
    loadLlmOptions.mockReset()
    loadLlmOptions.mockResolvedValue([
      { value: 's1', label: 'GPT' },
      { value: 's2', label: 'Claude' },
    ])
  })

  it('掛載後以載入的 LLM 設定渲染下拉選項', async () => {
    const wrapper = mount(LlmAssistantForm, { props: { config: {} } })
    await flushPromises()
    const options = wrapper.findAll('[data-test="llm-select"] option')
    // 含一個空白 placeholder
    expect(options.length).toBe(3)
    expect(wrapper.text()).toContain('GPT')
    expect(wrapper.text()).toContain('Claude')
  })

  it('選取 llmId 應 emit update:config 帶入 llmId', async () => {
    const wrapper = mount(LlmAssistantForm, { props: { config: {} } })
    await flushPromises()
    await wrapper.find('[data-test="llm-select"]').setValue('s2')
    const emitted = wrapper.emitted('update:config')
    expect(emitted).toBeTruthy()
    expect(emitted![emitted!.length - 1][0]).toEqual({ llmId: 's2' })
  })

  it('編輯 systemPrompt 應 emit update:config 並保留既有 llmId', async () => {
    const wrapper = mount(LlmAssistantForm, { props: { config: { llmId: 's1' } } })
    await flushPromises()
    await wrapper.find('[data-test="system-prompt"]').setValue('你是助理')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ llmId: 's1', systemPrompt: '你是助理' })
  })

  it('保留 config 中的未知鍵值', async () => {
    const wrapper = mount(LlmAssistantForm, { props: { config: { foo: 1 } } })
    await flushPromises()
    await wrapper.find('[data-test="llm-select"]').setValue('s1')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ foo: 1, llmId: 's1' })
  })
})
