import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { executeWorkflow } from '../workflowExecution'
import type { ExecutionEvent } from '../workflowExecution'

/** 把字串切成串流，模擬 SSE 逐塊送達 */
function streamOf(chunks: string[]): ReadableStream<Uint8Array> {
  const enc = new TextEncoder()
  return new ReadableStream({
    start(controller) {
      for (const c of chunks) controller.enqueue(enc.encode(c))
      controller.close()
    },
  })
}

/** 等待 executeWorkflow 內部的非同步流程跑完（它不回傳 Promise，只走回呼） */
const settle = () => new Promise((r) => setTimeout(r, 0))

const originalFetch = globalThis.fetch

beforeEach(() => {
  localStorage.setItem('token', 'test-token')
})
afterEach(() => {
  globalThis.fetch = originalFetch
  localStorage.clear()
  vi.restoreAllMocks()
})

describe('executeWorkflow 錯誤訊息', () => {
  /**
   * 迴歸（OBS-2）：execute 是唯一走原生 fetch 的 API，沒有 axios 攔截器幫忙取訊息。
   * 先前直接丟 `HTTP <status>`，使用者只看得到「HTTP 400」，看不到真正原因。
   */
  it('後端回 4xx 時，以 ApiResponse.message 作為錯誤訊息', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({ code: 400, message: '提示詞節點 abc123 未連接到任何 LLM 助手節點', data: null }),
        { status: 400 },
      ),
    )
    const onError = vi.fn()
    executeWorkflow('wf-1', vi.fn(), vi.fn(), onError)
    await settle()

    expect(onError).toHaveBeenCalledTimes(1)
    expect((onError.mock.calls[0][0] as Error).message).toBe('提示詞節點 abc123 未連接到任何 LLM 助手節點')
  })

  it('body 非 JSON 時退回 HTTP <status>，不把原文整段塞進提示', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response('<html><body>Gateway Error</body></html>', { status: 502 }),
    )
    const onError = vi.fn()
    executeWorkflow('wf-1', vi.fn(), vi.fn(), onError)
    await settle()

    expect((onError.mock.calls[0][0] as Error).message).toBe('HTTP 502')
  })

  it('JSON 內無 message（或為空字串）時退回 HTTP <status>', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 500, message: '   ', data: null }), { status: 500 }),
    )
    const onError = vi.fn()
    executeWorkflow('wf-1', vi.fn(), vi.fn(), onError)
    await settle()

    expect((onError.mock.calls[0][0] as Error).message).toBe('HTTP 500')
  })
})

describe('executeWorkflow 正常串流', () => {
  it('逐行解析 SSE 事件並於結束時呼叫 onDone', async () => {
    const events: ExecutionEvent[] = []
    const body = streamOf([
      'data: {"event":"execution.started","executionId":"e1","ts":"t"}\n',
      'data: {"event":"node.completed","executionId":"e1","nodeKey":"n1","ts":"t"}\n',
      'data: {"event":"execution.completed","executionId":"e1","status":"SUCCESS","ts":"t"}\n',
    ])
    globalThis.fetch = vi.fn().mockResolvedValue(new Response(body, { status: 200 }))
    const onDone = vi.fn()
    const onError = vi.fn()

    executeWorkflow('wf-1', (e) => events.push(e), onDone, onError)
    await settle()

    expect(events.map((e) => e.event)).toEqual([
      'execution.started',
      'node.completed',
      'execution.completed',
    ])
    expect(onDone).toHaveBeenCalledTimes(1)
    expect(onError).not.toHaveBeenCalled()
  })

  it('附帶 Authorization header', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(streamOf([]), { status: 200 }))
    globalThis.fetch = fetchMock
    executeWorkflow('wf-1', vi.fn(), vi.fn(), vi.fn())
    await settle()

    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer test-token')
  })
})

describe('executeWorkflow 指定觸發點', () => {
  /** 取出送出的 request body（已 JSON.parse） */
  async function bodyOf(options?: { triggerNodeKey?: string }): Promise<Record<string, unknown>> {
    const fetchMock = vi.fn().mockResolvedValue(new Response(streamOf([]), { status: 200 }))
    globalThis.fetch = fetchMock
    executeWorkflow('wf-1', vi.fn(), vi.fn(), vi.fn(), options)
    await settle()
    const init = fetchMock.mock.calls[0][1] as RequestInit
    return JSON.parse(init.body as string) as Record<string, unknown>
  }

  it('帶 triggerNodeKey 時放入 request body', async () => {
    const body = await bodyOf({ triggerNodeKey: 'trg-1' })
    expect(body.triggerNodeKey).toBe('trg-1')
    expect(body.id).toBe('wf-1')
  })

  it('未帶 triggerNodeKey 時 body 完全不含該鍵（避免送出 null）', async () => {
    const body = await bodyOf()
    expect('triggerNodeKey' in body).toBe(false)
    expect(body.id).toBe('wf-1')
  })

  it('triggerNodeKey 為空字串時視同未指定，不放入 body', async () => {
    const body = await bodyOf({ triggerNodeKey: '' })
    expect('triggerNodeKey' in body).toBe(false)
  })
})
