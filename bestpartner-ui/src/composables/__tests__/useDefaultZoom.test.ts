import { describe, it, expect } from 'vitest'
import { computeFitZoom, FIT_ZOOM_DEFAULTS } from '../useDefaultZoom'

/** 「剛好容納 nodeCount 個節點」的畫布寬度；由預設值推導，避免寫死而綁死節點尺寸 */
const EXACT_FIT_WIDTH = FIT_ZOOM_DEFAULTS.nodeCount * FIT_ZOOM_DEFAULTS.slotWidth

describe('computeFitZoom', () => {
  it('canvasWidth 剛好等於 nodeCount 個 slot 寬時，縮放為上限 1', () => {
    expect(computeFitZoom({ canvasWidth: EXACT_FIT_WIDTH })).toBe(1)
  })

  it('較窄畫布回傳依比例縮小的縮放', () => {
    const width = EXACT_FIT_WIDTH * 0.7
    expect(computeFitZoom({ canvasWidth: width })).toBeCloseTo(0.7, 4)
  })

  it('較寬畫布仍被上限 maxZoom 夾住（不放大超過 1）', () => {
    expect(computeFitZoom({ canvasWidth: EXACT_FIT_WIDTH * 2 })).toBe(1)
  })

  it('極窄畫布不低於 minZoom', () => {
    // 比例遠低於 minZoom → clamp
    expect(computeFitZoom({ canvasWidth: EXACT_FIT_WIDTH * 0.05 })).toBe(FIT_ZOOM_DEFAULTS.minZoom)
  })

  it('canvasWidth 為 0 時回傳 fallbackZoom', () => {
    expect(computeFitZoom({ canvasWidth: 0 })).toBe(FIT_ZOOM_DEFAULTS.fallbackZoom)
  })

  it('canvasWidth 為負數時回傳 fallbackZoom', () => {
    expect(computeFitZoom({ canvasWidth: -500 })).toBe(FIT_ZOOM_DEFAULTS.fallbackZoom)
  })

  it('canvasWidth 為 NaN 時回傳 fallbackZoom', () => {
    expect(computeFitZoom({ canvasWidth: Number.NaN })).toBe(FIT_ZOOM_DEFAULTS.fallbackZoom)
  })

  it('canvasWidth 為 Infinity 時回傳 fallbackZoom', () => {
    expect(computeFitZoom({ canvasWidth: Number.POSITIVE_INFINITY })).toBe(
      FIT_ZOOM_DEFAULTS.fallbackZoom,
    )
  })

  it('可覆寫 nodeCount 與 slotWidth', () => {
    // 3 個節點、每個 300 寬 → 900 / 900 = 1
    expect(computeFitZoom({ canvasWidth: 900, nodeCount: 3, slotWidth: 300 })).toBe(1)
  })

  it('可覆寫 fallbackZoom', () => {
    expect(computeFitZoom({ canvasWidth: 0, fallbackZoom: 0.5 })).toBe(0.5)
  })
})
