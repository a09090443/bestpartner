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

/**
 * 表格模式原本只 v-for 既有的鍵，config 為空時整個面板無路可走
 * （只顯示「無設定欄位」），逼使用者切到 JSON 模式手打 raw JSON。
 * 對沒有型別化表單的節點（CONDITION / LOOP / CODE / HTTP_REQUEST / DATA_TRANSFORM）是硬門檻。
 */
describe('JsonConfigEditor 表格模式新增／移除欄位', () => {
  it('config 為空時仍可新增欄位（不必切到 JSON 模式）', async () => {
    const wrapper = mount(JsonConfigEditor, { props: { modelValue: {} } })
    expect(wrapper.find('.empty').exists()).toBe(true)

    await wrapper.find('[data-test="new-field-key"]').setValue('triggerType')
    await wrapper.find('[data-test="add-field"]').trigger('click')

    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted).toBeTruthy()
    expect(emitted!.at(-1)![0]).toEqual({ triggerType: '' })
  })

  it('新增後鍵名輸入框清空，供接著新增下一個', async () => {
    const wrapper = mount(JsonConfigEditor, { props: { modelValue: {} } })
    const input = wrapper.find('[data-test="new-field-key"]')
    await input.setValue('foo')
    await wrapper.find('[data-test="add-field"]').trigger('click')
    expect((input.element as HTMLInputElement).value).toBe('')
  })

  it('鍵名為空或只有空白時不新增並顯示錯誤', async () => {
    const wrapper = mount(JsonConfigEditor, { props: { modelValue: {} } })
    await wrapper.find('[data-test="new-field-key"]').setValue('   ')
    await wrapper.find('[data-test="add-field"]').trigger('click')

    expect(wrapper.find('[data-test="new-field-error"]').text()).toContain('請輸入欄位名稱')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('鍵名重複時不覆蓋既有值並顯示錯誤', async () => {
    const wrapper = mount(JsonConfigEditor, { props: { modelValue: { a: 1 } } })
    await wrapper.find('[data-test="new-field-key"]').setValue('a')
    await wrapper.find('[data-test="add-field"]').trigger('click')

    expect(wrapper.find('[data-test="new-field-error"]').text()).toContain('已存在')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('可移除既有欄位', async () => {
    const wrapper = mount(JsonConfigEditor, { props: { modelValue: { a: 1, b: 2 } } })
    await wrapper.find('[data-test="remove-field-a"]').trigger('click')

    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted!.at(-1)![0]).toEqual({ b: 2 })
  })
})
