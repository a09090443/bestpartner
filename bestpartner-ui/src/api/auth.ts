import http from './http'
import type { ApiResponse } from '../types/api'

/**
 * 呼叫後端 POST /login/，取得 JWT token。
 * 回傳 ApiResponse.data（token 字串）。
 * 若後端回傳 code !== 200，拋出帶 message 的 Error。
 */
export async function login(email: string, password: string): Promise<string> {
  const res = await http.post<ApiResponse<string>>('/login/', { email, password })
  const body = res.data

  if (body.code !== 200) {
    throw new Error(body.message ?? '登入失敗')
  }

  if (!body.data) {
    throw new Error('登入回應不含 token')
  }

  return body.data
}
