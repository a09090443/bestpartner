# BestPartner 前端（bestpartner-ui）v1 實作計畫

> **For Claude:** REQUIRED SUB-SKILL: 使用 superpowers:executing-plans 逐任務實作本計畫。
> 本專案慣例「不使用 git worktree」，請直接在主目錄 `D:\projects\bestpartner` 上實作。

**Goal:** 在 `bestpartner-ui/` 建立 Vue 3 + Vue Flow 的 n8n-like 視覺化 workflow 編輯器 v1：登入後可建立／編排 workflow（拉節點、連線、設定 config），存檔到後端 Phase 1 的 `/llm/workflow/*` 端點並可重新載入回顯。

**Architecture:** 獨立 Vite SPA，置於 monorepo 子目錄 `bestpartner-ui/`。開發時 Vite dev server 以 proxy 轉發 `/llm`、`/login` 至後端 `http://localhost:80`。狀態以 Pinia 管理（authStore、workflowStore），畫布以 `@vue-flow/core` 呈現，畫布模型與後端 DTO 經 `useWorkflowSync` composable 雙向轉換。

**Tech Stack:** Vue 3（`<script setup>`）、TypeScript、Vite、Pinia、Vue Router、`@vue-flow/core`、Element Plus、axios、nanoid；測試用 Vitest + @vue/test-utils（jsdom）。

**規格來源:** `docs/plans/2026-06-29-frontend-ui-design.md`（架構、路由、畫布模型對應、狀態管理、錯誤處理、里程碑以該文件為準）。

---

## 前置須知（v1 共用）

- **後端契約**：v1 僅依賴已完成的 `POST /login/` 與 7 個 `/llm/workflow/*` 端點（create/save/get/list/update/delete/switchStatus）。回應一律包在 `ApiResponse<T> = { code:number, message:string, data:T }`，成功 `code=200`，業務錯誤 `code=400`（`message` 為後端 i18n 字串）。
- **JWT**：登入回傳的 token 在 `ApiResponse.data`（字串）。受保護端點需帶 `Authorization: Bearer <token>`。
- **DTO 欄位**（對齊後端）：
  - `WorkflowNodeDTO { nodeKey, type, name?, positionX, positionY, config }`（config 為任意 JSON 物件）
  - `WorkflowEdgeDTO { sourceNodeKey, targetNodeKey, sourceHandle?, targetHandle?, label?, condition? }`
  - `WorkflowDTO { id?, name, description?, status?, version?, nodes[], edges[], canvasMeta? }`
  - `WorkflowSaveRequestDTO { id?, version?, name, description?, nodes[], edges[], canvasMeta? }`
  - `WorkflowSummaryDTO { id?, name?, status?, version?, updatedAt? }`
  - `NodeType` = TRIGGER, LLM_ASSISTANT, TOOL, MCP_SERVER, KNOWLEDGE_RAG, CONDITION, LOOP, CODE, HTTP_REQUEST, DATA_TRANSFORM
  - `WorkflowStatus` = DRAFT, ACTIVE, INACTIVE
- **測試策略（誠實的測試設計）**：對「邏輯單元」（stores、`useWorkflowSync` 轉換、輕量 `validateGraph`、API client 對應）採嚴格 TDD；對「純呈現元件」採基本掛載/互動測試即可，不為樣式硬寫斷言。
- **指令工作目錄**：除特別說明，npm 指令都在 `bestpartner-ui/` 下執行。git 指令在 repo 根目錄。
- **commit 規範**：`<類型>(<範圍>): <主旨>`，範圍用 `bestpartner-ui`，結尾加 `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`。

---

## 里程碑總覽（6 Milestones）

| M | 名稱 | 範圍 |
|---|------|------|
| **M1** | 專案骨架 | Vite + Vue3 + TS + Element Plus + Vue Flow + Pinia + Router + Vitest，dev proxy 通，空白頁可跑 |
| **M2** | 認證 | TS 型別、axios 攔截器、authStore、LoginView、路由守衛 |
| **M3** | 列表頁 | workflow API client、workflowStore（清單）、WorkflowListView（list/create/delete/switchStatus）|
| **M4** | 畫布編輯器 | Vue Flow、自訂節點、節點面板、拖放、nodeKey 產生 |
| **M5** | Inspector + 存取 | useWorkflowSync（畫布↔DTO）、Inspector + 通用 JSON 編輯器、save/get round-trip、樂觀鎖 UX |
| **M6** | 收尾 | dirty/離頁攔截、輕量 validateGraph、測試整理 |

---

## M1：專案骨架

### Task 1：建立 Vite + Vue3 + TS 專案

**Files:**
- Create: `bestpartner-ui/`（整個專案）

**Step 1:** 在 repo 根目錄執行 scaffold（非互動）：
```bash
cd /d/projects/bestpartner
npm create vite@latest bestpartner-ui -- --template vue-ts
cd bestpartner-ui
npm install
```

**Step 2:** 安裝相依套件：
```bash
npm install vue-router@4 pinia element-plus @vue-flow/core axios nanoid
npm install -D vitest @vue/test-utils jsdom @vitejs/plugin-vue
```

**Step 3:** 確認可啟動 dev server（手動驗證後 Ctrl-C）：
```bash
npm run dev
```
Expected: Vite 於 http://localhost:5173 啟動成功。

**Step 4: Commit**
```bash
cd /d/projects/bestpartner
git add bestpartner-ui
git commit -m "新增(bestpartner-ui): 初始化 Vite + Vue3 + TS 專案"
```

---

### Task 2：設定 Vitest

**Files:**
- Modify: `bestpartner-ui/vite.config.ts`
- Create: `bestpartner-ui/src/__tests__/smoke.test.ts`
- Modify: `bestpartner-ui/package.json`（scripts 加 `"test": "vitest run"`、`"test:watch": "vitest"`）

**Step 1: 寫失敗測試** `src/__tests__/smoke.test.ts`：
```ts
import { describe, it, expect } from 'vitest'

describe('smoke', () => {
  it('1 + 1 = 2', () => {
    expect(1 + 1).toBe(2)
  })
})
```

**Step 2:** 在 `vite.config.ts` 加入 test 設定（`/// <reference types="vitest" />` 置頂，`test: { environment: 'jsdom', globals: true }`）。

**Step 3: 跑測試確認通過**
```bash
npm run test
```
Expected: 1 passed。

**Step 4: Commit**
```bash
git add bestpartner-ui/vite.config.ts bestpartner-ui/package.json bestpartner-ui/src/__tests__/smoke.test.ts
git commit -m "設定(bestpartner-ui): 設定 Vitest 測試環境"
```

---

### Task 3：設定 dev proxy 與環境變數

**Files:**
- Modify: `bestpartner-ui/vite.config.ts`
- Create: `bestpartner-ui/.env.development`

**Step 1:** `.env.development`：
```
VITE_API_BASE=
```
（留空，走 proxy 相對路徑。）

**Step 2:** `vite.config.ts` 的 `server.proxy` 加入：
```ts
server: {
  proxy: {
    '/llm': { target: 'http://localhost:80', changeOrigin: true },
    '/login': { target: 'http://localhost:80', changeOrigin: true },
  },
},
```

**Step 3:** 手動驗證（後端有起時）：`npm run dev` 後於瀏覽器 devtools 對 `/llm/workflow/list` 發請求應被轉發（非 CORS 錯誤）。若後端未起，本步驟先略過，待 M3 整合驗證。

**Step 4: Commit**
```bash
git add bestpartner-ui/vite.config.ts bestpartner-ui/.env.development
git commit -m "設定(bestpartner-ui): 設定 dev proxy 轉發後端 API"
```

---

### Task 4：掛載 Pinia / Router / Element Plus / Vue Flow

**Files:**
- Modify: `bestpartner-ui/src/main.ts`
- Create: `bestpartner-ui/src/router/index.ts`（暫放空白路由）
- Modify: `bestpartner-ui/src/App.vue`（改為 `<router-view />`）

**Step 1:** `router/index.ts` 先建立最小路由（`/` 對應一個暫時的 placeholder 元件或 `App` 內容），history 模式。

**Step 2:** `main.ts`：
```ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@vue-flow/core/dist/style.css'
import App from './App.vue'
import router from './router'

createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')
```

**Step 3:** `App.vue` 改為僅 `<template><router-view /></template>`。

**Step 4: 驗證 build 通過**
```bash
npm run build
```
Expected: 建置成功，無型別錯誤。

**Step 5: Commit**
```bash
git add bestpartner-ui/src
git commit -m "設定(bestpartner-ui): 掛載 Pinia/Router/Element Plus/Vue Flow"
```

---

## M2：認證

### Task 5：TS 型別（對應後端 DTO）

**Files:**
- Create: `bestpartner-ui/src/types/api.ts`、`bestpartner-ui/src/types/workflow.ts`

**Step 1:** `types/api.ts`：`export interface ApiResponse<T> { code: number; message: string; data: T | null }`。

**Step 2:** `types/workflow.ts`：定義 `NodeType`、`WorkflowStatus`（union 字面型別或 enum）、`WorkflowNodeDTO`、`WorkflowEdgeDTO`、`WorkflowDTO`、`WorkflowSaveRequestDTO`、`WorkflowSummaryDTO`，欄位對齊「前置須知」。`config` 型別用 `Record<string, unknown>`，`canvasMeta`/`condition` 用 `Record<string, unknown> | null`。

**Step 3:** 無執行期測試（純型別）。驗證：`npm run build` 通過。

**Step 4: Commit**
```bash
git add bestpartner-ui/src/types
git commit -m "新增(bestpartner-ui): 新增對應後端 DTO 的 TS 型別"
```

---

### Task 6：axios 實例與攔截器

**Files:**
- Create: `bestpartner-ui/src/api/http.ts`
- Test: `bestpartner-ui/src/api/__tests__/http.test.ts`

**Step 1: 寫失敗測試**：驗證 request 攔截器會在有 token 時加上 `Authorization` header。以 mock localStorage 設 token，呼叫攔截器函式（將攔截邏輯抽成可測純函式 `attachAuthHeader(config)`），斷言回傳的 `config.headers.Authorization === 'Bearer xxx'`。

**Step 2: 跑測試確認失敗**
```bash
npm run test -- http
```
Expected: FAIL（檔案/函式不存在）。

**Step 3: 實作** `api/http.ts`：建立 axios 實例（baseURL = `import.meta.env.VITE_API_BASE`）；匯出 `attachAuthHeader`（從 localStorage 取 `token`，有則加 header）並註冊為 request interceptor；response interceptor 偵測 HTTP 401/403 或 `data.code===401` → 清 token、`window.location` 導向 `/login`（導向動作以可注入的 handler 包裝，方便測試時不真的跳轉）。

**Step 4: 跑測試確認通過**
```bash
npm run test -- http
```
Expected: PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/api/http.ts bestpartner-ui/src/api/__tests__/http.test.ts
git commit -m "新增(bestpartner-ui): 新增 axios 實例與認證攔截器"
```

---

### Task 7：authStore

**Files:**
- Create: `bestpartner-ui/src/api/auth.ts`、`bestpartner-ui/src/stores/auth.ts`
- Test: `bestpartner-ui/src/stores/__tests__/auth.test.ts`

**Step 1: 寫失敗測試**（用 `setActivePinia(createPinia())`）：
- `login()` 成功時（mock `api/auth.login` 回傳 token）→ store `token`、`isAuthenticated===true`，且 `localStorage` 有寫入。
- `logout()` → 清空 token、`isAuthenticated===false`、localStorage 清除。
- `loadFromStorage()` → 能從 localStorage 還原 token。

**Step 2: 跑測試確認失敗**
```bash
npm run test -- auth
```
Expected: FAIL。

**Step 3: 實作**：`api/auth.ts` 的 `login(email, password)` 打 `POST /login/` 回傳 `data`（token）；`stores/auth.ts` 用 `defineStore` 實作上述 state/actions。

**Step 4: 跑測試確認通過**
```bash
npm run test -- auth
```
Expected: PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/api/auth.ts bestpartner-ui/src/stores/auth.ts bestpartner-ui/src/stores/__tests__/auth.test.ts
git commit -m "新增(bestpartner-ui): 新增 authStore 與登入 API"
```

---

### Task 8：LoginView 與路由守衛

**Files:**
- Create: `bestpartner-ui/src/views/LoginView.vue`、`bestpartner-ui/src/views/WorkflowListView.vue`（暫放 placeholder）
- Modify: `bestpartner-ui/src/router/index.ts`
- Test: `bestpartner-ui/src/views/__tests__/LoginView.test.ts`

**Step 1: 寫失敗測試**：掛載 `LoginView`，填入 email/password、觸發提交，驗證會呼叫 authStore.login（mock）且成功後 `router.push('/')`（mock router）。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作**：`LoginView` 用 Element Plus `el-form`（email、password、登入按鈕），提交呼叫 `authStore.login` 後導向 `/`。`router/index.ts` 定義三條路由（`/login` 公開、`/`、`/editor/:id?` 需登入，`meta.requiresAuth`）+ `beforeEach` 守衛（需登入無 token → `/login`；已登入訪問 `/login` → `/`）。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/views bestpartner-ui/src/router/index.ts bestpartner-ui/src/views/__tests__/LoginView.test.ts
git commit -m "新增(bestpartner-ui): 新增登入頁與路由守衛"
```

---

## M3：列表頁

### Task 9：workflow API client

**Files:**
- Create: `bestpartner-ui/src/api/workflow.ts`
- Test: `bestpartner-ui/src/api/__tests__/workflow.test.ts`

**Step 1: 寫失敗測試**（mock http 實例）：驗證各方法打對 URL 與 method —— `create`→POST `/llm/workflow/create`、`save`→POST `/llm/workflow/save`、`get`→POST `/llm/workflow/get`、`list`→GET `/llm/workflow/list`、`update`→POST `/llm/workflow/update`、`remove`→POST `/llm/workflow/delete`、`switchStatus`→POST `/llm/workflow/switchStatus`，且回傳 `ApiResponse.data`。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作** `api/workflow.ts`：7 個函式，輸入輸出用 `types/workflow.ts` 型別，統一回傳 `res.data.data`。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/api/workflow.ts bestpartner-ui/src/api/__tests__/workflow.test.ts
git commit -m "新增(bestpartner-ui): 新增 workflow API client"
```

---

### Task 10：workflowStore（清單部分）

**Files:**
- Create: `bestpartner-ui/src/stores/workflow.ts`
- Test: `bestpartner-ui/src/stores/__tests__/workflow.list.test.ts`

**Step 1: 寫失敗測試**：mock `api/workflow`，驗證 `fetchList()` 寫入 `summaries`；`remove(id)` 成功後從 `summaries` 移除；`switchStatus(id, active)` 後更新該筆 `status`。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作** `stores/workflow.ts` 的 state `summaries` 與 actions `fetchList/remove/switchStatus`（其餘編輯相關 state/actions M5 再加）。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/stores/workflow.ts bestpartner-ui/src/stores/__tests__/workflow.list.test.ts
git commit -m "新增(bestpartner-ui): 新增 workflowStore 清單功能"
```

---

### Task 11：WorkflowListView

**Files:**
- Modify: `bestpartner-ui/src/views/WorkflowListView.vue`
- Test: `bestpartner-ui/src/views/__tests__/WorkflowListView.test.ts`

**Step 1: 寫失敗測試**：掛載後呼叫 `fetchList`（mock store）並渲染清單列數；點「新建」導向 `/editor`；點某列「刪除」呼叫 `remove`（確認可加 Element Plus 確認框，測試時 stub）。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作**：用 Element Plus `el-table` 顯示 name/status/version/updatedAt + 操作欄（編輯→`/editor/:id`、刪除、啟用/停用）；頂部「新建 workflow」按鈕→`/editor`；右上角登出按鈕。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/views/WorkflowListView.vue bestpartner-ui/src/views/__tests__/WorkflowListView.test.ts
git commit -m "新增(bestpartner-ui): 新增 workflow 列表頁"
```

---

### Task 12：M3 端到端手動驗證

**Step 1:** 啟動後端（port 80）與 `npm run dev`，於瀏覽器登入 → 列表頁 → 新建一筆（暫時透過 store.create placeholder 或 Postman 先建資料）→ 確認清單顯示、刪除、啟用/停用可運作。
**Step 2:** 記錄任何 API 對應問題並修正（如欄位名不符）。此任務無自動測試，純整合驗證；若發現契約落差，補修 + commit。

---

## M4：畫布編輯器

### Task 13：nodeKey 產生工具

**Files:**
- Create: `bestpartner-ui/src/composables/useNodeKey.ts`
- Test: `bestpartner-ui/src/composables/__tests__/useNodeKey.test.ts`

**Step 1: 寫失敗測試**：`generateNodeKey()` 回傳長度 8 的字串；連續呼叫 1000 次皆唯一。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作**：以 `nanoid(8)` 實作 `generateNodeKey()`。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/composables/useNodeKey.ts bestpartner-ui/src/composables/__tests__/useNodeKey.test.ts
git commit -m "新增(bestpartner-ui): 新增 nodeKey 產生工具"
```

---

### Task 14：自訂節點元件 WorkflowNode

**Files:**
- Create: `bestpartner-ui/src/components/canvas/WorkflowNode.vue`、`bestpartner-ui/src/constants/nodeTypes.ts`
- Test: `bestpartner-ui/src/components/canvas/__tests__/WorkflowNode.test.ts`

**Step 1:** `constants/nodeTypes.ts`：匯出每種 `NodeType` 的顯示 meta（label、color、icon），含 `TRIGGER` 特別標示。

**Step 2: 寫失敗測試**：掛載 `WorkflowNode`（傳入 `data: { name, type }`），斷言渲染出節點名稱與型別標籤。

**Step 3: 跑測試確認失敗** → FAIL。

**Step 4: 實作**：`WorkflowNode.vue` 用 Vue Flow 的 `Handle`（輸入/輸出），依 `type` 套用 `nodeTypes.ts` 的色塊/圖示，顯示 `name`（無則顯示型別 label）。

**Step 5: 跑測試確認通過** → PASS。

**Step 6: Commit**
```bash
git add bestpartner-ui/src/components/canvas/WorkflowNode.vue bestpartner-ui/src/constants/nodeTypes.ts bestpartner-ui/src/components/canvas/__tests__/WorkflowNode.test.ts
git commit -m "新增(bestpartner-ui): 新增自訂節點元件與節點型別常數"
```

---

### Task 15：節點面板與畫布（WorkflowEditorView）

**Files:**
- Modify: `bestpartner-ui/src/views/WorkflowEditorView.vue`
- Create: `bestpartner-ui/src/components/canvas/NodePalette.vue`
- Test: `bestpartner-ui/src/components/canvas/__tests__/NodePalette.test.ts`

**Step 1: 寫失敗測試**：掛載 `NodePalette`，斷言列出 10 種 NodeType 可拖項；觸發某項的 dragstart 會設定 dataTransfer 的 nodeType。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作**：`NodePalette` 列出 10 種節點（draggable）；`WorkflowEditorView` 內嵌 `<VueFlow>`，註冊自訂節點 type，處理畫布的 drop（用 `useNodeKey` 產生 nodeKey、依放下座標建立節點）、連線（`onConnect` 加 edge）、刪除選取。左面板 = NodePalette、中央 = 畫布、右面板 = Inspector（M5，先放空容器）。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/views/WorkflowEditorView.vue bestpartner-ui/src/components/canvas/NodePalette.vue bestpartner-ui/src/components/canvas/__tests__/NodePalette.test.ts
git commit -m "新增(bestpartner-ui): 新增節點面板與 Vue Flow 畫布"
```

---

## M5：Inspector + 存取

### Task 16：useWorkflowSync（畫布 ↔ DTO 雙向轉換）

**Files:**
- Create: `bestpartner-ui/src/composables/useWorkflowSync.ts`
- Test: `bestpartner-ui/src/composables/__tests__/useWorkflowSync.test.ts`

**Step 1: 寫失敗測試**（純函式，TDD 重點）：
- `dtoToFlow(workflow)`：`WorkflowDTO` → `{ nodes, edges }`（Vue Flow 格式）。驗證 node.id===nodeKey、position 對應 positionX/Y、data 含 name/type/config；edge.source/target 對應 sourceNodeKey/targetNodeKey。
- `flowToSaveRequest(flowNodes, flowEdges, meta)`：→ `WorkflowSaveRequestDTO`。驗證反向對應正確、`config` 物件 round-trip 不失真（含巢狀）。
- round-trip：`flowToSaveRequest(dtoToFlow(wf)...)` 的 nodes/edges 與原 `wf` 等價。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作** `useWorkflowSync.ts`：匯出 `dtoToFlow`、`flowToSaveRequest` 兩個純函式。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/composables/useWorkflowSync.ts bestpartner-ui/src/composables/__tests__/useWorkflowSync.test.ts
git commit -m "新增(bestpartner-ui): 新增畫布與 DTO 雙向轉換"
```

---

### Task 17：workflowStore（編輯與存取部分）

**Files:**
- Modify: `bestpartner-ui/src/stores/workflow.ts`
- Test: `bestpartner-ui/src/stores/__tests__/workflow.editor.test.ts`

**Step 1: 寫失敗測試**：mock `api/workflow`，驗證：
- `load(id)` → 寫入當前編輯 state（id/name/version/nodes/edges）。
- `createNew()` → 清空為新建狀態（無 id、version 未定）。
- `save()` 成功 → 以回傳的新 `version` 更新 state、`dirty===false`。
- `save()` 遇 `code` 對應版本衝突 → 拋出可辨識的錯誤（供 UI 顯示對話框）。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作**：在 store 加入編輯 state（`current`：id/name/description/status/version/nodes/edges/canvasMeta、`dirty`）與 actions `load/createNew/save/setDirty`（save 用 `useWorkflowSync.flowToSaveRequest` 組請求）。版本衝突以 `message` 比對或自訂錯誤型別辨識。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/stores/workflow.ts bestpartner-ui/src/stores/__tests__/workflow.editor.test.ts
git commit -m "新增(bestpartner-ui): workflowStore 新增編輯與存取功能"
```

---

### Task 18：Inspector + 通用 JSON 編輯器

**Files:**
- Create: `bestpartner-ui/src/components/inspector/InspectorPanel.vue`、`bestpartner-ui/src/components/inspector/JsonConfigEditor.vue`
- Test: `bestpartner-ui/src/components/inspector/__tests__/JsonConfigEditor.test.ts`

**Step 1: 寫失敗測試**（JsonConfigEditor 為邏輯重點）：
- 傳入合法物件 → 顯示對應 key-value；編輯值並 emit 更新後的物件。
- raw JSON 模式輸入非法 JSON → 顯示錯誤、不 emit、回報無效（供存檔阻擋）。
- 合法 raw JSON → emit 解析後物件。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作**：`JsonConfigEditor` 提供 key-value 表格與 raw JSON 雙模式切換，`v-model` 綁定 config 物件，非法 JSON 時 emit `invalid` 狀態。`InspectorPanel`：選取節點時編輯 `name` + `JsonConfigEditor(config)`；未選取時編輯 workflow meta（name/description）。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/components/inspector bestpartner-ui/src/components/inspector/__tests__/JsonConfigEditor.test.ts
git commit -m "新增(bestpartner-ui): 新增屬性面板與通用 JSON 設定編輯器"
```

---

### Task 19：編輯器存取串接 + 樂觀鎖 UX

**Files:**
- Modify: `bestpartner-ui/src/views/WorkflowEditorView.vue`
- Test: `bestpartner-ui/src/views/__tests__/WorkflowEditorView.test.ts`

**Step 1: 寫失敗測試**：
- 進入帶 `:id` 路由 → 呼叫 `store.load(id)`（mock）並把節點渲染到畫布。
- 點工具列「存檔」→ 呼叫 `store.save`（mock）成功後顯示成功提示。
- `store.save` 拋版本衝突 → 顯示「重新載入」對話框，點擊後呼叫 `store.load`。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作**：`WorkflowEditorView` 串接 store（載入→`dtoToFlow` 回填畫布；存檔→收集畫布→`store.save`）；工具列含存檔、目前 status/version；版本衝突彈 Element Plus `ElMessageBox` 提供重新載入。Inspector 綁定選取節點。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/views/WorkflowEditorView.vue bestpartner-ui/src/views/__tests__/WorkflowEditorView.test.ts
git commit -m "新增(bestpartner-ui): 編輯器串接存取與樂觀鎖處理"
```

---

## M6：收尾

### Task 20：輕量 validateGraph

**Files:**
- Create: `bestpartner-ui/src/composables/useGraphValidation.ts`
- Test: `bestpartner-ui/src/composables/__tests__/useGraphValidation.test.ts`

**Step 1: 寫失敗測試**（對齊後端規則）：
- nodeKey 重複 → 回報錯誤含重複 key。
- edge 端點不存在 → 回報含缺失 key。
- 有向環 → 回報含環。
- 合法圖 → 無錯誤。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作**：`validateGraph(nodes, edges)` 回傳錯誤清單（nodeKey 唯一、edge 端點存在、Kahn 環偵測），與後端 `validateGraph` 同義。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/composables/useGraphValidation.ts bestpartner-ui/src/composables/__tests__/useGraphValidation.test.ts
git commit -m "新增(bestpartner-ui): 新增前端輕量畫布驗證"
```

---

### Task 21：dirty 追蹤與離頁攔截 + 接上輕量驗證

**Files:**
- Modify: `bestpartner-ui/src/views/WorkflowEditorView.vue`
- Test: `bestpartner-ui/src/views/__tests__/WorkflowEditorView.guard.test.ts`

**Step 1: 寫失敗測試**：
- 畫布變更 → `store.dirty===true`。
- dirty 為 true 時 `beforeRouteLeave` 觸發攔截（mock 確認框）。
- 存檔前若 `validateGraph` 有錯 → 不呼叫 `store.save`、顯示錯誤提示。

**Step 2: 跑測試確認失敗** → FAIL。

**Step 3: 實作**：畫布任何變更呼叫 `store.setDirty(true)`；`onBeforeRouteLeave` + `window.beforeunload` 在 dirty 時提示；存檔前先跑 `useGraphValidation`，有錯則擋下並標記/提示。

**Step 4: 跑測試確認通過** → PASS。

**Step 5: Commit**
```bash
git add bestpartner-ui/src/views/WorkflowEditorView.vue bestpartner-ui/src/views/__tests__/WorkflowEditorView.guard.test.ts
git commit -m "新增(bestpartner-ui): 新增未存攔截與存檔前驗證"
```

---

### Task 22：全測試回歸 + build + 文件同步

**Step 1:** 跑全部測試與建置：
```bash
cd /d/projects/bestpartner/bestpartner-ui
npm run test
npm run build
```
Expected: 全綠、build 成功。

**Step 2:** 呼叫 `documentation-sync` skill，同步：
- `README.md`（目錄結構新增 `bestpartner-ui/`、功能/啟動章節）
- `docs-site/`（如新增前端使用說明頁，選配）
- `.claude/rules/architecture-and-packages.md`（模組結構新增前端模組）
- `CLAUDE.md` / `AGENTS.md`（如有模組導覽）

**Step 3: Commit**（文件變更）
```bash
cd /d/projects/bestpartner
git add README.md docs-site .claude AGENTS.md
git commit -m "文件(bestpartner-ui): 同步前端 v1 模組文件"
```

---

## v1 完成定義（DoD）

- [ ] `bestpartner-ui/` 專案可 `npm run dev` / `npm run build` / `npm run test` 全綠
- [ ] 登入 → 列表（list/create/delete/switchStatus）→ 編輯器（畫布拉節點/連線、Inspector 編 config）→ 存檔 → 重載回顯，主線可手動跑通（後端 port 80）
- [ ] 樂觀鎖版本衝突有對話框、dirty 離頁攔截、存檔前輕量驗證
- [ ] 邏輯單元（authStore、workflowStore、useWorkflowSync、JsonConfigEditor、useGraphValidation、API client）皆有測試覆蓋
- [ ] `documentation-sync` 已執行
