import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock http 預設實例，攔截 get/post 並回傳可控的 ApiResponse 結構
const postMock = vi.fn()
const getMock = vi.fn()
vi.mock('../http', () => ({
  default: {
    post: (...args: unknown[]) => postMock(...args),
    get: (...args: unknown[]) => getMock(...args),
  },
}))

import * as workflowApi from '../workflow'
import type { WorkflowDTO, WorkflowSaveRequestDTO } from '../../types/workflow'

function apiOk<T>(data: T) {
  return { data: { code: 200, message: 'ok', data } }
}

describe('workflow API client', () => {
  beforeEach(() => {
    postMock.mockReset()
    getMock.mockReset()
  })

  it('create() 應 POST /llm/workflow/create 並回傳 data', async () => {
    const dto: WorkflowDTO = { id: 'w1', name: 'flow', nodes: [], edges: [] }
    postMock.mockResolvedValueOnce(apiOk(dto))

    const result = await workflowApi.create('flow')

    expect(postMock).toHaveBeenCalledWith('/llm/workflow/create', { name: 'flow' })
    expect(result).toEqual(dto)
  })

  it('save() 應 POST /llm/workflow/save 並回傳 data', async () => {
    const req: WorkflowSaveRequestDTO = { name: 'flow', nodes: [], edges: [] }
    const dto: WorkflowDTO = { id: 'w1', name: 'flow', nodes: [], edges: [] }
    postMock.mockResolvedValueOnce(apiOk(dto))

    const result = await workflowApi.save(req)

    expect(postMock).toHaveBeenCalledWith('/llm/workflow/save', req)
    expect(result).toEqual(dto)
  })

  it('get() 應 POST /llm/workflow/get 並回傳 data', async () => {
    const dto: WorkflowDTO = { id: 'w1', name: 'flow', nodes: [], edges: [] }
    postMock.mockResolvedValueOnce(apiOk(dto))

    const result = await workflowApi.get('w1')

    expect(postMock).toHaveBeenCalledWith('/llm/workflow/get', { id: 'w1' })
    expect(result).toEqual(dto)
  })

  it('list() 應 GET /llm/workflow/list 並回傳 data', async () => {
    const summaries = [{ id: 'w1', name: 'flow', status: 'DRAFT' as const }]
    getMock.mockResolvedValueOnce(apiOk(summaries))

    const result = await workflowApi.list()

    expect(getMock).toHaveBeenCalledWith('/llm/workflow/list')
    expect(result).toEqual(summaries)
  })

  it('update() 應 POST /llm/workflow/update 並回傳 data', async () => {
    const dto: WorkflowDTO = { id: 'w1', name: 'renamed', nodes: [], edges: [] }
    postMock.mockResolvedValueOnce(apiOk(dto))

    const result = await workflowApi.update({ id: 'w1', name: 'renamed' })

    expect(postMock).toHaveBeenCalledWith('/llm/workflow/update', { id: 'w1', name: 'renamed' })
    expect(result).toEqual(dto)
  })

  it('remove() 應 POST /llm/workflow/delete', async () => {
    postMock.mockResolvedValueOnce(apiOk(null))

    await workflowApi.remove('w1')

    expect(postMock).toHaveBeenCalledWith('/llm/workflow/delete', { id: 'w1' })
  })

  it('switchStatus() 應 POST /llm/workflow/switchStatus', async () => {
    postMock.mockResolvedValueOnce(apiOk(null))

    await workflowApi.switchStatus('w1', true)

    expect(postMock).toHaveBeenCalledWith('/llm/workflow/switchStatus', { id: 'w1', active: true })
  })

  describe('getNodeRequiredFields', () => {
    beforeEach(() => {
      // 模組級快取，跨測試殘留；每案例前清除以維持獨立性
      workflowApi.invalidateNodeRequiredFieldsCache()
    })

    it('應 GET /llm/workflow/nodeRequiredFields 並回傳 data', async () => {
      const fields = { LLM_ASSISTANT: ['llmId'], TOOL: ['toolId'] }
      getMock.mockResolvedValueOnce(apiOk(fields))

      const result = await workflowApi.getNodeRequiredFields()

      expect(getMock).toHaveBeenCalledWith('/llm/workflow/nodeRequiredFields')
      expect(result).toEqual(fields)
    })

    it('data 為 null 時回空物件', async () => {
      getMock.mockResolvedValueOnce(apiOk(null))

      const result = await workflowApi.getNodeRequiredFields()

      expect(result).toEqual({})
    })

    it('連續呼叫命中快取，只發一次請求', async () => {
      getMock.mockResolvedValue(apiOk({ TOOL: ['toolId'] }))

      await workflowApi.getNodeRequiredFields()
      await workflowApi.getNodeRequiredFields()

      expect(getMock).toHaveBeenCalledTimes(1)
    })

    it('請求失敗不留下壞快取，下次查詢可重試', async () => {
      getMock.mockRejectedValueOnce(new Error('boom'))
      await expect(workflowApi.getNodeRequiredFields()).rejects.toThrow('boom')

      getMock.mockResolvedValueOnce(apiOk({ TOOL: ['toolId'] }))
      const result = await workflowApi.getNodeRequiredFields()
      expect(result).toEqual({ TOOL: ['toolId'] })
      expect(getMock).toHaveBeenCalledTimes(2)
    })
  })
})
