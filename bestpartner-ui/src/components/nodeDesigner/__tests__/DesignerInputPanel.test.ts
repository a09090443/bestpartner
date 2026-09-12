import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import DesignerInputPanel from '../DesignerInputPanel.vue'
import { IN_MAIN, IN_TOOL } from '../../../constants/handles'
import type { NodeType } from '../../../types/workflow'
import type { UpstreamSource } from '../../../types/nodeDesigner'

function mountPanel(type: NodeType, sources: UpstreamSource[] = []) {
  return mount(DesignerInputPanel, { props: { type, sources } })
}

describe('DesignerInputPanel 四種空狀態', () => {
  it('① TRIGGER 無輸入埠：說明它是流程起點', () => {
    const w = mountPanel('TRIGGER')
    const empty = w.find('[data-test="node-designer-input-empty"]')
    expect(empty.exists()).toBe(true)
    expect(empty.text()).toContain('觸發節點')
  })

  it('① SKILL 無輸入埠：另有能力提供節點的專屬說明', () => {
    const w = mountPanel('SKILL')
    expect(w.find('[data-test="node-designer-input-empty"]').text()).toContain('能力提供節點')
  })

  it('② 有輸入埠但沒有上游', () => {
    const w = mountPanel('TOOL')
    expect(w.find('[data-test="node-designer-input-empty"]').text()).toContain('尚未連接上游')
  })

  it('③ 有上游但尚未執行', () => {
    const w = mountPanel('TOOL', [
      { nodeId: 'n1', name: '開始', targetHandle: IN_MAIN, output: undefined },
    ])
    expect(w.find('[data-test="node-designer-input-empty"]').text()).toContain('尚未執行')
    expect(w.find('[data-test="node-designer-input-json"]').exists()).toBe(false)
  })

  it('④ 有輸出時顯示來源名稱與 JSON', () => {
    const w = mountPanel('TOOL', [
      { nodeId: 'n1', name: '開始', targetHandle: IN_MAIN, output: { foo: 'bar' } },
    ])
    expect(w.find('[data-test="node-designer-input-empty"]').exists()).toBe(false)
    expect(w.text()).toContain('來自 開始')
    expect(w.find('[data-test="node-designer-input-json"]').text()).toContain('"foo": "bar"')
  })
})

describe('DesignerInputPanel 能力掛載埠', () => {
  it('in:tool 來源歸入「已掛載能力」，只列節點名不顯示 JSON', () => {
    const w = mountPanel('LLM_ASSISTANT', [
      { nodeId: 's1', name: '天氣 Skill', targetHandle: IN_TOOL, output: { ignored: true } },
    ])
    const chips = w.find('[data-test="node-designer-capability-list"]')
    expect(chips.exists()).toBe(true)
    expect(chips.text()).toContain('天氣 Skill')
    expect(w.find('[data-test="node-designer-input-json"]').exists()).toBe(false)
  })

  it('資料輸入與能力來源同時存在時分開呈現', () => {
    const w = mountPanel('LLM_ASSISTANT', [
      { nodeId: 'p1', name: '提示詞', targetHandle: IN_MAIN, output: { prompt: '你好' } },
      { nodeId: 's1', name: '天氣 Skill', targetHandle: IN_TOOL },
    ])
    expect(w.find('[data-test="node-designer-input-json"]').text()).toContain('你好')
    expect(w.find('[data-test="node-designer-capability-list"]').text()).toContain('天氣 Skill')
  })

  it('只有能力來源時不顯示「尚未連接上游」', () => {
    const w = mountPanel('LLM_ASSISTANT', [
      { nodeId: 's1', name: '天氣 Skill', targetHandle: IN_TOOL },
    ])
    expect(w.find('[data-test="node-designer-input-empty"]').exists()).toBe(false)
  })
})
