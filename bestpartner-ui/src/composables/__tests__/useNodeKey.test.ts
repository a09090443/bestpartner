import { describe, it, expect } from 'vitest'
import { generateNodeKey } from '../useNodeKey'

describe('generateNodeKey', () => {
  it('回傳長度為 8 的字串', () => {
    const key = generateNodeKey()
    expect(typeof key).toBe('string')
    expect(key).toHaveLength(8)
  })

  it('連續呼叫 1000 次皆唯一', () => {
    const keys = new Set<string>()
    for (let i = 0; i < 1000; i++) {
      keys.add(generateNodeKey())
    }
    expect(keys.size).toBe(1000)
  })
})
