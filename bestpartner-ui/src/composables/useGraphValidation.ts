import type { WorkflowNodeDTO, WorkflowEdgeDTO } from '../types/workflow'
import { getNodeTypeMeta } from '../constants/nodeTypes'
import { missingRequiredForNode } from '../utils/nodeRequiredFields'
import type { NodeRequiredFields } from '../api/workflow'

/** 驗證錯誤型別 */
export type GraphErrorType =
  | 'DUPLICATE_NODE_KEY'
  | 'EDGE_NODE_NOT_FOUND'
  | 'CYCLE'
  | 'UNKNOWN_HANDLE'
  | 'BRANCH_INCOMPLETE'
  | 'REQUIRED_FIELD_MISSING'

/** 驗證嚴重度：error 擋存檔；warning 可存檔、啟用時再擋 */
export type GraphErrorSeverity = 'error' | 'warning'

/** 單一驗證結果 */
export interface GraphValidationError {
  type: GraphErrorType
  severity: GraphErrorSeverity
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
 *
 * @param requiredFields 選填。傳入時額外依後端 NodeConfig 契約檢查各節點必填欄位是否缺漏
 *   （severity 一律為 warning；是否升級為擋存 error 由呼叫端依 workflow 狀態決定）。
 *   不傳則完全跳過此檢查，維持既有呼叫端行為不變。
 */
export function validateGraph(
  nodes: WorkflowNodeDTO[],
  edges: WorkflowEdgeDTO[],
  requiredFields?: NodeRequiredFields,
): GraphValidationError[] {
  const errors: GraphValidationError[] = []

  // nodeKey 唯一
  const keys = new Set<string>()
  const seen = new Set<string>()
  const nodeByKey = new Map<string, WorkflowNodeDTO>()
  nodes.forEach((n) => {
    if (keys.has(n.nodeKey) && !seen.has(n.nodeKey)) {
      seen.add(n.nodeKey)
      errors.push({
        type: 'DUPLICATE_NODE_KEY',
        severity: 'error',
        key: n.nodeKey,
        message: `節點 key 重複：${n.nodeKey}`,
      })
    }
    keys.add(n.nodeKey)
    nodeByKey.set(n.nodeKey, n)
  })

  // edge 端點存在
  edges.forEach((e) => {
    if (!keys.has(e.sourceNodeKey)) {
      errors.push({
        type: 'EDGE_NODE_NOT_FOUND',
        severity: 'error',
        key: e.sourceNodeKey,
        message: `連線來源節點不存在：${e.sourceNodeKey}`,
      })
    }
    if (!keys.has(e.targetNodeKey)) {
      errors.push({
        type: 'EDGE_NODE_NOT_FOUND',
        severity: 'error',
        key: e.targetNodeKey,
        message: `連線目標節點不存在：${e.targetNodeKey}`,
      })
    }
  })

  // 未知 handle：edge 帶的 handle 須存在於對應節點 meta 定義的埠內（error）
  edges.forEach((e) => {
    const source = nodeByKey.get(e.sourceNodeKey)
    if (e.sourceHandle && source) {
      const outputs = getNodeTypeMeta(source.type)?.outputs ?? []
      if (!outputs.some((p) => p.id === e.sourceHandle)) {
        errors.push({
          type: 'UNKNOWN_HANDLE',
          severity: 'error',
          key: e.sourceNodeKey,
          message: `未知的輸出埠：${e.sourceHandle}（節點 ${e.sourceNodeKey}）`,
        })
      }
    }
    const target = nodeByKey.get(e.targetNodeKey)
    if (e.targetHandle && target) {
      const inputs = getNodeTypeMeta(target.type)?.inputs ?? []
      if (!inputs.some((p) => p.id === e.targetHandle)) {
        errors.push({
          type: 'UNKNOWN_HANDLE',
          severity: 'error',
          key: e.targetNodeKey,
          message: `未知的輸入埠：${e.targetHandle}（節點 ${e.targetNodeKey}）`,
        })
      }
    }
  })

  // 分支未接完：CONDITION 的任一輸出埠未接（warning）
  nodes
    .filter((n) => n.type === 'CONDITION')
    .forEach((n) => {
      const outputs = getNodeTypeMeta(n.type)?.outputs ?? []
      const connected = new Set(
        edges.filter((e) => e.sourceNodeKey === n.nodeKey).map((e) => e.sourceHandle),
      )
      const missing = outputs.some((p) => !connected.has(p.id))
      if (missing) {
        errors.push({
          type: 'BRANCH_INCOMPLETE',
          severity: 'warning',
          key: n.nodeKey,
          message: `條件節點分支未接完：${n.nodeKey}`,
        })
      }
    })

  // 必填欄位缺漏：僅在呼叫端提供 requiredFields（後端契約）時檢查
  if (requiredFields) {
    nodes.forEach((n) => {
      const missing = missingRequiredForNode(n.type, n.config ?? {}, requiredFields)
      if (missing.length > 0) {
        errors.push({
          type: 'REQUIRED_FIELD_MISSING',
          severity: 'warning',
          key: n.nodeKey,
          message: `節點 ${n.nodeKey} 缺少必填欄位：${missing.join('、')}`,
        })
      }
    })
  }

  // 有向環
  const nodeKeys = nodes.map((n) => n.nodeKey)
  const edgePairs: Array<[string, string]> = edges.map((e) => [e.sourceNodeKey, e.targetNodeKey])
  if (hasCycle(nodeKeys, edgePairs)) {
    errors.push({ type: 'CYCLE', severity: 'error', message: '畫布存在有向環，請移除循環連線' })
  }

  return errors
}
