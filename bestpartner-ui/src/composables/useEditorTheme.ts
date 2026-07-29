import { computed, onScopeDispose, ref, watch } from 'vue'
import type { ComputedRef, Ref } from 'vue'

export type EditorTheme = 'light' | 'dark'

/** localStorage 鍵名。沿用既有扁平命名慣例（如 auth 的 'token'），不加命名空間前綴。 */
export const EDITOR_THEME_KEY = 'wf-theme'

/** 淺色為預設：設計稿的預設面貌，且未設定時落在與其他頁面（EP 淺色）一致的一側。 */
const DEFAULT_THEME: EditorTheme = 'light'

/**
 * 掛在 <html> 上的 class。Element Plus 的深色變數表（theme-chalk/dark/css-vars.css）
 * 以 html.dark 為選擇器，而 ElMessage / ElMessageBox 會 teleport 到 document.body、
 * 落在 .wf-editor 作用域之外，故必須有這個 document 層級的標記。
 */
const HTML_DARK_CLASS = 'dark'

function readStoredTheme(): EditorTheme {
  try {
    const stored = localStorage.getItem(EDITOR_THEME_KEY)
    return stored === 'light' || stored === 'dark' ? stored : DEFAULT_THEME
  } catch {
    // localStorage 不可用（隱私模式／SSR）時退回預設，不讓主題讀取拖垮編輯器
    return DEFAULT_THEME
  }
}

// 模組層級 singleton：工具列切換鈕與 view 的 data-wf-theme 需共享同一份狀態
const theme = ref<EditorTheme>(readStoredTheme())

function applyHtmlClass(value: EditorTheme): void {
  document.documentElement.classList.toggle(HTML_DARK_CLASS, value === 'dark')
}

/**
 * Workflow 編輯器主題狀態。
 *
 * 回傳的 theme 供 .wf-editor 的 data-wf-theme 屬性繫結（token 切換靠它），
 * 同時副作用地維護 <html class="dark"> 讓 Element Plus 的 teleport 元件跟著切換。
 *
 * ⚠️ 呼叫端的 effect scope 銷毀時會移除 <html class="dark">——離開編輯器頁面後，
 * Login / WorkflowList 必須回到 Element Plus 的淺色預設。
 */
export function useEditorTheme(): {
  theme: Ref<EditorTheme>
  isDark: ComputedRef<boolean>
  setTheme: (value: EditorTheme) => void
  toggleTheme: () => void
} {
  const isDark = computed(() => theme.value === 'dark')

  watch(theme, applyHtmlClass, { immediate: true })

  onScopeDispose(() => {
    document.documentElement.classList.remove(HTML_DARK_CLASS)
  })

  function setTheme(value: EditorTheme): void {
    theme.value = value
    try {
      localStorage.setItem(EDITOR_THEME_KEY, value)
    } catch {
      // 寫入失敗（配額／隱私模式）只影響「下次進來記不記得」，不影響本次切換
    }
  }

  function toggleTheme(): void {
    setTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  return { theme, isDark, setTheme, toggleTheme }
}

/** 測試用：重置模組層級 singleton 與 <html> class，避免測試間互相污染 */
export function __resetEditorThemeForTest(): void {
  theme.value = readStoredTheme()
  document.documentElement.classList.remove(HTML_DARK_CLASS)
}
