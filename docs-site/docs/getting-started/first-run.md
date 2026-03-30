---
sidebar_position: 3
---

# 第一次執行

## 啟動服務

```bash
java -jar bestpartner-service/build/bestpartner-service-0.10-SNAPSHOT-runner.jar
```

服務啟動後，可在瀏覽器開啟 [http://localhost](http://localhost) 確認服務狀態。

![服務啟動畫面](../../static/img/service-start.png)

## 取得 JWT Token

所有 API 都需要 JWT 認證。使用預設管理員帳號取得 Token：

```bash
curl -X POST http://localhost/login \
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

:::tip 使用 Postman
建議匯入 `docs/postman/basepartner.postman_collection.json`，其中已包含所有 API 範例及 Token 自動填入設定。
:::

## 第一個對話請求

取得 Token 後，試試發送第一個對話：

```bash
curl -X POST http://localhost/api/assistant/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "message": "你好，請介紹一下你自己",
    "modelId": 1
  }'
```

:::info 模型 ID
`modelId` 對應資料庫中設定的 AI 模型。初始化 SQL 已包含預設模型設定，請確認資料庫匯入成功。
:::

## 串流對話（WebSocket）

BestPartner 支援 WebSocket 串流回應，可以即時顯示 AI 生成的文字：

```javascript
const ws = new WebSocket('ws://localhost/api/assistant/stream');
ws.onopen = () => {
  ws.send(JSON.stringify({
    token: 'YOUR_JWT_TOKEN',
    message: '你好',
    modelId: 1
  }));
};
ws.onmessage = (event) => {
  console.log(event.data); // 逐字接收 AI 回應
};
```

## 下一步

- [設定 AI 模型](../features/ai-models) — 新增或切換 AI 模型
- [建立知識庫](../features/rag) — 上傳文件，啟用 RAG 功能
- [使用工具](../features/tools) — 讓 AI 可以搜尋網路或查詢資料庫
- [API 文件](../api/authentication) — 完整 API 參考
