import { describe, it, expect } from 'vitest'
import {
  NODE_TYPE_METAS,
  NODE_CATEGORIES,
  getNodeTypeMeta,
  getNodesByCategory,
} from '../nodeTypes'

describe('nodeTypes 埠定義', () => {
  it('每個型別都有 inputs / outputs 陣列', () => {
    for (const meta of NODE_TYPE_METAS) {
      expect(Array.isArray(meta.inputs)).toBe(true)
      expect(Array.isArray(meta.outputs)).toBe(true)
    }
  })

  it('TRIGGER 無輸入、單一 out:main', () => {
    const meta = getNodeTypeMeta('TRIGGER')!
    expect(meta.inputs).toEqual([])
    expect(meta.outputs.map((p) => p.id)).toEqual(['out:main'])
  })

  it('預設型別（TOOL）為 in:main + out:main', () => {
    const meta = getNodeTypeMeta('TOOL')!
    expect(meta.inputs.map((p) => p.id)).toEqual(['in:main'])
    expect(meta.outputs.map((p) => p.id)).toEqual(['out:main'])
  })

  it('CONDITION 有 out:true / out:false 兩輸出並帶 label', () => {
    const meta = getNodeTypeMeta('CONDITION')!
    expect(meta.inputs.map((p) => p.id)).toEqual(['in:main'])
    expect(meta.outputs.map((p) => p.id)).toEqual(['out:true', 'out:false'])
    expect(meta.outputs.every((p) => !!p.label)).toBe(true)
  })

  it('LOOP 有 out:loop / out:done 兩輸出', () => {
    const meta = getNodeTypeMeta('LOOP')!
    expect(meta.outputs.map((p) => p.id)).toEqual(['out:loop', 'out:done'])
  })
})

describe('nodeTypes 分類（category）', () => {
  it('NODE_CATEGORIES 依固定順序 Trigger → Action → AI → Logic', () => {
    expect(NODE_CATEGORIES).toEqual(['Trigger', 'Action', 'AI', 'Logic'])
  })

  it('每個型別都有 category 且屬於 NODE_CATEGORIES', () => {
    for (const meta of NODE_TYPE_METAS) {
      expect(NODE_CATEGORIES).toContain(meta.category)
    }
  })

  it('分類對映符合規格', () => {
    expect(getNodeTypeMeta('TRIGGER')!.category).toBe('Trigger')
    for (const t of ['TOOL', 'MCP_SERVER', 'HTTP_REQUEST'] as const) {
      expect(getNodeTypeMeta(t)!.category).toBe('Action')
    }
    for (const t of ['LLM_ASSISTANT', 'KNOWLEDGE_RAG'] as const) {
      expect(getNodeTypeMeta(t)!.category).toBe('AI')
    }
    for (const t of ['CONDITION', 'LOOP', 'CODE', 'DATA_TRANSFORM'] as const) {
      expect(getNodeTypeMeta(t)!.category).toBe('Logic')
    }
  })

  it('getNodesByCategory 涵蓋全部 11 種型別且不重複', () => {
    const all = NODE_CATEGORIES.flatMap((c) => getNodesByCategory(c).map((m) => m.type))
    expect(all).toHaveLength(NODE_TYPE_METAS.length)
    expect(new Set(all).size).toBe(NODE_TYPE_METAS.length)
  })
})
