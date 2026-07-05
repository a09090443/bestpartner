---
sidebar_position: 6
---

# Workflow API

Workflow API 提供視覺化工作流定義的管理功能。所有端點路徑前綴為 `/llm/workflow`，皆需登入（`@Authenticated`）；使用者僅能操作自己的 workflow，`admin` 角色可代管他人。

> 功能概念與驗證規則見 [視覺化 Workflow](../features/workflow.md)。

## 通用回應格式

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

錯誤時 `code` 為 400（業務例外，如版本衝突、畫布驗證失敗），`message` 為對應的 i18n 訊息。

## 端點列表

| HTTP | 路徑 | 說明 |
|------|------|------|
| POST | `/llm/workflow/create` | 建立空白 workflow |
| POST | `/llm/workflow/save` | 新增或整張覆寫 workflow（含 nodes/edges）|
| POST | `/llm/workflow/get` | 取得單一 workflow 完整定義 |
| GET | `/llm/workflow/list` | 列出當前使用者的 workflow 摘要 |
| POST | `/llm/workflow/update` | 僅更新 meta（name/description/canvasMeta）|
| POST | `/llm/workflow/delete` | 刪除 workflow（連鎖刪 node/edge）|
| POST | `/llm/workflow/switchStatus` | 啟用/停用 workflow |

---

## 建立 Workflow

`POST /llm/workflow/create`

**Request**

```json
{
  "name": "我的工作流",
  "description": "選填描述"
}
```

**Response**：回傳含新 `id`、`status=DRAFT`、`version=1` 的 `WorkflowDTO`。

## 儲存（新增或整張覆寫）

`POST /llm/workflow/save`

整張覆寫語意：後端會先刪除既有 node/edge，再寫入請求中的新集合。更新既有 workflow 時須帶入 `id` 與當前 `version`（樂觀鎖）。

**Request**

```json
{
  "id": "（更新時必填，新建時省略）",
  "version": 1,
  "name": "我的工作流",
  "description": null,
  "nodes": [
    { "nodeKey": "a", "type": "TRIGGER", "name": "開始", "positionX": 0, "positionY": 0, "config": {} },
    { "nodeKey": "b", "type": "TOOL", "name": "查詢", "positionX": 200, "positionY": 0, "config": {} }
  ],
  "edges": [
    { "sourceNodeKey": "a", "targetNodeKey": "b" }
  ],
  "canvasMeta": { "zoom": 1.0 }
}
```

**錯誤**

| message key | 說明 |
|------|------|
| `workflow.node.key.duplicated` | nodeKey 重複 |
| `workflow.edge.node.not.found` | edge 參考到不存在的節點 |
| `workflow.graph.has.cycle` | 圖含非法環 |
| `workflow.node.limit.exceeded` | 節點數超過上限 |
| `workflow.version.conflict` | version 與資料庫不符 |
| `workflow.not.found` | 指定 id 的 workflow 不存在 |
| `workflow.forbidden` | 非擁有者且非 admin |

## 節點 config 契約

每個節點的 `config` 在後端以**強型別 DTO** 定義契約：10 種 `NodeType` 各對應一個 `@Serializable` config 類別（`dto/workflow/config/NodeConfig.kt`，**此檔即 schema 的唯一事實來源**），並以兩段式驗證執行：

- **save（含 DRAFT）**：依 NodeType 嚴格反序列化——**未知欄位**或**結構性型別錯誤**（如陣列欄位給字串、非法 enum 值）回 400（`workflow.node.config.invalid`，訊息含 nodeKey 與原因）；必填欄位缺席**放行**，允許存不完整草稿。數字/布林形式的字串（如 `"topK": "5"`）會被寬鬆轉型接受。
- **switchStatus 啟用**：逐節點檢查必填欄位，驗不過回 400（`workflow.node.config.required.missing`，訊息含 nodeKey 與缺漏欄位清單）。

> ⚠️ 因未知欄位會被拒，前端表單寫入的欄位名必須與 DTO 完全一致；以 `JsonConfigEditor` 自由編輯的 config 若含契約外的鍵，存檔會被 400 擋下（錯誤訊息會指出節點與原因）。

下列 4 種節點具備型別化表單；其餘 6 種（`TRIGGER`、`CONDITION`、`LOOP`、`CODE`、`HTTP_REQUEST`、`DATA_TRANSFORM`）前端退回純 JSON 編輯器，但後端契約已定義（欄位見 `docs/workflow-engine/system-design.md` §2 與 `NodeConfig.kt`）。

### LLM_ASSISTANT（LLM 助手）

| 欄位 | 型別 | 必填 | 說明 |
|------|------|:----:|------|
| `llmId` | string | ✓ | LLM 設定 id（對應 `llm_setting`） |
| `systemPrompt` | string | | 系統提示 |
| `userPrompt` | string | | 使用者提示，支援變數插值引用上游輸出 |
| `enableMemory` | boolean | | 是否啟用對話 Memory |
| `memoryId` | string | | Memory 識別；留空則單次執行內共享（僅 `enableMemory=true` 時寫入） |
| `toolIds` | string[] | | 綁定的工具 id 清單 |
| `toolSettingIds` | string[] | | 對應工具的使用者設定 id（需 API key 的工具用此欄） |
| `mcpIds` | string[] | | 綁定的 MCP 伺服器 id 清單 |
| `mcpSettingIds` | string[] | | 對應 MCP 的使用者設定 id |
| `skillIds` | string[] | | 綁定的 Skill id 清單 |
| `knowledgeId` | string | | RAG 知識庫 id（自動增強） |
| `files` | string[] | | 附加的已上傳檔名，支援插值 |
| `responseFormat` | `"TEXT"` \| `"JSON"` | | 回應格式，預設 `TEXT` |
| `outputSchema` | object | | `responseFormat=JSON` 時的輸出 JSON Schema |
| `outputKey` | string | | 輸出鍵名，預設 `reply` |

**範例**

```json
{
  "nodeKey": "assistant-1",
  "type": "LLM_ASSISTANT",
  "name": "客服助手",
  "positionX": 200,
  "positionY": 0,
  "config": {
    "llmId": "3f2a...",
    "systemPrompt": "你是專業客服",
    "userPrompt": "請回覆：{{trigger.message}}",
    "toolIds": ["tool-google"],
    "responseFormat": "TEXT",
    "outputKey": "reply"
  }
}
```

### TOOL（工具）

| 欄位 | 型別 | 必填 | 說明 |
|------|------|:----:|------|
| `toolId` | string | ✓ | 工具 id |
| `toolSettingId` | string | | 執行期使用者設定 id |
| `arguments` | object | | 工具呼叫參數（支援插值） |
| `outputKey` | string | | 輸出鍵名 |

### MCP_SERVER（MCP 伺服器）

| 欄位 | 型別 | 必填 | 說明 |
|------|------|:----:|------|
| `mcpId` | string | ✓ | MCP 伺服器 id |
| `userSettingId` | string | | 執行期使用者設定 id（舊欄位名 `mcpSettingId` 已淘汰，表單讀取相容並自動遷移） |
| `toolName` | string | ✓ | 要呼叫的 MCP 工具名稱（啟用時必填） |
| `arguments` | object | | 工具呼叫參數（支援插值） |
| `outputKey` | string | | 輸出鍵名 |

### KNOWLEDGE_RAG（知識庫 RAG）

| 欄位 | 型別 | 必填 | 說明 |
|------|------|:----:|------|
| `knowledgeId` | string | ✓ | 知識庫 id |
| `embeddingModelId` | string | ✓ | Embedding 模型設定 id（啟用時必填；表單欄位待補，見已知落差） |
| `query` | string | ✓ | 檢索語句，支援插值（啟用時必填；表單欄位待補） |
| `topK` | number | | 取回筆數 |
| `minScore` | number | | 相似度下限 |
| `outputKey` | string | | 輸出鍵名 |

> ⚠️ 已知落差：前端 `KnowledgeRagForm` 目前僅提供 `knowledgeId`/`topK` 欄位，`embeddingModelId` 與 `query` 須先以 JSON 編輯器補上，否則啟用驗證不會通過。

### 其餘節點（後端契約已定義，前端以 JSON 編輯器輸入）

`TRIGGER`（`triggerType` 必填，enum：`MANUAL`/`WEBHOOK`/`CRON`）、`CONDITION`（`conditions` 必填非空）、`LOOP`（`inputArrayPath`、`loopBodyEntryNodeKey` 必填）、`CODE`（`language`、`source` 必填）、`HTTP_REQUEST`（`method`、`url` 必填）、`DATA_TRANSFORM`（`mappings` 與 `template` 至少擇一）。

完整欄位定義見 `docs/workflow-engine/system-design.md` §2；程式碼事實來源為 `dto/workflow/config/NodeConfig.kt`。以 JSON 編輯器輸入時，鍵名拼錯或型別不符會在存檔時被 400 擋下。

## 取得 Workflow

`POST /llm/workflow/get`

```json
{ "id": "workflow-id" }
```

**Response**：`WorkflowDTO`，含完整 `nodes` 與 `edges`。

## 列出 Workflow

`GET /llm/workflow/list`

**Response**：當前使用者的 `WorkflowSummaryDTO` 陣列（含 `id`、`name`、`status`、`version`、`updatedAt`）。

## 更新 Meta

`POST /llm/workflow/update`

僅更新 `name` / `description` / `canvasMeta`，不影響 node/edge。

```json
{
  "id": "workflow-id",
  "name": "更新後名稱",
  "description": "新描述",
  "canvasMeta": { "zoom": 1.2 }
}
```

## 刪除 Workflow

`POST /llm/workflow/delete`

```json
{ "id": "workflow-id" }
```

連鎖刪除其下的 node 與 edge。

## 啟用 / 停用

`POST /llm/workflow/switchStatus`

```json
{ "id": "workflow-id", "active": true }
```

`active=true` 設為 `ACTIVE`、`false` 設為 `INACTIVE`。啟用前須具備 `TRIGGER` 節點且圖無環，否則回 `workflow.trigger.node.required` 或 `workflow.graph.has.cycle`。
