import { describe, it, expect, vi, beforeEach } from 'vitest'

const postMock = vi.fn()
const getMock = vi.fn()
vi.mock('../http', () => ({
  default: {
    post: (...args: unknown[]) => postMock(...args),
    get: (...args: unknown[]) => getMock(...args),
  },
}))

import * as vectorApi from '../vector'

function apiOk<T>(data: T) {
  return { data: { code: 200, message: 'ok', data } }
}

describe('vector API client', () => {
  beforeEach(() => {
    postMock.mockReset()
    getMock.mockReset()
  })

  it('listKnowledgeStores() 應 POST /llm/vector/getKnowledgeStore（空 body）並依 knowledgeId 去重轉為 Option[]', async () => {
    postMock.mockResolvedValueOnce(
      apiOk([
        { knowledgeId: 'k1', knowledgeName: '產品手冊', fileName: 'a.pdf' },
        { knowledgeId: 'k1', knowledgeName: '產品手冊', fileName: 'b.pdf' },
        { knowledgeId: 'k2', knowledgeName: 'FAQ', fileName: 'c.md' },
      ]),
    )

    const result = await vectorApi.listKnowledgeStores()

    expect(postMock).toHaveBeenCalledWith('/llm/vector/getKnowledgeStore', {})
    expect(result).toEqual([
      { value: 'k1', label: '產品手冊' },
      { value: 'k2', label: 'FAQ' },
    ])
  })

  it('knowledgeName 缺失時以 knowledgeId 作為 label；無 knowledgeId 者略過', async () => {
    postMock.mockResolvedValueOnce(
      apiOk([{ knowledgeId: 'k3' }, { knowledgeName: '無 id' }]),
    )

    const result = await vectorApi.listKnowledgeStores()

    expect(result).toEqual([{ value: 'k3', label: 'k3' }])
  })

  it('data 為 null 時回空陣列', async () => {
    postMock.mockResolvedValueOnce(apiOk(null))

    const result = await vectorApi.listKnowledgeStores()

    expect(result).toEqual([])
  })
})
