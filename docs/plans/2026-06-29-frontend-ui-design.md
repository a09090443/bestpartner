# BestPartner 前端（bestpartner-ui）v1 設計

> 對應後端 Workflow 引擎 Phase 1（定義 CRUD 已完成）。本文件為前端 v1 的設計依據，後續以 `superpowers:writing-plans` 細化為實作計畫。
> 專案慣例「不使用 git worktree」，實作直接在主目錄進行。

**Goal**：以 Vue 3 + Vue Flow 打造 n8n-like 視覺化 workflow 編輯器。v1 聚焦「**純編輯器**」主線——登入後可建立／編排 workflow（拉節點、連線、設定屬性），存檔到後端並可重新載入回顯。執行（execute）與觸發器 UI 待後端 Phase 2-4 就緒後再做。

## 已確認決策

| 項目 | 決策 |
|------|------|
| v1 範圍 | 純編輯器優先（畫布編輯 + 存取），不含執行/回放 UI |
| 專案位置 | 同 repo 子目錄 `bestpartner-ui/` |
| 認證 | 極簡登入頁（email/password → JWT 存 localStorage + 路由守衛），v1 不做 token refresh |
| 節點 config 編輯 | 通用 JSON / key-value 編輯器（10 種 NodeType 皆可拉） |
| UI 元件庫 | Element Plus |
| 技術棧 | Vue 3（`<script setup>`）+ Vite + TypeScript + Pinia + Vue Router + `@vue-flow/core` + axios |

---

## 1. 整體架構與目錄結構

**定位**：`bestpartner-ui/` 為獨立 Vite SPA。開發時用 Vite dev server（預設 5173），透過 **dev proxy** 把 `/llm`、`/login` 轉發至後端 `http://localhost:80`，避免 CORS。正式部署 build 成靜態檔，由任意靜態伺服器或反向代理服務。

**技術棧**：Vue 3（Composition API）、TypeScript、Vite、Pinia、Vue Router、`@vue-flow/core`（畫布）、Element Plus（UI）、axios（API client）。

**目錄結構**：
```
bestpartner-ui/
├── src/
│   ├── api/            # axios 實例 + workflow / auth API 封裝
│   ├── stores/         # Pinia: authStore, workflowStore
│   ├── router/         # 路由定義 + 守衛
│   ├── views/          # LoginView, WorkflowListView, WorkflowEditorView
│   ├── components/
│   │   ├── canvas/     # 自訂節點、節點面板、工具列
│   │   └── inspector/  # 屬性面板（含通用 JSON 編輯器）
│   ├── types/          # 對應後端 DTO 的 TS 型別
│   ├── composables/    # useWorkflowSync 等
│   ├── App.vue
│   └── main.ts
├── .env.development    # VITE_API_BASE 等
├── vite.config.ts      # dev proxy 設定
├── tsconfig.json
└── package.json
```

**型別對應**：`types/` 手寫對齊後端 `WorkflowDTO / WorkflowNodeDTO / WorkflowEdgeDTO / WorkflowSaveRequestDTO / WorkflowSummaryDTO` 與 `NodeType`、`WorkflowStatus` enum，作為前後端契約的單一真實來源。

---

## 2. 路由與認證流程

**路由表**（Vue Router，history 模式）：

| 路徑 | 視圖 | 守衛 |
|------|------|------|
| `/login` | `LoginView` | 公開；已登入則導向 `/` |
| `/` | `WorkflowListView` | 需登入 |
| `/editor/:id?` | `WorkflowEditorView` | 需登入；無 `:id` = 新建、有 `:id` = 編輯既有 |

**認證流程**：
1. `LoginView` 表單（Element Plus `el-form`，email + password）→ `POST /login/`。
2. 後端回傳 JWT（`ApiResponse.data`）→ 存入 `localStorage` 並寫進 `authStore`。
3. 全域 `router.beforeEach`：需登入但無 token → 重導 `/login`；已登入訪問 `/login` → 重導 `/`。

**axios 攔截器**：
- **request**：自動加 `Authorization: Bearer <token>`。
- **response**：HTTP 401/403 或 `ApiResponse.code=401` → 清 token、導回 `/login`、訊息提示；其餘業務錯誤（`code=400`）統一彈出後端 i18n `message`，交呼叫端決定是否額外處理。

**YAGNI 取捨**：v1 不做 token 自動 refresh（過期即重登）、不做註冊頁。登出按鈕於列表頁右上角，清 token + 導回 `/login`。

---

## 3. 畫布編輯器（核心）

**版面**：`WorkflowEditorView` 內嵌 `<VueFlow>`，三區塊——左側**節點面板**（10 種 NodeType 可拖入）、中央**畫布**、右側**屬性面板（Inspector）**。

**Vue Flow ↔ 後端模型對應**：

| Vue Flow | 後端 |
|----------|------|
| `Node.id` | `nodeKey`（前端產生，畫布內唯一）|
| `Node.type` | 自訂節點元件，依 `NodeType` 渲染外觀 |
| `Node.position {x,y}` | `positionX / positionY` |
| `Node.data` | `{ name, type, config }` |
| `Edge.source/target` | `sourceNodeKey / targetNodeKey` |
| `Edge.sourceHandle/targetHandle` | `sourceHandle / targetHandle` |

**nodeKey 產生**：拖入新節點時用短 id（如 `nanoid(8)`）當 `nodeKey`，與後端 `varchar(64)` 相容。

**自訂節點元件**：通用 `WorkflowNode.vue`，依 `type` 顯示圖示與標題色塊、輸入/輸出 handle；`TRIGGER` 特別標示（啟用 workflow 的必要節點）。

**Inspector**：選取節點時可編輯 `name` 與 `config`；`config` 用**通用 JSON / key-value 編輯器**（「key-value 表格」與「raw JSON」雙模式切換，存檔前驗證 JSON 合法性）。未選取節點時，Inspector 顯示 workflow 本身 meta（name、description）。

**工具列**：新增節點、刪除選取、存檔、（唯讀）目前 status 與 version。

**前端輕量驗證**（即時提示，不取代後端）：nodeKey 重複、edge 連到不存在節點、空畫布提示——與後端 `validateGraph` 一致，後端仍為最終守門員。

---

## 4. 狀態管理、API client 與存檔流程

**Pinia stores**：
- `authStore`：`token`、`username`、`isAuthenticated`；actions：`login()`、`logout()`、`loadFromStorage()`。
- `workflowStore`：當前編輯中的 workflow（`id`、`name`、`description`、`status`、`version`、`nodes`、`edges`、`canvasMeta`）+ 列表頁 `summaries`；actions：`fetchList()`、`load(id)`、`createNew()`、`save()`、`remove(id)`、`switchStatus(id, active)`、`setDirty()`。

**API client**（`api/`）：
- `auth.ts`：`login(email, password)`。
- `workflow.ts`：對應 7 端點（`create / save / get / list / update / delete / switchStatus`），輸入輸出皆用 `types/` TS 型別。

**畫布 ↔ store 轉換**（composable `useWorkflowSync`）：
- **載入**：`get(id)` → `WorkflowDTO` → 轉成 Vue Flow `nodes/edges`（`config` JSONObject → 物件）回填畫布。
- **存檔**：畫布 `nodes/edges` → `WorkflowSaveRequestDTO`（帶當前 `version`）→ `save()`。

**存檔 / 樂觀鎖 UX**：
1. 成功 → 後端回新 `version`，更新 store、清 dirty、成功提示。
2. `WORKFLOW_VERSION_CONFLICT` → 對話框「此 workflow 已被其他來源修改」+「重新載入」按鈕（捨棄本地、重抓最新）。v1 不做自動合併。
3. 有未存變更時，`beforeRouteLeave` + `beforeunload` 攔截提示。

**dirty 追蹤**：任何節點/連線/config 變更 → `setDirty(true)`；存檔成功歸零，工具列顯示未儲存標記。

---

## 5. 錯誤處理、測試與里程碑

**錯誤處理**：
- 網路 / 401：axios 攔截器統一處理（清 token、導回登入）。
- 業務錯誤（`code=400`）：統一 `ElMessage` 顯示後端 i18n 訊息；版本衝突另走專屬對話框。
- 前端驗證：存檔前跑輕量 `validateGraph`，擋下明顯錯誤並標記問題節點。
- JSON 編輯：config 非法 JSON 即時提示、存檔阻擋。

**測試**（Vitest + Vue Test Utils）：
- 單元：`useWorkflowSync` 畫布↔DTO 雙向轉換（含 config JSON round-trip）、輕量 `validateGraph`、authStore/workflowStore actions。
- 元件：Inspector JSON/key-value 雙模式、登入表單。
- E2E（選配，v1 可延後）：Playwright 跑「登入 → 新建 → 拉 2 節點連線 → 存檔 → 重載回顯」主線。v1 先以單元/元件測試為主。

**里程碑（建議實作順序）**：

| M | 名稱 | 範圍 |
|---|------|------|
| M1 | 專案骨架 | Vite + Vue3 + TS + Element Plus + Vue Flow + Pinia + Router，dev proxy 通，空白頁可跑 |
| M2 | 認證 | 登入頁 + authStore + 攔截器 + 路由守衛 |
| M3 | 列表頁 | list / create / delete / switchStatus |
| M4 | 畫布編輯器 | Vue Flow + 自訂節點 + 節點面板 + 拖放 |
| M5 | Inspector + 存取 | 屬性面板 + 通用 JSON 編輯器 + save/get round-trip + 樂觀鎖 UX |
| M6 | 收尾 | dirty/離頁攔截 + 輕量驗證 + 測試 |

---

## 與後端的銜接

- v1 僅依賴 Phase 1 已完成的 7 個 `/llm/workflow/*` 端點與 `/login/`。
- 後端 Phase 2（execute、execution 查詢）就緒後，前端再加「執行/回放」UI（屬於後續迭代，不在 v1）。
- TS 型別（`types/`）需與後端 DTO 保持同步；後端 DTO 變更時一併更新。
