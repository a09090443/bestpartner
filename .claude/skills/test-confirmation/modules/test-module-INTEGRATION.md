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

