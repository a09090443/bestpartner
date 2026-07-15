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
│   └── inspector/  # 屬性面板 + inspector/forms/（各節點型別的設定表單）
├── composables/    # useXxx.ts：畫布↔DTO 轉換、驗證、下拉選項快取、自動排版
├── constants/      # 節點型別顯示 meta、handle 編碼工具
├── router/         # 路由與登入守衛
├── stores/         # Pinia store（auth、workflow、execution）
├── styles/         # 編輯器深色主題（workflow-theme.css）
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

## 核心設計慣例

- **全面 Composition API**：所有元件使用 `<script setup lang="ts">`，`defineProps`/`defineEmits` 採純型別泛型宣告，不做 runtime 驗證。
- **API 層集中管理**：`src/api/http.ts` 是唯一的 axios 實例，request 攔截器附加 JWT（`localStorage` 讀取），response 攔截器偵測 401/403 並導向登入頁；各 API 函式回傳前先 `unwrap<T>()` 取出 `ApiResponse<T>.data`。
- **狀態管理**：Pinia store 一律 setup 風格（`defineStore('xxx', () => {...})`），以 `ref`/`computed`/一般 function 分別對應 state／getter／action。
- **型別化節點設定表單**：Workflow 編輯器的 Inspector 依節點型別分派結構化表單，取代裸 JSON 編輯，必填欄位驗證與後端 `NodeConfig` 契約同步（見 [視覺化 Workflow](../features/workflow.md#型別化節點設定表單)）。
- **無 i18n 套件**：目前使用者提示訊息以中文字串直接寫在元件／store 中，尚未導入類似後端 `AppMessage` 的訊息管理機制。

## 測試策略

- **單元測試**：Vitest + `@vue/test-utils`，測試檔緊鄰原始檔（同層 `__tests__/` 子目錄）。
- **E2E**：`bestpartner-ui/e2e/` 下的 Playwright 程式碼目前僅作為**選擇器與操作旅程的參考**，不是正式執行入口；實際 E2E 測試流程須透過 `e2e-test-confirmation` skill 執行，以確保測試環境重啟、資料清理等前置作業一致。
