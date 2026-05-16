### ADMIN CHAT — 管理員聊天模組（`/llm/admin`）

> ⚙️ **此模組使用 `application.properties` 設定檔中的系統預設模型**（`ai-platform.openrouter.*` 等），與資料庫中的使用者 LLM Setting 無關。
> 測試前請確認設定檔已正確設定對應平台的 `api-key`、`model-name` 等參數，否則標記為 ⏭️。

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| ADCHAT-001 | 管理員聊天（admin 正常） | P0 | POST | `/llm/admin/chat` | | ⚠️ 需環境變數設定 API Key；未設定則 500 → ⏭️ |
| ADCHAT-002 | 管理員聊天（一般用戶存取） | P0 | POST | `/llm/admin/chat` | | 預期 403 |
| ADCHAT-003 | 管理員聊天（未認證） | P0 | POST | `/llm/admin/chat` | | 預期 401 |
| ADCHAT-004 | 管理員自定義助手聊天（admin 正常） | P1 | POST | `/llm/admin/customAssistantChat` | | ⚠️ 同 ADCHAT-001 |
| ADCHAT-005 | 管理員自定義助手聊天（一般用戶） | P1 | POST | `/llm/admin/customAssistantChat` | | 預期 403 |

#### ADMIN CHAT curl 範本

```bash
# ADCHAT-001 admin 聊天（需 OPENROUTER_API_KEY 環境變數；未設定則 ⏭️）
curl -s -X POST "http://localhost:80/llm/admin/chat" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"platform":"OPENROUTER","message":"Hello"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":"..."}
# 若 500 → 確認 application.properties 中 OPENROUTER api-key 已設定

# ADCHAT-002 一般用戶存取 admin chat
curl -s -X POST "http://localhost:80/llm/admin/chat" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"platform":"OPENROUTER","message":"Hello"}'
# 預期 HTTP 403 → {"code":403,"message":"權限錯誤","data":null}

# ADCHAT-003 未認證
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/admin/chat" \
  -H "Content-Type: application/json" \
  -d '{"platform":"OPENROUTER","message":"Hello"}'
# 預期 HTTP 401

# ADCHAT-004 admin customAssistantChat（需環境變數）
curl -s -X POST "http://localhost:80/llm/admin/customAssistantChat" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"platform":"OPENROUTER","message":"Hello"}'
# 預期 HTTP 200 SSE（若環境設定正確）

# ADCHAT-005 一般用戶存取 admin customAssistantChat
curl -s -X POST "http://localhost:80/llm/admin/customAssistantChat" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"platform":"OPENROUTER","message":"Hello"}'
# 預期 HTTP 403 → {"code":403,"message":"權限錯誤","data":null}
```

---

