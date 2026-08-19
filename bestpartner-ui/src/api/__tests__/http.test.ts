import { describe, it, expect, beforeEach, vi } from 'vitest'

// 先 import 再測試；http.ts 尚未存在時測試會直接失敗（紅）
import {
  attachAuthHeader,
  setRedirectHandler,
  handleUnauthorizedResponse,
  extractApiMessage,
  isLoginRequest,
} from '../http'

describe('attachAuthHeader', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('localStorage 有 token 時，應設定 Authorization: Bearer <token>', () => {
    localStorage.setItem('token', 'test-token-abc123')
    const config = { headers: {} as Record<string, string> }
    const result = attachAuthHeader(config as Parameters<typeof attachAuthHeader>[0])
    expect(result.headers!['Authorization']).toBe('Bearer test-token-abc123')
  })

  it('localStorage 無 token 時，不應加入 Authorization header', () => {
    const config = { headers: {} as Record<string, string> }
    const result = attachAuthHeader(config as Parameters<typeof attachAuthHeader>[0])
    expect((result.headers as Record<string, string> | undefined)?.['Authorization']).toBeUndefined()
  })

  it('config 未帶 headers 時，有 token 應自動初始化 headers 並設定 Authorization', () => {
    localStorage.setItem('token', 'init-header-token')
    const config = {} as Parameters<typeof attachAuthHeader>[0]
    const result = attachAuthHeader(config)
    expect(result.headers!['Authorization']).toBe('Bearer init-header-token')
  })
})

describe('extractApiMessage', () => {
  it('應優先取 axios error 的 response.data.message（後端業務訊息）', () => {
    const err = {
      message: 'Request failed with status code 400',
      response: { data: { message: 'An active workflow must contain a Trigger node' } },
    }
    expect(extractApiMessage(err)).toBe('An active workflow must contain a Trigger node')
  })

  it('無 response.data.message 時退回 error 本身的 message', () => {
    const err = { message: 'Network Error' }
    expect(extractApiMessage(err)).toBe('Network Error')
  })

  it('兩者皆無時回傳 undefined', () => {
    expect(extractApiMessage({})).toBeUndefined()
    expect(extractApiMessage(null)).toBeUndefined()
  })
})

describe('handleUnauthorizedResponse', () => {
  beforeEach(() => {
    localStorage.clear()
    // 重設回預設 redirect handler（避免測試間污染）
    setRedirectHandler(() => {})
  })

  it('應清除 localStorage 中的 token', () => {
    localStorage.setItem('token', 'should-be-removed')
    handleUnauthorizedResponse()
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('應呼叫已注入的 redirect handler 並傳入 /login', () => {
    const mockRedirect = vi.fn()
    setRedirectHandler(mockRedirect)
    handleUnauthorizedResponse()
    expect(mockRedirect).toHaveBeenCalledOnce()
    expect(mockRedirect).toHaveBeenCalledWith('/login')
  })

  it('注入新 redirect handler 後舊 handler 不應被呼叫', () => {
    const oldHandler = vi.fn()
    const newHandler = vi.fn()
    setRedirectHandler(oldHandler)
    setRedirectHandler(newHandler)
    handleUnauthorizedResponse()
    expect(oldHandler).not.toHaveBeenCalled()
    expect(newHandler).toHaveBeenCalledOnce()
  })
})

describe('isLoginRequest（登入端點自身的 401 不得走全域登出重導）', () => {
  // 背景：E2E 週期 202608192201 的 J1-04——帳密錯誤時後端回 HTTP 401，
  // 攔截器若一律呼叫 handleUnauthorizedResponse()，預設 redirect handler 會做
  // window.location.href 整頁導航，把 LoginView 剛拋出的 ElMessage 一起沖掉，
  // 使用者完全看不到「密碼錯誤」。
  it.each(['/login/', '/login', 'http://localhost:80/login/', '/api/login/', '/login/?x=1'])(
    '應把 %s 視為登入請求',
    (url) => {
      expect(isLoginRequest(url)).toBe(true)
    },
  )

  it.each(['/llm/workflow/list', '/login/check', '/llm/setting/get', '/relogin-history'])(
    '不應把 %s 視為登入請求',
    (url) => {
      expect(isLoginRequest(url)).toBe(false)
    },
  )

  it('url 為 undefined 時應回 false（不影響既有行為）', () => {
    expect(isLoginRequest(undefined)).toBe(false)
  })
})
