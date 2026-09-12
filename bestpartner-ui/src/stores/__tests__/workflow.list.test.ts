import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock api/workflow，避免真實 HTTP 請求
vi.mock('../../api/workflow', () => ({
  list: vi.fn(),
  remove: vi.fn(),
  switchStatus: vi.fn(),
}))

import * as workflowApi from '../../api/workflow'
import { useWorkflowStore } from '../workflow'
import type { WorkflowSummaryDTO } from '../../types/workflow'

describe('workflowStore — 清單', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('fetchList() 應將 API 回傳寫入 summaries', async () => {
    const data: WorkflowSummaryDTO[] = [
      { id: 'w1', name: 'A', status: 'DRAFT', version: 1 },
      { id: 'w2', name: 'B', status: 'ACTIVE', version: 2 },
    ]
    vi.mocked(workflowApi.list).mockResolvedValueOnce(data)

    const store = useWorkflowStore()
    await store.fetchList()

    expect(store.summaries).toEqual(data)
  })

  it('remove(id) 成功後應從 summaries 移除該筆', async () => {
    vi.mocked(workflowApi.list).mockResolvedValueOnce([
      { id: 'w1', name: 'A', status: 'DRAFT' },
      { id: 'w2', name: 'B', status: 'DRAFT' },
    ])
    vi.mocked(workflowApi.remove).mockResolvedValueOnce()

    const store = useWorkflowStore()
    await store.fetchList()
    await store.remove('w1')

    expect(workflowApi.remove).toHaveBeenCalledWith('w1')
    expect(store.summaries.map((s) => s.id)).toEqual(['w2'])
  })

  it('switchStatus(id, active) 後應更新該筆 status', async () => {
    vi.mocked(workflowApi.list).mockResolvedValueOnce([
      { id: 'w1', name: 'A', status: 'DRAFT' },
    ])
    vi.mocked(workflowApi.switchStatus).mockResolvedValueOnce()

    const store = useWorkflowStore()
    await store.fetchList()
    await store.switchStatus('w1', true)

    expect(workflowApi.switchStatus).toHaveBeenCalledWith('w1', true)
    expect(store.summaries.find((s) => s.id === 'w1')?.status).toBe('ACTIVE')
  })

  it('switchStatus(id, false) 應將 status 設為 INACTIVE', async () => {
    vi.mocked(workflowApi.list).mockResolvedValueOnce([
      { id: 'w1', name: 'A', status: 'ACTIVE' },
    ])
    vi.mocked(workflowApi.switchStatus).mockResolvedValueOnce()

    const store = useWorkflowStore()
    await store.fetchList()
    await store.switchStatus('w1', false)

    expect(store.summaries.find((s) => s.id === 'w1')?.status).toBe('INACTIVE')
  })
})
