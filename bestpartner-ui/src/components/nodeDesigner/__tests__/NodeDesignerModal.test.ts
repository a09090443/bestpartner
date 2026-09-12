import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// 型別化表單會呼叫 useNodeOptions，mock 掉避免真實網路
vi.mock('../../../composables/useNodeOptions', () => ({
  useNodeOptions: () => ({
    loadLlmOptions: vi.fn().mockResolvedValue([]),
    loadToolOptions: vi.fn().mockResolvedValue([]),
    loadMcpOptions: vi.fn().mockResolvedValue([]),
    loadSkillOptions: vi.fn().mockResolvedValue([]),
  }),
}))

import NodeDesignerModal from '../NodeDesignerModal.vue'
import type { NodeType } from '../../../types/workflow'

beforeEach(() => setActivePinia(createPinia()))

function mountModal(type: NodeType = 'TOOL', overrides: Record<string, unknown> = {}) {
  return mount(NodeDesignerModal, {
    props: {
      nodeId: 'n1',
      type,
      name: '我的節點',
      config: {},
      sources: [],
      ...overrides,
    },
    attachTo: document.body,
  })
}

describe('NodeDesignerModal', () => {
  it('渲染 header（型別、分類、名稱）與三欄面板', () => {
    const w = mountModal()
    expect(w.find('[data-test="node-designer-modal"]').exists()).toBe(true)
    expect(w.find('[data-test="node-designer-input-panel"]').exists()).toBe(true)
    expect(w.find('[data-test="node-designer-output-panel"]').exists()).toBe(true)
    expect(w.find('[data-test="node-designer-parameters"]').exists()).toBe(true)
    expect(
      (w.find('[data-test="node-designer-name-input"]').element as HTMLInputElement).value,
    ).toBe('我的節點')
  })

  it('點關閉鈕 emit close', async () => {
    const w = mountModal()
    await w.find('[data-test="node-designer-close"]').trigger('click')
    expect(w.emitted('close')).toBeTruthy()
  })

  it('點遮罩 emit close，點卡片內部不會', async () => {
    const w = mountModal()
    await w.find('.designer-card').trigger('click')
    expect(w.emitted('close')).toBeFalsy()

    await w.find('[data-test="node-designer-modal"]').trigger('click')
    expect(w.emitted('close')).toBeTruthy()
  })

  it('按 Esc emit close，卸載後不再回應', async () => {
    const w = mountModal()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    // 卸載後 wrapper.emitted() 會失效，先取住同一個陣列參考再驗證沒有新事件
    const emitted = w.emitted('close')!
    expect(emitted).toHaveLength(1)

    w.unmount()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(emitted).toHaveLength(1)
  })

  it('改名稱 emit update:node-name', async () => {
    const w = mountModal()
    await w.find('[data-test="node-designer-name-input"]').setValue('改過的名稱')
    const emitted = w.emitted('update:node-name')
    expect(emitted![emitted!.length - 1][0]).toBe('改過的名稱')
  })

  it('切換到 Settings 分頁顯示唯讀 metadata 與節點操作', async () => {
    const w = mountModal()
    await w.find('[data-test="node-designer-tab-settings"]').trigger('click')

    const settings = w.find('[data-test="node-designer-settings"]')
    expect(settings.text()).toContain('n1')
    expect(settings.text()).toContain('TOOL')

    await w.find('[data-test="node-designer-delete"]').trigger('click')
    expect(w.emitted('delete-node')).toBeTruthy()

    await w.find('[data-test="node-designer-duplicate"]').trigger('click')
    expect(w.emitted('duplicate-node')).toBeTruthy()
  })

  it('切換到 Docs 分頁顯示說明與自動渲染的連接埠清單', async () => {
    const w = mountModal('CONDITION')
    await w.find('[data-test="node-designer-tab-docs"]').trigger('click')

    const docs = w.find('[data-test="node-designer-docs"]')
    // 說明取自 constants/nodeDocs.ts
    expect(docs.text()).toContain('條件')
    // 連接埠直接讀 nodeTypes.ts 的 meta，CONDITION 有 True / False 兩個出埠
    expect(docs.text()).toContain('out:true')
    expect(docs.text()).toContain('out:false')
  })

  it('13 種型別皆可掛載且 Docs 分頁有內容', async () => {
    const types: NodeType[] = [
      'TRIGGER',
      'LLM_ASSISTANT',
      'PROMPT',
      'TOOL',
      'MCP_SERVER',
      'SKILL',
      'KNOWLEDGE_RAG',
      'CONDITION',
      'LOOP',
      'CODE',
      'HTTP_REQUEST',
      'DATA_TRANSFORM',
      'OUTPUT',
    ]
    for (const type of types) {
      const w = mountModal(type)
      await w.find('[data-test="node-designer-tab-docs"]').trigger('click')
      expect(w.find('[data-test="node-designer-docs"]').text().length).toBeGreaterThan(10)
      w.unmount()
    }
  })
})
