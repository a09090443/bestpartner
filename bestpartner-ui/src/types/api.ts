/**
 * 後端統一回應格式
 * 對應後端 ApiResponse<T>
 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T | null
}
