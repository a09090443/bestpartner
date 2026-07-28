import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// 表單掛載會呼叫 useNodeOptions，mock 掉避免真實網路
vi.mock('../../../composables/useNodeOptions', () => ({
  useNodeOptions: () => ({
    loadLlmOptions: vi.fn().mockResolvedValue([]),
    loadToolOptions: vi.fn().mockResolvedValue([]),
    loadMcpOptions: vi.fn().mockResolvedValue([]),
    loadSkillOptions: vi.fn().mockResolvedValue([]),
  }),
}))

import InspectorPanel from '../InspectorPanel.vue'
import LlmAssistantForm from '../forms/LlmAssistantForm.vue'
import ToolForm from '../forms/ToolForm.vue'
import McpServerForm from '../forms/McpServerForm.vue'
import SkillForm from '../forms/SkillForm.vue'
import KnowledgeRagForm from '../forms/KnowledgeRagForm.vue'
import PromptForm from '../forms/PromptForm.vue'
import JsonConfigEditor from '../JsonConfigEditor.vue'
import type { FlowNode } from '../../../composables/useWorkflowSync'
import type { NodeType } from '../../../types/workflow'
import type { NodeRequiredFields } from '../../../api/workflow'

// InspectorPanel 內用 useExecutionStore 讀取節點執行狀態，需先啟用 Pinia
beforeEach(() => setActivePinia(createPinia()))

function nodeOf(type: NodeType, config: Record<string, unknown> = {}): FlowNode {
  return { id: 'n1', type: 'workflow', position: { x: 0, y: 0 }, data: { type, config } }
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

  it('SKILL 渲染 SkillForm', () => {
    expect(mountWith('SKILL').findComponent(SkillForm).exists()).toBe(true)
  })

  it('KNOWLEDGE_RAG 渲染 KnowledgeRagForm', () => {
    expect(mountWith('KNOWLEDGE_RAG').findComponent(KnowledgeRagForm).exists()).toBe(true)
  })

  it('PROMPT 渲染 PromptForm 而非 JSON 編輯器', () => {
    const w = mountWith('PROMPT')
    expect(w.findComponent(PromptForm).exists()).toBe(true)
    expect(w.findComponent(JsonConfigEditor).exists()).toBe(false)
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

describe('InspectorPanel 必填欄位缺漏提示', () => {
  const requiredFields: NodeRequiredFields = { LLM_ASSISTANT: ['llmId'] }

  it('未提供 requiredFields 時不顯示提示', () => {
    const w = mountWith('LLM_ASSISTANT')
    expect(w.find('[data-test="required-field-warning"]').exists()).toBe(false)
  })

  it('提供 requiredFields 且缺漏必填欄位時顯示提示（欄位鍵中文化）', () => {
    const w = mount(InspectorPanel, {
      props: { selectedNode: nodeOf('LLM_ASSISTANT'), workflowName: 'wf', requiredFields },
    })
    const warning = w.find('[data-test="required-field-warning"]')
    expect(warning.exists()).toBe(true)
    // llmId → 'LLM 設定'，不應顯示原始英文鍵名
    expect(warning.text()).toContain('LLM 設定')
    expect(warning.text()).not.toContain('llmId')
  })

  it('複合字樣缺漏欄位顯示為「A 或 B（擇一）」而非原始管線字樣', () => {
    const dataTransformRequired: NodeRequiredFields = { DATA_TRANSFORM: ['mappings|template'] }
    const w = mount(InspectorPanel, {
      props: {
        selectedNode: nodeOf('DATA_TRANSFORM'),
        workflowName: 'wf',
        requiredFields: dataTransformRequired,
      },
    })
    const warning = w.find('[data-test="required-field-warning"]')
    expect(warning.text()).toContain('欄位對應 或 範本（擇一）')
    expect(warning.text()).not.toContain('|')
  })

  it('config 已齊全必填欄位時不顯示提示', () => {
    const w = mount(InspectorPanel, {
      props: {
        selectedNode: nodeOf('LLM_ASSISTANT', { llmId: 'l1' }),
        workflowName: 'wf',
        requiredFields,
      },
    })
    expect(w.find('[data-test="required-field-warning"]').exists()).toBe(false)
  })

  it('workflowStatus 為 ACTIVE 時提示採 error 語意（紅色）', () => {
    const w = mount(InspectorPanel, {
      props: {
        selectedNode: nodeOf('LLM_ASSISTANT'),
        workflowName: 'wf',
        requiredFields,
        workflowStatus: 'ACTIVE',
      },
    })
    const warning = w.find('[data-test="required-field-warning"]')
    expect(warning.attributes('data-severity')).toBe('error')
    expect(warning.classes()).toContain('required-warning--error')
  })

  it('workflowStatus 為 DRAFT/未提供時提示採 warning 語意（橘色）', () => {
    const w = mount(InspectorPanel, {
      props: {
        selectedNode: nodeOf('LLM_ASSISTANT'),
        workflowName: 'wf',
        requiredFields,
        workflowStatus: 'DRAFT',
      },
    })
    const warning = w.find('[data-test="required-field-warning"]')
    expect(warning.attributes('data-severity')).toBe('warning')
    expect(warning.classes()).toContain('required-warning--warning')
  })
})

describe('InspectorPanel 本次執行區塊', () => {
  it('未有執行狀態時不顯示本次執行區塊', () => {
    const w = mountWith('TOOL')
    expect(w.find('[data-test="node-exec-section"]').exists()).toBe(false)
  })

  it('nodeStates 有該節點資料時顯示狀態與輸出', async () => {
    const { useExecutionStore } = await import('../../../stores/execution')
    const store = useExecutionStore()
    store.applyEvent({
      event: 'node.completed',
      executionId: 'e1',
      nodeKey: 'n1',
      output: { result: 'ok' },
      durationMs: 12,
      ts: 't',
    })
    const w = mountWith('TOOL')
    const section = w.find('[data-test="node-exec-section"]')
    expect(section.exists()).toBe(true)
    expect(section.text()).toContain('SUCCESS')
    expect(section.text()).toContain('12')
    expect(section.text()).toContain('ok')
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

describe('InspectorPanel — 提示來源標示（promptBoundNodeKeys）', () => {
  function mountLlmWith(promptBoundNodeKeys?: string[]) {
    return mount(InspectorPanel, {
      props: {
        selectedNode: nodeOf('LLM_ASSISTANT', { llmId: 'l1' }),
        workflowName: 'wf',
        promptBoundNodeKeys,
      },
    })
  }

  it('選中的 LLM 節點在 promptBoundNodeKeys 內時傳入 hasUpstreamPrompt 並顯示標記', () => {
    const w = mountLlmWith(['n1'])
    expect(w.findComponent(LlmAssistantForm).props('hasUpstreamPrompt')).toBe(true)
    expect(w.find('[data-test="prompt-overridden-badge"]').exists()).toBe(true)
    expect(w.find('[data-test="prompt-source-hint"]').exists()).toBe(true)
  })

  it('未在 promptBoundNodeKeys 內時不顯示標記', () => {
    const w = mountLlmWith(['other'])
    expect(w.findComponent(LlmAssistantForm).props('hasUpstreamPrompt')).toBe(false)
    expect(w.find('[data-test="prompt-overridden-badge"]').exists()).toBe(false)
  })

  it('未傳 promptBoundNodeKeys 時視為未連接', () => {
    const w = mountLlmWith()
    expect(w.findComponent(LlmAssistantForm).props('hasUpstreamPrompt')).toBe(false)
  })

  it('非 LLM 型別的表單不會收到 hasUpstreamPrompt', () => {
    const w = mount(InspectorPanel, {
      props: {
        selectedNode: nodeOf('PROMPT', { prompt: '提問' }),
        workflowName: 'wf',
        promptBoundNodeKeys: ['n1'],
      },
    })
    // PromptForm 未宣告此 prop；若誤傳會變成落在根元素的 fallthrough attribute
    expect(w.findComponent(PromptForm).attributes('hasupstreamprompt')).toBeUndefined()
  })
})
