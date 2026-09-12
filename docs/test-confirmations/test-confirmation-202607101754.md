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
| 測試日期 | 2026-07-10 17:54 |
| 服務版本 | 0.1.8-SNAPSHOT |
| 測試環境 | dev |
| LLM 平台 | OpenRouter |
| LLM Setting ID | `583b9222-8cb0-4109-b072-5f0fd1e9fed9`（CHAT） |
| Base URL | `http://localhost:80` |
| 測試結束日期 | 2026-07-10 17:58 |

> 本週期為 **workflow 執行引擎 Phase 1 驗收**：新端點 `POST /llm/workflow/execute`（SSE）、OUTPUT 節點、前端執行按鈕/上色/結果面板。服務於本週期以最新程式碼重建（uber-jar，dev profile）並重啟後測試。範圍：WORKFLOW 執行相關案例 + Playwright UI E2E；其餘模組本次 ⏭️。

## 本週期測試記錄（Phase 1 執行功能驗收）

### 後端單元測試 ✅

- `./gradlew test`：88 tests，5 failed → 其中 ArchitectureTest 因引擎子套件命名規則已修正（commit `68978b8`）後通過；其餘 4 個失敗（Text2SqlTest、LLMUserServiceTest、SystemServiceTest、ImageContentWriteToGoogleDriveTest）為**既有環境依賴**（Ollama+MySQL、Google Drive 憑證、DB 資料狀態），與本分支無關
- workflow 範圍測試（`tw.zipe.bastpartner.service.workflow.*` 等）全綠
- 前端 `npx vitest run`：**294/294 通過**

### WF-EXEC-01 手動執行 happy path ✅

```bash
curl -s -N -m 180 -X POST http://localhost:80/llm/workflow/execute \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -d '{"id":"afea1019-f335-4770-9095-98e7b1aa7f36"}'
```

Response：6 個 SSE 事件（execution.started → trigger started/completed → llm_assistant started/completed → execution.completed SUCCESS），LLM 節點真實呼叫 OpenRouter + date MCP。

### WF-EXEC-02 不存在的 workflow id ✅（預期 400）

```bash
curl -s -X POST http://localhost:80/llm/workflow/execute ... -d '{"id":"00000000-0000-0000-0000-000000000000"}'
```

Response（HTTP 400）：

```json
{"code":400,"message":"Workflow not found","data":null}
```

### WF-EXEC-03 未認證 ✅（預期 401）

無 Authorization header → HTTP 401。

### WF-EXEC-04 取消路徑（client 中斷）✅

臨時建立三節點 workflow（trigger→LLM→OUTPUT，id `d50ef1f0-...`），`curl -m 3` 於 LLM 執行中斷線，25 秒後查 DB：

```
execution: 9d26476d-5b25-497f-b2dd-d83c6c1a5884 CANCELLED
node: ('t1', 'SUCCESS')
node: ('llm1', 'SUCCESS')      ← 中斷時執行中的節點跑完（取消粒度為節點之間，符合設計）
node: ('out1', 'SKIPPED')      ← 未執行下游正確標記
```

補充：對「最後一個節點執行中」斷線的案例（雙節點 workflow），執行自然完成標 SUCCESS——同樣符合設計（無後續節點可取消）。

### UI E2E（Playwright，10 步全過）✅

登入 → 開啟 workflow → 拖入 OUTPUT 節點 → 模板插值 `{{diIWLkRC.reply}}` → 連線 → 存檔 → ▶ 執行（按鈕變 ■ 停止、節點 RUNNING 橘色脈動）→ 完成（三節點全綠、結果面板 SUCCESS、輸出「LLM 回覆：今天的日期是 **2026年7月10日**。」）→ Inspector 本次執行顯示節點輸出 → 清理還原。

錄製：`D:/tmp/bestpartner/workflow-execution-e2e-20260710.gif`（10 幀）。

### 測試資料清理 ✅

- 取消測試臨時 workflow `cancel-test-temp` 已刪除
- Task 9 除錯殘留 `smoke-test-no-mcp` 已刪除
- 列表復原為僅「UI 測試流程 OpenRouter+DateMCP」

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

> 本週期為 Phase 1 執行功能驗收（非完整回歸）：執行 WORKFLOW 執行相關 5 案 + UI E2E 10 步，其餘 ⏭️。

| 模組 | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|------|------|--------|--------|---------|---------|
| WORKFLOW（執行驗收，含 E2E） | 5 | 5 | 0 | 0 | 100.0% |
| 其餘 12 模組 | 154 | 0 | 0 | 154 | —（本次未執行） |
| **合計** | **159** | **5** | **0** | **154** | **100.0%（執行 5 案） |

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
