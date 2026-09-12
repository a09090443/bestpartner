import { describe, it, expect, vi, beforeEach } from 'vitest'

const postMock = vi.fn()
const getMock = vi.fn()
vi.mock('../http', () => ({
  default: {
    post: (...args: unknown[]) => postMock(...args),
    get: (...args: unknown[]) => getMock(...args),
  },
}))

import * as mcpApi from '../mcpServer'

function apiOk<T>(data: T) {
  return { data: { code: 200, message: 'ok', data } }
}

describe('mcpServer API client', () => {
  beforeEach(() => {
    postMock.mockReset()
    getMock.mockReset()
  })

  it('listMcpServers() 應 GET /llm/mcpServer/list 並轉為 Option[]（value = mcpId）', async () => {
    getMock.mockResolvedValueOnce(
      apiOk([
        { mcpId: 'm1', name: '天氣 MCP' },
        { mcpId: 'm2', name: '檔案 MCP' },
      ]),
    )

    const result = await mcpApi.listMcpServers()

    expect(getMock).toHaveBeenCalledWith('/llm/mcpServer/list')
    expect(result).toEqual([
      { value: 'm1', label: '天氣 MCP' },
      { value: 'm2', label: '檔案 MCP' },
    ])
  })

  it('name 缺失時以 mcpId 作為 label；無 mcpId 者略過', async () => {
    getMock.mockResolvedValueOnce(apiOk([{ mcpId: 'm3' }, { name: '無 id' }]))

    const result = await mcpApi.listMcpServers()

    expect(result).toEqual([{ value: 'm3', label: 'm3' }])
  })
})
