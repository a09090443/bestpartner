import { describe, it, expect, beforeEach } from 'vitest'
import { effectScope, nextTick } from 'vue'
import {
  EDITOR_THEME_KEY,
  useEditorTheme,
  __resetEditorThemeForTest,
} from '../useEditorTheme'

/** composable 內含 onScopeDispose，須在 effect scope 內呼叫；回傳 scope 供測試主動銷毀 */
function mountTheme() {
  const scope = effectScope()
  const api = scope.run(() => useEditorTheme())!
  return { scope, ...api }
}

describe('useEditorTheme', () => {
  beforeEach(() => {
    localStorage.clear()
    __resetEditorThemeForTest()
  })

  it('未設定偏好時預設為淺色', () => {
    const { scope, theme, isDark } = mountTheme()
    expect(theme.value).toBe('light')
    expect(isDark.value).toBe(false)
    scope.stop()
  })

  it('還原 localStorage 中的既有偏好', () => {
    localStorage.setItem(EDITOR_THEME_KEY, 'dark')
    __resetEditorThemeForTest()

    const { scope, theme, isDark } = mountTheme()
    expect(theme.value).toBe('dark')
    expect(isDark.value).toBe(true)
    scope.stop()
  })

  it('localStorage 為非法值時回退淺色', () => {
    localStorage.setItem(EDITOR_THEME_KEY, 'solarized')
    __resetEditorThemeForTest()

    const { scope, theme } = mountTheme()
    expect(theme.value).toBe('light')
    scope.stop()
  })

  it('setTheme 更新狀態並寫回 localStorage', () => {
    const { scope, theme, setTheme } = mountTheme()

    setTheme('dark')
    expect(theme.value).toBe('dark')
    expect(localStorage.getItem(EDITOR_THEME_KEY)).toBe('dark')

    setTheme('light')
    expect(theme.value).toBe('light')
    expect(localStorage.getItem(EDITOR_THEME_KEY)).toBe('light')
    scope.stop()
  })

  it('toggleTheme 在深淺之間來回切換', () => {
    const { scope, theme, toggleTheme } = mountTheme()

    toggleTheme()
    expect(theme.value).toBe('dark')
    toggleTheme()
    expect(theme.value).toBe('light')
    scope.stop()
  })

  it('切換為深色時於 <html> 掛上 dark class，切回淺色時移除', async () => {
    const { scope, setTheme } = mountTheme()
    expect(document.documentElement.classList.contains('dark')).toBe(false)

    setTheme('dark')
    await nextTick()
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    setTheme('light')
    await nextTick()
    expect(document.documentElement.classList.contains('dark')).toBe(false)
    scope.stop()
  })

  it('scope 銷毀時移除 <html> 的 dark class（離開編輯器後其他頁面須維持淺色）', async () => {
    const { scope, setTheme } = mountTheme()
    setTheme('dark')
    await nextTick()
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    scope.stop()
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })
})
