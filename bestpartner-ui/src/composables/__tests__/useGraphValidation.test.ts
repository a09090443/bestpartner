import { describe, it, expect } from 'vitest'
import { validateGraph } from '../useGraphValidation'
import type { WorkflowNodeDTO, WorkflowEdgeDTO } from '../../types/workflow'

function node(nodeKey: string): WorkflowNodeDTO {
  return { nodeKey, type: 'TOOL', positionX: 0, positionY: 0, config: {} }
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
})
