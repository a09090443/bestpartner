---
sidebar_position: 6
description: BestPartner Workflow API，提供視覺化工作流定義的 CRUD 管理，端點前綴 /llm/workflow，含擁有權與 admin 代管規則。
keywords: [Workflow API, 工作流, CRUD, 節點, 畫布驗證]
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
| GET | `/llm/workflow/nodeRequiredFields` | 回傳各 NodeType 的必填欄位清單，供前端即時驗證 |
| POST | `/llm/workflow/update` | 僅更新 meta（name/description/canvasMeta）|
| POST | `/llm/workflow/delete` | 刪除 workflow（連鎖刪 node/edge）|
| POST | `/llm/workflow/switchStatus` | 啟用/停用 workflow |
| POST | `/llm/workflow/execute` | 執行 workflow，以 SSE 事件流回報進度與結果 |

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

每個節點的 `config` 在後端以**強型別 DTO** 定義契約：13 種 `NodeType` 各對應一個 `@Serializable` config 類別（`dto/workflow/config/NodeConfig.kt`，**此檔即 schema 的唯一事實來源**），並以兩段式驗證執行：

- **save（含 DRAFT）**：依 NodeType 嚴格反序列化——**未知欄位**或**結構性型別錯誤**（如陣列欄位給字串、非法 enum 值）回 400（`workflow.node.config.invalid`，訊息含 nodeKey 與原因）；必填欄位缺席**放行**，允許存不完整草稿。數字/布林形式的字串（如 `"topK": "5"`）會被寬鬆轉型接受。
- **switchStatus 啟用**：逐節點檢查必填欄位，驗不過回 400（`workflow.node.config.required.missing`，訊息含 nodeKey 與缺漏欄位清單）；接著依序檢查三項圖層級規則，任一不過即回 400（訊息皆含 nodeKey），確保「ACTIVE」等同「可執行」：**孤兒 SKILL 節點**（未掛載到任何 LLM 助手的 `in:tool` 埠）→ `workflow.skill.node.not.mounted`；**孤兒 PROMPT 節點**（未連到任何 LLM 助手的 `in:prompt` 埠）→ `workflow.prompt.node.not.connected`；**無提問來源的 LLM 節點**（既未填 `userPrompt` 也無 `PROMPT` 連入其 `in:prompt` 埠）→ `workflow.llm.prompt.required`。**停用（`active=false`）不跑圖驗證**，避免被卡在改不回去的狀態。
- **ACTIVE 重存**：對 status 已為 `ACTIVE` 的 workflow 執行 save 時，同步套用上述必填驗證（與啟用共用同一份邏輯），維持「ACTIVE ⟺ 通過啟用驗證」不變式；DRAFT/INACTIVE 的 save 不驗必填。

> 相容性：save 時後端會自動把 `MCP_SERVER` 節點 config 的舊欄位 `mcpSettingId` 改名為契約欄位 `userSettingId`（現值優先），涵蓋既有資料、載入後直接存與匯入等來源。

> ⚠️ 因未知欄位會被拒，前端表單寫入的欄位名必須與 DTO 完全一致；以 `JsonConfigEditor` 自由編輯的 config 若含契約外的鍵，存檔會被 400 擋下（錯誤訊息會指出節點與原因）。

下列 7 種節點具備型別化表單；其餘 6 種（`TRIGGER`、`CONDITION`、`LOOP`、`CODE`、`HTTP_REQUEST`、`DATA_TRANSFORM`）前端退回純 JSON 編輯器，但後端契約已定義（欄位見 `docs/workflow-engine/system-design.md` §2 與 `NodeConfig.kt`）。

> **Agent 模式（LLM 節點簡化）**：`LLM_ASSISTANT` 只保留 LLM 呼叫本身的欄位；工具、MCP、Skill、知識庫改為獨立節點（`TOOL` / `MCP_SERVER` / `SKILL` / `KNOWLEDGE_RAG`），以輸出連到 LLM 節點的工具輸入埠 `in:tool` 掛載，由 LLM 自主呼叫。舊有含 `toolIds` / `mcpIds` / `skillIds` / `knowledgeId` / `files` 的 LLM config 因嚴格 JSON 會被拒（開發期以重建種子資料處理）。

### LLM_ASSISTANT（LLM 助手）

| 欄位 | 型別 | 必填 | 說明 |
|------|------|:----:|------|
| `llmId` | string | ✓ | LLM 設定 id（對應 `llm_setting`） |
| `systemPrompt` | string | | 系統提示 |
| `userPrompt` | string | | 使用者提示（提問內容的**後備**來源），支援變數插值引用上游輸出 |
| `enableMemory` | boolean | | 是否啟用對話 Memory |
| `memoryId` | string | | Memory 識別；留空則單次執行內共享（僅 `enableMemory=true` 時寫入） |
| `responseFormat` | `"TEXT"` \| `"JSON"` | | 回應格式，預設 `TEXT` |
| `outputSchema` | object | | `responseFormat=JSON` 時的輸出 JSON Schema |
| `outputKey` | string | | 輸出鍵名，預設 `reply` |

> 工具 / MCP / Skill / 知識庫不再是本節點欄位——改由 `TOOL` / `MCP_SERVER` / `SKILL` / `KNOWLEDGE_RAG` 獨立節點連到 `in:tool` 埠掛載（Agent 模式，見上）。其中 `KNOWLEDGE_RAG` 為自動注入型 RAG（推論前自動檢索並注入），其餘三種由 LLM 於 agent loop 自主呼叫。

> **提問內容的優先序**：① 連到本節點 `in:prompt` 埠且**已執行成功**的 `PROMPT` 節點輸出（多個來源時依拓撲序取第一個有輸出者——未活化的分支沒有輸出，自然被略過）；② `userPrompt`（插值後）。兩者皆無時無法啟用（`workflow.llm.prompt.required`）。`PROMPT` 節點的輸出已於其自身 executor 插值完成，本節點不再二次插值。
>
> ⚠️ 邊界情形：本節點若經 `in:main` 被活化、但唯一的 `PROMPT` 來源落在未活化分支且未填 `userPrompt`，執行時該節點會 FAILED——啟用驗證只保證「至少存在一種來源」，無法預知執行期走哪條分支。

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

### PROMPT（提示詞）

| 欄位 | 型別 | 必填 | 說明 |
|------|------|:----:|------|
| `prompt` | string | ✓ | 提問內容，支援 `{{nodeKey.key}}` 插值（由 executor 於執行期解析一次） |
| `outputKey` | string | | 輸出鍵名，預設 `prompt` |

> 以 `out:main` 連到 `LLM_ASSISTANT` 的 `in:prompt` 埠作為該次推論的提問。**此邊為一般資料流邊**（與 `in:tool` 的能力掛載相反）：參與節點活化與拓撲排序，本節點照常執行、落執行紀錄並發 SSE 事件，輸出亦可被其他節點以 `{{promptNodeKey.prompt}}` 引用。
>
> 因此 `CONDITION` 的兩個分支可各接一個提示節點、再匯入同一顆 LLM——執行時只有被活化那條分支的提示節點有輸出，LLM 即取該提問。孤兒 PROMPT（未連到任何 LLM 提示埠）在 **switchStatus 啟用前**與 **execute 執行前**兩處驗證，皆回 `workflow.prompt.node.not.connected`（含 nodeKey）；`save` 放行（DRAFT 容許未完成的圖）。兩處共用 `WorkflowEngine.findUnconnectedPromptNodeKey`，避免規則分叉。

### SKILL（Skill 能力節點）

| 欄位 | 型別 | 必填 | 說明 |
|------|------|:----:|------|
| `skillId` | string | ✓ | Skill id（對應 `/llm/skill/list`） |

> 能力節點：只連到 `LLM_ASSISTANT` 的 `in:tool` 埠，本身不獨立執行、不落執行紀錄。孤兒 SKILL（未掛載任何 LLM）在 **switchStatus 啟用前**與 **execute 執行前**兩處驗證，皆回 `workflow.skill.node.not.mounted`（含 nodeKey）；`save` 放行（DRAFT 容許未完成的圖）。兩處共用 `WorkflowEngine.findUnmountedSkillNodeKey`，避免規則分叉。

### KNOWLEDGE_RAG（知識庫 RAG）

| 欄位 | 型別 | 必填 | 說明 |
|------|------|:----:|------|
| `knowledgeId` | string | ✓ | 知識庫 id（兩種模式共通的唯一**無條件**必填） |
| `embeddingModelId` | string | △ | Embedding 模型設定 id。pipeline 模式用；外掛模式由知識庫自身的 embedding 設定決定，可留空 |
| `query` | string | △ | 檢索語句，支援插值。**pipeline 模式必填**，但屬**執行時**驗證（缺 query 時 executor 拋錯）而非啟用前必填；外掛模式由 LLM 的問題自動帶入 |
| `topK` | number | | 取回筆數 |
| `minScore` | number | | 相似度下限 |
| `outputKey` | string | | 輸出鍵名 |

> △ ＝ **條件必填**，不列入 `missingRequiredFields()`，故不會出現在 `nodeRequiredFields` 清單、啟用時也不擋。
> 本節點有兩種模式，必填條件不同：
>
> | 連到的埠 | 模式 | 額外必填 | 是否獨立執行 |
> |---------|------|---------|------------|
> | LLM 的 `in:tool` | **外掛模式**（自動注入型 RAG） | 無 | ✗ 純能力掛載，不落執行紀錄 |
> | 一般 `in:main` | **pipeline 模式**（顯式檢索節點） | `query`（執行時驗證） | ✓ 產生自身 `node_execution` |
>
> 「純能力節點」的判定是**所有出邊皆為 `in:tool`**；若同一節點另有 `out:main` 出邊，仍會落入主遍歷、兼作 pipeline 步驟。

> `topK`（預設 5）／ `minScore`（預設 0.0）／ `embeddingModelId` 於執行時實際傳入相似度檢索（Phase 2 起生效）。

### OUTPUT（輸出）

| 欄位 | 型別 | 必填 | 說明 |
|------|------|:----:|------|
| `template` | string | ✓* | 輸出模板，支援 `{{nodeKey.path}}` 插值；結果放在 `result` 鍵 |
| `mappings` | object | ✓* | key=value 對映（value 支援插值），逐鍵組成輸出物件 |

> `template` 與 `mappings` **擇一必填**（任一有值即滿足）。OUTPUT 節點**無輸出埠**，為流程終點，其輸出即為執行結果的最終呈現。

> 前端 `ToolForm`、`McpServerForm`、`KnowledgeRagForm`、`PromptForm` 與 `OutputForm` 已涵蓋上述必填與主要選填欄位，`arguments`（物件型別）以 JSON 文字輸入（僅接受合法 JSON 物件，非物件或壞 JSON 不寫入 config）。

### 其餘節點（後端契約已定義，前端以 JSON 編輯器輸入）

`TRIGGER`（`triggerType` 必填，enum：`MANUAL`/`WEBHOOK`/`CRON`）、`CONDITION`（`conditions` 必填非空）、`LOOP`（`inputArrayPath`、`loopBodyEntryNodeKey` 必填）、`CODE`（`language`、`source` 必填）、`HTTP_REQUEST`（`method`、`url` 必填；`headers` / `secretHeaders` 執行時逐值插值後套用、`timeoutMs` 作為呼叫逾時，Phase 2 起生效；`secretHeaders` 加密儲存，見下方「機密欄位」）、`DATA_TRANSFORM`（`mappings` 與 `template` 至少擇一）。

完整欄位定義見 `docs/workflow-engine/system-design.md` §2；程式碼事實來源為 `dto/workflow/config/NodeConfig.kt`。以 JSON 編輯器輸入時，鍵名拼錯或型別不符會在存檔時被 400 擋下。

### 機密欄位（`HTTP_REQUEST` 的 `secretHeaders`）

`secretHeaders` 的值以 AES-GCM 加密儲存，**明文不會隨任何 API 回應外流**：

| 時機 | 行為 |
|------|------|
| `save` | 逐值加密後落地 |
| `get` | 值一律回傳遮罩 `__SECRET_KEPT__` |
| `execute` | 後端解密後才送出請求；執行紀錄中的 config 亦經遮罩 |

⚠️ **`save` 為整張覆寫，遮罩值必須原樣送回**：後端見到 `__SECRET_KEPT__` 即沿用既有密文、不覆寫。
若要更改金鑰，直接填入新明文即可。若把遮罩值搭配一個**新的 header 名稱**送出（等同改名），
因無對應既有密文，該欄位會被移除——改名請一併重填明文。

加密上線前存入的明文仍可正常執行，下次 `save` 會自動轉為密文——即使原樣送回遮罩值，
沿用路徑也會偵測到既有值非密文格式而補加密，明文不會永久殘留。

## 取得 Workflow

`POST /llm/workflow/get`

```json
{ "id": "workflow-id" }
```

**Response**：`WorkflowDTO`，含完整 `nodes` 與 `edges`。
其中 `HTTP_REQUEST` 節點的 `secretHeaders` 值為遮罩 `__SECRET_KEPT__`（見上方「機密欄位」）。

## 列出 Workflow

`GET /llm/workflow/list`

**Response**：當前使用者的 `WorkflowSummaryDTO` 陣列（含 `id`、`name`、`status`、`version`、`updatedAt`）。

## 取得節點必填欄位清單

`GET /llm/workflow/nodeRequiredFields`

回傳各 `NodeType` 的必填欄位清單，供前端載入時**即時驗證**節點設定、在存檔前提示缺漏欄位，而不需重複硬編必填規則。清單直接由後端 `NodeConfig` 契約（`missingRequiredFields`，以空 config 推導）產生——與 `save` / `switchStatus` 的必填檢核**共用同一份事實來源**。

**Response**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "LLM_ASSISTANT": ["llmId"],
    "PROMPT": ["prompt"],
    "TOOL": ["toolId"],
    "MCP_SERVER": ["mcpId", "toolName"],
    "SKILL": ["skillId"],
    "KNOWLEDGE_RAG": ["knowledgeId"],
    "TRIGGER": ["triggerType"],
    "CONDITION": ["conditions"],
    "LOOP": ["inputArrayPath", "loopBodyEntryNodeKey"],
    "CODE": ["language", "source"],
    "HTTP_REQUEST": ["method", "url"],
    "DATA_TRANSFORM": ["mappings|template"],
    "OUTPUT": ["template|mappings"]
  }
}
```

> ⚠️ 本清單只涵蓋**單一節點 config 內**可判定的必填欄位。**圖層級**的條件必填不在此列——例如 LLM 節點的提問來源（`userPrompt` 或連入的 `PROMPT` 節點）取決於連線而非 config，故 `LLM_ASSISTANT` 只列 `llmId`；該規則由 switchStatus 的 `workflow.llm.prompt.required` 把關，前端另以 `PROMPT_WIRING_INCOMPLETE` warning 提示。

> 複合字樣 `"mappings|template"` 表示兩欄位**擇一必填**（任一有值即滿足）。前端據此在 Inspector 即時提示、並於存檔前驗證；對 **ACTIVE** 的 workflow，缺必填會提前擋下存檔（與後端 ACTIVE 重存驗必填一致），DRAFT / INACTIVE 僅提示不擋。

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

## 執行 Workflow

`POST /llm/workflow/execute`

需登入（`@Authenticated`）。以 **SSE（`text/plain`）事件流**回傳執行進度與結果，每行為 `data:{json}` 格式。

**Request**

```json
{
  "id": "workflow-id",
  "inputPayload": { "message": "選填的觸發輸入" }
}
```

**Response（SSE 事件流範例）**

```text
data:{"event":"execution.started","executionId":"exec-1","ts":"2026-07-10T10:00:00"}
data:{"event":"node.started","executionId":"exec-1","nodeKey":"trigger-1","seqNo":1,"ts":"2026-07-10T10:00:00"}
data:{"event":"node.completed","executionId":"exec-1","nodeKey":"trigger-1","seqNo":1,"status":"SUCCESS","output":{"message":"選填的觸發輸入"},"durationMs":3,"ts":"2026-07-10T10:00:00"}
data:{"event":"node.started","executionId":"exec-1","nodeKey":"assistant-1","seqNo":2,"ts":"2026-07-10T10:00:01"}
data:{"event":"node.completed","executionId":"exec-1","nodeKey":"assistant-1","seqNo":2,"status":"SUCCESS","output":{"reply":"AI 回覆內容"},"durationMs":820,"ts":"2026-07-10T10:00:01"}
data:{"event":"node.started","executionId":"exec-1","nodeKey":"output-1","seqNo":3,"ts":"2026-07-10T10:00:02"}
data:{"event":"node.completed","executionId":"exec-1","nodeKey":"output-1","seqNo":3,"status":"SUCCESS","output":{"result":"最終輸出"},"durationMs":1,"ts":"2026-07-10T10:00:02"}
data:{"event":"execution.completed","executionId":"exec-1","status":"SUCCESS","output":{"result":"最終輸出"},"durationMs":824,"ts":"2026-07-10T10:00:02"}
```

**事件欄位**：`event` / `executionId` / `nodeKey` / `seqNo` / `status` / `output` / `error` / `durationMs` / `ts`（依事件類型，部分欄位為 null 或省略）。

**事件類型**

| 事件 | 說明 |
|------|------|
| `execution.started` | 整體執行開始 |
| `node.started` | 單一節點開始執行 |
| `node.completed` | 單一節點執行成功（`status=SUCCESS`，含 `output`） |
| `node.failed` | 單一節點執行失敗（`status=FAILED`，含 `error`） |
| `execution.completed` | 整體執行結束（含最終 `status`：`SUCCESS` / `FAILED` / `CANCELLED`） |

**行為說明**

- **已存檔即可執行**：不需將 workflow 切為 `ACTIVE`，DRAFT / INACTIVE 只要已存檔皆可執行。
- **執行前驗必填**：開始執行前會逐節點檢查 config 必填欄位（與啟用驗證同一份契約，經 `WorkflowEngine.validateForExecution` 於 request scope 預檢），**驗不過直接回 400，不建立執行紀錄**。
- **失敗語意**：單一節點失敗（`node.failed`）時，其**下游節點標記 `SKIPPED`**，整體執行以 `FAILED` 結束。
- **取消語意**：client 中途斷線時，執行標記為 `CANCELLED`。
- **變數插值**：全引擎統一使用 `{{nodeKey.path}}` 語法引用上游節點輸出；**引用不存在的節點該節點執行失敗**（錯誤訊息 i18n 化，含變數完整路徑）。
- **支援節點**：除 `SKILL`（能力節點，掛載到 LLM `in:tool` 埠、不獨立執行）外，其餘 11 種皆可執行——`TRIGGER`（`MANUAL`）、`LLM_ASSISTANT`、`TOOL`（動態呼叫）、`MCP_SERVER`、`KNOWLEDGE_RAG`、`CONDITION`（條件分支，未走分支下游 `SKIPPED`）、`LOOP`（子圖迭代，紀錄帶 `loop_index`）、`CODE`（GraalJS sandbox）、`HTTP_REQUEST`、`DATA_TRANSFORM`、`OUTPUT`。
- **節點逾時**：每節點以 config `timeoutMs`（HTTP／CODE）或預設 120s 為上限，逾時視同節點失敗。
- **執行紀錄**：整體與逐節點執行紀錄分別寫入 `llm_workflow_execution` 與 `llm_workflow_node_execution` 資料表。
