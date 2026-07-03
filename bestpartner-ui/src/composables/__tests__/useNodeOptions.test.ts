import { describe, it, expect, vi, beforeEach } from 'vitest'

const getSettings = vi.fn()
const listTools = vi.fn()
const listMcpServers = vi.fn()
const listKnowledgeStores = vi.fn()

vi.mock('../../api/llmSetting', () => ({ getSettings: (...a: unknown[]) => getSettings(...a) }))
vi.mock('../../api/tool', () => ({ listTools: (...a: unknown[]) => listTools(...a) }))
vi.mock('../../api/mcpServer', () => ({ listMcpServers: (...a: unknown[]) => listMcpServers(...a) }))
vi.mock('../../api/vector', () => ({
  listKnowledgeStores: (...a: unknown[]) => listKnowledgeStores(...a),
}))

describe('useNodeOptions', () => {
  beforeEach(() => {
    vi.resetModules() // 重置模組級快取
    getSettings.mockReset()
    listTools.mockReset()
    listMcpServers.mockReset()
    listKnowledgeStores.mockReset()
  })

  it('loadLlmOptions 首次載入回傳 api 結果', async () => {
    getSettings.mockResolvedValue([{ value: 's1', label: 'A' }])
    const { useNodeOptions } = await import('../useNodeOptions')
    const result = await useNodeOptions().loadLlmOptions()
    expect(result).toEqual([{ value: 's1', label: 'A' }])
    expect(getSettings).toHaveBeenCalledTimes(1)
  })

  it('loadLlmOptions 第二次呼叫使用快取，不再打 API', async () => {
    getSettings.mockResolvedValue([{ value: 's1', label: 'A' }])
    const { useNodeOptions } = await import('../useNodeOptions')
    const { loadLlmOptions } = useNodeOptions()
    const first = await loadLlmOptions()
    const second = await loadLlmOptions()
    expect(getSettings).toHaveBeenCalledTimes(1)
    expect(second).toBe(first)
  })

  it('loadToolOptions / loadMcpOptions 各自委派對應 api', async () => {
    listTools.mockResolvedValue([{ value: 't1', label: 'Tool' }])
    listMcpServers.mockResolvedValue([{ value: 'm1', label: 'Mcp' }])
    const { useNodeOptions } = await import('../useNodeOptions')
    const opts = useNodeOptions()
    expect(await opts.loadToolOptions()).toEqual([{ value: 't1', label: 'Tool' }])
    expect(await opts.loadMcpOptions()).toEqual([{ value: 'm1', label: 'Mcp' }])
  })

  it('loadKnowledgeOptions 委派 vector api 並快取', async () => {
    listKnowledgeStores.mockResolvedValue([{ value: 'k1', label: '知識庫' }])
    const { useNodeOptions } = await import('../useNodeOptions')
    const { loadKnowledgeOptions } = useNodeOptions()
    const first = await loadKnowledgeOptions()
    const second = await loadKnowledgeOptions()
    expect(first).toEqual([{ value: 'k1', label: '知識庫' }])
    expect(listKnowledgeStores).toHaveBeenCalledTimes(1)
    expect(second).toBe(first)
  })
})
