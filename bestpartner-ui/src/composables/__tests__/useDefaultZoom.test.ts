import { describe, it, expect } from 'vitest'
import { computeFitZoom, FIT_ZOOM_DEFAULTS } from '../useDefaultZoom'

describe('computeFitZoom', () => {
  it('canvasWidth 剛好等於 5 個 slot 寬時，縮放為上限 1', () => {
    // 5 * 260 = 1300
    expect(computeFitZoom({ canvasWidth: 1300 })).toBe(1)
  })

  it('較窄畫布回傳依比例縮小的縮放', () => {
    // 900 / 1300 ≈ 0.6923
    expect(computeFitZoom({ canvasWidth: 900 })).toBeCloseTo(0.6923, 4)
  })

  it('較寬畫布仍被上限 maxZoom 夾住（不放大超過 1）', () => {
    expect(computeFitZoom({ canvasWidth: 3000 })).toBe(1)
  })

  it('極窄畫布不低於 minZoom', () => {
    // 100 / 1300 ≈ 0.077 → clamp 至 0.3
    expect(computeFitZoom({ canvasWidth: 100 })).toBe(FIT_ZOOM_DEFAULTS.minZoom)
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
