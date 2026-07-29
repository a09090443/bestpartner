import { describe, it, expect } from 'vitest'
import { NODE_ICON_PATHS, getNodeIconPaths } from '../nodeIcons'
import { NODE_TYPE_METAS } from '../nodeTypes'

/** SVG path 的 `d` 允許出現的字元（指令字母 + 數字 + 分隔符） */
const VALID_PATH_CHARS = /^[MmLlHhVvCcSsQqTtAaZz0-9.,\-+eE\s]+$/

describe('nodeIcons', () => {
  it('13 種 NodeType 皆有對應圖示（與 NODE_TYPE_METAS 完全對齊）', () => {
    const metaTypes = NODE_TYPE_METAS.map((m) => m.type).sort()
    const iconTypes = Object.keys(NODE_ICON_PATHS).sort()
    expect(iconTypes).toEqual(metaTypes)
    expect(iconTypes).toHaveLength(13)
  })

  it('每種型別至少一條 path，且每條 d 皆為非空字串', () => {
    for (const [type, paths] of Object.entries(NODE_ICON_PATHS)) {
      expect(paths.length, `${type} 應至少有一條 path`).toBeGreaterThan(0)
      for (const d of paths) {
        expect(typeof d).toBe('string')
        expect(d.trim(), `${type} 的 path 不可為空`).not.toBe('')
      }
    }
  })

  it('path 的 d 只含合法 SVG 路徑字元', () => {
    for (const [type, paths] of Object.entries(NODE_ICON_PATHS)) {
      for (const d of paths) {
        expect(VALID_PATH_CHARS.test(d), `${type} 的 path 含非法字元：${d}`).toBe(true)
      }
    }
  })

  it('任兩型別的圖示不得完全相同（攔截複製貼上漏改）', () => {
    const seen = new Map<string, string>()
    for (const [type, paths] of Object.entries(NODE_ICON_PATHS)) {
      const key = paths.join('|')
      const duplicateOf = seen.get(key)
      expect(duplicateOf, `${type} 的圖示與 ${duplicateOf} 完全相同`).toBeUndefined()
      seen.set(key, type)
    }
  })

  it('getNodeIconPaths 回傳對應型別的 path 清單', () => {
    expect(getNodeIconPaths('TRIGGER')).toEqual(NODE_ICON_PATHS.TRIGGER)
    expect(getNodeIconPaths('LLM_ASSISTANT')).toEqual(NODE_ICON_PATHS.LLM_ASSISTANT)
  })
})
