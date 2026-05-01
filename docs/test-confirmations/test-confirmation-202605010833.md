# BestPartner API 測試功能確認表

> 本文件為定期測試記錄模板，每次測試週期開始前請複製此文件並填寫狀態欄位。
> 詳細測試說明請參考 `.claude/skills/test-confirmation/api-test-examples.md`。

---

## 使用說明

### 狀態符號

| 符號 | 意義 |
|------|------|
| ✅ | Pass — 測試通過 |
| ❌ | Fail — 測試失敗（請在「問題追蹤區」補充說明） |
| ⏭️ | Skip — 本次略過（請在備註欄說明原因） |
| — | 本次週期不適用 |

### 填寫流程

1. 複製此檔案，重新命名為 `test-confirmation-YYYYMMDDHHmm.md`
2. 填寫「測試週期資訊」
3. 取得測試帳號 JWT token（見「取得 Token」段落）
4. 按模組依序執行測試，參照「curl 範本」小節，逐項填入狀態
5. 測試完成後更新「測試結果摘要」
6. 所有 ❌ 項目需在「問題追蹤區」建立記錄

### 取得 Token（每次測試前先執行）

```bash
# admin token
export ADMIN_TOKEN=$(curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw","password":"admin"}' \
  | grep -o '"data":"[^"]*' | cut -d'"' -f4)

# user token
export USER_TOKEN=$(curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@bestpartner.com.tw","password":"user"}' \
  | grep -o '"data":"[^"]*' | cut -d'"' -f4)

# test_user token
export TEST_USER_TOKEN=$(curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@partmer.com.tw","password":"user"}' \
  | grep -o '"data":"[^"]*' | cut -d'"' -f4)

echo "ADMIN_TOKEN=${ADMIN_TOKEN:0:30}..."
echo "USER_TOKEN=${USER_TOKEN:0:30}..."
echo "TEST_USER_TOKEN=${TEST_USER_TOKEN:0:30}..."
```

---

## 測試週期資訊

| 項目 | 內容 |
|------|------|
| 測試日期 | 2026-05-01 08:33 |
| 服務版本 | 0.1.7-SNAPSHOT |
| 測試環境 | dev |
| LLM 平台 | Anthropic |
| LLM Setting ID | |
| Base URL | `http://localhost:80` |
| 測試結束日期 | |

---

## 預設測試帳號參考

| 帳號 | Email | 密碼 | JWT groups | 可測試場景 |
|------|-------|------|-----------|-----------|
| `admin` | `admin@bestpartner.com.tw` | `admin` | `admin`, `user-read`, `user-write` | 所有端點（公開、`@Authenticated`、`@RolesAllowed("admin")`） |
| `user` | `user@bestpartner.com.tw` | `user` | `user-read`, `user-write` | `@Authenticated` 及 `@RolesAllowed("user-read")` 端點；admin 端點預期 403 |
| `test_user` | `test@partmer.com.tw` | `user` | （空） | 無角色端點測試；`/llm/user/get` 使用此 token 會返回 400（無 user entity） |

### 已知系統配置缺陷（測試前必讀）

| 缺陷 | 影響端點 | 影響測試 ID | 預期行為 |
|------|---------|------------|---------|
| `OPENROUTER_API_KEY` 環境變數未設定 | `POST /llm/admin/chat`、`POST /llm/admin/customAssistantChat` | ADCHAT-001、ADCHAT-004 | `llmStore.chatModelMap` 為空 → 500；標記為 ⏭️ 環境配置限制 |
| `test_user` 帳號無 `llm_user_role` 記錄 | `POST /llm/user/get`（使用 test_user token） | USER-007（test_user） | `findUserInfo()` INNER JOIN 找不到記錄 → 400；改用 `user` token 可正常使用 |
| TOOL `saveSetting`/`updateSetting` 實際需要認證 | `POST /llm/tool/saveSetting`、`POST /llm/tool/updateSetting` | TOOL-011、TOOL-013 | API 文件標記公開，但 Service 層呼叫 `validateLoggedInUser()`；必須帶 `Authorization` header |
| VEC uploadFiles / getDataFromEmbeddingStore 需 Ollama + Milvus/Chroma | `/llm/vector/uploadFiles`、`/llm/vector/getDataFromEmbeddingStore` | VEC-005、VEC-008、VEC-014 | 基礎設施未啟動時 ⏭️ Skip |
| JWT Refresh 安全漏洞（P0） | 所有需認證端點 | SEC-004 | 竄改 JWT payload 後利用 Refresh 機制可取得有效 admin token（**Release Blocker**） |

### 常用已知 ID 參考（test_user 帳號）

> 以下 ID 使用 `test_user` 帳號（`test@partmer.com.tw`）登入後查詢取得。

| 名稱 | ID | 說明 |
|------|-----|------|
| `CHAT_LLM_ID` | `583b9222-8cb0-4109-b072-5f0fd1e9fed9` | test_user 的 CHAT 類型 LLM 設定 |
| `STREAMING_LLM_ID` | `2b44c811-d38c-417b-89d4-ae675b3093f5` | test_user 的 STREAMING_CHAT 類型 LLM 設定 |
| `OPENROUTER_PLATFORM_ID` | `006f1076-b023-4197-b917-70455f2a3501` | OpenRouter 平台 ID |
| `MCP_DATE_ID` | `4b9ba306-2fc5-4aa6-9e54-22bce834e021` | date MCP server |
| `TOOL_DATE_ID` | `3737ec4a-2c88-490e-8301-ebc611f2433f` | date 工具 |
| `CATEGORY_DATE_ID` | `ea8a08e2-342d-4ade-9579-127d2d1443c5` | 工具分類 date |

---

## 測試結果摘要

> 測試完成後手動更新本表

| 模組 | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|------|------|--------|--------|---------|---------|
| AUTH | 10 | | | | |
| CHAT | 18 | | | | |
| ADMIN CHAT | 5 | | | | |
| USER | 17 | | | | |
| LLM SETTING | 16 | | | | |
| VECTOR | 14 | | | | |
| TOOL | 19 | | | | |
| MCP | 19 | | | | |
| PERMISSION | 11 | | | | |
| SYSTEM SETTING | 13 | | | | |
| 跨模組整合 | 6 | | | | |
| 通用安全性 | 6 | | | | |
| **合計** | **154** | | | | |

### 通過標準

| 優先級 | 通過標準 | 說明 |
|--------|--------|------|
| P0 | 100% | 任何失敗均為 Release Blocker |
| P1 | ≥ 95% | 已知失敗需有追蹤 Issue |
| P2 | ≥ 80% | 剩餘列為 Tech Debt |

---

## 模組測試清單

---

### AUTH — 認證模組（`/login`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| AUTH-001 | 正常登入（有效帳密） | P0 | POST | `/login/` | | |
| AUTH-002 | 驗證 JWT 有效 | P0 | POST | `/login/check` | | 需 `user-read` 角色；test_user 預期 403 |
| AUTH-003 | 錯誤登入（密碼錯誤） | P1 | POST | `/login/` | | |
| AUTH-004 | 錯誤登入（信箱不存在） | P1 | POST | `/login/` | | |
| AUTH-005 | 必填驗證（缺 email） | P1 | POST | `/login/` | | |
| AUTH-006 | 必填驗證（缺 password） | P1 | POST | `/login/` | | |
| AUTH-007 | 必填驗證（空 body） | P1 | POST | `/login/` | | |
| AUTH-008 | email 格式不合法 | P2 | POST | `/login/` | | |
| AUTH-009 | JWT check（無效 token） | P1 | POST | `/login/check` | | |
| AUTH-010 | 邊界條件（超長 password） | P2 | POST | `/login/` | | |

#### AUTH curl 範本

```bash
# AUTH-001 正常登入（admin）
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw","password":"admin"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":"eyJ0eXAi...（JWT token）"}

# AUTH-001b 正常登入（user）
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@bestpartner.com.tw","password":"user"}'
# 預期 HTTP 200

# AUTH-002 驗證 JWT
curl -s -X POST "http://localhost:80/login/check" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":"check"}

# AUTH-002b test_user 呼叫 check（缺 user-read 角色）
curl -s -X POST "http://localhost:80/login/check" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 403 → {"code":403,"message":"權限錯誤","data":null}

# AUTH-003 密碼錯誤
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw","password":"wrongpassword"}'
# 預期 HTTP 401 → {"code":401,"message":"密碼錯誤","data":null}

# AUTH-004 信箱不存在
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"notexist@example.com","password":"anypassword"}'
# 預期 HTTP 401 → {"code":401,"message":"帳號不存在","data":null}

# AUTH-005 缺 email
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"password":"admin"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 email 不可為空值","data":null}

# AUTH-006 缺 password
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 password 不可為空值","data":null}

# AUTH-007 空 body
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 400

# AUTH-008 email 格式錯誤
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"not-an-email","password":"admin"}'
# 預期 HTTP 400 or 401

# AUTH-009 無效 token 呼叫 check
curl -s -X POST "http://localhost:80/login/check" \
  -H "Authorization: Bearer invalidtoken" \
  -H "Content-Type: application/json" -d '{}'
# 預期 HTTP 401

# AUTH-010 超長 password
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@bestpartner.com.tw\",\"password\":\"$(python3 -c 'print("x"*1000)')\"}"
# 預期 HTTP 400 或 401（系統行為）
```

---

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

### USER — 用戶模組（`/llm/user`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| USER-001 | 註冊（正常新帳號） | P0 | POST | `/llm/user/register` | | 測試後呼叫 DELETE 清理 |
| USER-002 | 註冊（重複 email） | P1 | POST | `/llm/user/register` | | |
| USER-003 | 註冊（缺 username） | P1 | POST | `/llm/user/register` | | |
| USER-004 | 註冊（缺 password） | P1 | POST | `/llm/user/register` | | |
| USER-005 | 註冊（缺 email） | P1 | POST | `/llm/user/register` | | |
| USER-006 | 註冊（非法 email） | P2 | POST | `/llm/user/register` | | |
| USER-007 | 取得用戶資訊（user token，正常） | P0 | POST | `/llm/user/get` | | 使用 `USER_TOKEN`；test_user token 會返回 400 |
| USER-008 | 取得用戶資訊（未認證） | P0 | POST | `/llm/user/get` | | 預期 401 |
| USER-009 | 更新用戶（正常） | P1 | POST | `/llm/user/update` | | |
| USER-010 | 更新用戶（缺 id） | P1 | POST | `/llm/user/update` | | |
| USER-011 | 更新用戶（id 不存在） | P1 | POST | `/llm/user/update` | | |
| USER-012 | 切換狀態（admin 正常） | P1 | POST | `/llm/user/switchStatus` | | 需 `ADMIN_TOKEN`（`@RolesAllowed("admin")`） |
| USER-013 | 切換狀態（缺 status） | P1 | POST | `/llm/user/switchStatus` | | |
| USER-014 | 刪除用戶（admin 正常） | P1 | DELETE | `/llm/user/delete` | | 需 `ADMIN_TOKEN`（`@RolesAllowed("admin")`） |
| USER-015 | 刪除用戶（缺 id） | P1 | DELETE | `/llm/user/delete` | | |
| USER-016 | 刪除用戶（id 不存在） | P2 | DELETE | `/llm/user/delete` | | |
| USER-017 | 業務邏輯（刪除後無法取得） | P1 | 多步 | `/llm/user/*` | | register → login → get → update → delete |

#### USER curl 範本

```bash
# USER-001 正常註冊（使用時間戳避免 email 衝突）
TS=$(date +%s)
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test_reg_$TS\",\"password\":\"testpass\",\"email\":\"test_reg_$TS@test.com\"}"
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"...","username":"test_reg_...","status":"ACTIVE"}}
# 記下 id，測試後用 USER-014 刪除

# USER-002 重複 email（先執行 USER-001 後再執行）
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"another_user","password":"testpass","email":"test_reg_<同上TS>@test.com"}'
# 預期 HTTP 400 → {"code":400,"message":"Database processing error","data":null}

# USER-003 缺 username
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d '{"password":"testpass","email":"missing_username@test.com"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 username 不可為空值","data":null}

# USER-004 缺 password
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"nopw","email":"nopw@test.com"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 password 不可為空值","data":null}

# USER-005 缺 email
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"noemail","password":"testpass"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 email 不可為空值","data":null}

# USER-006 非法 email
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"bademail","password":"testpass","email":"not-an-email"}'
# 預期 HTTP 400

# USER-007 取得用戶資訊（使用 USER_TOKEN）
curl -s -X POST "http://localhost:80/llm/user/get" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"...","email":"user@bestpartner.com.tw","role":{"roleName":"USER"},...}}

# USER-008 取得資訊（未認證）
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/user/get" \
  -H "Content-Type: application/json" -d '{}'
# 預期 HTTP 401

# USER-009 更新用戶（先用 USER-007 取得 id）
curl -s -X POST "http://localhost:80/llm/user/update" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<USER_ID>","email":"user@bestpartner.com.tw","status":"ACTIVE"}'
# 預期 HTTP 200

# USER-010 更新缺 id
curl -s -X POST "http://localhost:80/llm/user/update" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@bestpartner.com.tw","status":"ACTIVE"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 id 不可為空值","data":null}

# USER-012 切換狀態（admin）
curl -s -X POST "http://localhost:80/llm/user/switchStatus" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<TARGET_USER_ID>","status":"INACTIVE"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{...}}
# 注意：測試後記得切回 ACTIVE

# USER-013 切換狀態缺 status
curl -s -X POST "http://localhost:80/llm/user/switchStatus" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<TARGET_USER_ID>"}'
# 預期 HTTP 400

# USER-014 刪除用戶（admin）
curl -s -X DELETE "http://localhost:80/llm/user/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<USER_ID_TO_DELETE>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}

# USER-015 刪除缺 id
curl -s -X DELETE "http://localhost:80/llm/user/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 400
```

---

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

### TOOL — 工具模組（`/llm/tool`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| TOOL-001 | 列出工具（正常） | P0 | GET | `/llm/tool/list` | | 公開，無需認證 |
| TOOL-002 | 取得工具（正常） | P1 | POST | `/llm/tool/get` | | 公開，無需認證 |
| TOOL-003 | 取得工具（缺 id） | P1 | POST | `/llm/tool/get` | | |
| TOOL-004 | 取得工具（id 不存在） | P2 | POST | `/llm/tool/get` | | |
| TOOL-005 | 註冊工具（admin 正常） | P1 | POST | `/llm/tool/register` | | 需 `ADMIN_TOKEN`；測試後刪除 |
| TOOL-006 | 註冊工具（缺 name） | P1 | POST | `/llm/tool/register` | | |
| TOOL-007 | 註冊工具（缺 classPath） | P1 | POST | `/llm/tool/register` | | |
| TOOL-008 | 註冊工具（缺 type） | P1 | POST | `/llm/tool/register` | | |
| TOOL-009 | 刪除工具（admin 正常） | P1 | POST | `/llm/tool/delete` | | 需 `ADMIN_TOKEN` |
| TOOL-010 | 刪除工具（缺 id） | P1 | POST | `/llm/tool/delete` | | |
| TOOL-011 | 儲存工具設定（正常） | P1 | POST | `/llm/tool/saveSetting` | | ⚠️ 文件標公開但實際需要認證（帶 USER_TOKEN）；測試後刪除 |
| TOOL-012 | 儲存工具設定（缺 alias） | P2 | POST | `/llm/tool/saveSetting` | | |
| TOOL-013 | 更新工具設定（正常） | P1 | POST | `/llm/tool/updateSetting` | | ⚠️ `settingContent` 必須為 JSON 字串（非物件） |
| TOOL-014 | 更新工具設定（缺 settingContent） | P2 | POST | `/llm/tool/updateSetting` | | |
| TOOL-015 | 新增分類（admin 正常） | P1 | POST | `/llm/tool/category/save` | | 需 `ADMIN_TOKEN`；測試後刪除 |
| TOOL-016 | 新增分類（缺 group） | P2 | POST | `/llm/tool/category/save` | | |
| TOOL-017 | 更新分類（admin 正常） | P1 | POST | `/llm/tool/category/update` | | 需 `ADMIN_TOKEN` |
| TOOL-018 | 刪除分類（admin 正常） | P1 | POST | `/llm/tool/category/delete` | | 需 `ADMIN_TOKEN` |
| TOOL-019 | 業務邏輯（刪除後不出現清單） | P1 | 多步 | `/llm/tool/*` | | register → list → delete → list（確認已消失） |

#### TOOL curl 範本

```bash
# TOOL-001 列出工具（公開）
curl -s -X GET "http://localhost:80/llm/tool/list"
# 預期 HTTP 200 → {"code":200,"message":"success","data":[{"id":"3737ec4a-...","name":"date","type":"SYSTEM",...},{"id":"c14e82ca-...","name":"google_search",...}]}

# TOOL-002 取得特定工具
curl -s -X POST "http://localhost:80/llm/tool/get" \
  -H "Content-Type: application/json" \
  -d '{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"3737ec4a-...","name":"date","type":"SYSTEM",...}}

# TOOL-003 缺 id
curl -s -X POST "http://localhost:80/llm/tool/get" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 400

# TOOL-004 id 不存在
curl -s -X POST "http://localhost:80/llm/tool/get" \
  -H "Content-Type: application/json" \
  -d '{"id":"00000000-0000-0000-0000-000000000000"}'
# 預期 HTTP 400 → {"code":400,"message":"Tool not found","data":null}

# TOOL-005 admin 註冊工具（測試後刪除）
curl -s -X POST "http://localhost:80/llm/tool/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_tool","classPath":"tw.zipe.bastpartner.tool.DateTool","groupId":"ea8a08e2-342d-4ade-9579-127d2d1443c5","type":"SYSTEM"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"<NEW_TOOL_ID>",...}}

# TOOL-006 缺 name
curl -s -X POST "http://localhost:80/llm/tool/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"classPath":"tw.zipe.bastpartner.tool.DateTool","groupId":"ea8a08e2-...","type":"SYSTEM"}'
# 預期 HTTP 400

# TOOL-009 刪除工具（admin）
curl -s -X POST "http://localhost:80/llm/tool/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<TOOL_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}

# TOOL-011 儲存工具設定（⚠️ 需認證，雖文件標公開）
curl -s -X POST "http://localhost:80/llm/tool/saveSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f","alias":"my_date_tool"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"settingId":"<SETTING_ID>","alias":"my_date_tool"}}
# 記下 settingId，測試後用 updateSetting 或直接不再使用

# TOOL-012 saveSetting 缺 alias
curl -s -X POST "http://localhost:80/llm/tool/saveSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f"}'
# 預期 HTTP 400

# TOOL-013 更新工具設定（⚠️ settingContent 必須為 JSON 字串，非物件）
curl -s -X POST "http://localhost:80/llm/tool/updateSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"settingId":"<SETTING_ID>","settingContent":"{\"timezone\":\"Asia/Taipei\"}"}'
# 預期 HTTP 200

# TOOL-015 新增工具分類（admin，測試後刪除）
curl -s -X POST "http://localhost:80/llm/tool/category/save" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"group":"test_category"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"<CATEGORY_ID>","group":"test_category"}}

# TOOL-017 更新分類
curl -s -X POST "http://localhost:80/llm/tool/category/update" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"groupId":"<CATEGORY_ID>","group":"updated_category"}'
# 預期 HTTP 200

# TOOL-018 刪除分類
curl -s -X POST "http://localhost:80/llm/tool/category/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"groupId":"<CATEGORY_ID>"}'
# 預期 HTTP 200
```

---

### MCP — MCP Server 模組（`/llm/mcpServer`）

> #### 範例 MCP 設定參考
>
> **STDIO 類型**：`command` 和 `args` 為頂層欄位（非巢狀在 `commandSetting` 內）
>
> ```json
> {
>   "name": "mcp_name",
>   "type": "STDIO",
>   "command": "java",
>   "args": ["-jar", "path/to/server.jar"]
> }
> ```
>
> **SSE 類型**：
> ```json
> {
>   "name": "mcp_name",
>   "type": "SSE",
>   "server": "http://localhost:8080/sse"
> }
> ```

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| MCP-001 | 列出 MCP（已認證） | P0 | GET | `/llm/mcpServer/list` | | |
| MCP-002 | 列出 MCP（未認證） | P0 | GET | `/llm/mcpServer/list` | | 預期 401 |
| MCP-003 | 取得 MCP（正常） | P1 | POST | `/llm/mcpServer/get` | | |
| MCP-004 | 取得 MCP（mcpId 不存在） | P1 | POST | `/llm/mcpServer/get` | | |
| MCP-005 | 註冊 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/register` | | `command`/`args` 在頂層；測試後刪除 |
| MCP-006 | 註冊 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/register` | | 預期 403 |
| MCP-007 | 註冊 MCP（缺必填欄位） | P1 | POST | `/llm/mcpServer/register` | | |
| MCP-008 | 更新 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/update` | | |
| MCP-009 | 更新 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/update` | | 預期 403 |
| MCP-010 | 刪除 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/delete` | | |
| MCP-011 | 刪除 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/delete` | | 預期 403 |
| MCP-012 | 儲存使用者設定（正常） | P1 | POST | `/llm/mcpServer/saveSetting` | | `settingContent` 不可為空物件 `{}`；測試後刪除 |
| MCP-013 | 儲存使用者設定（缺 mcpId） | P1 | POST | `/llm/mcpServer/saveSetting` | | |
| MCP-014 | 取得使用者設定（正常） | P1 | POST | `/llm/mcpServer/getSetting` | | |
| MCP-015 | 取得使用者設定（id 不存在） | P2 | POST | `/llm/mcpServer/getSetting` | | |
| MCP-016 | 更新使用者設定（正常） | P1 | POST | `/llm/mcpServer/updateSetting` | | |
| MCP-017 | 刪除使用者設定（正常） | P1 | DELETE | `/llm/mcpServer/deleteSetting` | | |
| MCP-018 | 未認證取得清單 | P2 | GET | `/llm/mcpServer/list` | | 預期 401（與 MCP-002 相同，P2 邊界測試） |
| MCP-019 | 業務邏輯（設定後於聊天使用） | P1 | 多步 | `/llm/mcpServer/*` | | saveSetting → customAssistantChat（含 mcpSettingIds） → deleteSetting |

#### MCP curl 範本

```bash
# MCP-001 列出 MCP（已認證）
curl -s -X GET "http://localhost:80/llm/mcpServer/list" \
  -H "Authorization: Bearer $USER_TOKEN"
# 預期 HTTP 200 → {"code":200,"message":"success","data":[{"name":"google_drive","type":"STDIO","mcpId":"06a7fb6d-..."},{"name":"date","mcpId":"4b9ba306-..."},...]}

# MCP-002 未認證
curl -s -o /dev/null -w "%{http_code}" -X GET "http://localhost:80/llm/mcpServer/list"
# 預期 HTTP 401

# MCP-003 取得特定 MCP
curl -s -X POST "http://localhost:80/llm/mcpServer/get" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"name":"date","type":"STDIO","mcpId":"4b9ba306-...","args":["D:/MCP/date-1.0-SNAPSHOT-runner.jar"],...}}

# MCP-004 mcpId 不存在
curl -s -X POST "http://localhost:80/llm/mcpServer/get" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"00000000-0000-0000-0000-000000000000"}'
# 預期 HTTP 400 → {"code":400,"message":"MCP server not found","data":null}

# MCP-005 admin 註冊 MCP（⚠️ command/args 在頂層，非 commandSetting 巢狀）
curl -s -X POST "http://localhost:80/llm/mcpServer/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_mcp","type":"STDIO","command":"java","args":["-jar","test.jar"]}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"name":"test_mcp","command":"java","args":["-jar","test.jar"],"type":"STDIO","mcpId":"<NEW_MCP_ID>",...}}
# 記下 mcpId，測試後用 MCP-010 刪除

# MCP-006 一般用戶註冊 MCP
curl -s -X POST "http://localhost:80/llm/mcpServer/register" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"hack_mcp","type":"STDIO","command":"malware","args":[]}'
# 預期 HTTP 403 → {"code":403,"message":"權限錯誤","data":null}

# MCP-007 缺必填欄位（STDIO 需 command + args）
curl -s -X POST "http://localhost:80/llm/mcpServer/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_mcp","type":"STDIO"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 command 不可為空值\n欄位 args 不可為空值","data":null}

# MCP-008 更新 MCP（admin）
curl -s -X POST "http://localhost:80/llm/mcpServer/update" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"<MCP_ID>","name":"test_mcp_updated","type":"STDIO","command":"java","args":["-jar","updated.jar"]}'
# 預期 HTTP 200

# MCP-009 一般用戶更新
curl -s -X POST "http://localhost:80/llm/mcpServer/update" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"<MCP_ID>","name":"hack","type":"STDIO","command":"malware","args":[]}'
# 預期 HTTP 403

# MCP-010 刪除 MCP（admin）
curl -s -X POST "http://localhost:80/llm/mcpServer/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"<MCP_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}

# MCP-011 一般用戶刪除
curl -s -X POST "http://localhost:80/llm/mcpServer/delete" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"<MCP_ID>"}'
# 預期 HTTP 403

# MCP-012 儲存使用者 MCP 設定（⚠️ settingContent 不可為空物件）
curl -s -X POST "http://localhost:80/llm/mcpServer/saveSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021","settingContent":{"timezone":"Asia/Taipei"}}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"userSettingId":"<SETTING_ID>","settingContent":{"timezone":"Asia/Taipei"},...}}
# 記下 userSettingId，測試後用 MCP-017 刪除

# MCP-013 缺 mcpId
curl -s -X POST "http://localhost:80/llm/mcpServer/saveSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"settingContent":{"timezone":"UTC"}}'
# 預期 HTTP 400

# MCP-014 取得使用者設定
curl -s -X POST "http://localhost:80/llm/mcpServer/getSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"<SETTING_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"settingId":"<SETTING_ID>","settingContent":{...},"mcpId":"4b9ba306-...",...}}

# MCP-015 userSettingId 不存在
curl -s -X POST "http://localhost:80/llm/mcpServer/getSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"00000000-0000-0000-0000-000000000000"}'
# 預期 HTTP 400 → {"code":400,"message":"User setting not found","data":null}

# MCP-016 更新使用者設定
curl -s -X POST "http://localhost:80/llm/mcpServer/updateSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"<SETTING_ID>","settingContent":{"timezone":"UTC"}}'
# 預期 HTTP 200

# MCP-017 刪除使用者設定
curl -s -X DELETE "http://localhost:80/llm/mcpServer/deleteSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"<SETTING_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}
```

---

### PERMISSION — 權限模組（`/llm/permission`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| PERM-001 | 新增權限（admin 正常） | P1 | POST | `/llm/permission/add` | | 需 `ADMIN_TOKEN`；測試後刪除 |
| PERM-002 | 新增權限（未認證） | P1 | POST | `/llm/permission/add` | | 預期 401 |
| PERM-003 | 新增權限（缺 name） | P1 | POST | `/llm/permission/add` | | |
| PERM-004 | 新增權限（缺 num） | P1 | POST | `/llm/permission/add` | | |
| PERM-005 | 新增權限（非 admin 拒絕） | P2 | POST | `/llm/permission/add` | | 預期 403 |
| PERM-006 | 更新權限（admin 正常） | P1 | POST | `/llm/permission/update` | | 需 `ADMIN_TOKEN` |
| PERM-007 | 更新權限（缺 id） | P1 | POST | `/llm/permission/update` | | |
| PERM-008 | 更新權限（非 admin 拒絕） | P2 | POST | `/llm/permission/update` | | 預期 403 |
| PERM-009 | 刪除權限（admin 正常） | P1 | DELETE | `/llm/permission/delete` | | 需 `ADMIN_TOKEN` |
| PERM-010 | 刪除權限（缺 id） | P1 | DELETE | `/llm/permission/delete` | | |
| PERM-011 | 刪除權限（未認證） | P1 | DELETE | `/llm/permission/delete` | | 預期 401 |

#### PERMISSION curl 範本

```bash
# PERM-001 admin 新增權限（測試後刪除）
curl -s -X POST "http://localhost:80/llm/permission/add" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_permission","num":9999}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"<PERM_ID>","name":"test_permission","num":9999,"description":null}}
# 記下 id，測試後用 PERM-009 刪除

# PERM-002 未認證
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/permission/add" \
  -H "Content-Type: application/json" -d '{"name":"test","num":1}'
# 預期 HTTP 401

# PERM-003 缺 name
curl -s -X POST "http://localhost:80/llm/permission/add" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"num":100}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 name 不可為空值","data":null}

# PERM-004 缺 num（測試系統行為）
curl -s -X POST "http://localhost:80/llm/permission/add" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_perm"}'
# 預期 HTTP 400 或 200（num 為 nullable 時）

# PERM-005 非 admin 新增
curl -s -X POST "http://localhost:80/llm/permission/add" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"hack","num":1}'
# 預期 HTTP 403 → {"code":403,"message":"權限錯誤","data":null}

# PERM-006 更新權限（admin）
curl -s -X POST "http://localhost:80/llm/permission/update" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<PERM_ID>","name":"test_permission_updated","num":8888}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"<PERM_ID>","name":"test_permission_updated","num":8888}}

# PERM-007 更新缺 id
curl -s -X POST "http://localhost:80/llm/permission/update" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test","num":100}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 id 不可為空值","data":null}

# PERM-008 非 admin 更新
curl -s -X POST "http://localhost:80/llm/permission/update" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<PERM_ID>","name":"hack","num":1}'
# 預期 HTTP 403

# PERM-009 刪除權限（admin，清理測試資料）
curl -s -X DELETE "http://localhost:80/llm/permission/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<PERM_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}

# PERM-010 刪除缺 id
curl -s -X DELETE "http://localhost:80/llm/permission/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 400

# PERM-011 未認證刪除
curl -s -o /dev/null -w "%{http_code}" -X DELETE "http://localhost:80/llm/permission/delete" \
  -H "Content-Type: application/json" -d '{"id":"xxx"}'
# 預期 HTTP 401
```

---

### SYSTEM SETTING — 系統設定模組（`/systemSetting`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| SYSSET-001 | 列出設定（公開） | P0 | GET | `/systemSetting/list` | | 無需認證 |
| SYSSET-002 | 取得設定（正常） | P1 | POST | `/systemSetting/get` | | 無需認證 |
| SYSSET-003 | 取得設定（key 不存在） | P2 | POST | `/systemSetting/get` | | 預期返回空字串 |
| SYSSET-004 | 新增設定（正常） | P1 | POST | `/systemSetting/add` | | 無需認證；測試後刪除 |
| SYSSET-005 | 新增設定（缺 value） | P1 | POST | `/systemSetting/add` | | 預期 400 |
| SYSSET-006 | 更新設定（正常） | P1 | POST | `/systemSetting/update` | | 無需認證 |
| SYSSET-007 | 更新設定（缺 id） | P1 | POST | `/systemSetting/update` | | ⚠️ 已知缺陷：預期 400，實際 500 |
| SYSSET-008 | 刪除設定（正常） | P1 | DELETE | `/systemSetting/delete` | | 無需認證；`delete` 需帶 `key` 欄位 |
| SYSSET-009 | 刪除設定（id 不存在） | P2 | DELETE | `/systemSetting/delete` | | 預期返回 0 |
| SYSSET-010 | 公開端點確認（list 無需認證） | P1 | GET | `/systemSetting/list` | | |
| SYSSET-011 | 公開端點確認（get 無需認證） | P1 | POST | `/systemSetting/get` | | |
| SYSSET-012 | 公開端點確認（add 無需認證） | P1 | POST | `/systemSetting/add` | | 安全性注意：任何人可新增 |
| SYSSET-013 | 業務邏輯（新增 → 更新 → 刪除） | P1 | 多步 | `/systemSetting/*` | | 驗證整個生命週期 |

#### SYSTEM SETTING curl 範本

```bash
# SYSSET-001 列出所有設定（公開，無需認證）
curl -s -X GET "http://localhost:80/systemSetting/list"
# 預期 HTTP 200 → {"code":200,"message":"success","data":[{"id":1,"key":"default_llm_platform","value":"OPENROUTER",...},{"id":2,"key":"skill.global.dir","value":"D:/tmp/...",...}]}

# SYSSET-002 依 key 取得設定
curl -s -X POST "http://localhost:80/systemSetting/get" \
  -H "Content-Type: application/json" \
  -d '{"key":"default_llm_platform"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":"OPENROUTER"}

# SYSSET-003 key 不存在
curl -s -X POST "http://localhost:80/systemSetting/get" \
  -H "Content-Type: application/json" \
  -d '{"key":"nonexistent_key"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":""}（空字串）

# SYSSET-004 新增設定（公開，測試後刪除）
curl -s -X POST "http://localhost:80/systemSetting/add" \
  -H "Content-Type: application/json" \
  -d '{"key":"test_setting","value":"test_value","description":"Test setting"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":null,"key":"test_setting","value":"test_value",...}}
# 注意：response 中 id 為 null，需用 list 查詢取得 id

# 取得新增設定的 id
curl -s -X GET "http://localhost:80/systemSetting/list" | python3 -c "
import sys,json
data=json.load(sys.stdin)['data']
[print(f'id={item[\"id\"]}') for item in data if item.get('key')=='test_setting']
"

# SYSSET-005 缺 value
curl -s -X POST "http://localhost:80/systemSetting/add" \
  -H "Content-Type: application/json" \
  -d '{"key":"incomplete_setting"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 value 不可為空值","data":null}

# SYSSET-006 更新設定（需帶 id + key）
curl -s -X POST "http://localhost:80/systemSetting/update" \
  -H "Content-Type: application/json" \
  -d '{"id":<SETTING_ID>,"key":"test_setting","value":"updated_value","description":"Updated"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":<SETTING_ID>,"key":"test_setting","value":"updated_value",...}}

# SYSSET-007 更新缺 id（⚠️ 已知缺陷：預期 400，實際 500）
curl -s -X POST "http://localhost:80/systemSetting/update" \
  -H "Content-Type: application/json" \
  -d '{"key":"test_setting","value":"some_value"}'
# 預期 HTTP 400（缺陷：實際返回 500）

# SYSSET-008 刪除設定（⚠️ DELETE 端點需帶 key 欄位，否則反序列化失敗）
curl -s -X DELETE "http://localhost:80/systemSetting/delete" \
  -H "Content-Type: application/json" \
  -d '{"id":<SETTING_ID>,"key":"test_setting"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":1}

# SYSSET-009 刪除不存在的 id（需帶 key）
curl -s -X DELETE "http://localhost:80/systemSetting/delete" \
  -H "Content-Type: application/json" \
  -d '{"id":99999,"key":"fake_key"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":0}（0 rows affected）
```

---

### 跨模組整合測試

| 測試 ID | 測試場景 | 優先級 | 涉及端點 | 狀態 | 備註 |
|---------|---------|--------|----------|------|------|
| INT-001 | 完整聊天流程 | P0 | `login → setting/get → chat` | | |
| INT-002 | 向量知識庫輔助聊天 | P1 | `login → vector/save → uploadFiles → customAssistantChat` | | ⚠️ 需 Milvus/Chroma；可改測 customAssistantChat 基礎流程 |
| INT-003 | MCP 整合聊天 | P1 | `login → mcpServer/saveSetting → customAssistantChat` | | |
| INT-004 | 用戶生命週期 | P1 | `register → login → get → update → delete` | | |
| INT-005 | JWT 過期保護 | P1 | `login → (等待過期) → chat` | | |
| INT-006 | admin 工作流 | P1 | `login(admin) → platform/add → setting/save → cleanup` | | |

#### 跨模組整合 curl 範本

```bash
# INT-001 完整聊天流程（test_user）
# Step 1: 驗證 token 有效
curl -s -X POST "http://localhost:80/login/check" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" -d '{}'
# Step 2: 查詢 LLM 設定
curl -s -X POST "http://localhost:80/llm/setting/get" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9"}'
# Step 3: 執行聊天
curl -s -X POST "http://localhost:80/llm/chat" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Say: INT-001 OK"}'
# 預期全部 HTTP 200，最後返回含 "INT-001 OK" 的回覆

# INT-002 customAssistantChat 基礎流程（不需向量 DB）
curl -s -X POST "http://localhost:80/llm/customAssistantChat" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Say: INT-002 OK","memory":{"id":"int-002-session","maxSize":5},"promptContent":"You are a helpful assistant."}'
# 預期 HTTP 200

# INT-003 MCP 整合聊天
# Step 1: 建立 MCP 使用者設定
MCP_SETTING_RESP=$(curl -s -X POST "http://localhost:80/llm/mcpServer/saveSetting" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021","settingContent":{"note":"int003"}}')
export INT3_SETTING_ID=$(echo "$MCP_SETTING_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['userSettingId'])")
# Step 2: 含 MCP 的聊天
curl -s -X POST "http://localhost:80/llm/customAssistantChat" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"llmId\":\"583b9222-8cb0-4109-b072-5f0fd1e9fed9\",\"message\":\"What is today's date?\",\"memory\":{\"id\":\"int003-session\",\"maxSize\":5},\"promptContent\":\"You are helpful.\",\"mcpSettingIds\":[\"$INT3_SETTING_ID\"]}"
# Step 3: 清理設定
curl -s -X DELETE "http://localhost:80/llm/mcpServer/deleteSetting" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"userSettingId\":\"$INT3_SETTING_ID\"}"

# INT-004 用戶生命週期
TS=$(date +%s)
# register
REG=$(curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"lifecycle_$TS\",\"password\":\"pass\",\"email\":\"lifecycle_$TS@test.com\"}")
LC_USER_ID=$(echo "$REG" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
# login
LC_TOKEN=$(curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"lifecycle_$TS@test.com\",\"password\":\"pass\"}" | grep -o '"data":"[^"]*' | cut -d'"' -f4)
# get
curl -s -X POST "http://localhost:80/llm/user/get" -H "Authorization: Bearer $LC_TOKEN" -d '{}'
# update
curl -s -X POST "http://localhost:80/llm/user/update" \
  -H "Authorization: Bearer $LC_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"$LC_USER_ID\",\"email\":\"lifecycle_upd@test.com\",\"status\":\"ACTIVE\"}"
# delete (admin)
curl -s -X DELETE "http://localhost:80/llm/user/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"$LC_USER_ID\"}"

# INT-005 JWT 過期保護（使用偽造過期 token）
EXPIRED_TOKEN="eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJiYXN0LXBhcnRuZXIiLCJ1cG4iOiJhZG1pbiIsImdyb3VwcyI6WyJhZG1pbiJdLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MTcwMDAwMTgwMH0.invalidsig"
curl -s -X POST "http://localhost:80/llm/chat" \
  -H "Authorization: Bearer $EXPIRED_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-...","message":"test"}'
# 預期 HTTP 401 + 自動 refresh token（JWT Refresh 機制）

# INT-006 admin 工作流
# add platform
P_RESP=$(curl -s -X POST "http://localhost:80/llm/setting/platform/add" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"platform":"OPENROUTER"}')
INT6_PID=$(echo "$P_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
# save setting
S_RESP=$(curl -s -X POST "http://localhost:80/llm/setting/save" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "{\"platformId\":\"$INT6_PID\",\"modelType\":\"CHAT\",\"llmModel\":{\"modelName\":\"deepseek/deepseek-v3.2\",\"apiKey\":\"sk-test\"}}")
INT6_LID=$(echo "$S_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
# cleanup
curl -s -X POST "http://localhost:80/llm/setting/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "{\"id\":\"$INT6_LID\"}"
curl -s -X POST "http://localhost:80/llm/setting/platform/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "{\"id\":\"$INT6_PID\"}"
```

---

### 通用安全性測試

| 測試 ID | 測試場景 | 優先級 | 適用端點 | 狀態 | 備註 |
|---------|---------|--------|----------|------|------|
| SEC-001 | SQL Injection | P1 | `/login/` | | 預期 401（不登入成功） |
| SEC-002 | XSS Payload | P2 | `/llm/user/register` | | 預期 200（API 接受，前端轉義責任）；測試後刪除 |
| SEC-003 | 過期 JWT | P1 | 所有需認證端點 | | 預期 401 + refresh token |
| SEC-004 | 竄改 JWT Payload | P1 | 所有需認證端點 | | ⚠️ **已知 P0 安全漏洞**：refresh 機制不驗簽名，偽造 payload 可取得有效 JWT |
| SEC-005 | 水平越權 | P1 | `/llm/user/get`、`/llm/mcpServer/getSetting` | | 預期 400（查不到他人資料） |
| SEC-006 | 大量請求壓力 | P2 | `/llm/chat` | | 無 rate limiting，10 次快速請求全成功 |

#### 通用安全性 curl 範本

```bash
# SEC-001 SQL Injection
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw'\'' OR '\''1'\''='\''1","password":"anything"}'
# 預期 HTTP 401 → {"code":401,"message":"帳號不存在","data":null}（injection 無效）

# SEC-002 XSS Payload in register
TS=$(date +%s)
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"<script>alert(1)</script>\",\"password\":\"xss_pass\",\"email\":\"xss_$TS@test.com\"}"
# 預期 HTTP 200（API 接受原始字串，前端負責轉義）
# 注意：取回 id 後立即清理
# cleanup：curl -s -X DELETE "http://localhost:80/llm/user/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"<XSS_USER_ID>"}'

# SEC-003 過期 JWT（觀察系統行為）
EXPIRED="eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJiYXN0LXBhcnRuZXIiLCJ1cG4iOiJhZG1pbiIsImdyb3VwcyI6WyJhZG1pbiJdLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MTcwMDAwMTgwMH0.invalidsig"
curl -s -X POST "http://localhost:80/llm/user/get" \
  -H "Authorization: Bearer $EXPIRED" \
  -H "Content-Type: application/json" -d '{}'
# 預期 HTTP 401 + {"code":401,"message":"Credentials expired, new token has been generated","data":"<new_token>"}
# 注意：系統會自動生成新 token（JWT Refresh 機制）

# SEC-004 竄改 JWT Payload（⚠️ 已知 P0 安全漏洞）
# 偽造含 admin 群組的過期 token
TAMPERED="eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJiYXN0LXBhcnRuZXIiLCJ1cG4iOiJhdHRhY2tlciIsImdyb3VwcyI6WyJhZG1pbiJdLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MTcwMDAwMTgwMH0.fakesig"
curl -s -X POST "http://localhost:80/llm/permission/add" \
  -H "Authorization: Bearer $TAMPERED" \
  -H "Content-Type: application/json" \
  -d '{"name":"sec004_tampered","num":1}'
# 預期 HTTP 401（簽名無效）
# ⚠️ 若 HTTP 401 + 回傳新 token → 代表漏洞存在（refresh 未驗簽）
# ⚠️ 若使用回傳的 token 再次呼叫 → HTTP 200 admin 存取成功 = Release Blocker

# SEC-005 水平越權測試
# 嘗試存取其他人的 MCP setting
curl -s -X POST "http://localhost:80/llm/mcpServer/getSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"<OTHER_USER_SETTING_ID>"}'
# 預期 HTTP 400 → {"code":400,"message":"User setting not found","data":null}

# SEC-006 大量請求壓力（觀察是否有 rate limiting）
for i in {1..10}; do
  curl -s -o /dev/null -w "Request $i: HTTP %{http_code}\n" \
    -X POST "http://localhost:80/llm/chat" \
    -H "Authorization: Bearer $TEST_USER_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Hello"}'
done
# 預期：10/10 HTTP 200（目前無 rate limiting）
```

---

## 問題追蹤區

> 所有 ❌ Fail 項目請在此登記，格式如下：

| 測試 ID | 發現日期 | 問題描述 | 重現步驟 | 預期結果 | 實際結果 | 嚴重度 | 負責人 | 狀態 |
|---------|---------|---------|---------|---------|---------|--------|--------|------|
| | | | | | | | | |

---

## 常見錯誤排查

| 錯誤 | 原因 | 解決方案 |
|------|------|---------|
| HTTP 400 `欄位 X 不可為空值` | 必填欄位缺失 | 確認 request body 包含所有必填欄位 |
| HTTP 400 `Unexpected JSON token ... unknown key 'X'` | DTO 不接受此欄位（嚴格解析）| 確認欄位名稱正確；參照本文 curl 範本 |
| HTTP 400 `Please verify the LLM setting...` | llmId 不屬於目前登入的用戶 | 使用與 token 相符帳號所建立的 llmId |
| HTTP 401 | 未認證或 token 無效/過期 | 重新取得 token |
| HTTP 403 | 角色不符（admin 端點用 user token 呼叫） | 改用 ADMIN_TOKEN |
| HTTP 500 `Internal server error` | 服務端異常 | 檢查 `D:/tmp/bestpartner/bestpartner_error.log` |
| CHAT 500 type mismatch | CHAT 端點用了 STREAMING 類型的 llmId | CHAT → `CHAT_LLM_ID`；chatStreaming → `STREAMING_LLM_ID` |
| `customAssistantChat` 400 缺 promptContent | promptContent 為必填 | 加入 `"promptContent":"..."` 欄位 |
| `saveSetting` 400 `settingContent 不可為空值` | `settingContent` 不可傳 `{}` 空物件 | 至少包含一個有值的鍵 |
| `systemSetting/delete` 400 缺 key | SystemSettingDTO 反序列化要求 key 非 null | DELETE body 需包含 `"key":"..."` 欄位 |

---

*最後更新：2026-04-30（依 test-confirmation-202604291430.md 實測結果更新）*
