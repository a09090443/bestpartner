### VECTOR — 向量模組（`/llm/vector`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| VEC-001 | 新增向量設定（正常） | P0 | POST | `/llm/vector/save` | | 需含 `vectorStoreType`（頂層）+ `vectorStore`（巢狀）；測試後刪除 |
| VEC-002 | 新增向量設定（未認證） | P0 | POST | `/llm/vector/save` | | 預期 401 |
| VEC-003 | 更新向量設定（正常） | P1 | POST | `/llm/vector/update` | | |
| VEC-004 | 取得知識庫（正常） | P1 | POST | `/llm/vector/getKnowledgeStore` | | |
| VEC-005 | 上傳向量文件（正常） | P0 | POST | `/llm/vector/uploadFiles` | | ⚠️ 需 Ollama embedding + Milvus/Chroma；未啟動則 ⏭️ |
| VEC-006 | 上傳向量文件（未認證） | P0 | POST | `/llm/vector/uploadFiles` | | 預期 401 |
| VEC-007 | 上傳向量文件（無附件） | P2 | POST | `/llm/vector/uploadFiles` | | |
| VEC-008 | 搜尋向量資料（正常） | P0 | POST | `/llm/vector/getDataFromEmbeddingStore` | | ⚠️ 需已有向量資料；未啟動則 ⏭️ |
| VEC-009 | 搜尋向量資料（缺 knowledgeId） | P1 | POST | `/llm/vector/getDataFromEmbeddingStore` | | |
| VEC-010 | 搜尋向量資料（缺 content） | P1 | POST | `/llm/vector/getDataFromEmbeddingStore` | | |
| VEC-011 | 搜尋向量資料（knowledgeId 不存在） | P2 | POST | `/llm/vector/getDataFromEmbeddingStore` | | |
| VEC-012 | 刪除向量資料（正常） | P1 | DELETE | `/llm/vector/deleteData` | | |
| VEC-013 | 刪除向量資料（未認證） | P1 | DELETE | `/llm/vector/deleteData` | | 預期 401 |
| VEC-014 | 業務邏輯（上傳後可搜尋） | P1 | 多步 | `/llm/vector/*` | | ⚠️ 需 Ollama + 向量 DB；未啟動則 ⏭️ |

#### VECTOR curl 範本

```bash
# VEC-001 新增向量設定（注意：vectorStoreType 在頂層，vectorStore 為巢狀物件）
curl -s -X POST "http://localhost:80/llm/vector/save" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"alias":"test_vector","vectorStoreType":"CHROMA","vectorStore":{"collectionName":"test_collection","dimension":384,"url":"http://localhost:8000"}}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":"向量資料庫設定已儲存"}
# 記下 id（需另查詢 getKnowledgeStore），測試後刪除

# VEC-002 未認證
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/vector/save" \
  -H "Content-Type: application/json" \
  -d '{"vectorStoreType":"CHROMA","vectorStore":{"collectionName":"test","dimension":384}}'
# 預期 HTTP 401

# VEC-003 更新向量設定
curl -s -X POST "http://localhost:80/llm/vector/update" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<VECTOR_STORE_ID>","alias":"updated_vector","vectorStoreType":"CHROMA","vectorStore":{"collectionName":"updated_collection","dimension":384,"url":"http://localhost:8000"}}'
# 預期 HTTP 200

# VEC-004 取得知識庫（依 knowledgeId 查詢）
curl -s -X POST "http://localhost:80/llm/vector/getKnowledgeStore" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"knowledgeId":"<KNOWLEDGE_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":[...]}

# VEC-005 上傳向量文件（需 Ollama + Milvus/Chroma 運行中）
echo "This is test content." > /tmp/vec_test.txt
curl -s -X POST "http://localhost:80/llm/vector/uploadFiles" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -F "file=@/tmp/vec_test.txt" \
  -F "embeddingModelId=<EMBEDDING_MODEL_ID>" \
  -F "embeddingStoreId=<VECTOR_STORE_ID>"
# 預期 HTTP 200 → {"code":200,"message":"success","data":"<knowledgeId>"}
# 若基礎設施未啟動 → ⏭️

# VEC-006 上傳未認證
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/vector/uploadFiles" \
  -F "file=@/tmp/vec_test.txt" \
  -F "embeddingModelId=xxx" \
  -F "embeddingStoreId=xxx"
# 預期 HTTP 401

# VEC-007 無附件
curl -s -X POST "http://localhost:80/llm/vector/uploadFiles" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -F "embeddingModelId=xxx" \
  -F "embeddingStoreId=xxx"
# 預期 HTTP 400 → {"code":400,"message":"欄位 files 不可為空值","data":null}

# VEC-008 向量相似度搜尋（需已有資料）
curl -s -X POST "http://localhost:80/llm/vector/getDataFromEmbeddingStore" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"knowledgeId":"<KNOWLEDGE_ID>","content":"test search query"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":[{"content":"...","score":...}]}

# VEC-009 缺 knowledgeId
curl -s -X POST "http://localhost:80/llm/vector/getDataFromEmbeddingStore" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"test"}'
# 預期 HTTP 400

# VEC-010 缺 content
curl -s -X POST "http://localhost:80/llm/vector/getDataFromEmbeddingStore" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"knowledgeId":"xxx"}'
# 預期 HTTP 400

# VEC-012 刪除向量資料
curl -s -X DELETE "http://localhost:80/llm/vector/deleteData" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"knowledgeId":"<KNOWLEDGE_ID>"}'
# 預期 HTTP 200

# VEC-013 刪除未認證
curl -s -o /dev/null -w "%{http_code}" -X DELETE "http://localhost:80/llm/vector/deleteData" \
  -H "Content-Type: application/json" \
  -d '{"knowledgeId":"xxx"}'
# 預期 HTTP 401
```

---

