/**
 * 依畫布可視寬度計算「舒適容納 N 個節點」的預設縮放。
 *
 * 畫布（VueFlow）本身無限延伸，「預設大小」等同於初始檢視縮放。
 * 讓水平排列的 nodeCount 個節點（每個含間距約 slotWidth）剛好塞入畫布可視寬，
 * 使節點不至於過大重疊、且好對準連線點拉線。
 */
export interface FitZoomOptions {
  /** 畫布可視寬度（px）；不可用（≤0 或非有限值）時回傳 fallbackZoom */
  canvasWidth: number
  /** 目標容納節點數 */
  nodeCount?: number
  /** 單一節點含水平間距的估算寬（節點約 200 + 間距約 60） */
  slotWidth?: number
  /** 縮放下限 */
  minZoom?: number
  /** 縮放上限 */
  maxZoom?: number
  /** canvasWidth 不可用時（如測試、尚未掛載）的固定退回值 */
  fallbackZoom?: number
}

/** 預設參數：集中管理，避免魔術數字散落於呼叫端 */
export const FIT_ZOOM_DEFAULTS = {
  nodeCount: 5,
  slotWidth: 260,
  minZoom: 0.3,
  maxZoom: 1,
  fallbackZoom: 0.8,
} as const

/**
 * 純函式：不觸碰 DOM。
 * zoom = clamp(canvasWidth / (nodeCount * slotWidth), minZoom, maxZoom)
 * canvasWidth 不可用時回傳 fallbackZoom。
 */
export function computeFitZoom(options: FitZoomOptions): number {
  const {
    canvasWidth,
    nodeCount = FIT_ZOOM_DEFAULTS.nodeCount,
    slotWidth = FIT_ZOOM_DEFAULTS.slotWidth,
    minZoom = FIT_ZOOM_DEFAULTS.minZoom,
    maxZoom = FIT_ZOOM_DEFAULTS.maxZoom,
    fallbackZoom = FIT_ZOOM_DEFAULTS.fallbackZoom,
  } = options

  if (!Number.isFinite(canvasWidth) || canvasWidth <= 0) return fallbackZoom

  const raw = canvasWidth / (nodeCount * slotWidth)
  return Math.min(maxZoom, Math.max(minZoom, raw))
}
