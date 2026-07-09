---
sidebar_position: 2
description: BestPartner RAG 知識庫功能說明，示範文件切割、嵌入向量化、儲存至 EmbeddingStore 與相似度檢索的完整運作原理。
keywords: [RAG, 知識庫, 向量資料庫, Embedding, 檢索增強生成, Chroma, Milvus]
---

# RAG 知識庫

RAG（Retrieval-Augmented Generation）讓 AI 可以根據你上傳的文件內容來回答問題，而不只依賴模型原本的訓練資料。

## 運作原理

```
上傳文件
   └─→ 文件切割成段落（embeddingDocs）
         └─→ 嵌入模型將段落轉為向量
               └─→ 儲存至 EmbeddingStore

使用者提問
   └─→ 問題轉為向量（embeddingStoreSearch）
         └─→ 向量資料庫搜尋相似段落
               └─→ buildRetrievalAugmentor 組合 Prompt
                     └─→ AI 模型生成回答
```

## API 端點

所有知識庫相關 API 路徑前綴為 `/llm/vector`。

### 儲存向量資料庫設定

```http
POST /llm/vector/save
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 取得知識庫文件列表

```http
POST /llm/vector/getKnowledgeStore
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 上傳文件並嵌入

```http
POST /llm/vector/uploadFiles
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: multipart/form-data
```

**表單參數：**
- `file`：要上傳的文件（支援 PDF、TXT 等）

**curl 範例：**

```bash
curl -X POST http://localhost/llm/vector/uploadFiles \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@product-manual.pdf"
```

### 搜尋向量資料庫

```http
POST /llm/vector/getDataFromEmbeddingStore
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 刪除文件資料

```http
DELETE /llm/vector/deleteData
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 更新向量資料庫設定

```http
POST /llm/vector/update
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
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

:::info 嵌入模型
RAG 功能需要嵌入模型（Embedding Model）。請先在資料庫設定好 `EMBEDDING` 類型的模型（支援 OpenAI、Gemini、Ollama），詳見 [AI 模型設定](./ai-models)。
:::
