import { describe, it, expect } from 'vitest'
import { parseJsonObjectField } from '../json'

describe('parseJsonObjectField', () => {
  it('合法 JSON 物件 → 回傳解析後物件', () => {
    expect(parseJsonObjectField('{"q": "{{input}}"}')).toEqual({ q: '{{input}}' })
  })

  it('前後空白仍可解析', () => {
    expect(parseJsonObjectField('  {"a":1}  ')).toEqual({ a: 1 })
  })

  it('空字串 → null', () => {
    expect(parseJsonObjectField('')).toBeNull()
    expect(parseJsonObjectField('   ')).toBeNull()
  })

  it('非法 JSON → null', () => {
    expect(parseJsonObjectField('{ bad json')).toBeNull()
  })

  it('合法但非物件（陣列/字串/數字/布林/null）→ null', () => {
    expect(parseJsonObjectField('[1,2]')).toBeNull()
    expect(parseJsonObjectField('"hi"')).toBeNull()
    expect(parseJsonObjectField('123')).toBeNull()
    expect(parseJsonObjectField('true')).toBeNull()
    expect(parseJsonObjectField('null')).toBeNull()
  })
})
