---
sidebar_position: 5
description: bestpartner-ui 前端架構說明，涵蓋技術棧、專案結構、開發啟動方式、核心設計慣例與測試策略。
keywords: [前端, bestpartner-ui, Vue 3, Pinia, Vue Flow, Vite, 前端架構]
---

# 前端架構（bestpartner-ui）

`bestpartner-ui` 是 BestPartner 的前端應用，提供登入、Workflow 列表與 n8n-like 視覺化 Workflow 編輯器。本頁說明技術棧、專案結構與開發慣例；Workflow 編輯器本身的功能細節見 [視覺化 Workflow](../features/workflow.md)，各模組職責總覽見 [模組說明](./modules.md#bestpartner-ui-前端模組)。

## 技術棧

| 類別 | 技術 |
|------|------|
| 框架 | Vue 3（Composition API，全面採用 `<script setup lang="ts">`） |
| 建置工具 | Vite |
| 語言 | TypeScript |
| 狀態管理 | Pinia（setup 風格 store） |
| 路由 | Vue Router |
| UI 元件庫 | Element Plus |
| Workflow 畫布 | Vue Flow（`@vue-flow/core` + background／controls／minimap） |
| 自動排版 | `@dagrejs/dagre` |
| HTTP client | axios（唯一 http client，統一封裝於 `src/api/http.ts`） |
| 單元測試 | Vitest + `@vue/test-utils` + jsdom |

## 專案結構

```
bestpartner-ui/src/
├── api/            # 後端 API client，每個後端模組一個檔案（llmSetting.ts、workflow.ts ...）+ http.ts（axios 實例、JWT 攔截器）
├── components/
│   ├── canvas/     # Vue Flow 節點面板與自訂節點
│   ├── common/     # 跨區塊共用小元件（NodeIcon：節點型別的 inline SVG 圖示）
│   ├── inspector/  # 屬性面板 + inspector/forms/（各節點型別的設定表單）
│   └── nodeDesigner/ # 全屏節點編輯頁（輸入 / 參數・設定・說明 / 輸出 三欄）
├── composables/    # useXxx.ts：畫布↔DTO 轉換、驗證、下拉選項快取、自動排版、主題切換、節點設定同步
├── constants/      # 節點型別顯示 meta、handle 編碼工具、節點圖示與說明、畫布尺寸常數
├── router/         # 路由與登入守衛
├── stores/         # Pinia store（auth、workflow、execution）
├── styles/         # 編輯器主題 token 與共用表單樣式（純 CSS）
├── types/          # 對應後端 DTO 的 TypeScript 型別
└── views/          # 登入頁、Workflow 列表頁、Workflow 編輯器
```

## 開發啟動

```bash
cd bestpartner-ui
npm install
npm run dev      # 開發伺服器，預設 http://localhost:5173
npm run build    # vue-tsc 型別檢查 + production 建置
npm run test     # Vitest 單元測試
```

開發伺服器透過 Vite proxy（`vite.config.ts`）將 `/llm/**` 與 `/login/`（僅帶尾斜線的登入 API）轉發至後端（預設 `http://localhost:80`），裸路徑 `/login` 保留給 SPA 前端路由，避免與後端登入 API 衝突。

## 正式部署（nginx 容器） {#production-nginx}

SIT 等容器環境不使用 Vite 開發伺服器，而是以 `bestpartner-ui/Dockerfile` 多階段建置前端 image：

1. **建置階段**（`node:22-alpine`）：依 `package-lock.json` 執行 `npm ci`，再 `npm run build` 產生 `dist/`。
2. **執行階段**（`nginx:1.27-alpine`）：只複製 `dist/` 與 `nginx/default.conf`，由 nginx 提供靜態檔並反向代理後端。

`bestpartner-ui/.dockerignore` 排除 `node_modules`、`dist`、`.env`、`.env.*` 與 E2E／測試產物，前端 image 不含任何環境檔。前端程式以同源相對路徑呼叫 API（`VITE_API_BASE` 為空），因此不需修改程式碼即可在容器中運作。

`nginx/default.conf` 的代理對照（後端 upstream 為 compose 服務名 `backend:80`）：

| 路徑 | 處理 | 說明 |
|------|------|------|
| `/llm/` | 代理後端 | 全部 `/llm` 系列 API；關閉 `proxy_buffering`、讀寫逾時 3600s，讓 SSE 串流（`/llm/workflow/execute`、`/llm/chatStreaming`）不被緩衝或中斷 |
| `/login/` | 代理後端 | 只限帶結尾斜線的登入 API 及其子路徑 |
| `/login`（裸路徑） | SPA | `location = /login` 精確匹配回 `index.html`。不可省略：只有帶尾斜線且含 `proxy_pass` 的 `/login/` 時，nginx 會把裸 `/login` 自動 301 轉址到 `/login/`，登入頁直接開啟或重新整理就會進後端代理 |
| `/view/` | 代理後端 | 存活探測 `/view/chat` 經前端 port 80 仍可到達 |
| `/systemSetting/` | 代理後端 | 系統設定 API |
| 其他路徑 | SPA | `try_files $uri $uri/ /index.html`，`/editor/:id` 等深層連結重新整理不會 404 |

- 上傳大小上限 `client_max_body_size 12m`，不低於後端 Quarkus 預設的 10M，避免上傳端點先被 nginx 以 413 擋下。
- 存取日誌使用自訂格式，只記錄來源、時間、方法、路徑、狀態碼與耗時，**不記錄 Authorization 標頭、cookie 與 request body**。
- 前端容器不接收任何後端機密；`.env.sit` 只注入後端容器。部署步驟見 [安裝與設定](../getting-started/installation.md#sit-compose-deploy)。

## 核心設計慣例

- **全面 Composition API**：所有元件使用 `<script setup lang="ts">`，`defineProps`/`defineEmits` 採純型別泛型宣告，不做 runtime 驗證。
- **API 層集中管理**：`src/api/http.ts` 是唯一的 axios 實例，request 攔截器附加 JWT（`localStorage` 讀取），response 攔截器偵測 401/403 並導向登入頁；各 API 函式回傳前先 `unwrap<T>()` 取出 `ApiResponse<T>.data`。**登入端點自身的 401 為例外**——帳密錯誤時後端回的就是 401，若一併觸發登出重導會整頁重載、把登入頁剛顯示的錯誤訊息沖掉，因此攔截器以 `isLoginRequest(url)` 排除，改由 `api/auth.ts` 取出後端訊息（如「密碼錯誤」）往上拋給畫面顯示。
- **狀態管理**：Pinia store 一律 setup 風格（`defineStore('xxx', () => {...})`），以 `ref`/`computed`/一般 function 分別對應 state／getter／action。
- **型別化節點設定表單**：Workflow 編輯器的 Inspector 依節點型別分派結構化表單，取代裸 JSON 編輯，必填欄位驗證與後端 `NodeConfig` 契約同步（見 [視覺化 Workflow](../features/workflow.md#型別化節點設定表單)）。Inspector 與全屏節點編輯頁共用同一份分派表但各自建立實例，因此表單一律以 `composables/useConfigSync.ts` 跟隨節點設定的外部變更——在其中一處改設定，另一處會即時反映。
- **雙主題**：編輯器提供淺色（預設）／深色兩套主題，工具列可切換、偏好存於 `localStorage`。主題 token 集中在 `src/styles/workflow-theme.css`，作用域限定 `.wf-editor`——淺色為 base、深色以 `[data-wf-theme='dark']` 覆寫，故登入頁與列表頁不受影響。Element Plus 深色沿用官方 `theme-chalk/dark/css-vars.css`（`html.dark`），以涵蓋 teleport 到 `body` 的訊息元件。
- **無 i18n 套件**：目前使用者提示訊息以中文字串直接寫在元件／store 中，尚未導入類似後端 `AppMessage` 的訊息管理機制。

## 測試策略

- **單元測試**：Vitest + `@vue/test-utils`，測試檔緊鄰原始檔（同層 `__tests__/` 子目錄）。
- **E2E**：`bestpartner-ui/e2e/` 下的 Playwright 程式碼目前僅作為**選擇器與操作旅程的參考**，不是正式執行入口；實際 E2E 測試流程須透過 `e2e-test-confirmation` skill 執行，以確保測試環境重啟、資料清理等前置作業一致。
