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
7. 清場前詢問使用者資料要清理或保留，決議填入「測試資料處置」欄位（見「測試資料處置詢問」）

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
| 測試日期 | 2026-09-12 20:31 |
| 服務版本 | 0.1.8-SNAPSHOT |
| 測試環境 | **dev**（後端 dev profile，port 80） |
| LLM 平台 | **OpenRouter** `deepseek/deepseek-v3.2` |
| LLM Setting ID | |
| Base URL | `http://localhost:80` |
| **測試資料處置（清理／保留）** | **全部保留**（使用者於 Step 8 以 AskUserQuestion 決議，供後續手動驗證）。已依 [`test-data-retention.md`](../../.claude/rules/test-data-retention.md) 改名避開兜底掃描。⚠️ 後續清理責任歸使用者。 |
| **保留清單（類型／名稱／id／理由）** | **① workflow**：`8da3de2b-9487-4479-afc4-f5ec43ccfc12`，原名 `apitest-wf-202609122031` → 改名 **`keep-202609122031-多觸發點與截斷驗證`**（DRAFT、version 3）。連帶保留 `llm_workflow_execution` **2 筆**（`0f4c8874-…` SUCCESS／`944dd6db-…` FAILED-截斷）與其 node_execution 共 13 筆。**理由**：現成的雙觸發點圖形，可反覆執行觀察 `finishReason` 行為。<br>**② vector_store_setting**：`b30f887b-c151-49aa-bef0-4c556afa6b72` → 改名 **`keep-202609122031-milvus-2048`**（MILVUS, collection `apitest_202609122031`, dim 2048）。<br>**③ llm_tool_user_setting**：`ba9a8da7-1761-47d8-a96a-df5a27565d15`（`apitest-tavily-202609122031`，含 v2 密文，內容有效）＋ `879934c1-b3b7-4c97-9101-543e4c1acd2b`（`keep-202609122031-tavily`，**改名嘗試時誤新增的無效設定**，其 apiKey 為字面 `__SECRET_KEPT__` 加密值）。⚠️ **兩者皆無法經 API 刪除**（無 deleteSetting 端點，見問題追蹤 #9），如需清除請直接下 SQL：`DELETE FROM bestpartner.llm_tool_user_setting WHERE id IN ('ba9a8da7-1761-47d8-a96a-df5a27565d15','879934c1-b3b7-4c97-9101-543e4c1acd2b');`<br>**④ 上傳檔案**：`D:/tmp/bestpartner/upload/c88f57c8-…/chat-upload-test.txt`（39 bytes）。<br><br>**未觸及使用者既有資料**：`llm_knowledge` 測試前後皆 4 筆、`llm_platform` 6 筆（GROK 重複列已刪除還原）、`system_setting` 2 筆（`default_llm_platform` 維持 `OPENAI`）、`583b9222` 的 `maxTokens` 已還原為 32768（實測回 `RESTORED-OK`）。 |
| 測試結束日期 | 2026-09-12 21:15（單輪完成，0 個 ❌，無需第二輪） |

---

## 預設測試帳號參考

| 帳號 | Email | 密碼 | JWT groups | 可測試場景 |
|------|-------|------|-----------|-----------|
| `admin` | `admin@bestpartner.com.tw` | `admin` | `admin`, `user-read`, `user-write` | 所有端點（公開、`@Authenticated`、`@RolesAllowed("admin")`） |
| `user` | `user@bestpartner.com.tw` | `user` | `user-read`, `user-write` | `@Authenticated` 及 `@RolesAllowed("user-read")` 端點；admin 端點預期 403 |
| `test_user` | `test@partmer.com.tw` | `user` | （空） | 無角色端點測試；`/llm/user/get` 使用此 token 會返回 400（無 user entity） |

> ⚠️ **本次週期（202609122031）三個帳號的密碼皆非種子預設**：`admin` / `user` / `user` 一律回 401「密碼錯誤」，實際密碼由使用者提供（三帳號同一組，**不記錄於本報告**）。JWT groups 實測與上表相符：admin=[admin, user-write, user-read]、user=[user-write, user-read]、test_user=[]。

### 已知系統配置缺陷（測試前必讀）

| 缺陷 | 影響端點 | 影響測試 ID | 預期行為 |
|------|---------|------------|---------|
| `OPENROUTER_API_KEY` 環境變數（本次週期：根目錄 `.env` 已提供密文金鑰，實測可用） | `POST /llm/admin/chat`、`POST /llm/admin/customAssistantChat` | ADCHAT-001、ADCHAT-004 | `llmStore.chatModelMap` 為空 → 500；標記為 ⏭️ 環境配置限制 |
| `test_user` 帳號無 `llm_user_role` 記錄 | `POST /llm/user/get`（使用 test_user token） | USER-007（test_user） | `findUserInfo()` INNER JOIN 找不到記錄 → 400；改用 `user` token 可正常使用 |
| TOOL `saveSetting`/`updateSetting` 實際需要認證 | `POST /llm/tool/saveSetting`、`POST /llm/tool/updateSetting` | TOOL-011、TOOL-013 | API 文件標記公開，但 Service 層呼叫 `validateLoggedInUser()`；必須帶 `Authorization` header |
| VEC uploadFiles / getDataFromEmbeddingStore 需 Ollama + Milvus/Chroma | `/llm/vector/uploadFiles`、`/llm/vector/getDataFromEmbeddingStore` | VEC-005、VEC-008、VEC-014 | 基礎設施未啟動時 ⏭️ Skip |
| ~~JWT Refresh 安全漏洞（P0）~~ **已於 commit `def8f7b`（2026-05-05）修復** | 所有需認證端點 | SEC-004 | `JwtFilter` 進入 refresh 流程前先呼叫 `jwtService.isTokenSignatureValid(token)`，偽造 payload 的 token 會被擋下。本次週期實測驗證（見 SEC-004 記錄）|

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
| AUTH | 10 | 10 | 0 | 0 | 100% |
| CHAT | 18 | 17 | 0 | 1 | 94.4%（+1 加測 CHAT-EX1、+1 替代案例 CHAT-010b 皆通過） |
| ADMIN CHAT | 5 | 3 | 0 | 2 | 60%（2 ⏭️ 為資料設定限制，待裁示後重測） |
| USER | 17 | 17 | 0 | 0 | 100%（另加測 USER-007b/012b/014b 皆通過） |
| LLM SETTING | 16 | 16 | 0 | 0 | 100%（另加測 LLMSET-EX1 改金鑰通過） |
| VECTOR | 14 | 14 | 0 | 0 | 100%（模板標註可 ⏭️ 的 VEC-005/008/014 本次全部實測通過） |
| TOOL | 19 | 19 | 0 | 0 | 100%（另加測 TOOL-009b 通過） |
| MCP | 19 | 19 | 0 | 0 | 100%（另加測 MCP-EX1/EX2/EX3 擁有權檢核皆通過） |
| PERMISSION | 11 | 11 | 0 | 0 | 100% |
| SYSTEM SETTING | 13 | 13 | 0 | 0 | 100%（SYSSET-007 如實記錄既有 500 缺陷） |
| 跨模組整合 | 6 | 6 | 0 | 0 | 100% |
| 通用安全性 | 6 | 6 | 0 | 0 | 100%（含 SEC-004 修復實測確認） |
| **合計** | **154** | **151** | **0** | **3** | **98.1%** |
| WORKFLOW（補充，模板未涵蓋） | 13 | 13 | 0 | 0 | 100%（含 finishReason 把關驗收） |
| SKILL（補充，模板未涵蓋） | 8 | 8 | 0 | 0 | 100% |
| **含補充模組總計** | **175** | **172** | **0** | **3** | **98.3%** |

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
□ 測試資料清理完成（若該模組有新增資料的 API 測試；**純清場的刪除延後到 Step 8 依使用者決議處理**）
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

#### 兩種「刪除」要分清楚

| 情境 | 何時執行 | 受保留決議影響？ |
|------|---------|----------------|
| **刪除 API 本身就是測試案例**（如 `POST /llm/setting/delete` 的正常案例） | 照案例順序當場執行 | ✗ 不受影響——那是測試行為，不是清場 |
| **純為清場而刪**（案例已通過，資料留著只是髒資料） | **延後到 Step 8 依使用者決議處理** | ✓ 使用者選保留就不刪 |

#### 測試資料處置詢問（Step 8，強制）

所有模組測完、清場之前，必須以 `AskUserQuestion` 詢問使用者：本次測試資料要**清理**還是**保留**（保留供其後續手動測試）。

- 提問時附上本次建立的資料清單（類型／名稱／id），清單來自各案例執行時記錄的 id。
- 選項至少含「全部清理（預設建議）／全部保留／部分保留」。
- 決議與保留清單填入「測試週期資訊」的「測試資料處置」「保留清單」欄位。
- **保留資料不代表保留服務**：服務照 Step 9 關閉，回報時附重啟指令。

> ⚠️ 未詢問即清場或即保留，視同測試流程違規。完整規則見 [`.claude/rules/test-data-retention.md`](../../rules/test-data-retention.md)。

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
| | | | | | |

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

## 模組執行記錄

> 每個案例先寫 curl 記錄與完整 response，再標狀態（Step 7）。
> Token 變數：`$ADMIN_TOKEN` / `$USER_TOKEN` / `$TEST_USER_TOKEN`（由 `POST /login/` 取得，JWT 本文不入庫）。

### AUTH — 認證模組（`/login`）

| 測試 ID | 場景 | 優先 | HTTP | Response | 狀態 |
|---------|------|:---:|:----:|----------|:---:|
| AUTH-001 | 正常登入（admin） | P0 | 200 | `{"code":200,"message":"success","data":"<JWT>"}` | ✅ |
| AUTH-001b | 正常登入（user） | P0 | 200 | `{"code":200,"message":"success","data":"<JWT>"}` | ✅ |
| AUTH-002 | 驗證 JWT（admin） | P0 | 200 | `{"code":200,"message":"success","data":"check"}` | ✅ |
| AUTH-002b | 驗證 JWT（test_user，無 user-read） | P0 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| AUTH-003 | 密碼錯誤 | P1 | 401 | `{"code":401,"message":"密碼錯誤","data":null}` | ✅ |
| AUTH-004 | 信箱不存在 | P1 | 401 | `{"code":401,"message":"帳號不存在","data":null}` | ✅ |
| AUTH-005 | 缺 email | P1 | 400 | `{"code":400,"message":"欄位 email 不可為空值","data":null}` | ✅ |
| AUTH-006 | 缺 password | P1 | 400 | `{"code":400,"message":"欄位 password 不可為空值","data":null}` | ✅ |
| AUTH-007 | 空 body | P1 | 400 | `{"code":400,"message":"欄位 email 不可為空值\n欄位 password 不可為空值","data":null}` | ✅ |
| AUTH-008 | email 格式不合法 | P2 | 401 | `{"code":401,"message":"帳號不存在","data":null}`（預期 400 或 401，符合） | ✅ |
| AUTH-009 | 無效 token 呼叫 check | P1 | 401 | `{"code":401,"message":"Invalid or malformed token, please log in again","data":null}` | ✅ |
| AUTH-010 | 超長 password（1000 字元） | P2 | 401 | `{"code":401,"message":"密碼錯誤","data":null}` | ✅ |

<details>
<summary>AUTH curl 記錄</summary>

```bash
# AUTH-001 / 001b（密碼以 $PW 代入，不記錄明文）
curl -s -X POST "$BASE/login/" -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@bestpartner.com.tw\",\"password\":\"$PW\"}"
curl -s -X POST "$BASE/login/" -H "Content-Type: application/json" \
  -d "{\"email\":\"user@bestpartner.com.tw\",\"password\":\"$PW\"}"
# AUTH-002 / 002b
curl -s -X POST "$BASE/login/check" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" -d '{}'
curl -s -X POST "$BASE/login/check" -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" -d '{}'
# AUTH-003 / 004 / 005 / 006 / 007 / 008
curl -s -X POST "$BASE/login/" -H "Content-Type: application/json" -d '{"email":"admin@bestpartner.com.tw","password":"wrongpassword"}'
curl -s -X POST "$BASE/login/" -H "Content-Type: application/json" -d '{"email":"notexist@example.com","password":"anypassword"}'
curl -s -X POST "$BASE/login/" -H "Content-Type: application/json" -d '{"password":"admin"}'
curl -s -X POST "$BASE/login/" -H "Content-Type: application/json" -d '{"email":"admin@bestpartner.com.tw"}'
curl -s -X POST "$BASE/login/" -H "Content-Type: application/json" -d '{}'
curl -s -X POST "$BASE/login/" -H "Content-Type: application/json" -d '{"email":"not-an-email","password":"admin"}'
# AUTH-009 / 010
curl -s -X POST "$BASE/login/check" -H "Authorization: Bearer invalidtoken" -H "Content-Type: application/json" -d '{}'
LONGPW=$(printf 'x%.0s' $(seq 1 1000))
curl -s -X POST "$BASE/login/" -H "Content-Type: application/json" -d "{\"email\":\"admin@bestpartner.com.tw\",\"password\":\"$LONGPW\"}"
```

</details>

### CHAT — 聊天模組（`/llm`）

> ⚠️ **模板的「常用已知 ID」歸屬已過期**：`CHAT_LLM_ID` `583b9222-…`（OpenRouter deepseek-v3.2, CHAT）實際屬 **admin**，非 test_user。
> test_user 名下的 OpenRouter 設定只有 `2b44c811-…`（STREAMING_CHAT）。本次依實際歸屬調整執行帳號，並已列入待修正清單。
> `user@bestpartner.com.tw` 名下**無任何 LLM 設定**。

| 測試 ID | 場景 | 優先 | HTTP | Response | 狀態 |
|---------|------|:---:|:----:|----------|:---:|
| CHAT-001 | 同步聊天（admin + `583b9222`） | P0 | 200 | `{"code":200,"message":"success","data":"OK"}` | ✅ |
| CHAT-002 | 同步聊天（未認證） | P0 | 401 | （空 body） | ✅ |
| CHAT-003 | 缺 llmId | P1 | 400 | `{"code":400,"message":"欄位 llmId 不可為空值","data":null}` | ✅ |
| CHAT-004 | 缺 message | P1 | 400 | `{"code":400,"message":"欄位 message 不可為空值","data":null}` | ✅ |
| CHAT-005 | llmId 不存在 | P1 | 400 | `{"code":400,"message":"Please verify the LLM setting you are accessing exists","data":null}` | ✅ |
| CHAT-006 | 串流聊天（test_user + `2b44c811`） | P0 | 200 | `data:OK`（SSE，text/plain） | ✅ |
| CHAT-007 | 串流聊天（未認證） | P0 | 401 | （空 body） | ✅ |
| CHAT-008 | 自定義助手聊天（memory + promptContent） | P1 | 200 | `{"code":200,"message":"success","data":"OK"}` | ✅ |
| CHAT-009 | 缺 promptContent | P1 | 400 | `{"code":400,"message":"欄位 promptContent 不可為空值\nmemory: 欄位 id 不可為空值","data":null}` | ✅ |
| CHAT-010 | 自定義助手 + `mcpSettingIds`（**date**，`216810da-…`） | P2 | 200 | `{"code":200,...,"data":"I'll check the current date for you.\n\nI'll use the current date tool to get the exact information."}` — **工具實際未掛上**：日誌 `已配置 0 個MCP客戶端`，`buildUserSpecificMcpClient` 拋 `IllegalStateException: Operation cancelled due to transport failure: Process has exited`。根因＝`D:/MCP/date-1.0-SNAPSHOT.jar` 僅 7 KB 的 **thin jar（無 Main-Class）**，非 uber-jar → **環境限制，非程式缺陷** | ⏭️ |
| CHAT-010b | 同上改用 **google_map**（uber-jar，`1e575cec-…`） | P2 | 200 | 首輪模型回問缺少的 `language` / `maxResults`（**證明工具 schema 已載入**）；補齊參數後實際呼叫成功：`名稱：渣打銀行 信義分行、place_id：ChIJ_1dgl7qrQjQRLnEKiYk2w5o、地址：110台灣臺北市信義區安康里松仁路97號2樓`。**同時驗證 `llm_mcp_user_setting.setting_content` 整欄 AES-GCM 密文解密後正確注入 env** | ✅ |
| CHAT-011 | 自定義助手串流 | P1 | 200 | `data:OK`（SSE） | ✅ |
| CHAT-012 | 上傳檔案（正常） | P1 | 200 | `{"code":200,"message":"success","data":["chat-upload-test.txt"]}` | ✅ |
| CHAT-013 | 上傳檔案（未認證） | P1 | 401 | （空 body） | ✅ |
| CHAT-014 | 上傳檔案（無附件） | P2 | 400 | `{"code":400,"message":"欄位 files 不可為空值","data":null}` | ✅ |
| CHAT-015 | 上傳檔案（12 MB 超大檔） | P2 | 413 | （空 body，Payload Too Large） | ✅ |
| CHAT-016 | message 為空字串 | P2 | 400 | `{"code":400,"message":"欄位 message 不可為空值","data":null}` | ✅ |
| CHAT-017 | 同步聊天（admin 自己的 llmId） | P1 | 200 | `{"code":200,"message":"success","data":"ADMIN-OK"}` | ✅ |
| CHAT-018 | 串流聊天（admin 自己的 `4ba6eb24` deepseek-v4-flash） | P1 | 200 | `data:AD` / `data:MIN-ST` / `data:REAM-` / `data:OK`（分塊 SSE） | ✅ |
| CHAT-EX1 | **擁有權隔離（本次加測）**：test_user 用 admin 的 llmId | P1 | 400 | `{"code":400,"message":"Please verify the LLM setting you are accessing exists","data":null}` — 不洩漏「存在但無權」 | ✅ |

<details>
<summary>CHAT curl 記錄（節錄關鍵案例）</summary>

```bash
# CHAT-001 / 017
curl -s -X POST "$BASE/llm/chat" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Reply with exactly: OK"}'
# CHAT-006（test_user 的 STREAMING 設定）
curl -s -X POST "$BASE/llm/chatStreaming" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" \
  -d '{"llmId":"2b44c811-d38c-417b-89d4-ae675b3093f5","message":"Reply with exactly: OK"}'
# CHAT-008 / 009
curl -s -X POST "$BASE/llm/customAssistantChat" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-…","message":"Reply with exactly: OK","promptContent":"You are a helpful assistant.","memory":{"id":"test-mem-202609122031"}}'
# CHAT-010b（MCP 工具實際呼叫）
curl -s -X POST "$BASE/llm/customAssistantChat" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-…","message":"用 searchPlaces 工具查詢：query=台北市信義區 渣打銀行, language=zh-TW, maxResults=3。列出第一筆的名稱與 place_id。","memory":{"id":"mcp-gm2-202609122031","maxSize":5},"promptContent":"You are helpful. Use tools when needed.","mcpSettingIds":["1e575cec-3229-4da6-94d1-924dc56060cb"]}'
# CHAT-012 / 015（⚠️ multipart 欄位名為 file，非 files —— 見 FilesFromRequest.@FormParam("file")）
curl -s -X POST "$BASE/llm/uploadFile" -H "Authorization: Bearer $ADMIN_TOKEN" -F "file=@chat-upload-test.txt"
head -c 12582912 /dev/urandom > big-upload.bin
curl -s -X POST "$BASE/llm/uploadFile" -H "Authorization: Bearer $ADMIN_TOKEN" -F "file=@big-upload.bin"
# CHAT-EX1 擁有權隔離
curl -s -X POST "$BASE/llm/chat" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-8cb0-4109-b072-5f0fd1e9fed9","message":"Hello"}'
```

</details>

### ADMIN CHAT — 管理員聊天模組（`/llm/admin`）

> ⚠️ **模板「已知缺陷」表的記載已過時**：舊記載為「`OPENROUTER_API_KEY` 未設定 → 500」。
> 本次實測根因**不同**：`.env` 的 `OPENROUTER_API_KEY` 已設定且有效（CHAT 模組實測可用），
> 但 `LLMStore.initChatModelMap()` 的註冊條件是「系統設定 `default_llm_platform` 指定的平台」
> ∩「admin 名下該平台的 **CHAT** 類型 LLM 設定」。
> 現況 `default_llm_platform` = **`OPENAI`**，而 admin 名下**沒有 OPENAI 的 CHAT 設定**
> （只有 OLLAMA CHAT、OPENROUTER CHAT ×2、EMBEDDING ×2），故 `chatModelMap` 為空 → 400 `LLM model not found`。
> **屬資料設定問題，非程式缺陷。**
>
> ⚠️ 本次**未**變更該系統設定重測：修改 `default_llm_platform` 屬於改動使用者共享環境，
> 已被權限機制攔下，且未取得使用者授權。需使用者裁示後另行重測（見「問題追蹤區」#2）。
>
> ℹ️ 另一發現：服務必須從 **repo 根目錄**啟動，Quarkus 才會載入根目錄 `.env`。
> 首次從 `bestpartner-service/` 啟動時 `.env` 未載入；已重啟修正後重跑，結果不變（證實根因在上述資料設定）。

| 測試 ID | 場景 | 優先 | HTTP | Response | 狀態 |
|---------|------|:---:|:----:|----------|:---:|
| ADCHAT-001 | 管理員聊天（admin 正常） | P0 | 400 | `{"code":400,"message":"LLM model not found","data":null}` — 根因見上方說明（`default_llm_platform=OPENAI` 而 admin 無 OPENAI CHAT 設定） | ⏭️ |
| ADCHAT-002 | 管理員聊天（一般用戶存取） | P0 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| ADCHAT-003 | 管理員聊天（未認證） | P0 | 401 | （空 body） | ✅ |
| ADCHAT-004 | 管理員自定義助手聊天（admin） | P1 | 400 | `{"code":400,"message":"LLM model not found","data":null}` — 同 ADCHAT-001 | ⏭️ |
| ADCHAT-005 | 管理員自定義助手聊天（一般用戶） | P1 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |

<details>
<summary>ADMIN CHAT curl 記錄</summary>

```bash
# ADCHAT-001 / 004（admin）
curl -s -X POST "$BASE/llm/admin/chat" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Reply with exactly: ADMIN-CHAT-OK"}'
curl -s -X POST "$BASE/llm/admin/customAssistantChat" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Reply with exactly: ADMIN-ASSIST-OK"}'
# ADCHAT-002 / 005（一般用戶 → 403）
curl -s -X POST "$BASE/llm/admin/chat" -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Hello"}'
curl -s -X POST "$BASE/llm/admin/customAssistantChat" -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Hello"}'
# ADCHAT-003（未認證 → 401）
curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/llm/admin/chat" \
  -H "Content-Type: application/json" -d '{"platform":"OPENROUTER","message":"Hello"}'
# 佐證：系統設定現況
curl -s "$BASE/systemSetting/list"
# → {"id":1,"key":"default_llm_platform","value":"OPENAI","description":"system default llm_platform"}
```

</details>

### USER — 用戶模組（`/llm/user`）

> 本次建立並於 USER-014 案例中刪除的測試帳號：`apitest_202609122031`（id `11850c49-6774-4743-983e-463010e97174`）。
> 測試結束時該帳號已不存在（USER-017 驗證登入回 401「帳號不存在」）。

| 測試 ID | 場景 | 優先 | HTTP | Response | 狀態 |
|---------|------|:---:|:----:|----------|:---:|
| USER-001 | 註冊（正常新帳號） | P0 | 200 | `{"code":200,"message":"success","data":{"id":"11850c49-6774-4743-983e-463010e97174","username":"apitest_202609122031","password":"testpass",…,"status":"ACTIVE","role":null}}` ⚠️ **回應原樣回傳明文密碼**（見問題追蹤 #1） | ✅ |
| USER-002 | 註冊（重複 email） | P1 | 400 | `{"code":400,"message":"Database processing error","data":null}` | ✅ |
| USER-003 | 註冊（缺 username） | P1 | 400 | `{"code":400,"message":"欄位 username 不可為空值","data":null}` | ✅ |
| USER-004 | 註冊（缺 password） | P1 | 400 | `{"code":400,"message":"欄位 password 不可為空值","data":null}` | ✅ |
| USER-005 | 註冊（缺 email） | P1 | 400 | `{"code":400,"message":"欄位 email 不可為空值","data":null}` | ✅ |
| USER-006 | 註冊（非法 email） | P2 | 400 | `{"code":400,"message":"欄位 email 的 email 格式不正確","data":null}` | ✅ |
| USER-007 | 取得用戶資訊（user token） | P0 | 200 | `{"code":200,…,"data":{"id":"5554f255-…","username":"user","password":null,…,"role":{"roleNum":"1","roleName":"USER"}}}` — **`password` 正確為 null** | ✅ |
| USER-007b | 取得用戶資訊（test_user，無 llm_user_role） | P0 | 400 | `{"code":400,"message":"User not found","data":null}` — 與模板「已知缺陷」記載一致 | ✅ |
| USER-008 | 取得用戶資訊（未認證） | P0 | 401 | （空 body） | ✅ |
| USER-009 | 更新用戶（正常） | P1 | 200 | `{"code":200,…,"data":{"id":"11850c49-…","nickname":"updated-nick","email":"apitest_updated_202609122031@test.com","status":"ACTIVE"}}` ⚠️ `status` **為必填**（未帶回 400「欄位 status 不可為空值」），模板範本未標明 | ✅ |
| USER-010 | 更新用戶（缺 id） | P1 | 400 | `{"code":400,"message":"欄位 id 不可為空值\n欄位 status 不可為空值","data":null}` | ✅ |
| USER-011 | 更新用戶（id 不存在） | P1 | 400 | `{"code":400,"message":"User not found","data":null}` | ✅ |
| USER-012 | 切換狀態（admin 正常） | P1 | 200 | `{"code":200,…,"data":{…,"status":"INACTIVE","role":{"roleNum":"1","roleName":"USER"}}}` | ✅ |
| USER-012b | 切換狀態（一般用戶，加測） | P1 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| USER-013 | 切換狀態（缺 status） | P1 | 400 | `{"code":400,"message":"id and status are required","data":null}` | ✅ |
| USER-014 | 刪除用戶（admin 正常） | P1 | 200 | `{"code":200,"message":"success","data":true}` | ✅ |
| USER-014b | 刪除用戶（一般用戶，加測） | P1 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| USER-015 | 刪除用戶（缺 id） | P1 | 400 | `{"code":400,"message":"欄位 id 不可為空值","data":null}` | ✅ |
| USER-016 | 刪除用戶（id 不存在） | P2 | 200 | `{"code":200,"message":"success","data":false}` — 回 `false` 表示未刪到，符合模板「預期返回 0」 | ✅ |
| USER-017 | 業務邏輯（register → update → switchStatus → delete → 登入） | P1 | — | INACTIVE 後登入 `{"code":400,"message":"Account is disabled"}`；刪除後登入 `{"code":401,"message":"帳號不存在"}` | ✅ |

<details>
<summary>USER curl 記錄</summary>

```bash
TS=202609122031
# USER-001 / 002
curl -s -X POST "$BASE/llm/user/register" -H "Content-Type: application/json" \
  -d "{\"username\":\"apitest_$TS\",\"password\":\"testpass\",\"email\":\"apitest_$TS@test.com\"}"
curl -s -X POST "$BASE/llm/user/register" -H "Content-Type: application/json" \
  -d "{\"username\":\"another_user_$TS\",\"password\":\"testpass\",\"email\":\"apitest_$TS@test.com\"}"
# USER-003 / 004 / 005 / 006
curl -s -X POST "$BASE/llm/user/register" -H "Content-Type: application/json" -d '{"password":"testpass","email":"missing_username@test.com"}'
curl -s -X POST "$BASE/llm/user/register" -H "Content-Type: application/json" -d '{"username":"nopw","email":"nopw@test.com"}'
curl -s -X POST "$BASE/llm/user/register" -H "Content-Type: application/json" -d '{"username":"noemail","password":"testpass"}'
curl -s -X POST "$BASE/llm/user/register" -H "Content-Type: application/json" -d '{"username":"bademail","password":"testpass","email":"not-an-email"}'
# USER-007 / 007b / 008
curl -s -X POST "$BASE/llm/user/get" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{}'
curl -s -X POST "$BASE/llm/user/get" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" -d '{}'
curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/llm/user/get" -H "Content-Type: application/json" -d '{}'
# USER-009 / 010 / 011（⚠️ status 必填）
curl -s -X POST "$BASE/llm/user/update" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"11850c49-…","email":"apitest_updated_202609122031@test.com","nickname":"updated-nick","status":"ACTIVE"}'
curl -s -X POST "$BASE/llm/user/update" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"email":"x@test.com"}'
curl -s -X POST "$BASE/llm/user/update" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"00000000-0000-0000-0000-000000000000","email":"x@test.com","status":"ACTIVE"}'
# USER-012 / 012b / 013
curl -s -X POST "$BASE/llm/user/switchStatus" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"11850c49-…","status":"INACTIVE"}'
curl -s -X POST "$BASE/llm/user/switchStatus" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"id":"11850c49-…","status":"ACTIVE"}'
curl -s -X POST "$BASE/llm/user/switchStatus" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"11850c49-…"}'
# USER-014 / 014b / 015 / 016 / 017
curl -s -X DELETE "$BASE/llm/user/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"11850c49-…"}'
curl -s -X DELETE "$BASE/llm/user/delete" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"id":"11850c49-…"}'
curl -s -X DELETE "$BASE/llm/user/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{}'
curl -s -X DELETE "$BASE/llm/user/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"00000000-0000-0000-0000-000000000000"}'
curl -s -X POST "$BASE/login/" -H "Content-Type: application/json" -d '{"email":"apitest_updated_202609122031@test.com","password":"testpass"}'
```

</details>

### LLM SETTING — LLM 設定模組（`/llm/setting`）

> 🔑 **本模組為本次發版前回歸的重點**（欄位加密 AES-256-GCM v2 + legacy 相容 + 遮罩沿用）。
> 測試資料：`apitest-202609122031`（id `699dcec6-43c3-4c89-a021-8fdd7444ce35`，test_user 名下）
> 已於 LLMSET-010 刪除；測試用平台（GROK 重複列 `82a5ddca-…`）已於 LLMSET-014 刪除，
> `llm_platform` 的 GROK 已還原為 1 筆。

| 測試 ID | 場景 | 優先 | HTTP | Response | 狀態 |
|---------|------|:---:|:----:|----------|:---:|
| LLMSET-001 | 新增設定（正常） | P0 | 200 | `{"code":200,…,"data":{…,"llmModel":{…,"apiKey":"__SECRET_KEPT__",…},"id":"699dcec6-…","alias":"apitest-202609122031","modelType":"CHAT"}}` — **回應即遮罩** | ✅ |
| LLMSET-002 | 新增設定（未認證） | P0 | 401 | （空 body） | ✅ |
| LLMSET-003 | 缺 modelType | P1 | 400 | `{"code":400,"message":"欄位 modelType 不可為空值","data":null}` | ✅ |
| LLMSET-004 | 缺 platformId | P1 | 400 | `{"code":400,"message":"欄位 platformId 不可為空值","data":null}` | ✅ |
| LLMSET-005 | 缺 modelName | P1 | 400 | `{"code":400,"message":"llmModel: 欄位 modelName 不可為空值","data":null}` | ✅ |
| LLMSET-006 | 更新設定（`apiKey` 傳 `__SECRET_KEPT__`） | P1 | 200 | `{"code":200,"message":"success","data":"LLM setting updated successfully"}`；**DB 解密回 `sk-or-v1-apitest-plaintext-…`（＝原明文）** → 沿用正確（密文本身因 per-record salt 重新產生而改變，屬預期） | ✅ |
| LLMSET-007 | 取得設定（無篩選） | P0 | 200 | 12 筆；**全部 `apiKey` 皆為 `__SECRET_KEPT__` 或 null，無明文外流** | ✅ |
| LLMSET-008 | 取得設定（依 llmId 篩選） | P1 | 200 | 回單筆 `699dcec6-…`，`apiKey":"__SECRET_KEPT__"`、`maxTokens:8192`（LLMSET-006 的更新已生效） | ✅ |
| LLMSET-009 | 取得設定（llmId 不存在） | P2 | 200 | `{"code":200,"message":"success","data":[]}` | ✅ |
| LLMSET-010 | 刪除設定（正常） | P1 | 200 | `{"code":200,"message":"success","data":true}` | ✅ |
| LLMSET-011 | 刪除設定（缺 id） | P1 | 400 | `{"code":400,"message":"欄位 id 不可為空值","data":null}` | ✅ |
| LLMSET-012 | 新增平台（admin 正常） | P1 | 200 | `{"code":200,…,"data":{"id":"82a5ddca-…","platform":"GROK"}}`。⚠️ 兩項發現：① `platform` 必須是 `Platform` **enum 成員**，自訂字串回 400；② **允許新增重複平台名稱**（GROK 變 2 筆，無唯一性約束）→ 見問題追蹤 #3 | ✅ |
| LLMSET-013 | 新增平台（一般用戶） | P1 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| LLMSET-014 | 刪除平台（admin 正常） | P1 | 200 | `{"code":200,"message":"success","data":true}`；DB 確認 GROK 已還原為 1 筆 | ✅ |
| LLMSET-015 | 刪除平台（一般用戶） | P1 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| LLMSET-016 | 業務邏輯（save → get → delete → get） | P1 | 200 | 刪除後查詢回 `{"code":200,"message":"success","data":[]}` | ✅ |
| LLMSET-EX1 | **加測：改金鑰**（`apiKey` 傳新明文） | P1 | 200 | 更新成功；DB 解密回 `sk-or-v1-ROTATED-202609122031`（＝新明文）→ 非遮罩值時正確覆寫 | ✅ |

#### 🔑 加密回歸專項驗證（本次發版前重點）

| 驗證項 | 方法 | 結果 |
|--------|------|------|
| **v2 密文落地格式** | LLMSET-001 存入明文後直接查 DB | `v2$/mNYDhFfUzkE9TycmsFNDg==$ia3cP7DvPWcnEJZx$…`（長度 121）→ **v2 四段格式正確**（PBKDF2＋per-record salt） |
| **legacy 兩段密文相容** | 統計 DB 格式分佈後，用 legacy 密文的設定做真實 LLM 呼叫 | `llm_setting` 現況：**legacy 7 筆 / v2 2 筆 / 掩蔽佔位 7 筆 / 空 4 筆**。CHAT-001、017 實際呼叫成功的 `583b9222-…`（deepseek-v3.2）**正是 legacy 兩段密文** → **legacy 解密相容確認無誤**。CHAT-006 用的 `2b44c811-…` 亦為 legacy，串流正常 |
| **明文（掩蔽佔位）相容** | 種子資料的 7 筆 `sk-xxx` / `xxx` / `xai-xxx` 等非密文值 | `setting/get` 全部正常回應（200）未拋解密例外 → converter 的「非密文視為明文原樣回傳」相容路徑正常 |
| **`__SECRET_KEPT__` 沿用** | LLMSET-006 後以 `decryptDataSecret` 解密 DB 值 | 解回 **原明文**（非字面 `__SECRET_KEPT__`）→ 沿用正確 |
| **非遮罩值正確覆寫** | LLMSET-EX1 後解密 | 解回**新明文** → 改金鑰正常 |
| **明文不跨 HTTP 邊界** | LLMSET-007 掃描全部 12 筆回應 | `apiKey` 全為 `__SECRET_KEPT__` 或 null，**零明文外流** |

<details>
<summary>LLM SETTING curl／驗證記錄</summary>

```bash
PLATFORM=006f1076-b023-4197-b917-70455f2a3501
# LLMSET-001
curl -s -X POST "$BASE/llm/setting/save" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" \
  -d "{\"platformId\":\"$PLATFORM\",\"modelType\":\"CHAT\",\"alias\":\"apitest-202609122031\",\"llmModel\":{\"modelName\":\"deepseek/deepseek-v3.2\",\"apiKey\":\"<測試用假金鑰>\"}}"
# LLMSET-006（遮罩沿用）／EX1（改金鑰）
curl -s -X POST "$BASE/llm/setting/update" -H "Authorization: Bearer $TEST_USER_TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"699dcec6-…","platformId":"006f1076-…","modelType":"CHAT","alias":"apitest-202609122031-updated","llmModel":{"modelName":"deepseek/deepseek-v3.2","apiKey":"__SECRET_KEPT__","maxTokens":8192}}'
# 密文落地與解密驗證（走 data-secret skill 流程 B）
docker exec postgres-db psql -U pguser -d pgdb -t -A -c "SELECT api_key FROM bestpartner.llm_setting WHERE id='699dcec6-…';"
$env:CRYPTO_SECRET_KEY='<本機預設金鑰>'; $env:CRYPTO_SECRET_VALUE='<密文本體>'
& "$PWD\bestpartner-service\gradlew.bat" decryptDataSecret -q -p bestpartner-service
# 格式分佈統計
docker exec postgres-db psql -U pguser -d pgdb -t -c "SELECT CASE WHEN api_key LIKE 'v2\$%' THEN 'v2' WHEN api_key LIKE '%\$%' THEN 'legacy' WHEN api_key IS NULL OR api_key='' THEN 'null/空' ELSE '掩蔽佔位' END AS fmt, count(*) FROM bestpartner.llm_setting GROUP BY 1;"
# LLMSET-012 / 014（平台）
curl -s -X POST "$BASE/llm/setting/platform/add" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"platform":"GROK"}'
curl -s -X POST "$BASE/llm/setting/platform/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"82a5ddca-…"}'
```

</details>

### VECTOR — 向量模組（`/llm/vector`）

> 基礎設施本次**全部就緒**（Milvus standalone healthy、OpenRouter embedding 可用），
> 故模板標註「需 Ollama + Milvus/Chroma，未啟動則 ⏭️」的 VEC-005 / 008 / 014 **本次全部實測執行**。
> embedding 使用 `3b624ce5-…`（OpenRouter `nvidia/nemotron-3-embed-1b:free`，**2048 維**），
> 向量庫維度同為 2048（維度不一致會到檢索才炸，見 e2e-test-plan §J15 前置）。

| 測試 ID | 場景 | 優先 | HTTP | Response | 狀態 |
|---------|------|:---:|:----:|----------|:---:|
| VEC-001 | 新增向量設定（MILVUS，dim 2048） | P0 | 200 | `{"code":200,"message":"success","data":"Vector store settings saved successfully"}`；DB 新增 `b30f887b-…` | ✅ |
| VEC-002 | 新增向量設定（未認證） | P0 | 401 | （空 body） | ✅ |
| VEC-003 | 更新向量設定 | P1 | 200 | `{"code":200,"message":"success","data":1}` | ✅ |
| VEC-004 | 取得知識庫 | P1 | 200 | 回既有知識庫 `90c4deb7-…`（`e2e-scb-tnc-202607272132`，`tw-online-tnc.pdf`, 373745 bytes, embeddingModelId `3b624ce5-…`, embeddingStoreId `b8828b60-…`） | ✅ |
| VEC-005 | 上傳向量文件（真實 embedding + Milvus） | P0 | 200 | `{"code":200,"message":"success","data":"4e691057-b060-4da4-aed8-979874809c40"}` — 回傳新 knowledgeId | ✅ |
| VEC-006 | 上傳向量文件（未認證） | P0 | 401 | （空 body） | ✅ |
| VEC-007 | 上傳向量文件（無附件） | P2 | 400 | `{"code":400,"message":"欄位 files 不可為空值","data":null}` | ✅ |
| VEC-008 | 向量相似度搜尋（真實資料） | P0 | 200 | 以「開立數位存款帳戶的年齡條件」檢索，**Top-1 命中原句**：`1. 立約人應為具中華民國國籍且未受監護宣告或輔助宣告之年滿二十歲自然人。…`（`tw-online-tnc.pdf`） | ✅ |
| VEC-009 | 搜尋（缺 knowledgeId） | P1 | 400 | `{"code":400,"message":"欄位 knowledgeId 不可為空值","data":null}` | ✅ |
| VEC-010 | 搜尋（缺 content） | P1 | 400 | `{"code":400,"message":"欄位 content 不可為空值","data":null}` | ✅ |
| VEC-011 | 搜尋（knowledgeId 不存在） | P2 | 200 | `{"code":200,"message":"success","data":null}` | ✅ |
| VEC-012 | 刪除向量資料（正常） | P1 | 200 | `{"code":200,"message":"success","data":"Data deleted successfully"}`；刪除後再搜尋回 `data:null` | ✅ |
| VEC-013 | 刪除向量資料（未認證） | P1 | 401 | （空 body） | ✅ |
| VEC-014 | 業務邏輯（上傳 → 搜尋） | P1 | 200 | 上傳後立即檢索命中自身內容：`數位存款帳戶的 API 測試專用文件。測試代碼 APITEST-202609122031。…`（`vec_test.txt`） → **上傳→embedding→落 Milvus→檢索閉環成立** | ✅ |

> ⚠️ **VEC-012 的已知限制**：`deleteData` 只刪向量資料，**不刪 `llm_knowledge` metadata 列**
> （e2e-test-plan §J15 已記載）。故 knowledgeId `4e691057-…` 的 metadata 仍在 DB，
> 向量庫設定 `b30f887b-…`（`apitest_vec_202609122031_updated`）亦仍在 —— 兩者列入收尾資料處置清單。

<details>
<summary>VECTOR curl 記錄</summary>

```bash
KB=90c4deb7-fa5b-4a99-a510-3294f6bbcd10   # 既有（E2E 保留）
VS=b30f887b-c151-49aa-bef0-4c556afa6b72   # 本次建立
KB2=4e691057-b060-4da4-aed8-979874809c40  # 本次建立
# VEC-001 / 003
curl -s -X POST "$BASE/llm/vector/save" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"alias":"apitest_vec_202609122031","vectorStoreType":"MILVUS","vectorStore":{"collectionName":"apitest_202609122031","dimension":2048,"url":"http://localhost:19530"}}'
curl -s -X POST "$BASE/llm/vector/update" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"b30f887b-…","alias":"apitest_vec_202609122031_updated","vectorStoreType":"MILVUS","vectorStore":{"collectionName":"apitest_202609122031","dimension":2048,"url":"http://localhost:19530"}}'
# VEC-004 / 005（⚠️ multipart 欄位名為 file）
curl -s -X POST "$BASE/llm/vector/getKnowledgeStore" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d "{\"knowledgeId\":\"$KB\"}"
curl -s -X POST "$BASE/llm/vector/uploadFiles" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "file=@vec_test.txt" -F "embeddingModelId=3b624ce5-…" -F "embeddingStoreId=$VS" \
  -F "name=apitest-kb-202609122031" -F "desc=API 回歸測試知識庫"
# VEC-008 / 014
curl -s -X POST "$BASE/llm/vector/getDataFromEmbeddingStore" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "{\"knowledgeId\":\"$KB\",\"content\":\"開立數位存款帳戶的年齡條件\"}"
curl -s -X POST "$BASE/llm/vector/getDataFromEmbeddingStore" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "{\"knowledgeId\":\"$KB2\",\"content\":\"測試代碼是什麼\"}"
# VEC-009 / 010 / 011 / 012 / 013
curl -s -X POST "$BASE/llm/vector/getDataFromEmbeddingStore" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"content":"test"}'
curl -s -X POST "$BASE/llm/vector/getDataFromEmbeddingStore" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d "{\"knowledgeId\":\"$KB\"}"
curl -s -X POST "$BASE/llm/vector/getDataFromEmbeddingStore" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"knowledgeId":"00000000-0000-0000-0000-000000000000","content":"test"}'
curl -s -X DELETE "$BASE/llm/vector/deleteData" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d "{\"knowledgeId\":\"$KB2\"}"
curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/llm/vector/deleteData" -H "Content-Type: application/json" -d '{"knowledgeId":"xxx"}'
```

</details>

### TOOL — 工具模組（`/llm/tool`）

> 🔑 **本模組亦為加密回歸重點**：`llm_tool_user_setting.setting_content` 由 `SensitiveValueCodec`
> 對 `@ToolConfigField(sensitive=true)` 標記的 key **逐值加密**（非整欄）。
> 測試資料：工具 `ApiTestTool202609122031`（`05fb0824-…`）、分類 `APITEST_GRP_UPDATED`（`990e949e-…`）
> 已分別於 TOOL-009 / TOOL-018 刪除；工具設定 `ba9a8da7-…` 仍存在 → 列入收尾處置清單。

| 測試 ID | 場景 | 優先 | HTTP | Response | 狀態 |
|---------|------|:---:|:----:|----------|:---:|
| TOOL-001 | 列出工具（公開） | P0 | 200 | 回工具清單，含 `settingSchema`（TavilySearch 的 `apiKey` 標 `"required":true,"sensitive":true`） | ✅ |
| TOOL-002 | 取得工具（公開） | P1 | 200 | 回 `f95fda5f-…` TavilySearch 完整資訊（含 `settingArgs` 與 `settingSchema`） | ✅ |
| TOOL-003 | 取得工具（缺 id） | P1 | 400 | `{"code":400,"message":"欄位 id 不可為空值","data":null}` | ✅ |
| TOOL-004 | 取得工具（id 不存在） | P2 | 400 | `{"code":400,"message":"Tool not found","data":null}` | ✅ |
| TOOL-005 | 註冊工具（admin 正常） | P1 | 200 | `{"code":200,…,"data":{"id":"05fb0824-e62c-496c-97e2-0fc2b05ef0b6","name":"ApiTestTool202609122031",…}}` | ✅ |
| TOOL-006 | 註冊工具（缺 name） | P1 | 400 | `{"code":400,"message":"欄位 name 不可為空值","data":null}` | ✅ |
| TOOL-007 | 註冊工具（缺 classPath） | P1 | 400 | `{"code":400,"message":"欄位 classPath 不可為空值","data":null}` | ✅ |
| TOOL-008 | 註冊工具（缺 type） | P1 | 400 | `{"code":400,"message":"欄位 type 不可為空值","data":null}` | ✅ |
| TOOL-009 | 刪除工具（admin 正常） | P1 | 200 | `{"code":200,"message":"success","data":true}` | ✅ |
| TOOL-009b | 刪除工具（一般用戶，加測） | P1 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| TOOL-010 | 刪除工具（缺 id） | P1 | 400 | `{"code":400,"message":"欄位 id 不可為空值","data":null}` | ✅ |
| TOOL-011 | 儲存工具設定（含 sensitive apiKey） | P1 | 200 | `settingId":"ba9a8da7-…"`；**DB 落地逐值加密**：`{"apiKey": "v2$pGFAkoUdGiAutU61XiBP7g==$JEY9SRZpJqQ53LLd$…", "timeout": 60000}` — apiKey 為 v2 密文、timeout 保持明文。⚠️ **但回應原樣回顯明文 apiKey**（與 LLM SETTING save 的遮罩行為不一致）→ 見問題追蹤 #4 | ✅ |
| TOOL-012 | 儲存工具設定（缺 alias） | P2 | 400 | `{"code":400,"message":"欄位 alias 不可為空值","data":null}` | ✅ |
| TOOL-013 | 更新工具設定（`apiKey` 傳 `__SECRET_KEPT__`） | P1 | 200 | 回 `{"apiKey":"__SECRET_KEPT__","timeout":90000}`；**DB 密文逐字元完全未變**（連 salt 相同＝直接沿用既有密文，非重新加密），`timeout` 已更新為 90000；解密回 `tvly-APITEST-PLAINTEXT-…`（＝原明文）→ **沿用正確** | ✅ |
| TOOL-014 | 更新工具設定（缺 settingContent） | P2 | 400 | `{"code":400,"message":"欄位 settingContent 不可為空值","data":null}` | ✅ |
| TOOL-015 | 新增分類（admin 正常） | P1 | 200 | `{"code":200,…,"data":{"groupId":"990e949e-…","group":"APITEST_GROUP_202609122031",…}}` | ✅ |
| TOOL-016 | 新增分類（缺 group） | P2 | 400 | `{"code":400,"message":"欄位 group 不可為空值","data":null}` | ✅ |
| TOOL-017 | 更新分類（admin 正常） | P1 | 200 | `{"code":200,…,"data":{"groupId":"990e949e-…","group":"APITEST_GRP_UPDATED","groupDescription":"更新後描述"}}`。⚠️ 首次以 34 字元名稱送出回 400 `Database processing error`，日誌為 `PSQLException: value too long for type character varying(30)` → **缺長度驗證、錯誤訊息不具體**（見問題追蹤 #5）；改用 19 字元後通過 | ✅ |
| TOOL-018 | 刪除分類（admin 正常） | P1 | 200 | `{"code":200,…,"data":{"groupId":"990e949e-…",…}}` | ✅ |
| TOOL-019 | 業務邏輯（register → list → delete → list） | P1 | 200 | 註冊後清單有 1 筆（`group=APITEST_GRP_UPDATED`）；刪除後清單 **0 筆** | ✅ |

<details>
<summary>TOOL curl 記錄</summary>

```bash
TAVILY=f95fda5f-4632-4a1a-9a21-2d4facbd4279
# TOOL-001 / 002（公開）
curl -s "$BASE/llm/tool/list"
curl -s -X POST "$BASE/llm/tool/get" -H "Content-Type: application/json" -d "{\"id\":\"$TAVILY\"}"
# TOOL-005 / 009（⚠️ 註冊需先有 groupId）
curl -s -X POST "$BASE/llm/tool/register" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"ApiTestTool202609122031","classPath":"tw.zipe.bastpartner.tool.DateTool","groupId":"990e949e-…","type":"BUILT_IN","description":"API 回歸測試工具"}'
curl -s -X POST "$BASE/llm/tool/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"05fb0824-…"}'
# TOOL-011 / 013（⚠️ settingContent 為 JSON 「字串」，非物件）
curl -s -X POST "$BASE/llm/tool/saveSetting" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"f95fda5f-…","alias":"apitest-tavily-202609122031","settingContent":"{\"apiKey\":\"<測試用假金鑰>\",\"timeout\":60000}"}'
curl -s -X POST "$BASE/llm/tool/updateSetting" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"settingId":"ba9a8da7-…","settingContent":"{\"apiKey\":\"__SECRET_KEPT__\",\"timeout\":90000}"}'
# 落地與沿用驗證
docker exec postgres-db psql -U pguser -d pgdb -t -A -c "SELECT setting_content FROM bestpartner.llm_tool_user_setting WHERE id='ba9a8da7-…';"
# TOOL-015 / 017 / 018（分類）
curl -s -X POST "$BASE/llm/tool/category/save" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"group":"APITEST_GROUP_202609122031","groupDescription":"API 回歸測試分類"}'
curl -s -X POST "$BASE/llm/tool/category/update" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"groupId":"990e949e-…","group":"APITEST_GRP_UPDATED","groupDescription":"更新後描述"}'
curl -s -X POST "$BASE/llm/tool/category/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"groupId":"990e949e-…"}'
```

</details>

### MCP — MCP Server 模組（`/llm/mcpServer`）

> 🔑 **本模組為本次發版前回歸的第二重點**：commit `5347f8f`（updateSetting 欄位不一致＋擁有權檢核）
> 與 `fd8e2e0`（deleteSetting 擁有權檢核）自 2026-07-19 後未經 API 回歸。
> 另驗證 `llm_mcp_user_setting.setting_content` 的**整欄** AES-GCM 加密（`McpSettingEncryptConverter`）。
> 測試資料：MCP `apitest_mcp_202609122031_upd`（`ffa3e02a-…`）與 userSetting `216810da-…`
> 皆已於 MCP-010 / MCP-017 刪除。

| 測試 ID | 場景 | 優先 | HTTP | Response | 狀態 |
|---------|------|:---:|:----:|----------|:---:|
| MCP-001 | 列出 MCP（已認證） | P0 | 200 | 回 4 筆（date / filesystem / gmail / google_map） | ✅ |
| MCP-002 | 列出 MCP（未認證） | P0 | 401 | （空 body） | ✅ |
| MCP-003 | 取得 MCP（正常） | P1 | 200 | 回 date MCP 完整定義（`args:["-jar","D:/MCP/date-1.0-SNAPSHOT.jar"]`, `type:"STDIO"`） | ✅ |
| MCP-004 | 取得 MCP（mcpId 不存在） | P1 | 400 | `{"code":400,"message":"MCP server not found","data":null}` | ✅ |
| MCP-005 | 註冊 MCP（admin 正常） | P1 | 200 | `{"code":200,…,"data":{…,"mcpId":"ffa3e02a-60ea-46ce-94da-c6b479b7fa3e"}}` | ✅ |
| MCP-006 | 註冊 MCP（一般用戶） | P1 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| MCP-007 | 註冊 MCP（缺必填） | P1 | 400 | `{"code":400,"message":"欄位 name 不可為空值\n欄位 command 不可為空值\n欄位 args 不可為空值","data":null}` | ✅ |
| MCP-008 | 更新 MCP（admin 正常） | P1 | 200 | 回 `"name":"apitest_mcp_202609122031_upd"`、`"description":"更新後描述"` | ✅ |
| MCP-009 | 更新 MCP（一般用戶） | P1 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| MCP-010 | 刪除 MCP（admin 正常） | P1 | 200 | `{"code":200,"message":"success","data":true}` | ✅ |
| MCP-011 | 刪除 MCP（一般用戶） | P1 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| MCP-012 | 儲存使用者設定（正常） | P1 | 200 | `{"code":200,…,"data":{"alias":"api-test-202609122031-date","userSettingId":"216810da-84f4-48e4-b5e1-f46ad4c39808","settingContent":{"timezone":"Asia/Taipei"},…}}` | ✅ |
| MCP-013 | 儲存使用者設定（缺 mcpId） | P1 | 400 | `{"code":400,"message":"欄位 mcpId 不可為空值","data":null}` | ✅ |
| MCP-014 | 取得使用者設定（正常） | P1 | 200 | google_map 設定回 `"settingContent":{"GOOGLE_MAPS_API_KEY":"__SECRET_KEPT__"}` — **env 值正確遮罩** | ✅ |
| MCP-015 | 取得使用者設定（id 不存在） | P2 | 400 | `{"code":400,"message":"User setting not found","data":null}` | ✅ |
| MCP-016 | 更新使用者設定（正常） | P1 | 200 | 回 `"settingContent":{"timezone":"Asia/Tokyo"}`；再查確認已生效；**DB 落地為整欄密文** `v2$oqkjv2WXShKSLnN4tA1BCQ==$mZMhajHOVUWdhaOR$TuC8P…`（非 JSON 文字） | ✅ |
| MCP-017 | 刪除使用者設定（正常） | P1 | 200 | `{"code":200,"message":"success","data":true}` | ✅ |
| MCP-018 | 未認證取得清單 | P2 | 401 | （空 body，同 MCP-002） | ✅ |
| MCP-019 | 業務邏輯（saveSetting → customAssistantChat → deleteSetting） | P1 | 200 | 見 **CHAT-010b**：以 google_map userSetting 掛入 `customAssistantChat`，模型實際呼叫 `searchPlaces` 取得真實資料（`渣打銀行 信義分行` / `ChIJ_1dgl7qrQjQRLnEKiYk2w5o`）→ 整欄密文解密後正確注入 env。date MCP 版本因 thin jar 無法啟動（見 CHAT-010） | ✅ |

#### 🔑 擁有權檢核專項驗證（commit `5347f8f` / `fd8e2e0` 回歸）

> 以 `user@bestpartner.com.tw` 的 token 操作 **admin 名下**的 userSetting `216810da-…`：

| 加測 ID | 端點 | HTTP | Response | 判定 |
|---------|------|:----:|----------|:---:|
| MCP-EX1 | `getSetting` | 400 | `{"code":400,"message":"User setting not found","data":null}` | ✅ |
| MCP-EX2 | `updateSetting` | 400 | `{"code":400,"message":"User setting not found","data":null}` | ✅ |
| MCP-EX3 | `deleteSetting` | 400 | `{"code":400,"message":"User setting not found","data":null}` | ✅ |
| — | 竄改後驗證 | 200 | admin 再讀自己的設定，`settingContent` 仍為 `{"timezone":"Asia/Taipei"}` **未被竄改** | ✅ |

> ✅ **三個端點的回應與「設定不存在」（MCP-015）逐字元相同**，未洩漏「設定存在但無權限」的資訊，
> 與 `.claude/rules/api-endpoints.md` 的規格一致。

<details>
<summary>MCP curl 記錄</summary>

```bash
DATE_MCP=4b9ba306-2fc5-4aa6-9e54-22bce834e021
# MCP-005 / 008 / 010（⚠️ command/args 在頂層，非巢狀 commandSetting）
curl -s -X POST "$BASE/llm/mcpServer/register" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"apitest_mcp_202609122031","type":"STDIO","command":"java","args":["-jar","D:/MCP/date-1.0-SNAPSHOT.jar"],"description":"API 回歸測試 MCP"}'
curl -s -X POST "$BASE/llm/mcpServer/update" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"mcpId":"ffa3e02a-…","name":"apitest_mcp_202609122031_upd","type":"STDIO","command":"java","args":["-jar","D:/MCP/date-1.0-SNAPSHOT.jar"],"description":"更新後描述"}'
curl -s -X POST "$BASE/llm/mcpServer/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"mcpId":"ffa3e02a-…"}'
# MCP-012 / 014 / 016 / 017
curl -s -X POST "$BASE/llm/mcpServer/saveSetting" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"mcpId":"4b9ba306-…","alias":"api-test-202609122031-date","settingContent":{"timezone":"Asia/Taipei"}}'
curl -s -X POST "$BASE/llm/mcpServer/getSetting" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"userSettingId":"1e575cec-…"}'
curl -s -X POST "$BASE/llm/mcpServer/updateSetting" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"userSettingId":"216810da-…","settingContent":{"timezone":"Asia/Tokyo"}}'
curl -s -X DELETE "$BASE/llm/mcpServer/deleteSetting" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"userSettingId":"216810da-…"}'
# MCP-EX1 / EX2 / EX3（擁有權：user token 操作 admin 的設定）
curl -s -X POST "$BASE/llm/mcpServer/getSetting"    -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"userSettingId":"216810da-…"}'
curl -s -X POST "$BASE/llm/mcpServer/updateSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"userSettingId":"216810da-…","settingContent":{"timezone":"HACKED"}}'
curl -s -X DELETE "$BASE/llm/mcpServer/deleteSetting" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"userSettingId":"216810da-…"}'
# 整欄加密驗證
docker exec postgres-db psql -U pguser -d pgdb -t -A -c "SELECT left(setting_content,50) FROM bestpartner.llm_mcp_user_setting WHERE id='216810da-…';"
```

</details>

### PERMISSION — 權限模組（`/llm/permission`）

> 測試資料：權限 `apitest-perm-upd`（`22e6fdc0-…`）已於 PERM-009 刪除。

| 測試 ID | 場景 | 優先 | HTTP | Response | 狀態 |
|---------|------|:---:|:----:|----------|:---:|
| PERM-001 | 新增權限（admin 正常） | P1 | 200 | `{"code":200,…,"data":{"id":"22e6fdc0-8dca-4733-b025-05c6e9b96a2b","name":"apitest-perm-202609122031","num":9901,"description":null}}` | ✅ |
| PERM-002 | 新增權限（未認證） | P1 | 401 | （空 body） | ✅ |
| PERM-003 | 新增權限（缺 name） | P1 | 400 | `{"code":400,"message":"欄位 name 不可為空值","data":null}` | ✅ |
| PERM-004 | 新增權限（缺 num） | P1 | 400 | `{"code":400,"message":"欄位 num 不可為空值","data":null}` | ✅ |
| PERM-005 | 新增權限（非 admin） | P2 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| PERM-006 | 更新權限（admin 正常） | P1 | 200 | `{"code":200,…,"data":{"id":"22e6fdc0-…","name":"apitest-perm-upd","num":9911,…}}` | ✅ |
| PERM-007 | 更新權限（缺 id） | P1 | 400 | `{"code":400,"message":"欄位 id 不可為空值","data":null}` | ✅ |
| PERM-008 | 更新權限（非 admin） | P2 | 403 | `{"code":403,"message":"權限錯誤","data":null}` | ✅ |
| PERM-009 | 刪除權限（admin 正常） | P1 | 200 | `{"code":200,"message":"success","data":true}` | ✅ |
| PERM-010 | 刪除權限（缺 id） | P1 | 400 | `{"code":400,"message":"欄位 id 不可為空值","data":null}` | ✅ |
| PERM-011 | 刪除權限（未認證） | P1 | 401 | （空 body） | ✅ |

<details>
<summary>PERMISSION curl 記錄</summary>

```bash
curl -s -X POST "$BASE/llm/permission/add"    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"name":"apitest-perm-202609122031","num":"9901"}'
curl -s -X POST "$BASE/llm/permission/update" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"22e6fdc0-…","name":"apitest-perm-upd","num":"9911"}'
curl -s -X DELETE "$BASE/llm/permission/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"22e6fdc0-…"}'
# 權限邊界（非 admin / 未認證）
curl -s -X POST "$BASE/llm/permission/add"    -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"name":"should_fail","num":"9903"}'
curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/llm/permission/add" -H "Content-Type: application/json" -d '{"name":"x","num":"1"}'
```

</details>

### SYSTEM SETTING — 系統設定模組（`/systemSetting`）

> ⚠️ **欄位命名**：資料表為 `bestpartner.system_setting`，欄位是 `setting_key` / `setting_value`
> （非 `key` / `value`；API 層才用 `key` / `value`）。查 DB 時容易踩空。
> 測試資料：`apitest_key_202609122031`（id 3）已於 SYSSET-008 刪除，設定表還原為原始 2 筆。

| 測試 ID | 場景 | 優先 | HTTP | Response | 狀態 |
|---------|------|:---:|:----:|----------|:---:|
| SYSSET-001 | 列出設定（公開） | P0 | 200 | 2 筆：`skill.global.dir`、`default_llm_platform=OPENAI` | ✅ |
| SYSSET-002 | 取得設定（正常） | P1 | 200 | `{"code":200,"message":"success","data":"OPENAI"}` | ✅ |
| SYSSET-003 | 取得設定（key 不存在） | P2 | 200 | `{"code":200,"message":"success","data":""}` — 回空字串，符合模板預期 | ✅ |
| SYSSET-004 | 新增設定（正常） | P1 | 200 | `{"code":200,…,"data":{"id":null,"key":"apitest_key_202609122031","value":"apitest_value",…}}`（回應 `id` 為 null，實際 DB id=3） | ✅ |
| SYSSET-005 | 新增設定（缺 value） | P1 | 400 | `{"code":400,"message":"欄位 value 不可為空值","data":null}` | ✅ |
| SYSSET-006 | 更新設定（正常） | P1 | 200 | `{"code":200,…,"data":{"id":3,"key":"apitest_key_202609122031","value":"apitest_value_updated","description":"更新後"}}`；再 get 確認回 `apitest_value_updated` | ✅ |
| SYSSET-007 | 更新設定（缺 id） | P1 | **500** | `{"code":500,"message":"Internal server error","data":null}` — **模板記載的已知缺陷仍存在**（應為 400） | ✅（如實記錄現況） |
| SYSSET-008 | 刪除設定（正常） | P1 | 200 | `{"code":200,"message":"success","data":1}`；再 get 回空字串 | ✅ |
| SYSSET-009 | 刪除設定（id 不存在） | P2 | 200 | `{"code":200,"message":"success","data":0}` — 回 0 筆，符合預期 | ✅ |
| SYSSET-010 | 公開端點確認（list 無需認證） | P1 | 200 | 未帶 token 正常回應 | ✅ |
| SYSSET-011 | 公開端點確認（get 無需認證） | P1 | 200 | 未帶 token 正常回應 | ✅ |
| SYSSET-012 | 公開端點確認（add 無需認證） | P1 | 200 | 未帶 token 即可新增 — ⚠️ **任何人可寫入系統設定**（模板已註記安全性疑慮，見問題追蹤 #6） | ✅ |
| SYSSET-013 | 業務邏輯（add → update → delete） | P1 | 200 | 全程驗證：新增回 `apitest_value` → 更新後 get 回 `apitest_value_updated` → 刪除後 get 回 `""` | ✅ |

<details>
<summary>SYSTEM SETTING curl 記錄</summary>

```bash
curl -s "$BASE/systemSetting/list"
curl -s -X POST "$BASE/systemSetting/get"    -H "Content-Type: application/json" -d '{"key":"default_llm_platform"}'
curl -s -X POST "$BASE/systemSetting/get"    -H "Content-Type: application/json" -d '{"key":"no_such_key_202609122031"}'
curl -s -X POST "$BASE/systemSetting/add"    -H "Content-Type: application/json" -d '{"key":"apitest_key_202609122031","value":"apitest_value","description":"API 回歸測試設定"}'
curl -s -X POST "$BASE/systemSetting/add"    -H "Content-Type: application/json" -d '{"key":"incomplete_setting_202609122031"}'
curl -s -X POST "$BASE/systemSetting/update" -H "Content-Type: application/json" -d '{"id":3,"key":"apitest_key_202609122031","value":"apitest_value_updated","description":"更新後"}'
curl -s -X POST "$BASE/systemSetting/update" -H "Content-Type: application/json" -d '{"key":"apitest_key_202609122031","value":"x"}'   # → 500（已知缺陷）
curl -s -X DELETE "$BASE/systemSetting/delete" -H "Content-Type: application/json" -d '{"id":3,"key":"apitest_key_202609122031"}'
curl -s -X DELETE "$BASE/systemSetting/delete" -H "Content-Type: application/json" -d '{"id":999999,"key":"no_such_key"}'
# ⚠️ 查 DB 時欄位為 setting_key / setting_value
docker exec postgres-db psql -U pguser -d pgdb -t -A -c "SELECT id, setting_key, setting_value FROM bestpartner.system_setting;"
```

</details>

### INTEGRATION — 跨模組整合測試

| 測試 ID | 場景 | 優先 | 結果 | 狀態 |
|---------|------|:---:|------|:---:|
| INT-001 | 完整聊天流程（login → setting/get → chat） | P0 | ① login 200 取得 JWT ② `setting/get` 取出 CHAT+OPENROUTER 的 `583b9222-…` ③ `chat` 回 `{"code":200,"message":"success","data":"INT-001-OK"}` | ✅ |
| INT-002 | 向量知識庫輔助聊天（RAG 掛載，真實 Milvus） | P1 | `customAssistantChat` 帶 `knowledgeMounts:[{knowledgeId:"90c4deb7-…",topK:3}]`，回覆**正確引用文件原文**：「立約人應為具中華民國國籍且未受監護宣告或輔助宣告之**年滿二十歲**自然人」→ 依文件（20 歲）而非常識（18 歲），RAG 確實生效 | ✅ |
| INT-003 | MCP 整合聊天（saveSetting → customAssistantChat） | P1 | 見 **CHAT-010b**：google_map userSetting 掛入後模型實際呼叫 `searchPlaces`，回真實資料（`渣打銀行 信義分行` / `ChIJ_1dgl7qrQjQRLnEKiYk2w5o` / `臺北市信義區松仁路97號2樓`） | ✅ |
| INT-004 | 用戶生命週期（register → login → get → update → delete） | P1 | ① register → `5fea1c7f-…` ② login 取得 token ③ get 200（`password:null`、`role:{roleNum:"1",roleName:"USER"}`）④ update 200（email 改為 `…-upd@test.com`）⑤ delete 200 `data:true` | ✅ |
| INT-005 | JWT 過期保護 | P1 | 以 `exp` 已過期的 token 呼叫 `/llm/chat` → 401 `Invalid or malformed token, please log in again` | ✅ |
| INT-006 | admin 工作流（platform/add → setting/save → cleanup） | P1 | ① platform/add → `a5e303dd-…` ② setting/save → `d6e6986f-…` ③ setting/delete 200 ④ platform/delete 200 — **測試資料已全數清除** | ✅ |

### SECURITY — 通用安全性測試

| 測試 ID | 場景 | 優先 | HTTP | Response | 狀態 |
|---------|------|:---:|:----:|----------|:---:|
| SEC-001 | SQL Injection | P1 | 401 | email 注入 `' OR '1'='1` → `{"code":401,"message":"帳號不存在"}`；password 注入 → `{"code":401,"message":"密碼錯誤"}` — **未登入成功，JPA 參數化查詢有效** | ✅ |
| SEC-002 | XSS Payload | P2 | 200 | `username:"<script>alert(1)</script>"` 原樣接受（API 層不轉義，前端責任）。測試帳號 `b530f6de-…` **已刪除** | ✅ |
| SEC-003 | 過期 JWT | P1 | 401 | `{"code":401,"message":"Invalid or malformed token, please log in again","data":null}` | ✅ |
| SEC-004 | **竄改 JWT Payload（原 P0 Release Blocker）** | P1 | 401 | 取 `user` 的合法 token，把 `groups` 從 `["user-write","user-read"]` 竄改為 `["admin","user-read","user-write"]`、**簽章保持原樣**：<br>· 呼叫 admin 端點 `/llm/permission/add` → **401** `Invalid or malformed token`<br>· 呼叫 `/login/check`（refresh 路徑）→ **401**，**未換到任何有效 token**<br>→ **漏洞已修復並實測確認**（`JwtFilter` 進 refresh 前先驗 `isTokenSignatureValid`，commit `def8f7b`） | ✅ |
| SEC-005 | 水平越權 | P1 | 400 | `user` 讀 admin 的 MCP userSetting → `{"code":400,"message":"User setting not found"}`（與「不存在」同樣回應）。另見 MCP-EX1/EX2/EX3 與 CHAT-EX1 | ✅ |
| SEC-006 | 大量請求壓力 | P2 | 200×10 | 連續 10 次 `/llm/setting/get` **全部 200** → **無 rate limiting**（與模板記載一致，屬已知現況） | ✅ |

<details>
<summary>INTEGRATION／SECURITY curl 記錄</summary>

```bash
# INT-002（RAG 掛載）
curl -s -X POST "$BASE/llm/customAssistantChat" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"llmId":"583b9222-…","message":"依文件規定，開立數位存款帳戶的立約人年齡條件為何？請引用條款原文。","promptContent":"You are helpful. Answer strictly from the provided context.","memory":{"id":"int002-202609122031"},"knowledgeMounts":[{"knowledgeId":"90c4deb7-…","topK":3}]}'
# SEC-001（SQL injection）
curl -s -X POST "$BASE/login/" -H "Content-Type: application/json" -d '{"email":"admin@bestpartner.com.tw'"'"' OR '"'"'1'"'"'='"'"'1","password":"anything"}'
# SEC-004（竄改 payload，保留原簽章）
FORGED=$(node -e 'const t=process.argv[1].split(".");const p=JSON.parse(Buffer.from(t[1],"base64url").toString());
  p.groups=["admin","user-read","user-write"];
  console.log([t[0],Buffer.from(JSON.stringify(p)).toString("base64url"),t[2]].join("."))' "$USER_TOKEN")
curl -s -X POST "$BASE/llm/permission/add" -H "Authorization: Bearer $FORGED" -H "Content-Type: application/json" -d '{"name":"forged-perm","num":"9999"}'
curl -s -X POST "$BASE/login/check" -H "Authorization: Bearer $FORGED" -H "Content-Type: application/json" -d '{}'
# SEC-003 / INT-005（過期 token）
EXPIRED=$(node -e 'const t=process.argv[1].split(".");const p=JSON.parse(Buffer.from(t[1],"base64url").toString());
  p.exp=Math.floor(Date.now()/1000)-3600;
  console.log([t[0],Buffer.from(JSON.stringify(p)).toString("base64url"),t[2]].join("."))' "$ADMIN_TOKEN")
curl -s -X POST "$BASE/login/check" -H "Authorization: Bearer $EXPIRED" -H "Content-Type: application/json" -d '{}'
# SEC-006（10 次連續請求）
for i in $(seq 1 10); do curl -s -o /dev/null -w '%{http_code} ' -X POST "$BASE/llm/setting/get" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{}'; done
```

</details>

---

## 補充模組：WORKFLOW 與 SKILL（模板未涵蓋）

> ⚠️ **模板缺口**：`test-confirmation-checklist.md` 只列 12 個模組（154 案例），
> 但 `.claude/rules/api-endpoints.md` 記載共 **13 個模組**——缺 **WORKFLOW** 與 **SKILL**。
> 而 workflow 正是 2026-07-19 以來變更最密集之處（多觸發點、PROMPT 節點、知識庫外掛、
> `secretHeaders` 加密），**不測等於本次回歸的最大空白**。
> 故依 `docs/api-test-plan.md` 的 WORKFLOW 章節補測；模板應補上這兩個模組（見問題追蹤 #7）。

### WORKFLOW — `/llm/workflow`

> 測試資料：workflow `apitest-wf-202609122031`（`8da3de2b-…`），圖形為
> **t1→promptA→llm1→out1** 與 **t2→promptB→llm1**（雙觸發點匯流到同一顆 LLM）。

| 案例 | 優先 | HTTP | 結果 | 狀態 |
|------|:---:|:----:|------|:---:|
| create 建立空白 workflow | P0 | 200 | `{"id":"8da3de2b-…","status":"DRAFT","version":1,"nodes":[],"edges":[]}` | ✅ |
| nodeRequiredFields 取必填清單 | P2 | 200 | **13 種 NodeType 齊全**，且與規格一致：`LLM_ASSISTANT:["llmId"]`、`PROMPT:["prompt"]`、`DATA_TRANSFORM:["mappings\|template"]`、`OUTPUT:["template\|mappings"]`、`MCP_SERVER:["mcpId","toolName"]` | ✅ |
| save 合法 nodes/edges 整張覆寫 | P0 | 200 | version 1→2，6 節點 5 edge 全數還原。⚠️ **DTO 欄位名易踩雷**：節點是 `type`（非 `nodeType`）、edge **無** `edgeKey` 欄位（見下方 curl 記錄） | ✅ |
| save 節點 config 含未知欄位 | P1 | 400 | `{"code":400,"message":"Config of node t1 is invalid: Encountered an unknown key 'bogusField'.…"}` — **訊息含 nodeKey**，符合規格 | ✅ |
| save 樂觀鎖（version 不符） | P1 | 400 | `{"code":400,"message":"Workflow has been modified by another session, please reload","data":null}` | ✅ |
| list 列出當前使用者的 workflow | — | 200 | 6 筆 | ✅ |
| 非擁有者存取（user token） | P2 | 400 | `{"code":400,"message":"No permission to access this workflow","data":null}` | ✅ |
| **execute：指定 `triggerNodeKey="t1"`** | P0 | 200 | SSE：`execution.started` → t1 → promptA → llm1 → out1 → `execution.completed(SUCCESS)`，**完全無 t2／promptB 事件**；LLM 回覆 `BRANCH-A`（對應 promptA 的提問）；最終輸出 `{"result":"BRANCH-A"}` | ✅ |
| **執行紀錄記錄發起入口** | P1 | — | `llm_workflow_execution.trigger_node_key` = **`t1`**；`llm_workflow_node_execution`：t1/promptA/llm1/out1 皆 SUCCESS，**t2 與 promptB 各一筆 SKIPPED** | ✅ |
| execute 帶不存在的 `triggerNodeKey` | P1 | 400 | `{"code":400,"message":"Specified trigger node not found: nosuch","data":null}` — 訊息含 nodeKey，**未建立執行紀錄** | ✅ |
| execute 帶非 TRIGGER 型別的 nodeKey | P1 | 400 | `{"code":400,"message":"Specified node \"llm1\" is not a TRIGGER node (actual type: LLM_ASSISTANT)","data":null}` — 含 nodeKey 與實際型別 | ✅ |
| execute 帶不存在的 workflow id | P1 | 400 | `{"code":400,"message":"Workflow not found","data":null}` | ✅ |
| execute 未帶 token | P1 | 401 | （空 body） | ✅ |

#### 🔑 LLM 回覆完整性把關（本次新增修正的驗收）

> 對應 `docs/api-test-plan.md` 的「LLM 回覆完整性」案例。**這是本次程式修正的核心驗收**。

| 步驟 | 作法 | 實測結果 |
|------|------|---------|
| ① 正常結束（`STOP`）不受影響 | 以預設 `maxTokens=32768` 執行 | 後端日誌：`LLM 節點 [llm1] 推論結束：finishReason=STOP, tokenUsage=OpenAiTokenUsage { inputTokenCount = 18, …, outputTokenCount = 5, totalTokenCount = 23 }`；節點 **SUCCESS** → **可觀測性已補上，且不影響既有流程** ✅ |
| ② 強制截斷（`LENGTH`） | 暫時把 `583b9222` 的 `maxTokens` 調為 **5**（金鑰以 `__SECRET_KEPT__` 沿用），提示改為「請詳細說明台灣的地理環境與氣候特徵，至少三百字」 | 日誌 `finishReason=LENGTH`；SSE 發出 **`node.failed`**：`"error":"LLM response was cut off before completion (finish reason: LENGTH); the answer may be truncated"`；`execution.completed` 狀態 **FAILED** ✅ |
| ③ 下游連鎖 | 同上 | `llm_workflow_node_execution`：llm1 **FAILED**（`error_message` 帶 i18n 訊息）、下游 **out1 SKIPPED**；未選中的 t2/promptB 仍 SKIPPED ✅ |
| ④ 還原 | 把 `maxTokens` 改回 **32768** | `/llm/chat` 回 `RESTORED-OK`，設定完全還原 ✅ |

> **修正前的行為**：同樣情境下 llm1 會是 **SUCCESS**、`error_message` 為 null、後端無任何 WARN，
> 使用者拿到被截斷的答案卻無從察覺（e2e 週期 202608252128 問題追蹤 #1 實測 7 次取樣有 4 次截斷）。
> **修正後**：截斷變成明確的 FAILED 並附原因，且 `STOP` / `null` 不受影響。

### SKILL — `/llm/skill`

> 測試資料：skill `apitest-skill-202609122031`（`ef7fe292-…`）已於本模組末刪除。

| 案例 | HTTP | 結果 | 狀態 |
|------|:----:|------|:---:|
| list（已認證） | 200 | 回 admin 的 skill 清單（既有 `pdf` skill，`isGlobal:false`） | ✅ |
| list（未認證） | 401 | （空 body） | ✅ |
| upload（個人 zip） | 200 | `{"id":"ef7fe292-…","name":"apitest-skill-202609122031","dirPath":"D:\tmp\bestpartner\upload\skills\c88f57c8-…\apitest-skill-202609122031","isGlobal":false}` | ✅ |
| get（含 resources） | 200 | `content` 為 `skill.md` 全文；`resources` 含 `reference.txt`（`relativePath` + `content` 皆正確還原） | ✅ |
| **擁有權**：user 讀 admin 的 skill | 400 | `{"code":400,"message":"Skill not found","data":null}` — 不洩漏存在性 | ✅ |
| **擁有權**：user 刪除 admin 的 skill | 400 | `{"code":400,"message":"Skill not found","data":null}` | ✅ |
| global/upload（非 admin） | 403 | （空 body） | ✅ |
| delete（擁有者） | 200 | `{"code":200,"message":"success","data":"Data deleted successfully"}`；清單確認 0 筆 | ✅ |

<details>
<summary>WORKFLOW／SKILL curl 記錄</summary>

```bash
# create
curl -s -X POST "$BASE/llm/workflow/create" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"apitest-wf-202609122031","description":"API 回歸測試 workflow"}'
# save（⚠️ 節點用 "type" 不是 "nodeType"；edge 沒有 "edgeKey" 欄位）
curl -s -X POST "$BASE/llm/workflow/save" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{
  "id":"8da3de2b-…","name":"apitest-wf-202609122031","version":1,
  "nodes":[
    {"nodeKey":"t1","type":"TRIGGER","name":"觸發點A","config":{"triggerType":"MANUAL"},"positionX":0,"positionY":0},
    {"nodeKey":"t2","type":"TRIGGER","name":"觸發點B","config":{"triggerType":"MANUAL"},"positionX":0,"positionY":200},
    {"nodeKey":"promptA","type":"PROMPT","config":{"prompt":"Reply with exactly: BRANCH-A"},"positionX":250,"positionY":0},
    {"nodeKey":"promptB","type":"PROMPT","config":{"prompt":"Reply with exactly: BRANCH-B"},"positionX":250,"positionY":200},
    {"nodeKey":"llm1","type":"LLM_ASSISTANT","config":{"llmId":"583b9222-…"},"positionX":500,"positionY":100},
    {"nodeKey":"out1","type":"OUTPUT","config":{"template":"{{llm1.reply}}"},"positionX":750,"positionY":100}],
  "edges":[
    {"sourceNodeKey":"t1","targetNodeKey":"promptA","sourceHandle":"out:main","targetHandle":"in:main"},
    {"sourceNodeKey":"t2","targetNodeKey":"promptB","sourceHandle":"out:main","targetHandle":"in:main"},
    {"sourceNodeKey":"promptA","targetNodeKey":"llm1","sourceHandle":"out:main","targetHandle":"in:prompt"},
    {"sourceNodeKey":"promptB","targetNodeKey":"llm1","sourceHandle":"out:main","targetHandle":"in:prompt"},
    {"sourceNodeKey":"llm1","targetNodeKey":"out1","sourceHandle":"out:main","targetHandle":"in:main"}]}'
# execute（指定觸發點，SSE）
curl -s -N -X POST "$BASE/llm/workflow/execute" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"8da3de2b-…","triggerNodeKey":"t1"}'
# DB 查核
docker exec postgres-db psql -U pguser -d pgdb -t -A -c "SELECT trigger_node_key, status FROM bestpartner.llm_workflow_execution WHERE id='…';"
docker exec postgres-db psql -U pguser -d pgdb -t -A -c "SELECT node_key, status, left(error_message,60) FROM bestpartner.llm_workflow_node_execution WHERE execution_id='…' ORDER BY seq_no;"
# finishReason 日誌
grep "推論結束" backend.log
# SKILL
curl -s "$BASE/llm/skill/list" -H "Authorization: Bearer $ADMIN_TOKEN"
curl -s -X POST "$BASE/llm/skill/upload" -H "Authorization: Bearer $ADMIN_TOKEN" -F "file=@apitest-skill-202609122031.zip" -F "description=API 回歸測試 skill"
curl -s -X POST "$BASE/llm/skill/get"    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"ef7fe292-…"}'
curl -s -X POST "$BASE/llm/skill/delete" -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"id":"ef7fe292-…"}'
```

</details>

---

## 問題追蹤區（本次週期發現）

> 本次 **0 個 ❌**。以下為執行過程中發現、值得追蹤的產品／流程議題，皆非測試失敗項。

| # | 類別 | 嚴重度 | 現象 | 佐證 | 建議處置 |
|---|------|:-----:|------|------|---------|
| #1 | 安全（資訊外洩） | **P1** | **`/llm/user/register` 回應原樣回傳明文密碼** | USER-001：`"data":{…,"username":"apitest_202609122031","password":"testpass",…}`。對照 `/llm/user/get` 的 `password` 正確為 `null` | 註冊回應應比照 get 將 `password` 設為 null。密碼會進入前端記憶體、瀏覽器 devtools、以及任何記錄回應的日誌／APM |
| #2 | 環境／資料設定 | P2 | **ADMIN CHAT 兩個 P0 端點無法驗證** | ADCHAT-001/004 回 400 `LLM model not found`。根因：`default_llm_platform` = `OPENAI`，但 admin 名下無 OPENAI 的 **CHAT** 設定，故 `chatModelMap` 為空 | 二擇一：① 把 `default_llm_platform` 改為 `OPENROUTER`（admin 有有效的 `583b9222`）② 為 admin 建立 OPENAI CHAT 設定。**本次未自行變更**（屬使用者共享環境，需裁示）。另建議 `LLMStore` 在 map 為空時記一則 WARN，現況啟動無任何訊號 |
| #3 | 資料完整性 | P2 | **`/llm/setting/platform/add` 允許新增重複平台** | LLMSET-012：`GROK` 已存在仍成功新增，`llm_platform` 出現兩筆 `GROK`（已刪除還原） | 加上 `name` 唯一性約束，或 service 層擋重複。重複平台會讓前端下拉出現同名選項、使用者無法分辨 |
| #4 | 一致性（遮罩） | P2 | **`/llm/tool/saveSetting` 回應回顯明文 apiKey** | TOOL-011 回應含 `"settingContent":"{\"apiKey\":\"tvly-APITEST-…\"…}"`；對照 `/llm/setting/save` 的 apiKey 已遮罩為 `__SECRET_KEPT__` | 統一為遮罩。DB 落地本身**正確加密**（`{"apiKey":"v2$…"}`），純屬回應層不一致 |
| #5 | 可用性（錯誤訊息） | P2 | 超長輸入回籠統的 `Database processing error` | TOOL-017 送 34 字元分類名 → 400 `Database processing error`；日誌才看得到 `PSQLException: value too long for type character varying(30)` | 在 DTO 加 `@Size(max=30)` 之類的長度驗證，回具體欄位與上限 |
| #6 | 安全（存取控制） | P2 | **`/systemSetting/*` 全部公開**，任何人可新增／更新／刪除系統設定 | SYSSET-012：未帶 token 即可 `add` 成功。模板原已註記此疑慮 | `add`/`update`/`delete` 應加 `@RolesAllowed("admin")`；`default_llm_platform` 這類設定被任意改寫會影響全系統 |
| #7 | 測試流程（模板缺口） | P2 | **確認表模板缺 WORKFLOW 與 SKILL 兩個模組** | 模板列 12 模組 154 案例；`api-endpoints.md` 記載 13 模組。workflow 是近期變更最密集處卻無案例 | 補 `modules/test-module-WORKFLOW.md` 與 `test-module-SKILL.md`，並更新 checklist 的模組清單與合計 |
| #9 | API 缺口 | P2 | **工具使用者設定無刪除端點**：`/llm/tool` 有 `saveSetting` / `updateSetting`，但**沒有 `deleteSetting`**（`LLMToolResource.kt` 只有 `/delete` 刪工具本身）。設定一旦建立即無法經 API 移除，只能改 DB | 收尾改名時以 `saveSetting` 送出 → 新增了第二筆（`879934c1-…`）而非更名，且無端點可刪 | 補 `POST /llm/tool/deleteSetting`（比照 MCP 的 `deleteSetting`，含擁有權檢核）。對照：MCP 與 LLM SETTING 都有完整的 CRUD |
| #8 | 文件（過期資訊） | P2 | 模板的「常用已知 ID」與「已知缺陷」多處過期 | ① `CHAT_LLM_ID 583b9222` 標為 test_user 的，實際屬 **admin** ② SEC-004 仍標為未修的 P0 Release Blocker，實際 2026-05-05 已修（本次實測確認）③ ADMIN CHAT 失敗根因已不同（見 #2） | 更新 `test-confirmation-checklist.md`；本確認表已就地更正 |

### 觀察記錄（非缺陷，供後續參考）

| 項目 | 內容 |
|------|------|
| `save` 時傳 `__SECRET_KEPT__` | 新增（非更新）時傳遮罩值會把**字面字串當金鑰加密存入**，該設定後續呼叫必然失敗（實測回 400 `LLM服務處理發生錯誤`）。屬合理行為（新增無既有值可沿用），但前端應避免在新增表單送出遮罩值 |
| JWT refresh 機制 | 測試期間 token 自然到期，`update` 端點回 401 `Credentials expired, new token has been generated` 並**附上新 token** → refresh 機制運作正常（且 SEC-004 證實它不接受偽造 payload） |
| `date` MCP 無法使用 | `D:/MCP/date-1.0-SNAPSHOT.jar` 僅 7 KB 為 **thin jar**（無 Main-Class），啟動即 `Process has exited`。需替換為 uber-jar（`build/*-runner.jar`），同 e2e-test-plan §J15 前置警告 |
| 無 rate limiting | SEC-006 連續 10 次請求全 200，與模板記載一致 |
