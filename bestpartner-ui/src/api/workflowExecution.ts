export interface ExecutionEvent {
  event: 'execution.started' | 'node.started' | 'node.completed' | 'node.failed' | 'execution.completed'
  executionId: string
  nodeKey?: string
  seqNo?: number
  status?: string
  output?: Record<string, unknown>
  error?: string
  durationMs?: number
  ts: string
}

/**
 * 取出後端錯誤回應的業務訊息（`ApiResponse.message`）。
 *
 * ⚠️ 這裡必須把 body 讀出來。execute 是**唯一走原生 fetch** 的 API——其餘端點都經
 * `api/http.ts` 的 axios 攔截器，錯誤訊息由 `extractApiMessage()` 取得。若此處只丟
 * `HTTP <status>`，使用者看到的就只有「HTTP 400」，而不是「提示詞節點 xxx 未連接到任何
 * LLM 助手節點」這種可據以修正的原因，與
 * `docs-site/docs/features/workflow.md` 揭示的「顯示後端業務訊息而非通用 HTTP 狀態字串」相違。
 *
 * body 不是 JSON、或 JSON 內無 message 時退回 `HTTP <status>`，確保一定有字可顯示。
 */
async function readErrorMessage(res: Response): Promise<string> {
  const fallback = `HTTP ${res.status}`
  try {
    const text = await res.text()
    if (!text) return fallback
    const parsed = JSON.parse(text) as { message?: unknown }
    return typeof parsed?.message === 'string' && parsed.message.trim()
      ? parsed.message.trim()
      : fallback
  } catch {
    // 非 JSON（如 HTML 錯誤頁）不直接顯示原文，避免把整頁塞進提示
    return fallback
  }
}

/**
 * `executeWorkflow` 的選用請求欄位。
 *
 * 刻意做成 options bag 而非再長一個位置參數：後端 `WorkflowDTO` 已支援但前端至今未送的
 * `inputPayload` 之後也應該放這裡，不必再改簽名。
 */
export interface ExecuteOptions {
  /**
   * 指定由哪個 TRIGGER 節點發起本次執行。
   * 帶值時後端僅活化該觸發點，其餘 TRIGGER 與其獨佔下游落 SKIPPED；
   * 省略＝後端執行所有 TRIGGER（向後相容行為）。
   */
  triggerNodeKey?: string
}

/**
 * 呼叫 POST /llm/workflow/execute（SSE 串流），逐事件回呼 onEvent；
 * 正常結束呼叫 onDone，發生非取消性錯誤呼叫 onError。
 * URL 使用相對路徑，與既有 axios（baseURL 未設定，走 Vite dev proxy `/llm` → localhost:80）一致。
 * 回傳 abort 函式，呼叫後中止串流（視為正常結束，觸發 onDone 而非 onError）。
 */
export function executeWorkflow(
  workflowId: string,
  onEvent: (e: ExecutionEvent) => void,
  onDone: () => void,
  onError: (err: unknown) => void,
  options: ExecuteOptions = {},
): () => void {
  const controller = new AbortController()
  const token = localStorage.getItem('token')

  void (async () => {
    try {
      const res = await fetch('/llm/workflow/execute', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        // 條件展開而非直接給 undefined：避免送出 "triggerNodeKey": null
        body: JSON.stringify({
          id: workflowId,
          ...(options.triggerNodeKey ? { triggerNodeKey: options.triggerNodeKey } : {}),
        }),
        signal: controller.signal,
      })
      if (!res.ok) throw new Error(await readErrorMessage(res))
      if (!res.body) throw new Error(`HTTP ${res.status}`)
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      for (;;) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''
        for (const line of lines) {
          const text = line.startsWith('data:') ? line.slice(5).trim() : line.trim()
          if (!text) continue
          onEvent(JSON.parse(text) as ExecutionEvent)
        }
      }
      onDone()
    } catch (err) {
      if ((err as Error).name !== 'AbortError') onError(err)
      else onDone()
    }
  })()

  return () => controller.abort()
}
