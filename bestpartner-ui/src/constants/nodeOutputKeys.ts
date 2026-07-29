/**
 * 各節點型別「未指定 outputKey 時」的預設輸出鍵名。
 *
 * ⚠️ 值全部對應後端 executor 的實作，**不可憑印象填**。逐項出處：
 *   LLM_ASSISTANT  reply      LlmAssistantExecutor:111  `cfg.outputKey ?: "reply"`
 *   PROMPT         prompt     PromptExecutor:37         `DEFAULT_OUTPUT_KEY = "prompt"`
 *   TOOL           result     ToolNodeExecutor:63       `cfg.outputKey ?: "result"`
 *   MCP_SERVER     result     McpServerNodeExecutor:42  `cfg.outputKey ?: "result"`
 *   KNOWLEDGE_RAG  documents  KnowledgeRagExecutor:48   `cfg.outputKey ?: "documents"`
 *   CODE           result     CodeExecutor:35           `DEFAULT_OUTPUT_KEY = "result"`
 *   HTTP_REQUEST   response   HttpRequestExecutor:38    `cfg.outputKey ?: "response"`
 *   DATA_TRANSFORM result     DataTransformExecutor:25  `DEFAULT_OUTPUT_KEY = "result"`
 *   CONDITION      result / branch                      ConditionExecutor 輸出兩者（無 outputKey 設定）
 *
 * 未列入者：
 *   TRIGGER —— 輸出即啟動 payload 本身，鍵由執行時傳入的資料決定，無法預先得知
 *   SKILL / OUTPUT —— 不產生可供下游引用的輸出
 *
 * 改動後端 executor 的預設鍵名時，此表須同步（與 nodeDocs.ts 同屬「手抄後端行為」的檔案）。
 */
import type { NodeType } from '../types/workflow'

export const DEFAULT_OUTPUT_KEYS: Partial<Record<NodeType, string[]>> = {
  LLM_ASSISTANT: ['reply'],
  PROMPT: ['prompt'],
  TOOL: ['result'],
  MCP_SERVER: ['result'],
  KNOWLEDGE_RAG: ['documents'],
  CODE: ['result'],
  HTTP_REQUEST: ['response'],
  DATA_TRANSFORM: ['result'],
  CONDITION: ['result', 'branch'],
}

/** 可供下游以 `{{nodeKey.欄位}}` 引用的一組建議 */
export interface UpstreamRef {
  nodeId: string
  /** 節點顯示名稱，供使用者辨識 */
  name: string
  /** 建議的插值字串，如 `{{abc123.reply}}` */
  refs: string[]
}

/**
 * 組出某節點的可引用欄位建議。
 * 優先序：實際執行輸出的鍵 > 使用者自訂的 outputKey > 型別預設鍵。
 * 三者皆無（如未執行過的 TRIGGER）時回傳空陣列，呼叫端據以略過該節點。
 */
export function buildUpstreamRef(options: {
  nodeId: string
  name: string
  type: NodeType
  config?: Record<string, unknown>
  /** 最近一次執行的輸出；有的話最準確 */
  executedOutput?: Record<string, unknown>
}): UpstreamRef {
  const { nodeId, name, type, config, executedOutput } = options

  const keys = (() => {
    if (executedOutput && Object.keys(executedOutput).length > 0) return Object.keys(executedOutput)
    const custom = config?.outputKey
    if (typeof custom === 'string' && custom.trim()) return [custom.trim()]
    return DEFAULT_OUTPUT_KEYS[type] ?? []
  })()

  return { nodeId, name, refs: keys.map((k) => `{{${nodeId}.${k}}}`) }
}
