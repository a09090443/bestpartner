import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as workflowApi from '../api/workflow'
import { extractApiMessage } from '../api/http'
import { dtoToFlow, flowToSaveRequest } from '../composables/useWorkflowSync'
import type { FlowNode, FlowEdge } from '../composables/useWorkflowSync'
import type { WorkflowStatus, WorkflowSummaryDTO } from '../types/workflow'

/** 版本衝突（後端樂觀鎖）專屬錯誤，供 UI 顯示重新載入對話框 */
export class WorkflowVersionConflictError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'WorkflowVersionConflictError'
  }
}

/** 後端版本衝突訊息（en/zh 兩種語系） */
const VERSION_CONFLICT_MESSAGES = [
  'Workflow has been modified by another session, please reload',
  '工作流程已被其他作業修改，請重新載入',
]

/** 編輯中的 workflow（畫布以 Vue Flow 格式存放） */
export interface EditingWorkflow {
  id?: string
  name: string
  description?: string
  status?: WorkflowStatus
  version?: number
  canvasMeta?: Record<string, unknown> | null
  nodes: FlowNode[]
  edges: FlowEdge[]
}

export const useWorkflowStore = defineStore('workflow', () => {
  // 清單摘要
  const summaries = ref<WorkflowSummaryDTO[]>([])
  const loading = ref(false)

  // 編輯中的 workflow 與未存旗標
  const current = ref<EditingWorkflow | null>(null)
  const dirty = ref(false)

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

  /** 載入既有 workflow 至編輯 state（DTO → Vue Flow 畫布格式） */
  async function load(id: string): Promise<void> {
    const dto = await workflowApi.get(id)
    const { nodes, edges } = dtoToFlow(dto)
    current.value = {
      id: dto.id,
      name: dto.name,
      description: dto.description,
      status: dto.status,
      version: dto.version,
      canvasMeta: dto.canvasMeta,
      nodes,
      edges,
    }
    dirty.value = false
  }

  /** 建立新編輯 state（無 id、空畫布） */
  function createNew(): void {
    current.value = {
      name: '未命名流程',
      status: 'DRAFT',
      nodes: [],
      edges: [],
    }
    dirty.value = false
  }

  /**
   * 存檔：以畫布當前 nodes/edges + 編輯 meta 組請求送出。
   * 成功後以回傳的最新版本更新 state 並清除 dirty；
   * 遇後端版本衝突拋出 [WorkflowVersionConflictError]。
   */
  async function save(flowNodes: FlowNode[], flowEdges: FlowEdge[]): Promise<void> {
    if (!current.value) return
    const meta = current.value
    const req = flowToSaveRequest(flowNodes, flowEdges, {
      id: meta.id,
      version: meta.version,
      name: meta.name,
      description: meta.description,
      canvasMeta: meta.canvasMeta,
    })

    try {
      const saved = await workflowApi.save(req)
      current.value = {
        ...meta,
        id: saved.id,
        status: saved.status,
        version: saved.version,
        nodes: flowNodes,
        edges: flowEdges,
      }
      dirty.value = false
    } catch (err) {
      const message = extractApiMessage(err)
      if (message && VERSION_CONFLICT_MESSAGES.includes(message)) {
        throw new WorkflowVersionConflictError(message)
      }
      throw err
    }
  }

  /** 設定未存旗標 */
  function setDirty(value: boolean): void {
    dirty.value = value
  }

  return {
    summaries,
    loading,
    current,
    dirty,
    fetchList,
    remove,
    switchStatus,
    load,
    createNew,
    save,
    setDirty,
  }
})
