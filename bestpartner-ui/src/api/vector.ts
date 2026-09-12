import http from './http'
import type { ApiResponse } from '../types/api'
import type { Option } from '../types/options'

/** 後端知識庫文件 DTO（僅取下拉需要的欄位） */
interface LLMDocDTO {
  knowledgeId?: string
  knowledgeName?: string
}

/**
 * 查詢當前用戶的知識庫清單，轉為下拉 Option[]。
 * 空 body 取全部；後端回傳為文件（doc）層級，一個知識庫對應多筆文件，須依 knowledgeId 去重。
 * value = knowledgeId，label = knowledgeName。
 */
export async function listKnowledgeStores(): Promise<Option[]> {
  const res = await http.post<ApiResponse<LLMDocDTO[]>>('/llm/vector/getKnowledgeStore', {})
  const list = res.data.data ?? []
  const seen = new Map<string, Option>()
  for (const doc of list) {
    if (doc.knowledgeId && !seen.has(doc.knowledgeId)) {
      seen.set(doc.knowledgeId, {
        value: doc.knowledgeId,
        label: doc.knowledgeName || doc.knowledgeId,
      })
    }
  }
  return [...seen.values()]
}
