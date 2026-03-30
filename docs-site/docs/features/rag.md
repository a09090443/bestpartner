---
sidebar_position: 2
---

# RAG 知識庫

RAG（Retrieval-Augmented Generation）讓 AI 可以根據你上傳的文件內容來回答問題，而不只依賴模型原本的訓練資料。

## 運作原理

```
上傳文件
   └─→ 文件切割成段落
         └─→ 嵌入模型將段落轉為向量
               └─→ 儲存至向量資料庫

使用者提問
   └─→ 問題轉為向量
         └─→ 向量資料庫搜尋相似段落
               └─→ 相關段落 + 問題組合成 Prompt
                     └─→ AI 模型生成回答
```

## 建立知識庫

### 1. 建立知識庫設定

```bash
POST /api/knowledge-base
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "name": "產品手冊",
  "embeddingModelId": 1,
  "vectorStoreType": "IN_MEMORY"
}
```

`vectorStoreType` 可選值：`IN_MEMORY`、`CHROMA`、`MILVUS`

### 2. 上傳文件

支援 PDF、TXT 等格式：

```bash
POST /api/knowledge-base/{id}/upload
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: multipart/form-data

file: [你的文件]
```

### 3. 搜尋知識庫（驗證用）

可以直接測試向量搜尋效果：

```bash
POST /api/knowledge-base/{id}/search
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "query": "產品保固期限是多久？",
  "maxResults": 5
}
```

## 在對話中使用知識庫

在對話 API 中帶入 `knowledgeBaseId`：

```bash
POST /api/assistant/chat
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "message": "產品保固期限是多久？",
  "modelId": 1,
  "knowledgeBaseId": 1
}
```

AI 將基於上傳的文件內容回答，而非依賴訓練資料。

## 刪除文件

```bash
DELETE /api/knowledge-base/{knowledgeBaseId}/document/{documentId}
Authorization: Bearer YOUR_JWT_TOKEN
```

## 向量資料庫選擇建議

| 使用情境 | 建議 |
|---------|------|
| 開發測試 | InMemory（預設，無需設定）|
| 小規模生產 | Chroma（易於部署）|
| 大規模生產 | Milvus（高效能、高可用）|

:::caution InMemory 限制
使用 InMemory 向量儲存時，重啟服務後向量資料會消失，需要重新上傳文件。生產環境請使用 Chroma 或 Milvus。
:::
