import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock api/auth 模組，避免真實 HTTP 請求
vi.mock('../../api/auth', () => ({
  login: vi.fn(),
}))

// Mock per-user 快取清除入口，驗證 logout 有觸發
const invalidateSettingsCache = vi.fn()
const clearNodeOptionsCache = vi.fn()
vi.mock('../../api/llmSetting', () => ({
  invalidateSettingsCache: () => invalidateSettingsCache(),
}))
vi.mock('../../composables/useNodeOptions', () => ({
  clearNodeOptionsCache: () => clearNodeOptionsCache(),
}))

import * as authApi from '../../api/auth'
import { useAuthStore } from '../auth'

describe('authStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('login()', () => {
    it('登入成功後 token 應被設定，isAuthenticated 為 true，localStorage 有寫入', async () => {
      const mockToken = 'eyJhbGciOiJSUzI1NiJ9.test-token'
      vi.mocked(authApi.login).mockResolvedValueOnce(mockToken)

      const store = useAuthStore()
      await store.login('user@example.com', 'password123')

      expect(store.token).toBe(mockToken)
      expect(store.isAuthenticated).toBe(true)
      expect(localStorage.getItem('token')).toBe(mockToken)
    })

    it('login() 應以正確的帳密呼叫 api/auth.login', async () => {
      const mockToken = 'test-token'
      vi.mocked(authApi.login).mockResolvedValueOnce(mockToken)

      const store = useAuthStore()
      await store.login('admin@example.com', 'secret')

      expect(authApi.login).toHaveBeenCalledWith('admin@example.com', 'secret')
    })

    it('login() 失敗時應拋出錯誤，token 保持空', async () => {
      vi.mocked(authApi.login).mockRejectedValueOnce(new Error('帳密錯誤'))

      const store = useAuthStore()
      await expect(store.login('bad@example.com', 'wrong')).rejects.toThrow('帳密錯誤')

      expect(store.token).toBe('')
      expect(store.isAuthenticated).toBe(false)
    })
  })

  describe('logout()', () => {
    it('logout() 後 token 清空、isAuthenticated 為 false、localStorage 清除', async () => {
      const mockToken = 'existing-token'
      vi.mocked(authApi.login).mockResolvedValueOnce(mockToken)

      const store = useAuthStore()
      await store.login('user@example.com', 'pass')
      expect(store.isAuthenticated).toBe(true)

      store.logout()

      expect(store.token).toBe('')
      expect(store.isAuthenticated).toBe(false)
      expect(localStorage.getItem('token')).toBeNull()
    })

    it('logout() 清除 per-user 模組級快取（設定清單與節點選項）', () => {
      const store = useAuthStore()
      store.logout()

      expect(invalidateSettingsCache).toHaveBeenCalledTimes(1)
      expect(clearNodeOptionsCache).toHaveBeenCalledTimes(1)
    })
  })

  describe('loadFromStorage()', () => {
    it('能從 localStorage 還原 token，isAuthenticated 變為 true', () => {
      const storedToken = 'stored-jwt-token'
      localStorage.setItem('token', storedToken)

      const store = useAuthStore()
      store.loadFromStorage()

      expect(store.token).toBe(storedToken)
      expect(store.isAuthenticated).toBe(true)
    })

    it('localStorage 無 token 時，loadFromStorage() 不改變 token（保持空）', () => {
      const store = useAuthStore()
      store.loadFromStorage()

      expect(store.token).toBe('')
      expect(store.isAuthenticated).toBe(false)
    })
  })
})
