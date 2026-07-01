import http from './http'
import type { ApiResponse } from '../types/api'
import type { Option } from '../types/options'

/** 後端工具 DTO（僅取下拉需要的欄位） */
interface ToolDTO {
  id?: string
  name?: string
}

/** 列出所有已註冊工具，轉為下拉 Option[]（value = 工具 id，label = name） */
export async function listTools(): Promise<Option[]> {
  const res = await http.get<ApiResponse<ToolDTO[]>>('/llm/tool/list')
  const list = res.data.data ?? []
  return list
    .filter((t): t is ToolDTO & { id: string } => !!t.id)
    .map((t) => ({ value: t.id, label: t.name || t.id }))
}
