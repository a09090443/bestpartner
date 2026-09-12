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

> **流程守門已 hook 化**：`.claude/hooks/e2e-flow-guard.ps1` 會機械化把關「開測前確實停過服務」
> 「未經使用者核准不得改產品程式碼」「收工前報告填完且服務已關」。
> 說明見 [`.claude/hooks/README.md`](../.claude/hooks/README.md)。

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
| `E2E_HTTP_TEST_URL` | J11 的 `HTTP_REQUEST` 目標 | 預設 `http://localhost/systemSetting/list`（本機服務自身的公開端點，**刻意不依賴外網**以免網路抖動造成偽紅燈） |

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
| P0 | 建立新 workflow（命名 → 建圖 → **存檔**） | 落庫為 DRAFT、version 1。⚠️ **URL 不會帶 id**——`create-button` 不呼叫後端（§14-11），且存檔後路由也不改寫，workflow id 須於存檔後以 `workflow/list` 依名稱解析（§15-10）|
| P1 | 從列表點項目進編輯器 | 載入該 workflow 完整定義 |
| P1 | 編輯器點返回鈕（`data-test="back-button"`，無未存變更） | 導回列表 `/`，列表重新載入 |
| P1 | 編輯器有未存變更時點返回（或麵包屑 `data-test="breadcrumb-list"`） | 先跳「尚未存檔」確認（由 `onBeforeRouteLeave` 統一處理，非返回鈕自行跳窗）；選「離開」導回 `/`、選「留下」停在編輯器 |
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
| P1 | 圖有環時**存檔** | 400「畫布存在有向環，請移除循環連線」。⚠️ 把關點在 `workflow/save`（存檔前即驗無環），**帶環的圖存不進 DB，因此「帶環後去啟用」經 UI 不可達**；勿寫成 switchStatus 的預期 |
| P1 | 節點缺必填 config 啟用（如 LLM_ASSISTANT 缺 `llmId`） | 400，`workflow.node.config.required.missing`，UI 顯示 nodeKey 與缺漏欄位 |
| P1 | 孤兒 SKILL 節點（未連任何 LLM `in:tool`）save → 啟用 | save 放行（DRAFT 容許未完成）；**switchStatus 回 400** `workflow.skill.node.not.mounted`（訊息含 nodeKey），開關維持關閉、狀態仍 DRAFT。execute 亦回 400（第二層防線） |
| P1 | SKILL 已掛載到 LLM `in:tool` 後啟用（上一條的對照組） | 啟用成功，狀態轉 ACTIVE |
| P1 | **非能力來源節點**拉線到 LLM `in:tool` 埠（能力來源＝`TOOL` / `MCP_SERVER` / `SKILL` / `KNOWLEDGE_RAG`，見下方註） | `onConnect` 即擋下：toast「僅工具、MCP、Skill、知識庫節點可連到 LLM 的工具埠」，**edge 不建立**（CONNECTIONS 計數不變）。建議併測對照組：同一埠改由能力來源連入應成功 |
| P1 | KNOWLEDGE_RAG 掛 `in:tool`（外掛模式）且 `query` / `embeddingModelId` 留空時啟用 | **啟用成功**（外掛模式僅需 `knowledgeId`；檢索語句由 LLM 問題自動帶入、embedding 由知識庫本身設定決定） |
| P1 | 孤兒 PROMPT 節點（未連任何 LLM `in:prompt`）save → 啟用 | save 放行（DRAFT 容許未完成）；**switchStatus 回 400** `workflow.prompt.node.not.connected`（訊息含 nodeKey），狀態仍 DRAFT。execute 亦回 400（第二層防線） |
| P1 | LLM 節點 `userPrompt` 留空且無 PROMPT 連入 `in:prompt` 時啟用 | **400** `workflow.llm.prompt.required`（訊息含 LLM 的 nodeKey），狀態仍 DRAFT |
| P1 | LLM `userPrompt` 留空但已有 PROMPT 連入 `in:prompt`（上兩條的對照組） | 啟用成功，狀態轉 ACTIVE |
| P1 | **非 PROMPT 節點**拉線到 LLM `in:prompt` 埠（如 TRIGGER） | `onConnect` 即擋下：toast「僅提示詞節點可連到 LLM 的提示埠」，**edge 不建立**（CONNECTIONS 計數不變） |
| P1 | 已連 PROMPT 的 LLM 節點在 Inspector 檢視（**與 §J6 的同名案例為同一情境**，確認表模板以 J6-04 收錄，J4 不重複計數） | 「使用者提示」欄旁顯示 `data-test="prompt-overridden-badge"`（已由上游提示節點提供）與 `prompt-source-hint`；欄位仍可編輯（非 disabled） |
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

| P1 | execute 被後端擋下時（如含孤兒 PROMPT 節點）檢視執行結果面板 | 顯示**後端業務訊息**（含 nodeKey 與應連往的埠），**不得**只顯示 `HTTP 400`。⚠️ execute 是唯一走原生 `fetch`、不經 axios 攔截器的端點，其錯誤訊息解析在 `api/workflowExecution.ts` 自行實作，改動該檔時須重驗此條 |
| P0 | **多觸發點選擇器**：畫布放兩顆 TRIGGER（各接一條分支匯流到同一顆 LLM）後點工具列執行鈕 | 出現 `data-test="trigger-picker"`，列出兩個選項（`trigger-option-<nodeKey>`，顯示節點名稱）；**尚未**開始執行 |
| P0 | **選定入口後只跑該分支**：點 `trigger-option-<t1>` | 只有 t1 那條分支的節點依序亮起（`node.started`/`node.completed`）；**另一顆 TRIGGER 呈 skipped 樣式**（`.is-exec-skipped`，由前端預標——後端對 SKIPPED 不發事件）；結果面板輸出對應該分支 |
| P1 | **改從另一個入口執行**：再點執行鈕、改選 `trigger-option-<t2>` | 前一輪的 skipped 標記不殘留；本輪換成 t1 呈 skipped；`llm_workflow_execution.trigger_node_key` = `t2` |
| P1 | **節點卡「從此處執行」**：直接點 TRIGGER 節點卡右上的 `data-test="node-run-from-here"` | 不經選擇器直接以該觸發點開跑；且**不會**觸發節點拖曳或取消選取（按鈕已 `@click.stop` / `@mousedown.stop`） |
| P1 | **單一觸發點不彈選擇器**：畫布只有一顆 TRIGGER 時點執行鈕 | 直接開跑、無額外點擊；請求仍明確帶 `triggerNodeKey`（UI 永遠是個別執行語義） |
| P2 | 選擇器點取消（`trigger-picker-cancel`）或點遮罩 | 關閉選擇器且不開始執行 |
| P2 | **無觸發節點**：畫布無 TRIGGER 時點執行鈕 | 顯示「流程尚無觸發節點」提示，不開跑、不彈選擇器 |
| P2 | **觸發點未接下游**：畫布有 TRIGGER 但無出邊 | 前端回報 `TRIGGER_NO_DOWNSTREAM`（warning，不擋存檔） |

> **真實 LLM 斷言策略**：因輸出非確定，**不比對文字內容**；斷言聚焦於
> ①SSE 事件序列與型別正確、②每個節點狀態最終為 completed、③整體 `execution.completed=SUCCESS`、④DB 執行紀錄寫入。
> 逾時放寬（見 §6）；此案例需有效 `E2E_LLM_ID`，缺席時 skip 並在報告標註。

### J6 Inspector 表單（P1）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | 自 palette 拖入 TRIGGER 後直接檢視 Inspector | `TriggerForm`（`data-test="trigger-type"`）顯示且值已為 `MANUAL`，**無必填欄位警告**——`triggerType` 由 `NodeTypeMeta.defaultConfig` 於拖放時帶入。`WEBHOOK` / `CRON` 選項列出但 `disabled` |
| P1 | 對無型別化表單的節點（如 CODE）在 JSON 編輯器的**表格模式**新增欄位 | 以 `new-field-key` + `add-field` 可新增鍵（空鍵名／重複鍵顯示 `new-field-error`），`remove-field-<key>` 可移除；config 為空時不再只能切 JSON 模式手打 |
| P0 | 選取尚未設定的 OUTPUT 節點 | 顯示 `output-both-empty`（擇一必填提示）與 `output-ref-suggestions`（上游可引用輸出 chip，顯示為「節點名 › 欄位」）；點 chip 插入到模板游標處，提示隨即消失。⚠️ 建議鍵取自上游**實際執行輸出**，未執行過則用 `constants/nodeOutputKeys.ts` 的型別預設鍵——該表手抄自後端 executor，改 executor 預設鍵須同步 |
| P0 | 檢視含引用的輸出模板 | 引用渲染成色塊 `expr-token-<path>`，文字為「節點名 › 欄位」、`data-ref` 保留原始 `{{...}}`；**節點改名**後色塊文字跟著變而 `data-ref` 不變；引用不存在的節點時色塊帶 `is-unknown`。⚠️ 該欄位是 **contenteditable 而非 textarea**：Playwright 斷言要用 `textContent()`，**不可用 `inputValue()`** |
| P1 | 於輸出模板以**中文輸入法**打字 | 組字期間不得吃字、不得把注音／拼音半成品寫進 config；commit 後文字完整、既有引用色塊不受影響。存檔後 DB 中的 `template` 應為「原始 `{{...}}` ＋ 中文」，**不含**畫面上的顯示文字（如「LLM 助手 ›」） |
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

### J9 編輯器外觀與節點編輯頁（P1 / P2）

> 本旅程只驗 UI 行為，**不需後端資料變更**，可併入 J3 的同一個 workflow 進行。

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | 首次進編輯器（localStorage 無 `wf-theme`） | 根元素 `.wf-editor` 的 `data-wf-theme="light"`（淺色為預設） |
| P1 | 點主題切換鈕（`data-test="theme-toggle"`） | `data-wf-theme` 翻為 `dark`、`<html>` 加上 `dark` class、`localStorage.wf-theme="dark"`；重整後仍為深色 |
| P1 | 深色狀態下離開編輯器到 `/login` 或 `/`（列表） | `<html>` 的 `dark` class 已移除，兩頁維持 Element Plus 淺色（驗證 `onScopeDispose` 清理） |
| P2 | 深色下觸發 `ElMessageBox`（未存離開／登出確認） | 對話框為深色配色且文字可讀（EP 深色走 `html.dark`，teleport 到 body 仍生效） |
| P1 | 自訂縮放列（`zoom-bar`）：點 `zoom-in` / `zoom-out` / `zoom-fit` | 畫布縮放改變，`data-test="zoom-value"` 的百分比隨之更新 |
| P0 | **雙擊節點**開啟 Node Designer | 出現 `data-test="node-designer-modal"`；header 顯示型別、分類與名稱；該節點同時被選取（Inspector 亦切到該節點） |
| P1 | 由節點卡右上角鈕（`data-test="node-open-designer"`）開啟 | 同上；且**不會**觸發節點拖曳或取消選取（按鈕已 `@click.stop` / `@mousedown.stop`） |
| P1 | 由 Inspector 的 `data-test="open-node-designer-button"` 開啟 | 同上 |
| P1 | 三種關閉方式：Esc、點遮罩、`node-designer-close` | modal 皆消失；再次開啟時分頁回到 Parameters（`v-if` 卸載重建） |
| P0 | 在 modal 的 Parameters 改 config → 關閉 | Inspector 表單與畫布節點副標同步更新（兩處共用同一份 `node.data.config`）；工具列標記為「未存」 |
| P1 | 在 modal 的名稱輸入框打字後按 <kbd>Delete</kbd> | **節點不會被刪除**（Vue Flow 的 `delete-key-code` 綁在 pane 上，modal 在 `.vue-flow` DOM 之外） |
| P1 | 未執行流程時檢視 Input / Output 欄 | 兩側皆顯示 `node-designer-input-empty` / `node-designer-output-empty`（「尚未執行」） |
| P1 | 執行流程後開啟中段節點的 Designer | Input 顯示 `node-designer-input-json`（含 `來自 <上游名>`）、Output 顯示 `node-designer-output-json` 與可引用欄位 `{{nodeKey.欄位}}` |
| P1 | 開啟 TRIGGER 的 Designer | Input 為虛線空狀態「這是觸發節點…」；**不顯示** JSON |
| P1 | 開啟 SKILL 的 Designer | Input 空狀態為「這是能力提供節點…」；Output 空狀態為「不會產生自己的輸出」 |
| P1 | 開啟已掛能力節點的 LLM 的 Designer | Input 下半段出現 `node-designer-capability-list`，以 chip 列出掛在 `in:tool` 的節點名，**不顯示其 JSON** |
| P1 | 切到 Settings 分頁 | 顯示 nodeKey（可複製）、型別、分類、必填檢核與本次執行摘要；`node-designer-duplicate` / `node-designer-delete` 可用（刪除後 modal 自動關閉） |
| P2 | 切到 Docs 分頁（逐一檢視 13 種型別） | 皆有說明與行為要點；連接埠清單與該型別實際 handle 一致（如 CONDITION 出現 `out:true` / `out:false`） |

---

### J10 複合能力掛載：旅遊行程規劃（P1，**真實 MCP + 真實 Web 搜尋 + 真實 LLM**）

> 業務情境：台東開車二日遊行程規劃。單一 `LLM_ASSISTANT` 於 `in:tool` **同時掛載三種異質能力節點**——
> `MCP_SERVER`(google_map，Google Places API) 取景點座標、`TOOL`(TavilySearch) 查在地活動、`SKILL`(pdf) 提供輸出排版規範。
>
> **與既有旅程的分工**：J5 驗單一 MCP 掛載、J8-B 驗 KNOWLEDGE_RAG 外掛掛載，本旅程首次驗**三種能力併掛**，
> 且是 `SKILL` 節點型別的**首條 E2E 覆蓋**（`llm_skill` 在此之前為 0 筆）。

圖形：

```
TRIGGER(MANUAL) ──► LLM_ASSISTANT ──► OUTPUT
                          ▲ in:prompt
                    PROMPT(台東二日遊需求)
                          ▲ in:tool
        MCP_SERVER(google_map) · TOOL(TavilySearch) · SKILL(pdf)
```

| 案例 | 優先 | 案例 | 預期 |
|:---:|:---:|------|------|
| J10-01 | P1 | 拖出 6 節點並連線（3 條 `in:tool`、1 條 `in:prompt`、2 條 `out:main`） | 連線皆成立，畫布無驗證紅框 |
| J10-02 | P1 | 先斷開 SKILL 的 `in:tool` 邊再按啟用 | 擋下並回 `workflow.skill.node.not.mounted`，訊息含該 SKILL 的 nodeKey |
| J10-03 | P1 | 補回 SKILL 邊後啟用 | 啟用成功，狀態轉 ACTIVE |
| J10-04 | P1 | 執行 workflow，觀察 SSE 與 ExecutionResultDrawer | `execution.started` → 各 `node.started`/`node.completed` → `execution.completed`；全節點 SUCCESS |
| J10-05 | P1 | **deep-verify：景點真實性** | 輸出景點為真實臺東地點（地址含「臺東縣」），每點帶 `place_id` 與經緯度（源自 google_map，非模型臆造） |
| J10-06 | P1 | **deep-verify：活動查證** | 輸出含 TavilySearch 取得的活動／營業資訊與可追溯來源 URL |
| J10-07 | P2 | DB 落庫檢查 | `llm_workflow_execution` 一筆 SUCCESS；`llm_workflow_node_execution` **恰 4 筆**（TRIGGER/PROMPT/LLM/OUTPUT，三個純能力節點不落紀錄） |
| J10-08 | P1 | 由 OUTPUT 產出 PDF 行程表 | PDF 含 Day1/Day2 行程、每景點的 Google Maps 連結、活動資訊與來源 |

> **執行身分＝設定擁有者（沿用 §12 第 2 點）**：LLM 設定、google_map userSetting、Tavily toolSetting、pdf skill
> 必須同屬登入者，否則 `buildUserSpecificMcpClient` / `buildToolWithSetting` 以 `(settingId, userId)` 配對會查無資料。
>
> **MCP 節點務必帶 `userSettingId`**（§12 第 1 點）：google_map 需 `GOOGLE_MAPS_API_KEY`，
> 僅填 `mcpId` 會走 `buildDefaultMcpClient`、`${google_maps_api_key}` 佔位符不替換，工具形同不可用。
>
> **`searchPlaces` 有三個必填參數**：`query`、`language`、`maxResults`，缺任一個回
> `Missing required argument: <name>`。PROMPT 節點須明確要求模型呼叫時帶齊，否則第一輪工具呼叫即失敗。
>
> **SKILL 節點不執行程式碼**：`SkillService.buildSkills()` 只把 `skill.md` 與 resources **全文**注入 LLM
> （`resolveSkillResources` 逐檔 `readText`），`service/workflow/executor/` 下無 SkillNodeExecutor。
> 故 PDF 的實際渲染在平台外完成，本旅程對 SKILL 的斷言是「掛載契約成立且內容有進 LLM」，非「平台產出 PDF」。
>
> **斷言策略**：J10-04 只驗 SSE 事件序列與節點狀態（比照 J5，不比對輸出文字）；
> J10-05／06 為 deep-verify，依賴有效的 Google Places 與 Tavily 金鑰，金鑰缺席時標 ⏭️。

### J11 純資料管線（P0，**無 LLM，確定性斷言**）

> 對照 n8n 模板：房屋資訊爬蟲、Apify 撈 YouTube。缺口分析見
> [`docs/plans/2026-08-20-automation-capability-gap-analysis.md`](plans/2026-08-20-automation-capability-gap-analysis.md) §5。
>
> **本旅程的定位（與 J5 / J8 / J10 的關鍵差異）**：這是**唯一一條完全不含 LLM** 的執行旅程。
> J5 / J8 / J10 皆依賴真實模型，導致斷言只能驗 plumbing、逾時須放寬、金鑰缺席就 skip。
> 本旅程輸出**完全確定**，可逐欄比對 `finalOutput`，適合作為整個 E2E 體系的回歸基準
> ——執行引擎壞掉時，這條會比任何 LLM 旅程更早、更明確地紅燈。

圖形（4 節點 3 edge）：

```
TRIGGER(MANUAL) ──► HTTP_REQUEST(GET) ──► DATA_TRANSFORM ──► OUTPUT
```

**資料來源刻意選用本機服務自身的公開端點**（`GET /systemSetting/list`，`@PermitAll`），
不依賴外網：避免網路抖動造成偽紅燈，也讓 CI 階段（§8）不需額外對外連線權限。

| 案例 | 優先 | 案例 | 預期 |
|:---:|:---:|------|------|
| J11-01 | P0 | 拖出 4 節點並連線、以 Inspector 設定後存檔 | 存檔成功、version 1。⚠️ `HTTP_REQUEST` / `DATA_TRANSFORM` **無型別化表單**，Inspector 退回 `JsonConfigEditor`（見下方註），須以表格模式或 JSON 模式填 config |
| J11-02 | P0 | execute | SSE `execution.started` → 4 節點依序 `node.started`/`node.completed` → `execution.completed`（SUCCESS）；`llm_workflow_node_execution` 恰 4 筆皆 SUCCESS |
| J11-03 | P0 | **確定性輸出斷言（本旅程核心）**：比對 `finalOutput` | 與 `GET /systemSetting/list` 的實際回應**逐欄相符**；同一份資料重跑兩次輸出完全一致（**不使用模糊比對，不放寬逾時**） |
| J11-04 | P1 | `HTTP_REQUEST.response` 的型別 | 輸出為**回應 body 的原始字串**（`HttpRequestExecutor` 直接回 `response.body?.string()`），**不是**已解析的 JSON 物件；故 `DATA_TRANSFORM` 的 mappings **無法**以 `{{http.response.data}}` 下鑽欄位，只能整串引用。欄位抽取需另接 `CODE` 節點 `JSON.parse`（見 §J12-07 與 J14） |
| J11-05 | P1 | `secretHeaders` 加密落地與遮罩 | Inspector 填入 secretHeader 後 save → `workflow/get` 回 `__SECRET_KEPT__`；DB `llm_workflow_node.config` 內為密文（`WorkflowSecretConverter`），**明文不出現在回應、日誌或錯誤訊息** |
| J11-06 | P1 | `__SECRET_KEPT__` 沿用 | 不改該欄位再存一次 → DB 密文不變（非重新加密、非寫入字面值 `__SECRET_KEPT__`）；再次 execute 仍成功（代表沿用的是真實明文） |
| J11-07 | P1 | 非 2xx 回應（url 指向必然 404 的路徑） | 節點 `node.failed`、整體 FAILED，下游 `DATA_TRANSFORM` / `OUTPUT` 標 SKIPPED。⚠️ 錯誤訊息為 `HttpRequestExecutor` 寫死的英文 `Unexpected code 404`（`IOException`），**非 i18n 業務訊息**——見下方「已知偏離」 |
| J11-08 | P2 | `timeoutMs` 設極小值（如 1ms） | 節點 FAILED，錯誤指向 call timeout；服務本身不受影響，後續案例可繼續執行 |

> ⚠️ **這五種節點在 Inspector 沒有型別化表單**：`inspector/typedForms.ts` 的 `TYPED_FORMS`
> 只涵蓋 TRIGGER / LLM_ASSISTANT / PROMPT / TOOL / MCP_SERVER / SKILL / KNOWLEDGE_RAG / OUTPUT 八種；
> **`HTTP_REQUEST` / `DATA_TRANSFORM` / `CONDITION` / `LOOP` / `CODE` 一律退回 `JsonConfigEditor`**。
> J11–J14 的所有節點設定都得走 JSON 編輯器（機制本身由 §J6 覆蓋），撰寫腳本時**別找不存在的
> `data-test="http-url"` 這類選擇器**。此現況同時是產品缺口，已登錄於缺口分析 §3。

> **已知偏離（實作 vs `docs/workflow-engine/requirements.md`）**：AC-D7 規定 HTTP 失敗回
> `WORKFLOW_HTTP_REQUEST_FAILED`，但 `AppMessage` 中**無此鍵**，實際拋的是帶英文字串的
> `IOException`。同理 `HttpRequestExecutor` **只支援 GET / POST**，其他 method 拋
> `IllegalArgumentException("Unsupported HTTP method: …")`（同為寫死英文）。
> 兩者皆違反 [`i18n-messages.md`](../.claude/rules/i18n-messages.md) 的「業務訊息禁止寫死字串」，
> 本旅程**如實斷言現況**並在報告標註；修正與否另案決定，勿在測試中預設它已是 i18n 訊息。

---

### J12 迴圈批次處理（P1，`LOOP` 首條端到端覆蓋）

> 對照 n8n 模板：Gmail 電子發票（22 節點，Loop+Aggregate 為核心）、While True 迴圈範例。
>
> **為何值得單獨一條旅程**：`LoopExecutor` 是全專案唯一「薄殼 ＋ 引擎特判」的 executor
> ——迭代編排寫在 `WorkflowEngine.executeLoopNode()`，**主遍歷不經 `execute` 分派 LOOP**，
> executor 只提供 `resolveItems()` 與常數。這條分歧路徑目前**零端到端證據**。

圖形（子圖入口由 `loopBodyEntryNodeKey` 指定，非由連線推導）：

```
TRIGGER(MANUAL) ──► CODE(產生陣列) ──► LOOP ─out:loop→ [DATA_TRANSFORM(逐筆處理 {{item.*}})]
                                            └─out:done→ OUTPUT
```

**⚠️ 陣列為何由 `CODE` 供應，而非 TRIGGER 的 `inputPayload`**（202608202139 實測修訂）：
原設計寫「輸入陣列由 TRIGGER 的 `inputPayload` 提供」，但**前端從不送 `inputPayload`**
——`api/workflowExecution.ts:43` 的註解自承「`inputPayload` 之後也應該放這裡」，
故從編輯器按執行鈕時 `TriggerExecutor` 收到的 input 恆為 null，TRIGGER 輸出永遠是 `{}`，
`{{<triggerKey>.rows}}` 無從解析。**該圖形經 UI 不可達**，只能走 API 直呼。
改由 `CODE` 節點產生陣列後全程可由 UI 完成，且更貼近 n8n 模板的實際寫法（Code node 備料）。

> 這同時是一個**產品缺口**：`constants/nodeDocs.ts` 對 TRIGGER 的說明教使用者用
> `{{觸發節點key.欄位}}` 取值，但 UI 沒有任何地方能填入那些欄位。
> 已登錄於 202608202139 確認表的問題追蹤區 #3。

**若改由 `HTTP_REQUEST` 供應陣列會失敗**：其輸出是**字串**而非 List（見 J11-04），
`resolveItems` 會拋 `workflow.loop.input.not.array`——此組合即為 J12-07 的測法
（以 `CODE` 回傳字串模擬，效果等價且不需額外節點）。

| 案例 | 優先 | 案例 | 預期 |
|:---:|:---:|------|------|
| J12-01 | P1 | 建圖並存檔（5 節點 4 edge）：`CODE` 產生陣列、LOOP 的 `inputArrayPath` 指向 `{{<codeKey>.<outputKey>}}`、`loopBodyEntryNodeKey` 填子圖入口 nodeKey | 存檔成功、version 1。⚠️ `loopBodyEntryNodeKey` 是**必填**（`LoopNodeConfig.missingRequiredFields`）且**須手填 nodeKey 字串**（無 UI 選擇器），nodeKey 由 `.vue-flow__node[data-id]` 讀取 |
| J12-02 | P1 | execute（N=3 筆） | 整體 SUCCESS；子圖節點**每迭代各發一次** `node.started`/`node.completed` SSE 事件（`executeLoopBodyNode` 事件照發）→ 子圖節點的 SSE 事件數 = **2N**（N=3 時為 6） |
| J12-03 | P1 | **`node_execution` 落庫筆數（本旅程核心斷言）** | 子圖節點為 **N 筆**（每迭代一筆、帶 `loop_index` 0..N-1、`seq_no` 遞增），**非 1 筆**；LOOP 節點本身另有 1 筆。**N=3 時全圖 7 筆**：TRIGGER 1 ＋ **CODE 1** ＋ LOOP 1 ＋ DATA_TRANSFORM 3 ＋ OUTPUT 1（202608202139 實測值；先前寫 6 筆係漏計備料用的 CODE 節點） |
| J12-04 | P1 | 彙集鍵與元素型別 | LOOP **自身**輸出（`node_execution.output`）為 `{ <collectOutputKey 或 "items">: [...] }`；陣列元素是**每迭代子圖拓撲序最後一個節點的完整輸出 map**（非該節點某個欄位）。<br>⚠️ **斷言必須看 LOOP 自身的 `node_execution.output`，不可透過 OUTPUT 節點看**——`OutputNodeConfig.mappings` 型別為 `Map<String,String>`，會把陣列序列化成字串（202608202139 實測，首次斷言即因此誤判） |
| J12-05 | P1 | `out:loop` 與 `out:done` 的活化語義 | `out:loop` **僅界定子圖入口、不參與活化**（子圖節點不在主遍歷執行）；迭代完成後**只活化** `out:done`。斷言：OUTPUT 恰 1 筆、子圖節點恰 N 筆（＝迭代數，未被主遍歷重複執行一次） |
| J12-06 | P2 | 超過迭代上限（`maxIterations` 設 2，輸入 5 筆） | ⚠️ **截斷而非報錯**：引擎 `logger.warn("…僅執行前 N 項")` 後只跑前 2 筆，LOOP 的 `items` 長度為 2，整體仍 **SUCCESS**。這與 `requirements.md` AC-D6 規定的 `WORKFLOW_LOOP_LIMIT_EXCEEDED` **不符**（`AppMessage` 無此鍵）——如實斷言現況並在報告標註（202608202139 已實測確認） |
| J12-07 | P2 | `inputArrayPath` 指向非陣列（以 `CODE` 回傳字串製造） | LOOP 節點 FAILED、下游 SKIPPED，訊息 `Loop input path <原始字串> did not resolve to an array`（`workflow.loop.input.not.array`，含原始 path 字串）；`{{path}}` 包裹與裸 path **兩種寫法都要驗**，且兩者訊息只差在是否帶 `{{}}`（202608202139 實測皆正確解析） |

> **巢狀 LOOP 不支援**：任一 LOOP 子圖內含另一 LOOP → `workflow.loop.nested.not.supported`，
> 於 `validateNodes`（啟用前／執行前）即擋下、不進入迭代。本旅程不含此案例
> （需要 6 節點以上的畫布成本不成比例），但改動迴圈編排時須記得它存在。
>
> **迭代中失敗的行為**：子圖任一節點失敗 → 例外上拋 → **LOOP 節點整顆標 FAILED**、整體 FAILED，
> 已完成的迭代紀錄保留（NFR-4 的逐節點 flush）。無「跳過該筆繼續下一筆」的語義
> ——這正是缺口分析 GAP-6（節點層錯誤處理）在迴圈情境下的具體表現。

---

### J13 條件分流資料管線（P1）

> 對照 n8n 模板：LINE 關鍵字自動回覆。
>
> **與 §J5「分支擇一」案例的分工**：J5 測 CONDITION 分流**提示詞**（`in:prompt` 埠，下游是 PROMPT＋LLM）；
> 本旅程測分流**資料流**（`in:main` 埠，全程無 LLM），且刻意把分支**加長一節**以驗 SKIPPED 的傳播深度。

圖形（分支各兩節，用以區分「只標直接下游」與「標整條下游」）：

```
TRIGGER(payload 帶 category)
   └─► CONDITION ─out:true → DATA_TRANSFORM(A1) → DATA_TRANSFORM(A2) ─┐
                 └out:false→ DATA_TRANSFORM(B1) → DATA_TRANSFORM(B2) ─┴─► OUTPUT
```

| 案例 | 優先 | 案例 | 預期 |
|:---:|:---:|------|------|
| J13-01 | P1 | 建圖並存檔：CONDITION 填 `conditions`（`left`/`operator`/`right`）與 `logic` | 存檔成功；`conditions` 為**唯一無條件必填**欄位（`ConditionNodeConfig`） |
| J13-02 | P1 | 以命中 true 的 payload execute | `out:true` 側 A1、A2 皆 SUCCESS；`finalOutput` 對應 A 分支（確定性比對） |
| J13-03 | P1 | 改 payload 使判定為 false 後再 execute | `out:false` 側 B1、B2 皆 SUCCESS；`finalOutput` 對應 B 分支 |
| J13-04 | P1 | **SKIPPED 的傳播深度（本旅程核心斷言）** | 未活化分支的**整條下游皆為 SKIPPED**——B1 **與 B2 都要是** `SKIPPED`，不可只有直接相連的 B1 被標而 B2 缺紀錄或殘留其他狀態 |
| J13-05 | P2 | `operator` 填不支援的運算子 | 節點 FAILED，`workflow.condition.operator.not.supported`（訊息含該 operator 字樣）。⚠️ 非 `requirements.md` AC-D5 所寫的 `WORKFLOW_CONDITION_EVAL_FAILED`（無此鍵） |

> **多條件聚合**：`logic` 為 `or` 時任一成立即 true，其餘值（含未填）一律走 `all`（且）。
> 自訂 `trueHandle` / `falseHandle` 可覆寫預設的 `out:true` / `out:false`，本旅程使用預設值。
>
> ⚠️ **`CONDITION` 只有二分支**，無 N 路 Switch。LINE 模板的「三選一」在本平台只能串接多個
> CONDITION——已登錄為缺口分析 GAP-4，本旅程不測該替代寫法。

---

### J14 CODE 節點沙箱（P1 功能 / P2 邊界，含**安全性驗證**）

> GraalJS 沙箱是 [`tech-stack-and-versions.md`](../.claude/rules/tech-stack-and-versions.md) 著墨最深的一塊
> （`allowAllAccess(false)`、禁 host class / IO、逾時強制中斷、輸出上限、truffle-api 剝除 workaround），
> **卻沒有任何端到端覆蓋**。本旅程的後三條**同時是安全性驗證**，不只是功能驗證。

圖形（3 節點）：

```
TRIGGER(MANUAL, inputPayload) ──► CODE ──► OUTPUT
```

`CodeExecutor` 的執行契約（撰寫案例前必讀）：

| 項目 | 值 |
|------|-----|
| 全域變數 | `input` = 上游**所有**輸出（`context.allOutputs()` 經 JSON 序列化後於 JS 內 `JSON.parse` 還原，避免 host object 穿透） |
| 回傳值 | **腳本最後一個表達式**（非 `return`） |
| 預設輸出鍵 | `result`（`outputKey` 可覆寫） |
| 預設逾時 | 10,000 ms（`timeoutMs` 可覆寫），逾時以 `context.close(true)` 強制中斷 |
| 輸出上限 | 序列化後 256 KB |

| 案例 | 優先 | 案例 | 預期 |
|:---:|:---:|------|------|
| J14-01 | P1 | 正常 JS 轉換（讀 `input`、最後表達式為物件） | 節點 SUCCESS，輸出落於 `result`；`finalOutput` 確定性比對 |
| J14-02 | P1 | 自訂 `outputKey` | 下游以 `{{<codeKey>.<outputKey>}}` 取值成功；預設鍵 `result` 不再出現 |
| J14-03 | P2 | `language` 填 `python` | 節點 FAILED，`workflow.code.language.not.supported`（訊息含該值）；**不執行任何腳本** |
| J14-04 | P2 | **逾時**：無窮迴圈 ＋ `timeoutMs=2000` | 節點 FAILED，`workflow.code.timeout`（訊息含逾時毫秒數）；**且後續案例仍可正常執行**——驗證強制中斷確實回收了執行緒與 context，未拖垮服務 |
| J14-05 | P2 | **沙箱越界**：腳本嘗試存取 Java host class 或檔案系統（如 `Java.type('java.io.File')`） | 被沙箱擋下，節點 FAILED，`workflow.code.script.error`；**檔案系統無任何副作用** |
| J14-06 | P2 | **輸出上限**：產生 >256 KB 的字串 | 節點 FAILED，`workflow.code.output.too.large`（訊息含 256）；非靜默截斷 |

> ⚠️ **J14-04 與 J14-05 具破壞性風險**，務必安排在該旅程**最後**執行，且執行後以
> `/view/chat` 健康檢查確認服務仍存活再收尾；若服務已死，該事實本身即為 ❌ 並須立即報告
> （屬產品缺陷，走 §Step 5.7 失敗分流，不得自行修復後重跑掩蓋）。

---

### J15 知識庫（Milvus）＋ MCP 併掛同一 LLM（P1，**真實 embedding + Milvus + 真實 MCP + 真實 LLM**）

> 業務情境：**渣打數位存款帳戶開戶資格查核 ＋ 就近分行查詢**。使用者自述 19 歲、想線上開立
> 渣打數位存款帳戶；若不符資格，請系統找出臺北市信義區可臨櫃辦理的渣打分行。
>
> **與既有旅程的分工**：J5 驗**單一 MCP** 掛載的 plumbing、J8-B 驗 **RAG 外掛**的注入正確性、
> J10 驗 MCP+TOOL+SKILL **三種工具型能力**併掛。本旅程首次驗
> **「注入型能力（KNOWLEDGE_RAG，推論前由框架自動注入）＋ 呼叫型能力（MCP_SERVER，推論中由模型主動呼叫）
> 併掛同一顆 LLM 的 `in:tool`」**——這兩者在 `LlmAssistantExecutor` 走的是不同機制，
> 且**必填契約相反**（見 J15-04）。此路徑在 J15 之前從未被端到端驗證過。
>
> 亦是本計畫中**唯一同時斷言兩個獨立資料來源都出現在同一份輸出**的旅程。

圖形（6 節點 / 5 edge）：

```
TRIGGER(MANUAL) ──► LLM_ASSISTANT ──► OUTPUT(JSON)
                          ▲ in:prompt
                       PROMPT（19 歲開戶提問 ＋ 分行查詢要求）
                          ▲ in:tool
        KNOWLEDGE_RAG(渣打 TNC 知識庫)   ·   MCP_SERVER(google_map)
```

edge 組成：2 條 `out:main → in:tool`、1 條 `out:main → in:prompt`、2 條一般 `out:main`。

#### 情境為何這樣設計（兩個來源必須各自可辨識）

| 來源 | 只有它才知道／拿得到的東西 | 鑑別依據 |
|------|--------------------------|---------|
| KNOWLEDGE_RAG（Milvus） | `docs/rag/doc/scb/tw-online-tnc.pdf` 原文「立約人應為……**年滿二十歲**自然人」 | **文件事實（20 歲）≠ LLM 常識（民法成年 18 歲）**。此鑑別點已於週期 202607252225／202607272132 兩次坐實：向量庫故障時模型答 18 歲，修復後答 20 歲（§14-3） |
| MCP_SERVER（google_map） | 分行的 `place_id` 與經緯度 | **模型無法臆造合法 `place_id`**。不可改用「模型是否自述呼叫了工具」當判準（§15-4 教訓） |

#### ⚠️ 工具往返輪數預算（設計上的硬約束，改題目前必讀）

`WorkflowEngine.DEFAULT_NODE_TIMEOUT_MS = 120_000`，且 `LLM_ASSISTANT` **無法覆寫 `timeoutMs`**
（只有 `HttpRequestNodeConfig` / `CodeNodeConfig` 可以，§15-5 ISSUE-3）。實測：**約 3 輪安全、5 輪即逾時**。

本旅程預算：KNOWLEDGE_RAG 外掛 = **0 輪**（推論前自動注入，不是工具呼叫）＋
`searchPlaces` = **1 輪**，合計 **1 輪**，餘裕充足。

> **PROMPT 節點必須明文限制「只呼叫一次 `searchPlaces`、`maxResults` 最多 3」**，
> 否則模型可能逐間分行分開查詢而炸開輪數。

| 案例 | 優先 | 案例 | 預期 |
|:---:|:---:|------|------|
| J15-01 | P1 | **前置（API）**：建 EMBEDDING 設定（OpenRouter `nvidia/nemotron-3-embed-1b:free`）＋ Milvus 向量庫（dimension **2048**）＋ 上傳 `tw-online-tnc.pdf` 建**新** knowledgeId | `getDataFromEmbeddingStore` 以「開立數位存款帳戶的年齡條件」檢索，Top-K **命中「年滿二十歲」原句**。⚠️ 回空陣列即前置不成立，**不得只看 `getKnowledgeStore` 列得出知識庫**（metadata 在 Postgres、向量在 Milvus，會不同步） |
| J15-02 | P1 | **前置（API）**：確認 google_map MCP 可用 | `java -jar D:/MCP/google-map-1.0-SNAPSHOT.jar` 不報 manifest 錯；`/llm/mcpServer/getSetting` 回 200 且 env 值遮罩為 `__SECRET_KEPT__`；**mcpId／userSettingId／embedding 設定／CHAT 設定四者同屬登入者**（§12-2） |
| J15-03 | P1 | UI 建圖：6 節點 5 edge，**KNOWLEDGE_RAG 與 MCP_SERVER 併掛同一 LLM 的 `in:tool`** | 兩條 `in:tool` 皆放行、無相容性 toast、console error = 0；Overview 顯示 **6 NODES / 5 CONNECTIONS**；存檔成功、version 1 |
| J15-04 | P1 | **異質能力併掛的必填契約（核心 A）**：RAG 只填 `knowledge-id`（`query` / `embedding-model-select` 留空）、MCP 填 `mcp-select` ＋ `tool-name`(`searchPlaces`) ＋ `mcp-setting-id` 後啟用 | 啟用成功。⚠️ **兩者規則相反正是本案例的實質內容**：`McpServerNodeConfig` 必填 **`mcpId` ＋ `toolName`**，即使純能力掛載也不例外（§11-1，缺則 execute 直接 400）；`KnowledgeRagNodeConfig` 外掛模式則**只必填 `knowledgeId`**（§14-1） |
| J15-05 | P1 | execute（真實 embedding + 真實 MCP + 真實 LLM） | SSE `execution.started` → **僅 TRIGGER / PROMPT / LLM_ASSISTANT / OUTPUT 四節點** `node.*` → `execution.completed`(SUCCESS)；**記錄實際耗時**（預期 < 120s，超過即代表輪數預算被打破） |
| J15-06 | P1 | **兩種能力節點皆不落主遍歷** | RAG 與 MCP 節點的 Inspector **皆無**「本次執行」區塊（`data-test="node-exec-section"`）；`llm_workflow_node_execution` **恰 4 筆**，無 `KNOWLEDGE_RAG` / `MCP_SERVER` 型別列 |
| J15-07 | P1 | **deep-verify：雙來源同時生效（核心 B）** | `finalOutput` 必須**同時**滿足：①依文件判定 19 歲**不符**資格並引用「20／二十歲」（**不是**常識的 18 歲）②列出真實渣打分行，地址含「臺北市信義區」、每筆帶 `place_id` 與經緯度，且 **`place_id` 須與後端日誌中 `searchPlaces` 的實際回傳值逐字元相符**（見 J15-08：只驗「有 place_id」不足以排除幻覺）。⚠️ **回覆截斷的觀察點（2026-09-12 起）**：後端每次推論會以 INFO 記錄 `finishReason` 與 `tokenUsage`；若回覆被截斷且 `finishReason` 為 `LENGTH` / `CONTENT_FILTER`，該節點應為 **FAILED**（訊息 `workflow.llm.response.incomplete`）而非 SUCCESS。若截斷時 `finishReason` 回 `STOP` 或 `null`，則平台偵測不到——**如實記錄日誌中的實際值**，不得宣稱已修好（見 §18 問題追蹤 #1） |
| J15-08 | P2 | **來源鑑別對照組**：刪除 MCP 節點（連同其 `in:tool` 邊，避免留下孤兒）後以同一提問重跑 | 輸出**仍含**文件事實（20 歲），但分行資料**不可信**。⚠️ **實測推翻原預期**：模型並非「不再輸出 `place_id`」，而是**照樣輸出、只是全屬捏造**（尾碼流水 `…Q6/Q7/Q8`、三筆座標完全相同）。故判準必須是「`place_id` **與 MCP 工具實際回傳值逐字元相符**」，只檢查「有無 place_id」會被幻覺矇混 |
| J15-09 | P2 | **`userSettingId` 迴歸**（§12-1 已知陷阱）：MCP 節點清空 `mcp-setting-id`、只留 `mcpId` ＋ `toolName` 後重跑 | 走 `buildDefaultMcpClient`，`${google_maps_api_key}` 佔位符不替換 → Places API 認證失敗。**2026-09-12 起**後端會對此情境記一則 WARN（MCP 宣告了需填值設定卻無 userSettingId），重跑時應在後端日誌確認該 WARN 出現（見 §12-5）。⚠️ **確切失敗樣態未曾記錄**，本案例預期為「**如實記錄實際樣態**」並回寫 §12 |

> **斷言策略**：J15-05／06 比照 J5 只驗 plumbing（事件序列、節點狀態、DB 落庫），**不比對文字**；
> J15-07／08 為 deep-verify，依賴有效的 OpenRouter 金鑰與 Google Places 金鑰，任一缺席即標 ⏭️ 並註明。
> J15-09 是「記錄行為」而非「驗證既有規格」，結果無論如何都不計為 ❌，但**必須把實測樣態回寫 §12**。

> ⚠️ **J15-09 有兩條不同的失敗路徑，務必分辨清楚，否則會誤判**：
> ①**清空 `userSettingId`** → 走 `buildDefaultMcpClient`，env 佔位符原樣傳入，MCP 子行程起得來但 Places API 認證失敗；
> ②**填一個不存在的 `userSettingId`** → 走 `buildUserSpecificMcpClient`，查無設定時**回 null 被 `mapNotNull` 靜默丟棄、只留 WARN log**，模型連工具都看不到。
> 兩者的外顯症狀都像「模型不肯呼叫工具」——**失敗時務必先看後端 WARN 日誌**（`D:/tmp/bestpartner/bestpartner.log`），這正是 §15 ISSUE-1／ISSUE-2 當初誤判的來源。

> **節點表單選擇器**（撰寫腳本時直接引用；前端**無** LLM／向量庫／MCP 管理頁，三者一律 API 前置）：
>
> | 節點 | 表單檔 | `data-test` 欄位 | 後端必填 |
> |------|--------|-----------------|---------|
> | `LLM_ASSISTANT` | `inspector/forms/LlmAssistantForm.vue` | `llm-select`、`system-prompt`、`user-prompt`、`response-format`、`output-schema`、`output-key`、`enable-memory` | 僅 `llmId` |
> | `KNOWLEDGE_RAG` | `inspector/forms/KnowledgeRagForm.vue` | `knowledge-id`、`embedding-model-select`、`query`、`topk`（預設 4）、`min-score`、`output-key` | 僅 `knowledgeId` |
> | `MCP_SERVER` | `inspector/forms/McpServerForm.vue` | `mcp-select`、`tool-name`（**自由文字非下拉**）、`mcp-setting-id`（**手打 UUID**）、`arguments`、`output-key` | `mcpId` ＋ `toolName` |
> | `PROMPT` | `inspector/forms/PromptForm.vue` | `prompt-text`、`output-key` | `prompt` |
>
> `tool-name` 是自由文字，因為 MCP 工具清單只在執行期才發現、後端無查詢端點。J15 一律填 `searchPlaces`。

> **環境前置的易錯處**：
> - Milvus：`cd docs/docker/milvus && docker-compose up -d`，gRPC **19530**、healthz 9091、Attu **8000**，
>   healthcheck `start_period: 90s` → 前置等待抓 **≥90s**。⚠️ **Attu 與 Chroma 都佔 8000，不可同時起**。
> - `POST /llm/vector/uploadFiles` 必填是 **`files`（複數）＋ `embeddingModelId` ＋ `embeddingStoreId`** 三個
>   （`docs-site/docs/features/rag.md` 寫成單數 `file`、只列一個參數，照抄會 400）。
> - `/llm/vector` **沒有列出向量庫設定的端點**，`embeddingStoreId` 只能查 DB 的 `vector_store_setting`。
> - **維度不會自動校驗**：`MilvusBuilder` 直接把 `dimension` 餵給 `MilvusEmbeddingStore`，不一致要到
>   `addAll`／`search` 才炸。建 collection 前**先打一次 `/embeddings` 量測實際維度**最保險。
> - **InMemory 不是可選項**：`VectorStore` enum 只有 `CHROMA` / `MILVUS`，必須真的起 Milvus。
> - **重建知識庫必須換新 knowledgeId**：`deleteData` 不刪 `llm_knowledge` 列，同 id 重上傳會撞唯一鍵（§14-5）。
> - **勿硬編 id**：運行 dev DB 的 `llmId` / `mcpId` 與種子檔會漂移，沿用 `global-setup.ts` 的動態解析。

---
## 4. 測試資料策略

- **命名**：測試建立的 workflow 一律以 `e2e-<caseId>-<runTag>` 前綴命名，便於識別與掃描殘留。
- **清理前必問（強制）**：收尾清理之前，必須以 `AskUserQuestion` 詢問使用者本次資料要清理還是**保留**
  （保留供其後續手動測試）。選保留者不清理，且**必須改名成不含 `e2e-` 前綴**（建議 `keep-<runTag>-<原名>`），
  否則日後任一輪的前綴兜底掃描會把它清掉。保留與否都不影響「收尾一律關閉服務」。
  完整規則 → [`.claude/rules/test-data-retention.md`](../.claude/rules/test-data-retention.md)。
- **清理**（使用者選擇清理時）：
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
- **Node Designer 是全屏遮罩**：開啟後畫布完全不可點。任何「開 Designer → 回畫布繼續操作」的流程，**必須先確認 `node-designer-modal` 已消失**再進行下一步，否則點擊會落在遮罩上。
- **主題狀態會跨 spec 殘留**：偏好存於 `localStorage.wf-theme`，且深色會在 `<html>` 掛 `dark` class。斷言外觀相關項目前先明確設定或清除該鍵，不要依賴上一支 spec 的殘留狀態。
- **本次改版新增的 `data-test`**：`theme-toggle`、`zoom-bar` / `zoom-in` / `zoom-out` / `zoom-fit` / `zoom-value`、`node-open-designer`、`node-done-badge`、`open-node-designer-button`、`node-designer-modal` / `-close` / `-name-input` / `-tab-parameters|settings|docs` / `-parameters` / `-settings` / `-docs` / `-input-panel` / `-input-empty` / `-input-json` / `-capability-list` / `-output-panel` / `-output-empty` / `-output-json` / `-output-status` / `-output-error` / `-output-preview` / `-copy-key` / `-duplicate` / `-delete`。
- **返回列表導覽新增的 `data-test`**：`back-button`（工具列返回鈕）、`breadcrumb-list`（可點的「Personal」麵包屑）。兩者皆 `router.push('/')`，未存確認交由 `onBeforeRouteLeave`。
- **多觸發點執行新增的 `data-test`**：`node-run-from-here`（TRIGGER 節點卡的「從此處執行」鈕）、`trigger-picker`（多觸發點選擇面板）、`trigger-option-<nodeKey>`（各觸發點選項）、`trigger-picker-cancel`。
- **登入**：email 欄位為 `type=email`（原生驗證擋非 email），須用真實 email（admin 為 `admin@bestpartner.com.tw`）而非裸 `admin`。
- **ID 動態解析（重要）**：運行 dev DB 的 `llmId`/`toolId` 與 `docs/sql` 種子檔會漂移（實測 OpenRouter CHAT 於本機為 `1ee80ffa…`、種子檔為 `583b9222…`）。**禁止硬編 ID**；於 `global-setup` 以 API 依 alias/platform/name 解析當前 DB 真實 ID，寫入 `.artifacts/seed.json` 供 spec 讀取（`E2E_LLM_ID` 可覆寫，缺則 J5 skip）。
- **五種節點沒有型別化表單（J11–J14 必讀）**：`inspector/typedForms.ts` 的 `TYPED_FORMS` 只涵蓋八種型別，**`HTTP_REQUEST` / `DATA_TRANSFORM` / `CONDITION` / `LOOP` / `CODE` 一律退回 `JsonConfigEditor`**。設定這些節點只能走 JSON 編輯器的表格模式或 JSON 模式，**不存在 `data-test="http-url"` 這類欄位選擇器**，撰寫腳本前先確認，別對著不存在的選擇器除錯。
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
| SKILL upload / list / get、TOOL saveSetting、MCP SERVER saveSetting / getSetting | J10（間接，經 Step 4.5 資源前置與能力節點掛載） |
| WORKFLOW execute（`HTTP_REQUEST` / `DATA_TRANSFORM` 節點）、`secretHeaders` 加密與遮罩契約 | J11 |
| WORKFLOW execute（`LOOP` 迭代編排與 `node_execution` 落庫筆數） | J12 |
| WORKFLOW execute（`CONDITION` 資料流分支與 SKIPPED 傳播） | J13 |
| WORKFLOW execute（`CODE` 沙箱：逾時 / 越界 / 輸出上限） | J14 |

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
- **編輯器外觀／互動殼層異動時**（主題切換、縮放列、Node Designer 的開關入口或三欄內容），§J9 與 `e2e-test-checklist.md` 的對應項目須一併更新；新增可互動元素一律補 `data-test` 並登錄於 §6 的清單。
- **能力節點（`TOOL` / `MCP_SERVER` / `SKILL` / `KNOWLEDGE_RAG`）的設定解析或加解密方式異動時**，§J10 與 **§J15** 的資源前置與 deep-verify 案例須一併檢視（工具型三種併掛的路徑只在 §J10 覆蓋，注入型＋呼叫型併掛只在 §J15 覆蓋），並同步 `e2e-test-checklist.md`。
- **能力掛載的建構機制異動時**（`LlmAssistantExecutor` 的 RAG 注入時機、`McpServerService.buildUserSpecificMcpClient` / `buildDefaultMcpClient` 的分支條件與 `(userSettingId, userId)` 配對、`ToolService.buildToolWithSetting` 的查詢鍵），§J5（Agent 模式）、§J8-B（自動注入）、§J10、**§J15**（含 J15-09 的 `userSettingId` 迴歸）須一併檢視。⚠️ 這條路徑的失敗多為**靜默**（查無設定時 `mapNotNull` 丟棄、只留 WARN log），案例務必保留「先看後端日誌」的指引。
- **旅程矩陣（§3）增刪旅程或案例數異動時**，除 `e2e-test-checklist.md` 摘要表外，還須同步
  `e2e-test-confirmation` skill 的 **Step 1.5 範圍選單**（旅程清單與案例數），否則使用者選到的範圍與實際案例數會漂移。
- **失敗分流判準異動時**（API 直呼／後端日誌／瀏覽器 console／SSE 事件／DB 落庫這五類訊號），
  須同步 skill 的 Step 5.7 判準表與 `e2e-test-checklist.md` 的「問題分流與修正紀錄」欄位。
- **停服務範圍異動時**（port 清單或行程樣式），須同步 `.claude/hooks/e2e-flow-guard.ps1` 的 `$script:Ports`、
  skill 的 Step 4 / 6.5 指令與模板的前置檢查清單——三者不一致會導致 hook 擋下正常流程或漏擋。
- **資料類節點（`HTTP_REQUEST` / `DATA_TRANSFORM` / `CONDITION` / `LOOP` / `CODE`）的 executor 行為異動時**，§J11–§J14 的對應案例須一併更新，並同步 `e2e-test-checklist.md`。特別是這幾項**已被案例釘住的實作細節**：`HttpRequestExecutor` 的預設輸出鍵 `response` 與「回傳原始字串而非解析後 JSON」、`LoopExecutor` 的 `DEFAULT_ITEM_ALIAS` / `DEFAULT_COLLECT_KEY` / `DEFAULT_MAX_ITERATIONS` 與**超限截斷**語義、`ConditionExecutor` 的 `DEFAULT_TRUE_HANDLE` / `DEFAULT_FALSE_HANDLE`、`CodeExecutor` 的 `input` 全域 / `DEFAULT_OUTPUT_KEY` / `DEFAULT_TIMEOUT_MS` / `MAX_OUTPUT_KB`。
- **`typedForms.ts` 的 `TYPED_FORMS` 新增型別時**，§6 的「五種節點沒有型別化表單」清單與 §J11 的同名警語須縮減；該型別在 J11–J14 的操作方式會從 JSON 編輯器改為專屬表單，對應案例的選擇器須一併改寫。
- E2E 旅程若涉及 API 契約變更，須同步 `docs/api-test-plan.md` 與 `.claude/rules/api-endpoints.md`。
- 測試資料清理策略異動時，須確認仍不違反「備份只能寫 `bestpartner-init-data.sql`」鐵則，
  並同步 [`.claude/rules/test-data-retention.md`](../.claude/rules/test-data-retention.md)（收尾必問資料處置）、
  `e2e-test-confirmation` skill 的 Step 6／6.1／6.2 與 `e2e-test-checklist.md` 的「測試資料處置規則」。

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
5. **缺 `userSettingId` 的確切失敗樣態（J15-09 於週期 202608252128 補記，2026-08-25）**：走 `buildDefaultMcpClient` 後佔位符不替換，Places API 回 `INVALID_ARGUMENT: API key not valid`，但該錯誤被包在 MCP 回應的 `content[].text` 內、協定層 `isError=false`，故**節點與整體執行皆為 SUCCESS**、`error_message=null`、後端無任何 WARN／ERROR——使用者只看到「執行成功但答案沒有分行資料」。**2026-09-12 起**：`McpServerService.buildDefaultMcpClient` 對宣告了 env／argsDesc 需求卻無 userSetting 的 STDIO MCP 補記一則 **WARN**（工具仍會掛上、行為不變，但日誌留下線索）。重跑此案例時應在後端日誌確認該 WARN 出現。
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

---

## 15. J10 首條切片實測發現（2026-07-31，複合能力掛載 · 週期 202607312223）

首條 J10 切片以 webwright 驅動：`TRIGGER → LLM_ASSISTANT(OpenRouter deepseek-v3.2, JSON) → OUTPUT`，
`PROMPT → in:prompt`，`MCP_SERVER(google_map)` / `TOOL(TavilySearch)` / `SKILL(pdf)` 三者併掛 `in:tool`。
8 案例 7 ✅ 1 ❌（87.5%，P1 標準 ≥95% 未達標）。報告見
`docs/test-confirmations/e2e-test-confirmation-202607312223.md`。

1. **三種能力節點併掛可正常運作**：google_map MCP 的 7 個工具確實被模型呼叫，回傳真實臺東景點
   （三仙台 / 鐵花村 / 伯朗大道 / 知本溫泉，皆含 `place_id`、經緯度、臺東縣門牌、評分）。
   `llm_workflow_node_execution` 恰 4 筆（TRIGGER/PROMPT/LLM/OUTPUT），三個純能力節點不落紀錄，
   與 §12 對單一 MCP 的觀察一致。**SKILL 節點型別自此有 E2E 覆蓋**。

2. **孤兒 SKILL 檢查有效**：未接 `in:tool` 即啟用會被擋，訊息
   `SKILL node "<nodeKey>" must be connected to an LLM assistant nodes tool input port (in:tool)`。
   此路徑（`WorkflowEngine.findUnmountedSkillNodeKey`）先前無 E2E 覆蓋。

3. **ISSUE-1（缺陷，關鍵；已於 2026-07-31 修復）：TOOL 節點填 `toolSettingId` 必定執行失敗**。
   `ToolService.buildToolWithSetting(toolSettingId)` 把 **settingId** 傳進
   `LLMToolUserSettingRepository.findSettingByUserIdAndToolId(userId, toolId)`——該查詢比對的是 `tool_id`
   欄位，settingId 永不相等，故一律 `User tool setting not found`。能力掛載路徑同樣受影響
   （`LLMService.kt`）。**修復**：新增 `findSettingByIdAndUserId(settingId, userId)`，
   `buildToolWithSetting` 改用之並以該筆設定建構（不再退回 `buildTool(toolId)`，
   避免同一工具有多筆設定時取錯）。修復後 TOOL 節點填 `toolSettingId` 可正常運作。

4. **ISSUE-2（缺陷；已於 2026-07-31 修復）：BUILT_IN 且有 config class 的工具不會被掛給模型**。
   TavilySearch 掛上後模型自述 `NO_TOOLS`，輸出 `events: []`。三段對照實驗（同一模型同一請求形狀）：
   只掛 Tavily → `NO_TOOLS`；只掛 google_map MCP → 列出 7 個工具；改掛 `DateTool`（CUSTOMIZE、無
   config class）→ 列出 `getCurrentTime`。
   **真正根因是 `toolIds` 走 `ToolService.buildToolWithoutSetting`，該方法對 `configObjectPath`
   非空的工具一律 `return null`**（只留一句 `工具 X 無法使用` 的 warn）——與 `ClassInstantiator`
   的反射邏輯無關（曾誤判於此，補齊 `Tavily.kt` 8 個欄位仍不通，即因為根本沒走到實例化）。
   **修復**：`LLMService` 的 `toolIds` 路徑改用 `buildTool`（會依 `(登入者, toolId)` 帶出使用者設定，
   無設定時退回無參數建構），並移除 `buildToolWithoutSetting`；同時在實例化回 null 時補上 error 日誌，
   避免再次靜默失敗。**GoogleSearch 同屬此型態，一併受惠。**

   > 教訓：**「工具沒被呼叫」不要用模型自述當判準**。本次模型在工具其實可用時仍答過 `NO_TOOLS`，
   > 也在工具不可用時宣稱「根據網路搜尋結果」。可靠作法是問一個**非搜尋不可能答對**的問題
   > （本次用「2026 台東最美星空音樂會 8/22 壓軸是誰」，正解 孫淑媚），或直接看後端日誌。

5. **ISSUE-3（設計限制，未修）：Agent 模式的 LLM 節點受 120 秒硬逾時**。
   `WorkflowEngine.DEFAULT_NODE_TIMEOUT_MS = 120_000`，而 `timeoutMs` 只有
   `HttpRequestNodeConfig` / `CodeNodeConfig` 可覆寫（`WorkflowEngine.kt:688-690`），
   `LLM_ASSISTANT` 無從調整。每個工具呼叫都是一輪 LLM 往返，6 個景點（6 次 `searchPlaces`）即逾時，
   降為 4 個景點後 48 秒完成。**設計 J10 類案例時務必控制工具呼叫輪數。**
   ⚠️ ISSUE-2 修復後，「4 景點 ＋ 1 次 searchWeb ＋ SKILL 掛載」的完整配置在 API 層實測需 **253 秒**，
   即使精簡 skill 內容仍逾時。**完整配置本身沒問題——把工作量降到「2 景點 ＋ 1 次搜尋」（3 次工具呼叫）後，
   7 節點、三種能力全掛 in:tool 的配置 30 秒即 SUCCESS 並同時取得真實景點與真實活動（run_14）。**
   真正的限制是「單一 LLM 節點能容納的工具往返輪數」——約 3 輪安全、5 輪即逾時。

6. **ISSUE-5（未修）：掛載腳本型 skill 會讓模型放棄輸出契約**。
   pdf skill 掛上後，模型改去輸出 `reportlab` Python 腳本（8,065 字）、自行把二日改成三日、
   且不呼叫搜尋工具，即使 PROMPT 節點明文禁止產生程式碼亦然。
   原因是 `LLMService.applyToolProviders` 會以 skill 指引**覆寫 `systemMessageProvider`**
   （追加「先用 `activate_skill`」的引導），其優先權高於 PROMPT 節點的使用者訊息；
   而平台的 SKILL 節點**不執行程式碼**，等於把「請寫 Python」的指示餵給一個永遠跑不了 Python 的環境。
   把 skill 精簡成只剩 `skill.md`（93KB → 8.5KB）仍會複現。
   **選用要掛載的 skill 時，應限於「描述輸出規範」而非「指示執行腳本」的類型。**

7. **ISSUE-4（觀察）**：瀏覽器關閉使 SSE 中斷後，execution 會停在 RUNNING 直到節點逾時才收斂為 FAILED，
   期間 UI 無從得知。

8. **`searchPlaces` 有三個必填參數**（`query` / `language` / `maxResults`），缺一即回
   `Missing required argument: <name>`。PROMPT 節點須明確要求模型帶齊，否則第一輪工具呼叫就失敗。

9. **環境陷阱：MCP jar 要用 `build/*-runner.jar` 而非 `build/libs/*.jar`**。
   `D:/MCP/google-map-1.0-SNAPSHOT.jar` 原為 24,935 bytes 的 thin jar（無 `Main-Class`），
   `java -jar` 直接報「沒有主要資訊清單屬性」。Quarkus uber-jar 產物在 `build/` 根目錄，
   `build/libs/` 下的是未經 quarkusBuild 的薄 jar。⚠️ `D:/MCP` 的 `date` / `filesystem` / `gmail`
   三支 jar 目前**同樣是 thin jar**，應一併確認。

10. **selector / 路由變更（影響既有 spec）**：
   - TRIGGER 已改為型別化表單，用 `data-test="trigger-type"` 選 `MANUAL`；
     §10 記載的 `mode-toggle` + `raw-input` raw 模式在 Inspector 已不存在。
   - 路由為 `/editor/:id?`，**新建時 URL 無 id**（`create-button` 不呼叫後端，見 §14 第 11 點），
     workflow id 須於存檔後再解析。
   - 7 節點在 1280 寬視窗會被右側 Inspector 遮住，handle 取不到 boundingBox 而連線靜默漏接。
     對策：視窗加寬至 1920，並在連線前按 `tidy-button` ＋ `zoom-fit`；**每連一條就斷言 edge 數**，
     才能定位到是哪一條失敗。

---

## 16. 首次全旅程（J1–J10）實測發現（2026-08-19～20，週期 202608192201）

本週期首次一次跑完 **全部 10 條旅程、92 個案例**（此前皆為單旅程切片），並在 R1 發現兩個前端缺陷、
修復後以 R2 重跑全範圍。R1 90/92（97.8%）→ **R2 92/92（100%）**。
報告見 `docs/test-confirmations/e2e-test-confirmation-202608192201.md`（截圖 R1 237 張 ＋ R2 231 張）。

### 16.1 產品缺陷（兩件，皆已修復並重驗）

1. **登入端點自身的 401 觸發全域登出重導，把錯誤訊息沖掉**（J1-04）。
   `api/http.ts` 的 response interceptor 對**任何** 401 呼叫 `handleUnauthorizedResponse()`，
   預設 handler 執行 `window.location.href = '/login'`（整頁導航）。帳密錯誤時後端回的正是 401，
   於是 `LoginView` 的 `ElMessage.error` 還沒渲染完頁面就被重載——實測 **3 次硬導航、`.el-message` 6 秒內從未出現、
   email 欄位被清空**，使用者完全得不到失敗原因。
   **修法**：interceptor 以 `isLoginRequest(url)` 排除登入端點；`api/auth.ts` 的 `login()` 自行 catch 並以
   `extractApiMessage(err)` 改拋帶後端訊息的 Error（否則畫面顯示的是 axios 通用字串）。
   > 教訓：`401` 同時承載「session 失效」與「這次登入帳密不對」兩種語意，**全域攔截器不能一視同仁**。

2. **Node Designer 改了節點設定，Inspector 表單不同步**（J9-10）。
   `inspector/forms/*.vue` 以 `const xxx = ref(props.config.xxx ?? '')` **只在建立當下讀一次 props**，
   沒有 `watch`。Inspector 與 Node Designer 的 Parameters 面板共用 `typedForms.ts` 但各自 mount 實例，
   A 實例 emit 更新了 `node.data.config`，B 實例的本地 ref 不會變。
   實測對照很明確：**畫布節點副標即時同步、`dirty-badge` 正確、DB 也是新值，唯獨 Inspector 停在舊值**，
   要重新選取節點或重整才刷新。
   **修法**：新增 `composables/useConfigSync.ts`，8 支型別化表單接上。
   > ⚠️ 實作陷阱（由單元測試抓到）：略過「自身 emit 的回流」必須先 `toRaw()` 再比對物件參考——
   > 物件存進 reactive 容器後讀回來是 **Proxy**，直接比對永遠不相等，會把自己的回流誤判成外部變更，
   > 導致使用者每打一個字就被重灌（中文組字期間尤其明顯）。

### 16.2 測試腳本層面的反覆踩雷（寫新腳本前務必先看）

R1／R2 合計有 **6 類**首輪誤判最後都證實是腳本問題，不是產品缺陷：

| 症狀 | 真因 | 正確作法 |
|------|------|---------|
| 拉線靜默失敗、toast 也沒出現 | 節點卡右緣的 `out:main` handle 落在右側 Inspector / Overview 面板底下，取不到 boundingBox | 1920 視窗下節點 x 控制在 **約 1100 以內**；連線前先 `deselect` 收合 Inspector（§11-4／§13-7 的老問題，本輪又中一次） |
| 讀不到 `stat-nodes` / `stat-connections` | 有節點被選取時右側面板切成 Inspector，Overview 不在 DOM | 先點畫布空白處 deselect 再讀 |
| 節點執行狀態 class 抓不到 | `is-exec-*` 掛在**內層** `.workflow-node`（`WorkflowNode.vue:76`），不在外層 `.vue-flow__node` | 選擇器寫 `.vue-flow__node[data-id="x"] .workflow-node` |
| toast 監看突然全空 | `page.goto` / `page.reload` 會摧毀注入的 MutationObserver | **每次導航後重新掛觀察器** |
| Node Designer 開著時點不到工具列／存檔逾時 | Designer 是全屏遮罩 | 任何回畫布或工具列的操作前，先確認 `node-designer-modal` 已消失 |
| 把 `workflow/get` 的回應整包當 `save` payload | 回應含 `status`／`updatedAt` 等 save 不接受的欄位，會先撞 JSON 解析錯而非預期的業務錯 | 自行組 `{id, version, name, description, canvasMeta, nodes[], edges[]}` |

另外兩點：

- **不要硬編 id / nodeKey 到後續腳本**。R1 有 4 支腳本硬編前一輪的 workflow id 與 nodeKey，
  R2 重跑時全部失效、得逐一改寫。改為「依名稱解析 workflow id、依 `type` 與 edge 的 `sourceHandle`
  解析 nodeKey」後，R2 可直接重跑（§6 的「ID 動態解析」原則同樣適用於**跨腳本**引用）。
- **為了製造「未存變更」而拖進來的節點，記得在存檔前刪掉**。J9 為驗未存離開確認拖了一個空 CODE 節點，
  之後的存檔把它一併寫進 DB，執行時撞 `missing required config fields: language, source`，
  害 J9-13 判成失敗。

### 16.3 環境現況

- **`getKnowledgeStore` 只列得出有 `llm_doc` 紀錄的知識庫**。本機兩個 `e2e-scb-tnc-*` 只有
  `202607272132` 那筆列得出來，因此 J8-B 的「2×KNOWLEDGE_RAG」兩個節點指向同一個知識庫。
  連線契約、必填契約與「不落主遍歷」都驗得到，但**要驗「多知識庫合併注入的內容差異」需另建第二個含文件的知識庫**。
- **`D:/MCP` 的 `date` / `filesystem` / `gmail` 三支 jar 仍是 thin jar**（無 `Main-Class`），
  只有 `google-map` 是可用的 uber-jar。J5 的 Agent 模式改用 `TOOL(DateTool)` 驗證工具實際被呼叫
  （回傳值與執行當下時間一致，比「問模型有沒有工具」可靠得多——§15-4 的教訓）。
- **J5-03 的 client 斷線行為與 §15-7 的 ISSUE-4 不同**：**頁內導航**（`onBeforeUnmount` → `executionStore.stop()`）
  會確實中止 SSE，execution 在 15 秒內收斂為 `CANCELLED`、下游 OUTPUT 記 `SKIPPED`；
  ISSUE-4 描述的長時間 RUNNING 是**關閉整個瀏覽器**的情境，兩者勿混為一談。
- **J10 的 ISSUE-5（腳本型 skill 劫持輸出）本輪未複現**：pdf skill 同樣掛 `in:tool`，但在
  PROMPT 與系統提示同時明文要求「只輸出 JSON、不要輸出任何程式碼」、且工作量壓到 3 次工具呼叫的條件下，
  模型完整遵守契約（59–60 秒完成）。**這不代表已修復**——提示一放鬆或工作量放大仍可能重現。

### 16.4 案例數的權威來源

本文件 §3 的旅程矩陣為**唯一權威**。截至本週期，各旅程案例數為
J1 9／J2 6／J3 9／J4 12／J5 12／J6 9／J7 2／J8-A 5／J8-B 5／J9 17／J10 8／J11 8／J12 7／J13 5／J14 6／**J15 9**，**合計 129**。
`e2e-test-confirmation` skill 的 `e2e-test-checklist.md` 摘要表與 Step 1.5 範圍選單須與此一致
（202608192201 週期修正前三處數字互不相同：摘要表寫 83、各列相加為 86、明細表實為 92）。

> J11–J14 於 2026-08-20 新增（依 `docs/plans/2026-08-20-automation-capability-gap-analysis.md` §5），
> 補上 `HTTP_REQUEST` / `DATA_TRANSFORM` / `LOOP` / `CONDITION`（資料流）/ `CODE` 五種節點的執行覆蓋——
> 在此之前這五種節點在本計畫中**只出現在拉線反例與「製造未存變更的道具」，從未真的執行過**。
> 四條旅程已於 **2026-08-20 週期 202608202139 首次實測，26/26 全數通過**（實測發現見 §17）。
> 先行標註的三處偏離（J11-07、J12-06、J13-05）皆已坐實；另因實測修訂了 §J12 的圖形與 J12-03 的筆數。

> **J15 於 2026-08-25 新增**（尚未實測），補上「注入型能力（`KNOWLEDGE_RAG`）＋ 呼叫型能力（`MCP_SERVER`）
> 併掛同一顆 LLM `in:tool`」的覆蓋——在此之前 J5 只驗單一 MCP、J8-B 只驗單一種類的 RAG 外掛、
> J10 的三種併掛**完全不含 KNOWLEDGE_RAG**，這條路徑是覆蓋圖上的空洞。設計依據見 §18。

> §J4 原有一條「已連 PROMPT 的 LLM 在 Inspector 檢視」與 §J6 的同名案例為同一情境，
> 確認表模板以 **J6-04** 收錄，J4 不重複計數，故 J4 為 12 而非 13。

---

## 17. J11–J14 首次實測發現（2026-08-20，週期 202608202139）

四條資料類節點旅程首跑，**26/26 全數通過（R1 單輪，console error = 0，無產品缺陷）**。
報告：`docs/test-confirmations/e2e-test-confirmation-202608202139.md`。
以下為改動計畫或影響後續腳本的實測結論。

### 17.1 計畫被實測推翻的兩處（已於本次修訂）

| 項目 | 原計畫 | 實測 | 處置 |
|------|-------|------|------|
| §J12 圖形 | 陣列由 TRIGGER 的 `inputPayload` 供應 | **UI 從不送 `inputPayload`**（`api/workflowExecution.ts:43` 註解自承），TRIGGER 輸出恆為 `{}`，該圖形**經 UI 不可達** | §J12 圖形改為 `TRIGGER → CODE → LOOP`，由 CODE 備料 |
| §J12-03 筆數 | N=3 時全圖 **6 筆** | 全圖 **7 筆**（漏計備料用的 CODE 節點） | 已更正為 7 筆並標明組成 |

> 前者同時是**產品缺口**：`constants/nodeDocs.ts` 教使用者以 `{{觸發節點key.欄位}}` 取值，
> 但 UI 沒有任何地方能填那些欄位。已登錄確認表問題追蹤區 #3。

### 17.2 三處預判的實作／規格偏離，皆已實測坐實

| 案例 | 規格怎麼寫 | 實際 |
|------|-----------|------|
| J11-07 | AC-D7：回 `WORKFLOW_HTTP_REQUEST_FAILED` | `Unexpected code 404`（寫死英文 `IOException`，`AppMessage` 無該鍵） |
| J12-06 | AC-D6：回 `WORKFLOW_LOOP_LIMIT_EXCEEDED` | **靜默截斷**、整體仍 SUCCESS，僅 `logger.warn` |
| J13-05 | AC-D5：回 `WORKFLOW_CONDITION_EVAL_FAILED` | `workflow.condition.operator.not.supported`（AC-D5 的鍵不存在） |

> 另新增一處同類：**節點逾時訊息亦為寫死英文** `Node <key> execution timed out after N ms`，
> 且攔截點在**引擎層**而非 `CodeExecutor` 自身的 `workflow.code.timeout`——兩層逾時語義待釐清。

### 17.3 撰寫腳本前必讀（本次踩到六個，全屬測試腳本層）

- **`selectors.setTestIdAttribute('data-test')` 是獨立腳本的必要設定**：專案用 `data-test`，
  Playwright 預設找 `data-testid`。`e2e/playwright.config.ts` 有設，**自寫的 standalone 腳本沒有**。
- **TRIGGER 有型別化表單**（`TriggerForm`），`triggerType` 由 `defaultConfig` 帶入 `MANUAL`，
  **不要**對它找 `mode-toggle`／`raw-input`（舊 J5 spec 的寫法已過時）。
- **JSON 編輯器的模式是元件層記憶的**：已在 JSON 模式時再按 `mode-toggle` 會**切回表格模式**。
  正確寫法是「`raw-input` 已可見就不按」的幂等判斷。
- **編輯器 URL 不帶 workflow id**（§J2-02）：`page.reload()` 會得到空白畫布，
  接著存檔會**新建一張 `未命名流程`**（本次即誤建一筆，已依 id 清除）。
  要重新載入既有流程，必須回列表點該列的**「編輯」鈕**（名稱欄是純文字、不是連結）。
- **節點數多時最右側節點會被 Inspector 面板遮住**（Playwright 報 `.inspector-panel` 攔截 pointer events）：
  8 節點的 §J13 需先按 `zoom-fit` 正規化視野，或把節點座標往左收。
- **斷言結構化資料不可透過 OUTPUT 節點**：`OutputNodeConfig.mappings` 是 `Map<String,String>`，
  會把陣列／物件序列化成字串。要驗上游節點的輸出契約，**直接讀該節點的 `node_execution.output`**。

### 17.4 引用不存在的變數會讓節點整顆失敗（影響圖形寫法）

`ExecutionContext.resolvePath` 解不到路徑時回 `Variable not found: <path>`，**該節點 FAILED、其下游 SKIPPED**。
兩個實際後果：

1. **對字串下鑽會失敗**（J11-04）：`HTTP_REQUEST` 的輸出是原始字串，
   `{{httpKey.response.data}}` 不是回 null 而是讓節點死掉。要抽欄位須先過 `CODE` 的 `JSON.parse`。
2. **分支匯流無法同時引用兩側**（J13）：OUTPUT 若同時寫 `{{A2.x}}{{B2.x}}`，
   被 SKIPPED 那側解不到 → OUTPUT FAILED。撰寫匯流節點時只能引用必定會執行的來源。

### 17.5 CODE 沙箱實測結論（J14 的實質收穫）

| 驗證項 | 結果 |
|--------|------|
| host class 存取 `Java.type('java.io.File')` | `ReferenceError: Java is not defined` —— `allowAllAccess(false)` 讓符號**根本不存在**，非拋權限例外 |
| 無窮迴圈 ＋ `timeoutMs=2000` | 節點 FAILED；逾時後 `/view/chat` 回 200，**同一張流程改回正常腳本仍能執行** → 執行緒與 context 確實回收 |
| 輸出 300KB | `Code script output exceeds the 256KB limit`，**非靜默截斷** |
| `language: python` | `Code node language not supported: python`，**不執行任何腳本** |
| 全域 `input` 與回傳值 | `input` 可讀到上游全部輸出；**最後一個表達式**即回傳值（非 `return`）；預設輸出鍵 `result`，`outputKey` 可覆寫且覆寫後預設鍵不再出現 |

---

## 18. J15 設計依據與首次實測發現（2026-08-25，週期 202608252128）

本節前半記錄 J15 **為什麼長這樣**（供日後維護者判斷能不能換題目），後半 §18.6 起為**首次實測發現**。

> **首次實測結果**：週期 **202608252128**，R1 單輪 **9/9 全數通過**，無產品缺陷、未改動任何產品程式碼。
> 報告見 `docs/test-confirmations/e2e-test-confirmation-202608252128.md`（截圖 29 張）。

### 18.1 為什麼需要 J15：覆蓋圖上的空洞

截至週期 202608202139，LLM／Milvus／MCP 三者的覆蓋**分散且互不交集**：

| 旅程 | 驗到什麼 | 沒驗到什麼 |
|------|---------|-----------|
| J5（12） | 真實 LLM 的 SSE 序列、Agent 模式**單一** MCP 掛載 | 不比對輸出內容 |
| J8-A / J8-B（各 5） | Milvus RAG 的 pipeline 與外掛兩模式、內容正確性 | RAG 是 `in:tool` 上**唯一**的能力；J8-B 的 2 顆 RAG 還指向同一個知識庫（§16.3） |
| J10（8） | MCP + TOOL + SKILL **三種工具型能力**併掛 | **完全不含 KNOWLEDGE_RAG**；另依賴 Tavily 金鑰與 pdf skill |

亦即「**注入型能力（推論前由框架注入 context）＋ 呼叫型能力（推論中由模型主動呼叫、產生額外往返輪次）
併掛同一顆 LLM**」從未被端到端驗證。兩者在 `LlmAssistantExecutor` 走不同機制，
**必填契約甚至相反**（RAG 外掛免填 `query`；MCP 即使純掛載也必填 `toolName`）——
契約相反這件事本身就值得一條案例釘住（J15-04）。

### 18.2 為什麼選這個業務情境

需要一個**同時強迫兩種能力生效、且兩者產物可各自獨立辨識**的題目。
「19 歲能不能開渣打數位存款帳戶 → 不行的話找信義區分行」滿足三個條件：

1. **RAG 側有現成且已驗證過的鑑別事實**：`tw-online-tnc.pdf` 的「年滿二十歲」，
   與 LLM 常識（民法成年 18 歲）不同。此鑑別點在 202607252225（答 18，向量庫故障）與
   202607272132（答 20，修復後）兩個週期形成過完整對照，可信度高（§14-3）。
2. **MCP 側有無法臆造的產物**：`place_id`。不需要依賴「模型自述有沒有呼叫工具」——
   §15-4 已證實模型自述完全不可靠（工具可用時答過 `NO_TOOLS`、不可用時宣稱「根據搜尋結果」）。
3. **兩者在語意上必須串起來**：先查文件判定資格，才會需要找分行。單純把兩個無關問題並列，
   模型可能只回答其中一個而讓案例失去鑑別力。

### 18.3 輪數預算是怎麼算出來的

`WorkflowEngine.DEFAULT_NODE_TIMEOUT_MS = 120_000`，且 `LLM_ASSISTANT` **無法覆寫 `timeoutMs`**
（§15-5 ISSUE-3）。§15 實測的經驗值是 **3 輪安全、5 輪即逾時**（4 景點＋1 次搜尋＋skill 曾需 253 秒）。

J15 的預算刻意壓到 **1 輪**：RAG 外掛 0 輪（推論前注入，不是工具呼叫）＋ `searchPlaces` 1 輪。
**PROMPT 節點必須明文限制「只呼叫一次 `searchPlaces`、`maxResults` 最多 3」**——
沒有這句話，模型會傾向逐間分行分開查詢，輪數立刻失控。

> 改題目時的紅線：**任何讓模型需要多次工具往返的變體（多城市、多分行逐一查詢、加掛第三種能力）
> 都會逼近 120 秒硬牆**。要擴大工作量請先確認 ISSUE-3 已修（`LLM_ASSISTANT` 可覆寫 `timeoutMs`）。

### 18.4 J15-09 的兩條失敗路徑（寫案例前必須分清楚）

`userSettingId` 缺席與 `userSettingId` 錯誤走的是**不同分支**，症狀卻都像「模型不肯呼叫工具」：

| 情境 | 走哪條路 | 實際機制 |
|------|---------|---------|
| 清空 `userSettingId`（J15-09 採用） | `buildDefaultMcpClient` | env 佔位符 `${google_maps_api_key}` **原樣傳入**，MCP 子行程起得來但 Places API 認證失敗 |
| 填不存在的 `userSettingId` | `buildUserSpecificMcpClient` | 以 `(userSettingId, userId)` 配對查無資料 → **回 null 被 `mapNotNull` 靜默丟棄，只留 WARN log**，模型連工具都看不到 |

兩者都**不會**在 UI 上給出明確錯誤。診斷唯一可靠的入口是後端日誌
（`D:/tmp/bestpartner/bestpartner.log` 的 WARN）——§15 的 ISSUE-1／ISSUE-2 當初正是因為
沒先看日誌而誤判成「模型行為問題」，繞了很大一圈。

### 18.5 已知的前置脆弱性

- **Milvus collection 會隨容器 volume 重建而清空**，而知識庫 metadata 在 Postgres：
  `getKnowledgeStore` 列得出知識庫**不代表**檢索有東西。前置就緒判準**只能**是
  `getDataFromEmbeddingStore` 實際命中（§14-6）。
- **重建知識庫必須換新 knowledgeId**：`deleteData` 不刪 `llm_knowledge` 列（§14-5）。
- **embedding 維度不會被校驗**：`MilvusBuilder` 直接把 `dimension` 餵給 `MilvusEmbeddingStore`，
  不一致要到 `addAll` / `search` 才炸。本機選型為 OpenRouter `nvidia/nemotron-3-embed-1b:free`
  （實測 **2048**，種子檔的 `openrouter_local_embedding_test` 是另一個模型、dim 1536，勿混用）。
  建 collection 前先打一次 `/embeddings` 量測最保險（§13-3）。
- **`D:/MCP` 的 jar 不在版控**：只有 `google-map-1.0-SNAPSHOT.jar` 是可用的 uber-jar，
  `date` / `filesystem` / `gmail` 仍是無 `Main-Class` 的 thin jar（§15-9、§16.3）。**換機器即整批失效**。

### 18.6 首次實測發現（2026-08-25，週期 202608252128）

**契約面全部成立**（這是 J15 存在的目的，已驗證）：

- RAG 與 MCP 兩條 `out:main → LLM in:tool` **併掛皆放行**，無相容性 toast、console error = 0，
  Overview 6 NODES / 5 CONNECTIONS，存檔 version 1。
- **相反的必填契約同時成立**：RAG 落庫僅 `{topK, knowledgeId}`（`query` / `embeddingModelId` 全空）仍可啟用；
  MCP 則必須帶 `toolName`。啟用後狀態轉 ACTIVE，未回 `workflow.node.config.required.missing`。
- **兩種能力節點皆不落主遍歷**：SSE 恰 4 個節點發事件、`llm_workflow_node_execution` 恰 4 筆，
  無 `KNOWLEDGE_RAG` / `MCP_SERVER` 型別；UI 上兩顆能力節點 Inspector 無 `node-exec-section`
  且畫布上不帶執行勾選標記。
- **雙來源同時生效已取得完整實證**：單一 `finalOutput` 同時含文件事實（`ageThreshold` 二十歲、判 19 歲不符）
  與 MCP 真實資料（`place_id` `ChIJ_1dgl7qrQjQRLnEKiYk2w5o`、臺北市信義區地址、真實經緯度）。
- **輪數預算設計正確**：`searchPlaces` 每次執行僅 1 輪，整體 8–20s，遠低於 120s 硬牆。

**⚠️ 三個必須寫進計畫的實測教訓**：

1. **RAG＋MCP 併掛時，LLM 最終回覆會間歇性截斷（未解，最重要）**。
   目標配置取樣 7 次：**3 次完整（260/262/260 字元）、4 次截斷（71/73/75/73 字元）**，
   且 4 次都截在同一語意位置（RAG 注入的條款原文中間）。呈**雙峰分布**、與節點耗時完全相關
   （完整 12.3–17.0s／截斷 8.2–9.3s）。單一能力對照組（RAG only 549 字元、MCP only 316 字元）
   與無工具直呼 `/llm/chat` 皆完整。已排除 DB 欄位長度、固定位元組截斷、memory 視窗、工具呼叫失敗。
   **產品面的重點不是截斷本身，而是平台把不完整的模型回覆一律記為 SUCCESS**——
   未檢查 `finishReason`、無任何警示，使用者無從察覺答案被截斷。
   ⚠️ **寫 J15-07 斷言時務必容忍此不穩定性**（可重跑取樣），不要因單次截斷就判定併掛壞掉。

2. **拆掉 MCP 後模型會捏造 `place_id`，不是不輸出**（原 §J15-08 預期已據此修正）。
   實測捏造值為 `ChIJfQmJx7-qQjQRQJbQ6Q6Q6Q6/…Q7/…Q8`（尾碼流水）、三筆座標完全相同。
   **判準必須是「與工具實際回傳值逐字元相符」**，只檢查「有無 place_id」會被幻覺矇混過去。

3. **MCP 缺 `userSettingId` 是「全靜默降級」，不是失敗**（J15-09 的交付物，補充 §12-1）。
   實測鏈路：MCP 子行程照常啟動 → `searchPlaces` 照常被呼叫 → Places API 回
   `INVALID_ARGUMENT: API key not valid`（證實 `${google_maps_api_key}` 未替換）→
   ⚠️ **MCP 協定層回 `isError: false`**（錯誤只包在 `content[].text` 內）→
   **節點與整體執行皆 SUCCESS、`error_message` 為 null、後端無任何 WARN/ERROR**。
   使用者只會看到「執行成功但答案沒有分行資料」。
   ⚠️ 與「填**不存在**的 `userSettingId`」是**不同路徑**（後者走 `buildUserSpecificMcpClient`，
   回 null 被 `mapNotNull` 丟棄、至少留 WARN）——寫案例時勿混為一談。

**腳本層踩雷（補充 §16.2）**：

- **登入頁沒有任何 `data-test`**（`LoginView.vue` 用 Element Plus 原生元件），
  須改用 `input[type=email]` / `input[type=password]` / `button:has-text("登入")`。
  全 repo 的 `data-test` 慣例未涵蓋登入頁，屬慣例缺口。
- **`node-exec-section` 依賴前端 `executionStore.nodeStates`，不是後端資料**：
  在新開分頁檢查會得到「全部都是 0」的假象（連對照組 LLM 也是 0）。
  必須在**執行完成後、同一 session、不關結果抽屜**的狀態下檢查。
- palette → 畫布的 DnD 鍵為 **`application/node-type`**（`components/canvas/dragKeys.ts`），
  handle 選擇器為 `.vue-flow__handle[data-nodeid][data-handleid]`。
- 本機無 Python Playwright，webwright 契約以 Node ＋ `bestpartner-ui/node_modules/@playwright/test`
  ＋ chromium 履行（同 §14-7）。
