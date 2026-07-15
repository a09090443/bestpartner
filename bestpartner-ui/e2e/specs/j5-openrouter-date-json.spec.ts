import { test, expect, request, type Page } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { fetchLatestExecution } from '../fixtures/db'

const __dirname = dirname(fileURLToPath(import.meta.url))

/**
 * J5 垂直切片（首條真實 E2E）：透過 UI 建構並執行
 *   TRIGGER → LLM_ASSISTANT(OpenRouter, responseFormat=JSON) ←in:tool← TOOL(DateTool) → OUTPUT(JSON)
 *
 * ID 動態解析：llmId / dateToolId 由 global-setup 依當前 DB 解析後寫入 .artifacts/seed.json，
 *   本 spec 讀取之，不硬編（運行 DB 與種子檔 ID 會漂移）。
 *
 * 斷言邊界（真實 LLM，且 in:tool 能力掛載為後端待查項）：
 *   只驗 plumbing / SSE 最終 SUCCESS / OUTPUT 為 JSON / DB 落庫；
 *   不比對 LLM 輸出文字，也不斷言 LLM 實際呼叫了 date 工具。
 */

interface ResolvedSeed {
  llmId: string | null
  dateToolId: string | null
}
const seed: ResolvedSeed = JSON.parse(
  readFileSync(resolve(__dirname, '../.artifacts/seed.json'), 'utf-8'),
)

const API = process.env.E2E_API_URL || 'http://localhost:80'
const ADMIN = {
  email: process.env.E2E_ADMIN_USER || 'admin@bestpartner.com.tw',
  password: process.env.E2E_ADMIN_PASS || 'admin',
}
const RUN_TAG = `run${Date.now()}`
const WF_NAME = `e2e-j5-openrouter-date-${RUN_TAG}`

/** 報告逐步證據截圖（e2e-test-confirmation skill 要求每步驟一張） */
const SHOTS_DIR = resolve(__dirname, '../../../docs/test-confirmations/e2e-shots/202607152227')
let shotSeq = 0
async function shot(page: Page, caseId: string, label: string) {
  shotSeq += 1
  const n = String(shotSeq).padStart(2, '0')
  await page.screenshot({ path: resolve(SHOTS_DIR, `${caseId}-${n}-${label}.png`), fullPage: true })
}

/** 合成 HTML5 拖放：palette item → 畫布指定畫面座標（Vue Flow 用原生 DnD 新增節點） */
async function dragNodeToCanvas(page: Page, nodeType: string, x: number, y: number) {
  await page.evaluate(
    ({ nodeType, x, y }) => {
      const src = document.querySelector(`[data-test="palette-item-${nodeType}"]`) as HTMLElement
      const canvas = document.querySelector('.canvas') as HTMLElement
      if (!src || !canvas) throw new Error(`missing src/canvas for ${nodeType}`)
      const dt = new DataTransfer()
      const r = src.getBoundingClientRect()
      const fire = (el: Element, type: string, cx: number, cy: number) =>
        el.dispatchEvent(
          new DragEvent(type, { bubbles: true, cancelable: true, clientX: cx, clientY: cy, dataTransfer: dt }),
        )
      fire(src, 'dragstart', r.x + 8, r.y + 8)
      fire(canvas, 'dragenter', x, y)
      fire(canvas, 'dragover', x, y)
      fire(canvas, 'drop', x, y)
      fire(src, 'dragend', x, y)
    },
    { nodeType, x, y },
  )
}

/** 以 pointer 事件連線：來源節點 sourceHandle → 目標節點 targetHandle */
async function connect(page: Page, fromType: string, fromHandle: string, toType: string, toHandle: string) {
  const src = page.locator(`[data-node-type="${fromType}"] .vue-flow__handle[data-handleid="${fromHandle}"]`).first()
  const tgt = page.locator(`[data-node-type="${toType}"] .vue-flow__handle[data-handleid="${toHandle}"]`).first()
  const sb = await src.boundingBox()
  const tb = await tgt.boundingBox()
  if (!sb || !tb) throw new Error(`handle box missing ${fromType}:${fromHandle} → ${toType}:${toHandle}`)
  const sc = { x: sb.x + sb.width / 2, y: sb.y + sb.height / 2 }
  const tc = { x: tb.x + tb.width / 2, y: tb.y + tb.height / 2 }
  await page.mouse.move(sc.x, sc.y)
  await page.mouse.down()
  await page.mouse.move((sc.x + tc.x) / 2, (sc.y + tc.y) / 2, { steps: 6 })
  await page.mouse.move(tc.x, tc.y, { steps: 6 })
  await page.mouse.up()
}

async function selectNode(page: Page, nodeType: string) {
  await page.locator(`[data-node-type="${nodeType}"]`).first().click()
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

test('J5：建構並執行 OpenRouter + date tool + JSON 輸出 workflow', async ({ page }) => {
  test.skip(!seed.llmId, '缺 OpenRouter CHAT llmId（seed 解析不到且未給 E2E_LLM_ID），跳過真實執行')
  test.setTimeout(180_000)
  expect(seed.dateToolId, 'DateTool 未解析').toBeTruthy()

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

  // --- J3 拖 4 節點 ---
  const nodes = page.locator('.vue-flow__node')
  await dragNodeToCanvas(page, 'TRIGGER', 480, 300)
  await expect(nodes).toHaveCount(1)
  await dragNodeToCanvas(page, 'LLM_ASSISTANT', 820, 300)
  await expect(nodes).toHaveCount(2)
  await dragNodeToCanvas(page, 'TOOL', 820, 540)
  await expect(nodes).toHaveCount(3)
  await dragNodeToCanvas(page, 'OUTPUT', 1160, 300)
  await expect(nodes).toHaveCount(4)
  await shot(page, 'J3-01', 'four-nodes-dropped')

  const llmKey = await page
    .locator('[data-node-type="LLM_ASSISTANT"]')
    .first()
    .evaluate((el) => el.closest('.vue-flow__node')?.getAttribute('data-id') || '')
  expect(llmKey).not.toEqual('')

  // --- J3 連線 ---
  await connect(page, 'TRIGGER', 'out:main', 'LLM_ASSISTANT', 'in:main')
  await connect(page, 'TOOL', 'out:main', 'LLM_ASSISTANT', 'in:tool')
  await connect(page, 'LLM_ASSISTANT', 'out:main', 'OUTPUT', 'in:main')
  await expect(page.locator('.vue-flow__edge')).toHaveCount(3)
  await shot(page, 'J3-02', 'edges-connected')

  // --- TRIGGER：JsonConfigEditor raw 模式填 triggerType ---
  await selectNode(page, 'TRIGGER')
  await page.getByTestId('mode-toggle').click()
  await page.getByTestId('raw-input').fill('{"triggerType":"MANUAL"}')
  await shot(page, 'J3-03', 'trigger-config')

  // --- LLM_ASSISTANT：OpenRouter + JSON ---
  await selectNode(page, 'LLM_ASSISTANT')
  await page.getByTestId('llm-select').selectOption(seed.llmId!)
  await page.getByTestId('user-prompt').fill('請以 JSON 回覆一個包含 now 欄位的物件')
  await page.getByTestId('response-format').selectOption('JSON')
  await page.getByTestId('output-schema').fill('{"type":"object","properties":{"now":{"type":"string"}}}')
  await page.getByTestId('output-key').fill('reply')
  await shot(page, 'J3-03', 'llm-assistant-config')

  // --- TOOL：DateTool（需 zoneId，否則 ZoneId.of(null) 失敗）---
  await selectNode(page, 'TOOL')
  await page.getByTestId('tool-select').selectOption(seed.dateToolId!)
  await page.getByTestId('arguments').fill('{"zoneId":"Asia/Taipei"}')
  await shot(page, 'J3-03', 'tool-config')

  // --- OUTPUT：mappings 產 JSON，引用 LLM 輸出 ---
  await selectNode(page, 'OUTPUT')
  await page.getByTestId('output-mappings').fill(`now={{${llmKey}.reply}}`)
  await shot(page, 'J3-03', 'output-config')

  // --- 存檔 ---
  await page.getByTestId('save-button').click()
  await expect(page.getByTestId('saved-badge')).toBeVisible()
  await shot(page, 'J3-04', 'saved-v2')

  // --- 執行（真實 OpenRouter，SSE）---
  await page.getByTestId('run-button').click()
  const drawer = page.getByTestId('result-drawer')
  await expect(drawer).toBeVisible({ timeout: 150_000 })
  await shot(page, 'J5-01', 'drawer-visible')
  await expect(drawer.locator('.status')).toHaveText('SUCCESS')
  await shot(page, 'J5-01', 'status-success')

  // OUTPUT 為 JSON 物件（不比對內容）
  const outputText = await drawer.locator('.output-json').textContent()
  expect(() => JSON.parse(outputText ?? '')).not.toThrow()

  // --- DB 落庫斷言（Q2=B：直連 Postgres）---
  const rec = await fetchLatestExecution(WF_NAME)
  expect(rec, 'DB 無執行紀錄').not.toBeNull()
  expect(rec!.execution.status).toBe('SUCCESS')
  expect(rec!.execution.trigger_type).toBe('MANUAL')
  // Agent 模式：TOOL 掛 in:tool 為能力掛載，不獨立執行、不落 node_execution（見 e2e-test-plan.md §3 J5 案例二）
  const nodeTypes = rec!.nodes.map((n) => n.node_type)
  expect(nodeTypes).not.toContain('TOOL')
  expect(nodeTypes).toContain('TRIGGER')
  expect(nodeTypes).toContain('LLM_ASSISTANT')
  expect(nodeTypes).toContain('OUTPUT')
  expect(rec!.nodes.every((n) => n.status === 'SUCCESS')).toBeTruthy()
})
