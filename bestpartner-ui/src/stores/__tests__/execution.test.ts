import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock SSE api，避免真實 HTTP；executeWorkflow 回傳可控的 abort 並暴露回呼
vi.mock('../../api/workflowExecution', () => ({
  executeWorkflow: vi.fn(),
}))

import * as execApi from '../../api/workflowExecution'
import type { ExecutionEvent } from '../../api/workflowExecution'
import { useExecutionStore } from '../execution'

/** 便利建構 SSE 事件（ts 補預設值） */
function evt(e: Partial<ExecutionEvent> & Pick<ExecutionEvent, 'event' | 'executionId'>): ExecutionEvent {
  return { ts: '2026-07-11T00:00:00Z', ...e }
}

describe('executionStore — 取消後 RUNNING 節點終止態', () => {
  beforeEach(() => setActivePinia(createPinia()))
  afterEach(() => vi.clearAllMocks())

  it('stop() 應將所有仍為 RUNNING 的節點轉為 CANCELLED，已終止者不受影響', () => {
    const store = useExecutionStore()
    store.applyEvent(evt({ event: 'node.started', executionId: 'x', nodeKey: 'n1' }))
    store.applyEvent(evt({ event: 'node.started', executionId: 'x', nodeKey: 'n2' }))
    store.applyEvent(evt({ event: 'node.completed', executionId: 'x', nodeKey: 'n2' }))

    store.stop()

    expect(store.nodeStates.n1.status).toBe('CANCELLED')
    expect(store.nodeStates.n2.status).toBe('SUCCESS')
    expect(store.finalStatus).toBe('CANCELLED')
    expect(store.running).toBe(false)
  })

  it('收到 execution.completed status=CANCELLED 時 RUNNING 節點轉為 CANCELLED', () => {
    const store = useExecutionStore()
    store.applyEvent(evt({ event: 'node.started', executionId: 'x', nodeKey: 'n1' }))

    store.applyEvent(evt({ event: 'execution.completed', executionId: 'x', status: 'CANCELLED' }))

    expect(store.nodeStates.n1.status).toBe('CANCELLED')
    expect(store.finalStatus).toBe('CANCELLED')
    expect(store.running).toBe(false)
  })

  it('正常 SUCCESS 完成不影響節點狀態', () => {
    const store = useExecutionStore()
    store.applyEvent(evt({ event: 'node.started', executionId: 'x', nodeKey: 'n1' }))
    store.applyEvent(evt({ event: 'node.completed', executionId: 'x', nodeKey: 'n1' }))

    store.applyEvent(evt({ event: 'execution.completed', executionId: 'x', status: 'SUCCESS' }))

    expect(store.nodeStates.n1.status).toBe('SUCCESS')
    expect(store.finalStatus).toBe('SUCCESS')
  })

  it('SSE 中斷（onDone 而無 execution.completed 事件）時仍為 RUNNING 的節點轉為 CANCELLED', () => {
    let onDone: () => void = () => {}
    vi.mocked(execApi.executeWorkflow).mockImplementation((_id, onEvent, done) => {
      onDone = done
      onEvent(evt({ event: 'execution.started', executionId: 'x' }))
      onEvent(evt({ event: 'node.started', executionId: 'x', nodeKey: 'n1' }))
      return () => {}
    })

    const store = useExecutionStore()
    store.start('w1')
    onDone()

    expect(store.nodeStates.n1.status).toBe('CANCELLED')
  })

  it('SSE 正常完成（onDone 且已收到 execution.completed SUCCESS）不將節點轉為 CANCELLED', () => {
    let onDone: () => void = () => {}
    vi.mocked(execApi.executeWorkflow).mockImplementation((_id, onEvent, done) => {
      onDone = done
      onEvent(evt({ event: 'node.started', executionId: 'x', nodeKey: 'n1' }))
      onEvent(evt({ event: 'node.completed', executionId: 'x', nodeKey: 'n1' }))
      onEvent(evt({ event: 'execution.completed', executionId: 'x', status: 'SUCCESS' }))
      return () => {}
    })

    const store = useExecutionStore()
    store.start('w1')
    onDone()

    expect(store.nodeStates.n1.status).toBe('SUCCESS')
    expect(store.finalStatus).toBe('SUCCESS')
  })
})

describe('executionStore — 指定觸發點執行', () => {
  beforeEach(() => setActivePinia(createPinia()))
  afterEach(() => vi.clearAllMocks())

  it('start() 把 triggerNodeKey 透傳給 executeWorkflow 的 options', () => {
    vi.mocked(execApi.executeWorkflow).mockReturnValue(() => {})
    const store = useExecutionStore()

    store.start('w1', { triggerNodeKey: 'trg-1' })

    expect(vi.mocked(execApi.executeWorkflow).mock.calls[0][4]).toEqual({ triggerNodeKey: 'trg-1' })
    expect(store.triggerNodeKey).toBe('trg-1')
  })

  it('未指定觸發點時 options 為 undefined 且 store 的 triggerNodeKey 為 null', () => {
    vi.mocked(execApi.executeWorkflow).mockReturnValue(() => {})
    const store = useExecutionStore()

    store.start('w1')

    expect(vi.mocked(execApi.executeWorkflow).mock.calls[0][4]).toEqual({ triggerNodeKey: undefined })
    expect(store.triggerNodeKey).toBeNull()
  })

  it('skipNodeKeys 中的節點在開跑前即標為 SKIPPED（後端對 SKIPPED 不發事件）', () => {
    vi.mocked(execApi.executeWorkflow).mockReturnValue(() => {})
    const store = useExecutionStore()

    store.start('w1', { triggerNodeKey: 'trg-1', skipNodeKeys: ['trg-2', 'trg-3'] })

    expect(store.nodeStates['trg-2'].status).toBe('SKIPPED')
    expect(store.nodeStates['trg-3'].status).toBe('SKIPPED')
    expect(store.nodeStates['trg-1']).toBeUndefined()
  })

  it('reset() 清空 triggerNodeKey 與預標的 SKIPPED 狀態', () => {
    vi.mocked(execApi.executeWorkflow).mockReturnValue(() => {})
    const store = useExecutionStore()
    store.start('w1', { triggerNodeKey: 'trg-1', skipNodeKeys: ['trg-2'] })

    store.reset()

    expect(store.triggerNodeKey).toBeNull()
    expect(store.nodeStates).toEqual({})
  })

  it('再次 start 時前一輪的預標 SKIPPED 不殘留', () => {
    vi.mocked(execApi.executeWorkflow).mockReturnValue(() => {})
    const store = useExecutionStore()
    store.start('w1', { triggerNodeKey: 'trg-1', skipNodeKeys: ['trg-2'] })

    store.start('w1', { triggerNodeKey: 'trg-2', skipNodeKeys: ['trg-1'] })

    expect(store.nodeStates['trg-1'].status).toBe('SKIPPED')
    expect(store.nodeStates['trg-2']).toBeUndefined()
    expect(store.triggerNodeKey).toBe('trg-2')
  })
})
