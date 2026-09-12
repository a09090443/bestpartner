import { describe, it, expect } from 'vitest'
import { layoutGraph } from '../useCanvasLayout'
import type { FlowNode, FlowEdge } from '../useWorkflowSync'

function node(id: string): FlowNode {
  return { id, type: 'workflow', position: { x: 0, y: 0 }, data: { type: 'TOOL', config: {} } }
}

function edge(source: string, target: string): FlowEdge {
  return { id: `e-${source}-${target}`, source, target }
}

describe('layoutGraph', () => {
  it('LR 方向：鏈狀 a→b→c 的 x 座標依序遞增', () => {
    const nodes = [node('a'), node('b'), node('c')]
    const edges = [edge('a', 'b'), edge('b', 'c')]
    const result = layoutGraph(nodes, edges)

    const x = (id: string) => result.find((n) => n.id === id)!.position.x
    expect(x('a')).toBeLessThan(x('b'))
    expect(x('b')).toBeLessThan(x('c'))
  })

  it('保留節點數量與 data，不改動 id/type/data', () => {
    const nodes = [node('a'), node('b')]
    const result = layoutGraph(nodes, [edge('a', 'b')])
    expect(result).toHaveLength(2)
    const a = result.find((n) => n.id === 'a')!
    expect(a.data).toEqual({ type: 'TOOL', config: {} })
    expect(a.type).toBe('workflow')
  })

  it('回傳的 position 為數字', () => {
    const result = layoutGraph([node('a')], [])
    expect(typeof result[0].position.x).toBe('number')
    expect(typeof result[0].position.y).toBe('number')
    expect(Number.isNaN(result[0].position.x)).toBe(false)
  })

  it('空節點回傳空陣列', () => {
    expect(layoutGraph([], [])).toEqual([])
  })

  it('回傳新陣列與新節點物件，不變動輸入', () => {
    const nodes = [node('a')]
    const result = layoutGraph(nodes, [])
    expect(result).not.toBe(nodes)
    expect(result[0]).not.toBe(nodes[0])
    expect(nodes[0].position).toEqual({ x: 0, y: 0 })
  })
})
