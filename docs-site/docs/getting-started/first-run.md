---
sidebar_position: 3
---

# 第一次執行

## 啟動服務

```bash
java -jar bestpartner-service/build/bestpartner-service-0.1.6-SNAPSHOT-runner.jar
```

服務啟動後，可在瀏覽器開啟 [http://localhost](http://localhost) 確認服務狀態。

![服務啟動畫面](pathname:///img/service-start.png)

## 取得 JWT Token

所有 API 都需要 JWT 認證。使用預設管理員帳號取得 Token：

```bash
curl -X POST http://localhost/login/ \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin"
  }'
```

回應範例：

```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9.eyJ..."
}
```

:::info 預設帳號
初始化 SQL 包含兩個預設帳號：
- `admin` / `admin`（管理員）
- `test_user` / `test_user`（一般使用者）
:::

:::tip 使用 Postman
建議匯入 `docs/postman/basepartner.postman_collection.json`，其中已包含所有 API 範例及 Token 自動填入設定。
:::

## 第一個對話請求

```bash
curl -X POST http://localhost/llm/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "message": "你好，請介紹一下你自己",
    "modelId": "YOUR_MODEL_ID"
  }'
```

:::info 模型 ID
`modelId` 對應資料庫 `llm_setting` 表中的記錄 ID（UUID）。請先確認資料庫中已設定好 AI 模型。
:::

## 主要對話 API

BestPartner 提供多種對話模式：

| 端點 | 說明 |
|------|------|
| `POST /llm/chat` | 一般對話（等待完整回應）|
| `POST /llm/chatStreaming` | 串流對話（SSE，逐字輸出）|
| `POST /llm/customAssistantChat` | 自訂助手對話 |
| `POST /llm/customAssistantChatStreaming` | 自訂助手串流對話 |
| `POST /llm/uploadFile` | 上傳檔案至對話 |

## 串流對話（Server-Sent Events）

BestPartner 串流功能使用 **SSE（Server-Sent Events）** 而非 WebSocket，透過 HTTP 即時接收 AI 生成的文字：

```javascript
const response = await fetch('http://localhost/llm/chatStreaming', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
  },
  body: JSON.stringify({
    message: '請介紹台灣的歷史',
    modelId: 'YOUR_MODEL_ID'
  })
});

const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  process.stdout.write(decoder.decode(value)); // 逐字輸出
}
```

## 下一步

- [設定 AI 模型](../features/ai-models) — 新增或切換 AI 模型
- [建立知識庫](../features/rag) — 上傳文件，啟用 RAG 功能
- [使用工具](../features/tools) — 讓 AI 可以搜尋網路或查詢資料庫
- [API 文件](../api/authentication) — 完整 API 參考
