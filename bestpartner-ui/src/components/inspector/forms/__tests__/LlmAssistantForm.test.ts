import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const loadLlmOptions = vi.fn()
vi.mock('../../../../composables/useNodeOptions', () => ({
  useNodeOptions: () => ({ loadLlmOptions }),
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

  it('編輯 userPrompt 應 emit update:config 帶入 userPrompt', async () => {
    const wrapper = mount(LlmAssistantForm, { props: { config: { llmId: 's1' } } })
    await flushPromises()
    await wrapper.find('[data-test="user-prompt"]').setValue('請回覆：{{trigger.userMessage}}')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({
      llmId: 's1',
      userPrompt: '請回覆：{{trigger.userMessage}}',
    })
  })

  it('勾選 enableMemory 並填 memoryId 應 emit 兩欄位；取消勾選則兩者移除', async () => {
    const wrapper = mount(LlmAssistantForm, { props: { config: { llmId: 's1' } } })
    await flushPromises()
    await wrapper.find('[data-test="enable-memory"]').setValue(true)
    await wrapper.find('[data-test="memory-id"]').setValue('{{trigger.sessionId}}')
    let emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({
      llmId: 's1',
      enableMemory: true,
      memoryId: '{{trigger.sessionId}}',
    })
    await wrapper.find('[data-test="enable-memory"]').setValue(false)
    emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ llmId: 's1' })
  })

  it('responseFormat 預設 TEXT 不 emit；選 JSON 才顯示 outputSchema 並 emit', async () => {
    const wrapper = mount(LlmAssistantForm, { props: { config: { llmId: 's1' } } })
    await flushPromises()
    expect(wrapper.find('[data-test="output-schema"]').exists()).toBe(false)
    await wrapper.find('[data-test="response-format"]').setValue('JSON')
    expect(wrapper.find('[data-test="output-schema"]').exists()).toBe(true)
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ llmId: 's1', responseFormat: 'JSON' })
  })

  it('outputSchema 為合法 JSON 時 emit 解析後物件；非法 JSON 不 emit 該鍵', async () => {
    const wrapper = mount(LlmAssistantForm, { props: { config: { llmId: 's1' } } })
    await flushPromises()
    await wrapper.find('[data-test="response-format"]').setValue('JSON')
    await wrapper.find('[data-test="output-schema"]').setValue('{"type":"object"}')
    let emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({
      llmId: 's1',
      responseFormat: 'JSON',
      outputSchema: { type: 'object' },
    })
    await wrapper.find('[data-test="output-schema"]').setValue('{oops')
    emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ llmId: 's1', responseFormat: 'JSON' })
  })

  it('outputSchema 為合法但非物件的 JSON（如陣列）時不 emit 該鍵', async () => {
    const wrapper = mount(LlmAssistantForm, { props: { config: { llmId: 's1' } } })
    await flushPromises()
    await wrapper.find('[data-test="response-format"]').setValue('JSON')
    await wrapper.find('[data-test="output-schema"]').setValue('[1,2]')
    const emitted = wrapper.emitted('update:config')
    const last = emitted![emitted!.length - 1][0] as Record<string, unknown>
    expect(last).toEqual({ llmId: 's1', responseFormat: 'JSON' })
    expect(last).not.toHaveProperty('outputSchema')
  })

  it('編輯 outputKey 應 emit update:config 帶入 outputKey', async () => {
    const wrapper = mount(LlmAssistantForm, { props: { config: { llmId: 's1' } } })
    await flushPromises()
    await wrapper.find('[data-test="output-key"]').setValue('reply')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ llmId: 's1', outputKey: 'reply' })
  })

  it('編輯時清除已移除的欄位殘留鍵（toolIds/mcpIds/skillIds/knowledgeId/files）', async () => {
    const wrapper = mount(LlmAssistantForm, {
      props: {
        config: {
          llmId: 's1',
          toolIds: ['t1'],
          toolSettingIds: ['ts1'],
          mcpIds: ['m1'],
          mcpSettingIds: ['ms1'],
          skillIds: ['sk1'],
          knowledgeId: 'k1',
          files: ['a.png'],
        },
      },
    })
    await flushPromises()
    await wrapper.find('[data-test="output-key"]').setValue('reply')
    const emitted = wrapper.emitted('update:config')
    // 已移除欄位一律被剝除，避免後端嚴格 JSON 解析失敗
    expect(emitted![emitted!.length - 1][0]).toEqual({ llmId: 's1', outputKey: 'reply' })
  })

  it('由既有 config 還原保留的欄位值', async () => {
    const wrapper = mount(LlmAssistantForm, {
      props: {
        config: {
          llmId: 's1',
          userPrompt: 'hi',
          enableMemory: true,
          memoryId: 'mem1',
          responseFormat: 'JSON',
          outputSchema: { type: 'object' },
          outputKey: 'reply',
        },
      },
    })
    await flushPromises()
    expect((wrapper.find('[data-test="user-prompt"]').element as HTMLTextAreaElement).value).toBe(
      'hi',
    )
    expect(
      (wrapper.find('[data-test="enable-memory"]').element as HTMLInputElement).checked,
    ).toBe(true)
    expect((wrapper.find('[data-test="memory-id"]').element as HTMLInputElement).value).toBe(
      'mem1',
    )
    expect(
      (wrapper.find('[data-test="response-format"]').element as HTMLSelectElement).value,
    ).toBe('JSON')
    expect(
      (wrapper.find('[data-test="output-schema"]').element as HTMLTextAreaElement).value,
    ).toBe('{"type":"object"}')
    expect((wrapper.find('[data-test="output-key"]').element as HTMLInputElement).value).toBe(
      'reply',
    )
  })
})
