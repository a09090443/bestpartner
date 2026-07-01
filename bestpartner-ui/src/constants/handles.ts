/**
 * Workflow 連接點（handle）編碼工具。
 *
 * handle id 採語意字串 `role:port`：
 * - role：`in`（輸入）或 `out`（輸出）
 * - port：埠名稱，如 `main` / `true` / `false` / `loop` / `done`
 *
 * 此字串直接作為 Vue Flow Handle 的 id，並存入 DTO 的 sourceHandle / targetHandle。
 */

/** 預設輸入埠 */
export const IN_MAIN = 'in:main'
/** 預設輸出埠 */
export const OUT_MAIN = 'out:main'

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
