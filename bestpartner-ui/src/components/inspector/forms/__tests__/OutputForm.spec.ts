import { describe, expect, it } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import OutputForm from '../OutputForm.vue'

/**
 * ⚠️ 輸出模板欄位已由 textarea 改為 `ExpressionEditor`（contenteditable），
 * 目的是把 `{{nodeKey.field}}` 顯示成可讀色塊。
 * 因此**不能再用 `setValue()` / `inputValue()`**——要直接改 DOM 內容再觸發 input。
 * Playwright 端同理：`fill()` 可用，但斷言要改看 `textContent` 而非 `inputValue()`。
 */
async function typeTemplate(wrapper: VueWrapper, text: string) {
  const editor = wrapper.find('[data-test="output-template"]')
  editor.element.textContent = text
  await editor.trigger('input')
}

describe('OutputForm', () => {
  it('輸入 template 後 emit update:config 帶 template 鍵', async () => {
    const wrapper = mount(OutputForm, { props: { config: {} } })
    await typeTemplate(wrapper, '結果：{{llm.reply}}')
    const events = wrapper.emitted('update:config')!
    expect(events.at(-1)![0]).toEqual({ template: '結果：{{llm.reply}}' })
  })

  it('template 清空時移除鍵', async () => {
    const wrapper = mount(OutputForm, { props: { config: { template: 'x' } } })
    await typeTemplate(wrapper, '')
    const events = wrapper.emitted('update:config')!
    expect(events.at(-1)![0]).toEqual({})
  })

  it('既有 template 載入時渲染成可讀色塊，而非原始識別碼', () => {
    const wrapper = mount(OutputForm, {
      props: {
        config: { template: '{{Mr7gjxEL.reply}}' },
        refLabels: { Mr7gjxEL: 'LLM 助手' },
      },
    })
    const token = wrapper.find('[data-test="expr-token-Mr7gjxEL.reply"]')
    expect(token.exists()).toBe(true)
    expect(token.text()).toBe('LLM 助手 › reply')
  })

  it('引用到不存在的節點時標成未知（過去要等執行失敗才知道）', () => {
    const wrapper = mount(OutputForm, {
      props: { config: { template: '{{llm_reply}}' }, refLabels: { Mr7gjxEL: 'LLM 助手' } },
    })
    expect(wrapper.find('[data-test="expr-token-llm_reply"]').classes()).toContain('is-unknown')
  })
})

/**
 * 使用者實測回報：簡單流程執行時報
 * 「Node qoEPNaPS is missing required config fields: template|mappings」。
 * 原因是兩個欄位一個沒標必填、一個標「選填」，讀起來像都可以不填；
 * 且要填也得先自行挖出上游節點的 nanoid 才拼得出 {{key.reply}}。
 */
describe('OutputForm 必填提示與可引用欄位', () => {
  const refs = [
    { nodeId: 'qoEPNaPS', name: 'LLM 助手', refs: ['{{qoEPNaPS.reply}}'] },
    { nodeId: 'p1', name: '提示詞', refs: ['{{p1.prompt}}'] },
  ]

  it('兩個欄位皆空時顯示「至少填一個」提示', () => {
    const w = mount(OutputForm, { props: { config: {} } })
    expect(w.find('[data-test="output-both-empty"]').exists()).toBe(true)
  })

  it('填了 template 後提示消失', async () => {
    const w = mount(OutputForm, { props: { config: {} } })
    await typeTemplate(w, '{{n1.reply}}')
    expect(w.find('[data-test="output-both-empty"]').exists()).toBe(false)
  })

  it('只填 mappings 也算滿足（擇一必填）', async () => {
    const w = mount(OutputForm, { props: { config: {} } })
    await w.find('[data-test="output-mappings"]').setValue('answer={{n1.reply}}')
    expect(w.find('[data-test="output-both-empty"]').exists()).toBe(false)
  })

  it('chip 以「節點名 › 欄位」呈現，原始引用只放在 title（畫面不洩漏 nanoid）', () => {
    const w = mount(OutputForm, { props: { config: {}, upstreamRefs: refs } })
    const box = w.find('[data-test="output-ref-suggestions"]')
    expect(box.exists()).toBe(true)

    const chip = w.find('[data-test="output-ref-{{qoEPNaPS.reply}}"]')
    expect(chip.text()).toBe('LLM 助手 › reply')
    expect(chip.attributes('title')).toBe('插入 {{qoEPNaPS.reply}}')
    expect(box.text()).not.toContain('qoEPNaPS')
  })

  it('沒有可引用欄位時不顯示建議區塊', () => {
    const w = mount(OutputForm, { props: { config: {}, upstreamRefs: [] } })
    expect(w.find('[data-test="output-ref-suggestions"]').exists()).toBe(false)
  })

  it('點 chip 插入引用色塊並 emit 原始模板字串', async () => {
    const w = mount(OutputForm, {
      props: { config: {}, upstreamRefs: refs, refLabels: { qoEPNaPS: 'LLM 助手' } },
      attachTo: document.body,
    })
    await w.find('[data-test="output-ref-{{qoEPNaPS.reply}}"]').trigger('click')

    // config 存的是原始字串
    expect(w.emitted('update:config')!.at(-1)![0]).toEqual({ template: '{{qoEPNaPS.reply}}' })
    // 畫面上顯示的是可讀名稱
    expect(w.find('[data-test="expr-token-qoEPNaPS.reply"]').text()).toBe('LLM 助手 › reply')
  })

  it('連點兩個 chip 會接續插入，不覆蓋既有內容', async () => {
    const w = mount(OutputForm, {
      props: { config: {}, upstreamRefs: refs },
      attachTo: document.body,
    })
    await w.find('[data-test="output-ref-{{qoEPNaPS.reply}}"]').trigger('click')
    await w.find('[data-test="output-ref-{{p1.prompt}}"]').trigger('click')

    const value = (w.emitted('update:config')!.at(-1)![0] as { template: string }).template
    expect(value).toContain('{{qoEPNaPS.reply}}')
    expect(value).toContain('{{p1.prompt}}')
  })
})
