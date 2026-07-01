import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import KnowledgeRagForm from '../KnowledgeRagForm.vue'

describe('KnowledgeRagForm', () => {
  it('topK 預設為 4', () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: {} } })
    expect((wrapper.find('[data-test="topk"]').element as HTMLInputElement).value).toBe('4')
  })

  it('topK 以既有 config 值初始化', () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: { topK: 8 } } })
    expect((wrapper.find('[data-test="topk"]').element as HTMLInputElement).value).toBe('8')
  })

  it('編輯 knowledgeId 應 emit update:config（含預設 topK）', async () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: {} } })
    await wrapper.find('[data-test="knowledge-id"]').setValue('kb-1')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ knowledgeId: 'kb-1', topK: 4 })
  })

  it('編輯 topK 應 emit 數字型別並保留未知鍵值', async () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: { foo: 1, knowledgeId: 'kb-9' } } })
    await wrapper.find('[data-test="topk"]').setValue('6')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ foo: 1, knowledgeId: 'kb-9', topK: 6 })
  })
})
