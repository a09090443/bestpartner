import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import NodeIcon from '../NodeIcon.vue'
import { NODE_ICON_PATHS } from '../../../constants/nodeIcons'

describe('NodeIcon', () => {
  it('渲染 svg 並以 data-icon 標示型別', () => {
    const wrapper = mount(NodeIcon, { props: { type: 'TRIGGER' } })
    const svg = wrapper.find('svg')
    expect(svg.exists()).toBe(true)
    expect(svg.attributes('data-icon')).toBe('TRIGGER')
  })

  it('path 數量與 NODE_ICON_PATHS 一致', () => {
    const wrapper = mount(NodeIcon, { props: { type: 'LLM_ASSISTANT' } })
    expect(wrapper.findAll('path')).toHaveLength(NODE_ICON_PATHS.LLM_ASSISTANT.length)
  })

  it('size 未指定時為 18，指定時反映於 width / height', () => {
    const def = mount(NodeIcon, { props: { type: 'TOOL' } })
    expect(def.find('svg').attributes('width')).toBe('18')
    expect(def.find('svg').attributes('height')).toBe('18')

    const sized = mount(NodeIcon, { props: { type: 'TOOL', size: 22 } })
    expect(sized.find('svg').attributes('width')).toBe('22')
    expect(sized.find('svg').attributes('height')).toBe('22')
  })

  it('stroke 為 currentColor，顏色由父層決定', () => {
    const wrapper = mount(NodeIcon, { props: { type: 'OUTPUT' } })
    expect(wrapper.find('svg').attributes('stroke')).toBe('currentColor')
    expect(wrapper.find('svg').attributes('fill')).toBe('none')
  })

  it('以 aria-hidden 標示為裝飾性圖示', () => {
    const wrapper = mount(NodeIcon, { props: { type: 'CODE' } })
    expect(wrapper.find('svg').attributes('aria-hidden')).toBe('true')
  })
})
