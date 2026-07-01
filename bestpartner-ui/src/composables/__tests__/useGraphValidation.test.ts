import { describe, it, expect } from 'vitest'
import { validateGraph } from '../useGraphValidation'
import type { NodeType, WorkflowNodeDTO, WorkflowEdgeDTO } from '../../types/workflow'

function node(nodeKey: string, type: NodeType = 'TOOL'): WorkflowNodeDTO {
  return { nodeKey, type, positionX: 0, positionY: 0, config: {} }
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
