---
sidebar_position: 3
description: 第一次啟動 BestPartner 服務並取得 JWT Token 的操作教學，示範服務啟動確認與登入流程。
keywords: [第一次執行, 啟動服務, JWT Token, 登入, 快速上手]
---

# 第一次執行

## 啟動服務

於 repo 根目錄執行（jar 由 [安裝與設定](./installation.md) 第 5 步建置產生）：

```bash
java -jar bestpartner-service/build/bestpartner-service-0.1.8-runner.jar
```

服務預設監聽 **port 80**。確認服務已啟動（存活檢查）：

```bash
curl -i http://localhost/view/chat
# 預期 HTTP/1.1 200，回傳內建聊天測試頁的 HTML
```

也可以直接用瀏覽器開啟 [http://localhost/view/chat](http://localhost/view/chat)。

:::note Swagger 依 profile 開關
`/swagger-ui` 與 `/q/openapi` 只在 dev／docker／sit profile 開放；在 **uat／prod**（以及未指定 profile 時）
回 **404 屬正常行為**，不可用來判斷服務是否啟動——存活檢查請一律用 `/view/chat`。
:::

![服務啟動畫面](pathname:///img/service-start.png)

## 取得 JWT Token

除少數公開端點外，API 都需要 JWT 認證。登入**以 email 作為帳號**，使用預設管理員取得 Token：

```bash
curl -X POST http://localhost/login/ \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@bestpartner.com.tw",
    "password": "admin"
  }'
```

回應為統一的 `ApiResponse` 結構，Token 位於 `data` 欄位：

```json
{
  "code": 200,
  "message": "success",
  "data": "eyJhbGciOiJSUzI1NiJ9.eyJ..."
}
```

之後的請求以 `Authorization: Bearer <data 的值>` 帶入。

:::info 預設帳號
初始化 SQL（`bestpartner-init-data.sql`）包含兩組預設帳號，登入時填 email：

| 角色 | email | 密碼 |
|------|-------|------|
| 管理員 | `admin@bestpartner.com.tw` | `admin` |
| 一般使用者 | `test@partmer.com.tw` | `user` |
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
    "llmId": "YOUR_LLM_ID"
  }'
```

:::info LLM 設定 ID
`llmId` 對應資料庫 `llm_setting` 表中的記錄 ID（UUID），可用 `POST /llm/setting/get` 查詢目前帳號的 LLM 設定。
請先確認資料庫中已設定好 AI 模型（含有效的 API Key）。
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
    llmId: 'YOUR_LLM_ID'
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

## 使用前端介面

要用瀏覽器操作（Workflow 編輯器等），請依 [安裝與設定 › 前端安裝與執行](./installation.md#frontend) 啟動 `bestpartner-ui`，
開啟 http://localhost:5173 後以 `admin@bestpartner.com.tw`／`admin` 登入。

## 下一步

- [測試環境建置](./test-environment.md) — 要在 Linux 從零建立 sit 測試環境時，改看此頁
- [設定 AI 模型](../features/ai-models) — 新增或切換 AI 模型
- [建立知識庫](../features/rag) — 上傳文件，啟用 RAG 功能
- [使用工具](../features/tools) — 讓 AI 可以搜尋網路或查詢資料庫
- [API 文件](../api/authentication) — 完整 API 參考
