import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OutputForm from '../OutputForm.vue'

describe('OutputForm', () => {
  it('輸入 template 後 emit update:config 帶 template 鍵', async () => {
    const wrapper = mount(OutputForm, { props: { config: {} } })
    await wrapper.find('[data-test="output-template"]').setValue('結果：{{llm.reply}}')
    const events = wrapper.emitted('update:config')!
    expect(events.at(-1)![0]).toEqual({ template: '結果：{{llm.reply}}' })
  })

  it('template 清空時移除鍵', async () => {
    const wrapper = mount(OutputForm, { props: { config: { template: 'x' } } })
    await wrapper.find('[data-test="output-template"]').setValue('')
    const events = wrapper.emitted('update:config')!
    expect(events.at(-1)![0]).toEqual({})
  })
})
