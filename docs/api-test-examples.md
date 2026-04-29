# BestPartner API 測試範例集合

> 本文檔包含所有已驗證的 API 調用範例及預期響應。
> 更新時間：2026-04-29
> 測試環境：dev

---

## 環境變數設定

```bash
# 基礎變數
export DOMAIN="localhost:80"
export ADMIN_EMAIL="admin@bestpartner.com.tw"
export ADMIN_PASSWORD="admin"
export USER_EMAIL="user@bestpartner.com.tw"
export USER_PASSWORD="user"

# LLM 設定 ID（透過 LLM Setting 建立）
export CHAT_LLM_ID="33b5e4a4-798b-41fd-abad-21e2e68831b1"
export STREAMING_LLM_ID="9d94a87e-4d79-41f8-a252-0bcd1624d3cd"
export PLATFORM_ID="b09d3007-8775-4087-9f56-62f023b8a3d0"

# Token（登入後自動獲取）
export ADMIN_TOKEN=""
export USER_TOKEN=""
```

---

## AUTH 模組 - 認證

### AUTH-001: 正常登入（有效帳密）

**請求**：
```bash
curl -X POST "http://${DOMAIN}/login/" \
  -H "Content-Type: application/json" \
  -d '{
    "email":"admin@bestpartner.com.tw",
    "password":"admin"
  }'
```

**預期響應** (HTTP 200):
```json
{
  "code": 200,
  "message": "success",
  "data": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9..."
}
```

**保存 Token**：
```bash
export ADMIN_TOKEN=$(curl -s -X POST "http://${DOMAIN}/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw","password":"admin"}' | grep -o '"data":"[^"]*' | cut -d'"' -f4)
```

---

### AUTH-002: 驗證 JWT 有效

**請求**：
```bash
curl -X POST "http://${DOMAIN}/login/check" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**預期響應** (HTTP 200):
```json
{
  "code": 200,
  "message": "success",
  "data": "check"
}
```

---

### AUTH-003: 錯誤登入（密碼錯誤）

**請求**：
```bash
curl -X POST "http://${DOMAIN}/login/" \
  -H "Content-Type: application/json" \
  -d '{
    "email":"admin@bestpartner.com.tw",
    "password":"wrongpassword"
  }'
```

**預期響應** (HTTP 401):
```json
{
  "code": 401,
  "message": "密碼錯誤",
  "data": null
}
```

---

### AUTH-004: 錯誤登入（信箱不存在）

**請求**：
```bash
curl -X POST "http://${DOMAIN}/login/" \
  -H "Content-Type: application/json" \
  -d '{
    "email":"notexist@example.com",
    "password":"anypassword"
  }'
```

**預期響應** (HTTP 401):
```json
{
  "code": 401,
  "message": "帳號不存在",
  "data": null
}
```

---

### AUTH-005 至 AUTH-009: 驗證相關

**AUTH-005: 必填驗證（缺 email）** - HTTP 400
```bash
curl -X POST "http://${DOMAIN}/login/" \
  -H "Content-Type: application/json" \
  -d '{"password":"admin"}'
```

**AUTH-006: 必填驗證（缺 password）** - HTTP 400
```bash
curl -X POST "http://${DOMAIN}/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw"}'
```

**AUTH-007: 必填驗證（空 body）** - HTTP 400
```bash
curl -X POST "http://${DOMAIN}/login/" \
  -H "Content-Type: application/json" \
  -d '{}'
```

---

### AUTH-010: 邊界條件（超長密碼）

**請求**：
```bash
# 注意：超長密碼會返回 HTTP 400（格式驗證失敗），而非 401
LONG_PASSWORD=$(python3 -c "print('x'*1000)")
curl -X POST "http://${DOMAIN}/login/" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@bestpartner.com.tw\",\"password\":\"${LONG_PASSWORD}\"}"
```

**預期響應** (HTTP 400):
```json
{
  "code": 400,
  "message": "欄位驗證失敗",
  "data": null
}
```

**說明**：此為正確行為 - 輸入驗證應返回 400

---

## CHAT 模組 - 聊天功能

### 準備：建立 LLM 設定

**新增 OpenRouter 平台**：
```bash
curl -X POST "http://${DOMAIN}/llm/setting/platform/add" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"platform":"OPENROUTER"}'
```

**新增 CHAT 類型 LLM 設定**：
```bash
curl -X POST "http://${DOMAIN}/llm/setting/save" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"platformId\":\"${PLATFORM_ID}\",
    \"modelType\":\"CHAT\",
    \"llmModel\":{
      \"modelName\":\"deepseek/deepseek-r1\",
      \"apiKey\":\"sk-or-v1-test-key\"
    }
  }"
```

---

### CHAT-001: 同步聊天（正常請求）

**請求**：
```bash
curl -X POST "http://${DOMAIN}/llm/chat" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"llmId\":\"${CHAT_LLM_ID}\",
    \"message\":\"Hello, how are you?\"
  }"
```

**預期響應** (HTTP 200):
```json
{
  "code": 200,
  "message": "success",
  "data": "I'm doing well, thank you for asking!..."
}
```

---

### CHAT-002: 同步聊天（未認證）

**請求**：
```bash
curl -X POST "http://${DOMAIN}/llm/chat" \
  -H "Content-Type: application/json" \
  -d "{
    \"llmId\":\"${CHAT_LLM_ID}\",
    \"message\":\"Test\"
  }"
```

**預期響應** (HTTP 401):
```json
{
  "code": 401,
  "message": "Unauthorized",
  "data": null
}
```

---

### CHAT-006: 串流聊天（正常請求）

**請求**（使用 SSE）：
```bash
curl -X POST "http://${DOMAIN}/llm/chatStreaming" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"llmId\":\"${STREAMING_LLM_ID}\",
    \"message\":\"Hello\"
  }"
```

**預期響應** (HTTP 200 + SSE):
```
data: First chunk of response...
data: Second chunk...
data: ...
```

---

### CHAT-007: 串流聊天（未認證）

**請求**：
```bash
curl -X POST "http://${DOMAIN}/llm/chatStreaming" \
  -H "Content-Type: application/json" \
  -d "{
    \"llmId\":\"${STREAMING_LLM_ID}\",
    \"message\":\"Test\"
  }"
```

**預期響應** (HTTP 401)

---

## ADMIN CHAT 模組

### ADCHAT-001: 管理員聊天（admin 正常）

**請求**：
```bash
curl -X POST "http://${DOMAIN}/llm/admin/chat" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "message":"What is 2+2?",
    "platform":"OPENROUTER"
  }'
```

**預期響應** (HTTP 200):
```json
{
  "code": 200,
  "message": "success",
  "data": "2+2 equals 4."
}
```

---

### ADCHAT-002: 管理員聊天（一般用戶存取）

**請求**（使用 USER_TOKEN）：
```bash
curl -X POST "http://${DOMAIN}/llm/admin/chat" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "message":"Test",
    "platform":"OPENROUTER"
  }'
```

**預期響應** (HTTP 403):
```json
{
  "code": 403,
  "message": "Forbidden",
  "data": null
}
```

---

### ADCHAT-003: 管理員聊天（未認證）

**請求**：
```bash
curl -X POST "http://${DOMAIN}/llm/admin/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message":"Test",
    "platform":"OPENROUTER"
  }'
```

**預期響應** (HTTP 401)

---

## USER 模組

### USER-001: 註冊（正常新帳號）

**請求**：
```bash
# 使用唯一的郵箱
curl -X POST "http://${DOMAIN}/llm/user/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username":"newuser",
    "password":"password123",
    "email":"newuser@example.com"
  }'
```

**預期響應** (HTTP 200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "uuid-here",
    "username": "newuser",
    "email": "newuser@example.com",
    "status": "ACTIVE"
  }
}
```

---

### USER-007: 取得用戶資訊（已認證）

**請求**：
```bash
curl -X POST "http://${DOMAIN}/llm/user/get" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**預期響應** (HTTP 200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "user-id",
    "username": "user",
    "email": "user@bestpartner.com.tw",
    "status": "ACTIVE"
  }
}
```

---

### USER-008: 取得用戶資訊（未認證）

**請求**：
```bash
curl -X POST "http://${DOMAIN}/llm/user/get" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**預期響應** (HTTP 401)

---

## SYSTEM SETTING 模組

### SYS-001: 列出設定（正常）

**請求**：
```bash
curl -X GET "http://${DOMAIN}/systemSetting/list" \
  -H "Content-Type: application/json"
```

**預期響應** (HTTP 200):
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "setting-1",
      "key": "setting_key_1",
      "value": "value_1",
      "description": "Description"
    }
  ]
}
```

---

## 測試指令集合

### 快速測試所有 P0 API

```bash
#!/bin/bash
set -e

DOMAIN="localhost:80"

# 1. 登入
echo "=== 登入 ==="
ADMIN_RESPONSE=$(curl -s -X POST "http://${DOMAIN}/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw","password":"admin"}')
ADMIN_TOKEN=$(echo "$ADMIN_RESPONSE" | grep -o '"data":"[^"]*' | cut -d'"' -f4)
echo "✅ Admin Token: ${ADMIN_TOKEN:0:20}..."

# 2. 驗證 JWT
echo ""
echo "=== 驗證 JWT ==="
curl -s -X POST "http://${DOMAIN}/login/check" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{}' | grep -q "check" && echo "✅ JWT 驗證通過" || echo "❌ JWT 驗證失敗"

# 3. 測試同步聊天
echo ""
echo "=== 測試同步聊天 ==="
CHAT_RESP=$(curl -s -X POST "http://${DOMAIN}/llm/chat" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"33b5e4a4-798b-41fd-abad-21e2e68831b1","message":"Hello"}')
echo "$CHAT_RESP" | grep -q "code" && echo "✅ 同步聊天正常" || echo "❌ 同步聊天失敗"

# 4. 測試未認證
echo ""
echo "=== 測試未認證狀況 ==="
curl -s -X POST "http://${DOMAIN}/llm/chat" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"test","message":"test"}' | grep -q "401" && echo "✅ 未認證正確返回 401" || echo "❌ 未認證檢查失敗"

echo ""
echo "✅ 所有 P0 測試完成"
```

---

## 常見錯誤和解決方案

| 錯誤 | 原因 | 解決方案 |
|------|------|---------|
| HTTP 400 | 請求格式或驗證錯誤 | 檢查 JSON 格式和必填欄位 |
| HTTP 401 | 未認證或 Token 無效 | 確認 Bearer Token 正確 |
| HTTP 403 | 無權限 | 確認用戶角色和端點權限要求 |
| HTTP 500 | 服務器錯誤 | 檢查服務器日誌 |

---

## 注意事項

- 所有 LLM ID 需要透過 LLM SETTING API 先建立
- 部分端點需要特定角色（admin）
- Token 預設有效期為 30 分鐘
- 超長輸入會被驗證層攔截，返回 400

