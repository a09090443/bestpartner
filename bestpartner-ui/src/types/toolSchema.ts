/** 後端 ToolSchemaGenerator 反射產生的單一設定欄位描述 */
export interface ToolSettingFieldSchema {
  type: 'string' | 'integer' | 'number' | 'boolean' | 'array'
  required: boolean
  sensitive?: boolean
  description?: string
}

/** 工具 settingSchema：欄位名 -> 欄位描述 */
export type ToolSettingSchema = Record<string, ToolSettingFieldSchema>
