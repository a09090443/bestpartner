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
| P1 | 能力掛載：拖 TOOL / SKILL 節點，`out:main` 連到 LLM 的 `in:tool` 埠並 save | edge 建立（`targetHandle=in:tool`）；重載後畫布還原、連線保留 |
| P0 | 選節點 → InspectorPanel 填 config | 表單值寫回節點 |
| P0 | save 整張覆寫 | version+1；重載後畫布還原（nodes/edges/config 一致） |
| P1 | 重新整理頁面後 get 還原 | 畫布與 Inspector 內容與存檔一致 |

### J4 圖驗證與啟用（P1）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | 缺 TRIGGER 節點時 switchStatus 啟用 | 400，顯示對應訊息 |
| P1 | 圖有環時啟用 | 400，顯示對應訊息 |
| P1 | 節點缺必填 config 啟用（如 LLM_ASSISTANT 缺 `llmId`） | 400，`workflow.node.config.required.missing`，UI 顯示 nodeKey 與缺漏欄位 |
| P1 | 孤兒 SKILL 節點（未連任何 LLM `in:tool`）save/啟用 | 400，`workflow.skill.node.not.mounted`，UI 顯示 nodeKey |
| P1 | 非 TOOL/MCP/SKILL 節點拉線到 LLM `in:tool` 埠 | 拉線即被擋（toast），存檔驗證回 `INCOMPATIBLE_CONNECTION` |
| P1 | 合法圖 switchStatus 啟用 | 狀態轉 ACTIVE |

### J5 執行 workflow（P0，**真實 LLM**）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | 合法 workflow（TRIGGER → LLM_ASSISTANT → OUTPUT，DRAFT 即可）execute | ExecutionResultDrawer 依序反映 SSE：`execution.started` → 各節點 `node.started` / `node.completed` → `execution.completed`（SUCCESS）；`llm_workflow_execution` / `llm_workflow_node_execution` 有紀錄 |
| P1 | Agent 模式：TOOL / SKILL 節點以 `out:main → LLM in:tool` 掛載後 execute | LLM 可自主呼叫掛載工具並回覆；能力節點**不產生**獨立 `node.started` / `node.completed` 事件、`node_execution` 無其紀錄（僅 LLM 節點內部呼叫） |
| P1 | 某節點設定錯誤導致失敗 | 該節點顯示 `node.failed` 狀態與錯誤；執行標記失敗 |
| P1 | 執行中途 client 斷線（關抽屜 / 離開頁面） | 執行標記 `CANCELLED`，未執行下游節點標記 `SKIPPED`（對應 commit b24c128 的視覺行為） |

> **真實 LLM 斷言策略**：因輸出非確定，**不比對文字內容**；斷言聚焦於
> ①SSE 事件序列與型別正確、②每個節點狀態最終為 completed、③整體 `execution.completed=SUCCESS`、④DB 執行紀錄寫入。
> 逾時放寬（見 §6）；此案例需有效 `E2E_LLM_ID`，缺席時 skip 並在報告標註。

### J6 Inspector 表單（P1）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | LlmAssistantForm 選 `llmId`（下拉來自 LLM setting；表單已精簡，無工具/MCP/Skill/知識庫欄位） | 值寫回並可存檔；不再出現 `tool-ids` / `mcp-ids` / `skill-ids` / `knowledge-id` 欄位 |
| P1 | ToolForm / McpServerForm / SkillForm / KnowledgeRagForm / OutputForm 填寫 | 各節點 config 正確寫回；save 後重載一致（SkillForm 選 `skillId`，下拉來自 `/llm/skill/list`） |
| P2 | settingSchema 動態表單（sensitive 欄位遮罩） | 依 schema 正確渲染欄位型別 |

### J7 契約錯誤（P2）

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P2 | save 節點 config 型別錯誤（未知欄位 / 結構性型別錯） | 400，`workflow.node.config.invalid`，UI 顯示含 nodeKey 的訊息 |
| P2 | 樂觀鎖：兩處先後 save 同一 workflow，version 不符 | 400，`workflow.version.conflict`，UI 顯示衝突提示 |

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
- E2E 旅程若涉及 API 契約變更，須同步 `docs/api-test-plan.md` 與 `.claude/rules/api-endpoints.md`。
- 測試資料清理策略異動時，須確認仍不違反「備份只能寫 `bestpartner-init-data.sql`」鐵則。

---

## 10. 首條切片實測發現（2026-07-12）

首條 J5 垂直切片已落地並在真實 stack（port 80 + Postgres + 真實 OpenRouter）跑通，harness 位於 `bestpartner-ui/e2e/`（`playwright.config.ts` / `global-setup.ts` / `global-teardown.ts` / `fixtures/db.ts` / `specs/j5-openrouter-date-json.spec.ts`）。此切片：TRIGGER → LLM_ASSISTANT(OpenRouter, JSON) ←in:tool← TOOL(DateTool) → OUTPUT(JSON)，並直連 Postgres 斷言 `llm_workflow_execution` / `llm_workflow_node_execution` 落庫。

**已驗證**：HTML5 DnD 合成事件可靠、TRIGGER 無表單走 `JsonConfigEditor` raw 填 `triggerType`、`data-test` 選擇器、DB 落庫、ID 動態解析（見 §6）。

**待後端釐清（不阻擋 E2E，但影響斷言語意）**：

1. **TOOL(date) 節點會被引擎「獨立執行」且需 `arguments.zoneId`**：`ToolNodeExecutor` 直接呼叫 `DateTool`，未帶 `zoneId` 時 `ZoneId.of(null)` 拋 `ZoneRulesException` 使整條 FAILED；且該節點會落 `nodeType=TOOL` 執行紀錄——與「連 `in:tool` 僅為能力掛載、不獨立執行」的描述矛盾。
2. **連到 `in:tool` 的 date 工具未真正掛給 LLM 當能力**：實測 LLM 回「此環境未提供可查詢即時時間的工具」，代表 Agent 模式的 langchain4j 工具集未納入該 TOOL 節點。故 J5 此切片**只斷言 plumbing / SSE `SUCCESS` / OUTPUT 為 JSON / DB 落庫，不斷言 LLM 實際呼叫了工具**，待後端修復後再強化。
