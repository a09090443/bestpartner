import { request } from '@playwright/test'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))

/**
 * E2E global-setup：對 port 80 真實後端做前置檢查，並「動態解析」種子 ID。
 *
 * 為何動態解析：運行中的 dev DB 其 llmId/toolId 與 docs/sql 種子檔可能不同
 * （實測 OpenRouter CHAT 於本機為 1ee80ffa…，種子檔為 583b9222…）。硬編 ID 必然漂移，
 * 故此處以 API 依 alias/platform/name 解析當前 DB 的真實 ID，寫入 .artifacts/seed.json 供 spec 讀取。
 */

const API = process.env.E2E_API_URL || 'http://localhost:80'
const ADMIN = {
  email: process.env.E2E_ADMIN_USER || 'admin@bestpartner.com.tw',
  password: process.env.E2E_ADMIN_PASS || 'admin',
}
export const ARTIFACT_DIR = resolve(__dirname, '.artifacts')
export const SEED_FILE = resolve(ARTIFACT_DIR, 'seed.json')

export interface ResolvedSeed {
  /** OpenRouter CHAT 的 llmId；解析不到則為 null（spec 會 skip 真實執行斷言） */
  llmId: string | null
  /** 內建 DateTool 的 toolId */
  dateToolId: string | null
  /** date MCP server（STDIO java -jar date.jar）的 mcpId；解析不到則為 null（MCP 切片 spec 會 skip） */
  dateMcpId: string | null
}

export default async function globalSetup() {
  const ctx = await request.newContext({ baseURL: API })
  try {
    // 1) 健康檢查：公開端點須 200
    const health = await ctx.get('/systemSetting/list')
    if (!health.ok()) throw new Error(`後端 port 80 未就緒（/systemSetting/list = ${health.status()}）`)

    // 2) admin API 登入取 JWT
    const login = await ctx.post('/login/', { data: ADMIN })
    const jwt = (await login.json())?.data as string | undefined
    if (!jwt) throw new Error('admin 登入失敗，無法取得 JWT')
    const auth = { Authorization: `Bearer ${jwt}` }

    // 3) 解析 OpenRouter CHAT llmId（env 優先）
    const settingsResp = await ctx.post('/llm/setting/get', { data: {}, headers: auth })
    const settings = ((await settingsResp.json())?.data ?? []) as Array<{
      id?: string
      alias?: string
      platform?: string
      modelType?: string
    }>
    const openrouterChat = settings.find(
      (s) =>
        s.modelType === 'CHAT' &&
        (s.platform === 'OPENROUTER' || /openrouter/i.test(s.alias ?? '')),
    )
    const llmId = process.env.E2E_LLM_ID || openrouterChat?.id || null

    // 4) 解析內建 DateTool toolId
    const toolsResp = await ctx.get('/llm/tool/list', { headers: auth })
    const tools = ((await toolsResp.json())?.data ?? []) as Array<{ id?: string; name?: string }>
    const dateToolId = tools.find((t) => t.name === 'DateTool')?.id ?? null
    if (!dateToolId) throw new Error('種子檢查失敗：找不到內建 DateTool')

    // 5) 解析 date MCP server 的 mcpId（依 name；E2E_MCP_NAME 可覆寫，缺則 MCP 切片 skip）
    const mcpName = process.env.E2E_MCP_NAME || 'date'
    const mcpResp = await ctx.get('/llm/mcpServer/list', { headers: auth })
    const mcps = ((await mcpResp.json())?.data ?? []) as Array<{ mcpId?: string; name?: string }>
    const dateMcpId = mcps.find((m) => m.name === mcpName)?.mcpId ?? null

    const seed: ResolvedSeed = { llmId, dateToolId, dateMcpId }
    mkdirSync(ARTIFACT_DIR, { recursive: true })
    writeFileSync(SEED_FILE, JSON.stringify(seed, null, 2))
    // eslint-disable-next-line no-console
    console.log('[globalSetup] 解析種子：', seed, llmId ? '' : '（llmId 缺，J5 真實執行將 skip）')
  } finally {
    await ctx.dispose()
  }
}
