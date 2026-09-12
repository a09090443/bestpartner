import { describe, it, expect } from 'vitest'
import { DEFAULT_OUTPUT_KEYS, buildUpstreamRef } from '../nodeOutputKeys'

describe('DEFAULT_OUTPUT_KEYS', () => {
  /**
   * 這張表是手抄後端 executor 的預設鍵名，寫錯會讓使用者照著提示填卻取不到值。
   * 出處逐項註記於 nodeOutputKeys.ts。
   */
  it('對齊各 executor 的預設輸出鍵名', () => {
    expect(DEFAULT_OUTPUT_KEYS.LLM_ASSISTANT).toEqual(['reply'])
    expect(DEFAULT_OUTPUT_KEYS.PROMPT).toEqual(['prompt'])
    expect(DEFAULT_OUTPUT_KEYS.TOOL).toEqual(['result'])
    expect(DEFAULT_OUTPUT_KEYS.MCP_SERVER).toEqual(['result'])
    expect(DEFAULT_OUTPUT_KEYS.KNOWLEDGE_RAG).toEqual(['documents'])
    expect(DEFAULT_OUTPUT_KEYS.CODE).toEqual(['result'])
    expect(DEFAULT_OUTPUT_KEYS.HTTP_REQUEST).toEqual(['response'])
    expect(DEFAULT_OUTPUT_KEYS.DATA_TRANSFORM).toEqual(['result'])
    expect(DEFAULT_OUTPUT_KEYS.CONDITION).toEqual(['result', 'branch'])
  })

  it('不產生可引用輸出的型別不列入（TRIGGER / SKILL / OUTPUT / LOOP）', () => {
    expect(DEFAULT_OUTPUT_KEYS.TRIGGER).toBeUndefined()
    expect(DEFAULT_OUTPUT_KEYS.SKILL).toBeUndefined()
    expect(DEFAULT_OUTPUT_KEYS.OUTPUT).toBeUndefined()
    expect(DEFAULT_OUTPUT_KEYS.LOOP).toBeUndefined()
  })
})

describe('buildUpstreamRef 優先序', () => {
  it('未執行且無自訂 outputKey → 用型別預設鍵', () => {
    const r = buildUpstreamRef({ nodeId: 'abc123', name: 'LLM 助手', type: 'LLM_ASSISTANT' })
    expect(r.refs).toEqual(['{{abc123.reply}}'])
    expect(r.name).toBe('LLM 助手')
  })

  it('有自訂 outputKey → 蓋過型別預設鍵', () => {
    const r = buildUpstreamRef({
      nodeId: 'abc123',
      name: 'LLM 助手',
      type: 'LLM_ASSISTANT',
      config: { outputKey: 'answer' },
    })
    expect(r.refs).toEqual(['{{abc123.answer}}'])
  })

  it('已執行過 → 以實際輸出鍵為準（最準確，蓋過設定與預設）', () => {
    const r = buildUpstreamRef({
      nodeId: 'abc123',
      name: 'LLM 助手',
      type: 'LLM_ASSISTANT',
      config: { outputKey: 'answer' },
      executedOutput: { reply: 'hi', usage: 12 },
    })
    expect(r.refs).toEqual(['{{abc123.reply}}', '{{abc123.usage}}'])
  })

  it('空白的 outputKey 視同未設定，退回型別預設鍵', () => {
    const r = buildUpstreamRef({
      nodeId: 'n1',
      name: 'x',
      type: 'TOOL',
      config: { outputKey: '   ' },
    })
    expect(r.refs).toEqual(['{{n1.result}}'])
  })

  it('無從得知輸出鍵的型別（TRIGGER）回傳空建議，供呼叫端略過', () => {
    const r = buildUpstreamRef({ nodeId: 'n1', name: '觸發', type: 'TRIGGER' })
    expect(r.refs).toEqual([])
  })

  it('TRIGGER 執行過後改以實際 payload 鍵為建議', () => {
    const r = buildUpstreamRef({
      nodeId: 'n1',
      name: '觸發',
      type: 'TRIGGER',
      executedOutput: { userId: 'u1', topic: 'AI' },
    })
    expect(r.refs).toEqual(['{{n1.userId}}', '{{n1.topic}}'])
  })
})
