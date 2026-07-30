import type { InjectionKey } from 'vue'

/**
 * 「開啟該節點的 Node Designer」回呼，由 WorkflowEditorView provide、節點卡片 inject。
 *
 * Vue Flow 的自訂節點是由 VueFlow 內部渲染的，拿不到父層的事件監聽器，
 * 因此改用 provide/inject 傳遞。節點卡片 inject 時**必須給 no-op 預設值**，
 * 否則所有單獨 mount 節點的測試都得補 provide。
 */
export const OPEN_DESIGNER_KEY: InjectionKey<(nodeId: string) => void> =
  Symbol('workflow-open-designer')

/**
 * 「從此觸發點執行」回呼，由 WorkflowEditorView provide、TRIGGER 節點卡片 inject。
 *
 * 與 [OPEN_DESIGNER_KEY] 同樣的理由走 provide/inject（Vue Flow 自訂節點拿不到父層 listener），
 * inject 時同樣**必須給 no-op 預設值**。
 */
export const RUN_FROM_TRIGGER_KEY: InjectionKey<(nodeKey: string) => void> =
  Symbol('workflow-run-from-trigger')
