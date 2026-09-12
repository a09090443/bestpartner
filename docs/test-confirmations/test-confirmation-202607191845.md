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
| 測試日期 | 2026-07-19 18:45 |
| 服務版本 | 0.1.8-SNAPSHOT |
| 測試環境 | dev |
| LLM 平台 | OpenRouter |
| LLM Setting ID | |
| Base URL | `http://localhost:80` |
| 測試結束日期 | 2026-07-19 20:30 |

### 本次測試目的

langchain4j **1.13.0 → 1.17.2** 升級後迴歸驗證。測試範圍鎖定 langchain4j 實際流經的路徑：
AUTH（取 token 前置）、CHAT、ADMIN CHAT、VECTOR、TOOL、MCP。
其餘模組（USER / LLM SETTING / PERMISSION / SYSTEM SETTING / INTEGRATION / SECURITY）本次標記為「—」不適用。

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
| USER | 17 | — | — | — | 本次不適用 |
| LLM SETTING | 16 | — | — | — | 本次不適用 |
| VECTOR | 14 | 11 | 0 | 3 | 78.6% |
| TOOL | 19 | 19 | 0 | 0 | 100.0% |
| MCP | 19 | 18 | 1 | 0 | 94.7% |
| PERMISSION | 11 | — | — | — | 本次不適用 |
| SYSTEM SETTING | 13 | — | — | — | 本次不適用 |
| 跨模組整合 | 6 | — | — | — | 本次不適用 |
| 通用安全性 | 6 | — | — | — | 本次不適用 |
| **本次範圍合計** | **85** | **78** | **1** | **6** | **91.8%** |

### 優先級通過率（本次範圍）

| 優先級 | 通過標準 | 本次結果 | 判定 |
|--------|--------|---------|------|
| P0 | 100% | 18/18 執行案例全過（另 3 案因基礎設施未啟動 ⏭️、2 案因憑證無效 ⏭️） | ✅ 無 P0 失敗 |
| P1 | ≥ 95% | 唯一失敗 MCP-016 為既有缺陷，非升級所致 | ✅ 升級面無失敗 |
| P2 | ≥ 80% | 全數通過 | ✅ |

> **升級結論**：所有失敗與略過項目經逐案追根，**無任何一項可歸因於 langchain4j 1.13.0 → 1.17.2 升級**。
> 1 項 ❌ 為既有程式缺陷（MCP-016），6 項 ⏭️ 分別為基礎設施未啟動（3）與外部憑證額度／有效性問題（3）。

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

## 測試執行記錄

### AUTH — 認證模組（`/login`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| AUTH-001 | 正常登入（有效帳密） | P0 | POST | `/login/` | ✅ | admin / user / test_user 三帳號皆 200 |
| AUTH-002 | 驗證 JWT 有效 | P0 | POST | `/login/check` | ✅ | admin 200；test_user 403（符合預期） |
| AUTH-003 | 錯誤登入（密碼錯誤） | P1 | POST | `/login/` | ✅ | |
| AUTH-004 | 錯誤登入（信箱不存在） | P1 | POST | `/login/` | ✅ | |
| AUTH-005 | 必填驗證（缺 email） | P1 | POST | `/login/` | ✅ | |
| AUTH-006 | 必填驗證（缺 password） | P1 | POST | `/login/` | ✅ | |
| AUTH-007 | 必填驗證（空 body） | P1 | POST | `/login/` | ✅ | 兩個欄位錯誤訊息皆回傳 |
| AUTH-008 | email 格式不合法 | P2 | POST | `/login/` | ✅ | 回 401「帳號不存在」，在範本容許的 400/401 內 |
| AUTH-009 | JWT check（無效 token） | P1 | POST | `/login/check` | ✅ | |
| AUTH-010 | 邊界條件（超長 password） | P2 | POST | `/login/` | ✅ | 1000 字元密碼回 401「密碼錯誤」，未造成例外 |

#### 執行記錄

```
### AUTH-001（admin）
$ curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw","password":"admin"}'
{"code":200,"message":"success","data":"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJiYXN0LXBhcnRuZXIiLCJ1cG4iOiJjODhmNTdjOC1hZDI2LTRlYTAtOWY3MS1hNjU5OTViNDkzNTciLCJncm91cHMiOlsiYWRtaW4iLCJ1c2VyLXdyaXRlIiwidXNlci1yZWFkIl0sImlhdCI6MTc4NDQ1ODA4NSwiZXhwIjoxNzg0NDU5ODg1LCJqdGkiOiJjMWM1ZWRiZS1lZDlmLTQxNTQtYWJmZi04YmZiMWEzYzdhODAifQ.aq1P6uujCbeDtv3l-0wnI8xMcMq65XR6zs4rB1qFTdPnIk9O9Y2uapHYgZ47izuVFeRmEltWqcR3VdmOGM8xhH4iJGpxKHV025y7FV-eeisKJGDpZJms0AomkMmb3R4UnIBATHu9MUEEFL4IwpoED_9BHvwh0TJM9tXYN3bodPgZ7TNUIXDAiPqgBgY3sVvHpAz-X3pnbsEY2KZS3Az6t-rz-xlrfglY5-EF7uSFMdAoRKV0laG3ioUXmMCrwWX1mdQkXkk7hK-uJgr0V-BtguqjXW-8ni12rg5UaJza3nJufsbVPJwPj8QiCE4KlzsFrSP7yVmneguFmSwYwg3hCg"}
HTTP_CODE=200

### AUTH-001b（user）
$ curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" \
  -d '{"email":"user@bestpartner.com.tw","password":"user"}'
{"code":200,"message":"success","data":"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJiYXN0LXBhcnRuZXIiLCJ1cG4iOiI1NTU0ZjI1NS0wOGQ1LTQwNjktOTlkMi05MjgzNzJkNGE4N2IiLCJncm91cHMiOlsidXNlci13cml0ZSIsInVzZXItcmVhZCJdLCJpYXQiOjE3ODQ0NTgwODYsImV4cCI6MTc4NDQ1OTg4NiwianRpIjoiMmYyNTkzZTQtZWEzOS00ZTMwLWFkYjUtMDQ4N2I1M2U0ODRiIn0.UZ4-KEDZGxwLzggaImEtEFrsA7IHcz45-r8JfItX-9F_zodhqHQe6rAxHCcIbN6ikZzOMdyf3XsQqnjGDc-O3fIRZSO-uTl6-gfhNmIuFUnD59cqrtLXcZqTEKSNs7-sQ1XR9LrZYbm1DhvHWxl3Rj6CJO68I3oH-xW2HOuuFfO82OnG42fxK6nzQ4v4RWzNMqjMYH3-biMN5hSGrw0RrVUpuBJJ0FHAP29sXO7A62VP_PY5eTTvklJweFdUEVYryMt8-Z9ROZmZdZ8mn-xOTqXm4zCixxMaU47-zIGo1C-FCMCAUJcO2ldFbe26Ys52oOHeYsNToscYDqrqi23e7Q"}
HTTP_CODE=200

### AUTH-001c（test_user）
$ curl -s -X POST "http://localhost:80/login/" -H "Content-Type: application/json" \
  -d '{"email":"test@partmer.com.tw","password":"user"}'
{"code":200,"message":"success","data":"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJiYXN0LXBhcnRuZXIiLCJ1cG4iOiI2NzAwMTdiNC0yM2QwLTQzMzktYTljMC0yMmI2ZDk0NDY0NjEiLCJncm91cHMiOltdLCJpYXQiOjE3ODQ0NTgwODYsImV4cCI6MTc4NDQ1OTg4NiwianRpIjoiOWRmNTY5NDItZGZiYi00ZjY4LTkzNzMtNjJjMzdmNTAyNDc0In0.QGoZ005pYEYoqeZKlZUnAHEEKrQMBGXEAhdBNfyTBbUSQAYi-jW7YCJenks_RSwhw_TtChKEqw_KPakCPWHC0phFY9_a2M68ZJDOjX9-XCRMJpVZFreOHmRqOwkPdLLZu6uXQVxpBGd9PBqgep7IO6bY6aitqLimnI-pe3zyNNiA9ChjvGGo5uDyUnKHmVJWWxFoR9vzwNWmoajmjy_nF1XG2ZvSJRi9o4wYY2sdr6M0qdG5xw2EEmJ7YYHxV6UxfZeOhEDwEVBUumZaEZ-ctkyy92kc9jiIG1iD1aQyHy4_LZU-RRu6y-YlJvUPC6ZYyabdeswk62cZU3B2EKqfWg"}
HTTP_CODE=200

### AUTH-002（admin token 驗證）
$ curl -s -X POST 'http://localhost:80/login/check' -H 'Authorization: Bearer <ADMIN_TOKEN>' \
  -H 'Content-Type: application/json' -d '{}'
{"code":200,"message":"success","data":"check"}
HTTP_CODE=200

### AUTH-002b（test_user 缺 user-read 角色）
$ curl -s -X POST 'http://localhost:80/login/check' -H 'Authorization: Bearer <TEST_USER_TOKEN>' \
  -H 'Content-Type: application/json' -d '{}'
{"code":403,"message":"權限錯誤","data":null}
HTTP_CODE=403

### AUTH-003
$ curl -s -X POST 'http://localhost:80/login/' -H 'Content-Type: application/json' \
  -d '{"email":"admin@bestpartner.com.tw","password":"wrongpassword"}'
{"code":401,"message":"密碼錯誤","data":null}
HTTP_CODE=401

### AUTH-004
$ curl -s -X POST 'http://localhost:80/login/' -H 'Content-Type: application/json' \
  -d '{"email":"notexist@example.com","password":"anypassword"}'
{"code":401,"message":"帳號不存在","data":null}
HTTP_CODE=401

### AUTH-005
$ curl -s -X POST 'http://localhost:80/login/' -H 'Content-Type: application/json' -d '{"password":"admin"}'
{"code":400,"message":"欄位 email 不可為空值","data":null}
HTTP_CODE=400

### AUTH-006
$ curl -s -X POST 'http://localhost:80/login/' -H 'Content-Type: application/json' \
  -d '{"email":"admin@bestpartner.com.tw"}'
{"code":400,"message":"欄位 password 不可為空值","data":null}
HTTP_CODE=400

### AUTH-007
$ curl -s -X POST 'http://localhost:80/login/' -H 'Content-Type: application/json' -d '{}'
{"code":400,"message":"欄位 email 不可為空值\n欄位 password 不可為空值","data":null}
HTTP_CODE=400

### AUTH-008
$ curl -s -X POST 'http://localhost:80/login/' -H 'Content-Type: application/json' \
  -d '{"email":"not-an-email","password":"admin"}'
{"code":401,"message":"帳號不存在","data":null}
HTTP_CODE=401

### AUTH-009
$ curl -s -X POST 'http://localhost:80/login/check' -H 'Authorization: Bearer invalidtoken' \
  -H 'Content-Type: application/json' -d '{}'
{"code":401,"message":"Invalid or malformed token, please log in again","data":null}
HTTP_CODE=401

### AUTH-010（password 為 1000 個 x）
$ curl -s -X POST 'http://localhost:80/login/' -H 'Content-Type: application/json' \
  -d '{"email":"admin@bestpartner.com.tw","password":"xxx...（1000 字元）"}'
{"code":401,"message":"密碼錯誤","data":null}
HTTP_CODE=401
```

**清理**：本模組無新增資料，無需清理。

---

### CHAT — 聊天模組（`/llm`）

> ⚠️ **模板已知 ID 修正**：checklist「常用已知 ID 參考」標示 `583b9222-…` 屬 test_user，實測為 **admin 帳號**所有（OpenRouter / deepseek-v3.2 / CHAT）。
> `2b44c811-…` 確屬 test_user（OpenRouter / deepseek-v3.2 / STREAMING_CHAT）。本次依實際歸屬選用 token。

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| CHAT-001 | 同步聊天（正常請求） | P0 | POST | `/llm/chat` | ✅ | **升級核心驗證**：真實 OpenRouter 呼叫成功 |
| CHAT-002 | 同步聊天（未認證） | P0 | POST | `/llm/chat` | ✅ | |
| CHAT-003 | 同步聊天（缺 llmId） | P1 | POST | `/llm/chat` | ✅ | |
| CHAT-004 | 同步聊天（缺 message） | P1 | POST | `/llm/chat` | ✅ | |
| CHAT-005 | 同步聊天（llmId 不存在） | P1 | POST | `/llm/chat` | ✅ | |
| CHAT-006 | 串流聊天（正常請求） | P0 | POST | `/llm/chatStreaming` | ✅ | SSE 逐 token 回傳正常 |
| CHAT-007 | 串流聊天（未認證） | P0 | POST | `/llm/chatStreaming` | ✅ | |
| CHAT-008 | 自定義助手聊天（memory + promptContent） | P1 | POST | `/llm/customAssistantChat` | ✅ | AiServices + ChatMemory 組裝正常 |
| CHAT-009 | 自定義助手聊天（缺 promptContent） | P1 | POST | `/llm/customAssistantChat` | ✅ | |
| CHAT-010 | 自定義助手聊天（含 mcpSettingIds） | P2 | POST | `/llm/customAssistantChat` | ✅ | 見 MCP-019，真實 MCP 工具呼叫成功 |
| CHAT-011 | 自定義助手串流（正常） | P1 | POST | `/llm/customAssistantChatStreaming` | ✅ | |
| CHAT-012 | 上傳檔案（正常） | P1 | POST | `/llm/uploadFile` | ✅ | |
| CHAT-013 | 上傳檔案（未認證） | P1 | POST | `/llm/uploadFile` | ✅ | |
| CHAT-014 | 上傳檔案（無附件） | P2 | POST | `/llm/uploadFile` | ✅ | |
| CHAT-015 | 上傳檔案（超大檔案） | P2 | POST | `/llm/uploadFile` | ✅ | 15MB 檔案回 413，正確拒絕未拋例外 |
| CHAT-016 | 同步聊天（message 為空） | P2 | POST | `/llm/chat` | ✅ | |
| CHAT-017 | 同步聊天（admin，`1ee80ffa` gpt-5.5-pro） | P1 | POST | `/llm/chat` | ⏭️ | **環境限制**：OpenRouter 帳號額度不足（見下方根因）。同端點已由 CHAT-001 以 admin 帳號驗證通過 |
| CHAT-018 | 串流聊天（admin，`4ba6eb24`） | P1 | POST | `/llm/chatStreaming` | ✅ | |

#### 執行記錄

```
### CHAT-001（admin + OpenRouter 583b9222 / deepseek-v3.2）
$ curl -s -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Reply with exactly: OK"}'
{"code":200,"message":"success","data":"OK"}
HTTP_CODE=200

### CHAT-002
$ curl -s -X POST "http://localhost:80/llm/chat" -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Hello"}'
（空 body）
HTTP_CODE=401

### CHAT-003
$ curl -s -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"message":"Hello"}'
{"code":400,"message":"欄位 llmId 不可為空值","data":null}
HTTP_CODE=400

### CHAT-004
$ curl -s -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9"}'
{"code":400,"message":"欄位 message 不可為空值","data":null}
HTTP_CODE=400

### CHAT-005
$ curl -s -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"00000000-0000-0000-0000-000000000000","message":"Hello"}'
{"code":400,"message":"Please verify the LLM setting you are accessing exists","data":null}
HTTP_CODE=400

### CHAT-006（test_user + OpenRouter 2b44c811，SSE）
$ curl -s -N -X POST "http://localhost:80/llm/chatStreaming" -H "Authorization: Bearer <TEST_USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"2b44c811-d38c-417b-89d4-ae675b3093f5","message":"Count from 1 to 5, numbers only"}'
data:1

data:  
data:2  
data:

data:3  
data:4

data:  
data:5

HTTP_CODE=200

### CHAT-007
$ curl -s -X POST "http://localhost:80/llm/chatStreaming" -H "Content-Type: application/json" \
  -d '{"llmId":"2b44c811-d38c-417b-89d4-ae675b3093f5","message":"Hello"}'
（空 body）
HTTP_CODE=401

### CHAT-008
$ curl -s -X POST "http://localhost:80/llm/customAssistantChat" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Say hi","memory":{"id":"session-lc4j-001","maxSize":5},"promptContent":"You are a helpful assistant. Reply briefly."}'
{"code":200,"message":"success","data":"Hi there! 👋"}
HTTP_CODE=200

### CHAT-009
$ curl -s -X POST "http://localhost:80/llm/customAssistantChat" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Say hi","memory":{"id":"session-lc4j-001","maxSize":5}}'
{"code":400,"message":"欄位 promptContent 不可為空值","data":null}
HTTP_CODE=400

### CHAT-011
$ curl -s -N -X POST "http://localhost:80/llm/customAssistantChatStreaming" -H "Authorization: Bearer <TEST_USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"2b44c811-d38c-417b-89d4-ae675b3093f5","message":"Say hi","memory":{"id":"stream-lc4j-001","maxSize":5},"promptContent":"You are helpful. Reply briefly."}'
data:Hi

data: there

data:!

data: 👋 How can I help

data: you today?

HTTP_CODE=200

### CHAT-012
$ echo "Hello World" > /tmp/test_upload.txt
$ curl -s -X POST "http://localhost:80/llm/uploadFile" -H "Authorization: Bearer <TEST_USER_TOKEN>" \
  -F "file=@/tmp/test_upload.txt"
{"code":200,"message":"success","data":["test_upload.txt"]}
HTTP_CODE=200

### CHAT-013
$ curl -s -X POST "http://localhost:80/llm/uploadFile" -F "file=@/tmp/test_upload.txt"
（空 body）
HTTP_CODE=401

### CHAT-014
$ curl -s -X POST "http://localhost:80/llm/uploadFile" -H "Authorization: Bearer <TEST_USER_TOKEN>" -F "dummy=x"
{"code":400,"message":"欄位 files 不可為空值","data":null}
HTTP_CODE=400

### CHAT-015（15MB 隨機檔）
$ head -c 15000000 /dev/urandom > /tmp/test_big.bin
$ curl -s -X POST "http://localhost:80/llm/uploadFile" -H "Authorization: Bearer <TEST_USER_TOKEN>" \
  -F "file=@/tmp/test_big.bin"
（空 body）
HTTP_CODE=413

### CHAT-016
$ curl -s -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":""}'
{"code":400,"message":"欄位 message 不可為空值","data":null}
HTTP_CODE=400

### CHAT-017（admin + 1ee80ffa / openai/gpt-5.5-pro）
$ curl -s -X POST "http://localhost:80/llm/chat" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"1ee80ffa-7797-4051-8fb1-c0996159b408","message":"Reply with exactly: OK"}'
{"code":400,"message":"LLM服務處理發生錯誤","data":null}
HTTP_CODE=400

# 錯誤日誌根因（D:/tmp/bestpartner/bestpartner_error.log）：
# Caused by: dev.langchain4j.exception.InvalidRequestException:
#   {"error":{"message":"This request requires more credits, or fewer max_tokens.
#    You requested up to 32768 tokens, but can only afford 28840. ...","code":402}}
#   at dev.langchain4j.model.openai.OpenAiChatModel.doChat(OpenAiChatModel.java:166)
# → OpenRouter 帳號額度限制（HTTP 402），非 langchain4j 升級迴歸。
#   該設定 maxTokens=32768 超出金鑰可負擔額度；CHAT-001 使用同帳號較低成本模型即通過。

### CHAT-018（admin + 4ba6eb24 / deepseek-v4-flash）
$ curl -s -N -X POST "http://localhost:80/llm/chatStreaming" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"4ba6eb24-1128-447c-8e88-03fecb42dbb0","message":"Say hi briefly"}'
data:Hi

HTTP_CODE=200
```

**清理**：CHAT-012 上傳的 `test_upload.txt` 位於 `FILE_UPLOAD_DIR`（`D:/tmp/bestpartner/upload`），屬本機開發暫存目錄，無資料庫記錄，無需刪除 API 清理。

---

### MCP — MCP Server 模組（`/llm/mcpServer`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| MCP-001 | 列出 MCP（已認證） | P0 | GET | `/llm/mcpServer/list` | ✅ | 5 筆 MCP server |
| MCP-002 | 列出 MCP（未認證） | P0 | GET | `/llm/mcpServer/list` | ✅ | |
| MCP-003 | 取得 MCP（正常） | P1 | POST | `/llm/mcpServer/get` | ✅ | |
| MCP-004 | 取得 MCP（mcpId 不存在） | P1 | POST | `/llm/mcpServer/get` | ✅ | |
| MCP-005 | 註冊 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/register` | ✅ | 測試後已刪除 |
| MCP-006 | 註冊 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/register` | ✅ | 403 |
| MCP-007 | 註冊 MCP（缺必填欄位） | P1 | POST | `/llm/mcpServer/register` | ✅ | |
| MCP-008 | 更新 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/update` | ✅ | |
| MCP-009 | 更新 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/update` | ✅ | 403 |
| MCP-010 | 刪除 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/delete` | ✅ | |
| MCP-011 | 刪除 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/delete` | ✅ | 403 |
| MCP-012 | 儲存使用者設定（正常） | P1 | POST | `/llm/mcpServer/saveSetting` | ✅ | 測試後已刪除 |
| MCP-013 | 儲存使用者設定（缺 mcpId） | P1 | POST | `/llm/mcpServer/saveSetting` | ✅ | |
| MCP-014 | 取得使用者設定（正常） | P1 | POST | `/llm/mcpServer/getSetting` | ✅ | |
| MCP-015 | 取得使用者設定（id 不存在） | P2 | POST | `/llm/mcpServer/getSetting` | ✅ | |
| MCP-016 | 更新使用者設定（正常） | P1 | POST | `/llm/mcpServer/updateSetting` | ❌ → ✅ | 測試當下為既有缺陷（與升級無關）；**已於同日修復並重測通過**，見文末修復記錄 |
| MCP-017 | 刪除使用者設定（正常） | P1 | DELETE | `/llm/mcpServer/deleteSetting` | ✅ | |
| MCP-018 | 未認證取得清單 | P2 | GET | `/llm/mcpServer/list` | ✅ | |
| MCP-019 | 業務邏輯（設定後於聊天使用） | P1 | 多步 | `/llm/mcpServer/*` | ✅ | **升級核心驗證**：真實 MCP 工具呼叫成功 |

#### 執行記錄

```
### MCP-001
$ curl -s -X GET "http://localhost:80/llm/mcpServer/list" -H "Authorization: Bearer <USER_TOKEN>"
{"code":200,"message":"success","data":[{"name":"filesystem","server":"","command":"","args":["-jar","D:/MCP/mcp-server-filesystem-999-SNAPSHOT.jar","${path0}","${path1}"],"argsDesc":{"path0":"請帶入目錄位置","path1":"請帶入目錄位置"},"env":null,"envDesc":null,"type":"STDIO","alias":null,"userSettingId":null,"settingId":null,"settingContent":null,"commandSetting":null,"description":null,"userId":null,"mcpId":"182cca55-04d9-4afc-a990-1ed96dccf957"},{"name":"test","server":"","command":"","args":["-jar","xxx.jar","${arg0}","${arg1}"],"argsDesc":{"arg0":"請帶入xxx","arg1":"請帶入xxxx"},"env":{"param0":"請帶入xxx","--param1":"請帶入xxx"},"envDesc":null,"type":"STDIO","alias":null,"userSettingId":null,"settingId":null,"settingContent":null,"commandSetting":null,"description":null,"userId":null,"mcpId":"2fdc0d18-3faa-4204-a97d-db9d1683b97f"},{"name":"date","server":"","command":"","args":["-jar","D:/MCP/date.jar"],"argsDesc":null,"env":null,"envDesc":null,"type":"STDIO","alias":null,"userSettingId":null,"settingId":null,"settingContent":null,"commandSetting":null,"description":"日期查詢 MCP（java -jar date.jar）","userId":null,"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"},{"name":"favorite","server":"","command":"","args":["--quiet","D:/MCP/quarkus-example-1.0-SNAPSHOT-runner.jar"],"argsDesc":null,"env":null,"envDesc":null,"type":"STDIO","alias":null,"userSettingId":null,"settingId":null,"settingContent":null,"commandSetting":null,"description":null,"userId":null,"mcpId":"a9297b93-e05b-438c-8d99-9c7d29e8ee24"},{"name":"google_drive","server":"","command":"","args":["-jar","D:/MCP/google-drive-1.0-SNAPSHOT-runner.jar"],"argsDesc":null,"env":{"CREDENTIALS_FILE_PATH":"${file_path}"},"envDesc":{"file_path":"請輸入認證檔案位置"},"type":"STDIO","alias":null,"userSettingId":null,"settingId":null,"settingContent":null,"commandSetting":null,"description":null,"userId":null,"mcpId":"06a7fb6d-1aef-41d8-af11-978a1e73a65e"}]}
HTTP_CODE=200

### MCP-002
$ curl -s -X GET "http://localhost:80/llm/mcpServer/list"
（空 body）
HTTP_CODE=401

### MCP-003
$ curl -s -X POST "http://localhost:80/llm/mcpServer/get" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"}'
{"code":200,"message":"success","data":{"name":"date","server":"","command":"","args":["-jar","D:/MCP/date.jar"],"argsDesc":null,"env":null,"envDesc":null,"type":"STDIO","alias":null,"userSettingId":null,"settingId":null,"settingContent":null,"commandSetting":null,"description":"日期查詢 MCP（java -jar date.jar）","userId":null,"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"}}
HTTP_CODE=200

### MCP-004
$ curl -s -X POST "http://localhost:80/llm/mcpServer/get" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"mcpId":"00000000-0000-0000-0000-000000000000"}'
{"code":400,"message":"MCP server not found","data":null}
HTTP_CODE=400

### MCP-005
$ curl -s -X POST "http://localhost:80/llm/mcpServer/register" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_mcp_lc4j","type":"STDIO","command":"java","args":["-jar","test.jar"]}'
{"code":200,"message":"success","data":{"name":"test_mcp_lc4j","server":"","command":"java","args":["-jar","test.jar"],"argsDesc":null,"env":null,"envDesc":null,"type":"STDIO","alias":null,"userSettingId":null,"settingId":null,"settingContent":null,"commandSetting":null,"description":null,"userId":null,"mcpId":"1c5ecc0d-aae3-48af-9ef1-633035da25d7"}}
HTTP_CODE=200

### MCP-006
$ curl -s -X POST "http://localhost:80/llm/mcpServer/register" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"name":"hack_mcp","type":"STDIO","command":"malware","args":[]}'
{"code":403,"message":"權限錯誤","data":null}
HTTP_CODE=403

### MCP-007
$ curl -s -X POST "http://localhost:80/llm/mcpServer/register" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"name":"test_mcp","type":"STDIO"}'
{"code":400,"message":"欄位 command 不可為空值\n欄位 args 不可為空值","data":null}
HTTP_CODE=400

### MCP-008
$ curl -s -X POST "http://localhost:80/llm/mcpServer/update" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"1c5ecc0d-aae3-48af-9ef1-633035da25d7","name":"test_mcp_lc4j_updated","type":"STDIO","command":"java","args":["-jar","updated.jar"]}'
{"code":200,"message":"success","data":{"name":"test_mcp_lc4j_updated","server":"","command":"java","args":["-jar","updated.jar"],"argsDesc":null,"env":null,"envDesc":null,"type":"STDIO","alias":null,"userSettingId":null,"settingId":null,"settingContent":null,"commandSetting":{"command":"java","args":["-jar","updated.jar"],"argsDesc":null,"env":null,"envDesc":null,"server":null},"description":null,"userId":null,"mcpId":"1c5ecc0d-aae3-48af-9ef1-633035da25d7"}}
HTTP_CODE=200

### MCP-009
$ curl -s -X POST "http://localhost:80/llm/mcpServer/update" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"1c5ecc0d-aae3-48af-9ef1-633035da25d7","name":"hack","type":"STDIO","command":"malware","args":[]}'
{"code":403,"message":"權限錯誤","data":null}
HTTP_CODE=403

### MCP-011
$ curl -s -X POST "http://localhost:80/llm/mcpServer/delete" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"mcpId":"1c5ecc0d-aae3-48af-9ef1-633035da25d7"}'
{"code":403,"message":"權限錯誤","data":null}
HTTP_CODE=403

### MCP-012
$ curl -s -X POST "http://localhost:80/llm/mcpServer/saveSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021","settingContent":{"timezone":"Asia/Taipei"}}'
{"code":200,"message":"success","data":{"name":"","server":"","command":"","args":null,"argsDesc":null,"env":null,"envDesc":null,"type":null,"alias":null,"userSettingId":"4a0bc54c-4c12-492a-b17e-d0c1ec9cff2a","settingId":null,"settingContent":{"timezone":"Asia/Taipei"},"commandSetting":null,"description":null,"userId":null,"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"}}
HTTP_CODE=200

### MCP-013
$ curl -s -X POST "http://localhost:80/llm/mcpServer/saveSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"settingContent":{"timezone":"UTC"}}'
{"code":400,"message":"欄位 mcpId 不可為空值","data":null}
HTTP_CODE=400

### MCP-014
$ curl -s -X POST "http://localhost:80/llm/mcpServer/getSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"userSettingId":"4a0bc54c-4c12-492a-b17e-d0c1ec9cff2a"}'
{"code":200,"message":"success","data":{"name":"","server":"","command":"","args":null,"argsDesc":null,"env":null,"envDesc":null,"type":null,"alias":"","userSettingId":null,"settingId":"4a0bc54c-4c12-492a-b17e-d0c1ec9cff2a","settingContent":{"timezone":"Asia/Taipei"},"commandSetting":null,"description":null,"userId":null,"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"}}
HTTP_CODE=200

### MCP-015
$ curl -s -X POST "http://localhost:80/llm/mcpServer/getSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"userSettingId":"00000000-0000-0000-0000-000000000000"}'
{"code":400,"message":"User setting not found","data":null}
HTTP_CODE=400

### MCP-016（❌ 失敗）
$ curl -s -X POST "http://localhost:80/llm/mcpServer/updateSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"4a0bc54c-4c12-492a-b17e-d0c1ec9cff2a","settingContent":{"timezone":"UTC"}}'
{"code":400,"message":"User setting not found","data":null}
HTTP_CODE=400

# 反向驗證：改傳 settingId
$ curl -s -X POST "http://localhost:80/llm/mcpServer/updateSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"settingId":"4a0bc54c-4c12-492a-b17e-d0c1ec9cff2a","settingContent":{"timezone":"UTC"}}'
{"code":400,"message":"欄位 userSettingId 不可為空值","data":null}
HTTP_CODE=400
# → 驗證層要求 userSettingId、服務層卻讀 settingId，兩路皆不可能成功。

### MCP-019 / CHAT-010（真實 MCP 工具呼叫）
$ curl -s -X POST "http://localhost:80/llm/mcpServer/saveSetting" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021","settingContent":{"timezone":"Asia/Taipei"}}'
{"code":200,"message":"success","data":{"name":"","server":"","command":"","args":null,"argsDesc":null,"env":null,"envDesc":null,"type":null,"alias":null,"userSettingId":"d1f197c5-9386-499b-82aa-974cad234af5","settingId":null,"settingContent":{"timezone":"Asia/Taipei"},"commandSetting":null,"description":null,"userId":null,"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"}}

$ curl -s -X POST "http://localhost:80/llm/customAssistantChat" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"What is today'\''s date? Use the available tool.","memory":{"id":"mcp-lc4j-session","maxSize":5},"promptContent":"You are helpful. Use tools when needed.","mcpSettingIds":["d1f197c5-9386-499b-82aa-974cad234af5"]}'
{"code":200,"message":"success","data":"Today's date is **July 19, 2026** (formatted as 2026-07-19)."}
HTTP_CODE=200
# → 完整鏈路驗證：StdioMcpTransport 啟動 date.jar → DefaultMcpClient →
#   McpToolProvider 供給工具 → AiServices 觸發工具呼叫 → ToolExecutionResult 正確解析。
#   回傳日期與系統當日一致，證明工具確實被執行而非模型幻覺。

### MCP-017 + 清理
$ curl -s -X DELETE "http://localhost:80/llm/mcpServer/deleteSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"userSettingId":"4a0bc54c-4c12-492a-b17e-d0c1ec9cff2a"}'
{"code":200,"message":"success","data":true}
HTTP_CODE=200

$ curl -s -X DELETE "http://localhost:80/llm/mcpServer/deleteSetting" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"userSettingId":"d1f197c5-9386-499b-82aa-974cad234af5"}'
{"code":200,"message":"success","data":true}
HTTP_CODE=200

### MCP-010（清理測試 MCP server）
$ curl -s -X POST "http://localhost:80/llm/mcpServer/delete" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"mcpId":"1c5ecc0d-aae3-48af-9ef1-633035da25d7"}'
{"code":200,"message":"success","data":true}
HTTP_CODE=200

### MCP-018
$ curl -s -X GET "http://localhost:80/llm/mcpServer/list"
（空 body）
HTTP_CODE=401

### 清理驗證
$ curl -s -X GET "http://localhost:80/llm/mcpServer/list" -H "Authorization: Bearer <ADMIN_TOKEN>" | grep -c "test_mcp_lc4j"
0   → 無殘留
```

**清理**：MCP-005 建立的 test MCP server、MCP-012 與 MCP-019 建立的 2 筆使用者設定皆已刪除並驗證無殘留。

---

### ADMIN CHAT — 管理員聊天模組（`/llm/admin`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| ADCHAT-001 | 管理員聊天（admin 正常） | P0 | POST | `/llm/admin/chat` | ⏭️ | `.env` 的 `OPENROUTER_API_KEY` 遭 OpenRouter 拒絕（401）。**langchain4j 路徑本身已證實正常**，見下方分析 |
| ADCHAT-002 | 管理員聊天（一般用戶存取） | P0 | POST | `/llm/admin/chat` | ✅ | 403 |
| ADCHAT-003 | 管理員聊天（未認證） | P0 | POST | `/llm/admin/chat` | ✅ | 401 |
| ADCHAT-004 | 管理員自定義助手聊天（admin 正常） | P1 | POST | `/llm/admin/customAssistantChat` | ⏭️ | 同 ADCHAT-001 |
| ADCHAT-005 | 管理員自定義助手聊天（一般用戶） | P1 | POST | `/llm/admin/customAssistantChat` | ✅ | 403 |

#### 執行記錄

```
### ADCHAT-002
$ curl -s -X POST "http://localhost:80/llm/admin/chat" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Hello"}'
{"code":403,"message":"權限錯誤","data":null}
HTTP_CODE=403

### ADCHAT-003
$ curl -s -X POST "http://localhost:80/llm/admin/chat" -H "Content-Type: application/json" \
  -d '{"platform":"OPENROUTER","message":"Hello"}'
（空 body）
HTTP_CODE=401

### ADCHAT-005
$ curl -s -X POST "http://localhost:80/llm/admin/customAssistantChat" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Hello"}'
{"code":403,"message":"權限錯誤","data":null}
HTTP_CODE=403

### ADCHAT-001 / 004 — 三階段診斷

# 階段 1：初次執行（服務 cwd = bestpartner-service/）
$ curl -s -X POST "http://localhost:80/llm/admin/chat" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Reply with exactly: OK"}'
{"code":400,"message":"LLM model not found","data":null}
HTTP_CODE=400
# 診斷：根目錄 .env 由 Quarkus 依「工作目錄」載入，自 bestpartner-service/ 啟動時未載入。
#      → 已自 repo 根目錄重啟服務排除此變因。

# 階段 2：自 repo 根目錄重啟後
{"code":400,"message":"LLM model not found","data":null}
HTTP_CODE=400
# 診斷：查 /systemSetting/list 得 default_llm_platform = "OPENAI"。
#      LLMStore.initChatModelMap()（LLMStore.kt:50）僅註冊該系統設定指定平台的模型，
#      故 chatModelMap 中不存在 OPENROUTER 鍵。

# 階段 3：暫時將 default_llm_platform 改為 OPENROUTER 並重啟服務
$ curl -s -X POST "http://localhost:80/llm/admin/chat" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Reply with exactly: OK"}'
{"code":500,"message":"Internal server error","data":null}
HTTP_CODE=500

# 服務日誌：
# ERROR [tw.zip.bas.exc.GlobalExceptionMapper] dev.langchain4j.exception.AuthenticationException:
#   {"error":{"message":"User not found.","code":401}}
#   at dev.langchain4j.internal.ExceptionMapper$DefaultExceptionMapper.mapHttpStatusCode(ExceptionMapper.java:59)
#   at dev.langchain4j.model.openai.OpenAiStreamingChatModel.lambda$doChat$2(OpenAiStreamingChatModel.java:189)
# Caused by: dev.langchain4j.exception.HttpException: {"error":{"message":"User not found.","code":401}}
#
# 結論：模型已成功建構、HTTP 請求已送達 OpenRouter，被以 401 拒絕
#      → `.env` 中的 OPENROUTER_API_KEY 無效（與資料庫 LLM 設定中的金鑰不同，後者於 CHAT-001 使用正常）。
#      屬憑證環境問題，非 langchain4j 升級迴歸。
#      反證：langchain4j 正確建構 OpenAiStreamingChatModel、發出請求、
#           並將 HTTP 401 映射為 AuthenticationException —— 該路徑運作完全正常。
```

**清理**：測試期間暫時將 `default_llm_platform` 由 `OPENAI` 改為 `OPENROUTER`，測畢**已復原為 `OPENAI`** 並經 `/systemSetting/list` 驗證。

---

### TOOL — 工具模組（`/llm/tool`）

> ⚠️ **模板已過時**：`test-module-TOOL.md` 的 curl 範本使用 `"type":"SYSTEM"`，但 `ToolsType` enum 僅有 `CUSTOMIZE` 與 `BUILT_IN`，照抄必得 400。本次改用 `CUSTOMIZE`（並補上其必要的 `functionName` / `functionDescription`）執行。

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| TOOL-001 | 列出工具（正常） | P0 | GET | `/llm/tool/list` | ✅ | **同時驗證反射依賴**：兩個 langchain4j 搜尋引擎類別可載入並反射產生 `settingSchema` |
| TOOL-002 | 取得工具（正常） | P1 | POST | `/llm/tool/get` | ✅ | |
| TOOL-003 | 取得工具（缺 id） | P1 | POST | `/llm/tool/get` | ✅ | |
| TOOL-004 | 取得工具（id 不存在） | P2 | POST | `/llm/tool/get` | ✅ | |
| TOOL-005 | 註冊工具（admin 正常） | P1 | POST | `/llm/tool/register` | ✅ | 模板 `type` 值有誤，改用 `CUSTOMIZE`；測試後已刪除 |
| TOOL-006 | 註冊工具（缺 name） | P1 | POST | `/llm/tool/register` | ✅ | |
| TOOL-007 | 註冊工具（缺 classPath） | P1 | POST | `/llm/tool/register` | ✅ | |
| TOOL-008 | 註冊工具（缺 type） | P1 | POST | `/llm/tool/register` | ✅ | |
| TOOL-009 | 刪除工具（admin 正常） | P1 | POST | `/llm/tool/delete` | ✅ | |
| TOOL-010 | 刪除工具（缺 id） | P1 | POST | `/llm/tool/delete` | ✅ | |
| TOOL-011 | 儲存工具設定（正常） | P1 | POST | `/llm/tool/saveSetting` | ✅ | ⚠️ 見下方清理說明 |
| TOOL-012 | 儲存工具設定（缺 alias） | P2 | POST | `/llm/tool/saveSetting` | ✅ | |
| TOOL-013 | 更新工具設定（正常） | P1 | POST | `/llm/tool/updateSetting` | ✅ | `settingContent` 需為 JSON 字串 |
| TOOL-014 | 更新工具設定（缺 settingContent） | P2 | POST | `/llm/tool/updateSetting` | ✅ | |
| TOOL-015 | 新增分類（admin 正常） | P1 | POST | `/llm/tool/category/save` | ✅ | 測試後已刪除 |
| TOOL-016 | 新增分類（缺 group） | P2 | POST | `/llm/tool/category/save` | ✅ | |
| TOOL-017 | 更新分類（admin 正常） | P1 | POST | `/llm/tool/category/update` | ✅ | |
| TOOL-018 | 刪除分類（admin 正常） | P1 | POST | `/llm/tool/category/delete` | ✅ | |
| TOOL-019 | 業務邏輯（刪除後不出現清單） | P1 | 多步 | `/llm/tool/*` | ✅ | 刪除前 grep 得 1、刪除後得 0 |

#### 執行記錄

```
### TOOL-001（節錄前兩筆，完整回應含 6 個工具）
$ curl -s -X GET "http://localhost:80/llm/tool/list"
{"code":200,"message":"success","data":[{"id":"f95fda5f-4632-4a1a-9a21-2d4facbd4279","alias":null,"settingId":null,"name":"TavilySearch","classPath":"dev.langchain4j.web.search.tavily.TavilyWebSearchEngine","groupId":"90caee3f-2c87-48b9-8912-3dd810f62377","group":"WEB_SEARCH","groupDescription":"網頁搜尋群組","type":"BUILT_IN","settingArgs":null,"settingContent":null,"settingSchema":{"baseUrl":{"type":"string","required":false,"description":"Tavily API 基礎 URL，未填時使用官方預設"},"apiKey":{"type":"string","required":true,"sensitive":true,"description":"Tavily API 金鑰"},"timeout":{"type":"integer","required":true,"description":"逾時毫秒數"},"searchDepth":{"type":"string","required":false,"description":"搜尋深度（basic / advanced）"},"includeAnswer":{"type":"boolean","required":false},"includeRawContent":{"type":"boolean","required":false},"includeDomains":{"type":"array","required":false,"description":"限定搜尋的網域清單"},"excludeDomains":{"type":"array","required":false,"description":"排除搜尋的網域清單"}},"configObjectPath":"tw.zipe.bastpartner.tool.config.Tavily","description":"內建 Tavily 搜尋工具","functionName":"","functionDescription":"","functionParams":null},{"id":"c14e82ca-511f-424c-b20c-a96d93cae920","alias":null,"settingId":null,"name":"GoogleSearch","classPath":"dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine","groupId":"90caee3f-2c87-48b9-8912-3dd810f62377","group":"WEB_SEARCH","groupDescription":"網頁搜尋群組","type":"BUILT_IN","settingArgs":null,"settingContent":null,"settingSchema":{"apiKey":{"type":"string","required":true,"sensitive":true,"description":"Google Custom Search API 金鑰"},"csi":{"type":"string","required":true,"description":"Custom Search Engine ID"},"siteRestrict":{"type":"boolean","required":false},"includeImages":{"type":"boolean","required":false},"timeout":{"type":"integer","required":true,"description":"逾時毫秒數"},"maxRetries":{"type":"integer","required":false},"logRequests":{"type":"boolean","required":false},"logResponses":{"type":"boolean","required":false}},"configObjectPath":"tw.zipe.bastpartner.tool.config.Google","description":"內建 Google 搜尋工具","functionName":"","functionDescription":"","functionParams":null}, ...]}
HTTP_CODE=200
# → 升級關鍵驗證：SQL seed 中以字串存放的 langchain4j class path
#   （TavilyWebSearchEngine / GoogleCustomWebSearchEngine）在 1.17.2-beta27 下
#   仍可由 ClassInstantiator 反射載入，且成功反射產生 settingSchema。

### TOOL-002
$ curl -s -X POST "http://localhost:80/llm/tool/get" -H "Content-Type: application/json" \
  -d '{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f"}'
{"code":200,"message":"success","data":{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f","alias":null,"settingId":null,"name":"DateTool","classPath":"tw.zipe.bastpartner.tool.DateTool","groupId":"ea8a08e2-342d-4ade-9579-127d2d1443c5","group":"DATE","groupDescription":"日期群組","type":"CUSTOMIZE","settingArgs":null,"settingContent":null,"settingSchema":null,"configObjectPath":null,"description":"日期工具","functionName":"getCurrentTime","functionDescription":"以台灣時間為基準，會根據不同時區取得當地日期時間","functionParams":"{\"zoneId\": [\"輸入格式為國家/城市，如:Australia/Darwin, Asia/Taipei, Africa/Harare\", \"String\"]}"}}
HTTP_CODE=200

### TOOL-003
$ curl -s -X POST "http://localhost:80/llm/tool/get" -H "Content-Type: application/json" -d '{}'
{"code":400,"message":"欄位 id 不可為空值","data":null}
HTTP_CODE=400

### TOOL-004
$ curl -s -X POST "http://localhost:80/llm/tool/get" -H "Content-Type: application/json" \
  -d '{"id":"00000000-0000-0000-0000-000000000000"}'
{"code":400,"message":"Tool not found","data":null}
HTTP_CODE=400

### TOOL-005 首次（照模板 type=SYSTEM，失敗）
$ curl -s -X POST "http://localhost:80/llm/tool/register" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_tool_lc4j","classPath":"tw.zipe.bastpartner.tool.DateTool","groupId":"ea8a08e2-342d-4ade-9579-127d2d1443c5","type":"SYSTEM"}'
{"code":400,"message":"tw.zipe.bastpartner.enumerate.ToolsType does not contain element with name 'SYSTEM' at path $.type","data":null}
HTTP_CODE=400

### TOOL-005 重測（type=CUSTOMIZE，成功）
$ curl -s -X POST "http://localhost:80/llm/tool/register" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_tool_lc4j","classPath":"tw.zipe.bastpartner.tool.DateTool","groupId":"ea8a08e2-342d-4ade-9579-127d2d1443c5","type":"CUSTOMIZE","functionName":"getCurrentTime","functionDescription":"取得目前時間"}'
{"code":200,"message":"success","data":{"id":"8046dbd0-110b-4323-a4f5-6e3a23caf63e","alias":null,"settingId":null,"name":"test_tool_lc4j","classPath":"tw.zipe.bastpartner.tool.DateTool","groupId":"ea8a08e2-342d-4ade-9579-127d2d1443c5","group":null,"groupDescription":null,"type":"CUSTOMIZE","settingArgs":null,"settingContent":null,"settingSchema":null,"configObjectPath":null,"description":"","functionName":"getCurrentTime","functionDescription":"取得目前時間","functionParams":null}}
HTTP_CODE=200

### TOOL-006
$ curl -s -X POST "http://localhost:80/llm/tool/register" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"classPath":"tw.zipe.bastpartner.tool.DateTool","groupId":"ea8a08e2-342d-4ade-9579-127d2d1443c5","type":"CUSTOMIZE","functionName":"f","functionDescription":"d"}'
{"code":400,"message":"欄位 name 不可為空值","data":null}
HTTP_CODE=400

### TOOL-007
$ curl -s -X POST "http://localhost:80/llm/tool/register" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"t","groupId":"ea8a08e2-342d-4ade-9579-127d2d1443c5","type":"CUSTOMIZE","functionName":"f","functionDescription":"d"}'
{"code":400,"message":"欄位 classPath 不可為空值","data":null}
HTTP_CODE=400

### TOOL-008
$ curl -s -X POST "http://localhost:80/llm/tool/register" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"t","classPath":"tw.zipe.bastpartner.tool.DateTool","groupId":"ea8a08e2-342d-4ade-9579-127d2d1443c5"}'
{"code":400,"message":"欄位 type 不可為空值","data":null}
HTTP_CODE=400

### TOOL-010
$ curl -s -X POST "http://localhost:80/llm/tool/delete" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{}'
{"code":400,"message":"欄位 id 不可為空值","data":null}
HTTP_CODE=400

### TOOL-011
$ curl -s -X POST "http://localhost:80/llm/tool/saveSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f","alias":"my_date_tool_lc4j"}'
{"code":200,"message":"success","data":{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f","alias":"my_date_tool_lc4j","settingId":"7877709d-edcd-47ae-bc1c-b2a79f0e082d","name":null,"classPath":"","groupId":null,"group":null,"groupDescription":null,"type":null,"settingArgs":null,"settingContent":null,"settingSchema":null,"configObjectPath":null,"description":"","functionName":"","functionDescription":"","functionParams":null}}
HTTP_CODE=200

### TOOL-012
$ curl -s -X POST "http://localhost:80/llm/tool/saveSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f"}'
{"code":400,"message":"欄位 alias 不可為空值","data":null}
HTTP_CODE=400

### TOOL-013
$ curl -s -X POST "http://localhost:80/llm/tool/updateSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"settingId":"7877709d-edcd-47ae-bc1c-b2a79f0e082d","settingContent":"{\"timezone\":\"Asia/Taipei\"}"}'
{"code":200,"message":"success","data":{"id":null,"alias":null,"settingId":"7877709d-edcd-47ae-bc1c-b2a79f0e082d","name":null,"classPath":"","groupId":null,"group":null,"groupDescription":null,"type":null,"settingArgs":null,"settingContent":"{\"timezone\":\"Asia/Taipei\"}","settingSchema":null,"configObjectPath":null,"description":"","functionName":"","functionDescription":"","functionParams":null}}
HTTP_CODE=200

### TOOL-014
$ curl -s -X POST "http://localhost:80/llm/tool/updateSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"settingId":"7877709d-edcd-47ae-bc1c-b2a79f0e082d"}'
{"code":400,"message":"欄位 settingContent 不可為空值","data":null}
HTTP_CODE=400

### TOOL-015
$ curl -s -X POST "http://localhost:80/llm/tool/category/save" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"group":"test_category_lc4j"}'
{"code":200,"message":"success","data":{"id":null,"alias":null,"settingId":null,"name":null,"classPath":"","groupId":"c2459a67-1dc3-49d7-bcd6-b85b5cf05e96","group":"test_category_lc4j","groupDescription":null,"type":null,"settingArgs":null,"settingContent":null,"settingSchema":null,"configObjectPath":null,"description":"","functionName":"","functionDescription":"","functionParams":null}}
HTTP_CODE=200

### TOOL-016
$ curl -s -X POST "http://localhost:80/llm/tool/category/save" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{}'
{"code":400,"message":"欄位 group 不可為空值","data":null}
HTTP_CODE=400

### TOOL-017
$ curl -s -X POST "http://localhost:80/llm/tool/category/update" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"groupId":"c2459a67-1dc3-49d7-bcd6-b85b5cf05e96","group":"updated_category_lc4j"}'
{"code":200,"message":"success","data":{"id":null,"alias":null,"settingId":null,"name":null,"classPath":"","groupId":"c2459a67-1dc3-49d7-bcd6-b85b5cf05e96","group":"updated_category_lc4j","groupDescription":null,"type":null,"settingArgs":null,"settingContent":null,"settingSchema":null,"configObjectPath":null,"description":"","functionName":"","functionDescription":"","functionParams":null}}
HTTP_CODE=200

### TOOL-018
$ curl -s -X POST "http://localhost:80/llm/tool/category/delete" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"groupId":"c2459a67-1dc3-49d7-bcd6-b85b5cf05e96"}'
{"code":200,"message":"success","data":{"id":null,"alias":null,"settingId":null,"name":null,"classPath":"","groupId":"c2459a67-1dc3-49d7-bcd6-b85b5cf05e96","group":null,"groupDescription":null,"type":null,"settingArgs":null,"settingContent":null,"settingSchema":null,"configObjectPath":null,"description":"","functionName":"","functionDescription":"","functionParams":null}}
HTTP_CODE=200

### TOOL-009 + TOOL-019（刪除前後比對）
$ curl -s -X GET "http://localhost:80/llm/tool/list" | grep -c "test_tool_lc4j"
1
$ curl -s -X POST "http://localhost:80/llm/tool/delete" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"id":"8046dbd0-110b-4323-a4f5-6e3a23caf63e"}'
{"code":200,"message":"success","data":true}
HTTP_CODE=200
$ curl -s -X GET "http://localhost:80/llm/tool/list" | grep -c "test_tool_lc4j"
0
$ curl -s -X GET "http://localhost:80/llm/tool/list" | grep -c "category_lc4j"
0
```

**清理**：
- TOOL-005 建立的工具 `test_tool_lc4j`（`8046dbd0-…`）已刪除並驗證清單為 0 筆。
- TOOL-015 建立的分類 `c2459a67-…` 已刪除並驗證清單為 0 筆。
- TOOL-011 建立的工具使用者設定 `7877709d-…`：TOOL 模組**未提供刪除使用者設定的 API**
  （`api-endpoints.md` 的 TOOL 區僅有 `saveSetting` / `updateSetting`，無對應 delete 端點），
  故改以 SQL 直接刪除並以 SELECT 驗證為 0 筆。**此 API 缺口值得另案評估**。

---

### VECTOR — 向量模組（`/llm/vector`）

> ⚠️ **基礎設施未啟動**：Ollama（11434）、Chroma（8000）、Milvus（19530）三個 port 實測皆 DOWN，
> 故需實際 embedding 與向量庫連線的 VEC-005 / 008 / 014 依模板既有限制標記 ⏭️。

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| VEC-001 | 新增向量設定（正常） | P0 | POST | `/llm/vector/save` | ✅ | 測試後已刪除 |
| VEC-002 | 新增向量設定（未認證） | P0 | POST | `/llm/vector/save` | ✅ | 401 |
| VEC-003 | 更新向量設定（正常） | P1 | POST | `/llm/vector/update` | ✅ | 回 `data:1`（影響 1 列） |
| VEC-004 | 取得知識庫（正常） | P1 | POST | `/llm/vector/getKnowledgeStore` | ✅ | 回空陣列（無已上傳知識庫，符合現況） |
| VEC-005 | 上傳向量文件（正常） | P0 | POST | `/llm/vector/uploadFiles` | ⏭️ | **基礎設施未啟動**，見下方覆蓋缺口 |
| VEC-006 | 上傳向量文件（未認證） | P0 | POST | `/llm/vector/uploadFiles` | ✅ | 401 |
| VEC-007 | 上傳向量文件（無附件） | P2 | POST | `/llm/vector/uploadFiles` | ✅ | |
| VEC-008 | 搜尋向量資料（正常） | P0 | POST | `/llm/vector/getDataFromEmbeddingStore` | ⏭️ | 同 VEC-005 |
| VEC-009 | 搜尋向量資料（缺 knowledgeId） | P1 | POST | `/llm/vector/getDataFromEmbeddingStore` | ✅ | |
| VEC-010 | 搜尋向量資料（缺 content） | P1 | POST | `/llm/vector/getDataFromEmbeddingStore` | ✅ | |
| VEC-011 | 搜尋向量資料（knowledgeId 不存在） | P2 | POST | `/llm/vector/getDataFromEmbeddingStore` | ✅ | 回 200 + `data:null`（空結果而非錯誤，屬可接受行為） |
| VEC-012 | 刪除向量資料（正常） | P1 | DELETE | `/llm/vector/deleteData` | ✅ | 以不存在的 knowledgeId 驗證端點行為，回 200 |
| VEC-013 | 刪除向量資料（未認證） | P1 | DELETE | `/llm/vector/deleteData` | ✅ | 401 |
| VEC-014 | 業務邏輯（上傳後可搜尋） | P1 | 多步 | `/llm/vector/*` | ⏭️ | 同 VEC-005 |

#### 執行記錄

```
### 基礎設施檢查
$ for p in 11434 8000 19530; do (timeout 3 bash -c "</dev/tcp/localhost/$p") && echo "port $p UP" || echo "port $p DOWN"; done
port 11434 DOWN     # Ollama
port 8000 DOWN      # Chroma
port 19530 DOWN     # Milvus

### VEC-001
$ curl -s -X POST "http://localhost:80/llm/vector/save" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"alias":"test_vector_lc4j","vectorStoreType":"CHROMA","vectorStore":{"collectionName":"test_collection_lc4j","dimension":384,"url":"http://localhost:8000"}}'
{"code":200,"message":"success","data":"Vector store settings saved successfully"}
HTTP_CODE=200

### VEC-002
$ curl -s -X POST "http://localhost:80/llm/vector/save" -H "Content-Type: application/json" \
  -d '{"vectorStoreType":"CHROMA","vectorStore":{"collectionName":"test","dimension":384}}'
（空 body）
HTTP_CODE=401

### VEC-003（id 自資料庫取得：VEC-001 回應不含 id）
$ curl -s -X POST "http://localhost:80/llm/vector/update" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"id":"9f0fe588-8ae2-4a49-a820-b89f9e3ceeea","alias":"updated_vector_lc4j","vectorStoreType":"CHROMA","vectorStore":{"collectionName":"updated_collection_lc4j","dimension":384,"url":"http://localhost:8000"}}'
{"code":200,"message":"success","data":1}
HTTP_CODE=200

### VEC-004
$ curl -s -X POST "http://localhost:80/llm/vector/getKnowledgeStore" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{}'
{"code":200,"message":"success","data":[]}
HTTP_CODE=200

### VEC-006
$ echo "This is test content." > /tmp/vec_test.txt
$ curl -s -X POST "http://localhost:80/llm/vector/uploadFiles" -F "file=@/tmp/vec_test.txt" \
  -F "embeddingModelId=xxx" -F "embeddingStoreId=xxx"
（空 body）
HTTP_CODE=401

### VEC-007
$ curl -s -X POST "http://localhost:80/llm/vector/uploadFiles" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -F "embeddingModelId=xxx" -F "embeddingStoreId=xxx"
{"code":400,"message":"欄位 files 不可為空值","data":null}
HTTP_CODE=400

### VEC-009
$ curl -s -X POST "http://localhost:80/llm/vector/getDataFromEmbeddingStore" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"content":"test"}'
{"code":400,"message":"欄位 knowledgeId 不可為空值","data":null}
HTTP_CODE=400

### VEC-010
$ curl -s -X POST "http://localhost:80/llm/vector/getDataFromEmbeddingStore" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"knowledgeId":"xxx"}'
{"code":400,"message":"欄位 content 不可為空值","data":null}
HTTP_CODE=400

### VEC-011
$ curl -s -X POST "http://localhost:80/llm/vector/getDataFromEmbeddingStore" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"knowledgeId":"00000000-0000-0000-0000-000000000000","content":"test"}'
{"code":200,"message":"success","data":null}
HTTP_CODE=200

### VEC-012
$ curl -s -X DELETE "http://localhost:80/llm/vector/deleteData" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"knowledgeId":"00000000-0000-0000-0000-000000000000"}'
{"code":200,"message":"success","data":"Data deleted successfully"}
HTTP_CODE=200

### VEC-013
$ curl -s -X DELETE "http://localhost:80/llm/vector/deleteData" -H "Content-Type: application/json" \
  -d '{"knowledgeId":"xxx"}'
（空 body）
HTTP_CODE=401
```

**清理**：VEC-001 建立的向量設定 `9f0fe588-…`（`test_vector_lc4j` → 更新後為 `updated_vector_lc4j`）
已刪除；`SELECT * FROM bestpartner.vector_store_setting` 確認僅剩 `rag-test`、`chroma-local-test`
兩筆既有資料，無測試殘留。

> ⚠️ **本次升級的覆蓋缺口**：Ollama / Chroma / Milvus 未啟動，故
> `EmbeddingService` 的 langchain4j 路徑（`FileSystemDocumentLoader`、`ApacheTikaDocumentParser`、
> `DocumentSplitters.recursive`、`embeddingModel.embedAll`、`EmbeddingSearchRequest`、
> `ChromaEmbeddingStore` / `MilvusEmbeddingStore`）**本次未經實機驗證**。
> 這是 1.13.0 → 1.17.2 升級中唯一未覆蓋的主要 langchain4j 表面，建議於基礎設施可用時補測。

---

## 問題追蹤區

> 測試失敗（❌）的項目需在此詳細記錄，包括 Test ID、現象、期望行為、實際行為

| Test ID | 現象 | 期望行為 | 實際行為 | 根因 | 狀態 |
|---------|------|---------|---------|------|------|
| MCP-016 | `updateSetting` 以 `getSetting` 剛驗證過的同一 id 呼叫，回 400「User setting not found」 | HTTP 200，設定內容更新為 `{"timezone":"UTC"}` | HTTP 400「User setting not found」；改傳 `settingId` 則被驗證層擋下「欄位 userSettingId 不可為空值」 | **DTO 驗證層與服務層欄位名不一致**：驗證要求 `userSettingId`，但 `McpServerService.kt:94` 的 `updateSetting` 讀的是 `mcpDTO.settingId`，恆為 null → `findById("")` 找不到。兩條路徑皆不可能成功，該端點目前完全無法使用 | ✅ **已修復**（見下方修復記錄） |
| CHAT-017 | admin 使用 `1ee80ffa`（openai/gpt-5.5-pro）同步聊天回 400 | HTTP 200 | HTTP 400「LLM服務處理發生錯誤」 | **環境限制非程式缺陷**：OpenRouter 回 402，該 LLM 設定 `maxTokens=32768` 超出金鑰可負擔的 28840 tokens。同端點以 CHAT-001（admin + deepseek-v3.2）驗證通過 | 標記 ⏭️ Skip，非 Fail |

---

## MCP-016 修復記錄（2026-07-19）

### 修復內容

`McpServerService.updateSetting`（`McpServerService.kt:92`）兩處變更：

| 項目 | 修復前 | 修復後 |
|------|--------|--------|
| 讀取的 DTO 欄位 | `mcpDTO.settingId`（與驗證層契約不符，恆為 null） | `mcpDTO.userSettingId`（與 `getSetting` / `deleteSetting` 一致） |
| 查詢方法 | `findById(id)`（**不限使用者**） | `findSettingByUserIdAndSettingId(id)`（限當前使用者） |

> ⚠️ **為何必須同時改查詢方法**：原本端點因欄位錯誤而完全無法執行，越權風險並未浮現。
> 若只修正欄位名，等於啟用一條**未做擁有權檢核**的寫入路徑——任何登入者只要知道
> `userSettingId` 即可竄改他人的 MCP 設定（其中存放加密的 token / key）。
> 故本次一併對齊 `getUserSetting` 既有的使用者範圍查詢，避免修 bug 反而開洞。

### 驗證記錄

```
### 1. MCP-016 原始失敗請求重測（user 帳號）
$ curl -s -X POST "http://localhost:80/llm/mcpServer/updateSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"589d3088-dbb3-4f71-9921-2d199d4f9cd7","settingContent":{"timezone":"UTC"}}'
{"code":200,"message":"success","data":{...,"userSettingId":"589d3088-...","settingContent":{"timezone":"UTC"},...}}
HTTP_CODE=200

### 2. 確認值真的落地（非假性成功）
$ curl -s -X POST "http://localhost:80/llm/mcpServer/getSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"userSettingId":"589d3088-dbb3-4f71-9921-2d199d4f9cd7"}'
{...,"settingContent":{"timezone":"UTC"},...}
HTTP_CODE=200

### 3. 越權寫入測試：admin 嘗試修改 user 的設定
$ curl -s -X POST "http://localhost:80/llm/mcpServer/updateSetting" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"589d3088-dbb3-4f71-9921-2d199d4f9cd7","settingContent":{"timezone":"Hacked/Zone"}}'
{"code":400,"message":"User setting not found","data":null}
HTTP_CODE=400
# 且回查 user 的資料仍為 {"timezone":"UTC"} —— 未遭竄改。
# 訊息與「id 不存在」相同，不構成存在性判別的 oracle。

### 4. 邊界：不存在的 id
$ curl -s -X POST "http://localhost:80/llm/mcpServer/updateSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"00000000-0000-0000-0000-000000000000","settingContent":{"timezone":"UTC"}}'
{"code":400,"message":"User setting not found","data":null}
HTTP_CODE=400

### 5. 敏感值加密沿用（__SECRET_KEPT__）未受影響
# 因本次改動更換了 existingEntity 的取得方式，而該 entity 會餵給 encryptEnvValues
# 作為「沿用既有密文」的依據，故須驗證加密路徑未退化。
# 以有 env 欄位的 test MCP（param0 / --param1）建立設定後：

$ getSetting → "settingContent":{"arg0":"v0","arg1":"v1","param0":"__SECRET_KEPT__","--param1":"__SECRET_KEPT__"}
# env 值遮罩、一般值明文，正確。

$ updateSetting -d '{...,"arg0":"v0-updated","param0":"__SECRET_KEPT__","--param1":"NEW-SECRET"}'  → 200
# DB 實際內容：
#   param0    = "JbYpku8QWx72Ekeu$36aHmjsEYNfOWOQDA+LSA8hjyeOt5id+09UnsPPTzo3FQTaNUw=="
#   --param1  = "oHHMSdJjlkfCm2GJ$JSQ0apjQybGEnMiQd9+DRlSdpE9k/kjc1VA="
#   arg0      = "v0-updated"（明文）
# 兩個 env 值皆為 <iv>$<密文> 格式，未以明文落地。

$ 再次 updateSetting，param0 與 --param1 皆傳 __SECRET_KEPT__、arg0 改為 v0-again → 200
# DB 比對結果：
#   arg0 = "v0-again"（確實更新）
#   param0_ciphertext_unchanged  = true
#   param1_ciphertext_unchanged  = true
# → 密文逐位元組不變，證明沿用原密文而非將 __SECRET_KEPT__ 字面值重新加密
#   （若為重新加密，隨機 IV 會使密文必然改變）。
```

### 其他驗證

| 項目 | 結果 |
|------|------|
| 編譯（uber-jar） | ✅ BUILD SUCCESSFUL |
| ArchUnit | ✅ |
| 漂移掃描（`.ps1` + `.sh`） | ✅ 新漂移 0 項 |
| 測試資料清理 | ✅ 2 筆設定已刪除，SQL 確認 leftover = 0 |

### 同類問題：deleteSetting 越權刪除（已一併修復）

`deleteMcpUserSetting`（`McpServerService.kt:133`）原使用 `deleteById(userSettingId)`，
**同樣未做使用者範圍檢核**。與 `updateSetting` 修復前不同的是，該端點當時**可正常運作**，
因此是一條**現行可利用**的越權刪除路徑：任何登入者知道 `userSettingId` 即可刪除他人設定。

**修復內容**：改為先以 `findSettingByUserIdAndSettingId` 確認擁有權，查無則拋
`MCP_USER_SETTING_NOT_FOUND`，通過後才執行刪除。

> ⚠️ **行為變更**：目標不存在時的回應由 `200 {"data":false}` 改為
> `400 User setting not found`（與 `getSetting` / `updateSetting` 一致）。
> 已確認 `bestpartner-ui` 無任何呼叫端，僅 REST 端點本身，無既有消費者受影響。
> 副作用是刪除不再具冪等性——重複刪除同一 id 會回 400。

#### 驗證記錄

```
### 1. 越權刪除：admin 嘗試刪除 user 的設定
$ curl -s -X DELETE "http://localhost:80/llm/mcpServer/deleteSetting" -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" -d '{"userSettingId":"97d155c0-271c-4ec4-8ae8-bb0b06d4ce68"}'
{"code":400,"message":"User setting not found","data":null}
HTTP_CODE=400

### 2. 確認資料未被刪除
$ curl -s -X POST "http://localhost:80/llm/mcpServer/getSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"userSettingId":"97d155c0-271c-4ec4-8ae8-bb0b06d4ce68"}'
"settingId":"97d155c0-271c-4ec4-8ae8-bb0b06d4ce68"
"settingContent":{"timezone":"Asia/Taipei"}
HTTP_CODE=200

### 3. 正常路徑未被誤傷：本人刪除自己的設定
$ curl -s -X DELETE "http://localhost:80/llm/mcpServer/deleteSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"userSettingId":"97d155c0-271c-4ec4-8ae8-bb0b06d4ce68"}'
{"code":200,"message":"success","data":true}
HTTP_CODE=200

### 4. 確認確實刪除
$ curl -s -X POST "http://localhost:80/llm/mcpServer/getSetting" ... 
{"code":400,"message":"User setting not found","data":null}
HTTP_CODE=400

### 5. 邊界：不存在的 id（與越權情境回應相同，不構成存在性 oracle）
$ curl -s -X DELETE "http://localhost:80/llm/mcpServer/deleteSetting" -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"userSettingId":"00000000-0000-0000-0000-000000000000"}'
{"code":400,"message":"User setting not found","data":null}
HTTP_CODE=400

### 6. 重複刪除（行為變更：舊版回 200 false，現回 400）
$ curl -s -X DELETE "http://localhost:80/llm/mcpServer/deleteSetting" ... （同一已刪除的 id）
{"code":400,"message":"User setting not found","data":null}
HTTP_CODE=400
```

**清理**：驗證用設定 `97d155c0-…` 已刪除，SQL 確認 leftover = 0。

> MCP-017（刪除使用者設定）的既有測試案例仍為 ✅——正常路徑回應未變（200 `data:true`）。

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
