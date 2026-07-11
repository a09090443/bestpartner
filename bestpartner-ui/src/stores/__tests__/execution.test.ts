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
