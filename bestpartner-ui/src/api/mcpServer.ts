import http from './http'
import type { ApiResponse } from '../types/api'
import type { Option } from '../types/options'

/** 後端 MCP 伺服器 DTO（僅取下拉需要的欄位） */
interface McpDTO {
  mcpId?: string
  name?: string
}

/** 列出所有已註冊 MCP 伺服器，轉為下拉 Option[]（value = mcpId，label = name） */
export async function listMcpServers(): Promise<Option[]> {
  const res = await http.get<ApiResponse<McpDTO[]>>('/llm/mcpServer/list')
  const list = res.data.data ?? []
  return list
    .filter((m): m is McpDTO & { mcpId: string } => !!m.mcpId)
    .map((m) => ({ value: m.mcpId, label: m.name || m.mcpId }))
}
