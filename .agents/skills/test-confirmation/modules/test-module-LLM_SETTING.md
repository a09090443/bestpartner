### LLM SETTING — LLM 設定模組（`/llm/setting`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| LLMSET-001 | 新增設定（正常） | P0 | POST | `/llm/setting/save` | | `llmModel` 為巢狀物件（非平層）；測試後刪除 |
| LLMSET-002 | 新增設定（未認證） | P0 | POST | `/llm/setting/save` | | 預期 401 |
| LLMSET-003 | 新增設定（缺 modelType） | P1 | POST | `/llm/setting/save` | | |
| LLMSET-004 | 新增設定（缺 platformId） | P1 | POST | `/llm/setting/save` | | |
| LLMSET-005 | 新增設定（缺 modelName） | P1 | POST | `/llm/setting/save` | | |
| LLMSET-006 | 更新設定（正常） | P1 | POST | `/llm/setting/update` | | |
| LLMSET-007 | 取得設定（正常，無篩選） | P0 | POST | `/llm/setting/get` | | |
| LLMSET-008 | 取得設定（依 llmId 篩選） | P1 | POST | `/llm/setting/get` | | |
| LLMSET-009 | 取得設定（llmId 不存在） | P2 | POST | `/llm/setting/get` | | |
| LLMSET-010 | 刪除設定（正常） | P1 | POST | `/llm/setting/delete` | | |
| LLMSET-011 | 刪除設定（缺 id） | P1 | POST | `/llm/setting/delete` | | |
| LLMSET-012 | 新增平台（admin 正常） | P1 | POST | `/llm/setting/platform/add` | | 只需傳入 `platform` 欄位；測試後刪除 |
| LLMSET-013 | 新增平台（一般用戶拒絕） | P1 | POST | `/llm/setting/platform/add` | | 預期 403 |
| LLMSET-014 | 刪除平台（admin 正常） | P1 | POST | `/llm/setting/platform/delete` | | |
| LLMSET-015 | 刪除平台（一般用戶拒絕） | P1 | POST | `/llm/setting/platform/delete` | | 預期 403 |
| LLMSET-016 | 業務邏輯（刪除後查詢） | P1 | 多步 | `/llm/setting/*` | | save → get → delete → get（確認已消失） |

#### LLM SETTING curl 範本

```bash
# LLMSET-001 新增設定（注意 llmModel 為巢狀物件！）
curl -s -X POST "http://localhost:80/llm/setting/save" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"platformId":"006f1076-b023-4197-b917-70455f2a3501","modelType":"CHAT","llmModel":{"modelName":"deepseek/deepseek-v3.2","apiKey":"sk-or-v1-test-key"}}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"<NEW_LLM_ID>","alias":"","modelType":"CHAT","llmModel":{...}}}
# 記下 id，測試後用 LLMSET-010 刪除

# LLMSET-002 未認證
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/setting/save" \
  -H "Content-Type: application/json" \
  -d '{"platformId":"006f1076-...","modelType":"CHAT","llmModel":{"modelName":"test","apiKey":"test"}}'
# 預期 HTTP 401

# LLMSET-003 缺 modelType
curl -s -X POST "http://localhost:80/llm/setting/save" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"platformId":"006f1076-b023-4197-b917-70455f2a3501","llmModel":{"modelName":"test","apiKey":"test"}}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 modelType 不可為空值","data":null}

# LLMSET-004 缺 platformId
curl -s -X POST "http://localhost:80/llm/setting/save" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"modelType":"CHAT","llmModel":{"modelName":"test","apiKey":"test"}}'
# 預期 HTTP 400

# LLMSET-005 缺 modelName（llmModel 物件中）
curl -s -X POST "http://localhost:80/llm/setting/save" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"platformId":"006f1076-b023-4197-b917-70455f2a3501","modelType":"CHAT","llmModel":{"apiKey":"test"}}'
# 預期 HTTP 400

# LLMSET-006 更新設定
curl -s -X POST "http://localhost:80/llm/setting/update" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<LLM_ID>","alias":"updated_alias","llmModel":{"modelName":"deepseek/deepseek-v3.2","apiKey":"sk-or-v1-new-key"}}'
# 預期 HTTP 200

# LLMSET-007 取得設定（無篩選，取回全部）
curl -s -X POST "http://localhost:80/llm/setting/get" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":[{...},{...}]}

# LLMSET-008 依 llmId 篩選
curl -s -X POST "http://localhost:80/llm/setting/get" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9"}'
# 預期 HTTP 200 → 回傳只含該 llmId 的設定

# LLMSET-010 刪除設定
curl -s -X POST "http://localhost:80/llm/setting/delete" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<LLM_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}

# LLMSET-011 刪除缺 id
curl -s -X POST "http://localhost:80/llm/setting/delete" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 400

# LLMSET-012 新增平台（admin，只需 platform 欄位）
curl -s -X POST "http://localhost:80/llm/setting/platform/add" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"platform":"OPENROUTER"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"<PLATFORM_ID>","platform":"OPENROUTER"}}
# 記下 id，測試後用 LLMSET-014 刪除

# LLMSET-013 一般用戶新增平台
curl -s -X POST "http://localhost:80/llm/setting/platform/add" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"platform":"OPENROUTER"}'
# 預期 HTTP 403

# LLMSET-014 刪除平台（admin）
curl -s -X POST "http://localhost:80/llm/setting/platform/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<PLATFORM_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}
```

---

