import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import WorkflowNode from '../WorkflowNode.vue'

// Vue Flow 的 Handle 需 VueFlow context，單元測試以 stub 取代。
// stub 暴露 id/type 為 data 屬性，供斷言埠數量與 handle id。
const globalConfig = {
  stubs: {
    Handle: {
      props: ['id', 'type', 'position'],
      template: '<div class="handle-stub" :data-hid="id" :data-htype="type" />',
    },
  },
}

function handles(wrapper: ReturnType<typeof mount>, type: 'source' | 'target') {
  return wrapper.findAll(`.handle-stub[data-htype="${type}"]`)
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

  it('預設型別渲染單一 target 與單一 source handle', () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'LLM_ASSISTANT' } },
      global: globalConfig,
    })
    expect(handles(wrapper, 'target')).toHaveLength(1)
    expect(handles(wrapper, 'source')).toHaveLength(1)
  })

  it('TRIGGER 無 target handle、有一個 source handle', () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'TRIGGER' } },
      global: globalConfig,
    })
    expect(handles(wrapper, 'target')).toHaveLength(0)
    expect(handles(wrapper, 'source')).toHaveLength(1)
  })

  it('CONDITION 渲染兩個 source handle（out:true / out:false）並顯示 True/False label', () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'CONDITION' } },
      global: globalConfig,
    })
    const sources = handles(wrapper, 'source')
    expect(sources).toHaveLength(2)
    expect(sources.map((h) => h.attributes('data-hid'))).toEqual(['out:true', 'out:false'])
    expect(wrapper.text()).toContain('True')
    expect(wrapper.text()).toContain('False')
  })

  it('LOOP 渲染 out:loop / out:done 兩個 source handle', () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'LOOP' } },
      global: globalConfig,
    })
    expect(handles(wrapper, 'source').map((h) => h.attributes('data-hid'))).toEqual([
      'out:loop',
      'out:done',
    ])
  })
})
