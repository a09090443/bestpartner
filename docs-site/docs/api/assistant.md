---
sidebar_position: 3
description: BestPartner 助手 API，提供 AI 對話與 SSE 串流對話能力，端點前綴 /llm，含通用 ApiResponse 回應格式說明。
keywords: [助手 API, 對話, 串流, SSE, Chat, ApiResponse]
---

# 助手 API

助手 API 是 BestPartner 的核心功能，提供 AI 對話與串流對話能力。所有端點路徑前綴為 `/llm`。

## 通用回應格式

所有 API 的回應皆包裝在 `ApiResponse` 中：

```json
{
  "code": 200,
  "message": "",
  "data": "..."
}
```

## 一般對話

```http
POST /llm/chat
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 請求參數（ChatRequestDTO）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `message` | string | | 使用者輸入的訊息 |
| `promptContent` | string | | 系統提示詞，定義 AI 角色與行為 |
| `llmId` | string | | AI 模型 ID（`llm_setting` 表的 UUID）|
| `embeddingStoreId` | string | | 向量儲存 ID，啟用 RAG 功能 |
| `embeddingDocIds` | array | | 指定的文件 ID 列表 |
| `embeddingModelId` | string | | 嵌入模型 ID |
| `knowledgeId` | string | | 知識庫 ID |
| `toolIds` | array | | 工具 ID 列表（`BUILT_IN` 類型工具）|
| `toolSettingIds` | array | | 工具設定 ID 列表（`CUSTOMIZE` 類型工具）|
| `mcpIds` | array | | MCP Server ID 列表 |
| `mcpSettingIds` | array | | MCP Server 設定 ID 列表 |
| `files` | array | | 上傳檔案的路徑列表 |
| `memory.id` | string | | 對話 ID，用於多輪對話（留空則建立新對話）|
| `memory.maxSize` | int | | 對話記憶最大輪數 |

### 請求範例

**基本對話：**
```json
{
  "message": "台灣最高的山是哪座？",
  "llmId": "your-model-uuid"
}
```

**使用知識庫：**
```json
{
  "message": "我的保固期限是多久？",
  "llmId": "your-model-uuid",
  "knowledgeId": "your-knowledge-uuid"
}
```

**使用工具 + 多輪對話：**
```json
{
  "message": "幫我搜尋今天的 AI 新聞",
  "llmId": "your-model-uuid",
  "toolIds": ["tool-uuid-1"],
  "memory": {
    "id": "conversation-001",
    "maxSize": 10
  }
}
```

### 回應範例

```json
{
  "code": 200,
  "message": "",
  "data": "台灣最高的山是玉山，海拔 3,952 公尺..."
}
```

## 串流對話（SSE）

```http
POST /llm/chatStreaming
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

請求參數與 `/llm/chat` 相同。回應為 **Server-Sent Events（SSE）** 格式，逐字串流輸出。

```javascript
const response = await fetch('http://localhost/llm/chatStreaming', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
  },
  body: JSON.stringify({
    message: '請介紹台灣的歷史',
    llmId: 'your-model-uuid'
  })
});

const reader = response.body.getReader();
const decoder = new TextDecoder();
while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  process.stdout.write(decoder.decode(value));
}
```

## 自訂助手對話

```http
POST /llm/customAssistantChat
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

自訂助手模式允許更靈活的助手配置，請求參數與 `/llm/chat` 相同。

## 自訂助手串流對話

```http
POST /llm/customAssistantChatStreaming
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

## 上傳檔案

```http
POST /llm/uploadFile
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: multipart/form-data
```

### 表單參數（FilesFromRequest）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `file` | file | | 上傳的檔案（可多個）|
| `fileUrl` | string | | 或提供檔案 URL |
| `knowledgeId` | string | | 知識庫 ID（未提供則自動產生）|
| `name` | string | | 知識庫名稱 |
| `desc` | string | | 知識庫描述 |
| `embeddingModelId` | string | ✓ | 嵌入模型 ID |
| `embeddingStoreId` | string | ✓ | 向量儲存 ID |
| `maxSegmentSize` | int | | 文件切割大小（預設 300）|
| `maxOverlapSize` | int | | 段落重疊大小（預設 50）|

### 回應範例

```json
{
  "code": 200,
  "message": "",
  "data": ["uploaded-file-1.pdf", "uploaded-file-2.txt"]
}
```

## 端點一覽

| 端點 | 說明 |
|------|------|
| `POST /llm/chat` | 一般對話 |
| `POST /llm/chatStreaming` | 串流對話（SSE）|
| `POST /llm/customAssistantChat` | 自訂助手對話 |
| `POST /llm/customAssistantChatStreaming` | 自訂助手串流對話 |
| `POST /llm/uploadFile` | 上傳檔案 |
