import { describe, it, expect, vi, beforeEach } from 'vitest'

const postMock = vi.fn()
const getMock = vi.fn()
vi.mock('../http', () => ({
  default: {
    post: (...args: unknown[]) => postMock(...args),
    get: (...args: unknown[]) => getMock(...args),
  },
}))

import * as toolApi from '../tool'

function apiOk<T>(data: T) {
  return { data: { code: 200, message: 'ok', data } }
}

describe('tool API client', () => {
  beforeEach(() => {
    postMock.mockReset()
    getMock.mockReset()
  })

  it('listTools() 應 GET /llm/tool/list 並轉為 Option[]', async () => {
    getMock.mockResolvedValueOnce(
      apiOk([
        { id: 't1', name: 'Google 搜尋' },
        { id: 't2', name: 'Tavily' },
      ]),
    )

    const result = await toolApi.listTools()

    expect(getMock).toHaveBeenCalledWith('/llm/tool/list')
    expect(result).toEqual([
      { value: 't1', label: 'Google 搜尋' },
      { value: 't2', label: 'Tavily' },
    ])
  })

  it('name 缺失時以 id 作為 label；無 id 者略過', async () => {
    getMock.mockResolvedValueOnce(apiOk([{ id: 't3' }, { name: '無 id' }]))

    const result = await toolApi.listTools()

    expect(result).toEqual([{ value: 't3', label: 't3' }])
  })

  it('getTool() 應 POST /llm/tool/get 並解包回傳 DTO', async () => {
    const dto = {
      id: 't1',
      name: 'Google 搜尋',
      settingSchema: { apiKey: { type: 'string', required: true, sensitive: true } },
    }
    postMock.mockResolvedValueOnce(apiOk(dto))

    const result = await toolApi.getTool('t1')

    expect(postMock).toHaveBeenCalledWith('/llm/tool/get', { id: 't1' })
    expect(result).toEqual(dto)
  })

  it('saveToolSetting() 應 POST /llm/tool/saveSetting 並回傳含 settingId 的 DTO', async () => {
    postMock.mockResolvedValueOnce(apiOk({ id: 't1', alias: '我的搜尋', settingId: 's9' }))

    const result = await toolApi.saveToolSetting('t1', '我的搜尋')

    expect(postMock).toHaveBeenCalledWith('/llm/tool/saveSetting', { id: 't1', alias: '我的搜尋' })
    expect(result.settingId).toBe('s9')
  })

  it('updateToolSetting() 應 POST /llm/tool/updateSetting 帶 settingId 與 settingContent', async () => {
    postMock.mockResolvedValueOnce(apiOk(null))

    await toolApi.updateToolSetting('s9', '{"apiKey":"xxx"}')

    expect(postMock).toHaveBeenCalledWith('/llm/tool/updateSetting', {
      settingId: 's9',
      settingContent: '{"apiKey":"xxx"}',
    })
  })
})
