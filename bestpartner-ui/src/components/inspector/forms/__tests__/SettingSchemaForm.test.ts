import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import SettingSchemaForm from '../SettingSchemaForm.vue'
import type { ToolSettingSchema } from '../../../../types/toolSchema'

const schema: ToolSettingSchema = {
  apiKey: { type: 'string', required: true, sensitive: true, description: 'API 金鑰' },
  timeout: { type: 'integer', required: true },
  siteRestrict: { type: 'boolean', required: false },
}

describe('SettingSchemaForm', () => {
  it('依 schema 渲染欄位：sensitive 為 password、integer 為 number、boolean 為 checkbox', () => {
    const wrapper = mount(SettingSchemaForm, { props: { schema, modelValue: {} } })
    expect(wrapper.find('[data-test="field-apiKey"]').attributes('type')).toBe('password')
    expect(wrapper.find('[data-test="field-timeout"]').attributes('type')).toBe('number')
    expect(wrapper.find('[data-test="field-siteRestrict"]').attributes('type')).toBe('checkbox')
  })

  it('必填欄位 label 顯示星號', () => {
    const wrapper = mount(SettingSchemaForm, { props: { schema, modelValue: {} } })
    expect(wrapper.find('[data-test="label-apiKey"]').text()).toContain('*')
    expect(wrapper.find('[data-test="label-siteRestrict"]').text()).not.toContain('*')
  })

  it('輸入時 emit 型別正確的 modelValue（受控元件：各次 emit 以 props.modelValue 為基底）', async () => {
    // 測試中 props 不會自動回寫（無 v-model），故連續輸入時
    // 每次 emit 的物件都以未變的 props.modelValue（{}）為基底，各自只含該次欄位。
    const wrapper = mount(SettingSchemaForm, { props: { schema, modelValue: {} } })
    await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
    await wrapper.find('[data-test="field-timeout"]').setValue('3000')
    await wrapper.find('[data-test="field-siteRestrict"]').setValue(true)
    const events = wrapper.emitted('update:modelValue')!
    expect(events).toHaveLength(3)
    expect(events[0][0]).toEqual({ apiKey: 'sk-xxx' })
    expect(events[1][0]).toEqual({ timeout: 3000 }) // number，非字串
    expect(events[2][0]).toEqual({ siteRestrict: true })
  })

  it('emit 以 props.modelValue 為基底合併，保留既有欄位值', async () => {
    const wrapper = mount(SettingSchemaForm, {
      props: { schema, modelValue: { apiKey: 'sk-old', timeout: 1000 } },
    })
    await wrapper.find('[data-test="field-siteRestrict"]').setValue(true)
    const events = wrapper.emitted('update:modelValue')!
    expect(events[events.length - 1][0]).toEqual({
      apiKey: 'sk-old',
      timeout: 1000,
      siteRestrict: true,
    })
  })

  it('清空欄位時應從 modelValue 移除該鍵', async () => {
    const wrapper = mount(SettingSchemaForm, {
      props: { schema, modelValue: { apiKey: 'sk-old', timeout: 1000 } },
    })
    await wrapper.find('[data-test="field-apiKey"]').setValue('')
    const events = wrapper.emitted('update:modelValue')!
    expect(events[events.length - 1][0]).toEqual({ timeout: 1000 })
  })

  it('由既有 modelValue 還原欄位值', () => {
    const wrapper = mount(SettingSchemaForm, {
      props: { schema, modelValue: { apiKey: 'sk-old', timeout: 1000, siteRestrict: true } },
    })
    expect((wrapper.find('[data-test="field-apiKey"]').element as HTMLInputElement).value).toBe(
      'sk-old',
    )
    expect((wrapper.find('[data-test="field-timeout"]').element as HTMLInputElement).value).toBe(
      '1000',
    )
    expect(
      (wrapper.find('[data-test="field-siteRestrict"]').element as HTMLInputElement).checked,
    ).toBe(true)
  })

  it('顯示 description 說明文字', () => {
    const wrapper = mount(SettingSchemaForm, { props: { schema, modelValue: {} } })
    expect(wrapper.text()).toContain('API 金鑰')
  })
})
