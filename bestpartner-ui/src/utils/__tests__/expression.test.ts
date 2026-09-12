import { describe, it, expect } from 'vitest'
import {
  parseTemplate,
  refRootKey,
  refFieldPath,
  formatRefLabel,
  isKnownRef,
  serializeNodes,
  ZWSP,
} from '../expression'

const labels = { Mr7gjxEL: 'LLM 助手', hJRbZ8SG: '提示詞', __input__: '啟動資料' }

describe('parseTemplate', () => {
  it('純文字回傳單一 text 片段', () => {
    expect(parseTemplate('hello')).toEqual([{ kind: 'text', value: 'hello' }])
  })

  it('純引用回傳單一 ref 片段，保留原字串與路徑', () => {
    expect(parseTemplate('{{Mr7gjxEL.reply}}')).toEqual([
      { kind: 'ref', value: '{{Mr7gjxEL.reply}}', path: 'Mr7gjxEL.reply' },
    ])
  })

  it('文字與引用交錯時依序拆解', () => {
    expect(parseTemplate('答案：{{a.reply}} 完畢')).toEqual([
      { kind: 'text', value: '答案：' },
      { kind: 'ref', value: '{{a.reply}}', path: 'a.reply' },
      { kind: 'text', value: ' 完畢' },
    ])
  })

  it('容許大括號內前後空白（與後端 regex 一致）', () => {
    expect(parseTemplate('{{  a.reply  }}')[0]).toEqual({
      kind: 'ref',
      value: '{{  a.reply  }}',
      path: 'a.reply',
    })
  })

  it('不合語法的大括號視為純文字（如含空白或中文的路徑）', () => {
    expect(parseTemplate('{{不合法 路徑}}')).toEqual([{ kind: 'text', value: '{{不合法 路徑}}' }])
  })

  it('空字串回傳空陣列', () => {
    expect(parseTemplate('')).toEqual([])
  })

  it('連續多個引用不會漏掉（regex 帶 g，須每次重置 lastIndex）', () => {
    const first = parseTemplate('{{a.x}}{{b.y}}')
    const second = parseTemplate('{{a.x}}{{b.y}}')
    expect(first).toHaveLength(2)
    expect(second).toEqual(first)
  })
})

describe('引用路徑拆解', () => {
  it('refRootKey 取第一段', () => {
    expect(refRootKey('Mr7gjxEL.reply')).toBe('Mr7gjxEL')
    expect(refRootKey('item')).toBe('item')
  })

  it('refFieldPath 取其餘段落，單段回空字串', () => {
    expect(refFieldPath('Mr7gjxEL.reply')).toBe('reply')
    expect(refFieldPath('a.b.c')).toBe('b.c')
    expect(refFieldPath('item')).toBe('')
  })
})

describe('formatRefLabel / isKnownRef', () => {
  it('查得到節點名稱時顯示「名稱 › 欄位」', () => {
    expect(formatRefLabel('Mr7gjxEL.reply', labels)).toBe('LLM 助手 › reply')
  })

  it('單段引用只顯示名稱', () => {
    expect(formatRefLabel('__input__', labels)).toBe('啟動資料')
  })

  it('查不到節點時原樣顯示路徑，並判定為未知引用', () => {
    expect(formatRefLabel('llm_reply', labels)).toBe('llm_reply')
    expect(isKnownRef('llm_reply', labels)).toBe(false)
    expect(isKnownRef('Mr7gjxEL.reply', labels)).toBe(true)
  })
})

/** 以 jsdom 組出 contenteditable 可能產生的 DOM，驗證還原回模板字串 */
function buildRoot(html: string): HTMLElement {
  const div = document.createElement('div')
  div.innerHTML = html
  return div
}

describe('serializeNodes', () => {
  it('token 色塊還原成原始 {{...}} 字串', () => {
    const root = buildRoot(
      '答案：<span data-ref="{{Mr7gjxEL.reply}}" contenteditable="false">LLM 助手 › reply</span>',
    )
    expect(serializeNodes(root)).toBe('答案：{{Mr7gjxEL.reply}}')
  })

  it('<br> 還原成換行', () => {
    const root = buildRoot('第一行<br>第二行')
    expect(serializeNodes(root)).toBe('第一行\n第二行')
  })

  it('瀏覽器產生的 <div> 包裹視為換行', () => {
    const root = buildRoot('第一行<div>第二行</div>')
    expect(serializeNodes(root)).toBe('第一行\n第二行')
  })

  it('剝除插入 token 時補的零寬空格', () => {
    const root = buildRoot(
      `<span data-ref="{{a.b}}" contenteditable="false">x</span>${ZWSP}尾巴`,
    )
    expect(serializeNodes(root)).toBe('{{a.b}}尾巴')
  })

  it('色塊的顯示文字不影響還原結果（只認 data-ref）', () => {
    const root = buildRoot('<span data-ref="{{a.b}}" contenteditable="false">完全不同的字</span>')
    expect(serializeNodes(root)).toBe('{{a.b}}')
  })

  it('空內容回傳空字串', () => {
    expect(serializeNodes(buildRoot(''))).toBe('')
  })

  it('多個 token 與文字混排', () => {
    const root = buildRoot(
      '<span data-ref="{{a.x}}" contenteditable="false">A</span>＋' +
        '<span data-ref="{{b.y}}" contenteditable="false">B</span>',
    )
    expect(serializeNodes(root)).toBe('{{a.x}}＋{{b.y}}')
  })
})
