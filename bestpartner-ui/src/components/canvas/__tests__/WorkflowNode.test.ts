import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import WorkflowNode from '../WorkflowNode.vue'
import { useExecutionStore } from '../../../stores/execution'
import { OPEN_DESIGNER_KEY, RUN_FROM_TRIGGER_KEY } from '../designerInjection'

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

// WorkflowNode 內用 useExecutionStore 讀取執行狀態上色，需先啟用 Pinia
beforeEach(() => setActivePinia(createPinia()))

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

  it('預設型別（TOOL）渲染單一 target 與單一 source handle', () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'TOOL' } },
      global: globalConfig,
    })
    expect(handles(wrapper, 'target')).toHaveLength(1)
    expect(handles(wrapper, 'source')).toHaveLength(1)
  })

  it('LLM_ASSISTANT 渲染三個 target handle（in:main / in:prompt / in:tool）', () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'LLM_ASSISTANT' } },
      global: globalConfig,
    })
    const ids = handles(wrapper, 'target').map((h) => h.attributes('data-hid'))
    expect(ids).toEqual(['in:main', 'in:prompt', 'in:tool'])
    expect(handles(wrapper, 'source')).toHaveLength(1)
  })

  it('PROMPT 渲染單一 target / source handle', () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'PROMPT' } },
      global: globalConfig,
    })
    expect(handles(wrapper, 'target').map((h) => h.attributes('data-hid'))).toEqual(['in:main'])
    expect(handles(wrapper, 'source').map((h) => h.attributes('data-hid'))).toEqual(['out:main'])
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

  it('根元素以 --node-color CSS 變數帶出型別色（供選取外環／輸出埠使用）', () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'LLM_ASSISTANT' } },
      global: globalConfig,
    })
    const style = wrapper.find('.workflow-node').attributes('style') ?? ''
    expect(style).toContain('--node-color: #409eff')
  })

  it('節點於 execution store 為 CANCELLED 時掛上 is-exec-cancelled class', () => {
    const store = useExecutionStore()
    store.nodeStates['n1'] = { status: 'CANCELLED' }
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'LLM_ASSISTANT' } },
      global: globalConfig,
    })
    expect(wrapper.find('.workflow-node').classes()).toContain('is-exec-cancelled')
  })

  it('副標顯示 config 首個原始值；無 config 時退回型別小寫', () => {
    const withConfig = mount(WorkflowNode, {
      props: {
        id: 'n1',
        data: { type: 'HTTP_REQUEST', config: { url: 'https://api.example.com' } },
      },
      global: globalConfig,
    })
    expect(withConfig.find('[data-test="node-subtitle"]').text()).toContain(
      'https://api.example.com',
    )

    const noConfig = mount(WorkflowNode, {
      props: { id: 'n2', data: { type: 'CODE' } },
      global: globalConfig,
    })
    expect(noConfig.find('[data-test="node-subtitle"]').text()).toBe('code')
  })

  it('執行成功時顯示完成徽章，其他狀態不顯示', () => {
    const store = useExecutionStore()
    store.nodeStates['n1'] = { status: 'SUCCESS' }
    store.nodeStates['n2'] = { status: 'RUNNING' }

    const done = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'TOOL' } },
      global: globalConfig,
    })
    expect(done.find('[data-test="node-done-badge"]').exists()).toBe(true)

    const running = mount(WorkflowNode, {
      props: { id: 'n2', data: { type: 'TOOL' } },
      global: globalConfig,
    })
    expect(running.find('[data-test="node-done-badge"]').exists()).toBe(false)
  })

  it('右上角開啟鈕呼叫 inject 進來的 openDesigner 並帶入節點 id', async () => {
    const calls: string[] = []
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n7', data: { type: 'TOOL' } },
      global: {
        ...globalConfig,
        provide: { [OPEN_DESIGNER_KEY as symbol]: (id: string) => calls.push(id) },
      },
    })

    await wrapper.find('[data-test="node-open-designer"]').trigger('click')
    expect(calls).toEqual(['n7'])
  })

  it('未 provide openDesigner 時點開啟鈕不報錯（預設 no-op）', async () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'n8', data: { type: 'TOOL' } },
      global: globalConfig,
    })
    await expect(
      wrapper.find('[data-test="node-open-designer"]').trigger('click'),
    ).resolves.not.toThrow()
  })

  it('TRIGGER 節點渲染「從此觸發點執行」鈕，非 TRIGGER 不渲染', () => {
    const trigger = mount(WorkflowNode, {
      props: { id: 'n1', data: { type: 'TRIGGER' } },
      global: globalConfig,
    })
    expect(trigger.find('[data-test="node-run-from-here"]').exists()).toBe(true)

    const tool = mount(WorkflowNode, {
      props: { id: 'n2', data: { type: 'TOOL' } },
      global: globalConfig,
    })
    expect(tool.find('[data-test="node-run-from-here"]').exists()).toBe(false)
  })

  it('「從此觸發點執行」呼叫 inject 進來的回呼並帶入節點 id', async () => {
    const calls: string[] = []
    const wrapper = mount(WorkflowNode, {
      props: { id: 'trg-9', data: { type: 'TRIGGER' } },
      global: {
        ...globalConfig,
        provide: { [RUN_FROM_TRIGGER_KEY as symbol]: (key: string) => calls.push(key) },
      },
    })

    await wrapper.find('[data-test="node-run-from-here"]').trigger('click')
    expect(calls).toEqual(['trg-9'])
  })

  it('未 provide runFromTrigger 時點執行鈕不報錯（預設 no-op）', async () => {
    const wrapper = mount(WorkflowNode, {
      props: { id: 'trg-1', data: { type: 'TRIGGER' } },
      global: globalConfig,
    })
    await expect(
      wrapper.find('[data-test="node-run-from-here"]').trigger('click'),
    ).resolves.not.toThrow()
  })
})
