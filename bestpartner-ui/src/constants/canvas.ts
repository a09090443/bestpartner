/**
 * 畫布幾何常數（單一事實來源）。
 *
 * 節點卡片的實際尺寸寫在 components/canvas/WorkflowNode.vue 的 CSS，
 * 而 dagre 自動排版與預設縮放都需要同一組數字——改動任一處務必三邊同步，
 * 否則「整理版面」會重疊、預設視野會失準。
 */

/** 節點卡片固定尺寸（對應 WorkflowNode.vue 的 width / min-height） */
export const NODE_BOX = { width: 214, height: 76 } as const

/**
 * 單一節點含水平間距的槽寬，供 computeFitZoom 估算「容納約 N 個節點」的縮放。
 * = 節點寬 214 + 節點間留白約 86
 */
export const NODE_SLOT_WIDTH = 300
