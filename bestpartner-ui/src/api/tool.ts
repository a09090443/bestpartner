import http from './http'
import type { ApiResponse } from '../types/api'
import type { Option } from '../types/options'
import type { ToolSettingSchema } from '../types/toolSchema'

/** 後端工具 DTO（下拉與設定表單所需欄位） */
export interface ToolDTO {
  id?: string
  name?: string
  settingSchema?: ToolSettingSchema | null
  settingId?: string
  alias?: string
  settingContent?: string
}

/** 列出所有已註冊工具，轉為下拉 Option[]（value = 工具 id，label = name） */
export async function listTools(): Promise<Option[]> {
  const res = await http.get<ApiResponse<ToolDTO[]>>('/llm/tool/list')
  const list = res.data.data ?? []
  return list
    .filter((t): t is ToolDTO & { id: string } => !!t.id)
    .map((t) => ({ value: t.id, label: t.name || t.id }))
}

/** 取得單一工具詳情（含 settingSchema） */
export async function getTool(id: string): Promise<ToolDTO> {
  const res = await http.post<ApiResponse<ToolDTO>>('/llm/tool/get', { id })
  return res.data.data ?? {}
}

/** 建立工具設定（id + alias），回傳含 settingId 的 DTO */
export async function saveToolSetting(id: string, alias: string): Promise<ToolDTO> {
  const res = await http.post<ApiResponse<ToolDTO>>('/llm/tool/saveSetting', { id, alias })
  return res.data.data ?? {}
}

/** 更新工具設定內容（settingId + settingContent JSON 字串） */
export async function updateToolSetting(settingId: string, settingContent: string): Promise<void> {
  await http.post<ApiResponse<unknown>>('/llm/tool/updateSetting', { settingId, settingContent })
}
