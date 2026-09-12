import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    // 單元測試一律位於 src/**/__tests__/；e2e/ 下的 Playwright spec 不由 vitest 執行
    // （見 frontend-conventions.md：e2e/ harness 非正式執行入口）。
    include: ['src/**/*.{test,spec}.ts'],
    exclude: ['**/node_modules/**', '**/dist/**', 'e2e/**'],
  },
})
