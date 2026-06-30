import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as workflowApi from '../api/workflow'
import type { WorkflowSummaryDTO } from '../types/workflow'

export const useWorkflowStore = defineStore('workflow', () => {
  // 清單摘要（編輯相關 state 於 M5 再加入）
  const summaries = ref<WorkflowSummaryDTO[]>([])
  const loading = ref(false)

  /** 載入清單，寫入 summaries */
  async function fetchList(): Promise<void> {
    loading.value = true
    try {
      summaries.value = await workflowApi.list()
    } finally {
      loading.value = false
    }
  }

  /** 刪除 workflow，成功後由 summaries 移除 */
  async function remove(id: string): Promise<void> {
    await workflowApi.remove(id)
    summaries.value = summaries.value.filter((s) => s.id !== id)
  }

  /** 啟用/停用 workflow，成功後更新本地 status */
  async function switchStatus(id: string, active: boolean): Promise<void> {
    await workflowApi.switchStatus(id, active)
    const target = summaries.value.find((s) => s.id === id)
    if (target) {
      target.status = active ? 'ACTIVE' : 'INACTIVE'
    }
  }

  return {
    summaries,
    loading,
    fetchList,
    remove,
    switchStatus,
  }
})
