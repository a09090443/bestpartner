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

  it('getSettings() 應 POST /llm/setting/get（空 body）並轉為 Option[]，label 標示 modelType', async () => {
    postMock.mockResolvedValueOnce(
      apiOk([
        { id: 's1', alias: 'GPT-4o', modelType: 'CHAT' },
        { id: 's2', alias: 'Claude', modelType: 'STREAMING_CHAT' },
      ]),
    )

    const result = await llmSettingApi.getSettings()

    expect(postMock).toHaveBeenCalledWith('/llm/setting/get', {})
    expect(result).toEqual([
      { value: 's1', label: 'GPT-4o（CHAT）' },
      { value: 's2', label: 'Claude（STREAMING_CHAT）' },
    ])
  })

  it('過濾掉 EMBEDDING 型別的設定（無法用於對話節點）', async () => {
    postMock.mockResolvedValueOnce(
      apiOk([
        { id: 's1', alias: 'GPT-4o', modelType: 'CHAT' },
        { id: 's9', alias: 'text-embedding', modelType: 'EMBEDDING' },
      ]),
    )

    const result = await llmSettingApi.getSettings()

    expect(result).toEqual([{ value: 's1', label: 'GPT-4o（CHAT）' }])
  })

  it('alias 缺失時以 id 作為 label；modelType 缺失時不加註記', async () => {
    postMock.mockResolvedValueOnce(apiOk([{ id: 's3' }]))

    const result = await llmSettingApi.getSettings()

    expect(result).toEqual([{ value: 's3', label: 's3' }])
  })

  it('data 為 null 時回空陣列', async () => {
    postMock.mockResolvedValueOnce(apiOk(null))

    const result = await llmSettingApi.getSettings()

    expect(result).toEqual([])
  })

  it('getEmbeddingSettings() 僅保留 EMBEDDING 型別、濾除 CHAT', async () => {
    postMock.mockResolvedValueOnce(
      apiOk([
        { id: 's1', alias: 'GPT-4o', modelType: 'CHAT' },
        { id: 's9', alias: 'text-embedding', modelType: 'EMBEDDING' },
        { id: 's2', alias: 'Claude', modelType: 'STREAMING_CHAT' },
      ]),
    )

    const result = await llmSettingApi.getEmbeddingSettings()

    expect(postMock).toHaveBeenCalledWith('/llm/setting/get', {})
    expect(result).toEqual([{ value: 's9', label: 'text-embedding' }])
  })
})
