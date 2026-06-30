import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('../../api/workflow', () => ({
  get: vi.fn(),
  save: vi.fn(),
}))

import * as workflowApi from '../../api/workflow'
import { useWorkflowStore, WorkflowVersionConflictError } from '../workflow'
import type { WorkflowDTO } from '../../types/workflow'

const loaded: WorkflowDTO = {
  id: 'wf-1',
  name: '已存在流程',
  description: 'desc',
  status: 'DRAFT',
  version: 2,
  canvasMeta: { zoom: 1 },
  nodes: [
    { nodeKey: 'n1', type: 'TRIGGER', name: '開始', positionX: 0, positionY: 0, config: {} },
  ],
  edges: [],
}

describe('workflowStore — 編輯與存取', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('load(id) 應寫入當前編輯 state（id/name/version/nodes/edges）', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)

    const store = useWorkflowStore()
    await store.load('wf-1')

    expect(workflowApi.get).toHaveBeenCalledWith('wf-1')
    expect(store.current?.id).toBe('wf-1')
    expect(store.current?.name).toBe('已存在流程')
    expect(store.current?.version).toBe(2)
    expect(store.current?.nodes).toHaveLength(1)
    expect(store.current?.nodes[0].id).toBe('n1')
    expect(store.dirty).toBe(false)
  })

  it('createNew() 應清為新建狀態（無 id）', () => {
    const store = useWorkflowStore()
    store.createNew()

    expect(store.current).not.toBeNull()
    expect(store.current?.id).toBeUndefined()
    expect(store.current?.nodes).toEqual([])
    expect(store.current?.edges).toEqual([])
    expect(store.dirty).toBe(false)
  })

  it('save() 成功應以回傳的新 version 更新 state，dirty 變 false', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    vi.mocked(workflowApi.save).mockResolvedValueOnce({ ...loaded, version: 3 })

    const store = useWorkflowStore()
    await store.load('wf-1')
    store.setDirty(true)

    await store.save(
      [{ id: 'n1', type: 'workflow', position: { x: 0, y: 0 }, data: { name: '開始', type: 'TRIGGER', config: {} } }],
      [],
    )

    expect(workflowApi.save).toHaveBeenCalled()
    expect(store.current?.version).toBe(3)
    expect(store.dirty).toBe(false)
  })

  it('save() 遇版本衝突應拋出 WorkflowVersionConflictError', async () => {
    vi.mocked(workflowApi.get).mockResolvedValueOnce(loaded)
    vi.mocked(workflowApi.save).mockRejectedValueOnce({
      response: { data: { code: 400, message: '工作流程已被其他作業修改，請重新載入' } },
    })

    const store = useWorkflowStore()
    await store.load('wf-1')

    await expect(
      store.save(
        [{ id: 'n1', type: 'workflow', position: { x: 0, y: 0 }, data: { name: '開始', type: 'TRIGGER', config: {} } }],
        [],
      ),
    ).rejects.toBeInstanceOf(WorkflowVersionConflictError)
  })
})
