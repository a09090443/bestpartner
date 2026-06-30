import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import JsonConfigEditor from '../JsonConfigEditor.vue'

describe('JsonConfigEditor', () => {
  it('傳入合法物件應顯示對應 key', () => {
    const wrapper = mount(JsonConfigEditor, {
      props: { modelValue: { model: 'gpt', temperature: 0.7 } },
    })
    expect(wrapper.text()).toContain('model')
    expect(wrapper.text()).toContain('temperature')
  })

  it('key-value 模式編輯值應 emit 更新後的物件', async () => {
    const wrapper = mount(JsonConfigEditor, {
      props: { modelValue: { model: 'gpt' } },
    })
    const input = wrapper.find('[data-test="value-input-model"]')
    await input.setValue('claude')

    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted).toBeTruthy()
    expect(emitted!.at(-1)![0]).toEqual({ model: 'claude' })
  })

  it('raw 模式輸入非法 JSON 應顯示錯誤、emit invalid、不 emit 更新', async () => {
    const wrapper = mount(JsonConfigEditor, {
      props: { modelValue: { a: 1 } },
    })
    await wrapper.find('[data-test="mode-toggle"]').trigger('click')
    const raw = wrapper.find('[data-test="raw-input"]')
    await raw.setValue('{ invalid json }')

    expect(wrapper.find('[data-test="json-error"]').exists()).toBe(true)
    const validity = wrapper.emitted('validity-change')
    expect(validity).toBeTruthy()
    expect(validity!.at(-1)![0]).toBe(false)
    // 非法輸入不得 emit 更新
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('raw 模式輸入合法 JSON 應 emit 解析後物件並標記 valid', async () => {
    const wrapper = mount(JsonConfigEditor, {
      props: { modelValue: { a: 1 } },
    })
    await wrapper.find('[data-test="mode-toggle"]').trigger('click')
    const raw = wrapper.find('[data-test="raw-input"]')
    await raw.setValue('{ "b": 2, "nested": { "c": 3 } }')

    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted).toBeTruthy()
    expect(emitted!.at(-1)![0]).toEqual({ b: 2, nested: { c: 3 } })

    const validity = wrapper.emitted('validity-change')
    expect(validity!.at(-1)![0]).toBe(true)
  })
})
