import type { NodeRequiredFields } from '../api/workflow'

/**
 * 欄位鍵 → 中文顯示標籤映射。
 *
 * ⚠️ 這是「純顯示用」的 i18n 標籤表，不是必填規則本身——
 * 必填規則的唯一事實來源仍是後端 NodeConfig 契約（見 {@link NodeRequiredFields}，
 * 來自 GET /llm/workflow/nodeRequiredFields）。此表只負責把後端回傳的英文欄位鍵
 * 轉為使用者看得懂的中文提示文字，找不到映射的鍵會 fallback 顯示原始 key，
 * 不會因映射缺漏而漏顯示或出錯。
 */
const FIELD_LABELS: Record<string, string> = {
  llmId: 'LLM 設定',
  toolId: '工具',
  mcpId: 'MCP 伺服器',
  skillId: 'Skill',
  toolName: '工具名稱',
  knowledgeId: '知識庫',
  embeddingModelId: 'Embedding 模型',
  query: '檢索語句',
  triggerType: '觸發類型',
  conditions: '條件',
  mappings: '欄位對應',
  template: '範本',
  method: 'HTTP 方法',
  url: 'URL',
  inputArrayPath: '輸入陣列路徑',
  loopBodyEntryNodeKey: '迴圈起始節點',
  language: '語言',
  source: '程式碼',
}

/** 取得欄位鍵的中文顯示標籤；找不到映射時 fallback 回傳原始鍵名 */
function fieldLabel(key: string): string {
  return FIELD_LABELS[key] ?? key
}

/**
 * 將 {@link missingRequiredForNode} 回傳的缺漏欄位鍵陣列格式化為可讀的中文提示字串。
 *
 * 複合字樣（如 `"mappings|template"`）轉為「欄位對應 或 範本（擇一）」，
 * 讓使用者理解這是擇一滿足的語意，而非兩者皆缺。
 */
export function formatMissingFields(fields: string[]): string {
  return fields
    .map((field) => {
      if (field.includes('|')) {
        const labels = field.split('|').map(fieldLabel).join(' 或 ')
        return `${labels}（擇一）`
      }
      return fieldLabel(field)
    })
    .join('、')
}

/** 判斷欄位值是否視為「空」：undefined / null / 空字串 / 空陣列 / 空物件 */
function isEmptyValue(value: unknown): boolean {
  if (value === undefined || value === null) return true
  if (typeof value === 'string') return value.trim().length === 0
  if (Array.isArray(value)) return value.length === 0
  // 非 null 非陣列的物件（config 值來自 JSON，不含 Date/Map 等特殊物件）：無自身鍵即視為空，
  // 讓 object 型必填欄位（如未來的 mappings）以 {} 也能正確判為缺漏
  if (typeof value === 'object') return Object.keys(value as object).length === 0
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
