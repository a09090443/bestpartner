import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TriggerForm from '../TriggerForm.vue'

function mountForm(config: Record<string, unknown> = {}) {
  return mount(TriggerForm, { props: { config } })
}

describe('TriggerForm', () => {
  it('帶入既有的 triggerType 作為選中值', () => {
    const w = mountForm({ triggerType: 'MANUAL' })
    expect((w.find('[data-test="trigger-type"]').element as HTMLSelectElement).value).toBe('MANUAL')
  })

  it('選擇觸發類型後 emit update:config', async () => {
    const w = mountForm()
    await w.find('[data-test="trigger-type"]').setValue('MANUAL')
    const emitted = w.emitted('update:config')
    expect(emitted).toBeTruthy()
    expect(emitted![emitted!.length - 1][0]).toEqual({ triggerType: 'MANUAL' })
  })

  it('保留表單未涵蓋的既有 config 鍵（如 inputSchema）', async () => {
    const w = mountForm({ triggerType: '', inputSchema: { foo: 'bar' } })
    await w.find('[data-test="trigger-type"]').setValue('MANUAL')
    const emitted = w.emitted('update:config')!
    expect(emitted[emitted.length - 1][0]).toEqual({
      triggerType: 'MANUAL',
      inputSchema: { foo: 'bar' },
    })
  })

  it('清空選擇時移除 triggerType 鍵，維持 config 精簡', async () => {
    const w = mountForm({ triggerType: 'MANUAL' })
    await w.find('[data-test="trigger-type"]').setValue('')
    const emitted = w.emitted('update:config')!
    expect(emitted[emitted.length - 1][0]).toEqual({})
  })

  /**
   * WEBHOOK / CRON 尚無觸發來源實作（TriggerExecutor 不區分類型、引擎寫紀錄固定 MANUAL）。
   * 列出但停用，避免使用者選到存得起來卻永遠不會被觸發的設定。
   */
  it('WEBHOOK / CRON 列出但停用，MANUAL 可選', () => {
    const w = mountForm()
    const opts = w.findAll('[data-test="trigger-type"] option')
    const byValue = Object.fromEntries(
      opts.map((o) => [(o.element as HTMLOptionElement).value, o.element as HTMLOptionElement]),
    )
    expect(byValue.MANUAL.disabled).toBe(false)
    expect(byValue.WEBHOOK.disabled).toBe(true)
    expect(byValue.CRON.disabled).toBe(true)
  })
})
