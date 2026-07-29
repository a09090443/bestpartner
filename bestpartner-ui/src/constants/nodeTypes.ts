import type { NodeType } from '../types/workflow'
import { IN_MAIN, IN_PROMPT, IN_TOOL, OUT_MAIN } from './handles'

/** 節點分類（palette 分組／overview 圖例用），固定四類 */
export type NodeCategory = 'Trigger' | 'Action' | 'AI' | 'Logic'

/**
 * 查不到型別中繼資料時的代表色（MiniMap 著色、Inspector icon box、圖例等共用）。
 * 取自主題的 --wf-text-3，但這些使用點需要 JS 端的字串值，無法直接引用 CSS 變數。
 */
export const NODE_FALLBACK_COLOR = '#8c90a0'

/** 分類固定顯示順序 */
export const NODE_CATEGORIES: NodeCategory[] = ['Trigger', 'Action', 'AI', 'Logic']

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
  /** 所屬分類（palette 分組顯示） */
  category: NodeCategory
  /** 是否為觸發節點（畫布上特別標示，啟用 workflow 的必要節點） */
  isTrigger?: boolean
  /** 輸入埠（左緣）；TRIGGER 為空 */
  inputs: PortMeta[]
  /** 輸出埠（右緣）；CONDITION / LOOP 為多埠 */
  outputs: PortMeta[]
  /**
   * 自 palette 拖入畫布時帶入的初始 config。
   * 只在「該欄位必填、且目前只有唯一合法值」時才給——省去使用者手填一個沒有選擇餘地的值。
   * ⚠️ 取用時務必展開複製（`{ ...meta.defaultConfig }`），否則同型別的多個節點會共用同一個物件。
   */
  defaultConfig?: Record<string, unknown>
}

/** 預設單輸入埠 */
const DEFAULT_INPUTS: PortMeta[] = [{ id: IN_MAIN }]
/** 預設單輸出埠 */
const DEFAULT_OUTPUTS: PortMeta[] = [{ id: OUT_MAIN }]

/** 依顯示順序排列的節點型別清單（共 13 種） */
export const NODE_TYPE_METAS: NodeTypeMeta[] = [
  {
    type: 'TRIGGER',
    label: '觸發',
    color: '#f56c6c',
    category: 'Trigger',
    isTrigger: true,
    inputs: [],
    outputs: DEFAULT_OUTPUTS,
    // triggerType 是 TRIGGER 唯一的必填欄位，而目前 WEBHOOK / CRON 尚未實作，
    // 唯一可用值就是 MANUAL——不預設會讓每個使用者每張流程都得手動補一次。
    defaultConfig: { triggerType: 'MANUAL' },
  },
  {
    type: 'LLM_ASSISTANT',
    label: 'LLM 助手',
    color: '#409eff',
    category: 'AI',
    // in:prompt 為提問來源埠（一般資料流，供 PROMPT 節點連入）；
    // in:tool 為 Agent 模式的能力掛載埠（非資料流），供 TOOL / MCP_SERVER / SKILL / KNOWLEDGE_RAG 連入
    inputs: [
      { id: IN_MAIN, label: '輸入' },
      { id: IN_PROMPT, label: '提示' },
      { id: IN_TOOL, label: '工具' },
    ],
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'PROMPT',
    label: '提示詞',
    color: '#b37feb',
    category: 'AI',
    // 可被上游驅動（如接在 CONDITION 分支後），輸出以 out:main 連到 LLM 的 in:prompt 埠
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'TOOL',
    label: '工具',
    color: '#67c23a',
    category: 'Action',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'MCP_SERVER',
    label: 'MCP 伺服器',
    color: '#9254de',
    category: 'Action',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'SKILL',
    label: 'Skill',
    color: '#a0d911',
    category: 'AI',
    // Skill 為能力提供者：只作為 LLM 節點 in:tool 埠的來源，故無輸入埠
    inputs: [],
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'KNOWLEDGE_RAG',
    label: '知識庫 RAG',
    color: '#13c2c2',
    category: 'AI',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'CONDITION',
    label: '條件判斷',
    color: '#e6a23c',
    category: 'Logic',
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
    category: 'Logic',
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
    category: 'Logic',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'HTTP_REQUEST',
    label: 'HTTP 請求',
    color: '#2f54eb',
    category: 'Action',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'DATA_TRANSFORM',
    label: '資料轉換',
    color: '#eb2f96',
    category: 'Logic',
    inputs: DEFAULT_INPUTS,
    outputs: DEFAULT_OUTPUTS,
  },
  {
    type: 'OUTPUT',
    label: '輸出',
    color: '#52c41a',
    category: 'Action',
    inputs: DEFAULT_INPUTS,
    outputs: [],
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

/** 取得指定分類下的節點型別（維持 NODE_TYPE_METAS 順序） */
export function getNodesByCategory(category: NodeCategory): NodeTypeMeta[] {
  return NODE_TYPE_METAS.filter((meta) => meta.category === category)
}
