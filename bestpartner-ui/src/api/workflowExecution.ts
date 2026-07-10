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
        body: JSON.stringify({ id: workflowId }),
        signal: controller.signal,
      })
      if (!res.ok || !res.body) throw new Error(`HTTP ${res.status}`)
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
