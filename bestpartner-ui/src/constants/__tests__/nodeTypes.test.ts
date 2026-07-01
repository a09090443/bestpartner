import { describe, it, expect } from 'vitest'
import { NODE_TYPE_METAS, getNodeTypeMeta } from '../nodeTypes'

describe('nodeTypes 埠定義', () => {
  it('每個型別都有 inputs / outputs 陣列', () => {
    for (const meta of NODE_TYPE_METAS) {
      expect(Array.isArray(meta.inputs)).toBe(true)
      expect(Array.isArray(meta.outputs)).toBe(true)
    }
  })

  it('TRIGGER 無輸入、單一 out:main', () => {
    const meta = getNodeTypeMeta('TRIGGER')!
    expect(meta.inputs).toEqual([])
    expect(meta.outputs.map((p) => p.id)).toEqual(['out:main'])
  })

  it('預設型別（TOOL）為 in:main + out:main', () => {
    const meta = getNodeTypeMeta('TOOL')!
    expect(meta.inputs.map((p) => p.id)).toEqual(['in:main'])
    expect(meta.outputs.map((p) => p.id)).toEqual(['out:main'])
  })

  it('CONDITION 有 out:true / out:false 兩輸出並帶 label', () => {
    const meta = getNodeTypeMeta('CONDITION')!
    expect(meta.inputs.map((p) => p.id)).toEqual(['in:main'])
    expect(meta.outputs.map((p) => p.id)).toEqual(['out:true', 'out:false'])
    expect(meta.outputs.every((p) => !!p.label)).toBe(true)
  })

  it('LOOP 有 out:loop / out:done 兩輸出', () => {
    const meta = getNodeTypeMeta('LOOP')!
    expect(meta.outputs.map((p) => p.id)).toEqual(['out:loop', 'out:done'])
  })
})
