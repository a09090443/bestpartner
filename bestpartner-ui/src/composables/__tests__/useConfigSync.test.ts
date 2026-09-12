import { describe, it, expect } from 'vitest'
import { ref, nextTick } from 'vue'
import { useConfigSync } from '../useConfigSync'

/**
 * 背景：E2E 週期 202608192201 的 J9-10——Inspector 與 Node Designer 的 Parameters 面板
 * 各自 mount 一個型別化表單實例，其中一個 emit `update:config` 更新 `node.data.config` 後，
 * 另一個實例的本地 ref 不會跟著變（表單只在建立當下讀一次 props）。
 */
describe('useConfigSync', () => {
  it('外部改動 config 時應把新值套進本地狀態', async () => {
    const config = ref<Record<string, unknown>>({ llmId: 'old' })
    const local = ref('old')
    useConfigSync(
      () => config.value,
      (cfg) => {
        local.value = (cfg.llmId as string) ?? ''
      },
    )

    config.value = { llmId: 'new' }
    await nextTick()

    expect(local.value).toBe('new')
  })

  it('自身 emit 造成的回流應略過，不重灌本地狀態', async () => {
    const config = ref<Record<string, unknown>>({ llmId: 'a' })
    const local = ref('a')
    let applied = 0
    const { markSelfEmit } = useConfigSync(
      () => config.value,
      (cfg) => {
        applied += 1
        local.value = (cfg.llmId as string) ?? ''
      },
    )

    // 模擬表單自己送出：先 markSelfEmit，父層再把同一個物件指回 config
    const next = { llmId: 'typed-by-user' }
    markSelfEmit(next)
    config.value = next
    await nextTick()

    expect(applied).toBe(0)
    expect(local.value).toBe('a')
  })

  it('自身 emit 後，下一次外部改動仍應正常套用', async () => {
    const config = ref<Record<string, unknown>>({ llmId: 'a' })
    const local = ref('a')
    const { markSelfEmit } = useConfigSync(
      () => config.value,
      (cfg) => {
        local.value = (cfg.llmId as string) ?? ''
      },
    )

    const mine = { llmId: 'b' }
    markSelfEmit(mine)
    config.value = mine
    await nextTick()
    expect(local.value).toBe('a')

    // 另一個實例送來的是不同物件 → 應套用
    config.value = { llmId: 'c' }
    await nextTick()
    expect(local.value).toBe('c')
  })

  it('config 變為 undefined 時應以空物件套用（不拋錯）', async () => {
    const config = ref<Record<string, unknown> | undefined>({ llmId: 'a' })
    const local = ref('a')
    useConfigSync(
      () => config.value,
      (cfg) => {
        local.value = (cfg.llmId as string) ?? ''
      },
    )

    config.value = undefined
    await nextTick()

    expect(local.value).toBe('')
  })
})
