---
name: e2e-test-confirmation
description: Use when user wants to run the BestPartner UI-driven E2E test plan, execute Playwright end-to-end tests, or produce an E2E test report. Triggered by "執行 E2E", "E2E 測試", "端到端測試", "前端測試", "Playwright 測試", "跑 E2E", "workflow UI 測試", "run e2e", "e2e test".
---

# BestPartner E2E 測試確認表產生器

## Overview

每次 E2E 測試週期，依 `docs/e2e-test-plan.md` 的旅程矩陣（J1–J9），**基於固定模板** `./e2e-test-checklist.md` 建立一份帶日期的確認記錄檔。**每份測試報告都從該清單複製產出**，不得另行設計格式。

E2E 執行入口為 **`webwright` skill**（code-as-action、一次一個 bash 指令驅動本機 Playwright，截圖與操作記錄產於 `final_runs/run_<id>/`），對 **port 80 真實後端 + PostgreSQL** 全貫穿；J5 執行案例使用**真實 LLM**（斷言看 SSE 事件序列與節點狀態，不比對輸出文字）。

> `bestpartner-ui/e2e/` 的 Playwright spec harness **僅保留為選擇器/旅程參考**（`data-test`、`data-node-type`、DnD 手法、`fixtures/db.ts`），**不再作為執行入口**。實際 E2E 一律透過 webwright 驅動。

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
- 本次要用的資源（供 Step 4.5 前置確認）：
    · LLM 平台與 E2E_LLM_ID（可留空，由 Step 4.5 依 alias/platform 動態解析）
    · 若旅程涉及 MCP server / tool / skill，列出要用的名稱（同樣由 Step 4.5 解析真實 ID）
- 前端 baseURL（預設 http://localhost:4173，dev server 則 http://localhost:5173）與後端 API URL（預設 http://localhost:80）
```

### Step 2：建立帶日期時間的新檔案

**檔名格式固定**：`docs/test-confirmations/e2e-test-confirmation-YYYYMMDDHHmm.md`

> 前綴 `e2e-` 用以與 API 的 `test-confirmation-` 報告區隔。

**以 `./e2e-test-checklist.md` 完整內容為基礎複製**，不要自行設計表格。

### Step 3：填入 metadata

只修改「測試週期資訊」表格（日期、環境、瀏覽器、LLM 平台、E2E_LLM_ID、baseURL）。其餘所有旅程表格的「狀態」與「證據 / 備註」欄位保持空白。

### Step 4：服務生命週期與前置檢查（測試前必要，強制；任一失敗即停止並回報）

**先停相關服務再重啟**（不對殘留/舊狀態服務測試），再逐項前置檢查：

1. **停服務**（後端 port 80 + 前端）：
   ```powershell
   Get-NetTCPConnection -LocalPort 80,5173,4173 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
   ```
2. **起後端**（`bestpartner-service/`，dev profile，port 80，背景執行）：
   ```bash
   java -Dquarkus.profile=dev -jar build/bestpartner-service-0.1.8-SNAPSHOT-runner.jar
   ```
   > ⚠️ `-Dquarkus.profile=dev` 必須在 `-jar` **之前**，否則打包 jar 走 prod、dev 專屬鍵（如 `file.upload.dir`）缺失而啟動失敗。若後端程式有異動，先重編（見 API 版 `test-confirmation` Step 5）。
3. **起前端**（`bestpartner-ui/`，背景執行）：`npm run dev`（vite 5173）或 `npm run preview`（4173，與預設 baseURL 一致）。
4. **健康檢查**：`GET http://localhost:80/systemSetting/list` == 200；前端 baseURL 可存取。
5. PostgreSQL 已初始化，種子資料存在（`admin`、`test_user` 及其 LLM setting）。
6. webwright 可用（底層 Playwright 瀏覽器已安裝：`npx playwright install chromium`）。

> ⚠️ 未先停→重啟服務即開始、或前置任一項未過即開始，視同 E2E 流程違規。

### Step 4.5：資源與認證前置確認（API smoke，強制；失敗即停止或標 ⏭️）

**測試前先確認本次要用的資源存在、其真實 ID、且認證（token / api_key）可用**，避免中途才因資源漂移或 api_key 失效而失敗。以 API smoke 進行：

1. **登入取 JWT**：`POST /login/`，body `{"email":"admin@bestpartner.com.tw","password":"admin"}`（email 欄位為 `type=email`，須用真實 email）。
2. **解析真實 ID（禁止硬編；運行 DB 與 `docs/sql` 種子檔會漂移）**：
   - LLM：`POST /llm/setting/get`（空 body）→ 依 alias/platform 取 `llmId`（例：OpenRouter CHAT 於本機為 `1ee80ffa…`，種子檔為 `583b9222…`）。
   - MCP：`GET /llm/mcpServer/list` → 依 name 取 `mcpId`。
   - tool：`GET /llm/tool/list` → 依 name 取 `toolId`（例：`DateTool`）。
   - skill：`GET /llm/skill/list` → 依 name 取 `skillId`。
3. **認證可用性驗證**：對要用的 LLM 發一次最小 `POST /llm/chat`（帶解析到的 `llmId` + 簡短 message，需帶 `Authorization: Bearer <JWT>`），HTTP 200 且 body 非錯誤 → api_key 可用。
4. 將解析到的 ID 與驗證結果寫入報告「測試週期資訊」。
5. **失敗處理**：資源缺或認證不通 → 依賴該資源的旅程標 ⏭️ 並在備註說明；若為 J5 等核心旅程所需則**停止並回報**。

> ⚠️ 未做資源/認證前置確認即開測，視同違規。
> 替代/加強：必要時可另跑 backend `@QuarkusTest`（如 `LLMServiceTest` 或 assistant 相關）深驗真實 LLM/MCP 呼叫（非預設）。

### Step 5：逐旅程執行循環（J1 → J9 依序）

**執行入口為 `webwright` skill**（非 Playwright spec）。對每條旅程的每個案例：

1. **執行**：呼叫 `webwright` skill 以 code-as-action（一次一個 bash 指令）驅動本機 Playwright 完成該案例。用 Step 4.5 解析到的真實 ID 與 `bestpartner-ui/e2e/` 參考 harness 的選擇器（`data-test` / `data-node-type`）、DnD 手法（palette→畫布為 HTML5 原生 DnD 合成事件；連線為 pointer 序列，見 `e2e-test-plan.md` §6）。
2. **每個步驟都截圖（強制）**：webwright 於每個操作步驟存截圖至 `final_runs/run_<id>/`。將該案例**每一步**截圖依序**複製**到本報告專屬目錄並命名：
   `docs/test-confirmations/e2e-shots/<報告時間戳>/<caseId>-<步驟序號>-<簡述>.png`
   （例：`J1-02-01-login-page.png`、`J1-02-02-submit.png`、`J1-02-03-redirected-list.png`）。
   - J5 執行過程須含 ExecutionResultDrawer 各節點狀態轉移的截圖。
   - 失敗（❌）案例額外附失敗當下截圖與 webwright 操作記錄。
3. **DB 落庫斷言（J5，Q2=B）**：以 bash 步驟直連 Postgres 驗 `llm_workflow_execution` / `llm_workflow_node_execution`（`psql`，或沿用 `bestpartner-ui/e2e/fixtures/db.ts` 當獨立 node 腳本）。
4. **立即記錄證據（以 Markdown 圖片連結呈現）**：在證據欄依序嵌入該案例**每個步驟的圖片連結**，使用 Markdown 圖片語法（相對於報告檔的路徑）：
   `![J1-02-01 login-page](e2e-shots/<報告時間戳>/J1-02-01-login-page.png)`
   多步驟依序並列；另附 webwright 操作記錄連結、關鍵斷言或 SSE 事件序列（不得使用佔位符、不得省略、不得只放一張代表圖）。
5. **標記狀態**（✅ / ❌ / ⏭️）。

> ⚠️ **每個步驟都要有截圖，且以 Markdown 圖片連結附在該案例的證據欄**；缺任一步驟圖片連結、或只填純文字路徑而未用圖片語法，視同違規。
> ⚠️ **必須先寫入證據（含逐步圖片連結），再標記狀態。**
> J5 真實 LLM 案例：記錄 SSE 事件序列（`execution.started`→`node.*`→`execution.completed`）、各節點狀態轉移截圖與 DB 執行紀錄是否寫入，**不比對輸出文字**。

### Step 5.5：每條旅程完成後立即更新摘要（強制，不可延後）

每完成一條旅程所有案例，立即依模板「旅程完成檢查清單」逐項勾選並更新摘要表統計與 Pass 率。

> ⚠️ 不可等全部旅程測完才統一更新；違反視同流程違規。

### Step 6：測試資料清理（強制）

E2E 建立的 workflow 一律以 `e2e-<caseId>-<runTag>` 前綴命名。**建立時把 id 記錄下來，收尾只刪這些 id**；需要兜底掃描時必須帶本次的 `runTag`。

> ⚠️ 不得另建備份檔／目錄；殘留測試資料視為流程不完整（遵守「備份只能寫 `docs/sql/bestpartner-init-data.sql`」鐵則）。

> 🚫 **絕不可依「使用者也會自然產生的名稱」批次刪除**（實際事故：曾把 `name === '未命名流程'` 加進清理條件，
> 而那是使用者未命名就存檔的預設名稱，結果刪掉了使用者自己的流程）。
> 名稱樣式無法區分測試資料與使用者資料；**唯一安全依據是本次執行記錄下來的 id**。
> 同理，執行紀錄（`llm_workflow_execution` / `..._node_execution`）不隨 workflow 連鎖刪除，
> 要清也只能以本次產生的 execution id 為範圍，不可依日期或「孤兒」條件整批刪。

> 若使用者要求**保留**測試流程供其手動驗證，以其指示為準：不做清理、流程名稱避開 `e2e-` 前綴
> （免得日後任何一輪的兜底掃描把它清掉），並於回報中明確標示保留了哪些資料。

### Step 6.5：測試後關閉服務（強制）

**所有旅程執行完畢後，無論成敗一定關閉服務**（後端 + 前端）：

```powershell
Get-NetTCPConnection -LocalPort 80,5173,4173 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
```

> ⚠️ 測試完成後不得遺留後端 / 前端服務在背景執行。

### Step 7：收尾回報

回報：報告檔路徑、狀態符號說明、Pass 率摘要、❌ 項目已入「問題追蹤區」、測試資料已清理、**服務已關閉**、webwright `final_runs/run_<id>/` 操作記錄與截圖位置。

---

## 常見錯誤

> 步驟內 ⚠️ 違規警告為必讀，本表僅列步驟未涵蓋的概念性錯誤。

| 錯誤 | 正確做法 |
|------|---------|
| 用 Playwright spec（`npx playwright test`）當執行入口 | E2E 執行一律走 `webwright` skill；`bestpartner-ui/e2e/` 僅為選擇器/旅程參考 |
| 未做資源/認證前置就開測 | 先跑 Step 4.5 API smoke：解析真實 ID + 驗 api_key 可用 |
| 硬編 llmId/toolId | 由 Step 4.5 依 alias/platform/name 動態解析（運行 DB 與種子檔會漂移） |
| 未停→重啟服務即開測 | Step 4 先停後端(80)+前端再重啟並健康 200 |
| 測後未關服務 | Step 6.5 強制關閉後端與前端 |
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

- **程式碼修改後必檢視測試清單（強制）**：任何前端 UI／workflow 節點型別／執行事件／後端 API 的程式變更，**宣告完成前**必須檢視本清單是否需新增或調整確認項目——
  - 新增或改動使用者可操作的畫面／流程 → 檢查是否要新增旅程案例（Jx-nn）或修改既有案例的預期。
  - 若需要，先更新 `docs/e2e-test-plan.md` 的旅程矩陣，再同步 `e2e-test-checklist.md` 模板，最後才據以測試。
  - 判定「不需新增」時，仍須在該次變更說明中明述已檢視且無需調整，不得略過。
- `docs/e2e-test-plan.md` 的旅程／案例異動時，本 skill 的 `e2e-test-checklist.md` 模板須同步更新（由 `documentation-sync` 把關）。
- 涉及 API 契約變更時，另同步 `docs/api-test-plan.md` 與 `.claude/rules/api-endpoints.md`。
