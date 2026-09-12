import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import DesignerOutputPreview from '../DesignerOutputPreview.vue'

function mountPreview(value: unknown, nodeId = 'n1') {
  return mount(DesignerOutputPreview, { props: { value, nodeId } })
}

describe('DesignerOutputPreview', () => {
  it('物件輸出逐鍵列出可引用的插值字串與型別', () => {
    const w = mountPreview({ result: 'ok', count: 3 })
    expect(w.text()).toContain('{{n1.result}}')
    expect(w.text()).toContain('{{n1.count}}')
    expect(w.text()).toContain('string')
    expect(w.text()).toContain('number')
  })

  it('陣列標出長度、物件標出鍵數', () => {
    const w = mountPreview({ items: [1, 2, 3], meta: { a: 1 } })
    expect(w.text()).toContain('array(3)')
    expect(w.text()).toContain('object(1)')
  })

  it('輸出本身為純量時以單一 value 列表示', () => {
    const w = mountPreview('hello')
    expect(w.text()).toContain('{{n1.value}}')
    expect(w.text()).toContain('hello')
  })

  it('未執行（undefined）時不渲染任何列', () => {
    const w = mountPreview(undefined)
    expect(w.findAll('.row')).toHaveLength(0)
  })

  it('過長的值會截斷，避免撐爆版面', () => {
    const long = 'x'.repeat(200)
    const w = mountPreview({ text: long })
    expect(w.text()).toContain('…')
    expect(w.text()).not.toContain(long)
  })
})
