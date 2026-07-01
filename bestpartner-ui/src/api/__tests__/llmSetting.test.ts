import { describe, it, expect, vi, beforeEach } from 'vitest'

const postMock = vi.fn()
const getMock = vi.fn()
vi.mock('../http', () => ({
  default: {
    post: (...args: unknown[]) => postMock(...args),
    get: (...args: unknown[]) => getMock(...args),
  },
}))

import * as llmSettingApi from '../llmSetting'

function apiOk<T>(data: T) {
  return { data: { code: 200, message: 'ok', data } }
}

describe('llmSetting API client', () => {
  beforeEach(() => {
    postMock.mockReset()
    getMock.mockReset()
  })

  it('getSettings() 應 POST /llm/setting/get（空 body）並轉為 Option[]', async () => {
    postMock.mockResolvedValueOnce(
      apiOk([
        { id: 's1', alias: 'GPT-4o', modelType: 'CHAT' },
        { id: 's2', alias: 'Claude', modelType: 'STREAMING_CHAT' },
      ]),
    )

    const result = await llmSettingApi.getSettings()

    expect(postMock).toHaveBeenCalledWith('/llm/setting/get', {})
    expect(result).toEqual([
      { value: 's1', label: 'GPT-4o' },
      { value: 's2', label: 'Claude' },
    ])
  })

  it('alias 缺失時以 id 作為 label', async () => {
    postMock.mockResolvedValueOnce(apiOk([{ id: 's3', modelType: 'CHAT' }]))

    const result = await llmSettingApi.getSettings()

    expect(result).toEqual([{ value: 's3', label: 's3' }])
  })

  it('data 為 null 時回空陣列', async () => {
    postMock.mockResolvedValueOnce(apiOk(null))

    const result = await llmSettingApi.getSettings()

    expect(result).toEqual([])
  })
})
