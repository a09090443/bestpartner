# BestPartner 前端（bestpartner-ui）

BestPartner 平台的前端應用，以 **Vue 3（`<script setup>`）+ Vite + TypeScript** 打造，核心為 n8n-like 視覺化 Workflow 編輯器（Vue Flow）。狀態管理用 Pinia，UI 元件用 Element Plus。

## 開發指令

```bash
npm install        # 安裝依賴
npm run dev        # 啟動開發伺服器（http://localhost:5173）
npm run test       # 執行 vitest 單元測試
npm run build      # vue-tsc 型別檢查 + production build
```

## 後端連線與 proxy 慣例

開發伺服器透過 Vite proxy 將 API 請求轉發到本機後端（Quarkus，預設 `http://localhost:80`）。設定於 `vite.config.ts` 的 `server.proxy`。

**慣例：proxy key 只對應「實際的後端 API 路徑」，不可攔截與 SPA 前端路由同名的裸路徑。**

- 新增後端模組時，把該模組的路徑前綴（如 `/llm`）加入 proxy。
- 當某後端路徑與**前端路由**同名時，proxy key 要用**精準的 API 路徑**（通常帶結尾斜線），而非裸前綴。

  典型案例：後端登入 API 是 `POST /login/`，但前端也有 SPA 路由 `/login`（登入頁）。若 proxy key 寫成裸 `/login`，會把 SPA 路由一起攔截轉發到後端；當 401 攔截器以 `window.location.href = '/login'` 硬導覽時，就會被轉發到後端而回 **405 Method not allowed**，使用者看不到登入頁。因此 proxy key 必須收斂為 `/login/`：

  ```ts
  server: {
    proxy: {
      '/llm': { target: 'http://localhost:80', changeOrigin: true },
      // 僅代理登入 API（POST /login/），裸 `/login` 留給 SPA 路由
      '/login/': { target: 'http://localhost:80', changeOrigin: true },
    },
  }
  ```

> `VITE_API_BASE`（`.env.development`）預設為空，axios 走相對路徑交由上述 proxy 處理。若要直連遠端後端，可設定此變數。

## 認證與路由

- 登入成功後 JWT 存於 `localStorage`（key：`token`），由 axios request interceptor 自動附加 `Authorization: Bearer`。
- API 回應 HTTP 401/403 或後端 `code === 401` 時，interceptor 會清除 token 並硬導覽回 `/login`（見上方 proxy 慣例）。
- 路由守衛（`router/index.ts`）攔截需登入頁面：無 token 導向 `/login`，已登入訪問 `/login` 導回首頁。

## 進一步文件

- 功能與編輯器說明：`docs-site/docs/features/workflow.md`
- 模組結構：`docs-site/docs/architecture/modules.md`
