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
│   ├── canvas/     # Vue Flow 畫布元件（+ dragKeys.ts、designerInjection.ts 兩個非元件的鍵定義）
│   ├── common/     # 跨區塊共用的無狀態小元件（NodeIcon.vue）
│   ├── inspector/  # 節點屬性面板 + inspector/forms/（各 NodeType 對應的設定表單）+ typedForms.ts（表單分派表）
│   └── nodeDesigner/ # 全屏節點編輯頁（NodeDesignerModal + Input / Params / Output / OutputPreview 四個面板）
├── composables/    # useXxx.ts（Composition API 邏輯抽取）；useConfigSync.ts 供型別化表單跟隨 props.config
├── constants/      # nodeTypes.ts、handles.ts、nodeIcons.ts、nodeDocs.ts、canvas.ts
├── router/
├── stores/         # Pinia store，短檔名（auth.ts、workflow.ts、execution.ts）
├── styles/         # 純 CSS（無 SCSS/Less）：workflow-theme.css、wf-form.css、element-plus-wf.css
├── types/          # 跨模組共用型別（workflow.ts、api.ts、options.ts、toolSchema.ts、nodeDesigner.ts）
├── utils/
└── views/          # XxxView.vue
```

> `inspector/typedForms.ts` 是 NodeType → 設定表單的**唯一**分派表，由 InspectorPanel 與
> Node Designer 的 Parameters 面板共用；新增型別化表單只改這一處。

| 項目 | 命名規則 | 範例 |
|------|---------|------|
| `.vue` 檔 | PascalCase | `WorkflowNode.vue`、`ExecutionResultDrawer.vue` |
| `views/` 頁面元件 | PascalCase + `View` 結尾 | `LoginView.vue`、`WorkflowEditorView.vue` |
| composable | camelCase，`useXxx.ts` | `useCanvasLayout.ts`、`useGraphValidation.ts` |
| Pinia store | 檔名短 camelCase（不含 `Store`），導出函式 `useXxxStore` | `stores/workflow.ts` → `useWorkflowStore` |
| `api/`、`constants/`、`types/`、`utils/` | camelCase | `llmSetting.ts`、`nodeRequiredFields.ts` |
| 測試檔 | 緊鄰原始檔的同層 `__tests__/` 子目錄，副檔名 `.test.ts` | `src/composables/__tests__/useCanvasLayout.test.ts` |

> **已知漂移基線**：極少數測試檔用 `.spec.ts`（`ExecutionResultDrawer.spec.ts`、`OutputForm.spec.ts`、`execution.spec.ts`），新增測試一律用 `.test.ts`，不追溯修改既有檔名。此三檔已登錄於 `scripts/harness-drift-scan.ps1` 的 `$Baseline.SpecTsBaseline`；新增其他 `.spec.ts` 會被掃描標為 `NEW`。
>
> **vitest 執行範圍**：`vitest.config.ts` 以 `include: ['src/**/*.{test,spec}.ts']` 限定只跑 `src/` 下的單元測試，並排除 `e2e/`；`bestpartner-ui/e2e/` 的 Playwright spec 不由 vitest 收集（避免誤跑 Playwright API 而失敗）。CI（`harness.yml` 的 frontend job）跑 `npm run build`（`vue-tsc` 型別檢查）＋ `npm run test`（vitest）。

## 元件撰寫慣例

- 一律 `<script setup lang="ts">`，無 Options API 元件。
- `defineProps`/`defineEmits` 用**純型別泛型宣告**，不用 runtime 驗證、不用 `withDefaults`：
  ```ts
  const props = defineProps<{ id: string; data: NodeData }>()
  defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()
  ```
- `inspector/forms/` 底下各節點設定表單共用同一 prop/emit 契約：`config: Record<string, unknown>` in、`update:config` out。例外是 `SettingSchemaForm.vue`（通用 schema-driven 表單，非節點設定表單本身），走標準 `modelValue`/`update:modelValue` v-model 慣例。
- **各表單把 config 攤成本地 `ref` 編輯，因此必須接 `composables/useConfigSync.ts` 才會跟隨外部變更**：
  Inspector 與 Node Designer 的 Parameters 面板共用同一份 `typedForms.ts` 分派表，但**各自 mount 一個實例**；
  A 實例 emit `update:config` 更新了 `node.data.config` 後，B 實例的本地 `ref` 不會自己變。
  寫法為 `const { markSelfEmit } = useConfigSync(() => props.config, cfg => { /* 灌回本地 ref */ })`，
  並在自己的 `emitConfig()` 送出前呼叫 `markSelfEmit(next)`。
  ⚠️ **自身 emit 的回流必須略過**（`useConfigSync` 內以 `toRaw()` 做物件參考比對），否則使用者每打一個字
  都會被自己的值重灌——中文組字期間會吃字、游標會跳。新增型別化表單時一併接上，`SettingSchemaForm` 不在此列。
- 只在單一元件內使用的型別直接 inline 定義在該 `.vue` 的 `<script setup>` 內，不額外抽檔；跨模組共用型別才放 `src/types/`。
- `data-test` 屬性是 e2e 測試選擇器的硬性慣例（全 repo 約 70 處），新增可互動節點/元件時比照加上，e2e 選擇器變更需同步 `docs/e2e-test-plan.md`（見 [`documentation-update-policy.md`](documentation-update-policy.md)）。

## API 呼叫慣例

- `src/api/http.ts` 是唯一的 axios 實例：
  - Request interceptor 從 `localStorage.getItem('token')` 附加 `Authorization: Bearer <token>`
  - Response interceptor 偵測 401/403（含後端 `ApiResponse.code === 401`）觸發登出導頁，可用 `setRedirectHandler` 注入測試替身
  - ⚠️ **登入端點自身的 401 是例外**：以 `isLoginRequest(url)` 排除，不觸發全域登出重導。
    帳密錯誤時後端回的就是 401，若一併走登出流程，預設 handler 的 `window.location.href` 會整頁重載，
    把 `LoginView` 剛拋出的 `ElMessage.error` 沖掉，使用者只看到欄位被清空、看不到「密碼錯誤」
    （E2E 週期 202608192201 的 J1-04 實測到 3 次硬導航）。對應地，`api/auth.ts` 的 `login()` 需自行
    catch 並以 `extractApiMessage(err)` 改拋帶後端訊息的 Error，否則畫面會顯示 axios 的通用字串。
  - 匯出 `extractApiMessage(err)` 統一取得後端錯誤訊息
- 每支 API 函式對回應做 `unwrap<T>(res)` 取出 `ApiResponse<T>.data`（型別定義於 `src/types/api.ts`）
- 部分查詢類 API 有 module-scope Promise 快取去重（如 `getNodeRequiredFields()`），登出時須呼叫對應的 `invalidateXxxCache()` 清除

## 狀態管理慣例

- 全部用 setup 風格 `defineStore('xxx', () => {...})`，無 Options 風格 store。
- `ref()` 當 state、`computed()` 當 getter、一般 function 當 action，函式最後 `return { ... }` 顯式列出要導出的成員。
- 特殊情境用自訂 Error 類別讓 UI 判斷（如 `WorkflowVersionConflictError` 供樂觀鎖版本衝突處理）。

## Workflow / Canvas 慣例

- `NodeType` 聯合型別定義於 `src/types/workflow.ts`，對應後端 `WorkflowDTO`（13 種）。
- 節點顯示 meta（label/color/category/inputs/outputs）集中在 `src/constants/nodeTypes.ts`（`NODE_TYPE_METAS`、`getNodeTypeMeta()`）。
- 連接點（handle）編碼規則獨立在 `src/constants/handles.ts`：`role:port` 字串（如 `in:main`、`out:true`），用 `parseHandle()` 解析。
- DTO ↔ 畫布互轉在 `src/composables/useWorkflowSync.ts`，畫面驗證在 `useGraphValidation.ts`。
- **節點圖示為 inline SVG**：path 資料在 `src/constants/nodeIcons.ts`，渲染元件為 `components/common/NodeIcon.vue`（無 `<style>`、`stroke="currentColor"`，顏色由父層 `color` 繼承）。
- **節點的預設 config 放 `NodeTypeMeta.defaultConfig`**（`nodeTypes.ts`），由 `WorkflowEditorView` 的 `onDrop` 展開複製後帶入新節點。只在「該欄位必填、且目前只有唯一合法值」時才給（現況只有 `TRIGGER` 的 `triggerType: 'MANUAL'`）。⚠️ 取用務必 `{ ...meta.defaultConfig }`，直接指派會讓同型別的多個節點共用同一個物件。
- **節點卡尺寸是三處共用的常數**：`src/constants/canvas.ts` 的 `NODE_BOX`（214×76）與 `NODE_SLOT_WIDTH`，被 `WorkflowNode.vue` 的 CSS、`useCanvasLayout.ts`（dagre 排版）與 `useDefaultZoom.ts`（預設視野）同時依賴——**改尺寸時三處必須一起改**，否則整理版面會重疊、預設縮放會失準。
- **`Record<NodeType, T>` 是完整性強制手段**：`nodeIcons.ts` 與 `nodeDocs.ts` 皆以此宣告，新增 NodeType 而未補圖示／說明時 `vue-tsc` 會直接紅燈。新增同類對照表時比照辦理。
- 節點卡片是由 Vue Flow 內部渲染的，拿不到父層 listener；需要回呼父層時走 provide/inject（見 `components/canvas/designerInjection.ts`），且 `inject` **一律給 no-op 預設值**，避免所有單獨 mount 節點的測試都要補 provide。

## 樣式慣例

- 無 SCSS/Less/Tailwind，純 CSS，`<style scoped>` 為主，搭配 CSS 自訂變數（`--wf-*`）。
- **不寫 hex fallback**（`var(--wf-text, #e7e7ec)` 是禁忌）：fallback 值一定是某一套主題的顏色，在另一套主題下就是錯的；token 都定義在 `.wf-editor` 上不可能落空。
- `src/styles/workflow-theme.css` 是主題 token 的單一事實來源，作用域限定 `.wf-editor`：**淺色為 base、深色以 `[data-wf-theme='dark']` 覆寫**，切換由 `composables/useEditorTheme.ts` 掛屬性。
- `src/styles/wf-form.css`（由 workflow-theme.css `@import`）收斂 `.form` / `.field` / `.text-input` 的共用樣式，規則一律以 `.wf-editor ` 前綴命名空間；各表單的 `<style scoped>` 只留自己獨有的規則。
- `src/styles/element-plus-wf.css` 由 `main.ts` 在 EP 樣式**之後** import，只做少量對齊覆寫。EP 深色走官方 `theme-chalk/dark/css-vars.css`（選擇器 `html.dark`），因為 `ElMessage` / `ElMessageBox` 會 teleport 到 `document.body`、在 `.wf-editor` 作用域外。
- **主題相依的衍生色用 `color-mix()` 就地算**，不要在 JS 端算好再下放——JS 拿不到當下主題。JS 只下放型別色本身（`--node-color`）。
- 全屏 modal（Node Designer）**刻意不用 `<Teleport>`**：`--wf-*` token 與 `wf-form.css` 的 `.wf-editor ` 前綴都綁在 `.wf-editor` 上，teleport 到 body 兩者會同時失效。改以 `.wf-editor` 子節點 + `position: fixed` 覆蓋全屏。

## 常數與訊息管理

- **無 i18n 機制**（無 `vue-i18n` 或同類套件），這是從未建立過的機制，非漂移。使用者提示訊息（成功/失敗、預設名稱）直接就地寫死中文字串於元件/store 內，與後端強制的 [`i18n-messages.md`](i18n-messages.md) `AppMessage` 機制形成反差，屬於已知現況。
- 前端偶爾需比對後端回傳的中/英錯誤訊息字串（如 `stores/workflow.ts` 的樂觀鎖衝突判斷），屬於字串耦合，修改後端對應訊息時需留意是否影響前端比對邏輯。
- `src/constants/` 只放畫布/節點相關常數（`nodeTypes.ts`、`handles.ts`、`nodeIcons.ts`、`nodeDocs.ts`、`canvas.ts`），非通用訊息管理。
- `constants/nodeDocs.ts` 的說明文字**摘自後端 `service/workflow/executor/*.kt` 的 KDoc 與 `docs/workflow-engine/`**，描述的是引擎實際行為，不可憑印象撰寫；改動 executor 行為時須一併檢視。
- **插值運算式的顯示與儲存分離**：`utils/expression.ts` 負責解析／序列化 `{{nodeKey.field}}`（語法對齊後端 `ExecutionContext.PLACEHOLDER`，改後端 regex 須同步），`components/common/ExpressionEditor.vue` 以 contenteditable 把引用渲染成可讀色塊。**寫回 config 的永遠是原始字串**——顯示層不得改變儲存格式。使用 contenteditable 時務必守住兩點：組字（`compositionstart`～`compositionend`）期間不重繪也不 emit；只有外部值變更才重繪（比對自己最後 emit 的值），否則中文輸入會吃字、游標會跳。
- `constants/nodeOutputKeys.ts` 的 `DEFAULT_OUTPUT_KEYS` 同屬「手抄後端行為」的檔案：值逐項對應各 executor 的 `cfg.outputKey ?: "…"` 預設值（檔案內已註記出處與行號），改動 executor 預設鍵名時須同步，並有單元測試逐項固定。
