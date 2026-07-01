import http from './http'
import type { ApiResponse } from '../types/api'
import type { Option } from '../types/options'

/** 後端 LLM 設定 DTO（僅取下拉需要的欄位） */
interface LLMSettingDTO {
  id?: string
  alias?: string
  modelType?: string
}

/**
 * 查詢當前用戶的 LLM 設定，轉為下拉 Option[]。
 * 空 body 取全部；value = 設定 id（即 chat 端點使用的 llmId），label = alias。
 */
export async function getSettings(): Promise<Option[]> {
  const res = await http.post<ApiResponse<LLMSettingDTO[]>>('/llm/setting/get', {})
  const list = res.data.data ?? []
  return list
    .filter((s): s is LLMSettingDTO & { id: string } => !!s.id)
    .map((s) => ({ value: s.id, label: s.alias || s.id }))
}
