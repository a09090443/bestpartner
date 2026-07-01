import type { NodeType } from '../types/workflow'
import { IN_MAIN, OUT_MAIN } from './handles'

/** 單一連接點（handle）定義 */
export interface PortMeta {
  /** handle id，如 'in:main' / 'out:true'，直接作為 Vue Flow Handle 的 id */
  id: string
  /** 顯示文字（多埠時渲染於埠旁，如 True / False / 迴圈 / 結束） */
  label?: string
}

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
  /** 輸入埠（左緣）；TRIGGER 為空 */
  inputs: PortMeta[]
  /** 輸出埠（右緣）；CONDITION / LOOP 為多埠 */
  outputs: PortMeta[]
}

/** 預設單輸入埠 */
const DEFAULT_INPUTS: PortMeta[] = [{ id: IN_MAIN }]
/** 預設單輸出埠 */
const DEFAULT_OUTPUTS: PortMeta[] = [{ id: OUT_MAIN }]

/** 依顯示順序排列的節點型別清單（共 10 種） */
export const NODE_TYPE_METAS: NodeTypeMeta[] = [
  {
    type: 'TRIGGER',
    label: '觸發',
    color: '#f56c6c',
    icon: '⚡',
    isTrigger: true,
    inputs: [],
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'LLM_ASSISTANT',
    label: 'LLM 助手',
    color: '#409eff',
    icon: '🤖',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'TOOL',
    label: '工具',
    color: '#67c23a',
    icon: '🔧',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'MCP_SERVER',
    label: 'MCP 伺服器',
    color: '#9254de',
    icon: '🔌',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'KNOWLEDGE_RAG',
    label: '知識庫 RAG',
    color: '#13c2c2',
    icon: '📚',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'CONDITION',
    label: '條件判斷',
    color: '#e6a23c',
    icon: '🔀',
    inputs: DEFAULT_INPUTS,
    outputs: [
      { id: 'out:true', label: 'True' },
      { id: 'out:false', label: 'False' },
    ],
  },
  {
    type: 'LOOP',
    label: '迴圈',
    color: '#fa8c16',
    icon: '🔁',
    inputs: DEFAULT_INPUTS,
    outputs: [
      { id: 'out:loop', label: '迴圈' },
      { id: 'out:done', label: '結束' },
    ],
  },
  {
    type: 'CODE',
    label: '程式碼',
    color: '#606266',
    icon: '📝',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'HTTP_REQUEST',
    label: 'HTTP 請求',
    color: '#2f54eb',
    icon: '🌐',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'DATA_TRANSFORM',
    label: '資料轉換',
    color: '#eb2f96',
    icon: '🔄',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
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
