import type { NodeRequiredFields } from '../api/workflow'

/** 判斷欄位值是否視為「空」：undefined / null / 空字串 / 空陣列 */
function isEmptyValue(value: unknown): boolean {
  if (value === undefined || value === null) return true
  if (typeof value === 'string') return value.trim().length === 0
  if (Array.isArray(value)) return value.length === 0
  return false
}

/**
 * 依後端必填清單計算節點缺漏的必填欄位。
 *
 * 複合字樣（如 `"mappings|template"`）代表多個欄位擇一有值即滿足；
 * 全數為空時回傳原始複合字樣（而非拆開的個別欄位名），方便 UI 顯示與後端訊息對應。
 */
export function missingRequiredForNode(
  type: string,
  config: Record<string, unknown>,
  requiredFields: NodeRequiredFields,
): string[] {
  const fields = requiredFields[type]
  if (!fields || fields.length === 0) return []

  const missing: string[] = []
  fields.forEach((field) => {
    if (field.includes('|')) {
      const candidates = field.split('|')
      const satisfied = candidates.some((c) => !isEmptyValue(config[c]))
      if (!satisfied) missing.push(field)
      return
    }
    if (isEmptyValue(config[field])) missing.push(field)
  })
  return missing
}
