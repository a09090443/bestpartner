import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ExpressionEditor from '../ExpressionEditor.vue'

const refLabels = { Mr7gjxEL: 'LLM 助手', hJRbZ8SG: '提示詞' }

function mountEditor(modelValue = '', props: Record<string, unknown> = {}) {
  return mount(ExpressionEditor, {
    props: { modelValue, refLabels, ...props },
    attachTo: document.body,
  })
}

describe('ExpressionEditor 渲染', () => {
  it('把 {{nodeKey.field}} 渲染成可讀色塊，而非顯示原始識別碼', () => {
    const w = mountEditor('{{Mr7gjxEL.reply}}')
    const token = w.find('[data-test="expr-token-Mr7gjxEL.reply"]')
    expect(token.exists()).toBe(true)
    expect(token.text()).toBe('LLM 助手 › reply')
    // 原始字串保存在 data-ref，供還原用
    expect(token.attributes('data-ref')).toBe('{{Mr7gjxEL.reply}}')
    expect(token.attributes('contenteditable')).toBe('false')
  })

  it('文字與色塊混排時保留原文字', () => {
    const w = mountEditor('答案：{{Mr7gjxEL.reply}} 完畢')
    expect(w.element.textContent).toContain('答案：')
    expect(w.element.textContent).toContain('LLM 助手 › reply')
    expect(w.element.textContent).toContain('完畢')
  })

  /** 這正是 `{{llm_reply}}` 那類錯誤的樣子——過去要等執行失敗才知道 */
  it('查不到節點的引用標成未知樣式，並於 title 說明', () => {
    const w = mountEditor('{{llm_reply}}')
    const token = w.find('[data-test="expr-token-llm_reply"]')
    expect(token.classes()).toContain('is-unknown')
    expect(token.text()).toBe('llm_reply')
    expect(token.attributes('title')).toContain('找不到對應節點')
  })

  it('已知引用的 title 顯示原始字串，方便核對', () => {
    const w = mountEditor('{{Mr7gjxEL.reply}}')
    expect(w.find('[data-test="expr-token-Mr7gjxEL.reply"]').attributes('title')).toBe(
      '{{Mr7gjxEL.reply}}',
    )
  })

  it('空值時掛上 is-empty 以顯示 placeholder', () => {
    const w = mountEditor('', { placeholder: '請輸入' })
    expect(w.classes()).toContain('is-empty')
    expect(w.attributes('data-placeholder')).toBe('請輸入')
  })

  it('上游節點改名後色塊文字跟著更新', async () => {
    const w = mountEditor('{{Mr7gjxEL.reply}}')
    expect(w.find('[data-test="expr-token-Mr7gjxEL.reply"]').text()).toBe('LLM 助手 › reply')
    await w.setProps({ refLabels: { Mr7gjxEL: '主要模型' } })
    expect(w.find('[data-test="expr-token-Mr7gjxEL.reply"]').text()).toBe('主要模型 › reply')
  })
})

describe('ExpressionEditor 值同步', () => {
  it('使用者輸入後 emit 原始模板字串（非顯示文字）', async () => {
    const w = mountEditor('{{Mr7gjxEL.reply}}')
    w.element.appendChild(document.createTextNode('！'))
    await w.trigger('input')

    const emitted = w.emitted('update:modelValue')!
    expect(emitted.at(-1)![0]).toBe('{{Mr7gjxEL.reply}}！')
  })

  /**
   * 中文輸入法地雷：組字期間若把中途內容當成正式值 emit，
   * 會把注音／拼音寫進 config，且重繪會吃掉正在組的字。
   */
  it('組字期間不 emit，compositionend 後才回報一次', async () => {
    const w = mountEditor('')
    await w.trigger('compositionstart')
    w.element.appendChild(document.createTextNode('ㄊ'))
    await w.trigger('input')
    expect(w.emitted('update:modelValue')).toBeUndefined()

    w.element.textContent = '測試'
    await w.trigger('compositionend')
    const emitted = w.emitted('update:modelValue')!
    expect(emitted).toHaveLength(1)
    expect(emitted[0][0]).toBe('測試')
  })

  it('外部值變更會重繪；自己 emit 造成的回流不重繪（避免打斷游標）', async () => {
    const w = mountEditor('{{Mr7gjxEL.reply}}')

    // 模擬使用者輸入 → emit → 父層把同樣的值寫回 modelValue
    w.element.appendChild(document.createTextNode('x'))
    await w.trigger('input')
    const emittedValue = w.emitted('update:modelValue')!.at(-1)![0] as string
    await w.setProps({ modelValue: emittedValue })
    // 不重繪：DOM 仍是使用者當下敲的內容
    expect(w.element.textContent).toContain('x')

    // 外部換成不同的值 → 重繪
    await w.setProps({ modelValue: '{{hJRbZ8SG.prompt}}' })
    expect(w.find('[data-test="expr-token-hJRbZ8SG.prompt"]').exists()).toBe(true)
    expect(w.element.textContent).not.toContain('x')
  })

  it('貼上一律轉純文字，不把 HTML 結構帶進編輯區', async () => {
    const w = mountEditor('')
    const clipboardData = { getData: vi.fn(() => '<b>粗體</b>純文字') }
    await w.trigger('paste', { clipboardData })

    expect(clipboardData.getData).toHaveBeenCalledWith('text/plain')
    const emitted = w.emitted('update:modelValue')!
    expect(emitted.at(-1)![0]).toBe('<b>粗體</b>純文字')
    expect(w.element.querySelector('b')).toBeNull()
  })
})

describe('ExpressionEditor insertRef', () => {
  it('插入色塊並 emit 含該引用的模板字串', async () => {
    const w = mountEditor('')
    ;(w.vm as unknown as { insertRef: (raw: string, path: string) => void }).insertRef(
      '{{Mr7gjxEL.reply}}',
      'Mr7gjxEL.reply',
    )
    await w.vm.$nextTick()

    expect(w.find('[data-test="expr-token-Mr7gjxEL.reply"]').exists()).toBe(true)
    expect(w.emitted('update:modelValue')!.at(-1)![0]).toBe('{{Mr7gjxEL.reply}}')
  })

  it('連續插入兩個引用皆保留', async () => {
    const w = mountEditor('')
    const vm = w.vm as unknown as { insertRef: (raw: string, path: string) => void }
    vm.insertRef('{{Mr7gjxEL.reply}}', 'Mr7gjxEL.reply')
    vm.insertRef('{{hJRbZ8SG.prompt}}', 'hJRbZ8SG.prompt')
    await w.vm.$nextTick()

    const value = w.emitted('update:modelValue')!.at(-1)![0] as string
    expect(value).toContain('{{Mr7gjxEL.reply}}')
    expect(value).toContain('{{hJRbZ8SG.prompt}}')
  })
})
