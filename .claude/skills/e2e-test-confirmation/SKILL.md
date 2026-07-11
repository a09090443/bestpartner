---
name: e2e-test-confirmation
description: Use when user wants to run the BestPartner UI-driven E2E test plan, execute Playwright end-to-end tests, or produce an E2E test report. Triggered by "執行 E2E", "E2E 測試", "端到端測試", "前端測試", "Playwright 測試", "跑 E2E", "workflow UI 測試", "run e2e", "e2e test".
---

# BestPartner E2E 測試確認表產生器

## Overview

每次 E2E 測試週期，依 `docs/e2e-test-plan.md` 的旅程矩陣（J1–J7），**基於固定模板** `./e2e-test-checklist.md` 建立一份帶日期的確認記錄檔。**每份測試報告都從該清單複製產出**，不得另行設計格式。

E2E 以 Playwright 驅動真實瀏覽器，對 **port 80 真實後端 + PostgreSQL** 全貫穿；J5 執行案例使用**真實 LLM**（斷言看 SSE 事件序列與節點狀態，不比對輸出文字）。

## When to Use

- 使用者說「執行 E2E」、「跑端到端測試」、「E2E 測試報告」
- 定期回歸前要對 workflow UI 主軸做貫穿驗證
- 前後端契約變更後，要確認使用者路徑仍接得上

## 事實來源

- 測試範圍與旅程定義：`docs/e2e-test-plan.md`（權威）
- 報告模板：`.claude/skills/e2e-test-confirmation/e2e-test-checklist.md`
- 兩者若衝突，以 `docs/e2e-test-plan.md` 為準；發現漂移時先更新模板再測。

## Process（必須依序執行）

### Step 1：詢問 metadata

**測試日期與檔名自動取當下時間，不需詢問使用者。** 向使用者確認：

```
- 測試環境（dev / sit）
- 瀏覽器（chromium / firefox / webkit，可多選；預設 chromium）
- LLM 平台與 E2E_LLM_ID（J5 真實執行所需；若缺，J5 真實案例將標 ⏭️）
- 前端 baseURL（預設 http://localhost:4173）與後端 API URL（預設 http://localhost:80）
```

### Step 2：建立帶日期時間的新檔案

**檔名格式固定**：`docs/test-confirmations/e2e-test-confirmation-YYYYMMDDHHmm.md`

> 前綴 `e2e-` 用以與 API 的 `test-confirmation-` 報告區隔。

**以 `./e2e-test-checklist.md` 完整內容為基礎複製**，不要自行設計表格。

### Step 3：填入 metadata

只修改「測試週期資訊」表格（日期、環境、瀏覽器、LLM 平台、E2E_LLM_ID、baseURL）。其餘所有旅程表格的「狀態」與「證據 / 備註」欄位保持空白。

### Step 4：前置檢查（測試前必要，任一失敗即停止並回報）

依模板「前置檢查清單」逐項確認：

1. 後端服務於 port 80 健康回應（HTTP 200）。
2. PostgreSQL 已初始化，種子資料存在（`admin`、`test_user` 及其 LLM setting）。
3. 前端已 build 並可由 baseURL 存取。
4. Playwright 已安裝瀏覽器（`npx playwright install`）。
5. `storageState` 可由 API 登入產生（admin/admin）。

> ⚠️ 未完成前置檢查即開始，視同 E2E 流程違規。

### Step 5：逐旅程執行循環（J1 → J7 依序）

對每條旅程的每個案例：

1. **執行**：跑對應 Playwright spec / 手動驅動瀏覽器完成該案例。
2. **每個步驟都截圖（強制）**：案例的**每一個操作步驟**（如登入頁載入、輸入帳密、送出、導頁結果）都必須擷取一張畫面截圖，依序命名存入本報告專屬截圖目錄：
   `docs/test-confirmations/e2e-shots/<報告時間戳>/<caseId>-<步驟序號>-<簡述>.png`
   （例：`e2e-shots/202607111530/J1-02-01-login-page.png`、`J1-02-02-submit.png`、`J1-02-03-redirected-list.png`）。
   - Playwright 用 `page.screenshot({ path })`；J5 執行過程須含 ExecutionResultDrawer 各節點狀態轉移的截圖。
   - 失敗（❌）案例額外附失敗當下截圖與 trace。
3. **立即記錄證據（以 Markdown 圖片連結呈現）**：在證據欄依序嵌入該案例**每個步驟的圖片連結**，使用 Markdown 圖片語法（相對於報告檔的路徑），讓報告可直接預覽／點開：
   `![J1-02-01 login-page](e2e-shots/<報告時間戳>/J1-02-01-login-page.png)`
   多步驟依序並列多個圖片連結；另可附 trace / 錄影檔連結、關鍵斷言或 SSE 事件序列（不得使用佔位符、不得省略、不得只放一張代表圖）。
4. **標記狀態**（✅ / ❌ / ⏭️）。

> ⚠️ **每個步驟都要有截圖，且以 Markdown 圖片連結附在該案例的證據欄**；缺任一步驟圖片連結、或只填純文字路徑而未用圖片語法，視同違規。
> ⚠️ **必須先寫入證據（含逐步圖片連結），再標記狀態。**
> J5 真實 LLM 案例：記錄 SSE 事件序列（`execution.started`→`node.*`→`execution.completed`）、各節點狀態轉移截圖與 DB 執行紀錄是否寫入，**不比對輸出文字**。

### Step 5.5：每條旅程完成後立即更新摘要（強制，不可延後）

每完成一條旅程所有案例，立即依模板「旅程完成檢查清單」逐項勾選並更新摘要表統計與 Pass 率。

> ⚠️ 不可等全部旅程測完才統一更新；違反視同流程違規。

### Step 6：測試資料清理（強制）

E2E 建立的 workflow 一律以 `e2e-<caseId>-<runTag>` 前綴命名，測試中經 `workflow/delete` API 刪除；收尾時以前綴掃描 `workflow/list` 兜底清除任何殘留 `e2e-*`。

> ⚠️ 不得另建備份檔／目錄；殘留測試資料視為流程不完整（遵守「備份只能寫 `docs/sql/bestpartner-init-data.sql`」鐵則）。

### Step 7：收尾回報

回報：報告檔路徑、狀態符號說明、Pass 率摘要、❌ 項目已入「問題追蹤區」、測試資料已清理、Playwright report / trace artifact 位置。

---

## 常見錯誤

> 步驟內 ⚠️ 違規警告為必讀，本表僅列步驟未涵蓋的概念性錯誤。

| 錯誤 | 正確做法 |
|------|---------|
| 建立固定名稱 `e2e-checklist.md` | 使用帶日期的 `docs/test-confirmations/e2e-test-confirmation-YYYYMMDDHHmm.md` |
| 自行設計表格格式 | 從 `./e2e-test-checklist.md` 完整複製 |
| J5 比對 LLM 輸出文字 | 只驗 SSE 事件序列、節點狀態、DB 紀錄 |
| 缺 E2E_LLM_ID 仍硬跑 J5 真實案例 | 標 ⏭️ 並在備註說明；其餘旅程照跑 |
| 只放最終畫面一張截圖 | 每個操作步驟都要一張，依序存入 e2e-shots/<報告時間戳>/ |
| 證據欄只填純文字路徑 | 用 Markdown 圖片語法 `![](...)` 嵌入圖片連結，讓報告可預覽 |
| 先標狀態再補證據 | 先記錄逐步圖片連結 / trace / 事件序列，才標狀態 |
| 全部旅程測完才更新摘要 | 每條旅程完成立即更新（Step 5.5） |
| 測試殘留 `e2e-*` workflow 未清 | 以前綴掃描兜底刪除（Step 6） |

## 維護規則

- `docs/e2e-test-plan.md` 的旅程／案例異動時，本 skill 的 `e2e-test-checklist.md` 模板須同步更新（由 `documentation-sync` 把關）。
- 涉及 API 契約變更時，另同步 `docs/api-test-plan.md` 與 `.claude/rules/api-endpoints.md`。
