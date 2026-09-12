import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useExecutionStore } from '../execution'

describe('execution store', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('node.started 設 RUNNING，node.completed 設 SUCCESS 與 output', () => {
    const store = useExecutionStore()
    store.applyEvent({ event: 'execution.started', executionId: 'e1', ts: 't' })
    store.applyEvent({ event: 'node.started', executionId: 'e1', nodeKey: 'n1', ts: 't' })
    expect(store.nodeStates['n1'].status).toBe('RUNNING')
    store.applyEvent({ event: 'node.completed', executionId: 'e1', nodeKey: 'n1', status: 'SUCCESS', output: { reply: 'hi' }, durationMs: 5, ts: 't' })
    expect(store.nodeStates['n1']).toMatchObject({ status: 'SUCCESS', output: { reply: 'hi' }, durationMs: 5 })
  })

  it('node.failed 記錯誤，execution.completed 記最終狀態與輸出', () => {
    const store = useExecutionStore()
    store.applyEvent({ event: 'node.failed', executionId: 'e1', nodeKey: 'n1', error: 'boom', ts: 't' })
    expect(store.nodeStates['n1']).toMatchObject({ status: 'FAILED', error: 'boom' })
    store.applyEvent({ event: 'execution.completed', executionId: 'e1', status: 'FAILED', error: 'boom', ts: 't' })
    expect(store.finalStatus).toBe('FAILED')
    expect(store.running).toBe(false)
  })

  it('reset 清空全部狀態', () => {
    const store = useExecutionStore()
    store.applyEvent({ event: 'node.started', executionId: 'e1', nodeKey: 'n1', ts: 't' })
    store.reset()
    expect(Object.keys(store.nodeStates)).toHaveLength(0)
    expect(store.executionId).toBeNull()
  })
})
