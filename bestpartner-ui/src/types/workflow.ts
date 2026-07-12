/**
 * Workflow 相關 TS 型別定義
 * 對應後端 WorkflowDTO 系列 DTO
 */

/** 節點類型 */
export type NodeType =
  | 'TRIGGER'
  | 'LLM_ASSISTANT'
  | 'TOOL'
  | 'MCP_SERVER'
  | 'SKILL'
  | 'KNOWLEDGE_RAG'
  | 'CONDITION'
  | 'LOOP'
  | 'CODE'
  | 'HTTP_REQUEST'
  | 'DATA_TRANSFORM'
  | 'OUTPUT'

/** Workflow 狀態 */
export type WorkflowStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE'

/** 節點資料傳輸物件 */
export interface WorkflowNodeDTO {
  nodeKey: string
  type: NodeType
  name?: string
  positionX: number
  positionY: number
  config: Record<string, unknown>
}

/** 連線資料傳輸物件 */
export interface WorkflowEdgeDTO {
  sourceNodeKey: string
  targetNodeKey: string
  sourceHandle?: string
  targetHandle?: string
  label?: string
  condition?: Record<string, unknown> | null
}

/** Workflow 完整資料傳輸物件（含 nodes / edges） */
export interface WorkflowDTO {
  id?: string
  name: string
  description?: string
  status?: WorkflowStatus
  version?: number
  nodes: WorkflowNodeDTO[]
  edges: WorkflowEdgeDTO[]
  canvasMeta?: Record<string, unknown> | null
}

/** 儲存 Workflow 的請求 DTO */
export interface WorkflowSaveRequestDTO {
  id?: string
  version?: number
  name: string
  description?: string
  nodes: WorkflowNodeDTO[]
  edges: WorkflowEdgeDTO[]
  canvasMeta?: Record<string, unknown> | null
}

/** Workflow 清單摘要 DTO */
export interface WorkflowSummaryDTO {
  id?: string
  name?: string
  status?: WorkflowStatus
  version?: number
  updatedAt?: string
}
