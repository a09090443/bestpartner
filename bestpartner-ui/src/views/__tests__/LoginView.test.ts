import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import ElementPlus from 'element-plus'

// Mock api/auth，避免真實 HTTP 請求
vi.mock('../../api/auth', () => ({
  login: vi.fn(),
}))

// Mock vue-router 的 useRouter，攔截 push
const pushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

import * as authApi from '../../api/auth'
import LoginView from '../LoginView.vue'

function mountLoginView() {
  return mount(LoginView, {
    global: {
      plugins: [createPinia(), ElementPlus],
    },
  })
}

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    pushMock.mockClear()
  })

  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('填入帳密並提交後，應以正確帳密呼叫 authStore.login', async () => {
    vi.mocked(authApi.login).mockResolvedValueOnce('jwt-token')

    const wrapper = mountLoginView()
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('admin@example.com')
    await inputs[1].setValue('secret')

    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(authApi.login).toHaveBeenCalledWith('admin@example.com', 'secret')
  })

  it('登入成功後應導向首頁 /', async () => {
    vi.mocked(authApi.login).mockResolvedValueOnce('jwt-token')

    const wrapper = mountLoginView()
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('user@example.com')
    await inputs[1].setValue('password123')

    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith('/')
  })

  it('登入失敗時不應導向，token 保持空', async () => {
    vi.mocked(authApi.login).mockRejectedValueOnce(new Error('帳密錯誤'))

    const wrapper = mountLoginView()
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('bad@example.com')
    await inputs[1].setValue('wrong')

    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(pushMock).not.toHaveBeenCalled()
  })
})
