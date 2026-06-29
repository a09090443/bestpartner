import { describe, it, expect, beforeEach, vi } from 'vitest'

// 先 import 再測試；http.ts 尚未存在時測試會直接失敗（紅）
import { attachAuthHeader, setRedirectHandler, handleUnauthorizedResponse } from '../http'

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
