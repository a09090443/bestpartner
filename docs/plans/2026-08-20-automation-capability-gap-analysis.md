# 自動化能力缺口分析與強化提案（對照 n8n 模板實務）

日期：2026-08-20
狀態：**提案中**（尚未拍板，未進入實作）
提出者：Gary

> 本文是**分析與提案**，不是已拍板決策。所有優先序與工作量皆為建議值，
> 待討論後再決定是否轉為實作計畫（屆時另開 `docs/plans/<date>-<topic>.md` 實作篇）。

---

## 1. 問題背景

BestPartner 的 workflow 引擎自 [2026-06-28-workflow-engine.md](2026-06-28-workflow-engine.md) 起，
已完成 13 種 NodeType、12 支 executor、圖驗證、SSE 逐節點事件流、多觸發點選擇與
RAG／MCP／Skill 三種能力併掛（見 `docs/e2e-test-plan.md` §J10 實測）。

但這些能力全部集中在**「一次人工按下執行鈕」的同步鏈路**上。為了確認平台距離
「真實可用的自動化平台」還差什麼，本次以外部參考基準做橫向對照：

**參考基準**：[darrelltw n8n 模板站](https://www.darrelltw.com/tools/n8n_template/models.html)
共 30 個實務模板，涵蓋 Backup / Social / AI / Chatbot / Google / Line 六類。
深入拆解其中 4 個代表案例：

| 模板 | 節點數 | 骨架 |
|------|-------:|------|
| [Github 每日備份工作流](https://www.darrelltw.com/tools/n8n_template/model/n8n-github-backup-workflow.html) | 6 | 排程(每日 3am) → 抓 API → 篩選 → JSON 格式化 → GitHub commit |
| [Gmail 電子發票轉存 Google Sheet](https://www.darrelltw.com/tools/n8n_template/model/gmail_parse_invoice_to_sheet_and_analyze.html) | 22 | 排程 → Gmail 監控 → 附件二進位解析 → M/D 分類 → Loop+Aggregate → Sheets/Grok/Slack 三路輸出 |
| [AI 新聞每日摘要 Email](https://www.darrelltw.com/tools/n8n_template/model/n8n-ai-news-daily-digest.html) | 18 | 排程 → 4 來源 RSS 聚合 → GPT-4o-mini 評分摘要 → 分級 HTML → Gmail 寄送 |
| [LINE 自動回覆](https://www.darrelltw.com/tools/n8n_template/model/darrell_n8n_line_auto_reply.html) | 11 | Webhook 接事件 → 關鍵字多路分流 → HTTP 回呼 LINE Reply API |

---

## 2. 對照分析：n8n 模板的共通骨架 vs BestPartner 現況

30 個模板掃過一輪，形狀高度一致：

```
[排程/Webhook 觸發] → [拉外部資料] → [清洗/映射] → [多路分流]
    → [批次聚合] → [AI 處理] → [送到外部系統]
```

逐段對照：

| # | 骨架環節 | 模板出現率 | BestPartner 現況 | 判定 |
|:-:|---------|:---------:|-----------------|:---:|
| 1 | 排程觸發（Cron） | 極高 | `TriggerType.CRON` enum 存在；UI 選項 `disabled`；**無 `quarkus-scheduler` 依賴、全專案零個 `@Scheduled`** | ❌ 缺 |
| 2 | Webhook 觸發 | 高 | `TriggerType.WEBHOOK` enum 存在；`WorkflowResource` 僅 8 個端點，**無 `/webhook/{token}`** | ❌ 缺 |
| 3 | 拉外部資料 | 全部 | `HTTP_REQUEST` 節點可用；無 RSS / Gmail / Sheets 等現成整合 | ⚠️ 部分 |
| 4 | 資料清洗／欄位映射 | 全部 | `DATA_TRANSFORM`（mappings/template）＋ `CODE`（GraalJS 沙箱） | ✅ 有 |
| 5 | 多路分流 | 高 | `CONDITION` 僅 `out:true` / `out:false` **二分**，無 N 路 Switch | ⚠️ 部分 |
| 6 | 批次／聚合／合流 | 高 | `LOOP` 可迭代（`out:loop` / `out:done`）；**無 Aggregate / Merge / Split-in-Batches / 去重** | ⚠️ 部分 |
| 7 | AI 處理 | 高 | `LLM_ASSISTANT` ＋ TOOL / MCP_SERVER / SKILL / KNOWLEDGE_RAG 四種能力併掛 | ✅ **優於 n8n** |
| 8 | **送到外部系統** | **全部** | `OutputNodeConfig` 只有 `template` / `mappings`，**是「組字串」不是「送出去」**；結果僅落 `llm_workflow_execution` 與前端抽屜 | ❌ 缺 |
| 9 | 錯誤重試 | 中 | 無 node 層 retry / continueOnFail / error 分支；單點 FAILED 即整條中止 | ❌ 缺 |
| 10 | 執行歷史查詢 | — | `requirements.md` AC-C1 / AC-C2 已規格化，**未實作** | ❌ 缺 |
| 11 | 模板匯出／匯入 | 該站存在的前提 | 無 export / import 端點，無模板庫 | ❌ 缺 |

### 2.1 結論

> **差距不在 AI 能力，在自動化的頭與尾。**

第 7 項（AI 能力）BestPartner 明顯超前——n8n 需要串多個節點才做得到的
「同一個助手同時掛 MCP＋Web 搜尋＋Skill」，在 BestPartner 是 `in:tool` 埠一條線的事
（`docs/e2e-test-plan.md` §J10 已實測）。

但第 1、2 項（觸發）與第 8 項（外送）是空的，導致平台的定位停在
**「一個能接工具的 AI 對話編排器」**，而非模板站所展示的
**「無人值守的資料管線」**。使用者無法離開瀏覽器——每一次執行都要有人按鈕，
每一次結果都要有人自己看。

---

## 3. 缺口清單與優先序

工作量為 T-shirt 估計（S ≈ 1–2 日、M ≈ 3–5 日、L ≈ 1–2 週），僅供排序參考。

### P0 —— 沒有這些，模板的形狀做不出來

#### GAP-1　排程觸發（CRON）　`M`

- **現況**：`enumerate/TriggerType.kt` 已有 `CRON`；`TriggerNodeConfig.cron: JsonObject?` 欄位已預留；
  `service/workflow/ExecutionEvent.kt:29` 註解明寫「未來 CRON/WEBHOOK 觸發可另接」。
  但 `build.gradle.kts` 無 scheduler 依賴，無任何 `@Scheduled`。
- **規格已存在**：`docs/workflow-engine/requirements.md` AC-B3，含 `overlap_policy`（預設 SKIP）、
  cron 非法回 `WORKFLOW_CRON_EXPRESSION_INVALID`、背景失敗不中斷後續排程。
- **實作要點**：
  - 引入 `quarkus-scheduler`（版本由 BOM 管理，依 `gradle-conventions.md` **不寫進 `gradle.properties`**）。
  - 動態排程（非 `@Scheduled` 靜態註解）——需用 `Scheduler` programmatic API，
    於 workflow `switchStatus(active=true)` 時註冊、`active=false` 或 delete 時解除。
  - 執行身分：cron 無 JWT context，須以 workflow owner 的 userId 建立 `ExecutionContext`
    （`docs/e2e-test-plan.md` §12 已載明「執行身分＝設定擁有者」，此處必須沿用，
    否則 `buildUserSpecificMcpClient` / `buildToolWithSetting` 的 `(settingId, userId)` 配對會查無資料）。
  - `WorkflowEngine.kt:222` 目前把 `triggerType` 寫死為 `TriggerType.MANUAL`，須改為由觸發來源決定。
- **風險**：多實例部署時同一 cron 會重複觸發。v1 可先接受單實例假設並在文件明述，
  或以 DB 樂觀鎖搶佔（`llm_workflow_execution` 加唯一約束）。

#### GAP-2　Webhook 觸發　`M`

- **規格已存在**：AC-B2，含 `POST /llm/workflow/webhook/{token}`、token 無效回 401/404
  （刻意不洩漏存在性，對齊 `api-endpoints.md` 既有的 MCP setting 擁有權檢核風格）、
  workflow INACTIVE 回 409。
- **實作要點**：
  - 端點須 `@PermitAll`（外部系統無 JWT），改以不可猜測 token 認證 → 對應 NFR-1。
  - token 存放：`llm_workflow_node.config.webhook` 或另立欄位；**須加密落地**
    （比照 `WorkflowSecretConverter` 對 `secretHeaders` 的處理）。
  - LINE / Meta 這類平台要求 **challenge 驗證回應**（模板站的 `meta-challenge-respond` 僅 2 節點但不可省），
    須支援同步回傳指定 body。
- **附帶價值**：Webhook 落地後，`triggered_by` 欄位才有意義，也才有 GAP-7 執行歷史的使用情境。

#### GAP-3　OUTPUT 節點無法真的「輸出」　`M`

- **這是最違反直覺的缺口**。`OutputNodeConfig` 僅 `template` / `mappings`，
  `OutputExecutor` 產出的字串留在 DB 與前端抽屜，**沒有任何離開平台的路徑**。
- 對照模板：GitHub 備份→commit、發票→Sheets/Slack、新聞摘要→Gmail、LINE→Reply API，
  **100% 的模板價值在最後一哩**。
- **建議方案（擇一或並行）**：

  | 方案 | 說明 | 成本 | 評價 |
  |------|------|:---:|------|
  | A. 新增 `NOTIFY` 節點 | 內建 Email / Slack webhook / LINE push 三種 channel | M | 使用者體驗最好，但每加一個 channel 就是一次維護 |
  | B. 強化 `HTTP_REQUEST` 為終端節點 | 技術上**現在就做得到**，缺的是 UI 引導、範例模板與文件 | S | **建議先做**，零後端成本即可解鎖多數場景 |
  | C. 兩者都做 | B 當通用逃生口，A 覆蓋高頻場景 | M+ | 長期目標 |

- **建議**：先以 **方案 B** 補文件與範例（成本 S），再視使用回饋決定是否投入 A。

### P1 —— 有這些，複雜模板才寫得順

#### GAP-4　多路分支（SWITCH 節點）　`S`

`CONDITION` 只有二分。LINE 模板的「營業時間／地址／停車」三選一、
`line_message.html`（20 節點）的事件型別分流，用現有能力只能串接多個 CONDITION，
畫布迅速失控。建議新增 `SWITCH` NodeType，`out:case-<n>` / `out:default` 多出邊。

> ⚠️ 新增 NodeType 會觸發多處連鎖：後端 `NodeType.kt` ＋ `NodeConfig.kt` ＋ executor、
> 前端 `types/workflow.ts` ＋ `constants/nodeTypes.ts` / `nodeIcons.ts` / `nodeDocs.ts` /
> `nodeOutputKeys.ts` ＋ `inspector/typedForms.ts`，且 `nodeIcons` / `nodeDocs` 是
> `Record<NodeType, T>`（漏補會被 `vue-tsc` 擋下）。漂移掃描亦會比對前後端 NodeType 契約一致性。

#### GAP-5　資料聚合節點（AGGREGATE / MERGE）　`M`

Gmail 發票模板（22 節點）的核心就是「拆成 M 主檔＋D 明細 → 分別聚合 → 寫兩張表」。
現況 `LOOP` 只能迭代並以 `DEFAULT_COLLECT_KEY = "items"` 單一形式收集，
**無法合流兩條分支、無法去重、無法分批**。AI 新聞模板的「4 來源 RSS 聚合」同樣卡在這裡。

#### GAP-6　節點層錯誤處理（retry / continueOnFail / error 分支）　`M`

`HttpRequestNodeConfig` 有 `timeoutMs` 但無 `retry`；引擎無 `continueOnFail` 概念。
外部 API 抖一下就要人工重跑——與 GAP-1／GAP-2 的「無人值守」前提直接衝突。
**建議與 GAP-1 同批交付**，否則排程觸發上線後會產生大量無人處理的 FAILED 紀錄。

#### GAP-7　執行歷史 API 與 UI　`M`

- `requirements.md` AC-C1（`execution/list`，含 status / 分頁 / duration）與
  AC-C2（`execution/get`，含依序排列的 node_execution、input/output/error）**已完整規格化但未實作**。
- 現況：執行完關掉抽屜，該次執行就再也看不到。資料其實**都已落庫**
  （`llm_workflow_execution` / `llm_workflow_node_execution`，NFR-4 要求逐節點即時 flush），
  純粹缺讀取端點與畫面。
- **投報率高**：資料已在，只差 API 與一個列表頁。

#### GAP-8　Workflow 匯出／匯入 JSON　`S`

- darrelltw 這個站存在的前提就是「複製 JSON 貼進 n8n」。
- **成本極低**：`workflow/save` 已經吃完整圖形 DTO（`WorkflowDTO` 含 nodes/edges），
  `/export` 幾乎是回傳現有 `get` 結果、`/import` 是轉呼 `save`。
- **收益高**：可建模板庫、可版控（正好對應「GitHub 每日備份」那個模板的自我應用）、
  可在團隊間分享、E2E 測試可用固定 JSON 建圖而不必每次拖拉。
- ⚠️ 匯出時**必須沿用既有遮罩契約**：`HTTP_REQUEST.secretHeaders` 回 `__SECRET_KEPT__`
  （見 `api-endpoints.md` 的 `workflow/get`），否則匯出檔會外洩明文金鑰。
  匯入時值為 `__SECRET_KEPT__` 應視為「未提供」而非沿用（跨環境沒有既有密文可沿用）。

### P2 —— 體驗與治理

#### GAP-9　前端只有 3 個頁面　`L`

`bestpartner-ui/src/views/` 僅 `LoginView` / `WorkflowListView` / `WorkflowEditorView`。
LLM 設定、工具設定、MCP 設定、Skill 上傳、知識庫建立、使用者管理**全部只有 API 沒有 UI**。

`docs/e2e-test-plan.md` §J8 前置已自承此事：
> 「前端無向量庫／LLM 設定／檔案上傳的管理頁（僅 login/列表/編輯器三頁），故前置三步一律走 API 準備。」

實際後果：新使用者拿到平台，光是配一把 API key 就得開 Postman。

#### GAP-10　憑證（Credential）管理分散　`M`

現為三套獨立機制，各自加密、各自遮罩：

| 儲存位置 | 加密方式 | 遮罩 |
|---------|---------|------|
| `llm_setting.api_key` | `PasswordEncryptConverter`（v2 格式） | `__SECRET_KEPT__` |
| `llm_tool.setting_content` | `SensitiveValueCodec`（JSON 內逐值） | `__SECRET_KEPT__` |
| `llm_mcp_user_setting.setting_content` | `McpSettingEncryptConverter`（整欄） | env 分類遮罩 |

n8n 是統一的 Credential 概念，一次設定到處引用。節點種類一多（GAP-3 A 案的 Email/Slack/LINE）
會加劇分散。不急，但建議在新增第 4 種機制**之前**先收斂。

#### GAP-11　子流程（Execute Workflow 節點）　`M`

`workflow.max-nodes` 上限 100（NFR-3）。22 節點的發票模板換算到 BestPartner
（缺 Aggregate 需自行展開）恐怕更多。缺共用邏輯抽取手段，複雜場景遲早撞頂。

---

## 4. 建議實作順序

分三階段，每階段自身可交付、可驗證：

### 階段一：解鎖「無人值守」（P0 核心）

```
GAP-1 (CRON)   ─┬─► GAP-6 (錯誤重試) ─► 階段一可上線
GAP-2 (Webhook)─┘
GAP-3-B (HTTP_REQUEST 當終端節點：補文件與範例，S)
```

**為何 GAP-6 綁在階段一**：排程／Webhook 一旦上線就是無人看管，
沒有 retry 與 continueOnFail 會產生大量無人處理的 FAILED。兩者是同一件事的正反面。

**驗收信號**：能做出「每日 3am 抓某 API → LLM 摘要 → HTTP POST 到 Slack webhook」
這條完整鏈路，且中途 API 失敗會自動重試而非整條死掉。這正是 GitHub 備份模板的形狀。

### 階段二：補可觀測性與流通性（P1 高投報）

```
GAP-7 (執行歷史 API+UI，資料已落庫)
GAP-8 (匯出/匯入 JSON，成本 S)
```

兩者都是「資料／能力已存在，只差介面」，投報率最高。
GAP-8 完成後，E2E 測試可改用固定 JSON 建圖，大幅降低旅程撰寫成本。

### 階段三：補資料處理表達力（P1 其餘）

```
GAP-4 (SWITCH) ─► GAP-5 (AGGREGATE/MERGE)
```

放最後是因為兩者都要新增 NodeType，連鎖面最大（見 GAP-4 的警語），
且在階段一、二未完成前，做出來也沒有承載它們的場景。

P2（GAP-9/10/11）視使用者回饋另議。其中 **GAP-9 前端管理頁**若要做，
建議與 GAP-10 憑證收斂一起規劃，避免做出三套長得不一樣的設定頁。

---

## 5. 測試場景延伸（不依賴上述任何實作，現在就能跑）

盤點 `docs/e2e-test-plan.md` 的 J1–J10，發現一個明確空洞：

> **13 種 NodeType 中，`HTTP_REQUEST` / `CODE` / `LOOP` / `DATA_TRANSFORM` 四種為零執行覆蓋。**

現有提及僅止於「拉線相容性的反例」（§14-8 用 `HTTP_REQUEST` 測 `INCOMPATIBLE_CONNECTION`）
與「製造未存變更的道具」（§15 附註用空 `CODE` 節點）——**從未真的執行過**。
而這四種正是 n8n 模板的主力節點。

以下四條旅程用現有能力即可跑完，建議補進 `docs/e2e-test-plan.md`：

### J11　純資料管線（P0）

對照模板：房屋資訊爬蟲、Apify 撈 YouTube

```
TRIGGER(MANUAL, inputPayload 帶 URL)
  → HTTP_REQUEST(GET 公開 API)
  → DATA_TRANSFORM(mappings 抽欄位)
  → OUTPUT
```

**測試價值（本條最高）**：這是第一條**完全不含 LLM** 的執行旅程。
現有 J5 / J8 / J10 全部依賴真實 LLM，導致斷言只能驗 plumbing、逾時要放寬、
金鑰缺席就 skip。J11 是**確定性的**，可精確斷言 `finalOutput` 內容，
適合當整個 E2E 體系的回歸基準。

**順帶覆蓋**：`HTTP_REQUEST.secretHeaders` 的加密落地與 `__SECRET_KEPT__` 遮罩契約
（`WorkflowSecretConverter`）——目前只有後端單元測試，無 UI 端到端驗證。

### J12　迴圈批次處理（P1）

對照模板：Gmail 發票（22 節點）、While True 迴圈範例

```
TRIGGER → HTTP_REQUEST(回傳陣列)
  → LOOP ─out:loop→ [LLM_ASSISTANT 逐筆分類]
          └out:done→ DATA_TRANSFORM(彙集) → OUTPUT
```

**測試價值**：`LoopExecutor` 是「薄殼＋引擎特判」的特殊設計
（迭代編排在 `WorkflowEngine`，**不經 `execute` 分派**，僅呼叫 `resolveItems`）。
以下行為目前**零端到端證據**：

- `out:loop`（僅界定子圖入口、不參與活化）與 `out:done`（迭代完成後活化）的語義差異
- `DEFAULT_MAX_ITERATIONS = 100` 上限與 `WORKFLOW_LOOP_LIMIT_EXCEEDED`
- `inputArrayPath` 非陣列時的 `WORKFLOW_LOOP_INPUT_NOT_ARRAY`（含 `{{path}}` 與裸 path 兩種寫法）
- **子圖節點的 `node_execution` 落庫筆數**：N 次迭代是 N 筆還是 1 筆？此行為未見文件記載

### J13　條件分流資料管線（P1）

對照模板：LINE 關鍵字自動回覆

```
TRIGGER(payload 帶 message)
  → CONDITION ─out:true → DATA_TRANSFORM(A) ─┐
              └out:false→ DATA_TRANSFORM(B) ─┴→ OUTPUT
```

**與 J5 分支案例的分工**：J5 測 CONDITION 分流**提示詞**（`in:prompt` 埠，含 LLM）；
J13 測分流**資料流**（`in:main` 埠，無 LLM）。
重點斷言：`SKIPPED` 的**傳播深度**——未活化分支的**整條下游**是否都標 SKIPPED，
而非只有直接相連那顆。

### J14　CODE 節點沙箱（P1 功能 / P2 邊界）

GraalJS 沙箱是 `.claude/rules/tech-stack-and-versions.md` 著墨最深的一塊
（`allowAllAccess(false)`、禁 host class/IO、逾時與輸出上限、truffle-api 剝除 workaround），
**卻沒有任何 E2E**。建議三條：

| 案例 | 預期 |
|------|------|
| 正常 JS 資料轉換 | 節點 SUCCESS，output 為轉換結果 |
| 超過 `timeoutMs` 的無窮迴圈 | 節點 FAILED，錯誤訊息指出逾時，**不拖垮整個服務** |
| 嘗試存取檔案系統 / host class | 被沙箱擋下，節點 FAILED |

後兩條同時是**安全性驗證**，不只是功能驗證。

### J15　執行歷史查詢（阻塞中）

依賴 GAP-7。`execution/list` 與 `execution/get` 未實作前無法撰寫。
連帶影響：「執行失敗後回放除錯」這整類旅程目前無從測起。

---

## 6. 範圍外（本次不處理）

- **不新增外部整合節點**（Gmail / Google Sheets / Slack / LINE 專用節點）。
  n8n 的節點生態是它多年累積的護城河，正面比拚不划算；
  BestPartner 應走 GAP-3 方案 B（通用 HTTP）＋ MCP 生態的路線。
- **不處理二進位／檔案在節點間傳遞**（Gmail 發票模板的附件解析依賴此能力）。
  現況 `/llm/uploadFile` 只服務 chat，未接入 workflow。需求成立時另議。
- **不重構憑證機制**（GAP-10）——列入分析供決策參考，非本次建議實作。

---

## 7. 附帶發現：文件漂移

分析過程中發現一處與程式碼不符，**已於 2026-08-20 修正**：

| 文件 | 原記載 | 實際 | 處置 |
|------|-------|------|------|
| `.claude/rules/architecture-and-packages.md`（目錄樹的 executor 清單） | 「共 11 個」，未列 `DataTransformExecutor` | 目錄實有 **12 支** executor | 已補列並改為「共 12 個」 |

> 初稿誤記為「`.claude/CLAUDE.md` 與 `architecture-and-packages.md` 兩處」，實際只有後者記載 executor 數量；
> `.claude/CLAUDE.md` / `AGENTS.md` 僅在 ArchUnit 章節提及 `workflow.executor` 套件的命名規則，不含清單與計數，
> 故本次無需連動那組逐字一致的導覽文件（漂移掃描的「導覽一致性」仍為 ✓ OK）。

---

## 8. 證據來源

| 結論 | 依據 |
|------|------|
| CRON / WEBHOOK 未實作 | `enumerate/TriggerType.kt` 有 enum；`build.gradle.kts` 無 scheduler；全專案 `@Scheduled` = 0；`WorkflowResource.kt` 僅 8 個 `@Path` |
| `WorkflowEngine` 寫死 MANUAL | `service/workflow/WorkflowEngine.kt:222` |
| 架構已預留觸發擴充 | `service/workflow/ExecutionEvent.kt:29` 註解 |
| OUTPUT 不外送 | `dto/workflow/config/NodeConfig.kt` 的 `OutputNodeConfig`（僅 template / mappings） |
| CONDITION 僅二分 | `NodeConfig.kt` 的 `ConditionNodeConfig` ＋ `docs/e2e-test-plan.md` §J9（`out:true` / `out:false`） |
| LOOP 為引擎特判 | `service/workflow/executor/LoopExecutor.kt` 類別 KDoc 與 companion 常數 |
| 執行歷史規格已存在未實作 | `docs/workflow-engine/requirements.md` AC-C1 / AC-C2 vs `WorkflowResource.kt` |
| 前端僅 3 頁 | `bestpartner-ui/src/views/`；`docs/e2e-test-plan.md` §J8 前置說明自承 |
| 四種節點零執行覆蓋 | `docs/e2e-test-plan.md` 全文 grep：`HTTP_REQUEST` 僅見於 §14-8 反例、`CODE` 僅見於 §J6 表單與 §15 附註、`LOOP` / `DATA_TRANSFORM` 零命中 |
| n8n 模板骨架 | darrelltw 模板站 30 個模板清單 ＋ 4 個代表案例詳讀（連結見 §1） |

---

## 9. 修訂紀錄

| 日期 | 變更 |
|------|------|
| 2026-08-20 | 初版提案 |
