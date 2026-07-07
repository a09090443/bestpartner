/**
 * 解析「JSON 物件」欄位的輸入文字。
 *
 * 僅在文字為合法 JSON 且解析結果為「純物件」（非陣列、非字串/數字/布林/null）時回傳該物件；
 * 文字為空、parse 失敗、或結果非純物件（如 `[1,2]`、`"hi"`、`123`、`true`）一律回 null。
 *
 * 供 arguments / outputSchema 等後端型別為 JsonObject 的欄位共用，確保只有合法物件會被
 * 寫入 config；否則後端 JsonObject 反序列化會拋例外，導致整張 workflow save 失敗。
 */
export function parseJsonObjectField(text: string): Record<string, unknown> | null {
  const trimmed = text.trim()
  if (!trimmed) return null
  let parsed: unknown
  try {
    parsed = JSON.parse(trimmed)
  } catch {
    return null
  }
  if (typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)) {
    return parsed as Record<string, unknown>
  }
  return null
}
