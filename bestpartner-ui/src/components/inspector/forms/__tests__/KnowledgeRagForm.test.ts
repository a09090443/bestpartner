import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const loadKnowledgeOptions = vi.fn()
vi.mock('../../../../composables/useNodeOptions', () => ({
  useNodeOptions: () => ({
    loadKnowledgeOptions,
    loadLlmOptions: vi.fn(),
    loadToolOptions: vi.fn(),
    loadMcpOptions: vi.fn(),
  }),
}))

import KnowledgeRagForm from '../KnowledgeRagForm.vue'

describe('KnowledgeRagForm', () => {
  beforeEach(() => {
    loadKnowledgeOptions.mockReset()
    loadKnowledgeOptions.mockResolvedValue([
      { value: 'k1', label: '產品手冊' },
      { value: 'k2', label: 'FAQ' },
    ])
  })

  it('掛載後以載入的知識庫渲染下拉選項', async () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: {} } })
    await flushPromises()
    const options = wrapper.findAll('[data-test="knowledge-id"] option')
    // 含一個空白 placeholder
    expect(options.length).toBe(3)
    expect(wrapper.text()).toContain('產品手冊')
    expect(wrapper.text()).toContain('FAQ')
  })

  it('topK 預設為 4', () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: {} } })
    expect((wrapper.find('[data-test="topk"]').element as HTMLInputElement).value).toBe('4')
  })

  it('topK 以既有 config 值初始化', () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: { topK: 8 } } })
    expect((wrapper.find('[data-test="topk"]').element as HTMLInputElement).value).toBe('8')
  })

  it('選取 knowledgeId 應 emit update:config（含預設 topK）', async () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: {} } })
    await flushPromises()
    await wrapper.find('[data-test="knowledge-id"]').setValue('k1')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ knowledgeId: 'k1', topK: 4 })
  })

  it('編輯 topK 應 emit 數字型別並保留未知鍵值', async () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: { foo: 1, knowledgeId: 'k2' } } })
    await flushPromises()
    await wrapper.find('[data-test="topk"]').setValue('6')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ foo: 1, knowledgeId: 'k2', topK: 6 })
  })
})
