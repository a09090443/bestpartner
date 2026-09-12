import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import { ref } from 'vue'
import type { WorkflowSummaryDTO } from '../../types/workflow'

// 可控的 store mock。真實 Pinia setup store 取用 store.summaries 會自動解包，
// 故此處以 getter 回傳陣列本身，避免 el-table 收到 ref 物件。
const summaries = ref<WorkflowSummaryDTO[]>([])
const fetchListMock = vi.fn()
const removeMock = vi.fn()
const switchStatusMock = vi.fn()
vi.mock('../../stores/workflow', () => ({
  useWorkflowStore: () => ({
    get summaries() {
      return summaries.value
    },
    loading: false,
    fetchList: fetchListMock,
    remove: removeMock,
    switchStatus: switchStatusMock,
  }),
}))

// 可控的 auth store mock（登出按鈕用）
const logoutMock = vi.fn()
vi.mock('../../stores/auth', () => ({
  useAuthStore: () => ({ logout: logoutMock }),
}))

// router mock
const pushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

// 可斷言的 ElMessage mock（供錯誤訊息測試）
const msg = vi.hoisted(() => ({ successMock: vi.fn(), errorMock: vi.fn() }))

// ElMessageBox.confirm 直接 resolve，模擬使用者按下確認
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve('confirm')) },
    ElMessage: { success: msg.successMock, error: msg.errorMock },
  }
})

import WorkflowListView from '../WorkflowListView.vue'

function mountView() {
  return mount(WorkflowListView, {
    global: { plugins: [createPinia(), ElementPlus] },
  })
}

describe('WorkflowListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    summaries.value = [
      { id: 'w1', name: 'Flow A', status: 'DRAFT', version: 1, updatedAt: '2026-06-30' },
      { id: 'w2', name: 'Flow B', status: 'ACTIVE', version: 2, updatedAt: '2026-06-30' },
    ]
    fetchListMock.mockClear()
    removeMock.mockClear()
    switchStatusMock.mockClear()
    pushMock.mockClear()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('掛載時應呼叫 fetchList', () => {
    mountView()
    expect(fetchListMock).toHaveBeenCalled()
  })

  it('應渲染清單中的 workflow 名稱', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('Flow A')
    expect(wrapper.text()).toContain('Flow B')
  })

  it('點「新建」應導向 /editor', async () => {
    const wrapper = mountView()
    await wrapper.find('[data-test="create-button"]').trigger('click')
    expect(pushMock).toHaveBeenCalledWith('/editor')
  })

  it('點某列「刪除」並確認後應呼叫 store.remove', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('[data-test="delete-w1"]').trigger('click')
    await flushPromises()
    expect(removeMock).toHaveBeenCalledWith('w1')
  })

  it('updatedAt 為 null 時「更新時間」欄應顯示「—」', async () => {
    summaries.value = [{ id: 'w1', name: 'Flow A', status: 'DRAFT', version: 1, updatedAt: null as unknown as undefined }]
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('—')
  })

  it('切換狀態失敗應顯示後端 ApiResponse.message', async () => {
    switchStatusMock.mockRejectedValueOnce({
      response: { data: { message: 'An active workflow must contain a Trigger node' } },
    })
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('[data-test="toggle-w1"]').trigger('click')
    await flushPromises()
    expect(msg.errorMock).toHaveBeenCalledWith('An active workflow must contain a Trigger node')
  })
})
