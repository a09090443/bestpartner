import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin } from '../api/auth'
import { invalidateSettingsCache } from '../api/llmSetting'
import { clearNodeOptionsCache } from '../composables/useNodeOptions'

const TOKEN_KEY = 'token'

export const useAuthStore = defineStore('auth', () => {
  // 狀態
  const token = ref<string>('')
  const username = ref<string>('')

  // Getter：是否已認證
  const isAuthenticated = computed(() => token.value !== '')

  /**
   * 登入：呼叫後端 API 取得 token，儲存至 state 與 localStorage。
   * 失敗時拋出錯誤，state 保持不變。
   */
  async function login(email: string, password: string): Promise<void> {
    const receivedToken = await apiLogin(email, password)
    token.value = receivedToken
    localStorage.setItem(TOKEN_KEY, receivedToken)
  }

  /**
   * 登出：清除 state 與 localStorage 的 token，並清除 per-user 的模組級快取。
   * 登出走 client-side 導航不 reload 頁面，module-scope 快取會存活，
   * 若不清除，同分頁換使用者登入將看到上一位的設定 / 節點選項清單。
   */
  function logout(): void {
    token.value = ''
    username.value = ''
    localStorage.removeItem(TOKEN_KEY)
    invalidateSettingsCache()
    clearNodeOptionsCache()
  }

  /**
   * 從 localStorage 還原 token（頁面重整後呼叫）。
   */
  function loadFromStorage(): void {
    const stored = localStorage.getItem(TOKEN_KEY)
    if (stored) {
      token.value = stored
    }
  }

  return {
    token,
    username,
    isAuthenticated,
    login,
    logout,
    loadFromStorage,
  }
})
