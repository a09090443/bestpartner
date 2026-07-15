# 前端規範（bestpartner-ui）

> 路徑：`bestpartner-ui/`（Vue 3 + Vite + TypeScript）。本檔描述**現況慣例**，非強制規範宣言；改動前端程式碼時比照既有寫法即可。

## 技術棧

| 類別 | 技術 | 版本 |
|------|------|------|
| 框架 | Vue | ^3.5.38 |
| 建置工具 | Vite | ^8.1.0（`@vitejs/plugin-vue` ^6.0.7） |
| 語言 | TypeScript | ~6.0.2（`vue-tsc` ^3.3.5） |
| 狀態管理 | Pinia | ^3.0.4 |
| 路由 | vue-router | ^4.6.4 |
| UI 元件庫 | Element Plus | ^2.14.2 |
| Workflow 畫布 | `@vue-flow/core` ^1.48.2（+ background/controls/minimap） |
| 自動排版 | `@dagrejs/dagre` ^3.0.0 |
| HTTP client | axios | ^1.18.1（唯一 http client） |
| ID 產生 | nanoid | ^5.1.16 |
| 單元測試 | Vitest ^4.1.9 + `@vue/test-utils` ^2.4.11 + jsdom |
| E2E 工具 | `@playwright/test` ^1.61.1 |

> ⚠️ **無 eslint / prettier**：package.json 無對應 devDependency 與 script，唯一的靜態檢查是 `npm run build` 內的 `vue-tsc -b`（型別檢查）與 `tsconfig.app.json` 的 `noUnusedLocals`/`noUnusedParameters`/`noFallthroughCasesInSwitch`。修改程式碼時比照既有排版與命名手動維持一致，不要假設有自動 format 工具。

> ⚠️ **`bestpartner-ui/e2e/` 這套 Playwright harness 不是正式 E2E 執行入口**，依 `.claude/skills/e2e-test-confirmation/SKILL.md` 已降級為「選擇器/旅程參考」，真正執行走 `e2e-test-confirmation` skill（`webwright`）。不要把 `npx playwright test` 當成正式測試流程來執行或回報結果。

## 目錄結構與命名慣例

```
src/
├── api/            # 每個後端模組一個檔案（llmSetting.ts、mcpServer.ts、workflow.ts ...）+ http.ts（axios 實例）
├── components/
│   ├── canvas/     # Vue Flow 畫布元件
│   └── inspector/  # 節點屬性面板 + inspector/forms/（各 NodeType 對應的設定表單）
├── composables/    # useXxx.ts（Composition API 邏輯抽取）
├── constants/      # nodeTypes.ts、handles.ts
├── router/
├── stores/         # Pinia store，短檔名（auth.ts、workflow.ts、execution.ts）
├── styles/         # workflow-theme.css（純 CSS，無 SCSS/Less）
├── types/          # 跨模組共用型別（workflow.ts、api.ts、options.ts、toolSchema.ts）
├── utils/
└── views/          # XxxView.vue
```

| 項目 | 命名規則 | 範例 |
|------|---------|------|
| `.vue` 檔 | PascalCase | `WorkflowNode.vue`、`ExecutionResultDrawer.vue` |
| `views/` 頁面元件 | PascalCase + `View` 結尾 | `LoginView.vue`、`WorkflowEditorView.vue` |
| composable | camelCase，`useXxx.ts` | `useCanvasLayout.ts`、`useGraphValidation.ts` |
| Pinia store | 檔名短 camelCase（不含 `Store`），導出函式 `useXxxStore` | `stores/workflow.ts` → `useWorkflowStore` |
| `api/`、`constants/`、`types/`、`utils/` | camelCase | `llmSetting.ts`、`nodeRequiredFields.ts` |
| 測試檔 | 緊鄰原始檔的同層 `__tests__/` 子目錄，副檔名 `.test.ts` | `src/composables/__tests__/useCanvasLayout.test.ts` |

> **已知漂移基線**：極少數測試檔用 `.spec.ts`（`ExecutionResultDrawer.spec.ts`、`OutputForm.spec.ts`、`execution.spec.ts`），新增測試一律用 `.test.ts`，不追溯修改既有檔名。

## 元件撰寫慣例

- 一律 `<script setup lang="ts">`，無 Options API 元件。
- `defineProps`/`defineEmits` 用**純型別泛型宣告**，不用 runtime 驗證、不用 `withDefaults`：
  ```ts
  const props = defineProps<{ id: string; data: NodeData }>()
  defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()
  ```
- `inspector/forms/` 底下各節點設定表單共用同一 prop/emit 契約：`config: Record<string, unknown>` in、`update:config` out。例外是 `SettingSchemaForm.vue`（通用 schema-driven 表單，非節點設定表單本身），走標準 `modelValue`/`update:modelValue` v-model 慣例。
- 只在單一元件內使用的型別直接 inline 定義在該 `.vue` 的 `<script setup>` 內，不額外抽檔；跨模組共用型別才放 `src/types/`。
- `data-test` 屬性是 e2e 測試選擇器的硬性慣例（全 repo 約 70 處），新增可互動節點/元件時比照加上，e2e 選擇器變更需同步 `docs/e2e-test-plan.md`（見 [`documentation-update-policy.md`](documentation-update-policy.md)）。

## API 呼叫慣例

- `src/api/http.ts` 是唯一的 axios 實例：
  - Request interceptor 從 `localStorage.getItem('token')` 附加 `Authorization: Bearer <token>`
  - Response interceptor 偵測 401/403（含後端 `ApiResponse.code === 401`）觸發登出導頁，可用 `setRedirectHandler` 注入測試替身
  - 匯出 `extractApiMessage(err)` 統一取得後端錯誤訊息
- 每支 API 函式對回應做 `unwrap<T>(res)` 取出 `ApiResponse<T>.data`（型別定義於 `src/types/api.ts`）
- 部分查詢類 API 有 module-scope Promise 快取去重（如 `getNodeRequiredFields()`），登出時須呼叫對應的 `invalidateXxxCache()` 清除

## 狀態管理慣例

- 全部用 setup 風格 `defineStore('xxx', () => {...})`，無 Options 風格 store。
- `ref()` 當 state、`computed()` 當 getter、一般 function 當 action，函式最後 `return { ... }` 顯式列出要導出的成員。
- 特殊情境用自訂 Error 類別讓 UI 判斷（如 `WorkflowVersionConflictError` 供樂觀鎖版本衝突處理）。

## Workflow / Canvas 慣例

- `NodeType` 聯合型別定義於 `src/types/workflow.ts`，對應後端 `WorkflowDTO`（12 種）。
- 節點顯示 meta（label/color/icon/category/inputs/outputs）集中在 `src/constants/nodeTypes.ts`（`NODE_TYPE_METAS`、`getNodeTypeMeta()`）。
- 連接點（handle）編碼規則獨立在 `src/constants/handles.ts`：`role:port` 字串（如 `in:main`、`out:true`），用 `parseHandle()` 解析。
- DTO ↔ 畫布互轉在 `src/composables/useWorkflowSync.ts`，畫面驗證在 `useGraphValidation.ts`。

## 樣式慣例

- 無 SCSS/Less/Tailwind，純 CSS，`<style scoped>` 為主，搭配 CSS 自訂變數（`--wf-*`）與 fallback 值。
- `src/styles/workflow-theme.css` 是 workflow 編輯器的深色主題，變數作用域限定在 `.wf-editor` class 內。

## 常數與訊息管理

- **無 i18n 機制**（無 `vue-i18n` 或同類套件），這是從未建立過的機制，非漂移。使用者提示訊息（成功/失敗、預設名稱）直接就地寫死中文字串於元件/store 內，與後端強制的 [`i18n-messages.md`](i18n-messages.md) `AppMessage` 機制形成反差，屬於已知現況。
- 前端偶爾需比對後端回傳的中/英錯誤訊息字串（如 `stores/workflow.ts` 的樂觀鎖衝突判斷），屬於字串耦合，修改後端對應訊息時需留意是否影響前端比對邏輯。
- `src/constants/` 只放 `nodeTypes.ts`、`handles.ts` 這類畫布/節點相關常數，非通用訊息管理。
