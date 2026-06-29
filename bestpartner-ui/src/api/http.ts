import axios, { AxiosHeaders } from 'axios'
import type { InternalAxiosRequestConfig, AxiosResponse } from 'axios'

/**
 * 可注入的 redirect handler。
 * 預設行為：導向 /login（透過 window.location.href）。
 * 測試時可透過 setRedirectHandler 覆寫，避免真正跳轉。
 */
let _redirectHandler: (path: string) => void = (path: string) => {
  window.location.href = path
}

/**
 * 注入自訂 redirect handler（主要供測試使用）。
 */
export function setRedirectHandler(handler: (path: string) => void): void {
  _redirectHandler = handler
}

/**
 * 處理未授權回應：清除 localStorage token 並呼叫 redirect handler。
 * 供 response interceptor 與單元測試共用。
 */
export function handleUnauthorizedResponse(): void {
  localStorage.removeItem('token')
  _redirectHandler('/login')
}

/**
 * 為 axios request config 附加 Authorization header。
 * 從 localStorage 取得 token，有則設定 `Bearer <token>`，無則不修改。
 * 此函式為純函式（無副作用），可單獨進行單元測試。
 */
export function attachAuthHeader(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  const token = localStorage.getItem('token')
  if (token) {
    // InternalAxiosRequestConfig.headers 為 AxiosRequestHeaders（class 實例）。
    // 若測試情境傳入不帶 headers 的裸物件，需先初始化，避免對 undefined 賦值。
    if (!config.headers) {
      config.headers = new AxiosHeaders()
    }
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
}

/**
 * 全域 axios 實例。
 * baseURL 由環境變數 VITE_API_BASE 提供；
 * 若未設定則為 undefined，axios 會使用相對路徑（適合 vite proxy 開發情境）。
 */
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
})

// Request interceptor：自動帶入 JWT token
http.interceptors.request.use(
  (config) => attachAuthHeader(config),
  (error) => Promise.reject(error),
)

// Response interceptor：偵測 401/403 HTTP 狀態或後端 code 401 → 登出並重導
http.interceptors.response.use(
  (response: AxiosResponse) => {
    // 後端 ApiResponse code 401（業務層 token 失效）
    if (response.data?.code === 401) {
      handleUnauthorizedResponse()
    }
    return response
  },
  (error: unknown) => {
    // HTTP 層 401 / 403
    if (
      axios.isAxiosError(error) &&
      (error.response?.status === 401 || error.response?.status === 403)
    ) {
      handleUnauthorizedResponse()
    }
    return Promise.reject(error)
  },
)

export default http
