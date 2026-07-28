# E2E 測試計畫（UI-driven）

> 本文件為 BestPartner **前端驅動的端到端（E2E）測試計畫**，以 Playwright 驅動真實瀏覽器，
> 跨「登入 → workflow 列表 → 編輯器（畫布 / Inspector / 執行）」主軸，對 **port 80 真實服務 + PostgreSQL** 全貫穿。
>
> 與既有測試的分工：
> - `bestpartner-ui` 的 **vitest** 顧「元件 / composable / store 邏輯正確」。
> - `bestpartner-service` 的 **JUnit / QuarkusTest / ArchUnit** 顧「後端單元、整合與架構不變量」。
> - `docs/api-test-plan.md` 顧「API 契約與各端點行為」（手動 / Postman）。
> - **本計畫（E2E）** 顧「使用者真的能走完流程、前後端契約在真實瀏覽器中接得上」，**只跑關鍵使用者旅程，不與上述重疊**。

---

## 1. 選型與定位

| 項目 | 決策 | 理由 |
|------|------|------|
| 測試框架 | **Playwright** | 官方支援 Vite/Vue 佳、跨瀏覽器、內建 trace / 錄影 / 網路攔截，對 SSE 串流友善；與現有 vitest 並存互不干擾 |
| 目錄 | `bestpartner-ui/e2e/` | 獨立於 `src/**/__tests__`（vitest），各自 config |
| 後端 | **真實後端全貫穿**（port 80 + PostgreSQL） | 能驗到 SSE 執行事件流與 DB 寫入，最貼近正式環境 |
| 執行 happy path | **真實 LLM_ASSISTANT 節點** | 依需求採真實模型呼叫；斷言策略見 §3-J5 與 §6 |
| 本次交付 | **僅本計畫文件** | 測試碼實作為後續階段（見 §8） |

**分工原則**：E2E 只跑主軸旅程，不重跑 vitest / JUnit 已覆蓋的欄位級細節。

---

## 2. 測試環境與前置

### 2.1 前置條件（由 globalSetup 檢查，不自動亂啟以免污染）

1. PostgreSQL（`localhost:5432/pgdb`）已初始化（`docs/sql/bestpartner-ddl.sql` + `bestpartner-init-data.sql`）。
2. `bestpartner-service` 以 **dev profile** 啟動於 **port 80**（健康檢查失敗即 fail-fast，輸出清楚訊息）。
3. `bestpartner-ui` 已 build 並以 `vite preview`（或 dev server）提供前端；`baseURL` 指向該位址。
4. 種子資料存在：
   - `admin/admin`（RBAC 管理員）
   - `test_user` 及其 **CHAT / STREAMING_CHAT** LLM setting（llmId 對照見既有紀錄）
   - **可實跑的 LLM setting**：具備有效 `api_key` 的平台設定，供 J5 真實執行使用

### 2.2 登入態復用（storageState）

- `globalSetup` 以 API `admin/admin` 登入取得 JWT，寫入 `e2e/.auth/storageState.json`。
- 多數測試透過 `storageState` 直接帶登入態，**不必逐條重登**（重登流程本身由 J1 專門覆蓋）。

### 2.3 環境變數

| 變數 | 用途 | 備註 |
|------|------|------|
| `E2E_BASE_URL` | 前端位址 | 預設 `http://localhost:4173`（preview）或 dev server |
| `E2E_API_URL` | 後端位址 | 預設 `http://localhost:80` |
| `E2E_ADMIN_USER` / `E2E_ADMIN_PASS` | 登入帳密 | 預設 `admin` / `admin` |
| `E2E_LLM_ID` | J5 真實執行所用的 llmId | 指向具有效 api_key 的 CHAT 設定 |
| `E2E_EMBEDDING_ID` | J8 用的 embeddingModelId | 指向具有效 api_key 的 EMBEDDING 設定；缺則 J8 skip |
| `E2E_KNOWLEDGE_ID` / `E2E_EMBEDDING_STORE_ID` | J8 知識庫與向量庫 | 由前置 API 準備產出（或由前置步驟現建現用） |

---

## 3. 涵蓋的旅程與案例

> 優先級沿用專案定義：P0 核心阻斷、P1 主要業務、P2 次要 / 邊界 / 錯誤處理。

### J1 認證流程（P0）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | 未登入直接訪 `/` | 路由守衛導向 `/login` |
| P0 | `admin`/`admin` 登入 | 導向 workflow 列表 `/`，顯示使用者已登入 |
| P1 | 已登入訪 `/login` | 導回首頁 `/` |
| P1 | 錯誤帳密登入 | 停留 `/login`，顯示錯誤訊息 |
| P1 | 列表頁點登出（`data-test="logout-button"`） | 清 token 導向 `/login`；重訪受保護頁被導回登入 |
| P1 | 編輯器工具列點登出（無未存變更，`data-test="logout-button"`） | 清 token 導向 `/login`；per-user 快取（設定 / 節點選項）一併清除 |
| P1 | 編輯器有未存變更時點登出 → 確認框選「登出」 | 先跳「尚未存檔」確認；確認後清 token 導向 `/login` |
| P1 | 編輯器有未存變更時點登出 → 確認框選「取消」 | 留在編輯器且**仍為登入態**（token 未清），可續編輯 |
| P2 | token 失效 / 清除後訪受保護頁 | 導回 `/login` |

### J2 Workflow 列表（P0 / P1）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | 列表載入 | 顯示既有 workflow 摘要清單 |
| P0 | 建立新 workflow（輸入 name → create） | 進入 `/editor/:id`，狀態 DRAFT、version 1 |
| P1 | 從列表點項目進編輯器 | 載入該 workflow 完整定義 |
| P1 | 刪除 workflow | 列表移除該項，後端連鎖刪 node/edge |

### J3 畫布編輯（P0）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | 從 NodePalette 拖拉節點入畫布（TRIGGER / LLM_ASSISTANT / OUTPUT） | 節點出現於畫布，nodeKey 唯一 |
| P0 | 連線 edge（來源 handle → 目標 handle） | edge 建立，兩端點存在 |
| P1 | 能力掛載：拖 TOOL / MCP_SERVER / SKILL / **KNOWLEDGE_RAG** 節點，`out:main` 連到 LLM 的 `in:tool` 埠並 save | edge 建立（`targetHandle=in:tool`）；重載後畫布還原、連線保留 |
| P1 | 多個 KNOWLEDGE_RAG 同時掛到同一 LLM 的 `in:tool` 埠 | 皆放行、無相容性 toast；`CONNECTIONS` 計數與 edge 數一致 |
| P1 | 提示接線：拖 PROMPT 節點，`out:main` 連到 LLM 的 `in:prompt` 埠並 save | edge 建立（`targetHandle=in:prompt`）；重載後畫布還原、連線保留 |
| P1 | 兩個 PROMPT 分別接 CONDITION 的 `out:true` / `out:false`，再同時連到同一 LLM 的 `in:prompt` | 皆放行、無相容性 toast；`CONNECTIONS` 計數與 edge 數一致 |
| P0 | 選節點 → InspectorPanel 填 config | 表單值寫回節點 |
| P0 | save 整張覆寫 | version+1；重載後畫布還原（nodes/edges/config 一致） |
| P1 | 重新整理頁面後 get 還原 | 畫布與 Inspector 內容與存檔一致 |

### J4 圖驗證與啟用（P1）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | 缺 TRIGGER 節點時 switchStatus 啟用 | 400，顯示對應訊息 |
| P1 | 圖有環時啟用 | 400，顯示對應訊息 |
| P1 | 節點缺必填 config 啟用（如 LLM_ASSISTANT 缺 `llmId`） | 400，`workflow.node.config.required.missing`，UI 顯示 nodeKey 與缺漏欄位 |
| P1 | 孤兒 SKILL 節點（未連任何 LLM `in:tool`）save → 啟用 | save 放行（DRAFT 容許未完成）；**switchStatus 回 400** `workflow.skill.node.not.mounted`（訊息含 nodeKey），開關維持關閉、狀態仍 DRAFT。execute 亦回 400（第二層防線） |
| P1 | SKILL 已掛載到 LLM `in:tool` 後啟用（上一條的對照組） | 啟用成功，狀態轉 ACTIVE |
| P1 | **非能力來源節點**拉線到 LLM `in:tool` 埠（能力來源＝`TOOL` / `MCP_SERVER` / `SKILL` / `KNOWLEDGE_RAG`，見下方註） | `onConnect` 即擋下：toast「僅工具、MCP、Skill、知識庫節點可連到 LLM 的工具埠」，**edge 不建立**（CONNECTIONS 計數不變）。建議併測對照組：同一埠改由能力來源連入應成功 |
| P1 | KNOWLEDGE_RAG 掛 `in:tool`（外掛模式）且 `query` / `embeddingModelId` 留空時啟用 | **啟用成功**（外掛模式僅需 `knowledgeId`；檢索語句由 LLM 問題自動帶入、embedding 由知識庫本身設定決定） |
| P1 | 孤兒 PROMPT 節點（未連任何 LLM `in:prompt`）save → 啟用 | save 放行（DRAFT 容許未完成）；**switchStatus 回 400** `workflow.prompt.node.not.connected`（訊息含 nodeKey），狀態仍 DRAFT。execute 亦回 400（第二層防線） |
| P1 | LLM 節點 `userPrompt` 留空且無 PROMPT 連入 `in:prompt` 時啟用 | **400** `workflow.llm.prompt.required`（訊息含 LLM 的 nodeKey），狀態仍 DRAFT |
| P1 | LLM `userPrompt` 留空但已有 PROMPT 連入 `in:prompt`（上兩條的對照組） | 啟用成功，狀態轉 ACTIVE |
| P1 | **非 PROMPT 節點**拉線到 LLM `in:prompt` 埠（如 TRIGGER） | `onConnect` 即擋下：toast「僅提示詞節點可連到 LLM 的提示埠」，**edge 不建立**（CONNECTIONS 計數不變） |
| P1 | 已連 PROMPT 的 LLM 節點在 Inspector 檢視 | 「使用者提示」欄旁顯示 `data-test="prompt-overridden-badge"`（已由上游提示節點提供）與 `prompt-source-hint`；欄位仍可編輯（非 disabled） |
| P1 | 合法圖 switchStatus 啟用 | 狀態轉 ACTIVE |

> **能力來源節點型別（`in:tool` 白名單）的事實來源**：
> 後端 `WorkflowEngine.kt` 的 `CAPABILITY_SOURCE_TYPES`，前端 `useGraphValidation.ts` 的
> 同名常數須逐項一致（漂移掃描會比對）。目前為
> **`TOOL` / `MCP_SERVER` / `SKILL` / `KNOWLEDGE_RAG`** 四種。
> 白名單異動時，本節與 §J3 / §J5 / §J8 三處案例須同步更新。
>
> ⚠️ **KNOWLEDGE_RAG 有兩種模式，勿混淆**：
>
> | 連到的埠 | 模式 | 必填 config | 是否獨立執行 |
> |---------|------|-----------|------------|
> | LLM 的 `in:tool` | **外掛模式**（自動注入型 RAG） | 僅 `knowledgeId` | ✗ 純能力掛載，不落主遍歷 |
> | 一般 `in:main` | **pipeline 模式**（檢索節點） | `knowledgeId` + `query`（+ 需要時 `embeddingModelId`） | ✓ 產生自身 `node_execution` |
>
> 「純能力節點」的判定條件是**所有出邊皆為 `in:tool`**；若同一 RAG 節點另有 `in:main` 出邊，
> 則仍會落入主遍歷。
>
> ⚠️ **`INCOMPATIBLE_CONNECTION` 只存在於前端**（`useGraphValidation.ts`），後端無對應訊息。
> 且 `onConnect` 已在拉線當下阻止 edge 建立，**經 UI 無法把不相容 edge 帶到存檔階段**；
> `validateGraph` 的該錯誤是針對「後端載入的既有資料」的第二層防線。寫案例時勿把它當成 save 的預期回應。
>
> **孤兒 SKILL 檢查點（已於 202607272132 週期修正）**：規則抽為純函式
> `WorkflowEngine.findUnmountedSkillNodeKey(nodes, edges)`，由兩處共用、避免分叉——
> `WorkflowService.switchStatus`（**啟用前**，確保 ACTIVE ≡ 可執行）與
> `WorkflowEngine.validateNodes`（**執行前**，涵蓋「啟用後才被改壞」與 DRAFT 直接 execute）。
> 驗證順序兩邊一致：必填欄位先於掛載檢查，故 SKILL 缺 `skillId` 時會先回 REQUIRED_MISSING。
> 停用（`active=false`）不跑圖驗證，避免使用者被卡在改不回去的狀態。
>
> ⚠️ **`in:prompt` 與 `in:tool` 是相反語義的兩個埠，勿混淆**：
>
> | 埠 | 允許來源 | 是否資料流 | 來源節點是否執行 |
> |----|---------|-----------|----------------|
> | `in:tool` | `TOOL` / `MCP_SERVER` / `SKILL` / `KNOWLEDGE_RAG` | ✗ 不參與活化 | 純能力節點不執行、不落紀錄 |
> | `in:prompt` | 僅 `PROMPT` | ✓ 參與活化與拓撲排序 | ✓ 照常執行、落紀錄、發 SSE 事件 |
>
> 因為 `in:prompt` 是資料流邊，CONDITION 分支才能只活化其中一個提示節點；
> LLM 執行時取「已有輸出」的第一個來源（依拓撲序），未活化分支的提示節點沒有輸出，自然被略過。
>
> **提示接線檢查點**：兩條規則同樣抽為純函式由 switchStatus（啟用前）與 `validateNodes`（執行前）共用——
> `WorkflowEngine.findUnconnectedPromptNodeKey`（孤兒 PROMPT）與 `findPromptlessLlmNodeKey`
> （LLM 無提問來源）。驗證順序：必填欄位 → 孤兒 SKILL → 孤兒 PROMPT → LLM 無提問來源，
> 故 PROMPT 缺 `prompt` 時會先回 REQUIRED_MISSING。
> 前端另以 `PROMPT_WIRING_INCOMPLETE`（warning，不擋存檔）鏡射這兩條規則。

### J5 執行 workflow（P0，**真實 LLM**）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | 合法 workflow（TRIGGER → LLM_ASSISTANT → OUTPUT，DRAFT 即可）execute | ExecutionResultDrawer 依序反映 SSE：`execution.started` → 各節點 `node.started` / `node.completed` → `execution.completed`（SUCCESS）；`llm_workflow_execution` / `llm_workflow_node_execution` 有紀錄 |
| P1 | Agent 模式：TOOL / MCP_SERVER / SKILL 節點以 `out:main → LLM in:tool` 掛載後 execute | LLM 可自主呼叫掛載工具並回覆；能力節點**不產生**獨立 `node.started` / `node.completed` 事件、`node_execution` 無其紀錄（僅 LLM 節點內部呼叫） |
| P1 | 外掛型 RAG：KNOWLEDGE_RAG 以 `out:main → LLM in:tool` 掛載後 execute | 推論前**自動檢索並注入**知識庫內容（非由 LLM 主動呼叫）；同樣**不產生**獨立節點事件、`node_execution` 無其紀錄。內容正確性斷言歸 J8 |
| P1 | 提示節點驅動：PROMPT → `LLM in:prompt` → OUTPUT，LLM `userPrompt` 留空後 execute | PROMPT 節點**照常**產生 `node.started` / `node.completed` 與 `node_execution` 紀錄（與能力節點相反）；LLM 以該提示提問並成功回覆 |
| P1 | 分支擇一：CONDITION 兩分支各接一個 PROMPT，再匯入同一 LLM 後 execute | 被活化分支的 PROMPT 為 SUCCESS、另一顆 `node_execution` 為 SKIPPED；LLM 回覆風格對應被活化那條提示 |
| P1 | 優先序：LLM 同時填了 `userPrompt` 且有 PROMPT 連入 | 以 PROMPT 節點輸出為準；刪掉 `in:prompt` 連線後再執行則改用 `userPrompt` |
| P1 | 某節點設定錯誤導致失敗 | 該節點顯示 `node.failed` 狀態與錯誤；執行標記失敗 |
| P1 | 執行中途 client 斷線（關抽屜 / 離開頁面） | 執行標記 `CANCELLED`，未執行下游節點標記 `SKIPPED`（對應 commit b24c128 的視覺行為） |

> **真實 LLM 斷言策略**：因輸出非確定，**不比對文字內容**；斷言聚焦於
> ①SSE 事件序列與型別正確、②每個節點狀態最終為 completed、③整體 `execution.completed=SUCCESS`、④DB 執行紀錄寫入。
> 逾時放寬（見 §6）；此案例需有效 `E2E_LLM_ID`，缺席時 skip 並在報告標註。

### J6 Inspector 表單（P1）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | LlmAssistantForm 選 `llmId`（下拉來自 LLM setting；表單已精簡，無工具/MCP/Skill/知識庫欄位） | 值寫回並可存檔；不再出現 `tool-ids` / `mcp-ids` / `skill-ids` / `knowledge-id` 欄位 |
| P1 | ToolForm / McpServerForm / SkillForm / KnowledgeRagForm / PromptForm / OutputForm 填寫 | 各節點 config 正確寫回；save 後重載一致（SkillForm 選 `skillId`，下拉來自 `/llm/skill/list`；PromptForm 填 `prompt`（`data-test="prompt-text"`）與選填 `outputKey`） |
| P1 | LLM 節點接上 PROMPT 後檢視 LlmAssistantForm | 顯示 `prompt-overridden-badge`（已由上游提示節點提供）與 `prompt-source-hint`；`user-prompt` 仍可編輯（作為後備值，非 disabled） |
| P2 | settingSchema 動態表單（sensitive 欄位遮罩） | 依 schema 正確渲染欄位型別 |

### J7 契約錯誤（P2）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P2 | save 節點 config 型別錯誤（未知欄位 / 結構性型別錯） | 400，`workflow.node.config.invalid`，UI 顯示含 nodeKey 的訊息 |
| P2 | 樂觀鎖：兩處先後 save 同一 workflow，version 不符 | 400，`workflow.version.conflict`，UI 顯示衝突提示 |

### J8 知識庫 RAG 檢索問答（P1，**真實 embedding + Milvus 向量庫 + 真實 LLM**）

> 驗證完整 RAG 能力：文件 → embedding 存 Milvus → workflow `TRIGGER → KNOWLEDGE_RAG → LLM_ASSISTANT → OUTPUT`
> → LLM 依向量查詢結果作答。**與 J5 的關鍵差異**：J8 有已知來源文件，故**斷言輸出內容與文件事實相符**
> （而非只驗 plumbing）。前端無向量庫／LLM 設定／檔案上傳的管理頁（僅 login/列表/編輯器三頁），
> 故前置三步一律走 API 準備，UI 只負責 workflow 建置與執行。

> KNOWLEDGE_RAG 有 **pipeline / 外掛** 兩種模式（差異見 §J4 的模式對照表），兩種都要測，
> 故本旅程分 **J8-A（pipeline）** 與 **J8-B（外掛）** 兩組。

#### J8-A pipeline 模式：`TRIGGER → KNOWLEDGE_RAG → LLM_ASSISTANT → OUTPUT`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | 前置（API，無 UI）：建 EMBEDDING 設定 + Milvus 向量庫 + 上傳文件建知識庫 | 取得 embeddingModelId / embeddingStoreId / knowledgeId；`getDataFromEmbeddingStore` 檢索回傳含目標事實的片段（**維度須與 embedding 模型一致**） |
| P1 | UI 建 workflow：拖 TRIGGER / KNOWLEDGE_RAG / LLM_ASSISTANT / OUTPUT，Inspector 選知識庫 / embedding / LLM，並以 `{{<ragKey>.documents}}` 於 userPrompt 串接上游檢索結果 | 4 節點、3 edge；nodeKey 為 nanoid（可由 `.vue-flow__node[data-id]` 讀取供插值） |
| P1 | 存檔 + switchStatus 啟用 | version 1；KNOWLEDGE_RAG 無條件必填僅 `knowledgeId` 驗證通過，狀態 ACTIVE（pipeline 模式的 `query` 屬**執行時**必填，啟用不擋——缺 query 時本組會在 execute 才失敗） |
| P1 | execute（真實 embedding + LLM） | SSE `execution.started` → 4 節點 `node.*` → `execution.completed`（SUCCESS）；`node_execution` 四節點皆 SUCCESS、DB 落庫 |
| P1 | **RAG 正確性斷言（本旅程核心，異於 J5）**：finalOutput 與文件已知事實比對 | LLM 輸出含文件原文事實關鍵字（如渣打 TNC「年滿**二十歲**」→ 輸出含「20 / 二十」） |

#### J8-B 外掛模式：KNOWLEDGE_RAG `out:main → LLM in:tool`（自動注入型 RAG）

> 圖形為 `TRIGGER → LLM_ASSISTANT → OUTPUT`，另掛 N 個 KNOWLEDGE_RAG 到 LLM 的 `in:tool`。
> **userPrompt 只寫問題，不做任何 `{{...}}` 知識插值**——注入由框架在推論前自動完成。
> 首次實測見 §14。

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | UI 建圖：5 節點（TRIGGER / LLM_ASSISTANT / OUTPUT / 2×KNOWLEDGE_RAG）、4 edge，其中 **2 條為 RAG `out:main` → LLM `in:tool`** | 拉線放行、無相容性 toast、console error = 0；Overview 顯示 5 NODES / 4 CONNECTIONS；存檔成功 |
| P1 | **必填契約**：兩個 RAG 節點只選知識庫，`query` / `embeddingModelId` 皆留空後啟用 | 啟用成功（不回 `workflow.node.config.required.missing`） |
| P1 | execute（真實 embedding + LLM） | SSE `execution.started` → **僅 trigger / llm / output 三節點** `node.*` → `execution.completed`（SUCCESS） |
| P1 | **純能力節點不落主遍歷**：執行後檢視 RAG 節點 | RAG 節點 Inspector **無**「本次執行」區塊（`data-test="node-exec-section"`）；`llm_workflow_node_execution` 無 `KNOWLEDGE_RAG` 型別紀錄 |
| P1 | **自動注入正確性斷言**：finalOutput 與文件已知事實比對，且需能與 LLM 常識答案區分 | 輸出為**文件事實**而非常識（如渣打 TNC「年滿二十歲」→ 輸出「20歲」，而非民法成年的「18歲」） |

> **斷言策略**：先讀來源文件挑出**可驗證的唯一事實**作為「查詢關鍵字 → 預期答案」對；
> 檢索為確定性（同一文件同一 query 命中固定片段），LLM 依提供的 context 作答，故**可斷言輸出含該事實**。
> ⚠️ J8-B 另需該事實**與 LLM 常識答案不同**，否則無法區分「注入生效」與「模型自己知道」——
> 渣打 TNC 的「年滿二十歲」對上民法成年「18 歲」即為合格鑑別點。
> 需有效 embedding 與 CHAT 兩組設定；缺任一即 skip 並在報告標註。實測見 §13（J8-A）、§14（J8-B）。

---

## 4. 測試資料策略

- **命名**：測試建立的 workflow 一律以 `e2e-<caseId>-<runTag>` 前綴命名，便於識別與掃描殘留。
- **清理**：
  - 每條測試 `afterEach` 呼叫 `POST /llm/workflow/delete` 移除該條所建 workflow（連鎖刪 node/edge/execution）。
  - `globalTeardown` 再以前綴掃描 `workflow/list` 兜底刪除任何殘留 `e2e-*`。
  - **不建立任何備份檔 / 目錄**；E2E 產生的資料視為髒資料直接刪除，遵守「備份只能寫 `docs/sql/bestpartner-init-data.sql`」的專案鐵則。
- **種子資料**（`admin`、`test_user`、LLM setting）視為前置條件，由 setup **檢查**，測試不建立 / 不刪除。

---

## 5. 目錄與檔案佈局（規劃，本次不實作）

```
bestpartner-ui/
├── e2e/
│   ├── playwright.config.ts        # baseURL / 逾時 / reporter / projects
│   ├── global-setup.ts             # 健檢 port 80、種子檢查、產生 storageState
│   ├── global-teardown.ts          # 前綴掃描兜底清理
│   ├── fixtures/
│   │   ├── auth.ts                 # storageState 載入、API 登入 helper
│   │   ├── workflow-api.ts         # 經 API 建立 / 刪除 workflow 的 helper
│   │   └── seed.ts                 # 種子資料檢查（LLM setting 等）
│   ├── pages/                      # Page Object：LoginPage / WorkflowListPage / EditorPage / InspectorPanel / ExecutionDrawer
│   └── specs/
│       ├── j1-auth.spec.ts
│       ├── j2-workflow-list.spec.ts
│       ├── j3-canvas-edit.spec.ts
│       ├── j4-validate-activate.spec.ts
│       ├── j5-execute.spec.ts
│       ├── j6-inspector-forms.spec.ts
│       └── j7-contract-errors.spec.ts
└── package.json                    # 新增 script：test:e2e / test:e2e:ui
```

- 採 **Page Object** 模式隔離選擇器與操作，spec 只描述旅程。
- Vue Flow 畫布互動（拖拉、連線）以穩定的 `data-testid` 定位；若現況缺 testid，於實作階段補上（屬 UI 微調，非邏輯變更）。

---

## 6. 技術注意事項

- **SSE 串流**：execute 為 `text/plain` SSE。以 Playwright 監看 UI（ExecutionResultDrawer 狀態轉移）為主，必要時輔以 `page.waitForResponse` / 攔截網路事件驗證事件序列。
- **真實 LLM 逾時**：J5 單案例逾時放寬至 60–120s（可經 config 調整），並加入重試容忍（retries=1）以吸收偶發網路抖動；**斷言不依賴輸出文字**。
- **拖拉互動（兩種機制，勿混用；已實測驗證）**：
  - **palette → 畫布新增節點**是 **HTML5 原生 DnD**（palette item `draggable=true` + `dragstart` 寫 `dataTransfer`，畫布 `.canvas` 監 `drop`）。Playwright 須**合成 DnD 事件**：於 `page.evaluate` 內建一個**共用 `DataTransfer`**，依序 `dispatchEvent` `dragstart`(source) → `dragenter`/`dragover`/`drop`(`.canvas`，帶 `clientX/clientY`)。**`mousedown→move→up` 驅動不了原生 DnD，勿用**。
  - **節點連線**才是 pointer/滑鼠序列：`mouse.move(sourceHandle)→down→move(targetHandle,{steps})→up`，handle 以 `[data-node-type="X"] .vue-flow__handle[data-handleid="in:tool"]` 定位。
  - 每步後斷言 `.vue-flow__node` / `.vue-flow__edge` 數量再繼續，避免競態。
- **選擇器策略**：專案用 `data-test`（非 Playwright 預設 `data-testid`），config 須設 `testIdAttribute:'data-test'`。節點根已加 `data-node-type` 供依型別定位；優先 `getByRole`/`getByTestId`，避免耦合 i18n 文字。
- **登入**：email 欄位為 `type=email`（原生驗證擋非 email），須用真實 email（admin 為 `admin@bestpartner.com.tw`）而非裸 `admin`。
- **ID 動態解析（重要）**：運行 dev DB 的 `llmId`/`toolId` 與 `docs/sql` 種子檔會漂移（實測 OpenRouter CHAT 於本機為 `1ee80ffa…`、種子檔為 `583b9222…`）。**禁止硬編 ID**；於 `global-setup` 以 API 依 alias/platform/name 解析當前 DB 真實 ID，寫入 `.artifacts/seed.json` 供 spec 讀取（`E2E_LLM_ID` 可覆寫，缺則 J5 skip）。
- **隔離性**：各 spec 自建自清資料；storageState 唯讀復用，不被測試改寫。

---

## 7. 與 `docs/api-test-plan.md` 的對照

| API 測試計畫案例 | 由哪條 E2E 旅程間接覆蓋 |
|------------------|------------------------|
| AUTH 登入 / check | J1 |
| WORKFLOW create / save / get / list / delete | J2、J3 |
| WORKFLOW switchStatus 啟用前置與必填驗證 | J4 |
| WORKFLOW execute SSE happy path 與 CANCELLED/SKIPPED | J5 |
| WORKFLOW save 型別驗證、樂觀鎖 | J7 |
| TOOL settingSchema、LLM SETTING 取值 | J6（間接，經 Inspector 下拉 / 動態表單） |
| VECTOR save / uploadFiles / getDataFromEmbeddingStore、WORKFLOW execute（KNOWLEDGE_RAG 節點） | J8 |

> API 測試計畫仍是端點行為的權威來源；E2E 只驗「使用者路徑上這些契約確實被正確串接」。

---

## 8. CI 藍圖（第二階段，本次不實作）

E2E 需真實後端 + Postgres + 有效 LLM api_key，較重，分兩階段落地：

1. **第一階段（本機）**：開發者本機起 port 80 服務 + Postgres，`npm run test:e2e` 跑主軸旅程。J5 真實 LLM 案例需本機提供 `E2E_LLM_ID`。
2. **第二階段（CI，GitHub Actions）**：
   - `services: postgres` 起 DB → 匯入 `bestpartner-ddl.sql` + `bestpartner-init-data.sql`。
   - build backend uber-jar → 以 dev/sit profile 起 port 80。
   - `npm ci && npm run build && npm run preview` 起前端。
   - `npx playwright install --with-deps` → `npm run test:e2e`。
   - **LLM api_key 以 GitHub Secret 注入**；缺 secret 時 J5 真實執行案例自動 skip（其餘旅程照跑），避免 fork PR 洩鑰或無鑰紅燈。
   - 產出 Playwright HTML report + trace 為 artifact。
   - 與既有 `harness.yml`（ArchUnit + 漂移掃描）並列為獨立 workflow，避免拖慢核心把關。

---

## 9. 維護規則

- **程式碼修改後必檢視測試清單（強制）**：任何前端 UI / workflow 節點型別 / 執行事件 / 後端 API 的程式變更，宣告完成前必須檢視本計畫的旅程矩陣（§3）與 `e2e-test-confirmation` skill 的清單模板是否需新增或調整確認項目；需要則先更新本文件再同步模板，判定不需調整時亦須於變更說明中明述已檢視。
- 新增或修改 UI 路由 / workflow 節點型別 / 執行事件時，本文件與對應 spec 須同步更新（由 `documentation-sync` skill 把關）。
- **`in:tool` 能力來源白名單（`CAPABILITY_SOURCE_TYPES`）異動時**，§J3（能力掛載）、§J4（不相容連線 + 白名單註）、§J5（Agent 模式）、§J8（若涉及 RAG）四處案例須一併更新，並同步 `e2e-test-checklist.md`。此白名單前後端各有一份（`WorkflowEngine.kt` / `useGraphValidation.ts`），改一邊必改另一邊。
- **`in:prompt` 提示埠規則異動時**（允許來源、提問優先序、兩項提示接線驗證），§J3（提示接線）、§J4（負向案例 + 兩埠對照表）、§J5（分支擇一執行）、§J6（PromptForm 與徽章）四處案例須一併更新，並同步 `e2e-test-checklist.md`。此規則前後端各有一份（`WorkflowEngine.kt` 的 `PROMPT_INPUT_HANDLE` ＋兩個純函式 / `useGraphValidation.ts` 的相容性與 `PROMPT_WIRING_INCOMPLETE`），改一邊必改另一邊。
- E2E 旅程若涉及 API 契約變更，須同步 `docs/api-test-plan.md` 與 `.claude/rules/api-endpoints.md`。
- 測試資料清理策略異動時，須確認仍不違反「備份只能寫 `bestpartner-init-data.sql`」鐵則。

---

## 10. 首條切片實測發現（2026-07-12）

首條 J5 垂直切片已落地並在真實 stack（port 80 + Postgres + 真實 OpenRouter）跑通，harness 位於 `bestpartner-ui/e2e/`（`playwright.config.ts` / `global-setup.ts` / `global-teardown.ts` / `fixtures/db.ts` / `fixtures/canvas.ts`（畫布 DnD／連線共用 helper） / `specs/j5-openrouter-date-json.spec.ts` / `specs/j5-openrouter-date-mcp-json.spec.ts`）。此切片：TRIGGER → LLM_ASSISTANT(OpenRouter, JSON) ←in:tool← TOOL(DateTool) → OUTPUT(JSON)，並直連 Postgres 斷言 `llm_workflow_execution` / `llm_workflow_node_execution` 落庫。

**已驗證**：HTML5 DnD 合成事件可靠、TRIGGER 無表單走 `JsonConfigEditor` raw 填 `triggerType`、`data-test` 選擇器、DB 落庫、ID 動態解析（見 §6）。

**待後端釐清（不阻擋 E2E，但影響斷言語意）**：

1. **TOOL(date) 節點會被引擎「獨立執行」且需 `arguments.zoneId`**：`ToolNodeExecutor` 直接呼叫 `DateTool`，未帶 `zoneId` 時 `ZoneId.of(null)` 拋 `ZoneRulesException` 使整條 FAILED；且該節點會落 `nodeType=TOOL` 執行紀錄——與「連 `in:tool` 僅為能力掛載、不獨立執行」的描述矛盾。
2. **連到 `in:tool` 的 date 工具未真正掛給 LLM 當能力**：實測 LLM 回「此環境未提供可查詢即時時間的工具」，代表 Agent 模式的 langchain4j 工具集未納入該 TOOL 節點。故 J5 此切片**只斷言 plumbing / SSE `SUCCESS` / OUTPUT 為 JSON / DB 落庫，不斷言 LLM 實際呼叫了工具**，待後端修復後再強化。

---

## 11. 第二切片實測發現（2026-07-16，MCP 變體）

E2E 週期 202607162121 以 webwright 驗證後固化為 `specs/j5-openrouter-date-mcp-json.spec.ts`：TRIGGER → LLM_ASSISTANT(OpenRouter, JSON) ←in:tool← **MCP_SERVER(date, STDIO `java -jar D:/MCP/date.jar`)** → OUTPUT(JSON)。`mcpId` 由 `global-setup` 依 name（預設 `date`，`E2E_MCP_NAME` 可覆寫）解析入 `seed.json`（`dateMcpId`），缺席時該 spec skip。

1. **execute 前置驗證也驗能力節點必填欄位**：`WorkflowEngine.validateForExecution` 逐節點驗 `nodeRequiredFields` 契約，MCP_SERVER 缺 `toolName` 時 execute 直接回 400 `missing required config fields: toolName`——即使該節點為 `in:tool` 純能力掛載（Agent 模式僅聚合 `mcpId`/`userSettingId`，不使用 `toolName`）。spec 以 `E2E_MCP_TOOL_NAME`（預設 `getTodayDate`；date.jar 共 6 個 tools）填入通過驗證。**UI 面**：驗證擋下時畫面僅顯示「HTTP 400」，未帶出後端訊息的 nodeKey 與缺漏欄位，列為 UI 改善候選。
2. **§10 待修事項 1 已修復（正面回歸確認）**：能力節點不再獨立執行、不落 `node_execution`（實測 node_execution 僅 TRIGGER/LLM_ASSISTANT/OUTPUT）；兩支 J5 spec 均以 `not.toContain(...)` 斷言此行為。
3. **MCP 路徑的工具掛載有效**（對照 §10 待修事項 2）：LLM 回覆可感知 date MCP 工具集（主動提及可列出可用時區），代表 MCP 經 `in:tool` 掛載進 Agent 工具集正常；TOOL 路徑是否仍有掛載問題，待另行驗證後再強化斷言。
4. **畫布座標陷阱**：viewport 內拖放座標若落在右側 Workflow Overview 面板底下（1280 寬時 x≳950；本 harness viewport 1680×950 時 x≳1370），節點 handle 會被面板攔截 pointer 事件導致連線靜默失敗。spec 逐條連線後斷言 edge 數即可即刻定位。

---

## 12. 第三切片實測發現（2026-07-22，google_map MCP 變體 · deep-verify）

E2E 週期 202607222159 以 webwright 驗證後固化為 `specs/j5-openrouter-googlemap-json.spec.ts`：TRIGGER → LLM_ASSISTANT(OpenRouter deepseek-v3.2, JSON) ←in:tool← **MCP_SERVER(google_map, STDIO Google Places API)** → OUTPUT(JSON)，查詢「新北市金山區附近咖啡廳」。`googleMapMcpId` 由 `global-setup` 依 name（預設 `google_map`，`E2E_GM_MCP_NAME` 可覆寫）解析入 `seed.json`，缺席時 skip。報告見 `docs/test-confirmations/e2e-test-confirmation-202607222159.md`。

1. **需 env 機密的 MCP → 節點必須帶 `userSettingId`（新發現，關鍵）**：google_map 需 `GOOGLE_MAPS_API_KEY`。`LlmAssistantExecutor` 對 in:tool 掛載的 MCP_SERVER，**有 `userSettingId` 才走 `buildUserSpecificMcpClient` 注入使用者 env**；僅填 `mcpId` 會走 `buildDefaultMcpClient`、env 佔位 `${...}` 不替換，工具形同不可用。故 spec 於 MCP 節點填 `data-test="mcp-setting-id"`（date 切片因無 env 需求未觸及此欄）。
2. **執行身分＝設定擁有者**：LLM 與 MCP 設定皆以「登入者 userId」為範圍（`LLMService.buildLLM` 用 `validateLoggedInUser()`；`McpServerService.buildUserSpecificMcpClient` 以 `(userSettingId, userId)` 配對）。故本切片以 env 參數化登入身分（`E2E_GM_USER_EMAIL/PASS`）＋ `E2E_GM_SETTING_ID`（該使用者的 google_map userSettingId；後端無列舉端點故走 env），三者任一缺即 skip。
3. **deep-verify 通過（真實 Places 資料）**：LLM 實際呼叫 google_map 回傳 5 間金山實際咖啡廳（洋荳子咖啡 4.1／跳石沒有名字的咖啡店 4.3／海灣綠洲咖啡 4.2／舊金山總督溫泉咖啡廳 3.9／巴薩利斯克小館 4.4，皆含新北市金山區門牌）。**因其依賴有效 Maps 金鑰，CI spec 只斷言 plumbing/SSE SUCCESS/OUTPUT 為 JSON/DB 落庫（node_execution 不含 MCP_SERVER）**，真實地點斷言僅記錄於報告。
4. **IS-1 過期 jar 教訓（環境陷阱，非程式缺陷）**：首跑 execute 於 62ms `node.failed`＝`Database query failed: IllegalArgumentException`；深查為**執行中的 runner jar（建於 07-19 21:45）早於整欄加密 converter（commit `5c68b47`，07-22 08:10）**——DB 已加密但舊 jar 無 converter，讀 `setting_content` 時把密文當 JSON `Map` 反序列化而失敗（`/llm/mcpServer/getSetting` 同錯回 400）。**重編後端 uber-jar 後即恢復**（getSetting 轉 200、execute SUCCESS）。**E2E Step 4 起後端前，務必比對 jar 建置時間與最後一次後端 commit，落後即重編**（`e2e-test-confirmation` skill 已載明「後端有異動先重編」）。

---

## 13. J8 首條切片實測發現（2026-07-23，知識庫 RAG · deep-verify）

E2E 週期 202607232111 以 webwright（Node Playwright + chromium）驗證 J8：文件 `docs/rag/doc/scb/tw-online-tnc.pdf`（渣打數位存款帳戶特別約定條款）→ Milvus embedding → workflow `TRIGGER → KNOWLEDGE_RAG → LLM_ASSISTANT(OpenRouter deepseek-v3.2) → OUTPUT`，查詢「數位存款帳戶開立的年齡條件」。報告見 `docs/test-confirmations/e2e-test-confirmation-202607232111.md`。**15/15 通過（Phase A API 前置 6 + Phase B UI 9）**。

1. **RAG 正確性成立（deep-verify）**：以 PDF 原文「立約人應為…**年滿二十歲**自然人」為斷言依據。API 檢索（`getDataFromEmbeddingStore`）Top-1 片段即命中該句；UI 執行後 LLM finalOutput＝`{"result":"…開立數位存款帳戶的年齡條件是年滿 **20** 歲。"}`，與原文事實一致。**證明 J8 的「斷言輸出內容」策略對有已知來源的 RAG 是可行且必要的**（有別於 J5 不比對文字）。
2. **前端無設定管理頁（關鍵限制）**：`bestpartner-ui` 僅 `/login`、`/`、`/editor/:id?` 三個路由。**向量庫設定（`/llm/vector/save`）、LLM/embedding 設定（`/llm/setting/save`）、PDF 上傳（`/llm/vector/uploadFiles`）皆無 UI，一律走 API 準備**；UI 只在知識庫/設定「已存在」後於 RAG 節點下拉選用。J8 因此天生是「API 前置 + UI 執行」混合。
3. **embedding 選型與維度對齊陷阱**：本機**無 Ollama**（DB 唯一 embedding 設定 `ollama_local_embedding_test` bge-m3 dim 1024 不可用）。改用 **OpenRouter `nvidia/nemotron-3-embed-1b:free`**（免費、實測輸出**維度 2048**）。⚠️ OpenRouter `/api/v1/models` 目錄**不列 embedding 模型**，但 `/api/v1/embeddings` 端點與該模型可正常呼叫；後端 `OpenrouterModelBuilder.embeddingModel` 以 `OpenAiEmbeddingModel` 對接。**Milvus collection dimension 必須等於 embedding 實際輸出維度**（此處 2048），否則 `addAll`/`search` 失敗——建 collection 前先呼叫一次 `/embeddings` 量測維度最保險。
4. **KNOWLEDGE_RAG 節點插值需 nanoid nodeKey**：nodeKey 為 `nanoid(8)` 隨機值、不顯示於節點字幕，但 Vue Flow 於 `.vue-flow__node[data-id="<nodeKey>"]` 暴露，webwright 拖放後即可讀取，據以組 `{{<ragKey>.documents}}` 填 LLM userPrompt。採**靜態 query**（不依賴執行時 inputPayload，UI 無輸入面板）。
5. **Inspector 表單為原生控件**：KnowledgeRagForm / LlmAssistantForm / OutputForm 用原生 `<select>`/`<textarea>`（非 Element Plus），可直接 `selectOption({value})`；option value＝knowledgeId / 設定 id，故用前置解析到的真實 ID 選取最穩。
6. **憑證解密受分類器管制**：`.env` 的 OpenRouter 金鑰以 `${enc::}` 加密，自動模式分類器**擋下程式化 `decryptConfigSecret`**；需請使用者以 `!` 自解或直接提供金鑰（本次由使用者提供）。金鑰只寫入 scratchpad 暫存、收尾即刪，不落報告。
7. **占位陷阱（對照 §11-4）**：Inspector 面板開啟時佔右側約 322px（1280 寬 → x≳958 被遮），拖放節點或選取右側節點會被面板攔截 pointer。解法：節點放左側 60% 區、選取前先點空白 pane 收合面板（deselect）。

---

## 14. J8-B 首條切片實測發現（2026-07-27，KNOWLEDGE_RAG 外掛模式 · deep-verify）

E2E 週期 202607272132 以 webwright 工作區契約（Node Playwright + chromium）驗證 J8-B：
`TRIGGER → LLM_ASSISTANT(OpenRouter deepseek-v3.2) → OUTPUT`，另掛 **2 個 KNOWLEDGE_RAG 至 LLM `in:tool`**。
報告見 `docs/test-confirmations/e2e-test-confirmation-202607272132.md`。**5/5 通過**。

1. **外掛模式的必填契約確認成立**：兩個 RAG 節點僅選 `knowledgeId`，`query` / `embeddingModelId` 全空仍可
   switchStatus 啟用（不回 `workflow.node.config.required.missing`）。這與 J8-A（pipeline）需 `query` 相反，
   §J4 已補模式對照表；**寫案例時務必先確認是哪一種模式**，否則預期會寫反。
2. **純能力節點的 UI 斷言點**：執行後點選 RAG 節點，Inspector **不出現**「本次執行」區塊
   （`data-test="node-exec-section"`）；點 LLM / OUTPUT 節點則會出現。這比只看 DB 更貼近使用者感知，
   建議兩者併用（DB 端斷言 `llm_workflow_node_execution` 無 `KNOWLEDGE_RAG` 型別列）。
3. **RAG 斷言必須挑「與 LLM 常識不同」的事實（關鍵教訓）**：前次週期（202607252225）以「開戶最低年齡」
   提問得到「18歲」並記為疑似未注入——因 18 歲正是台灣民法成年，屬 LLM 常識。本次向量庫修復後同一問題
   輸出「**20歲**」，與 TNC 原文「年滿二十歲」一致。**若選到常識與文件一致的事實，該案例無鑑別力**，
   等同沒測到注入。§J8-B 斷言策略已載明此要求。
4. **Milvus 未啟動時的失敗樣態**：`POST /llm/workflow/execute` 仍回 **HTTP 200**（SSE 已建立），
   失敗發生在節點執行階段，Drawer 顯示 gRPC `DEADLINE_EXCEEDED: CallOptions deadline exceeded after ~10s`。
   **勿以 execute 的 HTTP 狀態碼判斷 RAG 環境是否就緒**；前置檢查應直接打 `getDataFromEmbeddingStore`
   確認命中（空陣列即代表向量未入庫或 Milvus 不可用）。
5. **`deleteData` 不連鎖刪 `llm_knowledge`（環境陷阱）**：`DELETE /llm/vector/deleteData` 只清
   `llm_doc` / `llm_doc_slice` 與向量，`llm_knowledge` 列留存。**以同一 knowledgeId 重新上傳會撞
   `llm_knowledge_pkey` 唯一鍵**，回「Database processing error」（後端日誌才看得到真因）。
   重建知識庫時請改用新的 knowledgeId。
6. **Milvus collection 會隨容器 volume 重建而清空**：知識庫 metadata 在 Postgres、向量在 Milvus，
   兩者不同步時 `getKnowledgeStore` 仍列得出知識庫、檢索卻回空。E2E 前置務必以檢索命中作為就緒判準，
   而非只看知識庫是否列得出來。
7. **本機無 Python Playwright（環境現況）**：`python` / `python3` 為 Windows Store stub，
   webwright 契約的 `playwright.firefox` 不可用；已安裝瀏覽器僅 chromium。
   實作改用 `bestpartner-ui/node_modules/@playwright/test` + chromium，**保留工作區契約**
   （`plan.md` / `final_runs/run_<id>/` / 逐步截圖 / `final_script_log.txt`）。
   腳本置於 scratchpad 時，`@playwright/test` 需以絕對路徑動態 import（模組解析基準是腳本位置而非 cwd），
   且該套件為 CJS，ESM interop 下 `chromium` 可能落在 `default` 上，需兩者皆試。
8. **`INCOMPATIBLE_CONNECTION` 是前端專屬、且經 UI 觸達不到 save（run_3 實測）**：以 `HTTP_REQUEST`
   拉向 LLM `in:tool`，`onConnect` 當下即以 toast 擋下且 **edge 根本不建立**（CONNECTIONS 維持不變）；
   同一埠改由 `KNOWLEDGE_RAG` 連入則成功，證明守衛依白名單選擇性放行。該錯誤常數只在
   `useGraphValidation.ts`，後端無對應 `AppMessage`——寫案例時勿寫成「save 回 `INCOMPATIBLE_CONNECTION`」。
9. **孤兒 SKILL 的檢查點在 execute，不在 save / switchStatus（run_3 實測，與原文件記載不符）**：
   實測 save → 200、switchStatus → 200（狀態確實轉 `ACTIVE`、UI 顯示「流程已啟用」），
   execute → 400 `SKILL node "<nodeKey>" must be connected to an LLM assistant nodes tool input port (in:tool)`。
   成因：檢查在 `WorkflowEngine.validateNodes`（`WorkflowEngine.kt:344`）僅由 `validateForExecution` 呼叫，
   而 `WorkflowService.switchStatus`（`WorkflowService.kt:220`）只驗「有 TRIGGER、無環、逐節點必填 config」。
   **後果：帶孤兒 SKILL 的 workflow 可停在 ACTIVE 卻永遠執行不了**；§J4 案例預期已改為記錄實際行為。
10. **測 J4-07 必須先有可選的 skill**：`SKILL` 的 `skillId` 為必填，留空會先撞
   `workflow.node.config.required.missing`（必填檢查在孤兒檢查之前），根本測不到本案例。
   本次以最小 zip（僅 `skill.md`）經 `POST /llm/skill/upload` 建 `e2e-orphan-skill`，收尾即刪。
   ⚠️ 該端點在 Git Bash 下 `curl -F file=@/c/...` 會失敗（HTTP 000），須用 Windows 路徑形式 `C:\...`。
11. **`create-button` 不呼叫後端**：實測 HTTP 序列無 `workflow/create`，workflow 直到按「存檔」才落庫。
   故未存檔的測試流程不會產生殘留資料——但也代表「建立即有 id」的假設不成立，案例勿依賴之。
