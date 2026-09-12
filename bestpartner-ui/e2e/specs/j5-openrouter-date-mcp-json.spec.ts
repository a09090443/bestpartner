import { test, expect, request, type Page } from '@playwright/test'
import { readFileSync, mkdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { fetchLatestExecution } from '../fixtures/db'
import { dragNodeToCanvas, connect, selectNode } from '../fixtures/canvas'

const __dirname = dirname(fileURLToPath(import.meta.url))

/**
 * J5 第二切片（MCP 變體，2026-07-16 E2E 週期 202607162121 以 webwright 驗證後固化）：
 *   TRIGGER → LLM_ASSISTANT(OpenRouter, responseFormat=JSON) ←in:tool← MCP_SERVER(date) → OUTPUT(JSON)
 *
 * 與 TOOL 切片（j5-openrouter-date-json.spec.ts）的差異：
 * - 能力節點為 MCP_SERVER（STDIO `java -jar D:/MCP/date.jar`），mcpId 由 global-setup 依 name 解析。
 * - `toolName` 必填：execute 前的 validateForExecution 逐節點驗必填欄位（缺它回 400
 *   `missing required config fields: toolName`），即使 in:tool 純能力掛載實際只聚合 mcpId、
 *   不使用 toolName。date.jar 有 6 個 tools，預設填 getTodayDate（E2E_MCP_TOOL_NAME 可覆寫）。
 *
 * 斷言邊界（真實 LLM）：只驗 plumbing / SSE 最終 SUCCESS / OUTPUT 為 JSON / DB 落庫；
 * 不比對 LLM 輸出文字，也不斷言 LLM 實際呼叫了 MCP 工具。
 * （202607162121 週期觀察：LLM 回覆可感知 date MCP 工具集，MCP 路徑掛載有效。）
 */

interface ResolvedSeed {
  llmId: string | null
  dateToolId: string | null
  dateMcpId: string | null
}
const seed: ResolvedSeed = JSON.parse(
  readFileSync(resolve(__dirname, '../.artifacts/seed.json'), 'utf-8'),
)

const API = process.env.E2E_API_URL || 'http://localhost:80'
const ADMIN = {
  email: process.env.E2E_ADMIN_USER || 'admin@bestpartner.com.tw',
  password: process.env.E2E_ADMIN_PASS || 'admin',
}
const MCP_TOOL_NAME = process.env.E2E_MCP_TOOL_NAME || 'getTodayDate'
const RUN_TAG = `run${Date.now()}`
const WF_NAME = `e2e-j5-openrouter-mcp-${RUN_TAG}`

/** 報告逐步證據截圖：E2E 測試週期以 E2E_SHOTS_DIR 指向 docs/test-confirmations/e2e-shots/<時間戳>/ */
const SHOTS_DIR = process.env.E2E_SHOTS_DIR || resolve(__dirname, `../.artifacts/shots/${RUN_TAG}`)
let shotSeq = 0
async function shot(page: Page, caseId: string, label: string) {
  shotSeq += 1
  const n = String(shotSeq).padStart(2, '0')
  mkdirSync(SHOTS_DIR, { recursive: true })
  await page.screenshot({ path: resolve(SHOTS_DIR, `${caseId}-${n}-${label}.png`), fullPage: true })
}

/** 收尾：以 API 依名稱刪除本測試建立的 workflow（連鎖刪 node/edge/execution） */
test.afterEach(async () => {
  const ctx = await request.newContext({ baseURL: API })
  try {
    const jwt = (await (await ctx.post('/login/', { data: ADMIN })).json())?.data as string | undefined
    if (!jwt) return
    const auth = { Authorization: `Bearer ${jwt}` }
    const list = ((await (await ctx.get('/llm/workflow/list', { headers: auth })).json())?.data ?? []) as Array<{
      id?: string
      name?: string
    }>
    for (const wf of list) {
      if (wf.id && wf.name === WF_NAME) {
        await ctx.post('/llm/workflow/delete', { data: { id: wf.id }, headers: auth })
      }
    }
  } finally {
    await ctx.dispose()
  }
})

test('J5（MCP 變體）：建構並執行 OpenRouter + date MCP + JSON 輸出 workflow', async ({ page }) => {
  test.skip(!seed.llmId, '缺 OpenRouter CHAT llmId（seed 解析不到且未給 E2E_LLM_ID），跳過真實執行')
  test.skip(!seed.dateMcpId, '缺 date MCP server（/llm/mcpServer/list 依 name 解析不到），跳過 MCP 切片')
  test.setTimeout(180_000)

  // --- J1 登入 ---
  await page.goto('/')
  await expect(page).toHaveURL(/\/login/)
  await shot(page, 'J1-02', 'login-page')
  await page.locator('input[type="email"]').fill(ADMIN.email)
  await page.locator('input[type="password"]').fill(ADMIN.password)
  await shot(page, 'J1-02', 'filled')
  await page.getByRole('button', { name: '登入' }).click()
  await expect(page.getByTestId('create-button')).toBeVisible()
  await shot(page, 'J1-02', 'redirected-list')

  // --- J2 建立空白 workflow → 編輯器 ---
  await page.getByTestId('create-button').click()
  await expect(page).toHaveURL(/\/editor/)
  await page.getByTestId('toolbar-name-input').fill(WF_NAME)
  await shot(page, 'J2-02', 'created-editor')

  // --- J3 拖 4 節點（座標須落在可視畫布內，勿壓到右側 Overview 面板下，否則 handle 被攔截）---
  const nodes = page.locator('.vue-flow__node')
  await dragNodeToCanvas(page, 'TRIGGER', 480, 300)
  await expect(nodes).toHaveCount(1)
  await dragNodeToCanvas(page, 'LLM_ASSISTANT', 820, 300)
  await expect(nodes).toHaveCount(2)
  await dragNodeToCanvas(page, 'MCP_SERVER', 820, 540)
  await expect(nodes).toHaveCount(3)
  await dragNodeToCanvas(page, 'OUTPUT', 1160, 300)
  await expect(nodes).toHaveCount(4)
  await shot(page, 'J3-01', 'four-nodes-dropped')

  const llmKey = await page
    .locator('[data-node-type="LLM_ASSISTANT"]')
    .first()
    .evaluate((el) => el.closest('.vue-flow__node')?.getAttribute('data-id') || '')
  expect(llmKey).not.toEqual('')

  // --- J3 連線（逐條斷言 edge 數，失敗可即刻定位是哪一條）---
  const edges = page.locator('.vue-flow__edge')
  await connect(page, 'TRIGGER', 'out:main', 'LLM_ASSISTANT', 'in:main')
  await expect(edges).toHaveCount(1)
  await connect(page, 'MCP_SERVER', 'out:main', 'LLM_ASSISTANT', 'in:tool')
  await expect(edges).toHaveCount(2)
  await connect(page, 'LLM_ASSISTANT', 'out:main', 'OUTPUT', 'in:main')
  await expect(edges).toHaveCount(3)
  await shot(page, 'J3-02', 'edges-connected')

  // --- TRIGGER：JsonConfigEditor raw 模式填 triggerType ---
  await selectNode(page, 'TRIGGER')
  await page.getByTestId('mode-toggle').click()
  await page.getByTestId('raw-input').fill('{"triggerType":"MANUAL"}')
  await shot(page, 'J3-03', 'trigger-config')

  // --- LLM_ASSISTANT：OpenRouter + JSON ---
  await selectNode(page, 'LLM_ASSISTANT')
  await page.getByTestId('llm-select').selectOption(seed.llmId!)
  await page.getByTestId('user-prompt').fill('請查詢現在時間，並以 JSON 回覆一個包含 now 欄位的物件')
  await page.getByTestId('response-format').selectOption('JSON')
  await page.getByTestId('output-schema').fill('{"type":"object","properties":{"now":{"type":"string"}}}')
  await page.getByTestId('output-key').fill('reply')
  await shot(page, 'J3-03', 'llm-assistant-config')

  // --- MCP_SERVER(date)：mcpId + toolName（見檔頭說明）---
  await selectNode(page, 'MCP_SERVER')
  await page.getByTestId('mcp-select').selectOption(seed.dateMcpId!)
  await page.getByTestId('tool-name').fill(MCP_TOOL_NAME)
  await shot(page, 'J3-03', 'mcp-config')

  // --- OUTPUT：mappings 產 JSON，引用 LLM 輸出 ---
  await selectNode(page, 'OUTPUT')
  await page.getByTestId('output-mappings').fill(`now={{${llmKey}.reply}}`)
  await shot(page, 'J3-03', 'output-config')

  // --- 存檔 ---
  await page.getByTestId('save-button').click()
  await expect(page.getByTestId('saved-badge')).toBeVisible()
  await shot(page, 'J3-04', 'saved')

  // --- 執行（真實 OpenRouter + STDIO date MCP，SSE）---
  await page.getByTestId('run-button').click()
  const drawer = page.getByTestId('result-drawer')
  await expect(drawer).toBeVisible({ timeout: 150_000 })
  await shot(page, 'J5-01', 'drawer-visible')
  await expect(drawer.locator('.status')).toHaveText('SUCCESS', { timeout: 150_000 })
  await shot(page, 'J5-01', 'status-success')

  // OUTPUT 為 JSON 物件（不比對內容）
  const outputText = await drawer.locator('.output-json').textContent()
  expect(() => JSON.parse(outputText ?? '')).not.toThrow()

  // --- DB 落庫斷言（Q2=B：直連 Postgres）---
  const rec = await fetchLatestExecution(WF_NAME)
  expect(rec, 'DB 無執行紀錄').not.toBeNull()
  expect(rec!.execution.status).toBe('SUCCESS')
  expect(rec!.execution.trigger_type).toBe('MANUAL')
  // Agent 模式：MCP_SERVER 掛 in:tool 為能力掛載，不獨立執行、不落 node_execution
  const nodeTypes = rec!.nodes.map((n) => n.node_type)
  expect(nodeTypes).not.toContain('MCP_SERVER')
  expect(nodeTypes).toContain('TRIGGER')
  expect(nodeTypes).toContain('LLM_ASSISTANT')
  expect(nodeTypes).toContain('OUTPUT')
  expect(rec!.nodes.every((n) => n.status === 'SUCCESS')).toBeTruthy()
})
