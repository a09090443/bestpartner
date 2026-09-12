### CHAT — 聊天模組（`/llm`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| CHAT-001 | 同步聊天（正常請求） | P0 | POST | `/llm/chat` | | 使用 `TEST_USER_TOKEN` + `CHAT_LLM_ID` |
| CHAT-002 | 同步聊天（未認證） | P0 | POST | `/llm/chat` | | |
| CHAT-003 | 同步聊天（缺 llmId） | P1 | POST | `/llm/chat` | | |
| CHAT-004 | 同步聊天（缺 message） | P1 | POST | `/llm/chat` | | |
| CHAT-005 | 同步聊天（llmId 不存在） | P1 | POST | `/llm/chat` | | |
| CHAT-006 | 串流聊天（正常請求） | P0 | POST | `/llm/chatStreaming` | | 使用 `STREAMING_LLM_ID` |
| CHAT-007 | 串流聊天（未認證） | P0 | POST | `/llm/chatStreaming` | | |
| CHAT-008 | 自定義助手聊天（正常，含 memory + promptContent） | P1 | POST | `/llm/customAssistantChat` | | `promptContent` 為必填 |
| CHAT-009 | 自定義助手聊天（缺 promptContent） | P1 | POST | `/llm/customAssistantChat` | | |
| CHAT-010 | 自定義助手聊天（含 mcpSettingIds） | P2 | POST | `/llm/customAssistantChat` | | |
| CHAT-011 | 自定義助手串流（正常） | P1 | POST | `/llm/customAssistantChatStreaming` | | |
| CHAT-012 | 上傳檔案（正常） | P1 | POST | `/llm/uploadFile` | | multipart/form-data |
| CHAT-013 | 上傳檔案（未認證） | P1 | POST | `/llm/uploadFile` | | |
| CHAT-014 | 上傳檔案（無附件） | P2 | POST | `/llm/uploadFile` | | |
| CHAT-015 | 上傳檔案（超大檔案） | P2 | POST | `/llm/uploadFile` | | |
| CHAT-016 | 同步聊天（message 為空） | P2 | POST | `/llm/chat` | | |
| CHAT-017 | 同步聊天（admin 帳號，使用 admin 自己的 llmId） | P1 | POST | `/llm/chat` | | 注意：llmId 必須屬於 admin 帳號 |
| CHAT-018 | 串流聊天（admin 帳號，使用 admin 自己的 llmId） | P1 | POST | `/llm/chatStreaming` | | 注意：llmId 必須屬於 admin 帳號 |

#### CHAT curl 範本

```bash
# CHAT-001 同步聊天（test_user）
curl -s -X POST "http://localhost:80/llm/chat" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Hello"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":"Hello! How can I assist..."}

# CHAT-002 未認證
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/chat" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Hello"}'
# 預期 HTTP 401

# CHAT-003 缺 llmId
curl -s -X POST "http://localhost:80/llm/chat" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 llmId 不可為空值","data":null}

# CHAT-004 缺 message
curl -s -X POST "http://localhost:80/llm/chat" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 message 不可為空值","data":null}

# CHAT-005 llmId 不存在（或不屬於自己）
curl -s -X POST "http://localhost:80/llm/chat" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"00000000-0000-0000-0000-000000000000","message":"Hello"}'
# 預期 HTTP 400 → {"code":400,"message":"Please verify the LLM setting you are accessing exists","data":null}

# CHAT-006 串流聊天（SSE）
curl -s -X POST "http://localhost:80/llm/chatStreaming" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"2b44c811-d38c-417b-89d4-ae675b3093f5","message":"Hello"}'
# 預期 HTTP 200 SSE → data:Hello\ndata:!\n...

# CHAT-007 串流未認證
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/chatStreaming" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"2b44c811-d38c-417b-89d4-ae675b3093f5","message":"Hello"}'
# 預期 HTTP 401

# CHAT-008 customAssistantChat（正常，必填 promptContent + memory）
curl -s -X POST "http://localhost:80/llm/customAssistantChat" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Say hi","memory":{"id":"session-001","maxSize":5},"promptContent":"You are a helpful assistant."}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":"Hi! How can I help you?"}

# CHAT-009 customAssistantChat 缺 promptContent
curl -s -X POST "http://localhost:80/llm/customAssistantChat" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Say hi","memory":{"id":"session-001","maxSize":5}}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 promptContent 不可為空值","data":null}

# CHAT-010 customAssistantChat 含 mcpSettingIds
# （先用 MCP-012 saveSetting 取得 userSettingId）
curl -s -X POST "http://localhost:80/llm/customAssistantChat" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"What date is it?","memory":{"id":"mcp-session","maxSize":5},"promptContent":"You are helpful.","mcpSettingIds":["<MCP_USER_SETTING_ID>"]}'
# 預期 HTTP 200 → 含日期資訊的回覆

# CHAT-011 customAssistantChatStreaming
curl -s -X POST "http://localhost:80/llm/customAssistantChatStreaming" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"2b44c811-d38c-417b-89d4-ae675b3093f5","message":"Say hi","memory":{"id":"stream-session","maxSize":5},"promptContent":"You are helpful."}'
# 預期 HTTP 200 SSE

# CHAT-012 上傳檔案
echo "Hello World" > /tmp/test_upload.txt
curl -s -X POST "http://localhost:80/llm/uploadFile" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -F "file=@/tmp/test_upload.txt"
# 預期 HTTP 200 → {"code":200,"message":"success","data":["test_upload.txt"]}

# CHAT-013 上傳檔案未認證
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/uploadFile" \
  -F "file=@/tmp/test_upload.txt"
# 預期 HTTP 401

# CHAT-016 message 為空字串
curl -s -X POST "http://localhost:80/llm/chat" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":""}'
# 預期 HTTP 400（空字串驗證）

# CHAT-017 admin 帳號聊天（注意：需使用 admin 帳號自己的 llmId）
# 先查詢 admin 的 llmId：
# curl -s -X POST "http://localhost:80/llm/setting/get" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{}'
# 取得 ADMIN_CHAT_LLM_ID 後：
curl -s -X POST "http://localhost:80/llm/chat" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"<ADMIN_CHAT_LLM_ID>","message":"Hello"}'
# 預期 HTTP 200
```

---

