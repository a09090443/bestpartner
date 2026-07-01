import { describe, it, expect, vi, beforeEach } from 'vitest'

const getSettings = vi.fn()
const listTools = vi.fn()
const listMcpServers = vi.fn()

vi.mock('../../api/llmSetting', () => ({ getSettings: (...a: unknown[]) => getSettings(...a) }))
vi.mock('../../api/tool', () => ({ listTools: (...a: unknown[]) => listTools(...a) }))
vi.mock('../../api/mcpServer', () => ({ listMcpServers: (...a: unknown[]) => listMcpServers(...a) }))

describe('useNodeOptions', () => {
  beforeEach(() => {
    vi.resetModules() // 重置模組級快取
    getSettings.mockReset()
    listTools.mockReset()
    listMcpServers.mockReset()
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
})
