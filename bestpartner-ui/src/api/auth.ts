import http, { extractApiMessage } from './http'
import type { ApiResponse } from '../types/api'

/**
 * 呼叫後端 POST /login/，取得 JWT token。
 * 回傳 ApiResponse.data（token 字串）。
 * 若後端回傳 code !== 200，拋出帶 message 的 Error。
 *
 * ⚠️ 帳密錯誤時後端回的是 **HTTP 401**（body `{"code":401,"message":"密碼錯誤"}`），
 * axios 會直接 reject，走不到下面的 `body.code` 判斷。若原樣往上拋，
 * LoginView 顯示的會是 axios 通用字串「Request failed with status code 401」，
 * 而不是後端真正的原因。故在此攔下並改拋帶後端訊息的 Error。
 */
export async function login(email: string, password: string): Promise<string> {
  let res
  try {
    res = await http.post<ApiResponse<string>>('/login/', { email, password })
  } catch (err) {
    throw new Error(extractApiMessage(err) ?? '登入失敗')
  }
  const body = res.data

  if (body.code !== 200) {
    throw new Error(body.message ?? '登入失敗')
  }

  if (!body.data) {
    throw new Error('登入回應不含 token')
  }

  return body.data
}
