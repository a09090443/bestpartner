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
    // raw list 快取為模組級狀態，跨測試殘留；每個案例前清除以維持獨立性
    llmSettingApi.invalidateSettingsCache()
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

  describe('raw list 快取共用', () => {
    it('getSettings 與 getEmbeddingSettings 並發呼叫共用一次請求，過濾結果各自正確', async () => {
      postMock.mockResolvedValue(
        apiOk([
          { id: 's1', alias: 'GPT-4o', modelType: 'CHAT' },
          { id: 's9', alias: 'text-embedding', modelType: 'EMBEDDING' },
        ]),
      )

      const [chat, embedding] = await Promise.all([
        llmSettingApi.getSettings(),
        llmSettingApi.getEmbeddingSettings(),
      ])

      // 兩 selector 共用同一次 POST（Promise 去重）
      expect(postMock).toHaveBeenCalledTimes(1)
      expect(chat).toEqual([{ value: 's1', label: 'GPT-4o（CHAT）' }])
      expect(embedding).toEqual([{ value: 's9', label: 'text-embedding' }])
    })

    it('連續呼叫兩 selector 命中快取，只發一次請求', async () => {
      postMock.mockResolvedValue(apiOk([{ id: 's1', alias: 'GPT-4o', modelType: 'CHAT' }]))

      await llmSettingApi.getSettings()
      await llmSettingApi.getEmbeddingSettings()

      expect(postMock).toHaveBeenCalledTimes(1)
    })

    it('invalidateSettingsCache 後再查詢會重新發出請求', async () => {
      postMock.mockResolvedValue(apiOk([{ id: 's1', alias: 'GPT-4o', modelType: 'CHAT' }]))

      await llmSettingApi.getSettings()
      expect(postMock).toHaveBeenCalledTimes(1)
      await llmSettingApi.getSettings()
      expect(postMock).toHaveBeenCalledTimes(1)

      llmSettingApi.invalidateSettingsCache()
      await llmSettingApi.getSettings()
      expect(postMock).toHaveBeenCalledTimes(2)
    })

    it('請求失敗不留下壞快取，下次查詢可重試', async () => {
      postMock.mockRejectedValueOnce(new Error('boom'))
      await expect(llmSettingApi.getSettings()).rejects.toThrow('boom')

      postMock.mockResolvedValueOnce(apiOk([{ id: 's1', alias: 'GPT-4o', modelType: 'CHAT' }]))
      const result = await llmSettingApi.getSettings()
      expect(result).toEqual([{ value: 's1', label: 'GPT-4o（CHAT）' }])
      expect(postMock).toHaveBeenCalledTimes(2)
    })
  })
})
