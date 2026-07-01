import { describe, it, expect } from 'vitest'
import { IN_MAIN, OUT_MAIN, parseHandle } from '../handles'

describe('handle 常數', () => {
  it('IN_MAIN / OUT_MAIN 為預期字串', () => {
    expect(IN_MAIN).toBe('in:main')
    expect(OUT_MAIN).toBe('out:main')
  })
})

describe('parseHandle', () => {
  it('解析輸入埠 in:main', () => {
    expect(parseHandle('in:main')).toEqual({ role: 'in', port: 'main' })
  })

  it('解析輸出埠 out:true', () => {
    expect(parseHandle('out:true')).toEqual({ role: 'out', port: 'true' })
  })

  it('port 含冒號時只切第一個冒號', () => {
    expect(parseHandle('out:a:b')).toEqual({ role: 'out', port: 'a:b' })
  })

  it('非 in/out 角色回傳 null', () => {
    expect(parseHandle('foo:main')).toBeNull()
  })

  it('缺少冒號回傳 null', () => {
    expect(parseHandle('outmain')).toBeNull()
  })

  it('空 port 回傳 null', () => {
    expect(parseHandle('out:')).toBeNull()
  })
})
