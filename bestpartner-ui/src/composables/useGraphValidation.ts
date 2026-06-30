import type { WorkflowNodeDTO, WorkflowEdgeDTO } from '../types/workflow'

/** 驗證錯誤型別（與後端 validateGraph 規則同義） */
export type GraphErrorType = 'DUPLICATE_NODE_KEY' | 'EDGE_NODE_NOT_FOUND' | 'CYCLE'

/** 單一驗證錯誤 */
export interface GraphValidationError {
  type: GraphErrorType
  message: string
  /** 相關 nodeKey（環錯誤無對應單一 key） */
  key?: string
}

/**
 * 以 Kahn 拓樸排序偵測有向環：可排序節點數 < 總節點數即存在環。
 */
function hasCycle(nodeKeys: string[], edges: Array<[string, string]>): boolean {
  const indegree = new Map<string, number>()
  nodeKeys.forEach((k) => indegree.set(k, 0))
  const adj = new Map<string, string[]>()
  edges.forEach(([source, target]) => {
    if (!adj.has(source)) adj.set(source, [])
    adj.get(source)!.push(target)
    indegree.set(target, (indegree.get(target) ?? 0) + 1)
  })

  const queue: string[] = []
  indegree.forEach((deg, key) => {
    if (deg === 0) queue.push(key)
  })

  let visited = 0
  while (queue.length > 0) {
    const current = queue.shift()!
    visited++
    adj.get(current)?.forEach((next) => {
      const deg = (indegree.get(next) ?? 0) - 1
      indegree.set(next, deg)
      if (deg === 0) queue.push(next)
    })
  }
  return visited !== nodeKeys.length
}

/**
 * 輕量畫布驗證（前端存檔前先擋）：nodeKey 唯一、edge 端點存在、無有向環。
 * 與後端 WorkflowService.validateGraph 同義，但收集所有錯誤而非遇到第一個即中止。
 */
export function validateGraph(
  nodes: WorkflowNodeDTO[],
  edges: WorkflowEdgeDTO[],
): GraphValidationError[] {
  const errors: GraphValidationError[] = []

  // nodeKey 唯一
  const keys = new Set<string>()
  const seen = new Set<string>()
  nodes.forEach((n) => {
    if (keys.has(n.nodeKey) && !seen.has(n.nodeKey)) {
      seen.add(n.nodeKey)
      errors.push({
        type: 'DUPLICATE_NODE_KEY',
        key: n.nodeKey,
        message: `節點 key 重複：${n.nodeKey}`,
      })
    }
    keys.add(n.nodeKey)
  })

  // edge 端點存在
  edges.forEach((e) => {
    if (!keys.has(e.sourceNodeKey)) {
      errors.push({
        type: 'EDGE_NODE_NOT_FOUND',
        key: e.sourceNodeKey,
        message: `連線來源節點不存在：${e.sourceNodeKey}`,
      })
    }
    if (!keys.has(e.targetNodeKey)) {
      errors.push({
        type: 'EDGE_NODE_NOT_FOUND',
        key: e.targetNodeKey,
        message: `連線目標節點不存在：${e.targetNodeKey}`,
      })
    }
  })

  // 有向環
  const nodeKeys = nodes.map((n) => n.nodeKey)
  const edgePairs: Array<[string, string]> = edges.map((e) => [e.sourceNodeKey, e.targetNodeKey])
  if (hasCycle(nodeKeys, edgePairs)) {
    errors.push({ type: 'CYCLE', message: '畫布存在有向環，請移除循環連線' })
  }

  return errors
}
