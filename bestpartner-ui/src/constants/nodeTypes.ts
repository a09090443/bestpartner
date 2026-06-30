import type { NodeType } from '../types/workflow'

/** 單一節點型別的顯示中繼資料 */
export interface NodeTypeMeta {
  type: NodeType
  /** 顯示名稱 */
  label: string
  /** 代表色（節點色塊/邊框） */
  color: string
  /** 圖示（emoji，避免額外圖示依賴） */
  icon: string
  /** 是否為觸發節點（畫布上特別標示，啟用 workflow 的必要節點） */
  isTrigger?: boolean
}

/** 依顯示順序排列的節點型別清單（共 10 種） */
export const NODE_TYPE_METAS: NodeTypeMeta[] = [
  { type: 'TRIGGER', label: '觸發', color: '#f56c6c', icon: '⚡', isTrigger: true },
  { type: 'LLM_ASSISTANT', label: 'LLM 助手', color: '#409eff', icon: '🤖' },
  { type: 'TOOL', label: '工具', color: '#67c23a', icon: '🔧' },
  { type: 'MCP_SERVER', label: 'MCP 伺服器', color: '#9254de', icon: '🔌' },
  { type: 'KNOWLEDGE_RAG', label: '知識庫 RAG', color: '#13c2c2', icon: '📚' },
  { type: 'CONDITION', label: '條件判斷', color: '#e6a23c', icon: '🔀' },
  { type: 'LOOP', label: '迴圈', color: '#fa8c16', icon: '🔁' },
  { type: 'CODE', label: '程式碼', color: '#606266', icon: '📝' },
  { type: 'HTTP_REQUEST', label: 'HTTP 請求', color: '#2f54eb', icon: '🌐' },
  { type: 'DATA_TRANSFORM', label: '資料轉換', color: '#eb2f96', icon: '🔄' },
]

/** 以 type 快速查詢中繼資料 */
export const NODE_TYPE_META_MAP: Record<NodeType, NodeTypeMeta> = NODE_TYPE_METAS.reduce(
  (acc, meta) => {
    acc[meta.type] = meta
    return acc
  },
  {} as Record<NodeType, NodeTypeMeta>,
)

/** 取得指定型別的中繼資料（找不到回傳 undefined） */
export function getNodeTypeMeta(type: NodeType): NodeTypeMeta | undefined {
  return NODE_TYPE_META_MAP[type]
}
