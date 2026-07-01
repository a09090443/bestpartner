# 型別化 Inspector 表單設計（Phase 3）

> 日期：2026-07-01
> 範圍：`bestpartner-ui` 前端，不改後端
> 目標：把 Inspector 的裸 JSON 設定，換成依節點型別的結構化表單，並串接既有後端下拉。

## 背景

現況 `InspectorPanel.vue` 對所有節點都用 `JsonConfigEditor`（裸 JSON），使用者須手打 config。Phase 3 為 4 種可串既有後端的節點提供專屬表單，其餘 6 種維持 JSON fallback。

前端 api 慣例：`api/http.ts`（axios 實例，自動帶 JWT）＋ `unwrap(ApiResponse)` 取 `data`。

## 決策

### 範圍：4 種串後端節點

只做 `LLM_ASSISTANT` / `TOOL` / `MCP_SERVER` / `KNOWLEDGE_RAG`。其餘（TRIGGER/CONDITION/LOOP/CODE/HTTP_REQUEST/DATA_TRANSFORM）維持 `JsonConfigEditor`。

### 下拉資料來源

| 節點 | 端點 | 回傳 | value / label |
|------|------|------|---------------|
| `LLM_ASSISTANT` | `POST /llm/setting/get`（空 body 取全部） | `List<LLMDTO>` | `id` / `alias` |
| `TOOL` | `GET /llm/tool/list` | `List<ToolDTO>` | `id` / `name` |
| `MCP_SERVER` | `GET /llm/mcpServer/list` | `List<McpDTO>` | `mcpId` / `name` |
| `KNOWLEDGE_RAG` | ⚠️ 無列舉端點 | — | 改用文字輸入 |

### KNOWLEDGE_RAG：文字欄位

後端無「列出知識庫」端點（`getKnowledgeStore` 需先給 knowledgeId、回傳文件清單）。Phase 3 純前端，不改後端，故 KNOWLEDGE_RAG 用 `knowledgeId` 文字輸入 + `topK` 數字。待後端補列舉端點再換下拉（後續任務）。

### UI 元件：原生 select/input

沿用 InspectorPanel 現有的原生 input 風格，不在 inspector 引入 Element Plus，測試簡單、風格一致。

## 設計

### 檔案佈局

```
api/
  llmSetting.ts        # getSettings() → Option[]
  tool.ts              # listTools()   → Option[]
  mcpServer.ts         # listMcpServers() → Option[]
components/inspector/
  InspectorPanel.vue   # 改：依 data.type 分派表單
  forms/
    LlmAssistantForm.vue
    ToolForm.vue
    McpServerForm.vue
    KnowledgeRagForm.vue
  JsonConfigEditor.vue # 保留為其餘 6 型別 fallback
composables/
  useNodeOptions.ts    # 載入 + 模組級快取三種下拉清單
```

### 資料流（沿用既有契約，編輯器不改）

```
InspectorPanel 依 selectedNode.data.type 分派表單；其餘型別用 JsonConfigEditor。
表單 props: config；emit: update:config（新的完整 config 物件）。
InspectorPanel 收 update:config → 沿用現有 emit('update:node-config', ...)
→ WorkflowEditorView.onNodeConfigUpdate 不需改。
```

### config schema

| 節點 | config 鍵值 |
|------|-------------|
| `LLM_ASSISTANT` | `llmId: string`、`systemPrompt?: string` |
| `TOOL` | `toolId: string` |
| `MCP_SERVER` | `mcpId: string` |
| `KNOWLEDGE_RAG` | `knowledgeId: string`、`topK?: number`（預設 4） |

表單行為：以 `props.config` 初始化；改動即以不可變方式 `emit('update:config', { ...props.config, 鍵: 值 })`，保留未知鍵值避免切換遺失。

### 驗證分工

- 表單為結構化輸入，不會有 JSON 解析錯誤，一律 `emit('config-validity', true)`（沿用契約，`handleSave` 的 `configValid` 不變）。
- 「必填未選」本期不擋存檔（與 Phase 2 分級一致，屬啟用前才需完整）。

### 選項載入與快取（`useNodeOptions.ts`）

三種下拉以模組級 `ref` 快取，一個 session 只抓一次：

```ts
export interface Option { value: string; label: string }
const llmOptions = ref<Option[] | null>(null)
// tool / mcp 同理
export function useNodeOptions() {
  async function loadLlmOptions() {
    if (!llmOptions.value) llmOptions.value = await llmSettingApi.getSettings()
    return llmOptions.value
  }
  // loadToolOptions / loadMcpOptions 同理
  return { loadLlmOptions, loadToolOptions, loadMcpOptions }
}
```

三層分工：api 模組負責 DTO→Option 轉換；composable 只管快取；表單只管顯示。載入失敗時 catch，選項留空、仍顯示已存 value，不讓 inspector 崩潰。

## 測試計畫（TDD，vitest）

- `api/llmSetting`、`api/tool`、`api/mcpServer`：`vi.mock` http，驗證 DTO→Option 轉換。
- `useNodeOptions`：第二次呼叫不再打 API（快取生效）。
- 各表單元件：`vi.mock` composable 回固定 options，驗證 select 渲染選項數、選取時 emit 正確 config、保留未知鍵值。
- `InspectorPanel`：型別分派（LLM_ASSISTANT → LlmAssistantForm；CODE → JsonConfigEditor）。

## 受影響檔案

| 檔案 | 動作 |
|------|------|
| `api/llmSetting.ts` / `api/tool.ts` / `api/mcpServer.ts` | 新增 |
| `composables/useNodeOptions.ts` | 新增 |
| `components/inspector/forms/*.vue`（4 檔） | 新增 |
| `components/inspector/InspectorPanel.vue` | 改：型別分派 |

## 範圍界線

Phase 3 純前端，不改後端。KNOWLEDGE_RAG 待後端補列舉端點再換下拉。必填強制留待 workflow 啟用驗證。
