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

describe('InspectorPanel 已選節點操作', () => {
  it('名稱輸入應 emit update:node-name', async () => {
    const w = mountWith('TOOL')
    await w.find('[data-test="node-name-input"]').setValue('新名稱')
    const emitted = w.emitted('update:node-name')
    expect(emitted).toBeTruthy()
    expect(emitted![emitted!.length - 1][0]).toBe('新名稱')
  })

  it('點 Duplicate 應 emit duplicate-node', async () => {
    const w = mountWith('TOOL')
    await w.find('[data-test="duplicate-node-button"]').trigger('click')
    expect(w.emitted('duplicate-node')).toBeTruthy()
  })

  it('點 Delete 應 emit delete-node', async () => {
    const w = mountWith('TOOL')
    await w.find('[data-test="delete-node-button"]').trigger('click')
    expect(w.emitted('delete-node')).toBeTruthy()
  })
})

describe('InspectorPanel 未選節點（Workflow overview）', () => {
  function mountOverview() {
    return mount(InspectorPanel, {
      props: {
        selectedNode: null,
        workflowName: '我的流程',
        workflowDescription: '說明',
        nodeCount: 5,
        connectionCount: 4,
        triggerCount: 1,
        workflowStatus: 'DRAFT' as const,
      },
    })
  }

  it('顯示 2×2 統計卡（Nodes / Connections / Triggers / Status）', () => {
    const w = mountOverview()
    expect(w.find('[data-test="stat-nodes"]').text()).toContain('5')
    expect(w.find('[data-test="stat-connections"]').text()).toContain('4')
    expect(w.find('[data-test="stat-triggers"]').text()).toContain('1')
    expect(w.find('[data-test="stat-status"]').text()).toContain('DRAFT')
  })

  it('顯示節點型別圖例（Trigger / Action / AI / Logic）', () => {
    const w = mountOverview()
    for (const category of ['Trigger', 'Action', 'AI', 'Logic']) {
      expect(w.find(`[data-test="legend-${category}"]`).exists()).toBe(true)
    }
  })

  it('流程名稱輸入應 emit update:workflow-name', async () => {
    const w = mountOverview()
    await w.find('[data-test="workflow-name-input"]').setValue('改名')
    const emitted = w.emitted('update:workflow-name')
    expect(emitted).toBeTruthy()
    expect(emitted![emitted!.length - 1][0]).toBe('改名')
  })

  it('流程描述輸入應 emit update:workflow-description', async () => {
    const w = mountOverview()
    await w.find('[data-test="workflow-description-input"]').setValue('新描述')
    const emitted = w.emitted('update:workflow-description')
    expect(emitted).toBeTruthy()
    expect(emitted![emitted!.length - 1][0]).toBe('新描述')
  })
})
