import { describe, it, expect, vi, beforeEach } from 'vitest'

const postMock = vi.fn()
const getMock = vi.fn()
vi.mock('../http', () => ({
  default: {
    post: (...args: unknown[]) => postMock(...args),
    get: (...args: unknown[]) => getMock(...args),
  },
}))

import * as skillApi from '../skill'

function apiOk<T>(data: T) {
  return { data: { code: 200, message: 'ok', data } }
}

describe('skill API client', () => {
  beforeEach(() => {
    postMock.mockReset()
    getMock.mockReset()
  })

  it('listSkills() 應 GET /llm/skill/list 並轉為 Option[]', async () => {
    getMock.mockResolvedValueOnce(
      apiOk([
        { id: 'sk1', name: 'pdf-report', isGlobal: false },
        { id: 'sk2', name: 'translate', isGlobal: true },
      ]),
    )

    const result = await skillApi.listSkills()

    expect(getMock).toHaveBeenCalledWith('/llm/skill/list')
    expect(result).toEqual([
      { value: 'sk1', label: 'pdf-report' },
      { value: 'sk2', label: 'translate' },
    ])
  })

  it('name 缺失時以 id 作為 label；無 id 者略過', async () => {
    getMock.mockResolvedValueOnce(apiOk([{ id: 'sk3' }, { name: '無 id' }]))

    const result = await skillApi.listSkills()

    expect(result).toEqual([{ value: 'sk3', label: 'sk3' }])
  })

  it('data 為 null 時回空陣列', async () => {
    getMock.mockResolvedValueOnce(apiOk(null))

    const result = await skillApi.listSkills()

    expect(result).toEqual([])
  })
})
