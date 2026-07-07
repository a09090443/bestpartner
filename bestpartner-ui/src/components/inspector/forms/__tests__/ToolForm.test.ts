import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import type { ToolSettingSchema } from '../../../../types/toolSchema'

const loadToolOptions = vi.fn()
vi.mock('../../../../composables/useNodeOptions', () => ({
  useNodeOptions: () => ({ loadToolOptions, loadLlmOptions: vi.fn(), loadMcpOptions: vi.fn() }),
}))

const getTool = vi.fn()
const saveToolSetting = vi.fn()
const updateToolSetting = vi.fn()
vi.mock('../../../../api/tool', () => ({
  getTool: (...args: unknown[]) => getTool(...args),
  saveToolSetting: (...args: unknown[]) => saveToolSetting(...args),
  updateToolSetting: (...args: unknown[]) => updateToolSetting(...args),
}))

import ToolForm from '../ToolForm.vue'

const schema: ToolSettingSchema = {
  apiKey: { type: 'string', required: true, sensitive: true },
  timeout: { type: 'integer', required: false },
}

describe('ToolForm', () => {
  beforeEach(() => {
    loadToolOptions.mockReset()
    loadToolOptions.mockResolvedValue([
      { value: 't1', label: 'Google' },
      { value: 't2', label: 'Tavily' },
    ])
    getTool.mockReset()
    getTool.mockResolvedValue({})
    saveToolSetting.mockReset()
    updateToolSetting.mockReset()
    updateToolSetting.mockResolvedValue(undefined)
  })

  it('掛載後渲染工具下拉選項', async () => {
    const wrapper = mount(ToolForm, { props: { config: {} } })
    await flushPromises()
    expect(wrapper.findAll('[data-test="tool-select"] option').length).toBe(3)
    expect(wrapper.text()).toContain('Tavily')
  })

  it('選取 toolId 應 emit update:config 並保留未知鍵值', async () => {
    const wrapper = mount(ToolForm, { props: { config: { foo: 1 } } })
    await flushPromises()
    await wrapper.find('[data-test="tool-select"]').setValue('t1')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ foo: 1, toolId: 't1' })
  })

  it('編輯 toolSettingId 應 emit update:config 帶入 toolSettingId', async () => {
    const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
    await flushPromises()
    await wrapper.find('[data-test="tool-setting-id"]').setValue('ts-9')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ toolId: 't1', toolSettingId: 'ts-9' })
  })

  it('清空 toolSettingId 應自 config 移除該鍵', async () => {
    const wrapper = mount(ToolForm, { props: { config: { toolId: 't1', toolSettingId: 'ts-9' } } })
    await flushPromises()
    await wrapper.find('[data-test="tool-setting-id"]').setValue('')
    const emitted = wrapper.emitted('update:config')
    expect(emitted![emitted!.length - 1][0]).toEqual({ toolId: 't1' })
  })

  describe('工具設定建立區塊', () => {
    it('選擇有 schema 的工具後顯示設定建立區塊', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      const wrapper = mount(ToolForm, { props: { config: {} } })
      await flushPromises()
      expect(wrapper.find('[data-test="setting-section"]').exists()).toBe(false)
      await wrapper.find('[data-test="tool-select"]').setValue('t1')
      await flushPromises()
      expect(getTool).toHaveBeenCalledWith('t1')
      expect(wrapper.find('[data-test="setting-section"]').exists()).toBe(true)
      expect(wrapper.find('[data-test="setting-alias"]').exists()).toBe(true)
      expect(wrapper.find('[data-test="field-apiKey"]').exists()).toBe(true)
    })

    it('初始 config 已有 toolId 時掛載即載入 schema（immediate）', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      expect(getTool).toHaveBeenCalledWith('t1')
      expect(wrapper.find('[data-test="setting-section"]').exists()).toBe(true)
    })

    it('選擇無 schema 的工具不顯示設定區塊（null 與空物件皆同）', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: null })
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      expect(wrapper.find('[data-test="setting-section"]').exists()).toBe(false)

      getTool.mockResolvedValue({ id: 't2', settingSchema: {} })
      await wrapper.find('[data-test="tool-select"]').setValue('t2')
      await flushPromises()
      expect(wrapper.find('[data-test="setting-section"]').exists()).toBe(false)
    })

    it('getTool 失敗視同無 schema，不影響既有欄位操作', async () => {
      getTool.mockRejectedValue(new Error('network error'))
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      expect(wrapper.find('[data-test="setting-section"]').exists()).toBe(false)
      // 原有 toolSettingId 手動輸入仍可運作
      await wrapper.find('[data-test="tool-setting-id"]').setValue('ts-1')
      const emitted = wrapper.emitted('update:config')
      expect(emitted![emitted!.length - 1][0]).toEqual({ toolId: 't1', toolSettingId: 'ts-1' })
    })

    it('alias 或必填欄位未填時建立按鈕 disabled，全部填妥後 enabled', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      const disabled = () => wrapper.find('[data-test="setting-create"]').attributes('disabled')
      // alias 與必填 apiKey 皆缺 → disabled
      expect(disabled()).toBeDefined()
      // 僅填 alias，必填 apiKey 仍缺 → 維持 disabled
      await wrapper.find('[data-test="setting-alias"]').setValue('my-alias')
      expect(disabled()).toBeDefined()
      // 補填必填 apiKey → enabled
      await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
      expect(disabled()).toBeUndefined()
    })

    it('必填欄位被清空後建立按鈕重新 disabled', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      const disabled = () => wrapper.find('[data-test="setting-create"]').attributes('disabled')
      await wrapper.find('[data-test="setting-alias"]').setValue('my-alias')
      await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
      expect(disabled()).toBeUndefined()
      await wrapper.find('[data-test="field-apiKey"]').setValue('')
      expect(disabled()).toBeDefined()
    })

    it('建立成功後手動編輯 toolSettingId 清除過時 successMsg', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      saveToolSetting.mockResolvedValue({ settingId: 'setting-1' })
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      await wrapper.find('[data-test="setting-alias"]').setValue('my-alias')
      await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
      await wrapper.find('[data-test="setting-create"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-test="setting-success"]').exists()).toBe(true)
      // 手動改 ID 後，先前「已帶入 setting-1」的提示與現值不符，須清除
      await wrapper.find('[data-test="tool-setting-id"]').setValue('manual-99')
      expect(wrapper.find('[data-test="setting-success"]').exists()).toBe(false)
    })

    it('建立設定成功後 toolSettingId 自動帶入並 emit config，顯示成功提示並收合表單', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      saveToolSetting.mockResolvedValue({ settingId: 'setting-1' })
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      await wrapper.find('[data-test="setting-alias"]').setValue('my-alias')
      await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
      await wrapper.find('[data-test="field-timeout"]').setValue('3000')
      await wrapper.find('[data-test="setting-create"]').trigger('click')
      await flushPromises()

      expect(saveToolSetting).toHaveBeenCalledWith('t1', 'my-alias')
      expect(updateToolSetting).toHaveBeenCalledWith(
        'setting-1',
        JSON.stringify({ apiKey: 'sk-xxx', timeout: 3000 }),
      )
      const emitted = wrapper.emitted('update:config')
      expect(emitted![emitted!.length - 1][0]).toEqual({
        toolId: 't1',
        toolSettingId: 'setting-1',
      })
      expect(
        (wrapper.find('[data-test="tool-setting-id"]').element as HTMLInputElement).value,
      ).toBe('setting-1')
      // 成功後顯示提示並收合表單主體（建立按鈕不再顯示）
      expect(wrapper.find('[data-test="setting-success"]').exists()).toBe(true)
      expect(wrapper.find('[data-test="setting-create"]').exists()).toBe(false)
    })

    it('saveToolSetting 回傳缺 settingId 時顯示錯誤且不呼叫 updateToolSetting', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      saveToolSetting.mockResolvedValue({})
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      await wrapper.find('[data-test="setting-alias"]').setValue('my-alias')
      // 必填 apiKey 須填妥才能啟用建立按鈕
      await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
      await wrapper.find('[data-test="setting-create"]').trigger('click')
      await flushPromises()

      expect(updateToolSetting).not.toHaveBeenCalled()
      expect(wrapper.find('[data-test="setting-error"]').exists()).toBe(true)
      // 不得自動帶入 toolSettingId
      expect(
        (wrapper.find('[data-test="tool-setting-id"]').element as HTMLInputElement).value,
      ).toBe('')
    })

    it('API 失敗時顯示錯誤文字且不清空使用者輸入', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      saveToolSetting.mockRejectedValue(new Error('boom'))
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      await wrapper.find('[data-test="setting-alias"]').setValue('my-alias')
      await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
      await wrapper.find('[data-test="setting-create"]').trigger('click')
      await flushPromises()

      expect(wrapper.find('[data-test="setting-error"]').exists()).toBe(true)
      expect(
        (wrapper.find('[data-test="setting-alias"]').element as HTMLInputElement).value,
      ).toBe('my-alias')
      expect(
        (wrapper.find('[data-test="field-apiKey"]').element as HTMLInputElement).value,
      ).toBe('sk-xxx')
    })

    it('建立中按鈕 disabled 防止重複送出', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      let resolveSave: (v: unknown) => void = () => {}
      saveToolSetting.mockReturnValue(new Promise((resolve) => (resolveSave = resolve)))
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      await wrapper.find('[data-test="setting-alias"]').setValue('my-alias')
      // 必填 apiKey 須填妥才能啟用建立按鈕
      await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
      await wrapper.find('[data-test="setting-create"]').trigger('click')
      await wrapper.vm.$nextTick()
      expect(wrapper.find('[data-test="setting-create"]').attributes('disabled')).toBeDefined()
      resolveSave({ settingId: 'setting-1' })
      await flushPromises()
      expect(saveToolSetting).toHaveBeenCalledTimes(1)
    })

    it('切換工具時重置 alias、設定值與錯誤訊息', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      saveToolSetting.mockRejectedValue(new Error('boom'))
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      await wrapper.find('[data-test="setting-alias"]').setValue('my-alias')
      await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
      await wrapper.find('[data-test="setting-create"]').trigger('click')
      await flushPromises()
      expect(wrapper.find('[data-test="setting-error"]').exists()).toBe(true)

      getTool.mockResolvedValue({ id: 't2', settingSchema: schema })
      await wrapper.find('[data-test="tool-select"]').setValue('t2')
      await flushPromises()
      expect(wrapper.find('[data-test="setting-error"]').exists()).toBe(false)
      expect(
        (wrapper.find('[data-test="setting-alias"]').element as HTMLInputElement).value,
      ).toBe('')
      expect(
        (wrapper.find('[data-test="field-apiKey"]').element as HTMLInputElement).value,
      ).toBe('')
    })

    it('建立 pending 中切換工具：中止流程，不寫入設定亦不帶入 toolSettingId', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      let resolveSave: (v: unknown) => void = () => {}
      saveToolSetting.mockReturnValue(new Promise((resolve) => (resolveSave = resolve)))
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      await wrapper.find('[data-test="setting-alias"]').setValue('my-alias')
      await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
      await wrapper.find('[data-test="setting-create"]').trigger('click')
      // saveToolSetting 尚未回應時切換工具
      getTool.mockResolvedValue({ id: 't2', settingSchema: schema })
      await wrapper.find('[data-test="tool-select"]').setValue('t2')
      await flushPromises()
      resolveSave({ settingId: 'setting-1' })
      await flushPromises()

      expect(updateToolSetting).not.toHaveBeenCalled()
      expect(
        (wrapper.find('[data-test="tool-setting-id"]').element as HTMLInputElement).value,
      ).toBe('')
      expect(wrapper.find('[data-test="setting-success"]').exists()).toBe(false)
      expect(wrapper.find('[data-test="setting-error"]').exists()).toBe(false)
      const emitted = wrapper.emitted('update:config')!
      expect(emitted[emitted.length - 1][0]).toEqual({ toolId: 't2' })
    })

    it('updateToolSetting 進行中切換工具：內容為建立當下快照，且不帶入 toolSettingId', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      saveToolSetting.mockResolvedValue({ settingId: 'setting-1' })
      let resolveUpdate: (v?: unknown) => void = () => {}
      updateToolSetting.mockReturnValue(new Promise((resolve) => (resolveUpdate = resolve)))
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      await wrapper.find('[data-test="setting-alias"]').setValue('my-alias')
      await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
      await wrapper.find('[data-test="setting-create"]').trigger('click')
      await flushPromises()
      // updateToolSetting 已以建立當下的快照內容送出（pending 中）
      expect(updateToolSetting).toHaveBeenCalledWith(
        'setting-1',
        JSON.stringify({ apiKey: 'sk-xxx' }),
      )
      // pending 期間切換工具（watch 會重置 settingValues）
      getTool.mockResolvedValue({ id: 't2', settingSchema: schema })
      await wrapper.find('[data-test="tool-select"]').setValue('t2')
      await flushPromises()
      resolveUpdate()
      await flushPromises()

      expect(
        (wrapper.find('[data-test="tool-setting-id"]').element as HTMLInputElement).value,
      ).toBe('')
      expect(wrapper.find('[data-test="setting-success"]').exists()).toBe(false)
      const emitted = wrapper.emitted('update:config')!
      expect(emitted[emitted.length - 1][0]).toEqual({ toolId: 't2' })
    })

    it('舊 getTool 請求晚到的失敗不覆寫新工具已載入的 schema', async () => {
      let rejectFirst: (e: unknown) => void = () => {}
      getTool.mockImplementationOnce(() => new Promise((_, reject) => (rejectFirst = reject)))
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      getTool.mockResolvedValue({ id: 't2', settingSchema: schema })
      await wrapper.find('[data-test="tool-select"]').setValue('t2')
      await flushPromises()
      expect(wrapper.find('[data-test="setting-section"]').exists()).toBe(true)
      // t1 的請求此時才失敗，不得清掉 t2 的 schema
      rejectFirst(new Error('slow failure'))
      await flushPromises()
      expect(wrapper.find('[data-test="setting-section"]').exists()).toBe(true)
    })

    it('初始 immediate 載入不清空 config 既有的 toolSettingId', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      const wrapper = mount(ToolForm, {
        props: { config: { toolId: 't1', toolSettingId: 'ts-9' } },
      })
      await flushPromises()
      expect(
        (wrapper.find('[data-test="tool-setting-id"]').element as HTMLInputElement).value,
      ).toBe('ts-9')
      // 未發生任何清空動作，不應 emit config
      expect(wrapper.emitted('update:config')).toBeUndefined()
    })

    it('實際切換工具時清空 toolSettingId 並 emit config', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      const wrapper = mount(ToolForm, {
        props: { config: { toolId: 't1', toolSettingId: 'ts-9' } },
      })
      await flushPromises()
      await wrapper.find('[data-test="tool-select"]').setValue('t2')
      await flushPromises()
      expect(
        (wrapper.find('[data-test="tool-setting-id"]').element as HTMLInputElement).value,
      ).toBe('')
      const emitted = wrapper.emitted('update:config')!
      expect(emitted[emitted.length - 1][0]).toEqual({ toolId: 't2' })
    })

    it('點擊摺疊標題可收合與展開表單主體', async () => {
      getTool.mockResolvedValue({ id: 't1', settingSchema: schema })
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      expect(wrapper.find('[data-test="setting-create"]').exists()).toBe(true)
      await wrapper.find('[data-test="setting-toggle"]').trigger('click')
      expect(wrapper.find('[data-test="setting-create"]').exists()).toBe(false)
      await wrapper.find('[data-test="setting-toggle"]').trigger('click')
      expect(wrapper.find('[data-test="setting-create"]').exists()).toBe(true)
    })

    it('required boolean 欄位未觸碰時不阻擋建立按鈕（boolean 必填語意模糊，false 亦合法）', async () => {
      const boolSchema: ToolSettingSchema = {
        apiKey: { type: 'string', required: true },
        strict: { type: 'boolean', required: true },
      }
      getTool.mockResolvedValue({ id: 't1', settingSchema: boolSchema })
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      const disabled = () => wrapper.find('[data-test="setting-create"]').attributes('disabled')
      await wrapper.find('[data-test="setting-alias"]').setValue('my-alias')
      // string 必填填妥；required boolean（strict）未觸碰不應使按鈕 disabled
      await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
      expect(disabled()).toBeUndefined()
    })
  })

  describe('arguments 呼叫參數欄位', () => {
    it('輸入合法 JSON 物件 → emit 帶 parse 後物件', async () => {
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      await wrapper.find('[data-test="arguments"]').setValue('{"q": "{{input}}"}')
      const emitted = wrapper.emitted('update:config')!
      expect(emitted[emitted.length - 1][0]).toEqual({
        toolId: 't1',
        arguments: { q: '{{input}}' },
      })
    })

    it('清空 arguments → 自 config 刪鍵', async () => {
      const wrapper = mount(ToolForm, {
        props: { config: { toolId: 't1', arguments: { a: 1 } } },
      })
      await flushPromises()
      await wrapper.find('[data-test="arguments"]').setValue('')
      const emitted = wrapper.emitted('update:config')!
      expect(emitted[emitted.length - 1][0]).toEqual({ toolId: 't1' })
    })

    it('輸入非法 JSON → 不寫入 arguments 鍵（不讓壞 JSON 進 config）', async () => {
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      await wrapper.find('[data-test="arguments"]').setValue('{ bad json')
      const emitted = wrapper.emitted('update:config')!
      const last = emitted[emitted.length - 1][0] as Record<string, unknown>
      expect(last).toEqual({ toolId: 't1' })
      expect(last).not.toHaveProperty('arguments')
    })

    it('由既有 config.arguments 初始化為格式化 JSON 文字', async () => {
      const wrapper = mount(ToolForm, {
        props: { config: { toolId: 't1', arguments: { a: 1 } } },
      })
      await flushPromises()
      expect((wrapper.find('[data-test="arguments"]').element as HTMLTextAreaElement).value).toBe(
        JSON.stringify({ a: 1 }, null, 2),
      )
    })

    it('輸入合法但非物件的 JSON（如陣列）→ 不寫入 arguments 鍵', async () => {
      const wrapper = mount(ToolForm, { props: { config: { toolId: 't1' } } })
      await flushPromises()
      await wrapper.find('[data-test="arguments"]').setValue('[1,2]')
      const emitted = wrapper.emitted('update:config')!
      const last = emitted[emitted.length - 1][0] as Record<string, unknown>
      expect(last).toEqual({ toolId: 't1' })
      expect(last).not.toHaveProperty('arguments')
    })
  })
})
