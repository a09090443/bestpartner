import { request } from '@playwright/test'

/**
 * E2E global-teardown：以前綴掃描兜底刪除任何殘留 `e2e-*` workflow
 * （連鎖刪 node/edge/execution）。不建立任何備份檔／目錄，遵守專案資料鐵則。
 */

const API = process.env.E2E_API_URL || 'http://localhost:80'
const ADMIN = {
  email: process.env.E2E_ADMIN_USER || 'admin@bestpartner.com.tw',
  password: process.env.E2E_ADMIN_PASS || 'admin',
}

export default async function globalTeardown() {
  const ctx = await request.newContext({ baseURL: API })
  try {
    const jwt = (await (await ctx.post('/login/', { data: ADMIN })).json())?.data as string | undefined
    if (!jwt) return
    const auth = { Authorization: `Bearer ${jwt}` }
    const list = ((await (await ctx.get('/llm/workflow/list', { headers: auth })).json())?.data ??
      []) as Array<{ id?: string; name?: string }>
    for (const wf of list) {
      if (wf.id && (wf.name ?? '').startsWith('e2e-')) {
        await ctx.post('/llm/workflow/delete', { data: { id: wf.id }, headers: auth })
      }
    }
  } finally {
    await ctx.dispose()
  }
}
