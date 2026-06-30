import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import WorkflowNode from '../WorkflowNode.vue'

// Vue Flow 的 Handle 需 VueFlow context，單元測試以 stub 取代
const globalConfig = {
  stubs: {
    Handle: { template: '<div class="handle-stub" />' },
  },
}

describe('WorkflowNode', () => {
  it('應渲染節點名稱', () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { name: '我的助手', type: 'LLM_ASSISTANT' } },
      global: globalConfig,
    })
    expect(wrapper.text()).toContain('我的助手')
  })

  it('應渲染型別標籤（label）', () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { name: '我的助手', type: 'LLM_ASSISTANT' } },
      global: globalConfig,
    })
    expect(wrapper.text()).toContain('LLM 助手')
  })

  it('無 name 時以型別 label 作為顯示名稱', () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'TRIGGER' } },
      global: globalConfig,
    })
    expect(wrapper.text()).toContain('觸發')
  })
})
