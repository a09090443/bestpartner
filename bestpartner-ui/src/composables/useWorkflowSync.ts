import type {
  WorkflowDTO,
  WorkflowNodeDTO,
  WorkflowEdgeDTO,
  WorkflowSaveRequestDTO,
  NodeType,
} from '../types/workflow'

/** Vue Flow 節點（僅取本專案需要的欄位） */
export interface FlowNode {
  id: string
  type: string
  position: { x: number; y: number }
  data: {
    name?: string
    type: NodeType
    config: Record<string, unknown>
  }
}

/** Vue Flow 連線（僅取本專案需要的欄位） */
export interface FlowEdge {
  id: string
  source: string
  target: string
  sourceHandle?: string | null
  targetHandle?: string | null
  label?: string
}

/** 存檔請求的 meta 部分（畫布以外的欄位） */
export interface SaveMeta {
  id?: string
  version?: number
  name: string
  description?: string
  canvasMeta?: Record<string, unknown> | null
}

/** 自訂 Vue Flow 節點型別名稱（所有語意節點共用此 type，語意放於 data.type） */
const FLOW_NODE_TYPE = 'workflow'

/**
 * 產生 edge id，帶入來源/目標 handle 以避免同一對節點的多分支撞 id。
 * handle 為空時以 'main' 佔位，例：e-c1:out:true-t1:in:main。
 */
export function makeEdgeId(
  source: string,
  target: string,
  sourceHandle?: string | null,
  targetHandle?: string | null,
): string {
  return `e-${source}:${sourceHandle ?? 'main'}-${target}:${targetHandle ?? 'main'}`
}

/**
 * WorkflowDTO → Vue Flow 的 { nodes, edges }。
 * node.id 取 nodeKey、position 取 positionX/Y、data 帶 name/type/config。
 */
export function dtoToFlow(workflow: WorkflowDTO): { nodes: FlowNode[]; edges: FlowEdge[] } {
  const nodes: FlowNode[] = workflow.nodes.map((n) => ({
    id: n.nodeKey,
    type: FLOW_NODE_TYPE,
    position: { x: n.positionX, y: n.positionY },
    data: {
      name: n.name,
      type: n.type,
      config: n.config ?? {},
    },
  }))

  const edges: FlowEdge[] = workflow.edges.map((e) => ({
    id: makeEdgeId(e.sourceNodeKey, e.targetNodeKey, e.sourceHandle, e.targetHandle),
    source: e.sourceNodeKey,
    target: e.targetNodeKey,
    sourceHandle: e.sourceHandle ?? null,
    targetHandle: e.targetHandle ?? null,
    label: e.label,
  }))

  return { nodes, edges }
}

/**
 * Vue Flow 的 nodes/edges + meta → WorkflowSaveRequestDTO。
 * 為 dtoToFlow 的反向轉換，config 物件原樣保留（含巢狀）。
 */
export function flowToSaveRequest(
  flowNodes: FlowNode[],
  flowEdges: FlowEdge[],
  meta: SaveMeta,
): WorkflowSaveRequestDTO {
  const nodes: WorkflowNodeDTO[] = flowNodes.map((n) => ({
    nodeKey: n.id,
    type: n.data.type,
    name: n.data.name,
    positionX: n.position.x,
    positionY: n.position.y,
    config: n.data.config ?? {},
  }))

  const edges: WorkflowEdgeDTO[] = flowEdges.map((e) => ({
    sourceNodeKey: e.source,
    targetNodeKey: e.target,
    sourceHandle: e.sourceHandle ?? undefined,
    targetHandle: e.targetHandle ?? undefined,
    label: e.label,
  }))

  return {
    id: meta.id,
    version: meta.version,
    name: meta.name,
    description: meta.description,
    canvasMeta: meta.canvasMeta,
    nodes,
    edges,
  }
}
