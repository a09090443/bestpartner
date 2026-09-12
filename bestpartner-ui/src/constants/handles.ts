/**
 * Workflow 連接點（handle）編碼工具。
 *
 * handle id 採語意字串 `role:port`：
 * - role：`in`（輸入）或 `out`（輸出）
 * - port：埠名稱，如 `main` / `prompt` / `tool` / `true` / `false` / `loop` / `done`
 *
 * 此字串直接作為 Vue Flow Handle 的 id，並存入 DTO 的 sourceHandle / targetHandle。
 */

/** 預設輸入埠 */
export const IN_MAIN = 'in:main'
/** 預設輸出埠 */
export const OUT_MAIN = 'out:main'
/**
 * LLM 節點的工具輸入埠（Agent 模式能力掛載）。
 * TOOL / MCP_SERVER / SKILL 節點以 out:main 連到此埠，代表「掛載為 LLM 可自主呼叫的工具集」，
 * 而非一般資料流連線。後端 WorkflowEngine 以 targetHandle === 'in:tool' 辨識此語義。
 */
export const IN_TOOL = 'in:tool'
/**
 * LLM 節點的提示輸入埠。PROMPT 節點以 out:main 連到此埠，提供本次推論的提問內容。
 *
 * ⚠️ 與 IN_TOOL 語義相反：這是**一般資料流連線**，會參與後端的節點活化判斷與拓撲排序，
 * 故 CONDITION 分支可只活化其中一個提示節點，讓同一顆 LLM 依分支取得不同提問。
 * 後端 WorkflowEngine 以 targetHandle === 'in:prompt' 辨識此語義。
 */
export const IN_PROMPT = 'in:prompt'

/** handle 角色 */
export type HandleRole = 'in' | 'out'

/** parseHandle 的解析結果 */
export interface ParsedHandle {
  role: HandleRole
  port: string
}

/**
 * 解析 handle id 字串為 { role, port }。
 * 僅接受 `in:` / `out:` 開頭且 port 非空者，否則回傳 null。
 * port 內若含冒號（如 out:a:b），只切第一個冒號。
 */
export function parseHandle(id: string): ParsedHandle | null {
  const idx = id.indexOf(':')
  if (idx < 0) return null

  const role = id.slice(0, idx)
  const port = id.slice(idx + 1)
  if (role !== 'in' && role !== 'out') return null
  if (port.length === 0) return null

  return { role, port }
}
