import http from './http'
import type { ApiResponse } from '../types/api'
import type {
  WorkflowDTO,
  WorkflowSaveRequestDTO,
  WorkflowSummaryDTO,
} from '../types/workflow'

/** 僅更新 meta 的請求（id 必填，其餘選填） */
export interface WorkflowUpdateRequestDTO {
  id: string
  name?: string
  description?: string
  canvasMeta?: Record<string, unknown> | null
}

/** 統一解析 ApiResponse，回傳 data 欄位 */
function unwrap<T>(res: { data: ApiResponse<T> }): T {
  return res.data.data as T
}

/** 建立空白 workflow（狀態 DRAFT、版本 1） */
export async function create(name: string): Promise<WorkflowDTO> {
  const res = await http.post<ApiResponse<WorkflowDTO>>('/llm/workflow/create', { name })
  return unwrap(res)
}

/** 新增或整張覆寫 workflow（含 nodes/edges） */
export async function save(req: WorkflowSaveRequestDTO): Promise<WorkflowDTO> {
  const res = await http.post<ApiResponse<WorkflowDTO>>('/llm/workflow/save', req)
  return unwrap(res)
}

/** 取得單一 workflow 完整定義 */
export async function get(id: string): Promise<WorkflowDTO> {
  const res = await http.post<ApiResponse<WorkflowDTO>>('/llm/workflow/get', { id })
  return unwrap(res)
}

/** 列出當前使用者擁有的 workflow 摘要清單 */
export async function list(): Promise<WorkflowSummaryDTO[]> {
  const res = await http.get<ApiResponse<WorkflowSummaryDTO[]>>('/llm/workflow/list')
  return unwrap(res)
}

/** 僅更新 meta（name/description/canvasMeta） */
export async function update(req: WorkflowUpdateRequestDTO): Promise<WorkflowDTO> {
  const res = await http.post<ApiResponse<WorkflowDTO>>('/llm/workflow/update', req)
  return unwrap(res)
}

/** 刪除 workflow（連鎖刪 node/edge） */
export async function remove(id: string): Promise<void> {
  await http.post<ApiResponse<null>>('/llm/workflow/delete', { id })
}

/** 啟用/停用 workflow */
export async function switchStatus(id: string, active: boolean): Promise<void> {
  await http.post<ApiResponse<null>>('/llm/workflow/switchStatus', { id, active })
}
