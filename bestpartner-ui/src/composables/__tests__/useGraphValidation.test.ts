import { describe, it, expect } from 'vitest'
import { validateGraph } from '../useGraphValidation'
import type { NodeType, WorkflowNodeDTO, WorkflowEdgeDTO } from '../../types/workflow'
import type { NodeRequiredFields } from '../../api/workflow'

function node(nodeKey: string, type: NodeType = 'TOOL', config: Record<string, unknown> = {}): WorkflowNodeDTO {
  return { nodeKey, type, positionX: 0, positionY: 0, config }
}

function edge(sourceNodeKey: string, targetNodeKey: string): WorkflowEdgeDTO {
  return { sourceNodeKey, targetNodeKey }
}

describe('validateGraph', () => {
  it('合法圖應無錯誤', () => {
    const errors = validateGraph([node('a'), node('b'), node('c')], [edge('a', 'b'), edge('b', 'c')])
    expect(errors).toEqual([])
  })

  it('nodeKey 重複應回報含重複 key 的錯誤', () => {
    const errors = validateGraph([node('a'), node('a')], [])
    expect(errors.length).toBeGreaterThan(0)
    expect(errors.some((e) => e.type === 'DUPLICATE_NODE_KEY' && e.key === 'a')).toBe(true)
  })

  it('edge 端點不存在應回報含缺失 key 的錯誤', () => {
    const errors = validateGraph([node('a')], [edge('a', 'missing')])
    expect(errors.some((e) => e.type === 'EDGE_NODE_NOT_FOUND' && e.key === 'missing')).toBe(true)
  })

  it('有向環應回報環錯誤', () => {
    const errors = validateGraph(
      [node('a'), node('b'), node('c')],
      [edge('a', 'b'), edge('b', 'c'), edge('c', 'a')],
    )
    expect(errors.some((e) => e.type === 'CYCLE')).toBe(true)
  })

  it('自我迴圈也應視為環', () => {
    const errors = validateGraph([node('a')], [edge('a', 'a')])
    expect(errors.some((e) => e.type === 'CYCLE')).toBe(true)
  })

  it('既有規則的 severity 為 error', () => {
    const errors = validateGraph([node('a'), node('a')], [])
    const dup = errors.find((e) => e.type === 'DUPLICATE_NODE_KEY')
    expect(dup?.severity).toBe('error')
  })
})

describe('validateGraph — handle 規則', () => {
  it('edge 帶未知 sourceHandle 應回報 UNKNOWN_HANDLE（error）', () => {
    const c = node('c', 'CONDITION')
    const t = node('t', 'TOOL')
    const e: WorkflowEdgeDTO = {
      sourceNodeKey: 'c',
      targetNodeKey: 't',
      sourceHandle: 'out:nope',
      targetHandle: 'in:main',
    }
    const errors = validateGraph([c, t], [e])
    const unknown = errors.find((x) => x.type === 'UNKNOWN_HANDLE')
    expect(unknown).toBeDefined()
    expect(unknown?.severity).toBe('error')
  })

  it('edge 帶未知 targetHandle 應回報 UNKNOWN_HANDLE', () => {
    const a = node('a', 'TRIGGER')
    const b = node('b', 'TOOL')
    const e: WorkflowEdgeDTO = {
      sourceNodeKey: 'a',
      targetNodeKey: 'b',
      sourceHandle: 'out:main',
      targetHandle: 'in:bogus',
    }
    const errors = validateGraph([a, b], [e])
    expect(errors.some((x) => x.type === 'UNKNOWN_HANDLE')).toBe(true)
  })

  it('CONDITION 兩分支皆接妥時無 handle 相關錯誤或警告', () => {
    const c = node('c', 'CONDITION')
    const t1 = node('t1', 'TOOL')
    const t2 = node('t2', 'TOOL')
    const edges: WorkflowEdgeDTO[] = [
      { sourceNodeKey: 'c', targetNodeKey: 't1', sourceHandle: 'out:true', targetHandle: 'in:main' },
      { sourceNodeKey: 'c', targetNodeKey: 't2', sourceHandle: 'out:false', targetHandle: 'in:main' },
    ]
    const errors = validateGraph([c, t1, t2], edges)
    expect(errors.some((x) => x.type === 'UNKNOWN_HANDLE')).toBe(false)
    expect(errors.some((x) => x.type === 'BRANCH_INCOMPLETE')).toBe(false)
  })

  it('CONDITION 僅接一路分支應回報 BRANCH_INCOMPLETE（warning）', () => {
    const c = node('c', 'CONDITION')
    const t1 = node('t1', 'TOOL')
    const edges: WorkflowEdgeDTO[] = [
      { sourceNodeKey: 'c', targetNodeKey: 't1', sourceHandle: 'out:true', targetHandle: 'in:main' },
    ]
    const errors = validateGraph([c, t1], edges)
    const branch = errors.find((x) => x.type === 'BRANCH_INCOMPLETE')
    expect(branch).toBeDefined()
    expect(branch?.severity).toBe('warning')
    expect(branch?.key).toBe('c')
  })
})

describe('validateGraph — 工具埠相容性（in:tool）', () => {
  it('TOOL/MCP/SKILL 連到 LLM 工具埠應無相容性錯誤', () => {
    const llm = node('llm', 'LLM_ASSISTANT')
    const tool = node('t', 'TOOL')
    const skill = node('s', 'SKILL')
    const edges: WorkflowEdgeDTO[] = [
      { sourceNodeKey: 't', targetNodeKey: 'llm', sourceHandle: 'out:main', targetHandle: 'in:tool' },
      { sourceNodeKey: 's', targetNodeKey: 'llm', sourceHandle: 'out:main', targetHandle: 'in:tool' },
    ]
    const errors = validateGraph([llm, tool, skill], edges)
    expect(errors.some((e) => e.type === 'INCOMPATIBLE_CONNECTION')).toBe(false)
  })

  it('多個 KNOWLEDGE_RAG 連到 LLM 工具埠應無相容性錯誤（自動注入型 RAG 掛載）', () => {
    const llm = node('llm', 'LLM_ASSISTANT')
    const rag1 = node('r1', 'KNOWLEDGE_RAG')
    const rag2 = node('r2', 'KNOWLEDGE_RAG')
    const edges: WorkflowEdgeDTO[] = [
      { sourceNodeKey: 'r1', targetNodeKey: 'llm', sourceHandle: 'out:main', targetHandle: 'in:tool' },
      { sourceNodeKey: 'r2', targetNodeKey: 'llm', sourceHandle: 'out:main', targetHandle: 'in:tool' },
    ]
    const errors = validateGraph([llm, rag1, rag2], edges)
    expect(errors.some((e) => e.type === 'INCOMPATIBLE_CONNECTION')).toBe(false)
  })

  it('非 TOOL/MCP/SKILL 來源連到工具埠應回報 INCOMPATIBLE_CONNECTION（error）', () => {
    const llm = node('llm', 'LLM_ASSISTANT')
    const code = node('c', 'CODE')
    const e: WorkflowEdgeDTO = {
      sourceNodeKey: 'c',
      targetNodeKey: 'llm',
      sourceHandle: 'out:main',
      targetHandle: 'in:tool',
    }
    const errors = validateGraph([llm, code], [e])
    const incompatible = errors.find((x) => x.type === 'INCOMPATIBLE_CONNECTION')
    expect(incompatible).toBeDefined()
    expect(incompatible?.severity).toBe('error')
    expect(incompatible?.key).toBe('c')
  })
})

describe('validateGraph — 提示埠相容性（in:prompt）', () => {
  const promptEdge = (source: string, target: string): WorkflowEdgeDTO => ({
    sourceNodeKey: source,
    targetNodeKey: target,
    sourceHandle: 'out:main',
    targetHandle: 'in:prompt',
  })

  it('PROMPT 連到 LLM 提示埠應無相容性錯誤', () => {
    const errors = validateGraph(
      [node('llm', 'LLM_ASSISTANT'), node('p', 'PROMPT', { prompt: '提問' })],
      [promptEdge('p', 'llm')],
    )
    expect(errors.some((e) => e.type === 'INCOMPATIBLE_CONNECTION')).toBe(false)
  })

  it('多個 PROMPT 連到同一個 LLM 提示埠應無相容性錯誤（分支擇一的接法）', () => {
    const errors = validateGraph(
      [
        node('llm', 'LLM_ASSISTANT'),
        node('p1', 'PROMPT', { prompt: 'A' }),
        node('p2', 'PROMPT', { prompt: 'B' }),
      ],
      [promptEdge('p1', 'llm'), promptEdge('p2', 'llm')],
    )
    expect(errors.some((e) => e.type === 'INCOMPATIBLE_CONNECTION')).toBe(false)
  })

  it('非 PROMPT 來源連到提示埠應回報 INCOMPATIBLE_CONNECTION（error）', () => {
    const errors = validateGraph(
      [node('llm', 'LLM_ASSISTANT'), node('c', 'CODE')],
      [promptEdge('c', 'llm')],
    )
    const incompatible = errors.find((e) => e.type === 'INCOMPATIBLE_CONNECTION')
    expect(incompatible).toBeDefined()
    expect(incompatible?.severity).toBe('error')
    expect(incompatible?.key).toBe('c')
  })
})

describe('validateGraph — 提示接線完整性（鏡射後端啟用驗證）', () => {
  const promptEdge = (source: string, target: string): WorkflowEdgeDTO => ({
    sourceNodeKey: source,
    targetNodeKey: target,
    sourceHandle: 'out:main',
    targetHandle: 'in:prompt',
  })

  it('孤兒 PROMPT 節點應回報 warning', () => {
    const errors = validateGraph([node('p', 'PROMPT', { prompt: '沒人取用' })], [])
    const wiring = errors.find((e) => e.type === 'PROMPT_WIRING_INCOMPLETE' && e.key === 'p')
    expect(wiring).toBeDefined()
    expect(wiring?.severity).toBe('warning')
  })

  it('LLM 既無 userPrompt 也無 PROMPT 連入應回報 warning', () => {
    const errors = validateGraph([node('llm', 'LLM_ASSISTANT', { llmId: 'l1' })], [])
    const wiring = errors.find((e) => e.type === 'PROMPT_WIRING_INCOMPLETE' && e.key === 'llm')
    expect(wiring).toBeDefined()
    expect(wiring?.severity).toBe('warning')
  })

  it('LLM 有 userPrompt 時不回報', () => {
    const errors = validateGraph(
      [node('llm', 'LLM_ASSISTANT', { llmId: 'l1', userPrompt: '嗨' })],
      [],
    )
    expect(errors.some((e) => e.type === 'PROMPT_WIRING_INCOMPLETE')).toBe(false)
  })

  it('LLM 無 userPrompt 但有 PROMPT 連入時兩者皆不回報', () => {
    const errors = validateGraph(
      [node('llm', 'LLM_ASSISTANT', { llmId: 'l1' }), node('p', 'PROMPT', { prompt: '提問' })],
      [promptEdge('p', 'llm')],
    )
    expect(errors.some((e) => e.type === 'PROMPT_WIRING_INCOMPLETE')).toBe(false)
  })

  it('LLM 的 userPrompt 只有空白字元時視同未填', () => {
    const errors = validateGraph([node('llm', 'LLM_ASSISTANT', { llmId: 'l1', userPrompt: '   ' })], [])
    expect(errors.some((e) => e.type === 'PROMPT_WIRING_INCOMPLETE' && e.key === 'llm')).toBe(true)
  })
})

describe('validateGraph — 必填欄位規則', () => {
  const requiredFields: NodeRequiredFields = {
    LLM_ASSISTANT: ['llmId'],
  }

  it('不傳 requiredFields 時完全不檢查必填欄位', () => {
    const errors = validateGraph([node('a', 'LLM_ASSISTANT', {})], [])
    expect(errors.some((e) => e.type === 'REQUIRED_FIELD_MISSING')).toBe(false)
  })

  it('傳入 requiredFields 且節點缺必填欄位時應回報 warning', () => {
    const errors = validateGraph([node('a', 'LLM_ASSISTANT', {})], [], requiredFields)
    const missing = errors.find((e) => e.type === 'REQUIRED_FIELD_MISSING')
    expect(missing).toBeDefined()
    expect(missing?.severity).toBe('warning')
    expect(missing?.key).toBe('a')
    // message 使用 formatMissingFields 中文化，'llmId' 顯示為 'LLM 設定'
    expect(missing?.message).toContain('LLM 設定')
  })

  it('傳入 requiredFields 但 config 齊全時不回報', () => {
    const errors = validateGraph([node('a', 'LLM_ASSISTANT', { llmId: 'l1' })], [], requiredFields)
    expect(errors.some((e) => e.type === 'REQUIRED_FIELD_MISSING')).toBe(false)
  })
})

describe('validateGraph — 多觸發點與觸發點下游', () => {
  it('多個 TRIGGER 不產生任何 error（多觸發點是刻意支援的形狀）', () => {
    const errors = validateGraph(
      [node('t1', 'TRIGGER'), node('t2', 'TRIGGER'), node('a'), node('b')],
      [edge('t1', 'a'), edge('t2', 'b')],
    )
    expect(errors.filter((e) => e.severity === 'error')).toEqual([])
  })

  it('TRIGGER 無任何出邊時回報 TRIGGER_NO_DOWNSTREAM warning', () => {
    const errors = validateGraph([node('t1', 'TRIGGER'), node('a')], [])
    const warn = errors.find((e) => e.type === 'TRIGGER_NO_DOWNSTREAM')
    expect(warn).toBeDefined()
    expect(warn?.severity).toBe('warning')
    expect(warn?.key).toBe('t1')
  })

  it('TRIGGER 有出邊時不回報 TRIGGER_NO_DOWNSTREAM', () => {
    const errors = validateGraph([node('t1', 'TRIGGER'), node('a')], [edge('t1', 'a')])
    expect(errors.some((e) => e.type === 'TRIGGER_NO_DOWNSTREAM')).toBe(false)
  })

  it('多觸發點中只有未接線那顆被回報', () => {
    const errors = validateGraph(
      [node('t1', 'TRIGGER'), node('t2', 'TRIGGER'), node('a')],
      [edge('t1', 'a')],
    )
    const warns = errors.filter((e) => e.type === 'TRIGGER_NO_DOWNSTREAM')
    expect(warns).toHaveLength(1)
    expect(warns[0].key).toBe('t2')
  })

  it('無上游的 PROMPT 節點不被回報（合法形狀，引擎照常執行）', () => {
    const errors = validateGraph(
      [node('t1', 'TRIGGER'), node('p', 'PROMPT', { prompt: 'x' }), node('llm', 'LLM_ASSISTANT', { llmId: 'l1', userPrompt: 'x' })],
      [edge('t1', 'llm'), { sourceNodeKey: 'p', targetNodeKey: 'llm', targetHandle: 'in:prompt' }],
    )
    expect(errors.filter((e) => e.severity === 'error')).toEqual([])
    expect(errors.some((e) => e.type === 'TRIGGER_NO_DOWNSTREAM')).toBe(false)
  })
})
