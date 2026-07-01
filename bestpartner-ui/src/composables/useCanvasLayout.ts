import dagre from '@dagrejs/dagre'
import type { FlowNode, FlowEdge } from './useWorkflowSync'

/** 排版參數（以 Vue Flow 座標為單位） */
export interface LayoutOptions {
  /** 節點寬（供 dagre 計算間距用） */
  nodeWidth?: number
  /** 節點高 */
  nodeHeight?: number
  /** 同層節點間距 */
  nodeSep?: number
  /** 層與層間距 */
  rankSep?: number
}

const DEFAULTS: Required<LayoutOptions> = {
  nodeWidth: 160,
  nodeHeight: 64,
  nodeSep: 40,
  rankSep: 80,
}

/**
 * 以 dagre 對節點做左至右（LR）自動排版，回傳帶新 position 的節點陣列。
 * 純函式：不變動輸入；只更新 position，其餘欄位原樣保留。
 * dagre 回傳中心座標，轉為 Vue Flow 的左上角座標。
 */
export function layoutGraph(
  nodes: FlowNode[],
  edges: FlowEdge[],
  options: LayoutOptions = {},
): FlowNode[] {
  if (nodes.length === 0) return []

  const opt = { ...DEFAULTS, ...options }
  const g = new dagre.graphlib.Graph()
  g.setGraph({ rankdir: 'LR', nodesep: opt.nodeSep, ranksep: opt.rankSep })
  g.setDefaultEdgeLabel(() => ({}))

  nodes.forEach((n) => g.setNode(n.id, { width: opt.nodeWidth, height: opt.nodeHeight }))
  edges.forEach((e) => {
    // 僅為存在的節點建立邊，避免 dagre 因懸空端點新增幽靈節點
    if (g.hasNode(e.source) && g.hasNode(e.target)) g.setEdge(e.source, e.target)
  })

  dagre.layout(g)

  return nodes.map((n) => {
    const pos = g.node(n.id)
    return {
      ...n,
      position: { x: pos.x - opt.nodeWidth / 2, y: pos.y - opt.nodeHeight / 2 },
    }
  })
}
