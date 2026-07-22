import { test, expect, request, type Page } from '@playwright/test'
import { readFileSync, mkdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { fetchLatestExecution } from '../fixtures/db'
import { dragNodeToCanvas, connect, selectNode } from '../fixtures/canvas'

const __dirname = dirname(fileURLToPath(import.meta.url))

/**
 * J5 第三切片（google_map MCP 變體，2026-07-22 E2E 週期 202607222159 以 webwright 驗證後固化）：
 *   TRIGGER → LLM_ASSISTANT(OpenRouter, responseFormat=JSON) ←in:tool← MCP_SERVER(google_map) → OUTPUT(JSON)
 *
 * 與 date MCP 切片（j5-openrouter-date-mcp-json.spec.ts）的關鍵差異：
 * - google_map MCP 需 env `GOOGLE_MAPS_API_KEY`，故 MCP_SERVER 節點**必須帶 userSettingId**
 *   （data-test="mcp-setting-id"）——引擎據此走 buildUserSpecificMcpClient 注入使用者 env；
 *   僅填 mcpId 會走 buildDefaultMcpClient、env 未替換而工具不可用。
 * - 因 LLM/MCP 設定皆以「登入者 userId」為範圍，執行身分必須是**擁有該 google_map 設定的使用者**。
 *   本 spec 以 env 參數化登入身分（E2E_GM_USER_EMAIL/PASS，預設 admin），並要求：
 *     · seed.googleMapMcpId（global-setup 依 name `google_map` 解析，缺則 skip）
 *     · E2E_GM_SETTING_ID（該使用者的 google_map userSettingId，缺則 skip；後端無列舉端點故走 env）
 *     · 該使用者具 OpenRouter CHAT llmId（E2E_GM_LLM_ID 可覆寫，缺則 skip）
 *   任一資源缺席即 test.skip（CI 無 Maps 金鑰時本切片自然略過，與 date 切片一致）。
 *
 * 斷言邊界（真實 LLM，CI 用）：只驗 plumbing / SSE 最終 SUCCESS / OUTPUT 為合法 JSON / DB 落庫
 * （node_execution 不含 MCP_SERVER）。**不逐字比對輸出、不在 CI 斷言「回傳真實地點」**——
 * 深度驗證（LLM 實際呼叫 google_map 並回傳金山實際咖啡廳）已於週期 202607222159 以 webwright 實測並記錄於
 * docs/test-confirmations/e2e-test-confirmation-202607222159.md（含 5 間金山咖啡廳店名/評分/地址），
 * 因其依賴有效 Maps 金鑰而不納入 CI 斷言。
 */

interface ResolvedSeed {
  llmId: string | null
  googleMapMcpId: string | null
}
const seed: ResolvedSeed = JSON.parse(
  readFileSync(resolve(__dirname, '../.artifacts/seed.json'), 'utf-8'),
)

const API = process.env.E2E_API_URL || 'http://localhost:80'
const GM_USER = {
  email: process.env.E2E_GM_USER_EMAIL || 'admin@bestpartner.com.tw',
  password: process.env.E2E_GM_USER_PASS || 'admin',
}
const GM_SETTING_ID = process.env.E2E_GM_SETTING_ID || ''
const GM_TOOL_NAME = process.env.E2E_GM_TOOL_NAME || 'search_places'
const RUN_TAG = `run${Date.now()}`
const WF_NAME = `e2e-j5-googlemap-${RUN_TAG}`

/** 報告逐步證據截圖：E2E 測試週期以 E2E_SHOTS_DIR 指向 docs/test-confirmations/e2e-shots/<時間戳>/ */
const SHOTS_DIR = process.env.E2E_SHOTS_DIR || resolve(__dirname, `../.artifacts/shots/${RUN_TAG}`)
let shotSeq = 0
async function shot(page: Page, caseId: string, label: string) {
  shotSeq += 1
  const n = String(shotSeq).padStart(2, '0')
  mkdirSync(SHOTS_DIR, { recursive: true })
  await page.screenshot({ path: resolve(SHOTS_DIR, `${caseId}-${n}-${label}.png`) })
}

/** 以 GM 使用者身分登入 API 並解析其 OpenRouter CHAT llmId（E2E_GM_LLM_ID 可覆寫） */
async function resolveGmLlmId(): Promise<string | null> {
  if (process.env.E2E_GM_LLM_ID) return process.env.E2E_GM_LLM_ID
  const ctx = await request.newContext({ baseURL: API })
  try {
    const jwt = (await (await ctx.post('/login/', { data: GM_USER })).json())?.data as string | undefined
    if (!jwt) return null
    const auth = { Authorization: `Bearer ${jwt}` }
    const settings = ((await (await ctx.post('/llm/setting/get', { data: {}, headers: auth })).json())?.data ??
      []) as Array<{ id?: string; alias?: string; platform?: string; modelType?: string }>
    return (
      settings.find(
        (s) => s.modelType === 'CHAT' && (s.platform === 'OPENROUTER' || /openrouter/i.test(s.alias ?? '')),
      )?.id ?? null
    )
  } finally {
    await ctx.dispose()
  }
}

/** 收尾：以 API（GM 使用者）依名稱刪除本測試建立的 workflow（連鎖刪 node/edge/execution） */
test.afterEach(async () => {
  const ctx = await request.newContext({ baseURL: API })
  try {
    const jwt = (await (await ctx.post('/login/', { data: GM_USER })).json())?.data as string | undefined
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

test('J5（google_map 變體）：建構並執行 OpenRouter + google_map MCP + JSON 輸出 workflow', async ({ page }) => {
  test.skip(!seed.googleMapMcpId, '缺 google_map MCP server（/llm/mcpServer/list 依 name 解析不到），跳過 google_map 切片')
  test.skip(!GM_SETTING_ID, '缺 E2E_GM_SETTING_ID（google_map userSettingId），跳過——無它則 env 金鑰不會注入')
  const llmId = await resolveGmLlmId()
  test.skip(!llmId, '缺 OpenRouter CHAT llmId（GM 使用者無 CHAT 設定且未給 E2E_GM_LLM_ID），跳過真實執行')
  test.setTimeout(240_000)

  // --- 登入（GM 使用者：google_map 設定擁有者）---
  await page.goto('/')
  await expect(page).toHaveURL(/\/login/)
  await shot(page, 'J5GM-01', 'login-page')
  await page.locator('input[type="email"]').fill(GM_USER.email)
  await page.locator('input[type="password"]').fill(GM_USER.password)
  await page.getByRole('button', { name: '登入' }).click()
  await expect(page.getByTestId('create-button')).toBeVisible()
  await shot(page, 'J5GM-01', 'workflow-list')

  // --- 建立空白 workflow → 編輯器 ---
  await page.getByTestId('create-button').click()
  await expect(page).toHaveURL(/\/editor/)
  await page.getByTestId('toolbar-name-input').fill(WF_NAME)

  // --- 拖 4 節點（座標須落在可視畫布內，勿壓到右側 Overview 面板下）---
  const nodes = page.locator('.vue-flow__node')
  await dragNodeToCanvas(page, 'TRIGGER', 480, 300)
  await expect(nodes).toHaveCount(1)
  await dragNodeToCanvas(page, 'LLM_ASSISTANT', 820, 300)
  await expect(nodes).toHaveCount(2)
  await dragNodeToCanvas(page, 'MCP_SERVER', 820, 540)
  await expect(nodes).toHaveCount(3)
  await dragNodeToCanvas(page, 'OUTPUT', 1160, 300)
  await expect(nodes).toHaveCount(4)
  await shot(page, 'J5GM-01', 'four-nodes')

  const llmKey = await page
    .locator('[data-node-type="LLM_ASSISTANT"]')
    .first()
    .evaluate((el) => el.closest('.vue-flow__node')?.getAttribute('data-id') || '')
  expect(llmKey).not.toEqual('')

  // --- 連線（逐條斷言 edge 數）---
  const edges = page.locator('.vue-flow__edge')
  await connect(page, 'TRIGGER', 'out:main', 'LLM_ASSISTANT', 'in:main')
  await expect(edges).toHaveCount(1)
  await connect(page, 'MCP_SERVER', 'out:main', 'LLM_ASSISTANT', 'in:tool')
  await expect(edges).toHaveCount(2)
  await connect(page, 'LLM_ASSISTANT', 'out:main', 'OUTPUT', 'in:main')
  await expect(edges).toHaveCount(3)
  await shot(page, 'J5GM-01', 'edges-connected')

  // --- TRIGGER：raw 模式填 triggerType ---
  await selectNode(page, 'TRIGGER')
  await page.getByTestId('mode-toggle').click()
  await page.getByTestId('raw-input').fill('{"triggerType":"MANUAL"}')

  // --- LLM_ASSISTANT：OpenRouter + JSON ---
  await selectNode(page, 'LLM_ASSISTANT')
  await page.getByTestId('llm-select').selectOption(llmId!)
  await page
    .getByTestId('user-prompt')
    .fill(
      '請使用可用的地圖工具，查詢台灣新北市金山區附近的咖啡廳，列出 3-5 間，每間含 name、rating、address，並以 JSON 物件回覆 {"places":[...],"summary":""}。',
    )
  await page.getByTestId('response-format').selectOption('JSON')
  await page
    .getByTestId('output-schema')
    .fill('{"type":"object","properties":{"places":{"type":"array"},"summary":{"type":"string"}}}')
  await page.getByTestId('output-key').fill('result')

  // --- MCP_SERVER(google_map)：mcpId + toolName + userSettingId（關鍵：注入 Maps 金鑰）---
  await selectNode(page, 'MCP_SERVER')
  await page.getByTestId('mcp-select').selectOption(seed.googleMapMcpId!)
  await page.getByTestId('tool-name').fill(GM_TOOL_NAME)
  await page.getByTestId('mcp-setting-id').fill(GM_SETTING_ID)
  await shot(page, 'J5GM-01', 'mcp-config')

  // --- OUTPUT：mappings 引用 LLM 輸出 ---
  await selectNode(page, 'OUTPUT')
  await page.getByTestId('output-mappings').fill(`result={{${llmKey}.result}}`)

  // --- 存檔 ---
  await page.getByTestId('save-button').click()
  await expect(page.getByTestId('saved-badge')).toBeVisible()

  // --- 執行（真實 OpenRouter + STDIO google_map MCP，SSE）---
  await page.getByTestId('run-button').click()
  const drawer = page.getByTestId('result-drawer')
  await expect(drawer).toBeVisible({ timeout: 60_000 })
  await expect(drawer.locator('.status')).toHaveText('SUCCESS', { timeout: 220_000 })
  await shot(page, 'J5GM-01', 'status-success')

  // OUTPUT 為 JSON（不比對內容；深度驗證見報告 202607222159）
  const outputText = await drawer.locator('.output-json').textContent()
  expect((outputText ?? '').length).toBeGreaterThan(0)

  // --- DB 落庫斷言（直連 Postgres）---
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
