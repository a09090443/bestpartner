---
sidebar_position: 2
---

# 助手 API

助手 API 是 BestPartner 的核心功能，提供 AI 對話、串流對話等能力。

## 一般對話

```http
POST /api/assistant/chat
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 請求參數

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `message` | string | ✓ | 使用者輸入的訊息 |
| `modelId` | long | ✓ | AI 模型 ID（對應資料庫設定） |
| `knowledgeBaseId` | long | | 知識庫 ID，啟用 RAG 功能 |
| `toolIds` | array | | 工具 ID 列表（無需自訂設定的工具）|
| `toolSettingIds` | array | | 工具設定 ID 列表（需自訂設定的工具）|
| `mcpServerIds` | array | | MCP Server ID 列表 |
| `systemPrompt` | string | | 系統提示詞，定義 AI 角色與行為 |
| `conversationId` | string | | 對話 ID，用於多輪對話（留空則建立新對話）|

### 請求範例

**基本對話：**
```json
{
  "message": "台灣最高的山是哪座？",
  "modelId": 1
}
```

**使用知識庫：**
```json
{
  "message": "我的保固期限是多久？",
  "modelId": 1,
  "knowledgeBaseId": 1
}
```

**使用搜尋工具：**
```json
{
  "message": "幫我搜尋今天的 AI 新聞",
  "modelId": 1,
  "toolIds": [1]
}
```

**完整功能：**
```json
{
  "message": "根據最新新聞，幫我查詢相關的客戶資料",
  "modelId": 1,
  "knowledgeBaseId": 1,
  "toolIds": [1, 2],
  "toolSettingIds": [1],
  "systemPrompt": "你是一個專業的資料分析助理",
  "conversationId": "conv-001"
}
```

### 回應格式

```json
{
  "reply": "台灣最高的山是玉山（又稱新高山），海拔 3,952 公尺...",
  "conversationId": "conv-abc123",
  "modelId": 1,
  "tokensUsed": 256
}
```

## 串流對話（WebSocket）

使用 WebSocket 可以即時接收 AI 生成的回應，提供更好的使用者體驗。

```
WebSocket: ws://localhost/api/assistant/stream
```

### 連線與傳送訊息

```javascript
const ws = new WebSocket('ws://localhost/api/assistant/stream');

ws.onopen = () => {
  ws.send(JSON.stringify({
    token: 'YOUR_JWT_TOKEN',
    message: '請介紹台灣的歷史',
    modelId: 1
  }));
};

ws.onmessage = (event) => {
  // 逐字接收 AI 生成的文字
  process.stdout.write(event.data);
};

ws.onclose = () => {
  console.log('對話結束');
};
```

### WebSocket 訊息格式

**傳入格式：**

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `token` | string | ✓ | JWT Token |
| `message` | string | ✓ | 使用者訊息 |
| `modelId` | long | ✓ | AI 模型 ID |
| `knowledgeBaseId` | long | | 知識庫 ID |
| `toolIds` | array | | 工具 ID 列表 |

**回傳格式：** 純文字串流，每則訊息為 AI 生成的片段文字，直到連線關閉。

## 多輪對話

透過 `conversationId` 維持對話歷史：

```json
// 第一輪
{
  "message": "我叫小明",
  "modelId": 1
}
// 回應: { "reply": "你好，小明！", "conversationId": "conv-xyz" }

// 第二輪（帶入相同 conversationId）
{
  "message": "你還記得我叫什麼嗎？",
  "modelId": 1,
  "conversationId": "conv-xyz"
}
// 回應: { "reply": "你叫小明。", "conversationId": "conv-xyz" }
```
