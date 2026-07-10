import { defineStore } from 'pinia'
import { ref } from 'vue'
import { executeWorkflow, type ExecutionEvent } from '../api/workflowExecution'

export type NodeRunStatus = 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'

export interface NodeRunState {
  status: NodeRunStatus
  output?: Record<string, unknown>
  error?: string
  durationMs?: number
}

export const useExecutionStore = defineStore('execution', () => {
  const running = ref(false)
  const executionId = ref<string | null>(null)
  const nodeStates = ref<Record<string, NodeRunState>>({})
  const finalStatus = ref<string | null>(null)
  const finalOutput = ref<Record<string, unknown> | null>(null)
  const errorMessage = ref<string | null>(null)
  let abortFn: (() => void) | null = null

  /** 清空所有執行狀態，回到初始值 */
  function reset(): void {
    running.value = false
    executionId.value = null
    nodeStates.value = {}
    finalStatus.value = null
    finalOutput.value = null
    errorMessage.value = null
  }

  /** 依 SSE 事件更新 store 狀態（純邏輯，供測試與 SSE 回呼共用） */
  function applyEvent(e: ExecutionEvent): void {
    executionId.value = e.executionId
    switch (e.event) {
      case 'execution.started':
        running.value = true
        break
      case 'node.started':
        if (e.nodeKey) nodeStates.value[e.nodeKey] = { status: 'RUNNING' }
        break
      case 'node.completed':
        if (e.nodeKey)
          nodeStates.value[e.nodeKey] = {
            status: 'SUCCESS',
            output: e.output,
            durationMs: e.durationMs,
          }
        break
      case 'node.failed':
        if (e.nodeKey) nodeStates.value[e.nodeKey] = { status: 'FAILED', error: e.error }
        break
      case 'execution.completed':
        running.value = false
        finalStatus.value = e.status ?? null
        finalOutput.value = e.output ?? null
        errorMessage.value = e.error ?? null
        break
    }
  }

  /** 開始執行 workflow：清空舊狀態並建立 SSE 連線 */
  function start(workflowId: string): void {
    reset()
    running.value = true
    abortFn = executeWorkflow(
      workflowId,
      applyEvent,
      () => {
        running.value = false
      },
      (err) => {
        running.value = false
        errorMessage.value = err instanceof Error ? err.message : String(err)
        finalStatus.value = finalStatus.value ?? 'FAILED'
      },
    )
  }

  /** 中止執行中的 SSE 連線 */
  function stop(): void {
    abortFn?.()
    abortFn = null
    running.value = false
    finalStatus.value = finalStatus.value ?? 'CANCELLED'
  }

  return { running, executionId, nodeStates, finalStatus, finalOutput, errorMessage, applyEvent, start, stop, reset }
})
