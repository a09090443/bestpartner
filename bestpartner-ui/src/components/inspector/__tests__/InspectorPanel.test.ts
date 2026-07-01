import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'

// 表單掛載會呼叫 useNodeOptions，mock 掉避免真實網路
vi.mock('../../../composables/useNodeOptions', () => ({
  useNodeOptions: () => ({
    loadLlmOptions: vi.fn().mockResolvedValue([]),
    loadToolOptions: vi.fn().mockResolvedValue([]),
    loadMcpOptions: vi.fn().mockResolvedValue([]),
  }),
}))

import InspectorPanel from '../InspectorPanel.vue'
import LlmAssistantForm from '../forms/LlmAssistantForm.vue'
import ToolForm from '../forms/ToolForm.vue'
import McpServerForm from '../forms/McpServerForm.vue'
import KnowledgeRagForm from '../forms/KnowledgeRagForm.vue'
import JsonConfigEditor from '../JsonConfigEditor.vue'
import type { FlowNode } from '../../../composables/useWorkflowSync'
import type { NodeType } from '../../../types/workflow'

function nodeOf(type: NodeType): FlowNode {
  return { id: 'n1', type: 'workflow', position: { x: 0, y: 0 }, data: { type, config: {} } }
}

function mountWith(type: NodeType) {
  return mount(InspectorPanel, {
    props: { selectedNode: nodeOf(type), workflowName: 'wf' },
  })
}

describe('InspectorPanel 型別分派', () => {
  it('LLM_ASSISTANT 渲染 LlmAssistantForm', () => {
    const w = mountWith('LLM_ASSISTANT')
    expect(w.findComponent(LlmAssistantForm).exists()).toBe(true)
    expect(w.findComponent(JsonConfigEditor).exists()).toBe(false)
  })

  it('TOOL 渲染 ToolForm', () => {
    expect(mountWith('TOOL').findComponent(ToolForm).exists()).toBe(true)
  })

  it('MCP_SERVER 渲染 McpServerForm', () => {
    expect(mountWith('MCP_SERVER').findComponent(McpServerForm).exists()).toBe(true)
  })

  it('KNOWLEDGE_RAG 渲染 KnowledgeRagForm', () => {
    expect(mountWith('KNOWLEDGE_RAG').findComponent(KnowledgeRagForm).exists()).toBe(true)
  })

  it('其餘型別（CODE）維持 JsonConfigEditor fallback', () => {
    const w = mountWith('CODE')
    expect(w.findComponent(JsonConfigEditor).exists()).toBe(true)
    expect(w.findComponent(LlmAssistantForm).exists()).toBe(false)
  })

  it('表單 update:config 應再 emit update:node-config', async () => {
    const w = mountWith('TOOL')
    w.findComponent(ToolForm).vm.$emit('update:config', { toolId: 't1' })
    await w.vm.$nextTick()
    const emitted = w.emitted('update:node-config')
    expect(emitted).toBeTruthy()
    expect(emitted![emitted!.length - 1][0]).toEqual({ toolId: 't1' })
  })

  it('選中型別化節點時 emit config-validity=true', () => {
    const w = mountWith('LLM_ASSISTANT')
    const emitted = w.emitted('config-validity')
    expect(emitted).toBeTruthy()
    expect(emitted![emitted!.length - 1][0]).toBe(true)
  })
})
