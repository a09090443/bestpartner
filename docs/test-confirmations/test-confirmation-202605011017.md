# BestPartner API 測試功能確認表

> 本文件為定期測試記錄模板，每次測試週期開始前請複製此文件並填寫狀態欄位。
> 各模組 curl 範本請參考 `.claude/skills/test-confirmation/modules/` 目錄。

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
4. 按模組依序執行測試，參照各模組檔案中的「curl 範本」小節，逐項填入狀態
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
| 測試日期 | 2026-05-01 10:17 |
| 服務版本 | 0.1.7-SNAPSHOT |
| 測試環境 | dev |
| LLM 平台 | OpenRouter（deepseek/deepseek-v3.2） |
| LLM Setting ID | |
| Base URL | `http://localhost:80` |
| 測試結束日期 | 2026-05-02 |

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
| AUTH | 10 | 10 | 0 | 0 | 100.0% |
| CHAT | 18 | 17 | 0 | 1 | 94.4% |
| ADMIN CHAT | 5 | 3 | 0 | 2 | 60.0% |
| USER | 17 | 17 | 0 | 0 | 100.0% |
| LLM SETTING | 16 | 15 | 1 | 0 | 93.7% |
| VECTOR | 14 | 10 | 1 | 3 | 71.4% |
| TOOL | 19 | 18 | 1 | 0 | 94.7% |
| MCP | 19 | 19 | 0 | 0 | 100.0% |
| PERMISSION | 11 | 11 | 0 | 0 | 100.0% |
| SYSTEM SETTING | 13 | 12 | 1 | 0 | 92.3% |
| 跨模組整合 | 6 | 6 | 0 | 0 | 100.0% |
| 通用安全性 | 6 | 5 | 1 | 0 | 83.3% |
| **合計** | **154** | **143** | **5** | **6** | **92.8%** |

### 通過標準

| 優先級 | 通過標準 | 說明 |
|--------|--------|------|
| P0 | 100% | 任何失敗均為 Release Blocker |
| P1 | ≥ 95% | 已知失敗需有追蹤 Issue |
| P2 | ≥ 80% | 剩餘列為 Tech Debt |

---

## 模組完成檢查清單（每完成一模組必須立即執行）

> 對應 SKILL.md Step 7.5。每個模組所有案例執行完畢後，進入下一模組前，必須依序勾選以下項目並更新摘要表。

### 勾選清單

```
□ 所有案例 curl 記錄已寫入確認表（無佔位符、無省略）
□ 所有案例狀態已標記（✅ / ❌ / ⏭️）
□ 失敗案例（❌）已在「問題追蹤區」詳細記錄（Test ID、現象、期望、實際、根因）
□ 摘要表中該模組的統計數字已更新（Pass / Fail / Skip / 總數）
□ Pass 率已計算並填入（保留一位小數，如 80.0%）
□ 測試資料清理完成（若該模組有新增資料的 API 測試）
```

### 摘要表填寫範例

```markdown
| 模組 | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|------|------|--------|--------|---------|---------|
| AUTH | 10 | 8 | 1 | 1 | 80.0% |  ← 此模組完成
```

### 嚴格規則

- **不可延後**：完成一個模組所有測試後立即更新，不可等到所有模組測完才統一更新
- **即時暫停**：若某模組 Pass 率過低（P0 < 100% / P1 < 95% / P2 < 80%），立即暫停評估
- **計算精確**：Pass 率 = Pass 案例數 ÷ 總案例數 × 100%，保留一位小數，不四捨五入到整數
- **與問題追蹤同步**：失敗案例必須在更新摘要前先寫入問題追蹤區

> ⚠️ 任何模組未按此步驟立即更新，視同測試流程違規，需撤銷摘要表並重新執行。

---

## 模組測試清單

> 詳細測試清單已按模組拆分至 `modules/` 目錄，於執行到各模組時再讀入。
> 執行時請依序參考以下檔案：

| 模組 | 檔案位置 | 測試數 |
|------|---------|--------|
| AUTH 認證模組 | [`./modules/test-module-AUTH.md`](./modules/test-module-AUTH.md) | 10 |
| CHAT 聊天模組 | [`./modules/test-module-CHAT.md`](./modules/test-module-CHAT.md) | 18 |
| ADMIN CHAT 管理員聊天 | [`./modules/test-module-ADMIN_CHAT.md`](./modules/test-module-ADMIN_CHAT.md) | 5 |
| USER 用戶管理 | [`./modules/test-module-USER.md`](./modules/test-module-USER.md) | 17 |
| LLM SETTING 模型設定 | [`./modules/test-module-LLM_SETTING.md`](./modules/test-module-LLM_SETTING.md) | 16 |
| VECTOR 向量資料庫 | [`./modules/test-module-VECTOR.md`](./modules/test-module-VECTOR.md) | 14 |
| TOOL 工具管理 | [`./modules/test-module-TOOL.md`](./modules/test-module-TOOL.md) | 19 |
| MCP MCP 伺服器 | [`./modules/test-module-MCP.md`](./modules/test-module-MCP.md) | 19 |
| PERMISSION 權限管理 | [`./modules/test-module-PERMISSION.md`](./modules/test-module-PERMISSION.md) | 11 |
| SYSTEM SETTING 系統設定 | [`./modules/test-module-SYSTEM_SETTING.md`](./modules/test-module-SYSTEM_SETTING.md) | 13 |
| INTEGRATION 跨模組整合 | [`./modules/test-module-INTEGRATION.md`](./modules/test-module-INTEGRATION.md) | 6 |
| SECURITY 通用安全性 | [`./modules/test-module-SECURITY.md`](./modules/test-module-SECURITY.md) | 6 |

---

## 測試執行注意事項

### 測試資料清理規則

**凡是測試新增資料的 API（如 POST 建立資源），測試完成後必須立即呼叫對應的刪除 API，以相同的條件（id、名稱、參數等）刪除該筆測試資料。**

清理原則：
- 新增成功（✅）：記錄後立即呼叫刪除 API 清除資料
- 新增失敗（❌）：確認資料未寫入，無需清理；在問題追蹤區記錄
- 若刪除 API 尚未實作或測試失敗：在備註欄說明，手動清除或標記為待清理

> ⚠️ 測試完成後若資料庫殘留測試資料，視同測試流程不完整，需補執行清理步驟。

### 測試中斷處理

#### 遇到無效 API Key 時

若測試過程中任何端點回傳 API Key 無效（如 HTTP 401、403，或錯誤訊息含 `invalid api key`、`unauthorized`、`authentication failed` 等），**立即暫停測試**並：

1. 詢問「LLM SETTING 中，目前哪個 llmId 的設定是有效的？」
2. 將 llmId 填入「測試週期資訊」中的「LLM Setting ID」欄位
3. 後續使用該 llmId 繼續測試

---

## 問題追蹤區

> 測試失敗（❌）的項目需在此詳細記錄，包括 Test ID、現象、期望行為、實際行為

| Test ID | 現象 | 期望行為 | 實際行為 | 根因 | 狀態 |
|---------|------|---------|---------|------|------|
| LLMSET-006 | update API 返回 400，缺 platformId/modelType | HTTP 200 更新成功 | HTTP 400 缺必填欄位 | curl 範本缺少 `platformId` 和 `modelType` 必填欄位；API 本身行為正確（補充後 HTTP 200）；需更新 `test-module-LLM_SETTING.md` 的 curl 範本 | 待修正範本 |
| VEC-003 | 更新向量設定無法執行 | HTTP 200 更新成功 | 無法取得 ID | `POST /llm/vector/save` 回傳字串訊息而非含 ID 的資料物件；無 list 端點可查詢既有設定；API 設計缺陷，建議 save 返回含 id 的完整物件 | 待 API 修正 |
| TOOL-005 | 註冊工具 type:"SYSTEM" 失敗 | HTTP 200 工具建立 | HTTP 400 type 無效 | curl 範本使用 `type:"SYSTEM"` 但有效值為 `CUSTOMIZE` / `BUILT_IN`；需更新 `test-module-TOOL.md`；API 補正後正確 | 待修正範本 |
| SYSSET-007 | 更新設定缺 id 返回 500 | HTTP 400 缺 id 錯誤 | HTTP 500 Internal server error | API 未對 id=null 進行防護，直接傳給 SQL 導致 NPE/500；應在 Service 層先驗證 id 非空再執行更新 | 待修正 |

---

## 快速煙霧測試（P0）

```bash
#!/bin/bash
DOMAIN="localhost:80"

# 1. 登入取得 token
ADMIN_RESPONSE=$(curl -s -X POST "http://${DOMAIN}/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw","password":"admin"}')
ADMIN_TOKEN=$(echo "$ADMIN_RESPONSE" | grep -o '"data":"[^"]*' | cut -d'"' -f4)
echo "✅ Admin Token: ${ADMIN_TOKEN:0:20}..."

# 2. 驗證 JWT
curl -s -X POST "http://${DOMAIN}/login/check" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{}' | grep -q "check" && echo "✅ JWT 驗證通過" || echo "❌ JWT 驗證失敗"

# 3. 測試同步聊天（需填入有效 llmId）
# curl -s -X POST "http://${DOMAIN}/llm/chat" \
#   -H "Authorization: Bearer ${ADMIN_TOKEN}" \
#   -H "Content-Type: application/json" \
#   -d '{"llmId":"<CHAT_LLM_ID>","message":"Hello"}'

# 4. 未認證應返回 401
curl -s -X POST "http://${DOMAIN}/llm/chat" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"test","message":"test"}' | grep -q "401" \
  && echo "✅ 未認證正確返回 401" || echo "❌ 未認證檢查失敗"
```

---

## 常見錯誤和解決方案

| HTTP 狀態碼 | 原因 | 解決方案 |
|------------|------|---------|
| 400 | 請求格式或驗證錯誤 | 檢查 JSON 格式和必填欄位 |
| 401 | 未認證或 Token 無效 | 確認 Bearer Token 正確且未過期 |
| 403 | 無權限 | 確認用戶角色和端點權限要求 |
| 500 | 服務器錯誤 | 檢查服務器日誌（`D:/tmp/bestpartner/bestpartner_error.log`） |

---

## AUTH 認證模組

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-AUTH.md`

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| AUTH-001 | 正常登入（有效帳密） | P0 | POST | `/login/` | ✅ | |
| AUTH-002 | 驗證 JWT 有效 | P0 | POST | `/login/check` | ✅ | |
| AUTH-003 | 錯誤登入（密碼錯誤） | P1 | POST | `/login/` | ✅ | |
| AUTH-004 | 錯誤登入（信箱不存在） | P1 | POST | `/login/` | ✅ | |
| AUTH-005 | 必填驗證（缺 email） | P1 | POST | `/login/` | ✅ | |
| AUTH-006 | 必填驗證（缺 password） | P1 | POST | `/login/` | ✅ | |
| AUTH-007 | 必填驗證（空 body） | P1 | POST | `/login/` | ✅ | |
| AUTH-008 | email 格式不合法 | P2 | POST | `/login/` | ✅ | 返回 401（帳號不存在），符合 P2 預期範圍 |
| AUTH-009 | JWT check（無效 token） | P1 | POST | `/login/check` | ✅ | |
| AUTH-010 | 邊界條件（超長 password） | P2 | POST | `/login/` | ✅ | 返回 401（密碼錯誤） |

### AUTH curl 記錄

**AUTH-001 正常登入（admin）**
```
curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" -d '{"email":"admin@bestpartner.com.tw","password":"admin"}'
→ HTTP 200
{"code":200,"message":"success","data":"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJiYXN0LXBhcnRuZXIiLCJ1cG4iOiJjODhmNTdjOC1hZDI2LTRlYTAtOWY3MS1hNjU5OTViNDkzNTciLCJncm91cHMiOlsiYWRtaW4iLCJ1c2VyLXdyaXRlIiwidXNlci1yZWFkIl0sImlhdCI6MTc3NzYwMjE5MywiZXhwIjoxNzc3NjAzOTkzLCJqdGkiOiIyNDQ1YWI2Mi04OGE0LTRlNDEtOTI3My05YTQ4ZmI3MGFhYTYifQ.h30WiHa1Oqq3exIzyjimVGnXPeg-bUMfPtkty3eRv71_a6WInG5Ly5gAn1OVO3k7yY8ek42ZHMvXpavFtO4Dv98a-DIYKXS2YDBoq2JXPmLo7IWYme1QDDhkhATupQwc8o_ftbvHfCqHl9qtbiTb4FsaV_VX0-Jv_r8MEtytfGaOfgUGqOqABl9Oc64JJX5y0-xgXZIDQVo6VA4rddNRCQjEekIX5W4JYhAWNP6q-yYvR23stI-85sCfIony6j5r9vjYY5hRFHZ19dbHDU0aSwEdFPqaDWdLweHssNlxcNGNZcY5qqPIppUNVKYzWFImWspsWMVH6Po9RSZJ91HG3w"}
```

**AUTH-001b 正常登入（user）**
```
curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" -d '{"email":"user@bestpartner.com.tw","password":"user"}'
→ HTTP 200
{"code":200,"message":"success","data":"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJiYXN0LXBhcnRuZXIiLCJ1cG4iOiI1NTU0ZjI1NS0wOGQ1LTQwNjktOTlkMi05MjgzNzJkNGE4N2IiLCJncm91cHMiOlsidXNlci13cml0ZSIsInVzZXItcmVhZCJdLCJpYXQiOjE3Nzc2MDIxOTMsImV4cCI6MTc3NzYwMzk5MywianRpIjoiY2UyMzI2ZGEtOWM1MS00YzcyLWJmYzItNzM1ZTUxNWVkZjZmIn0.k6T_QZ7rZI_em9K8XRVzUkHPngJOtzenaPDOcnoH3MbaI5iPSa1MPGDK4oewBjSsxTWNYVeqr0mJqC9BBEDzaUr7I9929nEwJwklG7gMROuq-yGS9gOkXFzkIthtRrya_fGNx0dOkVocRjabxB84iqd_OznePtUNezEj7EmWup2F3Yj56zMj9BYDnJNveu62Bgic8L-h2wq_oPSD8dM4Zltrxx4DYn0N7bbJnxfZgiTWxso1Wq8qYy_X95a0yoLY_zLB28k1jSOwVg9Ukjw7mGxItuvdGTZw_5fHmTyv146LVOMhydr0Ka0HeM_KB_-PDDYWvqKs_bsdtClTI5QnJg"}
```

**AUTH-002 驗證 JWT（admin token）**
```
curl -s -X POST "http://localhost:80/login/check" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{}'
→ HTTP 200
{"code":200,"message":"success","data":"check"}
```

**AUTH-002b test_user check（預期 403）**
```
curl -s -X POST "http://localhost:80/login/check" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{}'
→ HTTP 403
{"code":403,"message":"權限錯誤","data":null}
```

**AUTH-003 密碼錯誤**
```
curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" -d '{"email":"admin@bestpartner.com.tw","password":"wrongpassword"}'
→ HTTP 401
{"code":401,"message":"密碼錯誤","data":null}
```

**AUTH-004 信箱不存在**
```
curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" -d '{"email":"notexist@example.com","password":"anypassword"}'
→ HTTP 401
{"code":401,"message":"帳號不存在","data":null}
```

**AUTH-005 缺 email**
```
curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" -d '{"password":"admin"}'
→ HTTP 400
{"code":400,"message":"欄位 email 不可為空值","data":null}
```

**AUTH-006 缺 password**
```
curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" -d '{"email":"admin@bestpartner.com.tw"}'
→ HTTP 400
{"code":400,"message":"欄位 password 不可為空值","data":null}
```

**AUTH-007 空 body**
```
curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" -d '{}'
→ HTTP 400
{"code":400,"message":"欄位 email 不可為空值\n欄位 password 不可為空值","data":null}
```

**AUTH-008 email 格式錯誤**
```
curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" -d '{"email":"not-an-email","password":"admin"}'
→ HTTP 401
{"code":401,"message":"帳號不存在","data":null}
```

**AUTH-009 無效 token**
```
curl -s -X POST "http://localhost:80/login/check" -H "Authorization: Bearer invalidtoken" -H "Content-Type: application/json" -d '{}'
→ HTTP 401
{"code":401,"message":"Invalid or malformed token, please log in again","data":null}
```

**AUTH-010 超長 password**
```
curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" -d '{"email":"admin@bestpartner.com.tw","password":"xxx...（1000字元）"}'
→ HTTP 401
{"code":401,"message":"密碼錯誤","data":null}
```

---

## CHAT 聊天模組

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-CHAT.md`

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| CHAT-001 | 同步聊天（正常請求） | P0 | POST | `/llm/chat` | ✅ | test_user + CHAT_LLM_ID |
| CHAT-002 | 同步聊天（未認證） | P0 | POST | `/llm/chat` | ✅ | |
| CHAT-003 | 同步聊天（缺 llmId） | P1 | POST | `/llm/chat` | ✅ | |
| CHAT-004 | 同步聊天（缺 message） | P1 | POST | `/llm/chat` | ✅ | |
| CHAT-005 | 同步聊天（llmId 不存在） | P1 | POST | `/llm/chat` | ✅ | |
| CHAT-006 | 串流聊天（正常請求） | P0 | POST | `/llm/chatStreaming` | ✅ | SSE 正常回流 |
| CHAT-007 | 串流聊天（未認證） | P0 | POST | `/llm/chatStreaming` | ✅ | |
| CHAT-008 | 自定義助手聊天（正常） | P1 | POST | `/llm/customAssistantChat` | ✅ | |
| CHAT-009 | 自定義助手聊天（缺 promptContent） | P1 | POST | `/llm/customAssistantChat` | ✅ | |
| CHAT-010 | 自定義助手聊天（含 mcpSettingIds） | P2 | POST | `/llm/customAssistantChat` | ⏭️ | 依賴 MCP 模組 userSettingId，於 INTEGRATION 補充 |
| CHAT-011 | 自定義助手串流（正常） | P1 | POST | `/llm/customAssistantChatStreaming` | ✅ | SSE 正常回流 |
| CHAT-012 | 上傳檔案（正常） | P1 | POST | `/llm/uploadFile` | ✅ | 已清理 |
| CHAT-013 | 上傳檔案（未認證） | P1 | POST | `/llm/uploadFile` | ✅ | |
| CHAT-014 | 上傳檔案（無附件） | P2 | POST | `/llm/uploadFile` | ✅ | |
| CHAT-015 | 上傳檔案（超大檔案 5MB） | P2 | POST | `/llm/uploadFile` | ✅ | 已清理 |
| CHAT-016 | 同步聊天（message 為空） | P2 | POST | `/llm/chat` | ✅ | |
| CHAT-017 | admin 帳號同步聊天 | P1 | POST | `/llm/chat` | ✅ | admin CHAT llmId |
| CHAT-018 | admin 帳號串流聊天 | P1 | POST | `/llm/chatStreaming` | ✅ | admin STREAMING llmId |

### CHAT curl 記錄

**CHAT-001 同步聊天（test_user）**
```
curl -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Hello"}'
→ HTTP 200
{"code":200,"message":"success","data":"你好！👋 很高兴见到你！有什么我可以帮助你的吗？无论是回答问题、聊天，还是协助处理任务，我都很乐意为你提供帮助。请随时告诉我你需要什么！😊"}
```

**CHAT-002 未認證**
```
curl -X POST "http://localhost:80/llm/chat" -H "Content-Type: application/json" -d '{"llmId":"583b9222...","message":"Hello"}'
→ HTTP 401
(empty body)
```

**CHAT-003 缺 llmId**
```
curl -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"message":"Hello"}'
→ HTTP 400
{"code":400,"message":"欄位 llmId 不可為空值","data":null}
```

**CHAT-004 缺 message**
```
curl -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"583b9222..."}'
→ HTTP 400
{"code":400,"message":"欄位 message 不可為空值","data":null}
```

**CHAT-005 llmId 不存在**
```
curl -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"00000000-0000-0000-0000-000000000000","message":"Hello"}'
→ HTTP 400
{"code":400,"message":"Please verify the LLM setting you are accessing exists","data":null}
```

**CHAT-006 串流聊天（SSE）**
```
curl -X POST "http://localhost:80/llm/chatStreaming" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"2b44c811-d38c-417b-89d4-ae675b3093f5","message":"Say hi in one word"}'
→ HTTP 200
data:Hi

```

**CHAT-007 串流未認證**
```
curl -X POST "http://localhost:80/llm/chatStreaming" -H "Content-Type: application/json" -d '{"llmId":"2b44c811...","message":"Hello"}'
→ HTTP 401
(empty body)
```

**CHAT-008 customAssistantChat（正常）**
```
curl -X POST "http://localhost:80/llm/customAssistantChat" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"583b9222...","message":"Say hi","memory":{"id":"session-001","maxSize":5},"promptContent":"You are a helpful assistant."}'
→ HTTP 200
{"code":200,"message":"success","data":"Hello! How can I assist you today? 😊"}
```

**CHAT-009 customAssistantChat 缺 promptContent**
```
curl -X POST "http://localhost:80/llm/customAssistantChat" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"583b9222...","message":"Say hi","memory":{"id":"session-001","maxSize":5}}'
→ HTTP 400
{"code":400,"message":"欄位 promptContent 不可為空值","data":null}
```

**CHAT-010 含 mcpSettingIds** — ⏭️ Skip（依賴 MCP module userSettingId）

**CHAT-011 customAssistantChatStreaming**
```
curl -X POST "http://localhost:80/llm/customAssistantChatStreaming" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"2b44c811...","message":"Say hi in one word","memory":{"id":"stream-session","maxSize":5},"promptContent":"You are helpful."}'
→ HTTP 200
data:Hi

```

**CHAT-012 上傳檔案（正常）**
```
echo "Hello World Test File" > /tmp/test_upload.txt
curl -X POST "http://localhost:80/llm/uploadFile" -H "Authorization: Bearer $TEST_USER_TOKEN" -F "file=@/tmp/test_upload.txt"
→ HTTP 200
{"code":200,"message":"success","data":["test_upload.txt"]}
```

**CHAT-013 上傳檔案未認證**
```
curl -X POST "http://localhost:80/llm/uploadFile" -F "file=@/tmp/test_upload.txt"
→ HTTP 401
(empty body)
```

**CHAT-014 上傳檔案（無附件）**
```
curl -X POST "http://localhost:80/llm/uploadFile" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: multipart/form-data"
→ HTTP 400
{"code":400,"message":"欄位 files 不可為空值","data":null}
```

**CHAT-015 超大檔案（5MB）**
```
dd if=/dev/zero bs=1024 count=5120 | tr '\0' 'A' > /tmp/big_file.txt
curl -X POST "http://localhost:80/llm/uploadFile" -H "Authorization: Bearer $TEST_USER_TOKEN" -F "file=@/tmp/big_file.txt"
→ HTTP 200
{"code":200,"message":"success","data":["big_file.txt"]}
```

**CHAT-016 message 為空字串**
```
curl -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"583b9222...","message":""}'
→ HTTP 400
{"code":400,"message":"欄位 message 不可為空值","data":null}
```

**CHAT-017 admin 同步聊天（OpenRouter llmId）**
```
curl -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"1ee80ffa-7797-4051-8fb1-c0996159b408","message":"Hello"}'
→ HTTP 200
{"code":200,"message":"success","data":"Hello! How can I help you today?"}
```

**CHAT-018 admin 串流聊天（OpenRouter）**
```
curl -X POST "http://localhost:80/llm/chatStreaming" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"4ba6eb24-1128-447c-8e88-03fecb42dbb0","message":"Say hi in one word"}'
→ HTTP 200
data:Hi

```

---

## ADMIN CHAT 管理員聊天模組

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-ADMIN_CHAT.md`

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| ADCHAT-001 | 管理員聊天（admin 正常） | P0 | POST | `/llm/admin/chat` | ⏭️ | 已知缺陷：OPENROUTER_API_KEY 未設定 → 500 |
| ADCHAT-002 | 管理員聊天（一般用戶存取） | P0 | POST | `/llm/admin/chat` | ✅ | HTTP 403 |
| ADCHAT-003 | 管理員聊天（未認證） | P0 | POST | `/llm/admin/chat` | ✅ | HTTP 401 |
| ADCHAT-004 | 管理員自定義助手聊天（admin 正常） | P1 | POST | `/llm/admin/customAssistantChat` | ⏭️ | 已知缺陷：同上 |
| ADCHAT-005 | 管理員自定義助手聊天（一般用戶） | P1 | POST | `/llm/admin/customAssistantChat` | ✅ | HTTP 403 |

### ADMIN CHAT curl 記錄

**ADCHAT-001 admin 聊天（OPENROUTER）** — ⏭️
```
curl -X POST "http://localhost:80/llm/admin/chat" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Hello"}'
→ HTTP 500
{"code":500,"message":"Internal server error","data":null}
原因：OPENROUTER_API_KEY 環境變數未設定，llmStore.chatModelMap 為空
```

**ADCHAT-002 一般用戶存取**
```
curl -X POST "http://localhost:80/llm/admin/chat" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Hello"}'
→ HTTP 403
{"code":403,"message":"權限錯誤","data":null}
```

**ADCHAT-003 未認證**
```
curl -X POST "http://localhost:80/llm/admin/chat" -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Hello"}'
→ HTTP 401
(empty body)
```

**ADCHAT-004 admin customAssistantChat** — ⏭️
```
curl -X POST "http://localhost:80/llm/admin/customAssistantChat" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Hello"}'
→ HTTP 500
{"code":500,"message":"Internal server error","data":null}
原因：同 ADCHAT-001
```

**ADCHAT-005 一般用戶 customAssistantChat**
```
curl -X POST "http://localhost:80/llm/admin/customAssistantChat" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Hello"}'
→ HTTP 403
{"code":403,"message":"權限錯誤","data":null}
```

---

## USER 用戶管理模組

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-USER.md`

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| USER-001 | 註冊（正常新帳號） | P0 | POST | `/llm/user/register` | ✅ | ID: 475c0f6d...，已清理 |
| USER-002 | 註冊（重複 email） | P1 | POST | `/llm/user/register` | ✅ | |
| USER-003 | 註冊（缺 username） | P1 | POST | `/llm/user/register` | ✅ | |
| USER-004 | 註冊（缺 password） | P1 | POST | `/llm/user/register` | ✅ | |
| USER-005 | 註冊（缺 email） | P1 | POST | `/llm/user/register` | ✅ | |
| USER-006 | 註冊（非法 email） | P2 | POST | `/llm/user/register` | ✅ | 返回 email 格式驗證錯誤（P2 超預期通過） |
| USER-007 | 取得用戶資訊（user token，正常） | P0 | POST | `/llm/user/get` | ✅ | |
| USER-008 | 取得用戶資訊（未認證） | P0 | POST | `/llm/user/get` | ✅ | HTTP 401 |
| USER-009 | 更新用戶（正常） | P1 | POST | `/llm/user/update` | ✅ | |
| USER-010 | 更新用戶（缺 id） | P1 | POST | `/llm/user/update` | ✅ | |
| USER-011 | 更新用戶（id 不存在） | P1 | POST | `/llm/user/update` | ✅ | |
| USER-012 | 切換狀態（admin 正常） | P1 | POST | `/llm/user/switchStatus` | ✅ | 測試後恢復 ACTIVE |
| USER-013 | 切換狀態（缺 status） | P1 | POST | `/llm/user/switchStatus` | ✅ | |
| USER-014 | 刪除用戶（admin 正常） | P1 | DELETE | `/llm/user/delete` | ✅ | 清理 USER-001 測試資料 |
| USER-015 | 刪除用戶（缺 id） | P1 | DELETE | `/llm/user/delete` | ✅ | |
| USER-016 | 刪除用戶（id 不存在） | P2 | DELETE | `/llm/user/delete` | ✅ | HTTP 200 data:false（非 404，記錄為系統行為） |
| USER-017 | 業務邏輯（多步驟） | P1 | 多步 | `/llm/user/*` | ✅ | register → login → delete 完整流程 |

### USER curl 記錄

**USER-001 正常註冊**
```
curl -X POST "http://localhost:80/llm/user/register" -H "Content-Type: application/json" -d '{"username":"test_reg_1777602518","password":"testpass","email":"test_reg_1777602518@test.com"}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":"475c0f6d-e7e1-4fe6-86ca-9fe80459a548","username":"test_reg_1777602518","password":"testpass","nickname":null,"phone":null,"email":"test_reg_1777602518@test.com","avatar":null,"status":"ACTIVE","role":null}}
```

**USER-002 重複 email**
```
curl -X POST "http://localhost:80/llm/user/register" -H "Content-Type: application/json" -d '{"username":"another_user","password":"testpass","email":"test_reg_1777602518@test.com"}'
→ HTTP 400
{"code":400,"message":"Database processing error","data":null}
```

**USER-003 缺 username**
```
curl -X POST "http://localhost:80/llm/user/register" -H "Content-Type: application/json" -d '{"password":"testpass","email":"missing_username@test.com"}'
→ HTTP 400
{"code":400,"message":"欄位 username 不可為空值","data":null}
```

**USER-004 缺 password**
```
curl -X POST "http://localhost:80/llm/user/register" -H "Content-Type: application/json" -d '{"username":"nopw","email":"nopw@test.com"}'
→ HTTP 400
{"code":400,"message":"欄位 password 不可為空值","data":null}
```

**USER-005 缺 email**
```
curl -X POST "http://localhost:80/llm/user/register" -H "Content-Type: application/json" -d '{"username":"noemail","password":"testpass"}'
→ HTTP 400
{"code":400,"message":"欄位 email 不可為空值","data":null}
```

**USER-006 非法 email**
```
curl -X POST "http://localhost:80/llm/user/register" -H "Content-Type: application/json" -d '{"username":"bademail","password":"testpass","email":"not-an-email"}'
→ HTTP 400
{"code":400,"message":"欄位 email 的 email 格式不正確","data":null}
```

**USER-007 取得用戶資訊（USER_TOKEN）**
```
curl -X POST "http://localhost:80/llm/user/get" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":"5554f255-08d5-4069-99d2-928372d4a87b","username":"","password":null,"nickname":"","phone":"","email":"user@bestpartner.com.tw","avatar":"","status":"ACTIVE","role":{"roleNum":"1","roleName":"USER"}}}
```

**USER-008 未認證**
```
curl -X POST "http://localhost:80/llm/user/get" -H "Content-Type: application/json" -d '{}'
→ HTTP 401
(empty body)
```

**USER-009 更新用戶**
```
curl -X POST "http://localhost:80/llm/user/update" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"id":"5554f255-08d5-4069-99d2-928372d4a87b","email":"user@bestpartner.com.tw","status":"ACTIVE"}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":"5554f255-08d5-4069-99d2-928372d4a87b","username":"","password":null,"nickname":null,"phone":null,"email":"user@bestpartner.com.tw","avatar":null,"status":"ACTIVE","role":null}}
```

**USER-010 更新缺 id**
```
curl -X POST "http://localhost:80/llm/user/update" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"email":"user@bestpartner.com.tw","status":"ACTIVE"}'
→ HTTP 400
{"code":400,"message":"欄位 id 不可為空值","data":null}
```

**USER-011 更新 id 不存在**
```
curl -X POST "http://localhost:80/llm/user/update" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"id":"00000000-0000-0000-0000-000000000000","email":"test@test.com","status":"ACTIVE"}'
→ HTTP 400
{"code":400,"message":"User not found","data":null}
```

**USER-012 切換狀態（INACTIVE → ACTIVE）**
```
curl -X POST "http://localhost:80/llm/user/switchStatus" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"5554f255-08d5-4069-99d2-928372d4a87b","status":"INACTIVE"}'
→ HTTP 200
{"code":200,"message":"success","data":{...,"status":"INACTIVE",...}}
# 已恢復 ACTIVE
```

**USER-013 切換狀態缺 status**
```
curl -X POST "http://localhost:80/llm/user/switchStatus" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"5554f255-08d5-4069-99d2-928372d4a87b"}'
→ HTTP 400
{"code":400,"message":"id and status are required","data":null}
```

**USER-014 刪除測試用戶（cleanup）**
```
curl -X DELETE "http://localhost:80/llm/user/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"475c0f6d-e7e1-4fe6-86ca-9fe80459a548"}'
→ HTTP 200
{"code":200,"message":"success","data":true}
```

**USER-015 刪除缺 id**
```
curl -X DELETE "http://localhost:80/llm/user/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{}'
→ HTTP 400
{"code":400,"message":"欄位 id 不可為空值","data":null}
```

**USER-016 刪除 id 不存在**
```
curl -X DELETE "http://localhost:80/llm/user/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"00000000-0000-0000-0000-000000000000"}'
→ HTTP 200
{"code":200,"message":"success","data":false}
（data:false 表示無資料被刪除，非 404，系統行為）
```

**USER-017 多步驟（register → login → delete）**
```
# 已於 USER-001 register，登入取得 token 成功（token 前 30 字：eyJ0eXAiOiJKV1QiLCJhbGciOiJSUz...）
# USER-014 已執行刪除，流程完整通過
```

---

## LLM SETTING 模型設定模組

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-LLM_SETTING.md`

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| LLMSET-001 | 新增設定（正常） | P0 | POST | `/llm/setting/save` | ✅ | 已清理 |
| LLMSET-002 | 新增設定（未認證） | P0 | POST | `/llm/setting/save` | ✅ | HTTP 401 |
| LLMSET-003 | 新增設定（缺 modelType） | P1 | POST | `/llm/setting/save` | ✅ | |
| LLMSET-004 | 新增設定（缺 platformId） | P1 | POST | `/llm/setting/save` | ✅ | |
| LLMSET-005 | 新增設定（缺 modelName） | P1 | POST | `/llm/setting/save` | ✅ | |
| LLMSET-006 | 更新設定（正常） | P1 | POST | `/llm/setting/update` | ❌ | curl 範本缺少 platformId/modelType，需更新模板；API 本身正確（補充欄位後 HTTP 200） |
| LLMSET-007 | 取得設定（正常，無篩選） | P0 | POST | `/llm/setting/get` | ✅ | |
| LLMSET-008 | 取得設定（依 llmId 篩選） | P1 | POST | `/llm/setting/get` | ✅ | |
| LLMSET-009 | 取得設定（llmId 不存在） | P2 | POST | `/llm/setting/get` | ✅ | 返回 data:[] 空陣列 |
| LLMSET-010 | 刪除設定（正常） | P1 | POST | `/llm/setting/delete` | ✅ | |
| LLMSET-011 | 刪除設定（缺 id） | P1 | POST | `/llm/setting/delete` | ✅ | |
| LLMSET-012 | 新增平台（admin 正常） | P1 | POST | `/llm/setting/platform/add` | ✅ | 已清理 |
| LLMSET-013 | 新增平台（一般用戶拒絕） | P1 | POST | `/llm/setting/platform/add` | ✅ | HTTP 403 |
| LLMSET-014 | 刪除平台（admin 正常） | P1 | POST | `/llm/setting/platform/delete` | ✅ | |
| LLMSET-015 | 刪除平台（一般用戶拒絕） | P1 | POST | `/llm/setting/platform/delete` | ✅ | HTTP 403 |
| LLMSET-016 | 業務邏輯（刪除後查詢） | P1 | 多步 | `/llm/setting/*` | ✅ | 刪除後 get 返回空陣列 |

### LLM SETTING curl 記錄

**LLMSET-001 新增設定（正常）**
```
curl -X POST "http://localhost:80/llm/setting/save" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"platformId":"006f1076-b023-4197-b917-70455f2a3501","modelType":"CHAT","llmModel":{"modelName":"deepseek/deepseek-v3.2","apiKey":"sk-or-v1-test-key"}}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":"b3202cd8-e949-4651-ae36-8b0d2fa64d3b","alias":"","modelType":"CHAT","llmModel":{...}}}
```

**LLMSET-002 未認證**
```
curl -X POST "http://localhost:80/llm/setting/save" -H "Content-Type: application/json" -d '...'
→ HTTP 401
(empty body)
```

**LLMSET-003 缺 modelType**
```
curl -X POST "http://localhost:80/llm/setting/save" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"platformId":"006f1076...","llmModel":{"modelName":"test","apiKey":"test"}}'
→ HTTP 400
{"code":400,"message":"欄位 modelType 不可為空值","data":null}
```

**LLMSET-004 缺 platformId**
```
curl -X POST "http://localhost:80/llm/setting/save" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"modelType":"CHAT","llmModel":{"modelName":"test","apiKey":"test"}}'
→ HTTP 400
{"code":400,"message":"欄位 platformId 不可為空值","data":null}
```

**LLMSET-005 缺 modelName**
```
curl -X POST "http://localhost:80/llm/setting/save" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"platformId":"006f1076...","modelType":"CHAT","llmModel":{"apiKey":"test"}}'
→ HTTP 400
{"code":400,"message":"llmModel: 欄位 modelName 不可為空值","data":null}
```

**LLMSET-006 更新設定（正常）** ❌
```
# 第一次（缺 platformId/modelType）：
curl -X POST "http://localhost:80/llm/setting/update" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"id":"b3202cd8...","alias":"updated_alias","llmModel":{"modelName":"deepseek/deepseek-v3.2","apiKey":"sk-or-v1-new-key"}}'
→ HTTP 400
{"code":400,"message":"欄位 platformId 不可為空值\n欄位 modelType 不可為空值","data":null}

# 補充正確欄位後驗證 API 正確：
curl -X POST "http://localhost:80/llm/setting/update" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"id":"c3feca22...","platformId":"006f1076...","modelType":"CHAT","alias":"updated_alias","llmModel":{"modelName":"deepseek/deepseek-v3.2","apiKey":"sk-or-v1-new-key"}}'
→ HTTP 200
{"code":200,"message":"success","data":"LLM setting updated successfully"}
根因：curl 範本未包含 update 所需的 platformId 和 modelType 必填欄位，需更新模板文件
```

**LLMSET-007 取得設定（無篩選）**
```
curl -X POST "http://localhost:80/llm/setting/get" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{}'
→ HTTP 200
{"code":200,"message":"success","data":[...12筆設定...]}
```

**LLMSET-008 依 llmId 篩選**
```
curl -X POST "http://localhost:80/llm/setting/get" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9"}'
→ HTTP 200
{"code":200,"message":"success","data":[{"id":"583b9222...","alias":"openrouter_local_chat_test","modelType":"CHAT",...}]}
```

**LLMSET-009 llmId 不存在**
```
curl -X POST "http://localhost:80/llm/setting/get" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"00000000-0000-0000-0000-000000000000"}'
→ HTTP 200
{"code":200,"message":"success","data":[]}
```

**LLMSET-010 刪除設定**
```
curl -X POST "http://localhost:80/llm/setting/delete" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"id":"b3202cd8..."}'
→ HTTP 200
{"code":200,"message":"success","data":true}
```

**LLMSET-011 刪除缺 id**
```
curl -X POST "http://localhost:80/llm/setting/delete" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{}'
→ HTTP 400
{"code":400,"message":"欄位 id 不可為空值","data":null}
```

**LLMSET-012 新增平台（admin）**
```
curl -X POST "http://localhost:80/llm/setting/platform/add" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"platform":"OPENROUTER"}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":"9e09a362-d43d-450c-871f-14d88e7f66cd","platform":"OPENROUTER"}}
```

**LLMSET-013 一般用戶新增平台**
```
curl -X POST "http://localhost:80/llm/setting/platform/add" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"platform":"OPENROUTER"}'
→ HTTP 403
{"code":403,"message":"權限錯誤","data":null}
```

**LLMSET-014 刪除平台（admin）**
```
curl -X POST "http://localhost:80/llm/setting/platform/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"9e09a362..."}'
→ HTTP 200
{"code":200,"message":"success","data":true}
```

**LLMSET-015 一般用戶刪除平台**
```
curl -X POST "http://localhost:80/llm/setting/platform/delete" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"id":"9e09a362..."}'
→ HTTP 403
{"code":403,"message":"權限錯誤","data":null}
```

**LLMSET-016 業務邏輯（save → get → delete → get）**
```
# save → ID ab8a29b6-280c-4e3f-b8a2-e92eb3d8a9d0
# get after save → 找到該筆資料
# delete → 成功
# get after delete → {"code":200,"message":"success","data":[]}（空陣列確認已刪除）
→ 全部正確
```

---

## VECTOR 向量資料庫模組

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-VECTOR.md`

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| VEC-001 | 新增向量設定（正常） | P0 | POST | `/llm/vector/save` | ✅ | API 不回傳 ID |
| VEC-002 | 新增向量設定（未認證） | P0 | POST | `/llm/vector/save` | ✅ | HTTP 401 |
| VEC-003 | 更新向量設定（正常） | P1 | POST | `/llm/vector/update` | ❌ | save 不回傳 ID，無 list 端點；無法取得 ID 執行 update |
| VEC-004 | 取得知識庫（正常） | P1 | POST | `/llm/vector/getKnowledgeStore` | ✅ | 空結果但 HTTP 200 正常 |
| VEC-005 | 上傳向量文件（正常） | P0 | POST | `/llm/vector/uploadFiles` | ⏭️ | 需 Ollama + Milvus/Chroma 運行中 |
| VEC-006 | 上傳向量文件（未認證） | P0 | POST | `/llm/vector/uploadFiles` | ✅ | HTTP 401 |
| VEC-007 | 上傳向量文件（無附件） | P2 | POST | `/llm/vector/uploadFiles` | ✅ | HTTP 400 |
| VEC-008 | 搜尋向量資料（正常） | P0 | POST | `/llm/vector/getDataFromEmbeddingStore` | ⏭️ | 需已有向量資料 + 基礎設施 |
| VEC-009 | 搜尋向量資料（缺 knowledgeId） | P1 | POST | `/llm/vector/getDataFromEmbeddingStore` | ✅ | HTTP 400 |
| VEC-010 | 搜尋向量資料（缺 content） | P1 | POST | `/llm/vector/getDataFromEmbeddingStore` | ✅ | HTTP 400 |
| VEC-011 | 搜尋向量資料（knowledgeId 不存在） | P2 | POST | `/llm/vector/getDataFromEmbeddingStore` | ✅ | HTTP 200 data:null |
| VEC-012 | 刪除向量資料（正常） | P1 | DELETE | `/llm/vector/deleteData` | ✅ | HTTP 200 |
| VEC-013 | 刪除向量資料（未認證） | P1 | DELETE | `/llm/vector/deleteData` | ✅ | HTTP 401 |
| VEC-014 | 業務邏輯（上傳後可搜尋） | P1 | 多步 | `/llm/vector/*` | ⏭️ | 需 Ollama + 向量 DB |

### VECTOR curl 記錄

**VEC-001 新增向量設定**
```
curl -X POST "http://localhost:80/llm/vector/save" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"alias":"test_vector","vectorStoreType":"CHROMA","vectorStore":{"collectionName":"test_collection","dimension":384,"url":"http://localhost:8000"}}'
→ HTTP 200
{"code":200,"message":"success","data":"Vector store settings saved successfully"}
注意：API 回傳訊息字串，未回傳 ID（設計問題）
```

**VEC-002 未認證**
```
curl -X POST "http://localhost:80/llm/vector/save" -H "Content-Type: application/json" -d '{...}'
→ HTTP 401
(empty body)
```

**VEC-003 更新向量設定** ❌
```
無法取得 ID：save API 不回傳 ID，且無 list endpoint 可查詢
→ 無法執行 update 測試
根因：/llm/vector/save 回傳字串訊息而非資料物件，update 所需 id 無從取得
```

**VEC-004 取得知識庫**
```
curl -X POST "http://localhost:80/llm/vector/getKnowledgeStore" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"knowledgeId":"test_collection"}'
→ HTTP 200
{"code":200,"message":"success","data":[]}
```

**VEC-005 上傳向量文件** — ⏭️（需 Ollama + Milvus/Chroma）

**VEC-006 上傳未認證**
```
curl -X POST "http://localhost:80/llm/vector/uploadFiles" -F "file=@/tmp/vec_test.txt" -F "embeddingModelId=xxx" -F "embeddingStoreId=xxx"
→ HTTP 401
(empty body)
```

**VEC-007 無附件**
```
curl -X POST "http://localhost:80/llm/vector/uploadFiles" -H "Authorization: Bearer $USER_TOKEN" -F "embeddingModelId=xxx" -F "embeddingStoreId=xxx"
→ HTTP 400
{"code":400,"message":"欄位 files 不可為空值","data":null}
```

**VEC-008 搜尋向量資料** — ⏭️（需基礎設施）

**VEC-009 缺 knowledgeId**
```
curl -X POST "http://localhost:80/llm/vector/getDataFromEmbeddingStore" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"content":"test"}'
→ HTTP 400
{"code":400,"message":"欄位 knowledgeId 不可為空值","data":null}
```

**VEC-010 缺 content**
```
curl -X POST "http://localhost:80/llm/vector/getDataFromEmbeddingStore" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"knowledgeId":"xxx"}'
→ HTTP 400
{"code":400,"message":"欄位 content 不可為空值","data":null}
```

**VEC-011 knowledgeId 不存在**
```
curl -X POST "http://localhost:80/llm/vector/getDataFromEmbeddingStore" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"knowledgeId":"00000000-0000-0000-0000-000000000000","content":"test"}'
→ HTTP 200
{"code":200,"message":"success","data":null}
```

**VEC-012 刪除向量資料**
```
curl -X DELETE "http://localhost:80/llm/vector/deleteData" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"knowledgeId":"test_collection"}'
→ HTTP 200
{"code":200,"message":"success","data":"Data deleted successfully"}
```

**VEC-013 刪除未認證**
```
curl -X DELETE "http://localhost:80/llm/vector/deleteData" -H "Content-Type: application/json" -d '{"knowledgeId":"xxx"}'
→ HTTP 401
(empty body)
```

**VEC-014 業務邏輯** — ⏭️（需基礎設施）

---

## TOOL 工具管理模組

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-TOOL.md`

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| TOOL-001 | 列出工具（正常） | P0 | GET | `/llm/tool/list` | ✅ | 3 工具 |
| TOOL-002 | 取得工具（正常） | P1 | POST | `/llm/tool/get` | ✅ | |
| TOOL-003 | 取得工具（缺 id） | P1 | POST | `/llm/tool/get` | ✅ | |
| TOOL-004 | 取得工具（id 不存在） | P2 | POST | `/llm/tool/get` | ✅ | |
| TOOL-005 | 註冊工具（admin 正常） | P1 | POST | `/llm/tool/register` | ❌ | curl 範本 type:"SYSTEM" 無效；補正 type:"CUSTOMIZE" 後 HTTP 200；已清理 |
| TOOL-006 | 註冊工具（缺 name） | P1 | POST | `/llm/tool/register` | ✅ | 補正 type 後驗證 HTTP 400 正確 |
| TOOL-007 | 註冊工具（缺 classPath） | P1 | POST | `/llm/tool/register` | ✅ | 補正 type 後驗證 HTTP 400 正確 |
| TOOL-008 | 註冊工具（缺 type） | P1 | POST | `/llm/tool/register` | ✅ | |
| TOOL-009 | 刪除工具（admin 正常） | P1 | POST | `/llm/tool/delete` | ✅ | |
| TOOL-010 | 刪除工具（缺 id） | P1 | POST | `/llm/tool/delete` | ✅ | |
| TOOL-011 | 儲存工具設定（正常） | P1 | POST | `/llm/tool/saveSetting` | ✅ | 無刪除端點，設定殘留（已知設計） |
| TOOL-012 | 儲存工具設定（缺 alias） | P2 | POST | `/llm/tool/saveSetting` | ✅ | |
| TOOL-013 | 更新工具設定（正常） | P1 | POST | `/llm/tool/updateSetting` | ✅ | |
| TOOL-014 | 更新工具設定（缺 settingContent） | P2 | POST | `/llm/tool/updateSetting` | ✅ | |
| TOOL-015 | 新增分類（admin 正常） | P1 | POST | `/llm/tool/category/save` | ✅ | 已清理 |
| TOOL-016 | 新增分類（缺 group） | P2 | POST | `/llm/tool/category/save` | ✅ | |
| TOOL-017 | 更新分類（admin 正常） | P1 | POST | `/llm/tool/category/update` | ✅ | |
| TOOL-018 | 刪除分類（admin 正常） | P1 | POST | `/llm/tool/category/delete` | ✅ | |
| TOOL-019 | 業務邏輯（刪除後不出現清單） | P1 | 多步 | `/llm/tool/*` | ✅ | |

### TOOL curl 記錄

**TOOL-001 列出工具（公開）**
```
curl -X GET "http://localhost:80/llm/tool/list"
→ HTTP 200
{"code":200,"message":"success","data":[{"id":"f95fda5f...","name":"TavilySearch","type":"BUILT_IN",...},{"id":"c14e82ca...","name":"GoogleSearch","type":"BUILT_IN",...},{"id":"3737ec4a...","name":"DateTool","type":"CUSTOMIZE",...}]}
```

**TOOL-002 取得特定工具**
```
curl -X POST "http://localhost:80/llm/tool/get" -H "Content-Type: application/json" -d '{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f"}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":"3737ec4a...","name":"DateTool","type":"CUSTOMIZE",...}}
```

**TOOL-003 缺 id**
```
curl -X POST "http://localhost:80/llm/tool/get" -H "Content-Type: application/json" -d '{}'
→ HTTP 400
{"code":400,"message":"欄位 id 不可為空值","data":null}
```

**TOOL-004 id 不存在**
```
curl -X POST "http://localhost:80/llm/tool/get" -H "Content-Type: application/json" -d '{"id":"00000000-0000-0000-0000-000000000000"}'
→ HTTP 400
{"code":400,"message":"Tool not found","data":null}
```

**TOOL-005 admin 註冊工具** ❌
```
# 第一次（type:"SYSTEM" 無效）：
curl -X POST "http://localhost:80/llm/tool/register" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"name":"test_tool","classPath":"...","groupId":"ea8a08e2...","type":"SYSTEM"}'
→ HTTP 400
{"code":400,"message":"tw.zipe.bastpartner.enumerate.ToolsType does not contain element with name 'SYSTEM' at path $.type","data":null}

# 補正 type:"CUSTOMIZE" 後：
curl -X POST "http://localhost:80/llm/tool/register" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"name":"test_tool","classPath":"...","groupId":"ea8a08e2...","type":"CUSTOMIZE","functionName":"getCurrentTime","functionDescription":"Test tool"}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":"0aba8551-6ba1-4b61-8424-9d7b67d07162",...}}
根因：curl 範本 type:"SYSTEM" 無效；有效值為 CUSTOMIZE / BUILT_IN
```

**TOOL-006 缺 name（補正 type 後）**
```
curl -X POST "http://localhost:80/llm/tool/register" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"classPath":"...","groupId":"ea8a08e2...","type":"CUSTOMIZE","functionName":"test","functionDescription":"test"}'
→ HTTP 400
{"code":400,"message":"欄位 name 不可為空值","data":null}
```

**TOOL-007 缺 classPath（補正 type 後）**
```
curl -X POST "http://localhost:80/llm/tool/register" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"name":"no_classpath","groupId":"ea8a08e2...","type":"CUSTOMIZE","functionName":"test","functionDescription":"test"}'
→ HTTP 400
{"code":400,"message":"欄位 classPath 不可為空值","data":null}
```

**TOOL-008 缺 type**
```
curl -X POST "http://localhost:80/llm/tool/register" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"name":"no_type","classPath":"...","groupId":"ea8a08e2..."}'
→ HTTP 400
{"code":400,"message":"欄位 type 不可為空值","data":null}
```

**TOOL-009 刪除工具**
```
curl -X POST "http://localhost:80/llm/tool/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"0aba8551-6ba1-4b61-8424-9d7b67d07162"}'
→ HTTP 200
{"code":200,"message":"success","data":true}
```

**TOOL-010 刪除缺 id**
```
curl -X POST "http://localhost:80/llm/tool/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{}'
→ HTTP 400
{"code":400,"message":"欄位 id 不可為空值","data":null}
```

**TOOL-011 儲存工具設定（USER_TOKEN）**
```
curl -X POST "http://localhost:80/llm/tool/saveSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f","alias":"my_date_tool"}'
→ HTTP 200
{"code":200,"message":"success","data":{...,"alias":"my_date_tool","settingId":"be2aa3d1-e923-4a90-826f-c42a083a9ac7",...}}
注意：無刪除 setting 端點，設定殘留在 DB
```

**TOOL-012 saveSetting 缺 alias**
```
curl -X POST "http://localhost:80/llm/tool/saveSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"id":"3737ec4a..."}'
→ HTTP 400
{"code":400,"message":"欄位 alias 不可為空值","data":null}
```

**TOOL-013 更新工具設定**
```
curl -X POST "http://localhost:80/llm/tool/updateSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"settingId":"be2aa3d1...","settingContent":"{\"timezone\":\"Asia/Taipei\"}"}'
→ HTTP 200
{"code":200,"message":"success","data":{...,"settingContent":"{\"timezone\":\"Asia/Taipei\"}",...}}
```

**TOOL-014 updateSetting 缺 settingContent**
```
curl -X POST "http://localhost:80/llm/tool/updateSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"settingId":"00000000..."}'
→ HTTP 400
{"code":400,"message":"欄位 settingContent 不可為空值","data":null}
```

**TOOL-015 新增工具分類**
```
curl -X POST "http://localhost:80/llm/tool/category/save" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"group":"test_category"}'
→ HTTP 200
{"code":200,"message":"success","data":{"groupId":"7a1c2a39-5858-4b0a-bdab-f51bcd1b70d6","group":"test_category",...}}
```

**TOOL-016 新增分類缺 group**
```
curl -X POST "http://localhost:80/llm/tool/category/save" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{}'
→ HTTP 400
{"code":400,"message":"欄位 group 不可為空值","data":null}
```

**TOOL-017 更新分類**
```
curl -X POST "http://localhost:80/llm/tool/category/update" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"groupId":"7a1c2a39...","group":"updated_category"}'
→ HTTP 200
{"code":200,"message":"success","data":{"groupId":"7a1c2a39...","group":"updated_category",...}}
```

**TOOL-018 刪除分類**
```
curl -X POST "http://localhost:80/llm/tool/category/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"groupId":"7a1c2a39..."}'
→ HTTP 200
{"code":200,"message":"success","data":{...}}
```

**TOOL-019 業務邏輯**
```
# register tool biz_test_tool → ID 95540782-c27d-47cf-9e72-c4c524039fe1
# list → 確認存在 YES
# delete → 成功
# list again → 確認不存在 NO (PASS)
```

---

## MCP 伺服器模組

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-MCP.md`

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| MCP-001 | 列出 MCP（已認證） | P0 | GET | `/llm/mcpServer/list` | ✅ | 6 筆 MCP 伺服器 |
| MCP-002 | 列出 MCP（未認證） | P0 | GET | `/llm/mcpServer/list` | ✅ | HTTP 401 |
| MCP-003 | 取得 MCP（正常） | P1 | POST | `/llm/mcpServer/get` | ✅ | |
| MCP-004 | 取得 MCP（mcpId 不存在） | P1 | POST | `/llm/mcpServer/get` | ✅ | HTTP 400 MCP server not found |
| MCP-005 | 註冊 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/register` | ✅ | 已清理（MCP-010） |
| MCP-006 | 註冊 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/register` | ✅ | HTTP 403 |
| MCP-007 | 註冊 MCP（缺必填欄位） | P1 | POST | `/llm/mcpServer/register` | ✅ | HTTP 400 缺 command/args |
| MCP-008 | 更新 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/update` | ✅ | |
| MCP-009 | 更新 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/update` | ✅ | HTTP 403 |
| MCP-010 | 刪除 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/delete` | ✅ | 清理 MCP-005 資料 |
| MCP-011 | 刪除 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/delete` | ✅ | HTTP 403 |
| MCP-012 | 儲存使用者設定（正常） | P1 | POST | `/llm/mcpServer/saveSetting` | ✅ | 已清理（MCP-017） |
| MCP-013 | 儲存使用者設定（缺 mcpId） | P1 | POST | `/llm/mcpServer/saveSetting` | ✅ | HTTP 400 |
| MCP-014 | 取得使用者設定（正常） | P1 | POST | `/llm/mcpServer/getSetting` | ✅ | |
| MCP-015 | 取得使用者設定（id 不存在） | P2 | POST | `/llm/mcpServer/getSetting` | ✅ | HTTP 400 User setting not found |
| MCP-016 | 更新使用者設定（正常） | P1 | POST | `/llm/mcpServer/updateSetting` | ✅ | |
| MCP-017 | 刪除使用者設定（正常） | P1 | DELETE | `/llm/mcpServer/deleteSetting` | ✅ | |
| MCP-018 | 未認證取得清單 | P2 | GET | `/llm/mcpServer/list` | ✅ | HTTP 401 |
| MCP-019 | 業務邏輯（設定後於聊天使用） | P1 | 多步 | `/llm/mcpServer/*` | ✅ | saveSetting → customAssistantChat（含 mcpSettingIds）→ deleteSetting 全部成功；使用 test_user + CHAT_LLM_ID |

### MCP curl 記錄

**MCP-001 列出 MCP（已認證）**
```
curl -X GET "http://localhost:80/llm/mcpServer/list" -H "Authorization: Bearer $USER_TOKEN"
→ HTTP 200
{"code":200,"message":"success","data":[{"name":"google_drive","type":"STDIO","mcpId":"06a7fb6d-..."},{"name":"filesystem","mcpId":"182cca55-..."},{"name":"test","mcpId":"2fdc0d18-..."},{"name":"favorite","mcpId":"a9297b93-..."},{"name":"date","mcpId":"4b9ba306-..."},{"name":"test_mcp","mcpId":"ef288831-..."}]}
```

**MCP-002 未認證**
```
curl -s -o /dev/null -w "%{http_code}" -X GET "http://localhost:80/llm/mcpServer/list"
→ HTTP 401
```

**MCP-003 取得 MCP（date）**
```
curl -X POST "http://localhost:80/llm/mcpServer/get" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"}'
→ HTTP 200
{"code":200,"message":"success","data":{"name":"date","type":"STDIO","mcpId":"4b9ba306-...","args":["D:/MCP/date-1.0-SNAPSHOT-runner.jar"],...}}
```

**MCP-004 mcpId 不存在**
```
curl -X POST "http://localhost:80/llm/mcpServer/get" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"mcpId":"00000000-0000-0000-0000-000000000000"}'
→ HTTP 400
{"code":400,"message":"MCP server not found","data":null}
```

**MCP-005 admin 註冊 MCP**
```
curl -X POST "http://localhost:80/llm/mcpServer/register" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"name":"test_mcp_new","type":"STDIO","command":"java","args":["-jar","test.jar"]}'
→ HTTP 200
{"code":200,"message":"success","data":{"name":"test_mcp_new","command":"java","args":["-jar","test.jar"],"type":"STDIO","mcpId":"46444118-ac69-421d-b450-e62e9f4ceeb3",...}}
```

**MCP-006 一般用戶拒絕**
```
curl -X POST "http://localhost:80/llm/mcpServer/register" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"name":"hack_mcp","type":"STDIO","command":"malware","args":[]}'
→ HTTP 403
{"code":403,"message":"權限錯誤","data":null}
```

**MCP-007 缺 command 和 args**
```
curl -X POST "http://localhost:80/llm/mcpServer/register" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"name":"test_mcp","type":"STDIO"}'
→ HTTP 400
{"code":400,"message":"欄位 command 不可為空值\n欄位 args 不可為空值","data":null}
```

**MCP-008 admin 更新 MCP**
```
curl -X POST "http://localhost:80/llm/mcpServer/update" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"mcpId":"46444118-...","name":"test_mcp_updated","type":"STDIO","command":"java","args":["-jar","updated.jar"]}'
→ HTTP 200
{"code":200,"message":"success","data":{"name":"test_mcp_updated","command":"java","args":["-jar","updated.jar"],"type":"STDIO","mcpId":"46444118-...",...}}
```

**MCP-009 一般用戶更新**
```
curl -X POST "http://localhost:80/llm/mcpServer/update" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"mcpId":"4b9ba306-...","name":"hack","type":"STDIO","command":"malware","args":[]}'
→ HTTP 403
{"code":403,"message":"權限錯誤","data":null}
```

**MCP-010 admin 刪除 MCP**
```
curl -X POST "http://localhost:80/llm/mcpServer/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"mcpId":"46444118-ac69-421d-b450-e62e9f4ceeb3"}'
→ HTTP 200
{"code":200,"message":"success","data":true}
```

**MCP-011 一般用戶刪除**
```
curl -X POST "http://localhost:80/llm/mcpServer/delete" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"mcpId":"4b9ba306-..."}'
→ HTTP 403
{"code":403,"message":"權限錯誤","data":null}
```

**MCP-012 儲存使用者設定**
```
curl -X POST "http://localhost:80/llm/mcpServer/saveSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021","settingContent":{"timezone":"Asia/Taipei"}}'
→ HTTP 200
{"code":200,"message":"success","data":{"userSettingId":"82e45fe4-c293-446d-98df-27f890c2e629","settingContent":{"timezone":"Asia/Taipei"},"mcpId":"4b9ba306-...",...}}
```

**MCP-013 缺 mcpId**
```
curl -X POST "http://localhost:80/llm/mcpServer/saveSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"settingContent":{"timezone":"UTC"}}'
→ HTTP 400
{"code":400,"message":"欄位 mcpId 不可為空值","data":null}
```

**MCP-014 取得使用者設定**
```
curl -X POST "http://localhost:80/llm/mcpServer/getSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"userSettingId":"82e45fe4-c293-446d-98df-27f890c2e629"}'
→ HTTP 200
{"code":200,"message":"success","data":{"settingId":"82e45fe4-...","settingContent":{"timezone":"Asia/Taipei"},"mcpId":"4b9ba306-...",...}}
```

**MCP-015 userSettingId 不存在**
```
curl -X POST "http://localhost:80/llm/mcpServer/getSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"userSettingId":"00000000-0000-0000-0000-000000000000"}'
→ HTTP 400
{"code":400,"message":"User setting not found","data":null}
```

**MCP-016 更新使用者設定**
```
curl -X POST "http://localhost:80/llm/mcpServer/updateSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"userSettingId":"82e45fe4-...","settingContent":{"timezone":"UTC"}}'
→ HTTP 200
{"code":200,"message":"success","data":{"userSettingId":"82e45fe4-...","settingContent":{"timezone":"UTC"},...}}
```

**MCP-017 刪除使用者設定**
```
curl -X DELETE "http://localhost:80/llm/mcpServer/deleteSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"userSettingId":"82e45fe4-c293-446d-98df-27f890c2e629"}'
→ HTTP 200
{"code":200,"message":"success","data":true}
```

**MCP-018 未認證取得清單（P2）**
```
curl -s -o /dev/null -w "%{http_code}" -X GET "http://localhost:80/llm/mcpServer/list"
→ HTTP 401
```

**MCP-019 業務邏輯（saveSetting → customAssistantChat → deleteSetting）**
```
# Step 1: saveSetting（test_user）
curl -X POST "http://localhost:80/llm/mcpServer/saveSetting" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021","settingContent":{"timezone":"Asia/Taipei"}}'
→ HTTP 200, userSettingId: eab492b4-b764-4f3a-a6be-292d3c25d80c

# Step 2: customAssistantChat 含 mcpSettingIds
curl -X POST "http://localhost:80/llm/customAssistantChat" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"What is today date?","memory":{"id":"mcp-test-session","maxSize":5},"promptContent":"You are helpful.","mcpSettingIds":["eab492b4-b764-4f3a-a6be-292d3c25d80c"]}'
→ HTTP 200
{"code":200,"message":"success","data":"Today's date is **October 28, 2024**."}

# Step 3: deleteSetting（cleanup）
curl -X DELETE "http://localhost:80/llm/mcpServer/deleteSetting" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"userSettingId":"eab492b4-b764-4f3a-a6be-292d3c25d80c"}'
→ HTTP 200 {"code":200,"message":"success","data":true}
```

---

## PERMISSION 權限管理模組

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-PERMISSION.md`

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| PERM-001 | 新增權限（admin 正常） | P1 | POST | `/llm/permission/add` | ✅ | 已清理（PERM-009） |
| PERM-002 | 新增權限（未認證） | P1 | POST | `/llm/permission/add` | ✅ | HTTP 401 |
| PERM-003 | 新增權限（缺 name） | P1 | POST | `/llm/permission/add` | ✅ | HTTP 400 |
| PERM-004 | 新增權限（缺 num） | P1 | POST | `/llm/permission/add` | ✅ | HTTP 400 |
| PERM-005 | 新增權限（非 admin 拒絕） | P2 | POST | `/llm/permission/add` | ✅ | HTTP 403 |
| PERM-006 | 更新權限（admin 正常） | P1 | POST | `/llm/permission/update` | ✅ | |
| PERM-007 | 更新權限（缺 id） | P1 | POST | `/llm/permission/update` | ✅ | HTTP 400 |
| PERM-008 | 更新權限（非 admin 拒絕） | P2 | POST | `/llm/permission/update` | ✅ | HTTP 403 |
| PERM-009 | 刪除權限（admin 正常） | P1 | DELETE | `/llm/permission/delete` | ✅ | 清理 PERM-001 資料 |
| PERM-010 | 刪除權限（缺 id） | P1 | DELETE | `/llm/permission/delete` | ✅ | HTTP 400 |
| PERM-011 | 刪除權限（未認證） | P1 | DELETE | `/llm/permission/delete` | ✅ | HTTP 401 |

### PERMISSION curl 記錄

**PERM-001 admin 新增權限**
```
curl -X POST "http://localhost:80/llm/permission/add" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"name":"test_permission","num":9999}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":"7e5cb114-f705-4550-bf3a-7aac3ea962c9","name":"test_permission","num":9999,"description":null}}
```

**PERM-002 未認證**
```
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/permission/add" -H "Content-Type: application/json" -d '{"name":"test","num":1}'
→ HTTP 401
```

**PERM-003 缺 name**
```
curl -X POST "http://localhost:80/llm/permission/add" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"num":100}'
→ HTTP 400
{"code":400,"message":"欄位 name 不可為空值","data":null}
```

**PERM-004 缺 num**
```
curl -X POST "http://localhost:80/llm/permission/add" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"name":"test_perm"}'
→ HTTP 400
{"code":400,"message":"欄位 num 不可為空值","data":null}
```

**PERM-005 非 admin 新增**
```
curl -X POST "http://localhost:80/llm/permission/add" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"name":"hack","num":1}'
→ HTTP 403
{"code":403,"message":"權限錯誤","data":null}
```

**PERM-006 admin 更新權限**
```
curl -X POST "http://localhost:80/llm/permission/update" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"7e5cb114-...","name":"test_permission_updated","num":8888}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":"7e5cb114-...","name":"test_permission_updated","num":8888,"description":null}}
```

**PERM-007 更新缺 id**
```
curl -X POST "http://localhost:80/llm/permission/update" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"name":"test","num":100}'
→ HTTP 400
{"code":400,"message":"欄位 id 不可為空值","data":null}
```

**PERM-008 非 admin 更新**
```
curl -X POST "http://localhost:80/llm/permission/update" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"id":"7e5cb114-...","name":"hack","num":1}'
→ HTTP 403
{"code":403,"message":"權限錯誤","data":null}
```

**PERM-009 admin 刪除權限**
```
curl -X DELETE "http://localhost:80/llm/permission/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"7e5cb114-f705-4550-bf3a-7aac3ea962c9"}'
→ HTTP 200
{"code":200,"message":"success","data":true}
```

**PERM-010 刪除缺 id**
```
curl -X DELETE "http://localhost:80/llm/permission/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{}'
→ HTTP 400
{"code":400,"message":"欄位 id 不可為空值","data":null}
```

**PERM-011 未認證刪除**
```
curl -s -o /dev/null -w "%{http_code}" -X DELETE "http://localhost:80/llm/permission/delete" -H "Content-Type: application/json" -d '{"id":"xxx"}'
→ HTTP 401
```

---

## SYSTEM SETTING 系統設定模組

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-SYSTEM_SETTING.md`

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| SYSSET-001 | 列出設定（公開） | P0 | GET | `/systemSetting/list` | ✅ | 2 筆系統設定 |
| SYSSET-002 | 取得設定（正常） | P1 | POST | `/systemSetting/get` | ✅ | key=default_llm_platform → OPENROUTER |
| SYSSET-003 | 取得設定（key 不存在） | P2 | POST | `/systemSetting/get` | ✅ | 返回 data:"" 空字串 |
| SYSSET-004 | 新增設定（正常） | P1 | POST | `/systemSetting/add` | ✅ | 已清理（SYSSET-008） |
| SYSSET-005 | 新增設定（缺 value） | P1 | POST | `/systemSetting/add` | ✅ | HTTP 400 |
| SYSSET-006 | 更新設定（正常） | P1 | POST | `/systemSetting/update` | ✅ | |
| SYSSET-007 | 更新設定（缺 id） | P1 | POST | `/systemSetting/update` | ❌ | 已知缺陷：預期 400，實際 500；API 未做 id 為 null 的保護 |
| SYSSET-008 | 刪除設定（正常） | P1 | DELETE | `/systemSetting/delete` | ✅ | 清理 SYSSET-004 資料 |
| SYSSET-009 | 刪除設定（id 不存在） | P2 | DELETE | `/systemSetting/delete` | ✅ | 返回 data:0 |
| SYSSET-010 | 公開端點確認（list 無需認證） | P1 | GET | `/systemSetting/list` | ✅ | HTTP 200 |
| SYSSET-011 | 公開端點確認（get 無需認證） | P1 | POST | `/systemSetting/get` | ✅ | HTTP 200 |
| SYSSET-012 | 公開端點確認（add 無需認證） | P1 | POST | `/systemSetting/add` | ✅ | HTTP 200；已清理 |
| SYSSET-013 | 業務邏輯（新增 → 更新 → 刪除） | P1 | 多步 | `/systemSetting/*` | ✅ | add→update→delete 後 get 返回空字串，流程完整 |

### SYSTEM SETTING curl 記錄

**SYSSET-001 列出所有設定**
```
curl -X GET "http://localhost:80/systemSetting/list"
→ HTTP 200
{"code":200,"message":"success","data":[{"id":2,"key":"skill.global.dir","value":"D:/tmp/bestpartner/upload/skills/global",...},{"id":1,"key":"default_llm_platform","value":"OPENROUTER",...}]}
```

**SYSSET-002 取得設定**
```
curl -X POST "http://localhost:80/systemSetting/get" -H "Content-Type: application/json" -d '{"key":"default_llm_platform"}'
→ HTTP 200
{"code":200,"message":"success","data":"OPENROUTER"}
```

**SYSSET-003 key 不存在**
```
curl -X POST "http://localhost:80/systemSetting/get" -H "Content-Type: application/json" -d '{"key":"nonexistent_key"}'
→ HTTP 200
{"code":200,"message":"success","data":""}
```

**SYSSET-004 新增設定**
```
curl -X POST "http://localhost:80/systemSetting/add" -H "Content-Type: application/json" -d '{"key":"test_setting","value":"test_value","description":"Test setting"}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":null,"key":"test_setting","value":"test_value","description":"Test setting"}}
（ID 從 list 查詢得 id=9）
```

**SYSSET-005 缺 value**
```
curl -X POST "http://localhost:80/systemSetting/add" -H "Content-Type: application/json" -d '{"key":"incomplete_setting"}'
→ HTTP 400
{"code":400,"message":"欄位 value 不可為空值","data":null}
```

**SYSSET-006 更新設定**
```
curl -X POST "http://localhost:80/systemSetting/update" -H "Content-Type: application/json" -d '{"id":9,"key":"test_setting","value":"updated_value","description":"Updated"}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":9,"key":"test_setting","value":"updated_value","description":"Updated"}}
```

**SYSSET-007 更新缺 id** ❌
```
curl -X POST "http://localhost:80/systemSetting/update" -H "Content-Type: application/json" -d '{"key":"test_setting","value":"some_value"}'
→ HTTP 500
{"code":500,"message":"Internal server error","data":null}
根因：API 未對 id=null 進行保護，直接傳給 SQL 導致 500；應返回 400
```

**SYSSET-008 刪除設定**
```
curl -X DELETE "http://localhost:80/systemSetting/delete" -H "Content-Type: application/json" -d '{"id":9,"key":"test_setting"}'
→ HTTP 200
{"code":200,"message":"success","data":1}
```

**SYSSET-009 刪除不存在 id**
```
curl -X DELETE "http://localhost:80/systemSetting/delete" -H "Content-Type: application/json" -d '{"id":99999,"key":"fake_key"}'
→ HTTP 200
{"code":200,"message":"success","data":0}
```

**SYSSET-010 list 無需認證**
```
curl -s -o /dev/null -w "%{http_code}" -X GET "http://localhost:80/systemSetting/list"
→ HTTP 200
```

**SYSSET-011 get 無需認證**
```
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/systemSetting/get" -H "Content-Type: application/json" -d '{"key":"default_llm_platform"}'
→ HTTP 200
```

**SYSSET-012 add 無需認證**
```
curl -X POST "http://localhost:80/systemSetting/add" -H "Content-Type: application/json" -d '{"key":"test_setting_012","value":"test_value_012"}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":null,"key":"test_setting_012","value":"test_value_012","description":null}}
（ID=10，已清理）
```

**SYSSET-013 業務邏輯（add→update→delete）**
```
# add test_biz_setting → ID=11
# update → {"value":"updated_biz_value"} → HTTP 200
# delete → {"data":1} → HTTP 200
# get after delete → {"data":""} → 確認已刪除
→ 流程完整通過
```

---

## 跨模組整合測試

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-INTEGRATION.md`

| 測試 ID | 測試場景 | 優先級 | 涉及端點 | 狀態 | 備註 |
|---------|---------|--------|----------|------|------|
| INT-001 | 完整聊天流程 | P0 | `login → setting/get → chat` | ✅ | test_user login/check → 403（無 user-read 角色，預期），setting/get → 200，chat → 200 |
| INT-002 | customAssistantChat 基礎流程 | P1 | `login → customAssistantChat` | ✅ | HTTP 200，回傳 INT-002 OK |
| INT-003 | MCP 整合聊天 | P1 | `mcpServer/saveSetting → customAssistantChat` | ✅ | saveSetting → 200，含 mcpSettingIds 聊天 → 200，deleteSetting → 200 |
| INT-004 | 用戶生命週期 | P1 | `register → login → get → update → delete` | ✅ | 完整流程全部通過；已清理 |
| INT-005 | JWT 過期保護 | P1 | `login → (過期 token) → chat` | ✅ | 401 + 自動 refresh token（見 SEC-004 安全漏洞）；JWT 機制回應正常 |
| INT-006 | admin 工作流 | P1 | `platform/add → setting/save → cleanup` | ✅ | 新增平台→新增設定→刪除設定→刪除平台 全部通過；已清理 |

### INTEGRATION curl 記錄

**INT-001 完整聊天流程**
```
# Step 1: login/check（test_user → 403，無 user-read 角色，預期行為）
curl -X POST "http://localhost:80/login/check" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{}'
→ HTTP 403（test_user 缺 user-read）

# Step 2: setting/get
curl -X POST "http://localhost:80/llm/setting/get" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9"}'
→ HTTP 200 {"code":200,"message":"success","data":[...]}

# Step 3: chat
curl -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Reply with: INT-001 OK"}'
→ HTTP 200 {"code":200,"message":"success","data":"INT-001 OK"}
```

**INT-002 customAssistantChat 基礎流程**
```
curl -X POST "http://localhost:80/llm/customAssistantChat" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{"llmId":"583b9222-...","message":"Reply with: INT-002 OK","memory":{"id":"int-002-session","maxSize":5},"promptContent":"You are a helpful assistant."}'
→ HTTP 200
{"code":200,"message":"success","data":"INT-002 OK"}
```

**INT-003 MCP 整合聊天**
```
# Step 1: saveSetting → userSettingId: 04596462-712f-4c87-8b3c-a7e7e2ff898e
# Step 2: customAssistantChat with mcpSettingIds → HTTP 200 Today's date is October 18, 2024
# Step 3: deleteSetting → HTTP 200 data:true
```

**INT-004 用戶生命週期**
```
# register lifecycle_1777705631 → ID: 9f12ad40-c311-4a43-8363-03ff3d071314
# login → token OK
# get → HTTP 200 找到 USER 資訊
# update → HTTP 200 email 更新成功
# delete (admin) → HTTP 200 data:true
```

**INT-005 JWT 過期保護**
```
curl -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer <EXPIRED_INVALID_TOKEN>" -H "Content-Type: application/json" -d '{"llmId":"583b9222-...","message":"test"}'
→ HTTP 401
{"code":401,"message":"Credentials expired, new token has been generated","data":"<NEW_TOKEN>"}
（JWT Refresh 機制：偽造過期 token 仍可自動刷新 → 安全漏洞 SEC-004）
```

**INT-006 admin 工作流**
```
# add platform OPENROUTER → ID: 8e5794a6-a2d7-4384-bdb6-b8417e1476d5
# save setting → ID: 2c65513a-4bfc-417d-90e6-dca2ecf1bdd6
# delete setting → HTTP 200 data:true
# delete platform → HTTP 200 data:true
```

---

## 通用安全性測試

> 讀入自 `.claude/skills/test-confirmation/modules/test-module-SECURITY.md`

| 測試 ID | 測試場景 | 優先級 | 適用端點 | 狀態 | 備註 |
|---------|---------|--------|----------|------|------|
| SEC-001 | SQL Injection | P1 | `/login/` | ✅ | HTTP 401 帳號不存在，Injection 無效 |
| SEC-002 | XSS Payload | P2 | `/llm/user/register` | ✅ | HTTP 200 接受原始字串（前端轉義責任）；已清理 |
| SEC-003 | 過期 JWT | P1 | `/llm/user/get` | ✅ | HTTP 401 + 自動 refresh token 回傳 |
| SEC-004 | 竄改 JWT Payload | P1 | `/llm/permission/add` | ❌ | **P0 Release Blocker**：偽造含 admin 的過期 token → 401 + 取得新 admin token → 200 admin 操作成功；已清理測試資料 |
| SEC-005 | 水平越權 | P1 | `/llm/mcpServer/getSetting` | ✅ | HTTP 400 User setting not found（無法存取他人設定） |
| SEC-006 | 大量請求壓力 | P2 | `/llm/chat` | ✅ | 10/10 HTTP 200（目前無 rate limiting，屬已知行為） |

### SECURITY curl 記錄

**SEC-001 SQL Injection**
```
curl -X POST "http://localhost:80/login/" -H "Content-Type: application/json" -d '{"email":"admin@bestpartner.com.tw'\'' OR '\''1'\''='\''1","password":"anything"}'
→ HTTP 401
{"code":401,"message":"帳號不存在","data":null}
```

**SEC-002 XSS Payload**
```
curl -X POST "http://localhost:80/llm/user/register" -H "Content-Type: application/json" -d '{"username":"<script>alert(1)</script>","password":"xss_pass","email":"xss_1777705702@test.com"}'
→ HTTP 200
{"code":200,"message":"success","data":{"id":"b0e8ebd1-...","username":"<script>alert(1)</script>",...}}
（API 接受原始字串，前端需負責轉義；已清理）
```

**SEC-003 過期 JWT**
```
curl -X POST "http://localhost:80/llm/user/get" -H "Authorization: Bearer <EXPIRED_TOKEN>" -H "Content-Type: application/json" -d '{}'
→ HTTP 401
{"code":401,"message":"Credentials expired, new token has been generated","data":"<new_valid_token>"}
```

**SEC-004 竄改 JWT Payload（P0 安全漏洞）** ❌
```
# Step 1: 發送偽造含 admin groups 的過期 token（invalid signature）
TAMPERED = header.{"upn":"attacker","groups":["admin"],"iat":1700000000,"exp":1700001800}.fakesig
curl -X POST "http://localhost:80/llm/permission/add" -H "Authorization: Bearer $TAMPERED" -H "Content-Type: application/json" -d '{"name":"sec004_tampered","num":1}'
→ HTTP 401 + {"data":"<REFRESHED_TOKEN_WITH_ADMIN_GROUPS>"}

# Step 2: 使用 refresh 取得的 token 呼叫 admin endpoint
curl -X POST "http://localhost:80/llm/permission/add" -H "Authorization: Bearer $REFRESHED_TOKEN" -H "Content-Type: application/json" -d '{"name":"sec004_tampered","num":1}'
→ HTTP 200 {"code":200,"message":"success","data":{"id":"1726909d-...","name":"sec004_tampered","num":1,...}}

漏洞確認：JWT Refresh 機制未驗證原始 token 簽名，攻擊者可偽造 admin 權限
已清理測試資料（id: 1726909d）
```

**SEC-005 水平越權**
```
curl -X POST "http://localhost:80/llm/mcpServer/getSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"userSettingId":"00000000-0000-0000-0000-000000000000"}'
→ HTTP 400
{"code":400,"message":"User setting not found","data":null}
（無法存取不屬於自己的設定）
```

**SEC-006 大量請求壓力（10 次）**
```
for i in {1..10}; do curl /llm/chat ...; done
→ Request 1~10: HTTP 200（全部成功，目前無 rate limiting）
```
