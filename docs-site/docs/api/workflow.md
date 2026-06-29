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
