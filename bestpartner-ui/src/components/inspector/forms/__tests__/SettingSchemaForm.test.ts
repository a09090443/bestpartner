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

  it('array 型別欄位渲染為 text，emit 時以逗號切割為字串陣列', async () => {
    const arraySchema: ToolSettingSchema = {
      stopWords: { type: 'array', required: false, description: '停用詞清單' },
    }
    const wrapper = mount(SettingSchemaForm, { props: { schema: arraySchema, modelValue: {} } })
    const input = wrapper.find('[data-test="field-stopWords"]')
    expect(input.attributes('type')).toBe('text')
    // trim 每段並濾除空段（末尾多餘逗號不產生空元素）
    await input.setValue('a.com, b.com ,')
    const events = wrapper.emitted('update:modelValue')!
    expect(events[events.length - 1][0]).toEqual({ stopWords: ['a.com', 'b.com'] })
  })

  it('array 欄位空輸入時刪除該鍵', async () => {
    const arraySchema: ToolSettingSchema = {
      stopWords: { type: 'array', required: false },
    }
    const wrapper = mount(SettingSchemaForm, {
      props: { schema: arraySchema, modelValue: { stopWords: ['x'] } },
    })
    await wrapper.find('[data-test="field-stopWords"]').setValue('')
    const events = wrapper.emitted('update:modelValue')!
    expect(events[events.length - 1][0]).toEqual({})
  })

  it('array 欄位由既有陣列 modelValue 還原為逗號字串顯示', () => {
    const arraySchema: ToolSettingSchema = {
      stopWords: { type: 'array', required: false },
    }
    const wrapper = mount(SettingSchemaForm, {
      props: { schema: arraySchema, modelValue: { stopWords: ['a.com', 'b.com'] } },
    })
    expect((wrapper.find('[data-test="field-stopWords"]').element as HTMLInputElement).value).toBe(
      'a.com, b.com',
    )
  })

  it('array 欄位逐字輸入逗號不被吞（本地 buffer 不從 model 反推）', async () => {
    const arraySchema: ToolSettingSchema = { tags: { type: 'array', required: false } }
    const wrapper = mount(SettingSchemaForm, { props: { schema: arraySchema, modelValue: {} } })
    const input = wrapper.find('[data-test="field-tags"]')
    const el = input.element as HTMLInputElement
    // 逐字打到剛按下逗號：DOM 值須保留 'a,'（逗號不被吃掉），model 為 ['a']
    el.value = 'a,'
    await input.trigger('input')
    await wrapper.vm.$nextTick()
    expect((wrapper.find('[data-test="field-tags"]').element as HTMLInputElement).value).toBe('a,')
    let events = wrapper.emitted('update:modelValue')!
    expect(events[events.length - 1][0]).toEqual({ tags: ['a'] })
    // 繼續打 'b'
    el.value = 'a,b'
    await input.trigger('input')
    await wrapper.vm.$nextTick()
    events = wrapper.emitted('update:modelValue')!
    expect(events[events.length - 1][0]).toEqual({ tags: ['a', 'b'] })
  })

  it('切換 schema 時 array buffer 由新 modelValue 重建', async () => {
    const arraySchema: ToolSettingSchema = { tags: { type: 'array', required: false } }
    const wrapper = mount(SettingSchemaForm, {
      props: { schema: arraySchema, modelValue: { tags: ['x'] } },
    })
    expect((wrapper.find('[data-test="field-tags"]').element as HTMLInputElement).value).toBe('x')
    // 外部（切換工具）帶入不同 modelValue → buffer 應重建
    await wrapper.setProps({ modelValue: { tags: ['a.com', 'b.com'] } })
    expect((wrapper.find('[data-test="field-tags"]').element as HTMLInputElement).value).toBe(
      'a.com, b.com',
    )
  })

  it('integer 欄位輸入小數時截斷為整數，number 欄位保留小數', async () => {
    const numberSchema: ToolSettingSchema = {
      timeout: { type: 'integer', required: false },
      ratio: { type: 'number', required: false },
    }
    const wrapper = mount(SettingSchemaForm, { props: { schema: numberSchema, modelValue: {} } })
    await wrapper.find('[data-test="field-timeout"]').setValue('3.7')
    await wrapper.find('[data-test="field-ratio"]').setValue('3.5')
    const events = wrapper.emitted('update:modelValue')!
    expect(events[0][0]).toEqual({ timeout: 3 }) // integer 截斷
    expect(events[1][0]).toEqual({ ratio: 3.5 }) // number 保留小數
  })

  it('integer 欄位負數小數趨零截斷（-3.7 → -3）', async () => {
    const numberSchema: ToolSettingSchema = { offset: { type: 'integer', required: false } }
    const wrapper = mount(SettingSchemaForm, { props: { schema: numberSchema, modelValue: {} } })
    await wrapper.find('[data-test="field-offset"]').setValue('-3.7')
    const events = wrapper.emitted('update:modelValue')!
    expect(events[events.length - 1][0]).toEqual({ offset: -3 })
  })

  it('sensitive 欄位帶 autocomplete="new-password"，一般欄位不帶', () => {
    const wrapper = mount(SettingSchemaForm, { props: { schema, modelValue: {} } })
    expect(wrapper.find('[data-test="field-apiKey"]').attributes('autocomplete')).toBe(
      'new-password',
    )
    expect(wrapper.find('[data-test="field-timeout"]').attributes('autocomplete')).toBeUndefined()
  })

  it('integer 欄位帶 step="1"，number 欄位不帶', () => {
    const numberSchema: ToolSettingSchema = {
      timeout: { type: 'integer', required: false },
      ratio: { type: 'number', required: false },
    }
    const wrapper = mount(SettingSchemaForm, { props: { schema: numberSchema, modelValue: {} } })
    expect(wrapper.find('[data-test="field-timeout"]').attributes('step')).toBe('1')
    expect(wrapper.find('[data-test="field-ratio"]').attributes('step')).toBeUndefined()
  })

  it('label 與 input 關聯（input 包在 label 內）', () => {
    const wrapper = mount(SettingSchemaForm, { props: { schema, modelValue: {} } })
    const label = wrapper.find('[data-test="label-apiKey"]')
    expect(label.element.tagName).toBe('LABEL')
    expect(label.find('input').exists()).toBe(true)
  })
})
