import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import DesignerOutputPanel from '../DesignerOutputPanel.vue'
import type { NodeType } from '../../../types/workflow'
import type { NodeRunState } from '../../../stores/execution'

function mountPanel(type: NodeType, run?: NodeRunState) {
  return mount(DesignerOutputPanel, { props: { nodeId: 'n1', type, run } })
}

describe('DesignerOutputPanel', () => {
  it('OUTPUT（無輸出埠）說明它是流程終點', () => {
    const w = mountPanel('OUTPUT')
    expect(w.find('[data-test="node-designer-output-empty"]').text()).toContain('流程的終點')
  })

  /**
   * 迴歸：SKILL 有 out:main 埠（掛載到 LLM in:tool 用），只看埠數會誤判成
   * 「可執行、只是還沒跑」，錯誤顯示「執行流程後可在此檢視實際輸出資料」。
   * 後端沒有 SkillExecutor，此節點永遠不會有輸出。（E2E J9-15 抓到）
   */
  it('SKILL 雖有輸出埠，仍須說明它不會產生自己的輸出', () => {
    const w = mountPanel('SKILL')
    const text = w.find('[data-test="node-designer-output-empty"]').text()
    expect(text).toContain('不會產生自己的輸出')
    expect(text).not.toContain('尚未執行')
  })

  it('一般節點未執行時顯示「尚未執行」', () => {
    const w = mountPanel('TOOL')
    expect(w.find('[data-test="node-designer-output-empty"]').text()).toContain('尚未執行')
  })

  it('有輸出時顯示可引用欄位與原始 JSON', () => {
    const w = mountPanel('TOOL', { status: 'SUCCESS', output: { result: 'ok' }, durationMs: 12 })
    expect(w.find('[data-test="node-designer-output-empty"]').exists()).toBe(false)
    expect(w.find('[data-test="node-designer-output-json"]').text()).toContain('"result": "ok"')
    expect(w.find('[data-test="node-designer-output-preview"]').text()).toContain('{{n1.result}}')
    expect(w.find('[data-test="node-designer-output-status"]').text()).toContain('SUCCESS')
  })

  it('執行失敗時顯示錯誤訊息', () => {
    const w = mountPanel('TOOL', { status: 'FAILED', error: '工具呼叫逾時' })
    expect(w.find('[data-test="node-designer-output-error"]').text()).toContain('工具呼叫逾時')
  })
})
