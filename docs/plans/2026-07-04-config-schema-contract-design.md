# 節點 config 與 Tool 設定的 JSON 契約設計

日期：2026-07-04
狀態：**已實作**（2026-07-05 完成，commits `4f66c9c`..`af82576`；實作計畫見 [2026-07-04-config-schema-contract.md](2026-07-04-config-schema-contract.md)）

> 實作與本設計的偏離（以程式碼為準）：`TriggerNodeConfig.triggerType` 採 `TriggerType` enum（非 String）；DATA_TRANSFORM 必填回報字樣為 `mappings|template`；kotlinx 對 scalar 有寬鬆轉型（數字/布林字串會被接受）；MCP 節點設定欄位名為 `userSettingId`（前端已同步）。

## 問題背景

前端實作設定表單時缺乏機器可讀的契約，造成「靠猜」：

1. **Workflow 節點 config**：schema 僅存在於 `docs/workflow-engine/system-design.md` §2（markdown），後端以通用 JSON（`Map<String, Any?>`）儲存，`WorkflowService.validateGraph()` 只驗圖結構、完全不驗 config 內容。前端 `LlmAssistantForm.vue` 等表單照文件手刻，文件改版時前端漂移不會被任何機制抓到。
2. **Tool settingContent**：工具設定欄位的事實來源其實已存在——`llm_tool.config_object_path` 指向 Kotlin config data class（如 `tool/config/Google.kt`），但未暴露給前端，前端只能提供 raw JSON editor 讓使用者自己填。
3. **LLM 模型參數**（temperature / topP 等）：前端無對應表單欄位，本次不處理（見「範圍外」）。

## 已拍板決策

| 決策點 | 結論 |
|--------|------|
| 範圍 | 節點 config 與 Tool settingContent 兩者都做 |
| 架構 | 混合式：節點 config 用後端強型別 DTO（程式碼即契約）；Tool 用 API 回傳 settingSchema（schema 即資料） |
| 節點驗證時機 | 兩段式：save（DRAFT）驗型別正確性、允許必填缺席；switchStatus 啟用時嚴格驗必填 |
| Tool schema 來源 | 從 configObjectPath 的 data class 反射自動產生，annotation 補 UI 提示，避免雙重來源漂移 |

## 設計

### A. 節點 config 強型別化（後端）

新增 package `dto/workflow/config/`，為 10 種 `NodeType` 各建一個 `@Serializable` config DTO：

`TriggerNodeConfig`、`LlmAssistantNodeConfig`、`ToolNodeConfig`、`McpServerNodeConfig`、`KnowledgeRagNodeConfig`、`ConditionNodeConfig`、`LoopNodeConfig`、`CodeNodeConfig`、`HttpRequestNodeConfig`、`DataTransformNodeConfig`

- 欄位定義以 `docs/workflow-engine/system-design.md` §2 為準；此後**事實來源為 DTO 程式碼**，文件降級為說明。
- 所有欄位宣告為 nullable，配合兩段式驗證。
- 新增 `NodeConfigRegistry`（`NodeType` → serializer 映射）。

驗證流程：

1. **save（DRAFT）**：每個節點的 config 依 type 反序列化，`ignoreUnknownKeys = false`——型別錯誤或未知欄位 → 400；必填欄位缺席放行（允許存不完整草稿）。
2. **switchStatus 啟用**：逐節點檢查必填欄位非 null；驗不過回傳「哪個 nodeKey 缺哪些欄位」。
3. 錯誤訊息依 i18n 規範新增 `AppMessage` entry（`workflow.node.config.*`），en_US 與 zh_TW 同步。

### B. Tool settingSchema 反射產生（後端）

- 新增 annotation `@ToolConfigField(description: String = "", sensitive: Boolean = false)`，標註於 config data class 欄位（如 `Google.apiKey` 標 `sensitive = true`）。
- 新增 `ToolSchemaGenerator` util：從 `configObjectPath` 反射產生 schema——欄位名、型別（string / integer / number / boolean）、nullable → `required: false`、annotation → `description` / `sensitive`。
- `ToolDTO` 新增 `settingSchema` 欄位；`/llm/tool/list` 與 `/llm/tool/get` 回傳。
- 無 config class 的工具（CUSTOMIZE 型或 configObjectPath 為 null）回 `null`。

settingSchema 回傳格式範例：

```json
{
  "apiKey":  { "type": "string",  "required": true,  "sensitive": true },
  "csi":     { "type": "string",  "required": true },
  "siteRestrict": { "type": "boolean", "required": false },
  "timeout": { "type": "integer", "required": true }
}
```

### C. 前端（bestpartner-ui）

- 工具設定表單改為依 `settingSchema` 動態渲染：`string` → text input、`sensitive` → password input、`boolean` → checkbox、`integer`/`number` → number input，必填欄位標示；`settingSchema` 為 `null` 時 fallback 到現有 raw JSON editor。
- 節點表單（`LlmAssistantForm.vue` 等）維持手刻，正確性由後端 A 機制把關（存檔 400 即回饋欄位錯誤）。

### D. 文件同步

依 `documentation-update-policy.md`，實作完成後透過 `documentation-sync` skill 更新：

- `.claude/rules/api-endpoints.md`（tool list/get 回傳欄位變更）
- `docs-site/docs/api/`、`docs-site/docs/features/tools.md`
- `docs/api-test-plan.md`、`docs/postman/basepartner.postman_collection.json`
- `docs/workflow-engine/system-design.md` §2 加註「事實來源為 dto/workflow/config/」

## 測試重點

- 每個 NodeType config DTO：合法 config 反序列化成功；型別錯 / 未知欄位 → save 400。
- 兩段式驗證：缺必填可存 DRAFT、啟用時被擋且錯誤指出 nodeKey 與欄位。
- `ToolSchemaGenerator`：Google / Tavily 產出的 schema 與 data class 欄位一致；nullable 對應 required、annotation 對應 sensitive/description。
- 前端：schema-driven 表單渲染各型別欄位、null schema fallback JSON editor（Vitest）。

## 範圍外（後續延伸）

- **MCP settingContent**：與 Tool 同構的問題，待 Tool 機制驗證後比照辦理。
- **LLM 模型參數**（temperature / topP / maxTokens）表單化。
- **全面 schema-driven 節點表單**（類 n8n）：若未來節點種類暴增再評估。
