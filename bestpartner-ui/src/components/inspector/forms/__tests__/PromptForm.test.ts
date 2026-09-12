import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PromptForm from '../PromptForm.vue'

describe('PromptForm', () => {
  it('由既有 config 還原欄位值', () => {
    const wrapper = mount(PromptForm, {
      props: { config: { prompt: '以正式語氣回答', outputKey: 'askText' } },
    })
    expect(
      (wrapper.find('[data-test="prompt-text"]').element as HTMLTextAreaElement).value,
    ).toBe('以正式語氣回答')
    expect((wrapper.find('[data-test="output-key"]').element as HTMLInputElement).value).toBe(
      'askText',
    )
  })

  it('編輯 prompt 應 emit update:config', async () => {
    const wrapper = mount(PromptForm, { props: { config: {} } })
    await wrapper.find('[data-test="prompt-text"]').setValue('請用繁體中文回答')
    const emitted = wrapper.emitted('update:config')
    expect(emitted).toBeTruthy()
    expect(emitted![emitted!.length - 1][0]).toEqual({ prompt: '請用繁體中文回答' })
  })

  it('編輯 outputKey 應 emit 並保留既有 prompt', async () => {
    const wrapper = mount(PromptForm, { props: { config: { prompt: '提問' } } })
    await wrapper.find('[data-test="output-key"]').setValue('askText')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ prompt: '提問', outputKey: 'askText' })
  })

  it('清空欄位應移除對應鍵', async () => {
    const wrapper = mount(PromptForm, {
      props: { config: { prompt: '提問', outputKey: 'askText' } },
    })
    await wrapper.find('[data-test="output-key"]').setValue('')
    await wrapper.find('[data-test="prompt-text"]').setValue('')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({})
  })

  it('保留 config 中的未知鍵值', async () => {
    const wrapper = mount(PromptForm, { props: { config: { legacyField: 'keep-me' } } })
    await wrapper.find('[data-test="prompt-text"]').setValue('提問')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ legacyField: 'keep-me', prompt: '提問' })
  })

  it('顯示提示埠接線說明', () => {
    const wrapper = mount(PromptForm, { props: { config: {} } })
    expect(wrapper.find('[data-test="prompt-hint"]').text()).toContain('in:prompt')
  })
})
