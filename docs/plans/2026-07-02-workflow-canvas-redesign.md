# Workflow 畫布深色重設計實作規格

> 來源：claude.ai 設計專案「N8n 工作流畫布設計」/ `Workflow Canvas.dc.html`
> 目標：把該 n8n 風格深色設計套用到既有 `bestpartner-ui` 工作流編輯器。
> **重構，不重寫**：保留 Vue Flow 引擎與網域邏輯（`useWorkflowSync` / `useGraphValidation` / `useCanvasLayout` / store）。

## 範圍決定

- 深色主題**僅限編輯器視圖**（`WorkflowEditorView` 及其子元件），不影響 Login／WorkflowList。
- **不納入 Execute 按鈕**：後端目前無執行引擎，設計檔的執行動畫為純前端模擬，接上會誤導。保留 Active 切換與存檔。
- 沿用專案 10 種 `NodeType`，新增 `category` 欄位對映設計的四大分組，不採用 n8n 的節點集合。

## 設計 Token（取自設計檔 CSS）

| Token | 值 | 用途 |
|---|---|---|
| `--wf-bg` | `#131316` | 畫布最底層背景 |
| `--wf-surface` | `#17171c` | 左右側欄背景 |
| `--wf-surface-2` | `#1a1a20` | 頂部工具列 |
| `--wf-card` | `#1e1e26` | 節點卡片背景 |
| `--wf-input` | `#0f0f13` | 輸入框背景 |
| `--wf-border` | `#29292f` | 分隔線／欄框 |
| `--wf-border-2` | `#31313c` | 節點邊框 |
| `--wf-text` | `#e7e7ec` | 主文字 |
| `--wf-text-dim` | `#8a8a95` | 次要文字（副標） |
| `--wf-text-mute` | `#5c5c67` | 分類標題 |
| `--wf-accent` | `#FF6A54` | 主色（選取環、主要按鈕、暫時連線） |
| `--wf-success` | `#3ecf8e` | 已存／完成狀態 |
| `--wf-grid-dot` | `#2b2b34` | 點陣格線 |

- 字型：主體 `Manrope`（Google Fonts）、等寬 `IBM Plex Mono`（副標、程式碼欄位）。以 `@import` 或 index.html `<link>` 引入，並限定套用於編輯器根容器 class（如 `.wf-editor`）避免污染全站。
- 建議把 token 集中於 `bestpartner-ui/src/styles/workflow-theme.css`，以 `.wf-editor { --wf-...: ... }` 界定作用域，於 `WorkflowEditorView` 根 div 掛 `class="wf-editor"`。

## 節點分類（新增至 `NodeTypeMeta.category`）

| category | NodeType |
|---|---|
| `Trigger` | TRIGGER |
| `Action` | TOOL, MCP_SERVER, HTTP_REQUEST |
| `AI` | LLM_ASSISTANT, KNOWLEDGE_RAG |
| `Logic` | CONDITION, LOOP, CODE, DATA_TRANSFORM |

- `category` 型別：`'Trigger' | 'Action' | 'AI' | 'Logic'`。
- NodePalette 依此分組顯示，順序 Trigger → Action → AI → Logic。
- 既有每型別 `color` 沿用（皆能在深色背景辨識）。

## 逐元件變更

### 1. `constants/nodeTypes.ts`
- `NodeTypeMeta` 新增 `category: NodeCategory`。
- 為 10 種型別各補 `category`（見上表）。
- 匯出 `NODE_CATEGORIES: NodeCategory[]`（固定順序）與 `getNodesByCategory()` 或於 palette 端分組皆可。
- 更新 `nodeTypes.test.ts`：斷言每型別有 category、分類涵蓋全部型別。

### 2. `components/canvas/NodePalette.vue`（左側欄，寬 250px）
- 深色背景 `--wf-surface`，右邊框 `--wf-border`。
- 頂部「ADD NODE」小標題（uppercase、letter-spacing）＋搜尋框（放大鏡 icon、`--wf-input` 背景、focus 時邊框轉 accent）。
- 依 `category` 分組：每組小標題（uppercase `--wf-text-mute`）＋項目列（icon box 以型別色 16% 底＋色邊框、label、右側加號 icon、hover `#212128`）。
- 搜尋以 label 過濾（大小寫不敏感），空組不顯示。
- 保留現有 `draggable` + `dragstart(DRAG_NODE_TYPE_KEY)` 行為（拖放到畫布新增節點）；點擊項目可選配「加到畫布中央」但**優先保留拖放**，避免破壞既有 `onDrop` 測試。
- 更新 `NodePalette.test.ts` 對映新結構（保留 `data-test="palette-item-<type>"`）。

### 3. `components/canvas/WorkflowNode.vue`（節點卡片）
- 卡片：`--wf-card` 底、圓角 14px、`1.5px` 邊框 `--wf-border-2`、陰影；橫向排版：左 icon box（36×36、型別色 16% 底＋色邊框＋色 icon）＋右資訊區。
- 資訊區三行：型別 label（9.5px uppercase 型別色）／節點名稱（13.5px 粗體 `#eff0f4`，溢出省略）／副標（IBM Plex Mono 10.5px `--wf-text-dim`）。副標由 config 摘要產生（如 CONDITION 顯示條件、HTTP 顯示 method+url），可先做簡單版：顯示 type label 或 config 首個值。
- **選取狀態**：邊框轉型別色＋外環 `0 0 0 2px rgba(color,.55)`（用 Vue Flow 的 `.selected` class 掛樣式）。
- 保留資料驅動多埠（`inputs`/`outputs`、`portTop`），埠改為設計樣式：13px 圓、`--wf-card` 底、輸入埠灰邊、輸出埠型別色邊；多埠 label（True/False、迴圈/結束）貼右緣。
- 更新 `WorkflowNode.test.ts` 保留埠 id 斷言（`in:main` / `out:true` 等），調整結構斷言。

### 4. `views/WorkflowEditorView.vue`（工具列＋畫布容器）
- 根 div 掛 `class="wf-editor"`，套深色 token。
- **頂部工具列**（54px）：左側 logo 方塊（accent 底）＋「Personal /」麵包屑＋流程名稱 inline input（可編輯，改寫 `store.current.name`）＋「Saved／未存」狀態徽章（dirty 時橘、否則綠點 Saved）。右側 Editor/Executions 分頁樣式（Executions 可先為視覺佔位、disabled）＋Active 切換開關（接 `switchStatus`，或先只切 UI 狀態並標 dirty）＋Save 按鈕（icon＋文字）。**不放 Execute**。
- **畫布**：`Background` 改 `variant="dots"`、深色 `pattern-color="#2b2b34"`、`color`/`bg-color` 深色；`Controls` 與 `MiniMap` 以 `:deep()` 覆寫為深色樣式（`--wf-surface-2` 底、`--wf-border` 邊）。
- 邊線預設色改深色系（`#4a4a55`），hover/selected 轉 accent。
- 保留 `handleTidyUp`（整理版面／dagre）與 `handleSave`（含驗證、樂觀鎖衝突處理）邏輯不變，只改按鈕外觀與位置。
- 更新 `WorkflowEditorView.test.ts`：保留 `data-test="save-button"`、`data-test="tidy-button"`；名稱 input 若移到工具列需同步。

### 5. `components/inspector/InspectorPanel.vue`（右側欄，寬 322px）
- 深色背景 `--wf-surface`、左邊框 `--wf-border`。
- **已選節點**：頂部 header（型別 icon box＋型別 label＋名稱 input）；中段 Parameters（沿用型別化表單 `LLM_ASSISTANT/TOOL/MCP_SERVER/KNOWLEDGE_RAG`，其餘退回 `JsonConfigEditor`，欄位改深色樣式）；底部 Duplicate／Delete 按鈕列（Delete 為紅色系）。Duplicate/Delete 透過既有 store／Vue Flow API 實作（新增對應 emit 或直接呼叫）。
- **未選節點（Workflow overview）**：取代目前的流程名稱/描述表單，改為：概述說明＋2×2 統計卡（Nodes／Connections／Triggers／Status）＋節點型別圖例（Trigger/Action/AI/Logic 顏色）。流程名稱/描述編輯可移至工具列 inline input＋一個「描述」欄位，或保留於 overview 上方。**流程 meta 編輯能力不可遺失**（現有 `update:workflow-name` / `update:workflow-description` emit 要保留可用路徑）。
- 統計數字由 props 傳入（父層以 `toObject()` 或 store 計算 nodes/edges 數、trigger 數、status）。
- 更新 `InspectorPanel.test.ts`：保留 `node-name-input`；overview 分支改對映統計卡；保留型別化表單分派測試。

## 驗收條件

- `npx vue-tsc --noEmit`（型別）通過。
- `npx vitest run` 全綠（既有測試按結構調整後保持通過，不得刪測試換過關）。
- `npx vite build` 成功。
- 視覺上編輯器呈深色 n8n 風格；淺色其餘頁面不受影響。
- 存檔／整理版面／驗證／拖放新增節點／多埠連線等既有功能不回歸。

## 不觸發 documentation-sync

本次僅前端樣式與前端型別，無 API／entity／DDL／gradle／application.properties 異動，不符觸發條件。惟前端說明文件 `docs-site/docs/features/workflow.md` 可於完成後補一段「深色編輯器外觀」，非強制。
