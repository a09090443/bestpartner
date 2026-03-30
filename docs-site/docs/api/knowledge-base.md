---
sidebar_position: 3
---

# 知識庫 API

知識庫 API 提供 RAG 相關的管理功能，包含建立知識庫、上傳文件、搜尋等操作。

## 建立知識庫

```http
POST /api/knowledge-base
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 請求參數

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `name` | string | ✓ | 知識庫名稱 |
| `embeddingModelId` | long | ✓ | 嵌入模型 ID |
| `vectorStoreType` | string | ✓ | 向量儲存類型：`IN_MEMORY`、`CHROMA`、`MILVUS` |
| `description` | string | | 知識庫描述 |

### 請求範例

```json
{
  "name": "產品手冊",
  "embeddingModelId": 1,
  "vectorStoreType": "CHROMA",
  "description": "包含所有產品規格與使用說明"
}
```

### 回應範例

```json
{
  "id": 1,
  "name": "產品手冊",
  "embeddingModelId": 1,
  "vectorStoreType": "CHROMA",
  "createdAt": "2025-04-25T10:00:00"
}
```

## 上傳文件

```http
POST /api/knowledge-base/{id}/upload
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: multipart/form-data
```

### 路徑參數

| 參數 | 說明 |
|------|------|
| `id` | 知識庫 ID |

### 表單參數

| 參數 | 類型 | 說明 |
|------|------|------|
| `file` | file | 要上傳的文件（支援 PDF、TXT）|

### 請求範例（curl）

```bash
curl -X POST http://localhost/api/knowledge-base/1/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@product-manual.pdf"
```

## 搜尋知識庫

```http
POST /api/knowledge-base/{id}/search
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 請求參數

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `query` | string | ✓ | 搜尋查詢文字 |
| `maxResults` | int | | 最大回傳結果數（預設 5）|
| `minScore` | double | | 最低相似度分數（0~1，預設 0.5）|

### 請求範例

```json
{
  "query": "產品保固條款",
  "maxResults": 3,
  "minScore": 0.7
}
```

### 回應範例

```json
{
  "results": [
    {
      "content": "本產品自購買日起提供一年保固服務...",
      "score": 0.92,
      "documentId": "doc-001",
      "source": "product-manual.pdf"
    }
  ]
}
```

## 刪除文件

```http
DELETE /api/knowledge-base/{knowledgeBaseId}/document/{documentId}
Authorization: Bearer YOUR_JWT_TOKEN
```

### 路徑參數

| 參數 | 說明 |
|------|------|
| `knowledgeBaseId` | 知識庫 ID |
| `documentId` | 文件 ID |

## 取得知識庫列表

```http
GET /api/knowledge-base
Authorization: Bearer YOUR_JWT_TOKEN
```

### 回應範例

```json
[
  {
    "id": 1,
    "name": "產品手冊",
    "vectorStoreType": "CHROMA",
    "documentCount": 5
  },
  {
    "id": 2,
    "name": "常見問題",
    "vectorStoreType": "IN_MEMORY",
    "documentCount": 12
  }
]
```

## 刪除知識庫

```http
DELETE /api/knowledge-base/{id}
Authorization: Bearer YOUR_JWT_TOKEN
```

:::caution
刪除知識庫會同時刪除所有已上傳的文件與向量資料，此操作不可復原。
:::
