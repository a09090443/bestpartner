/**
 * Node Designer（全屏節點編輯頁）用的跨模組型別。
 *
 * 上游來源由 WorkflowEditorView 依畫布 edges + execution store 解析後傳入，
 * 讓 modal 與其子面板維持 dumb component（不自行取資料，易於測試）。
 */

export interface UpstreamSource {
  /** 上游節點 key */
  nodeId: string
  /** 上游節點顯示名稱 */
  name: string
  /** 連進本節點的哪個埠（in:main / in:prompt / in:tool） */
  targetHandle: string
  /** 上游節點最近一次執行的輸出；未執行為 undefined */
  output?: unknown
}
