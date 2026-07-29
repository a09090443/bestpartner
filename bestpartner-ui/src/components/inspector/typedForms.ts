/**
 * NodeType → 型別化設定表單的分派表。
 *
 * 由 InspectorPanel 與 Node Designer 的 Parameters 面板共用：新增型別化表單只改這裡一處，
 * 也避免 hasUpstreamPrompt 這類特例邏輯出現第二份實作。
 * 表未涵蓋的型別由呼叫端退回 JsonConfigEditor。
 */
import { markRaw } from 'vue'
import TriggerForm from './forms/TriggerForm.vue'
import LlmAssistantForm from './forms/LlmAssistantForm.vue'
import PromptForm from './forms/PromptForm.vue'
import ToolForm from './forms/ToolForm.vue'
import McpServerForm from './forms/McpServerForm.vue'
import SkillForm from './forms/SkillForm.vue'
import KnowledgeRagForm from './forms/KnowledgeRagForm.vue'
import OutputForm from './forms/OutputForm.vue'
import type { NodeType } from '../../types/workflow'
import type { UpstreamRef } from '../../constants/nodeOutputKeys'

const TYPED_FORMS = markRaw({
  TRIGGER: TriggerForm,
  LLM_ASSISTANT: LlmAssistantForm,
  PROMPT: PromptForm,
  TOOL: ToolForm,
  MCP_SERVER: McpServerForm,
  SKILL: SkillForm,
  KNOWLEDGE_RAG: KnowledgeRagForm,
  OUTPUT: OutputForm,
})

/** 取得該型別對應的表單元件；無對應者回傳 null（呼叫端改用 JsonConfigEditor） */
export function resolveTypedForm(type?: NodeType) {
  return (type && TYPED_FORMS[type as keyof typeof TYPED_FORMS]) || null
}

/**
 * 組出要傳給型別化表單的 props。
 *
 * ⚠️ 額外的 prop 一律只加給宣告了它的表單——其他表單未宣告，
 * 硬傳會變成落在根元素上的 fallthrough attribute。
 */
export function buildTypedFormProps(options: {
  type?: NodeType
  nodeId?: string
  config?: Record<string, unknown>
  promptBoundNodeKeys?: string[]
  /** 可引用的上游輸出建議，只有 OutputForm 需要 */
  upstreamRefs?: UpstreamRef[]
  /** nodeKey → 顯示名稱，供 OutputForm 的運算式編輯器把識別碼渲染成可讀名稱 */
  refLabels?: Record<string, string>
}): Record<string, unknown> {
  const base: Record<string, unknown> = { config: options.config ?? {} }
  if (options.type === 'LLM_ASSISTANT') {
    base.hasUpstreamPrompt = (options.promptBoundNodeKeys ?? []).includes(options.nodeId ?? '')
  }
  if (options.type === 'OUTPUT') {
    base.upstreamRefs = options.upstreamRefs ?? []
    base.refLabels = options.refLabels ?? {}
  }
  return base
}
