---
sidebar_position: 3
---

# 知識庫 API

知識庫 API 提供 RAG 相關的管理功能。所有端點路徑前綴為 `/llm/vector`。

## 通用回應格式

```json
{
  "code": 200,
  "message": "",
  "data": { ... }
}
```

## 儲存向量資料庫設定

```http
POST /llm/vector/save
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 請求參數（VectorStoreDTO）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `alias` | string | ✓ | 向量資料庫別名 |
| `vectorStoreType` | string | ✓ | 類型：`IN_MEMORY`、`CHROMA`、`MILVUS` |
| `knowledgeId` | string | | 知識庫 ID |
| `vectorStore.collectionName` | string | ✓ | 向量集合名稱 |
| `vectorStore.dimension` | int | ✓ | 向量維度 |
| `vectorStore.url` | string | | 向量資料庫 URL（Chroma/Milvus 需填）|
| `vectorStore.username` | string | | 資料庫帳號（如有）|
| `vectorStore.password` | string | | 資料庫密碼（如有）|
| `vectorStore.requestLog` | boolean | | 是否記錄請求 log（預設 false）|
| `vectorStore.responseLog` | boolean | | 是否記錄回應 log（預設 false）|

### 請求範例

```json
{
  "alias": "產品手冊知識庫",
  "vectorStoreType": "CHROMA",
  "vectorStore": {
    "collectionName": "product-manual",
    "dimension": 1536,
    "url": "http://localhost:8000"
  }
}
```

## 更新向量資料庫設定

```http
POST /llm/vector/update
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

請求參數與 `save` 相同，需加上 `id` 欄位。

## 取得知識庫文件列表

```http
POST /llm/vector/getKnowledgeStore
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

## 上傳文件並嵌入

```http
POST /llm/vector/uploadFiles
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: multipart/form-data
```

### 表單參數

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `file` | file | | 上傳的文件（支援 PDF、TXT 等）|
| `embeddingModelId` | string | ✓ | 嵌入模型 ID |
| `embeddingStoreId` | string | ✓ | 向量儲存 ID |

### curl 範例

```bash
curl -X POST http://localhost/llm/vector/uploadFiles \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@product-manual.pdf" \
  -F "embeddingModelId=your-embedding-model-uuid" \
  -F "embeddingStoreId=your-vector-store-uuid"
```

## 搜尋向量資料庫

```http
POST /llm/vector/getDataFromEmbeddingStore
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 請求參數（LLMDocDTO）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `knowledgeId` | string | ✓ | 知識庫 ID |
| `content` | string | ✓ | 搜尋查詢文字 |

### 請求範例

```json
{
  "knowledgeId": "your-knowledge-uuid",
  "content": "產品保固條款"
}
```

## 刪除文件資料

```http
DELETE /llm/vector/deleteData
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 請求參數

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `knowledgeId` | string | ✓ | 知識庫 ID |
| `docIds` | array | | 要刪除的文件 ID 列表（未提供則刪除整個知識庫）|

### 請求範例

```json
{
  "knowledgeId": "your-knowledge-uuid",
  "docIds": ["doc-id-1", "doc-id-2"]
}
```

## 端點一覽

| 端點 | 說明 |
|------|------|
| `POST /llm/vector/save` | 儲存向量資料庫設定 |
| `POST /llm/vector/update` | 更新向量資料庫設定 |
| `POST /llm/vector/getKnowledgeStore` | 取得知識庫文件列表 |
| `POST /llm/vector/uploadFiles` | 上傳文件並嵌入 |
| `POST /llm/vector/getDataFromEmbeddingStore` | 搜尋向量資料庫 |
| `DELETE /llm/vector/deleteData` | 刪除文件資料 |
