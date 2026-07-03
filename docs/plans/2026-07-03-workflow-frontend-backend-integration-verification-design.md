# Workflow 前端 × 後端 端到端整合驗證設計

- **日期**：2026-07-03
- **範圍**：`bestpartner-ui` workflow 編輯器 ↔ `bestpartner-service` `/llm/workflow/*`、`/login` 的端到端整合驗證
- **狀態**：設計已定案，實作待啟動（等本機 PostgreSQL 就緒）

---

## 背景與問題定義

前端 workflow 編輯器在**程式碼層面已完整串接真實後端**，並非離線 demo：

| 環節 | 現況 |
|------|------|
| Vite proxy | `/llm`、`/login` → `http://localhost:80`（`vite.config.ts`） |
| 登入 | `auth.ts` 打真實 `POST /login/`，存 JWT，router guard 擋未登入 |
| 清單頁 | `fetchList/remove/switchStatus` 全打真實 API |
| 編輯頁 | `load/save/switchStatus` 全打真實 API，含樂觀鎖版本衝突處理 |
| HTTP | JWT 攔截器 + 401/403 自動登出，`ApiResponse` 統一解包 |

**真正的缺口**：這條鏈路從未對著真的跑起來的後端端到端驗證過——目前全部只有 mock 單元測試。沒人確認過前端期待的 DTO 欄位形狀是否真的與後端回傳一致、登入成功碼、版本衝突訊息字串、node/edge 能否無損 round-trip。

本設計的本質是**端到端整合驗證 + 修補浮現的契約落差**，不是撰寫新的串接程式。

## 目標

- 以真實使用者流程驗證前端 ↔ 後端整合，抓出並修補契約落差。
- 全程使用真實 `admin/admin` 登入，不依賴任何 `token=dev` 之類離線 hack。

## 非目標（YAGNI）

- 不驗 workflow **執行引擎**（另一題）。
- 不驗 tool/mcp/vector 註冊端點，除非擋住節點屬性面板選項載入（`useNodeOptions`）。
- 不做 Vue Flow 拉線手勢的 UI 自動化驗證（已有單元測試覆蓋）。
- 不改造 auth/登入 UI、不碰執行歷史（Executions 分頁維持 disabled）。

---

## 取向決策

**採取向 A：瀏覽器真實流程為主 + Network 面板抓實際 payload。**

以 headless Chrome 走真實使用者流程，同時攔截每個 API 的實際 request/response JSON 比對前端 TS 型別。理由：整合 bug 往往藏在前端解析回應那一層，只有真的跑過瀏覽器才抓得到（`dtoToFlow` round-trip、router guard、401 攔截）。

已評估未採用：
- **取向 B（純 API 腳本）**：快、穩，但驗不到前端解析與 UI 綁定。
- **取向 C（混合）**：契約用腳本、UI 用瀏覽器補跑；比 A 複雜，本次選 A 保持單一驅動面。

---

## 第 1 段：驗證環境與量測手法（Harness）

**前置（使用者負責）**：啟動本機 PostgreSQL，確保 `pgdb` / `pguser` / `pgpass` 可連、且已灌 `docs/sql/bestpartner-ddl.sql` + `bestpartner-init-data.sql`（含預設 `admin/admin`）。

**服務啟動（執行者負責）**：
1. 用既有 runner jar 起 Quarkus 於 **:80**（dev profile，Swagger 開著方便對照）。先打 `/systemSetting/list` 確認 DB 連上、服務活著。
2. 起 Vite dev（:5173），proxy 把 `/llm`、`/login` 轉到 :80。

**驅動方式**：superpowers-chrome `use_browser`（CDP headless）開 `http://localhost:5173`，走真實使用者流程，用真實 `admin/admin` 登入。

**契約量測（核心）**：每個 API 呼叫用 `read_network_requests` 攔下 request + response 的實際 JSON，逐一比對：
- 回應是否符合 `ApiResponse<T>` 包裝（`code`/`message`/`data`）。
- `data` 內欄位形狀是否吻合 `types/workflow.ts` 與 `unwrap<T>` 的假設。
- 存檔 round-trip：送出的 node/edge JSON 經後端存回、再 `get` 回來能否無損還原（`dtoToFlow`/`flowToSaveRequest` 對稱性）。

**產出**：每個 scenario 存截圖 + 攔到的 payload 至 scratchpad，落差整理成清單。

---

## 第 2 段：驗證 Scenario 清單（依序執行）

| ID | Scenario | 端點 | 要驗什麼 | 高風險契約點 |
|----|----------|------|----------|--------------|
| **S0** | 服務健檢 | `GET /systemSetting/list` | DB 連上、`ApiResponse` 包裝正常 | DB 沒連上即中止 |
| **S1** | 登入 | `POST /login/` | 回應 `code`、`data` 為 JWT、localStorage 存入、導向清單頁 | ⚠️ `auth.ts` 寫死 `code === 200` 才算成功，成功碼不符則登入直接爆 |
| **S2** | 清單 | `GET /llm/workflow/list` | 每筆 `id/name/status/version/updatedAt` 欄位齊全 | ⚠️ `updatedAt` 直接顯示於表格，須為可讀字串 |
| **S3** | 新建 + 存檔 | `POST /llm/workflow/save`（無 id） | 拖入 TRIGGER + LLM_ASSISTANT → 存檔，回應回吐 `id/status/version` | ⚠️ 前端靠這三個欄位更新 state |
| **S4** | 重載 round-trip | `POST /llm/workflow/get` | 重整後畫布無損還原節點 `nodeKey/type/name/positionX/Y/config` | ⚠️ `config` 巢狀物件、position 數值型別、`sourceHandle` null/undefined 落差 |
| **S5** | 二次存檔（樂觀鎖） | `POST /llm/workflow/save`（帶 id+version） | 兩分頁製造版本衝突 | ⚠️ 後端訊息須逐字吻合 `VERSION_CONFLICT_MESSAGES`，差一字即失效 |
| **S6** | 啟用 | `POST /llm/workflow/switchStatus` | Active toggle 啟用流程 | ⚠️ 後端要求須含 Trigger 且無環，驗錯誤訊息正確浮現 |
| **S7** | 刪除 | `POST /llm/workflow/delete` | 清單頁刪除後由 summaries 移除 | — |
| **S8** | 401 攔截 | 任一受保護端點 | 手動塞壞 token → 攔截器自動登出重導 `/login` | — |

---

## 第 3 段：連線 round-trip 處理 + 修補策略

### 連線（edge）怎麼驗——避開 Vue Flow 拉線不穩

Vue Flow 連線靠 pointer 事件，headless 自動化拉線不可靠。但 S3/S4 round-trip 若缺 edge 就驗不到 `sourceHandle`/`targetHandle`/`label` 這些最易出落差的欄位。兩層退路：

1. **首選**：`use_browser` 對來源 handle → 目標 handle 做 CDP 級 `mousedown → mousemove → mouseup`（真實 pointer 座標序列）。
2. **退路**：CDP 拉線仍不穩時，改用 `javascript_tool` 在頁面內直接呼叫 Vue Flow `addEdges`（等同 `onConnect` 產物，帶 `makeEdgeId` 的 handle 編碼），再走正常存檔。

**界線說明**：退路是為了驗**存檔/後端/重載** round-trip，非驗使用者拉線手勢（後者屬 UI 互動，已有單元測試覆蓋）。報告會明講此界線。無論哪條退路，S4 重載後都比對 edge 四個 handle 欄位 + `label` 能否無損還原。

### 找到落差後的修補策略

- **判定基準**：後端 `WorkflowDTO` 是 source of truth。落差預設**改前端**對齊後端實際回傳，除非後端明顯違反自己的 API 文件（`api-endpoints.md`）才動後端。
- **分級**：
  - **Blocker**（如 S1 登入成功碼不符）→ 立即修，否則後續全卡。
  - **Contract**（欄位形狀/命名/null 假設）→ 修 `types/*.ts`、`unwrap`、`dtoToFlow`/`flowToSaveRequest` 或對應 mapper。
  - **Brittle**（如 S5 寫死的衝突訊息字串比對）→ 記錄並提改善建議，不一定當場改。
- **每個修補**：補/更新對應單元測試，`vue-tsc` + `vitest` 綠燈才算完成。
- **動到後端**（若需）→ Kotlin 改動觸發 `documentation-sync` 強制規範。
- **產出**：落差報告（scenario × 期望 vs 實際 × 修法 × 狀態），存 scratchpad。

---

## 第 4 段：成功準則與交付定義（Done）

**驗證通過的定義**

- **S0–S8 全綠**：九個 scenario 都跑完，各留截圖 + payload 佐證。
- **核心 round-trip 無損**：S3→S4 存檔再取回，節點與連線逐欄比對一致，`config` 巢狀物件不走樣。
- **關鍵路徑真的通**：真實 `admin/admin` 登入 → JWT → router guard → 清單 → 存檔 → 啟用 → 401 自動登出，全程無離線 hack。
- **落差修補完成**：所有 Blocker / Contract 落差已修並回歸，`vue-tsc` exit 0、`vitest` 全綠。Brittle 級可只記錄，但須在報告列明。

**交付物**

1. **落差報告**（scratchpad）：scenario × 期望 vs 實際 × 判定分級 × 修法 × 狀態表格，含截圖與 payload 佐證。
2. **程式修補**：對齊契約的 `types/*.ts` / mapper / interceptor 變更 + 對應測試，依「分批 commit」慣例分類型提交（`修復` / `測試` / 必要時 `文件`）。
3. **若動到後端 Kotlin**：跑 `documentation-sync`。

---

## 相依契約參考（設計時快照）

- **登入**：`POST /login/ {email,password}` → `ApiResponse<string>`，`auth.ts` 要求 `code === 200`、`data` 為 JWT。
- **清單**：`GET /llm/workflow/list` → `ApiResponse<WorkflowSummaryDTO[]>`。
- **存檔**：`POST /llm/workflow/save`（`WorkflowSaveRequestDTO`）→ `WorkflowDTO`，前端讀 `saved.id/status/version`。
- **取得**：`POST /llm/workflow/get {id}` → `WorkflowDTO`，含 `nodes[]`（`nodeKey/type/name/positionX/Y/config`）與 `edges[]`（`sourceNodeKey/targetNodeKey/sourceHandle/targetHandle/label`）。
- **版本衝突訊息**（`stores/workflow.ts` 寫死比對）：
  - `Workflow has been modified by another session, please reload`
  - `工作流程已被其他作業修改，請重新載入`
