import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ExecutionResultDrawer from '../ExecutionResultDrawer.vue'
import { useExecutionStore } from '../../../stores/execution'

describe('ExecutionResultDrawer', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('無執行結果時不顯示', () => {
    const wrapper = mount(ExecutionResultDrawer)
    expect(wrapper.find('[data-test="result-drawer"]').exists()).toBe(false)
  })

  it('執行完成顯示狀態與最終輸出', async () => {
    const store = useExecutionStore()
    store.applyEvent({ event: 'execution.completed', executionId: 'e1', status: 'SUCCESS', output: { result: '答案' }, ts: 't' })
    const wrapper = mount(ExecutionResultDrawer)
    expect(wrapper.find('[data-test="result-drawer"]').text()).toContain('SUCCESS')
    expect(wrapper.find('[data-test="result-drawer"]').text()).toContain('答案')
  })
})
