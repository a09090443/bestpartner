---
name: e2e-test-confirmation
description: Use when user wants to run the BestPartner UI-driven E2E test plan, execute Playwright end-to-end tests, or produce an E2E test report. Triggered by "執行 E2E", "E2E 測試", "端到端測試", "前端測試", "Playwright 測試", "跑 E2E", "workflow UI 測試", "run e2e", "e2e test".
---

# BestPartner E2E 測試確認表產生器

## Overview

每次 E2E 測試週期，依 `docs/e2e-test-plan.md` 的旅程矩陣（J1–J10），**基於固定模板** `./e2e-test-checklist.md` 建立一份帶日期的確認記錄檔。**每份測試報告都從該清單複製產出**，不得另行設計格式。

> 本流程的關卡（開測前停服務、收工前報告與關服務）由 `.claude/hooks/e2e-flow-guard.ps1` 機械化把關：
> 未確實停過服務就呼叫 webwright 會被 PreToolUse 擋下（exit 2）；報告未完成或服務未關就想收工會被 Stop hook 擋下。
> 說明見 [`.claude/hooks/README.md`](../../hooks/README.md)。

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

### Step 1.5：確認本次測試範圍（強制）

**用 `AskUserQuestion` 讓使用者複選本次要測的旅程**，不得逕自全跑或自行縮減：

| 旅程 | 優先 | 案例數 | 主題 |
|------|:---:|:---:|------|
| J1 | P0/P1/P2 | 9 | 認證流程 |
| J2 | P0/P1 | 6 | Workflow 列表 |
| J3 | P0/P1 | 9 | 畫布編輯 |
| J4 | P1 | 12 | 驗證與啟用 |
| J5 | P0/P1 | 12 | 執行 workflow（真實 LLM） |
| J6 | P0/P1/P2 | 9 | Inspector 表單 |
| J7 | P2 | 2 | 契約錯誤 |
| J8-A / J8-B | P1 | 5 / 5 | 知識庫 RAG（pipeline / 外掛） |
| J9 | P0/P1/P2 | 17 | 編輯器外觀與節點編輯頁 |
| J10 | P1/P2 | 8 | 複合能力掛載（旅遊行程規劃） |
| J11 | P0/P1/P2 | 8 | 純資料管線（**無 LLM，確定性斷言**） |
| J12 | P1/P2 | 7 | 迴圈批次處理（LOOP） |
| J13 | P1/P2 | 5 | 條件分流資料管線（CONDITION） |
| J14 | P1/P2 | 6 | CODE 節點沙箱（含安全性驗證） |
| J15 | P1/P2 | 9 | 知識庫＋MCP 併掛同一 LLM（Milvus + MCP + 真實 LLM） |

- 選項至少提供「全部（129 案例）」「核心 P0（J1/J2/J3/J5）」「不需 LLM 金鑰（J1/J2/J3/J4/J6/J7/J9/J11/J12/J13/J14）」「**能力面（J5/J8/J10/J15）**」「依變更範圍」五種常見組合，並允許自訂。
- **依變更範圍**時，先看 `git diff --name-only HEAD` 推薦旅程（如 `inspector/`→J6、`canvas/`→J3+J9、`workflow/executor/`→J5、RAG 相關→J8、能力節點→J10、`HttpRequestExecutor`/`DataTransformExecutor`→J11、`LoopExecutor`→J12、`ConditionExecutor`→J13、`CodeExecutor`→J14、`LlmAssistantExecutor`/`McpServerService`/`ToolService` 的能力建構→J10+J15），仍須讓使用者確認。
- **J11–J14 不需任何 LLM／embedding 金鑰**（全程無模型呼叫），是金鑰不可用時仍能驗證執行引擎的唯一選擇；
  其中 **J11 為確定性回歸基準**，引擎壞掉時會比任何 LLM 旅程更早紅燈，建議任何範圍組合都納入。
- 選定後回寫狀態檔，供 hook 與收尾檢查使用：
  ```powershell
  pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Mark -Field journeys -Value "J1,J5"
  ```
- **未選中的旅程在確認表整段標 ⏭️，備註「本次範圍外」**，不得留空白也不得刪除該段。

> ⚠️ 未確認範圍即開測，視同 E2E 流程違規。

### Step 2：建立帶日期時間的新檔案

**檔名格式固定**：`docs/test-confirmations/e2e-test-confirmation-YYYYMMDDHHmm.md`

> 前綴 `e2e-` 用以與 API 的 `test-confirmation-` 報告區隔。

**以 `./e2e-test-checklist.md` 完整內容為基礎複製**，不要自行設計表格。

建檔後立即回寫報告路徑與本輪標記（供 Stop hook 檢查收尾完成度）：

```powershell
pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Mark -Field reportPath -Value "docs/test-confirmations/e2e-test-confirmation-YYYYMMDDHHmm.md"
pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Mark -Field runTag -Value "YYYYMMDDHHmm"
```

### Step 3：填入 metadata

只修改「測試週期資訊」表格（日期、runTag、**本次測試範圍與未選原因**、測試輪次、環境、瀏覽器、LLM 平台、E2E_LLM_ID、baseURL）。其餘所有旅程表格的「狀態」與「證據 / 備註」欄位保持空白；未選旅程於該段開頭註明「本次範圍外」並把狀態填 ⏭️。

### Step 4：服務生命週期與前置檢查（測試前必要，強制；任一失敗即停止並回報）

**先停相關服務再重啟**（不對殘留/舊狀態服務測試），再逐項前置檢查：

1. **停服務**（後端 port 80 + 前端）：
   ```powershell
   Get-NetTCPConnection -LocalPort 80,5173,4173 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
   ```
   停完後標記（**該指令會實測三個 port 是否真的沒有 listener，未通過會 exit 1**）：
   ```powershell
   pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Mark -Field phase -Value services-stopped
   ```
   > 這一步不是形式：未通過標記就呼叫 `webwright`，PreToolUse hook 會以 exit 2 擋下，測試無法開始。
   > 殘留的 Playwright / chromium 行程若佔用上述 port 也會一併被停掉。
2. **起後端**（`bestpartner-service/`，dev profile，port 80，背景執行）：
   ```bash
   java -Dquarkus.profile=dev -jar build/bestpartner-service-0.1.8-runner.jar
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

### Step 5：逐旅程執行循環（依 Step 1.5 選定範圍，J 編號由小到大）

**執行入口為 `webwright` skill**（非 Playwright spec）。**只執行選定範圍內的旅程**，未選旅程整段標 ⏭️「本次範圍外」。對範圍內每條旅程的每個案例：

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

### Step 5.7：失敗分流（每出現一個 ❌ 立即執行，不得累積到最後）

**先取證，再判定。** 至少取得下列三類訊號（取不到要在報告註明「無法取得」）：

| 訊號 | 取得方式 | 判「前端」 | 判「後端」 |
|------|---------|-----------|-----------|
| API 直呼比對 | 用 Step 4.5 的 JWT，對同一 endpoint 送與 UI 相同的 payload | API 回 2xx 且資料正確，但畫面沒反映 | API 自身 4xx/5xx，或回傳資料本身就錯 |
| 後端日誌 | `Get-Content D:/tmp/bestpartner/bestpartner.log -Tail 200`（`LOG_DIR` 可覆寫）與 `bestpartner_error.log`，比對失敗當下時間窗 | 時間窗內無相關 stacktrace | 時間窗內有 exception / stacktrace |
| 瀏覽器 console / network | webwright 收集 `page.on('console')`、`page.on('pageerror')`、`page.on('response')` | 有 JS 例外、未捕捉的 rejection、渲染錯誤 | network 有 4xx/5xx 而 console 無 JS 例外 |
| SSE 事件序列（J5/J8/J10） | 觀察 `execution.started → node.* → execution.completed` | 事件序列完整，但 ExecutionResultDrawer 未更新 | 序列缺 `execution.completed`，或節點 FAILED |
| DB 落庫 | 直連 Postgres 查 `llm_workflow_execution` / `llm_workflow_node_execution` | 有紀錄且正確、UI 顯示不出來 | 無紀錄或狀態錯 |

**另有兩類非產品缺陷，必須分開記，不可混入前端／後端**：

- `測試腳本`：選擇器漂移、等待時序、DnD 手法用錯、硬編 ID → 修 webwright 腳本或更新 `e2e-test-plan.md` §6 的選擇器清單。
- `環境`：api_key 失效、Milvus 未起、MCP jar 非 uber-jar、種子資料缺 → 相關案例標 ⏭️。

判定結果連同根因（指到檔案與函式）、修正方案寫入確認表的「問題追蹤區」與「問題分流與修正紀錄」。

### Step 5.8：修正提案與等待使用者確認（強制停點）

> ⚠️ **在使用者核准前，絕對不得修改 `bestpartner-service/src` 或 `bestpartner-ui/src`、`bestpartner-ui/e2e` 的檔案**——
> PreToolUse hook 會以 exit 2 擋下。

1. 把所有 `層別 ∈ {前端, 後端}` 的失敗整理成提案，逐項含：案例 ID／現象／分流判定與依據／根因（檔案:函式）／修正方案／影響面／風險。
2. 標記等待核准：
   ```powershell
   pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Mark -Field phase -Value awaiting-fix
   ```
3. 以 `AskUserQuestion` 或直接回報把提案交給使用者，然後**停下等待**（此時 Stop hook 不會擋，這是合法停點）。
4. 使用者回覆「同意 / 確認修正 / approve」→ hook 自動把 phase 轉為 `fixing`，此時才可改碼。
   回覆「不要修 / 先不修 / 列為已知問題」→ 登記為已知問題（狀態維持 ❌），不改碼、不重測，直接進 Step 6。

### Step 5.9：修正後完整重測（強制）

1. 依核准的方案修正（僅限提案內的檔案；超出範圍要回頭重新提案）。
2. 後端有改 → 重編 uber-jar：
   ```bash
   ./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=dev
   ```
   前端有改 → 重跑 `npm run build`（preview 模式）或重啟 dev server。
3. **回到 Step 4**：停服務 → `Mark -Field phase -Value services-stopped` → 起服務 → 健康檢查 → Step 4.5 smoke。
4. 標記輪次並**重跑 Step 1.5 選定的全部旅程**（不是只重測失敗案例——目的在抓修正引入的回歸）：
   ```powershell
   pwsh -NoProfile -File .claude/hooks/e2e-flow-guard.ps1 -HookEvent Mark -Field round -Value 2
   ```
   截圖存同一個 `e2e-shots/<報告時間戳>/` 下，檔名加 `R2-` 前綴。
   環境類 ⏭️ 案例維持 ⏭️，不需重試。
5. 結果寫入確認表的「第二輪重測結果」章節，含與第一輪的差異對照；**R1 ✅ → R2 ❌ 的新回歸必須回到 Step 5.7 重新分流**。
6. 第二輪仍有 ❌ → 重複 5.7～5.9，**最多三輪**；第三輪仍失敗一律轉為已知問題並收尾，不無限修測。

### Step 6：測試資料處置詢問（強制停點，清理前必做）

**所有旅程（含重測輪次）執行完畢、清理之前，必須用 `AskUserQuestion` 詢問使用者本次資料要清理還是保留**
（保留是為了讓使用者接著自行手動測試）。不得逕自清理，也不得逕自保留。多輪重測**只在最後一輪問一次**。

1. 整理**本次建立的資料清單**：workflow 名稱與 id（來自各案例執行時記錄的 id）、相關 execution id、上傳的 skill / 知識庫等。
2. 以 `AskUserQuestion` 提問，至少提供：全部清理（預設建議）／全部保留（供後續手動測試）／部分保留（列清單讓使用者挑）。
3. 依決議執行 Step 6.1（清理）或 Step 6.2（保留）。
4. 決議與保留清單寫入確認表的「測試資料處置」欄位，並於 Step 7 收尾回報明示。

> ⚠️ 未詢問即清理或即保留，視同 E2E 流程違規。完整規則見 [`test-data-retention.md`](../../rules/test-data-retention.md)。

### Step 6.1：測試資料清理（使用者選擇清理時）

E2E 建立的 workflow 一律以 `e2e-<caseId>-<runTag>` 前綴命名。**建立時把 id 記錄下來，收尾只刪這些 id**；需要兜底掃描時必須帶本次的 `runTag`。

> ⚠️ 不得另建備份檔／目錄；殘留測試資料視為流程不完整（遵守「備份只能寫 `docs/sql/bestpartner-init-data.sql`」鐵則）。

> 🚫 **絕不可依「使用者也會自然產生的名稱」批次刪除**（實際事故：曾把 `name === '未命名流程'` 加進清理條件，
> 而那是使用者未命名就存檔的預設名稱，結果刪掉了使用者自己的流程）。
> 名稱樣式無法區分測試資料與使用者資料；**唯一安全依據是本次執行記錄下來的 id**。
> 同理，執行紀錄（`llm_workflow_execution` / `..._node_execution`）不隨 workflow 連鎖刪除，
> 要清也只能以本次產生的 execution id 為範圍，不可依日期或「孤兒」條件整批刪。

### Step 6.2：測試資料保留（使用者選擇保留時）

1. **不做清理**：保留清單內的 workflow 與其 execution 紀錄一律不刪，兜底掃描要排除它們。
2. **改名避開兜底掃描（強制）**：把保留的 workflow 從 `e2e-<caseId>-<runTag>` 改成**不含 `e2e-` 前綴**的名稱
   （建議 `keep-<runTag>-<原名>`，以 `POST /llm/workflow/update` 改 name）——否則日後任何一輪的兜底掃描會把它清掉。
3. **部分保留**時，未被選中保留的資料照 Step 6.1 清理。
4. 保留資料的後續清理責任歸使用者；不得在後續測試輪次的前置或兜底清理中自動刪除它。
5. 在確認表「測試資料處置」欄位記錄：決議、保留清單（名稱／id）、改名後的名稱、保留理由。

### Step 6.5：測試後關閉服務（強制）

**所有旅程執行完畢後，無論成敗、無論資料保留與否，一定關閉服務**（後端 + 前端）：

```powershell
Get-NetTCPConnection -LocalPort 80,5173,4173 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
```

> ⚠️ 測試完成後不得遺留後端 / 前端服務在背景執行。
> 關閉後 Stop hook 會檢查 port 是否真的淨空；仍有 listener 會擋下收工。
>
> ⚠️ **保留資料 ≠ 保留服務**：使用者選擇保留資料時服務照關，回報中附上重啟指令供其手動測試：
> ```bash
> cd bestpartner-service && java -Dquarkus.profile=dev -jar build/bestpartner-service-0.1.8-runner.jar
> cd bestpartner-ui && npm run preview
> ```

### Step 7：收尾回報

先把確認表補完（**「測試結束時間」必填**，否則 Stop hook 會擋下收工），再回報：

- 報告檔路徑、狀態符號說明
- **本次選定範圍**與未選旅程
- 各輪 Pass 率（R1 / R2…）與最終結論
- 問題分流與修正摘要（前端 / 後端 / 測試腳本 / 環境各幾件、改了哪些檔案）
- ❌ 項目已入「問題追蹤區」、**測試資料處置決議**（清理／保留：保留了哪些、改成什麼名稱、如何自行清理）、**服務已關閉**（附重啟指令）
- webwright `final_runs/run_<id>/` 操作記錄與截圖位置

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
| 測試殘留 `e2e-*` workflow 未清 | 以前綴掃描兜底刪除（Step 6.1） |
| 測完直接清資料，沒問使用者 | Step 6 先用 `AskUserQuestion` 問清理或保留，等回覆再動手 |
| 保留的 workflow 沿用 `e2e-<runTag>` 名稱 | Step 6.2 改成不含 `e2e-` 前綴（如 `keep-<runTag>-…`），否則下一輪兜底掃描會清掉 |
| 使用者要保留資料就順手把服務留著 | 服務一律關閉（Step 6.5），回報附重啟指令 |
| 未確認範圍就把 J1–J10 全跑 | Step 1.5 用 AskUserQuestion 複選，並回寫 `Mark -Field journeys` |
| ❌ 只記進問題追蹤區就結案 | Step 5.7 立即分流（前端／後端／測試腳本／環境）＋ 查根因 |
| 自行判斷後直接改程式碼 | Step 5.8 先提案等使用者核准；未核准改產品程式碼會被 hook exit 2 擋下 |
| 修完只重測失敗的那幾個案例 | Step 5.9 重跑選定範圍的**全部**旅程，抓修正引入的回歸 |
| 另開一份「修正報告」檔 | 一律寫回同一份確認表的新章節（問題分流與修正紀錄 / 第二輪重測結果） |

## 維護規則

- **程式碼修改後必檢視測試清單（強制）**：任何前端 UI／workflow 節點型別／執行事件／後端 API 的程式變更，**宣告完成前**必須檢視本清單是否需新增或調整確認項目——
  - 新增或改動使用者可操作的畫面／流程 → 檢查是否要新增旅程案例（Jx-nn）或修改既有案例的預期。
  - 若需要，先更新 `docs/e2e-test-plan.md` 的旅程矩陣，再同步 `e2e-test-checklist.md` 模板，最後才據以測試。
  - 判定「不需新增」時，仍須在該次變更說明中明述已檢視且無需調整，不得略過。
- `docs/e2e-test-plan.md` 的旅程／案例異動時，本 skill 的 `e2e-test-checklist.md` 模板須同步更新（由 `documentation-sync` 把關）。
- 涉及 API 契約變更時，另同步 `docs/api-test-plan.md` 與 `.claude/rules/api-endpoints.md`。
