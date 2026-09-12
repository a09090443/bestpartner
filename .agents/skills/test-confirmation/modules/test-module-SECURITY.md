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
