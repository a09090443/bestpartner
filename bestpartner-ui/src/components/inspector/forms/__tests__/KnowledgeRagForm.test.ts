import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const loadKnowledgeOptions = vi.fn()
const loadEmbeddingOptions = vi.fn()
vi.mock('../../../../composables/useNodeOptions', () => ({
  useNodeOptions: () => ({
    loadKnowledgeOptions,
    loadEmbeddingOptions,
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
    loadEmbeddingOptions.mockReset()
    loadEmbeddingOptions.mockResolvedValue([
      { value: 'e1', label: 'nomic-embed' },
      { value: 'e2', label: 'text-embedding-3-small' },
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

  it('掛載後以載入的 Embedding 模型渲染下拉選項', async () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: {} } })
    await flushPromises()
    const options = wrapper.findAll('[data-test="embedding-model-select"] option')
    // 含一個空白 placeholder
    expect(options.length).toBe(3)
    expect(wrapper.text()).toContain('nomic-embed')
    expect(wrapper.text()).toContain('text-embedding-3-small')
  })

  it('選取 embeddingModelId 應 emit update:config（啟用必填欄位）', async () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: { knowledgeId: 'k1' } } })
    await flushPromises()
    await wrapper.find('[data-test="embedding-model-select"]').setValue('e2')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({
      knowledgeId: 'k1',
      embeddingModelId: 'e2',
      topK: 4,
    })
  })

  it('清空 embeddingModelId 應自 config 移除該鍵', async () => {
    const wrapper = mount(KnowledgeRagForm, {
      props: { config: { knowledgeId: 'k1', embeddingModelId: 'e1' } },
    })
    await flushPromises()
    await wrapper.find('[data-test="embedding-model-select"]').setValue('')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ knowledgeId: 'k1', topK: 4 })
  })

  it('編輯檢索語句應 emit update:config 帶入 query，清空則移除', async () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: { knowledgeId: 'k1' } } })
    await flushPromises()
    await wrapper.find('[data-test="query"]').setValue('{{trigger.input}}')
    let emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({
      knowledgeId: 'k1',
      query: '{{trigger.input}}',
      topK: 4,
    })
    await wrapper.find('[data-test="query"]').setValue('')
    emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ knowledgeId: 'k1', topK: 4 })
  })

  it('query 以既有 config 值初始化', () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: { query: '查詢語句' } } })
    expect((wrapper.find('[data-test="query"]').element as HTMLTextAreaElement).value).toBe(
      '查詢語句',
    )
  })

  it('編輯相似度下限應 emit number 型別，清空則移除', async () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: { knowledgeId: 'k1' } } })
    await flushPromises()
    await wrapper.find('[data-test="min-score"]').setValue('0.75')
    let emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({
      knowledgeId: 'k1',
      minScore: 0.75,
      topK: 4,
    })
    await wrapper.find('[data-test="min-score"]').setValue('')
    emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ knowledgeId: 'k1', topK: 4 })
  })

  it('編輯輸出鍵名應 emit update:config 帶入 outputKey，清空則移除', async () => {
    const wrapper = mount(KnowledgeRagForm, { props: { config: { knowledgeId: 'k1' } } })
    await flushPromises()
    await wrapper.find('[data-test="output-key"]').setValue('ragResult')
    let emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({
      knowledgeId: 'k1',
      outputKey: 'ragResult',
      topK: 4,
    })
    await wrapper.find('[data-test="output-key"]').setValue('')
    emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ knowledgeId: 'k1', topK: 4 })
  })
})
