# 畫布預設縮放設計：容納 5 個節點的初始視野

- 日期：2026-07-09
- 範圍：`bestpartner-ui` workflow 畫布（VueFlow）
- 狀態：待使用者 review

## 目標

Workflow 畫布在**所有情境**（新建空白流程、載入既有流程、整理版面）下，預設檢視縮放都能舒適橫向容納約 **5 個節點且不重疊**，並維持正常拉線（連線）操作。

## 問題背景

- VueFlow 畫布本身是無限延伸、可平移縮放的，「畫布大小」實際上等同於「初始檢視縮放與視野」。
- 目前設定 `fit-view-on-init`：載入既有流程會自動框住所有節點，但只有 1–2 個節點時會被放到過大；空白新流程沒有節點，`fitView` 不作用，落在 VueFlow 預設 `zoom = 1`，導致拖入的節點顯得過大、彼此易擠在一起。
- 需求是讓預設視野在各情境下都以「約 5 個節點寬」為基準，節點不至於過大重疊，且好拉線。

## 設計

### 核心：動態計算縮放（方案 B），量測失敗時退回固定值（方案 A）

新增純函式（放入 `composables/`，例如 `useCanvasLayout.ts` 或新檔 `useDefaultZoom.ts`）：

```
computeFitZoom({
  canvasWidth,          // 畫布可視寬度（px）
  nodeCount = 5,        // 目標容納節點數
  slotWidth = 260,      // 單一節點含水平間距的估算寬（節點約 200 + 間距約 60）
  minZoom = 0.3,
  maxZoom = 1,
  fallbackZoom = 0.8,   // canvasWidth 不可用時的固定退回值
}) => number
```

- 公式：`zoom = clamp(canvasWidth / (nodeCount * slotWidth), minZoom, maxZoom)`。
- `canvasWidth` ≤ 0 或非有限值（測試 / 尚未掛載）時，回傳 `fallbackZoom`。
- 純函式、不觸碰 DOM，易於單元測試。

### WorkflowEditorView 整合

1. canvas 容器 `<div class="canvas">` 加 `ref`，`onMounted` 量測 `clientWidth` 得 `canvasWidth`，算出 `defaultZoom`。
2. **空白新流程**（`store.createNew()`）：以 `setViewport({ x, y, zoom: defaultZoom })` 定位，x/y 給合理留白讓左上角有空間放第一個節點。
3. **載入既有流程**：載入完成後改以手動 `fitView({ maxZoom: defaultZoom, padding: 0.2 })`，確保少數節點不被放大超過預設縮放。
4. **整理版面**（`handleTidyUp`）：現有的 `fitView()` 一併帶入 `{ maxZoom: defaultZoom, padding: 0.2 }`，行為一致。
5. 縮放 / 間距相關數值（`slotWidth`、`fallbackZoom`、`minZoom`、`maxZoom`、`padding`）以具名常數集中，避免魔術數字散落。

### 拉線（畫線）

- 連線點（`Handle`）與 `onConnect` 目前運作正常，**不需修改**。縮放調整到舒適區間後，反而更好對準連線點拉線。

## 測試

- 對 `computeFitZoom` 寫 vitest 單元測試：
  - 一般值：例如 `canvasWidth = 1300`、`slotWidth = 260`、`nodeCount = 5` → `zoom = 1`（clamp 上限）。
  - 較窄畫布：例如 `canvasWidth = 900` → `zoom ≈ 0.69`。
  - `canvasWidth = 0` / `NaN` → 回傳 `fallbackZoom`（0.8）。
  - clamp 下限：極窄畫布不低於 `minZoom`。
- 專案既有 vitest 測試架構（`src/**/__tests__/`）可直接沿用。

## 不做（YAGNI）

- 不自動預放範本節點（使用者選定「調整縮放」而非「放入範本節點」情境）。
- 不修改連線點 / handle 邏輯（畫線本來正常）。
- 不做垂直方向容納計算（節點以左至右水平流為主）。
- 不改動存檔 / 驗證 / 後端契約。

## 影響檔案（預估）

- 新增：`bestpartner-ui/src/composables/useDefaultZoom.ts`（或併入 `useCanvasLayout.ts`）+ 對應 `__tests__`。
- 修改：`bestpartner-ui/src/views/WorkflowEditorView.vue`（canvas ref、onMounted 量測、三處視野套用）。
