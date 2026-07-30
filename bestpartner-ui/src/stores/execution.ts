import { defineStore } from 'pinia'
import { ref } from 'vue'
import { executeWorkflow, type ExecutionEvent } from '../api/workflowExecution'

export type NodeRunStatus = 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED' | 'CANCELLED'

export interface NodeRunState {
  status: NodeRunStatus
  output?: Record<string, unknown>
  error?: string
  durationMs?: number
}

/** `start()` 的選用參數 */
export interface StartOptions {
  /** 指定由哪個 TRIGGER 節點發起（透傳給後端 execute） */
  triggerNodeKey?: string
  /**
   * 開跑前即標為 SKIPPED 的節點鍵——用於未被選中的觸發點。
   *
   * ⚠️ 後端對 SKIPPED 節點**刻意不發 SSE 事件**，因此這些節點在畫布上原本會完全無狀態，
   * 使用者分不清「刻意沒跑」與「還沒跑到」。呼叫端只應傳入「與引擎規則 1:1 對應、
   * 不含預測成分」的集合（即未選中的 TRIGGER 本身），不要自行 BFS 推算下游——
   * 引擎會執行 indegree 0 的孤立節點，預測會與實際不符而造成狀態閃爍。
   */
  skipNodeKeys?: string[]
}

export const useExecutionStore = defineStore('execution', () => {
  const running = ref(false)
  const executionId = ref<string | null>(null)
  const nodeStates = ref<Record<string, NodeRunState>>({})
  const finalStatus = ref<string | null>(null)
  const finalOutput = ref<Record<string, unknown> | null>(null)
  const errorMessage = ref<string | null>(null)
  /** 本次執行由哪個 TRIGGER 節點發起；null = 未指定（所有觸發點皆執行） */
  const triggerNodeKey = ref<string | null>(null)
  let abortFn: (() => void) | null = null
  // 本次執行是否已收到 execution.completed 事件；用於區分「正常結束」與「SSE 中斷」
  let completedReceived = false

  /** 清空所有執行狀態，回到初始值 */
  function reset(): void {
    running.value = false
    executionId.value = null
    nodeStates.value = {}
    finalStatus.value = null
    finalOutput.value = null
    errorMessage.value = null
    triggerNodeKey.value = null
    completedReceived = false
  }

  /** 將所有仍為 RUNNING 的節點轉為終止態 CANCELLED（停止/取消時使用）；已終止者不動 */
  function cancelRunningNodes(): void {
    for (const key of Object.keys(nodeStates.value)) {
      const state = nodeStates.value[key]
      if (state.status === 'RUNNING') {
        nodeStates.value[key] = { ...state, status: 'CANCELLED' }
      }
    }
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
        completedReceived = true
        finalStatus.value = e.status ?? null
        finalOutput.value = e.output ?? null
        errorMessage.value = e.error ?? null
        // 取消結束：仍在跑的節點補上終止態視覺
        if (e.status === 'CANCELLED') cancelRunningNodes()
        break
    }
  }

  /** 開始執行 workflow：清空舊狀態並建立 SSE 連線 */
  function start(workflowId: string, options: StartOptions = {}): void {
    reset()
    running.value = true
    triggerNodeKey.value = options.triggerNodeKey ?? null
    for (const key of options.skipNodeKeys ?? []) {
      nodeStates.value[key] = { status: 'SKIPPED' }
    }
    abortFn = executeWorkflow(
      workflowId,
      applyEvent,
      () => {
        running.value = false
        // SSE 正常關閉但未收到 execution.completed（或為取消）→ 殘留 RUNNING 節點視為取消
        if (!completedReceived || finalStatus.value === 'CANCELLED') cancelRunningNodes()
      },
      (err) => {
        running.value = false
        errorMessage.value = err instanceof Error ? err.message : String(err)
        finalStatus.value = finalStatus.value ?? 'FAILED'
        if (!completedReceived || finalStatus.value === 'CANCELLED') cancelRunningNodes()
      },
      { triggerNodeKey: options.triggerNodeKey },
    )
  }

  /** 中止執行中的 SSE 連線；使用者主動停止，殘留 RUNNING 節點轉為 CANCELLED */
  function stop(): void {
    abortFn?.()
    abortFn = null
    running.value = false
    finalStatus.value = finalStatus.value ?? 'CANCELLED'
    cancelRunningNodes()
  }

  return {
    running,
    executionId,
    nodeStates,
    finalStatus,
    finalOutput,
    errorMessage,
    triggerNodeKey,
    applyEvent,
    start,
    stop,
    reset,
  }
})
