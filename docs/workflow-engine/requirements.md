# BestPartner 視覺化 Workflow 引擎 v1 — 需求規格（requirements.md）

> 版本：v1 (MVP) ｜ 撰寫角色：SA/SD ｜ 對齊技術棧：Quarkus 3.21 + Kotlin 2.1 + PostgreSQL + JDK 21
> 套件根路徑：`tw.zipe.bastpartner`（注意是 **bastpartner**，少一個 e）
> 本文件為「業務邏輯層」，架構與資料表設計請見同目錄 `system-design.md`。

---

## 1. 專案概述

BestPartner 為類似 Dify / Coze 的 AI 應用平台。本需求新增 **n8n-like 視覺化 workflow 編排能力**：使用者於前端（Vue 3 + Vue Flow）以拖拉方式建立節點（node）、連線（edge），組合成可重複執行的自動化流程，並可串接平台既有能力（LLM 自定義助手、內建工具、MCP Server、Skill、RAG 檢索）。

本引擎的設計目標：

- **無損還原畫布**：資料模型須能 100% 還原前端 Vue Flow 的 node / edge 狀態（座標、連接埠、樣式、參數）。
- **可重複執行與可觀測**：每次執行（execution）皆落地完整紀錄，支援前端逐節點回放與除錯。
- **與既有專案 100% 一致**：entity 繼承 `BaseEntity`、API 走 `ApiResponse` 包裝、JWT（`@Authenticated` / `@RolesAllowed`）權限控管、JSON 欄位以 `@JdbcTypeCode(SqlTypes.JSON)` 儲存。

---

## 2. v1 範疇邊界

### 2.1 v1 要做（In Scope）

| 編號 | 範疇項目 | 說明 |
|------|---------|------|
| S1 | Workflow 定義 CRUD | 建立 / 查詢 / 更新 / 刪除 workflow，含 nodes 與 edges 的整體存取 |
| S2 | 畫布無損存取 | 一次 API 取回 workflow 連同所有 node / edge，可在前端完整重繪 |
| S3 | 同步執行引擎核心 | 一次 execution 依「拓樸順序（topological order）」把所有節點跑到完成，請求-回應式 |
| S4 | 完整 n8n 風格節點類型 | Trigger、LLM/助手、Tool、MCP、Knowledge/RAG、Condition、Loop、Code、HTTP Request、Data Transform（共 10 類，子型見 system-design.md） |
| S5 | 三種觸發來源 | Manual（手動）、Webhook、Cron（排程） |
| S6 | 完整執行紀錄 | 整體執行（狀態、觸發者、起訖時間）+ 每節點（input / output / 耗時 / 錯誤訊息） |
| S7 | 執行紀錄查詢 | 執行列表查詢、單次執行詳情（含各節點）供 UI 回放 |

### 2.2 v1 不做（Out of Scope，明確排除）

| 編號 | 排除項目 | 原因 / 後續版本處理 |
|------|---------|------|
| N1 | **非同步 / 事件驅動 / 訊息佇列執行** | v1 為同步請求-回應語意；不導入 Kafka / RabbitMQ 等。 |
| N2 | **節點層級的平行（parallel）執行** | v1 嚴格依拓樸順序「單執行緒序列」執行單一 execution；分支匯流以序列走訪實作。 |
| N3 | **執行中暫停 / 人工審批 / 等待外部回呼（human-in-the-loop）** | execution 一旦啟動即跑到終態，不支援中途等待。 |
| N4 | **Code 節點的正式安全沙箱** | v1 僅設計資料模型並**預設停用實際執行**（feature flag 關閉），標註為高風險未決項，見 §6 與 system-design.md [LOGIC CONFLICT-2]。 |
| N5 | **Workflow 版本控制 / 版本回溯 UI** | v1 在 ERD 預留 `version` 欄位（樂觀鎖 + 顯示版號），但不提供多版本並存切換、diff、回滾的完整功能。屬開放問題，見 §7。 |
| N6 | **執行的取消（cancel）API 與分散式併發控制** | 狀態機保留 `CANCELLED`，但 v1 不開放主動取消端點；CANCELLED 僅由系統逾時 / 關機 reaper 設定。 |
| N7 | **節點重試（retry）/ 斷點續跑（resume）** | 失敗即整體 FAILED，不自動重試、不從失敗節點續跑。 |
| N8 | **跨 workflow 子流程呼叫（sub-workflow）** | 不支援節點呼叫另一個 workflow。 |
| N9 | **即時推播執行進度（SSE / WebSocket）** | v1 以輪詢執行紀錄查詢進度；不開即時串流。 |
| N10 | **LLM 節點 token 串流輸出（StreamingChatModel）** | workflow 為同步引擎，LLM 節點一律走同步 `ChatModel` 取完整回覆；聊天串流場景仍走既有 `/llm/customAssistantChatStreaming` 端點，不經 workflow。 |

---

## 3. 角色與 RBAC

| 角色 | 說明 | 對應註解 |
|------|------|---------|
| 一般使用者（user） | 可對「自己擁有」的 workflow 進行 CRUD 與執行 | `@Authenticated`，並於 service 層做 `user_id` 擁有權檢核 |
| 管理員（admin） | 可管理所有 workflow、設定 Cron trigger、檢視所有執行紀錄 | `@RolesAllowed("admin")` |
| 匿名 / 外部系統 | 僅能透過 Webhook 公開端點觸發（以不可猜測 token 驗證） | `@PermitAll` + webhook token 驗證 |

> 擁有權原則：除 admin 外，所有讀寫一律以 JWT 內 `userId` 過濾，禁止跨使用者存取。每個 API 的精確 RBAC 等級見 system-design.md「API 契約基礎」。

---

## 4. User Stories

### Epic A — Workflow 設計與管理

- **US-A1**：身為使用者，我要建立一個空白 workflow（給定名稱、描述），以便開始在畫布上編排。
- **US-A2**：身為使用者，我要在一次儲存中提交整張畫布（所有 node 與 edge 及其座標、參數），以便完整保存我的設計。
- **US-A3**：身為使用者，我要以單一 API 取回某個 workflow 的完整定義（含 nodes / edges），以便前端重繪畫布。
- **US-A4**：身為使用者，我要列出我擁有的所有 workflow（含啟用狀態、最後更新時間），以便管理。
- **US-A5**：身為使用者，我要更新 workflow（改名、改描述、整張畫布覆寫），以便迭代設計。
- **US-A6**：身為使用者，我要刪除 workflow（連同其 nodes / edges / triggers / 執行紀錄），以便清理。
- **US-A7**：身為使用者，我要啟用 / 停用 workflow，以便控制其是否可被 Webhook / Cron 觸發。

### Epic B — 執行

- **US-B1**：身為使用者，我要手動執行一個 workflow 並傳入初始輸入（payload），同步取得整體執行結果。
- **US-B2**：身為外部系統，我要透過 Webhook URL（帶 token）觸發 workflow，並同步取得執行結果。
- **US-B3**：身為管理員，我要為 workflow 設定 Cron 排程，讓系統在背景按時自動執行，執行結果落地可查。
- **US-B4**：身為使用者，當任一節點失敗時，我要整體執行標記為 FAILED 並清楚定位是哪個節點、什麼錯誤訊息。

### Epic C — 觀測與除錯

- **US-C1**：身為使用者，我要查詢某 workflow 的執行紀錄列表（狀態、觸發來源、觸發者、起訖時間、耗時）。
- **US-C2**：身為使用者，我要查詢單次執行的詳情，逐節點看到 input / output / 狀態 / 耗時 / 錯誤訊息，以便回放除錯。

### Epic D — 節點能力整合

- **US-D1**：身為使用者，我要放置「LLM / 自定義助手」節點並設定 llmId、prompt、是否啟用 Memory / Tool（含需 API key 的使用者設定版工具）/ MCP（含個人 MCP 設定）/ Skill / RAG 知識庫（knowledgeId 自動增強）/ 多模態檔案，使其呼叫既有 customAssistant 能力，且能力不弱於 `/llm/customAssistantChat` 端點。
- **US-D1a**：身為使用者，我要讓 LLM 節點以 JSON Schema 回傳結構化輸出（responseFormat=JSON + outputSchema），以便下游 Condition / Data Transform 節點對欄位做精準取值與分支判斷。
- **US-D2**：身為使用者，我要放置「Tool 節點」呼叫內建工具（Google / Tavily / Date / Text2SQL）。
- **US-D3**：身為使用者，我要放置「MCP Server 節點」呼叫已註冊的 MCP server。
- **US-D4**：身為使用者，我要放置「Knowledge / RAG 節點」依 knowledgeId + query 檢索向量資料。
- **US-D5**：身為使用者，我要放置「Condition 節點」做 if-else 分支，依條件走不同 edge。
- **US-D6**：身為使用者，我要放置「Loop 節點」對陣列逐筆迭代執行子節點。
- **US-D7**：身為使用者，我要放置「HTTP Request 節點」呼叫外部 REST API。
- **US-D8**：身為使用者，我要放置「Data Transform 節點」對上游輸出做欄位映射 / 格式化。

---

## 5. 驗收準則（Acceptance Criteria, Given/When/Then）

> 每條 AC 皆涵蓋成功路徑與失敗 / 異常路徑。錯誤訊息一律走 i18n（`AppMessage` enum + `MessageUtil`），回應以 `ApiResponse`（失敗時 `code != 200`、`message` 為 i18n 文字）。

### AC-A2 — 儲存整張畫布（無損）

- **成功**
  - Given 已登入使用者擁有 workflow `W`，且提交的 payload 含 N 個 node、M 條 edge，每個 node 帶 `nodeKey`（前端產生、workflow 內唯一）、`type`、`positionX`、`positionY`、`config`（JSON），每條 edge 帶 `sourceNodeKey`、`targetNodeKey`、`sourceHandle`、`targetHandle`、`condition`
  - When 呼叫 `POST /llm/workflow/save`
  - Then 系統以「整張覆寫（full replace within transaction）」方式刪除舊 node/edge 並寫入新集合；回傳 `ApiResponse.success`，且 `version` +1；再次 `GET` 取回的 node/edge 與提交內容逐欄位一致（座標、handle、config 不失真）。
- **失敗 — 邊參考不存在的節點**
  - Given payload 中某 edge 的 `targetNodeKey` 不在本次 node 集合內
  - When 呼叫 save
  - Then 整筆交易 rollback，回傳 `WORKFLOW_EDGE_NODE_NOT_FOUND`（code 400），不留下半套資料。
- **失敗 — nodeKey 重複**
  - Given 兩個 node 的 `nodeKey` 相同
  - When save
  - Then rollback，回傳 `WORKFLOW_NODE_KEY_DUPLICATED`。
- **失敗 — 圖含環（cycle）且非 Loop 回邊**
  - Given node/edge 構成有向環，且該環不屬於合法 Loop 結構
  - When save（或執行前驗證）
  - Then 回傳 `WORKFLOW_GRAPH_HAS_CYCLE`。
- **失敗 — 無 Trigger 節點**
  - Given 畫布不含任何 Trigger 節點
  - When save
  - Then 回傳 `WORKFLOW_TRIGGER_NODE_REQUIRED`（save 可允許草稿，但「啟用」時必須有 trigger，見 AC-A7）。
- **失敗 — 非擁有者**
  - Given 使用者嘗試 save 他人 workflow（且非 admin）
  - When save
  - Then 回傳 `WORKFLOW_FORBIDDEN`（code 403）。

### AC-A3 — 取得完整 workflow

- **成功**：Given 擁有者；When `POST /llm/workflow/get`（傳 `id`）；Then 回傳 workflow meta + 完整 nodes 陣列 + 完整 edges 陣列，欄位足以前端重繪。
- **失敗 — 不存在**：When id 不存在；Then 回傳 `WORKFLOW_NOT_FOUND`（code 404）。
- **失敗 — 無權**：When 非擁有者且非 admin；Then `WORKFLOW_FORBIDDEN`。

### AC-A6 — 刪除 workflow（連鎖）

- **成功**：Given 擁有者；When `POST /llm/workflow/delete`；Then 於單一交易內連鎖刪除 workflow + node + edge + trigger + execution + node_execution；回傳 `SUCCESS_DATA_DELETED`。
- **失敗 — 有進行中執行**：Given 該 workflow 有 `RUNNING` 中的 execution；When delete；Then 回傳 `WORKFLOW_DELETE_WHILE_RUNNING`，拒絕刪除。

### AC-A7 — 啟用 / 停用

- **成功（啟用）**：Given workflow 含合法 Trigger 節點且圖無環；When `POST /llm/workflow/switchStatus`（active=true）；Then `status=ACTIVE`，Webhook / Cron 可觸發。
- **失敗 — 缺 Trigger**：Given 無 trigger 節點；When 啟用；Then `WORKFLOW_TRIGGER_NODE_REQUIRED`。

### AC-B1 — 手動同步執行

- **成功**
  - Given 使用者擁有且 workflow 可執行（圖合法），傳入 `inputPayload`（JSON，選填）
  - When `POST /llm/workflow/execute`
  - Then 系統建立一筆 `workflow_execution`（status=RUNNING、trigger_type=MANUAL、triggered_by=當前 userId、started_at=now），依拓樸順序逐節點執行，每節點寫一筆 `node_execution`（含 input/output/耗時/狀態）；全部成功則 execution `status=SUCCESS`、`finished_at`、`duration_ms`，回傳 executionId + 整體輸出（末端節點 output）。
- **失敗 — 節點執行錯誤**
  - Given 第 k 個節點拋例外（如 LLM 逾時、HTTP 4xx/5xx、Tool 失敗）
  - When 執行
  - Then 該 node_execution `status=FAILED`、記錄 `error_message`；execution `status=FAILED`、`error_node_key=該節點`、`error_message`；後續節點不再執行（status 維持 PENDING / SKIPPED）；API 仍回 200 包裝體，但 `data.status=FAILED`（HTTP 層成功、業務層失敗，與既有 ApiResponse 慣例一致）。
- **失敗 — workflow 不可執行**：Given 圖含環或缺節點；When execute；Then 回傳對應驗證錯誤（`WORKFLOW_GRAPH_HAS_CYCLE` 等），不建立 execution。
- **異常 — 逾時**
  - Given 整體執行時間超過 `workflow.execution.timeout-seconds`（預設 300s）
  - When 逾時觸發
  - Then execution `status=TIMEOUT`（或 FAILED 並標記 timeout 原因），current node_execution 標 FAILED，釋放資源，回傳逾時錯誤 `WORKFLOW_EXECUTION_TIMEOUT`。
- **異常 — 超過節點數上限**
  - Given workflow node 數 > `workflow.max-nodes`（預設 100）
  - When execute / save
  - Then 回傳 `WORKFLOW_NODE_LIMIT_EXCEEDED`。

### AC-B2 — Webhook 觸發

- **成功**：Given workflow `status=ACTIVE` 且含 webhook trigger，外部以正確 `webhookToken` 呼叫 `POST /llm/workflow/webhook/{token}`，body 作為 inputPayload；Then 走與 manual 相同的同步引擎，同步回傳執行結果（200）；triggered_by 記為 `webhook:{triggerId}`。
- **失敗 — token 無效**：When token 不存在 / 不匹配；Then 回 `WORKFLOW_WEBHOOK_TOKEN_INVALID`（code 401/404，避免洩漏存在性）。
- **失敗 — workflow 停用**：Given `status=INACTIVE`；When webhook 呼叫；Then `WORKFLOW_INACTIVE`（code 409）。

### AC-B3 — Cron 排程觸發

- **成功**：Given admin 為 ACTIVE workflow 設定合法 cron 表達式；When 到達排程時刻；Then 排程器在背景執行緒呼叫同一套同步引擎，建立 execution（trigger_type=CRON、triggered_by=`cron:{triggerId}`），結果落地；不需 HTTP 呼叫端等待。
- **失敗 — cron 表達式非法**：When 設定時 cron 字串無法解析；Then `WORKFLOW_CRON_EXPRESSION_INVALID`，不建立排程。
- **失敗 — 背景執行錯誤**：Given 排程執行中節點失敗；When 執行；Then execution 落地為 FAILED 並記錄錯誤，排程器不中斷後續排程；下一次到點仍正常觸發。
- **邊界 — 重疊執行**：Given 上一輪 cron execution 尚 RUNNING 而下一輪到點；When 觸發；Then 依 `overlap_policy`（預設 SKIP）跳過本輪並記錄一筆 `SKIPPED` 紀錄（或依設定 QUEUE，v1 預設 SKIP）。

### AC-C1 — 執行紀錄列表

- **成功**：Given 擁有者；When `POST /llm/workflow/execution/list`（傳 workflowId、可選 status / 分頁）；Then 回傳分頁後的 execution 摘要清單（含 status、trigger_type、triggered_by、started_at、finished_at、duration_ms）。
- **失敗 — 無權**：非擁有者且非 admin；Then `WORKFLOW_FORBIDDEN`。

### AC-C2 — 單次執行詳情（回放）

- **成功**：Given 擁有者；When `POST /llm/workflow/execution/get`（傳 executionId）；Then 回傳 execution 主體 + 依執行順序排序的 node_execution 陣列（含 nodeKey、nodeType、input、output、status、duration_ms、error_message）。
- **失敗 — 不存在**：`WORKFLOW_EXECUTION_NOT_FOUND`。

### AC-D1 — LLM / 助手節點

- **成功**：Given 節點 config 含合法 `llmId` 與 `prompt`（可含 `{{變數}}` 引用上游輸出）；When 執行該節點；Then 呼叫既有 customAssistant 能力（依 config 啟用 Memory / Tool / MCP / Skill / RAG / 檔案），output 寫入 node_execution。
- **成功 — 使用者設定版工具**：Given config 含 `toolSettingIds` 指向使用者已配置 API key 的工具（如 Google / Tavily）；When 執行；Then 以 `buildToolWithSetting` 建出工具供 LLM 呼叫，行為與既有 chat 端點一致。
- **成功 — RAG 自動增強**：Given config 含合法 `knowledgeId`；When 執行；Then LLM 呼叫掛上 retrievalAugmentor 自動檢索知識庫並參考回答（不落地中間檢索結果）。
- **成功 — 結構化輸出**：Given `responseFormat=JSON` 且 `outputSchema` 為合法 JSON Schema；When 執行；Then output 為解析後的結構化 JSON，下游可以 `{{nodeKey.outputKey.欄位}}` 取值。
- **失敗 — llmId 無效**：`LLM_SETTING_NOT_FOUND`，節點 FAILED。
- **失敗 — 變數引用解析失敗**：上游無對應輸出鍵；Then `WORKFLOW_VARIABLE_RESOLVE_FAILED`，節點 FAILED。
- **失敗 — 結構化輸出不符 schema**：模型回傳無法解析或不符 `outputSchema`；Then `WORKFLOW_LLM_OUTPUT_PARSE_FAILED`，節點 FAILED。
- **失敗 — 引用檔案不存在**：`files` 中的檔名在使用者上傳目錄不存在；Then `WORKFLOW_LLM_FILE_NOT_FOUND`，節點 FAILED。

### AC-D5 — Condition 節點

- **成功（true 分支）**：Given config 定義條件運算式，執行期判定為 true；Then 僅走 `sourceHandle=true` 的 edge，false 分支節點標 `SKIPPED`。
- **成功（false 分支）**：判定 false；Then 走 false 分支。
- **失敗 — 運算式非法**：條件無法評估；Then `WORKFLOW_CONDITION_EVAL_FAILED`，節點 FAILED。

### AC-D6 — Loop 節點

- **成功**：Given config 指定 `inputArrayPath` 指向上游某陣列；Then 對每個元素執行 loop body 子圖一次，蒐集每輪 output 為陣列；超過 `max-loop-iterations`（預設 1000）則中止。
- **失敗 — 來源非陣列**：`WORKFLOW_LOOP_INPUT_NOT_ARRAY`；節點 FAILED。
- **失敗 — 超過迭代上限**：`WORKFLOW_LOOP_LIMIT_EXCEEDED`。

### AC-D7 — HTTP Request 節點

- **成功**：Given config 含 method / url / headers / body；Then 發出請求，2xx 回應寫入 output。
- **失敗 — 非 2xx / 連線錯誤 / 逾時**：記錄狀態碼與錯誤訊息，節點 FAILED（`WORKFLOW_HTTP_REQUEST_FAILED`）。

### AC-D（Code 節點，v1 受限）

- **預設停用**：Given feature flag `workflow.node.code.enabled=false`（v1 預設）；When 執行 Code 節點；Then 回傳 `WORKFLOW_CODE_NODE_DISABLED`，節點 FAILED，不執行任何使用者程式碼。（資料模型仍須完整設計，見 system-design.md。）

---

## 6. 非功能需求（NFR）

| 編號 | 類別 | 需求 |
|------|------|------|
| NFR-1 | 權限 | 所有讀寫除 admin 外一律以 JWT `userId` 過濾；webhook 以不可猜測 token 驗證；錯誤訊息不洩漏資源存在性。 |
| NFR-2 | 同步逾時 | 單次 execution 全程逾時上限可設定 `workflow.execution.timeout-seconds`（預設 300）；逾時須中止並落地 TIMEOUT / FAILED。 |
| NFR-3 | 規模上限 | 單一 workflow 節點數上限 `workflow.max-nodes`（預設 100）；Loop 迭代上限 `workflow.max-loop-iterations`（預設 1000）；單節點 output JSON 大小上限（預設 1 MB），超出截斷並標註。 |
| NFR-4 | 交易一致性 | 畫布 save、workflow delete 採單一資料庫交易，全有或全無；execution 過程逐節點即時 flush node_execution，確保 crash 後仍可見已完成節點。 |
| NFR-5 | 可觀測性 | execution 與 node_execution 落地完整 input/output/耗時/錯誤；logger 訊息不納入 i18n（依專案規範）。 |
| NFR-6 | 資料安全 | 節點 config 內若含敏感欄位（如 HTTP Authorization、外部 API key）須以 `PasswordEncryptConverter`（AES-GCM）加密儲存，回應時遮罩。 |
| NFR-7 | Cron 可靠性 | 排程器以 Quarkus Scheduler 實作，背景執行緒池與同步 HTTP 請求執行緒隔離，避免互相阻塞；重疊執行採 SKIP 政策。 |
| NFR-8 | 相容性 | 新增資料表 DDL 手動維護於 `docs/sql/bestpartner-ddl.sql`（無 Flyway 自動遷移）；entity / API / 風格與既有專案一致。 |
| NFR-9 | i18n | 所有對外錯誤 / 成功訊息新增至 `messages_en_US.properties`、`messages_zh_TW.properties` 與 `AppMessage` enum。 |
| NFR-10 | 沙箱安全 | Code 節點在具備安全沙箱前一律停用（feature flag）；屬高風險未決項。 |

---

## 7. 已拍板決策（2026-06-28 使用者確認）

> 原為開放問題，使用者已拍板如下，本文件其餘章節以此為準。

1. **Code 節點沙箱方案** → **v1 不啟用，僅保留資料模型**。Code 節點可拖拉、可設定，但執行時一律回 `WORKFLOW_CODE_NODE_DISABLED`（feature flag `workflow.node.code.enabled=false`）。沙箱技術選型（GraalVM Polyglot / 子行程隔離）延後至正式啟用時再決策，不阻塞 v1。
2. **Workflow 版本控制深度** → **單一最新版 + 樂觀鎖**。僅保留最新版，`version` 欄位用於樂觀鎖防多人覆寫（save 須帶 version，不符回 `WORKFLOW_VERSION_CONFLICT` / 409）。v1 不做多版本並存、回滾、diff（不新增 `workflow_version` 表）。
3. **同步逾時與 Webhook 回應模式** → **統一 300 秒同步等待，逾時 TIMEOUT**。manual 與 webhook 一律同步等待結果；逾時上限 `workflow.execution.timeout-seconds`（預設 300，可設定），超時 execution 落 `TIMEOUT`。**v1 不提供 webhook 202 非同步受理模式**（移除 `responseMode=ASYNC_ACCEPTED`，列入後續版本）。

---

## 8. 修訂紀錄

| 日期 | 修訂內容 |
|------|---------|
| 2026-07-03 | 對照既有 `ChatRequestDTO` / langchain4j 能力盤點後補強：LLM 節點新增 `toolSettingIds`、`mcpSettingIds`、`knowledgeId`（自動 RAG）、`files`（多模態）、`responseFormat`/`outputSchema`（結構化輸出）；定義 `memoryId` 預設語意；新增 N10（LLM token 串流不入 workflow）與 US-D1a、AC-D1 對應情境。 |

> 業務細節（資料表欄位、節點 config schema、API 契約、狀態機）以 `system-design.md` 為準。
