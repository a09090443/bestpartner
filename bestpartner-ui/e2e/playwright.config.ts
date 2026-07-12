import { defineConfig } from '@playwright/test'

/**
 * BestPartner E2E（Playwright）設定 — 首條垂直切片驗證用。
 *
 * 對 port 80 真實後端 + PostgreSQL + 真實 OpenRouter 全貫穿；
 * baseURL 指向 vite dev server（預設 5173）。
 */
export default defineConfig({
  testDir: './specs',
  globalSetup: './global-setup.ts',
  globalTeardown: './global-teardown.ts',
  timeout: 180_000,
  expect: { timeout: 20_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { outputFolder: '../playwright-report', open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    // 專案慣例用 data-test（非 Playwright 預設的 data-testid）
    testIdAttribute: 'data-test',
    trace: 'on',
    screenshot: 'on',
    video: 'retain-on-failure',
    viewport: { width: 1680, height: 950 },
    actionTimeout: 20_000,
  },
})
