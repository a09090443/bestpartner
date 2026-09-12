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
| 測試日期 | |
| 服務版本 | 0.1.8 |
| 測試環境 | dev / sit |
| LLM 平台 | |
| LLM Setting ID | |
| Base URL | `http://localhost:80` |
| **測試資料處置（清理／保留）** | |
| **保留清單（類型／名稱／id／理由）** | |
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
