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
| 測試日期 | 2026-07-09 23:34 |
| 服務版本 | 0.1.8-SNAPSHOT |
| 測試環境 | dev |
| LLM 平台 | OpenRouter |
| LLM Setting ID | `583b9222-8cb0-4109-b072-5f0fd1e9fed9`（CHAT） |
| Base URL | `http://localhost:80` |
| 測試結束日期 | 2026-07-09 23:56 |

> 本週期為 **MCP 修復重點驗證**：修復 `LLMMcpServerEntity` 缺 `description` 欄位（mcpSettingIds 路徑 UnknownPathException）與 date MCP 註冊 jar 路徑錯誤後之回歸驗證。範圍：MCP 模組相關案例 + CHAT 模組 customAssistantChat MCP 案例；其餘模組本次標記 ⏭️。

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

> 本週期為 MCP 修復重點驗證（非完整回歸）：僅執行 MCP / CHAT 相關案例，其餘標記 ⏭️。
> 執行案例之 Pass 率 = Pass ÷ 實際執行數。

| 模組 | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|------|------|--------|--------|---------|---------|
| AUTH | 10 | 0 | 0 | 10 | —（本次未執行） |
| CHAT | 18 | 4 | 0 | 14 | 100.0%（執行 4 案） |
| ADMIN CHAT | 5 | 0 | 0 | 5 | —（本次未執行） |
| USER | 17 | 0 | 0 | 17 | —（本次未執行） |
| LLM SETTING | 16 | 0 | 0 | 16 | —（本次未執行） |
| VECTOR | 14 | 0 | 0 | 14 | —（本次未執行） |
| TOOL | 19 | 0 | 0 | 19 | —（本次未執行） |
| MCP | 19 | 3 | 0 | 16 | 100.0%（執行 3 案） |
| PERMISSION | 11 | 0 | 0 | 11 | —（本次未執行） |
| SYSTEM SETTING | 13 | 0 | 0 | 13 | —（本次未執行） |
| 跨模組整合 | 6 | 0 | 0 | 6 | —（本次未執行） |
| 通用安全性 | 6 | 0 | 0 | 6 | —（本次未執行） |
| **合計** | **154** | **7** | **0** | **147** | **100.0%（執行 7 案）** |

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

## 本週期測試記錄（MCP 修復重點驗證）

> 修復內容：
> 1. **Bug A**：`LLMMcpServerEntity` 缺 `description` 欄位（DDL 有、entity 無），`LLMMcpUserSettingRepository.findByCondition` JPQL 選取 `lms.description` 直接拋 `UnknownPathException` → `mcpSettingIds` 路徑 100% 失敗且被靜默吞掉
> 2. **Bug B**：`BaseRepository` Tuple→DTO 映射器 `setEnumValue` 不支援 JPQL 回傳的 enum 實例（僅支援 String/Number）→ `type` 設值失敗 → user setting 被誤判為 not found
> 3. **Bug C**：`customAssistantChatStreaming` 的 `finally` 在非同步 `.start()` 返回後立即關閉 MCP clients → 串流中工具呼叫時 MCP 行程已死；改為 `emitter.onTermination` 關閉
> 4. **資料修正**：date MCP 註冊 args 原指向不存在的 `D:/MCP/date-1.0-SNAPSHOT-runner.jar`（jbang），更新為 `java -jar D:/MCP/date.jar`

### MCP-FIX-01 更新 date MCP 註冊（admin）✅

```bash
curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:80/llm/mcpServer/update \
  -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
  "mcpId": "4b9ba306-2fc5-4aa6-9e54-22bce834e021",
  "name": "date",
  "type": "STDIO",
  "command": "D:/tools/openlogic-openjdk-21.0.6+7-windows-x64/bin/java",
  "args": ["-jar", "D:/MCP/date.jar"],
  "description": "日期查詢 MCP（java -jar date.jar）"
}'
```

Response（HTTP 200）：

```json
{"code":200,"message":"success","data":{"name":"date","server":"","command":"D:/tools/openlogic-openjdk-21.0.6+7-windows-x64/bin/java","args":["-jar","D:/MCP/date.jar"],"argsDesc":null,"env":null,"envDesc":null,"type":"STDIO","alias":null,"userSettingId":null,"settingId":null,"settingContent":null,"commandSetting":{"command":"D:/tools/openlogic-openjdk-21.0.6+7-windows-x64/bin/java","args":["-jar","D:/MCP/date.jar"],"argsDesc":null,"env":null,"envDesc":null,"server":null},"description":"日期查詢 MCP（java -jar date.jar）","userId":null,"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"}}
```

### MCP-FIX-02 查詢 date MCP 確認 description 持久化 ✅

```bash
curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:80/llm/mcpServer/get \
  -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"}'
```

Response（HTTP 200）：

```json
{"code":200,"message":"success","data":{"name":"date","server":"","command":"","args":["-jar","D:/MCP/date.jar"],"argsDesc":null,"env":null,"envDesc":null,"type":"STDIO","alias":null,"userSettingId":null,"settingId":null,"settingContent":null,"commandSetting":null,"description":"日期查詢 MCP（java -jar date.jar）","userId":null,"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"}}
```

> `description` 讀得回來，證明 entity 修復後欄位正確持久化（修復前恆為 null）。

### MCP-FIX-03 saveSetting 空 settingContent 驗證 ✅（預期 400）

```bash
curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:80/llm/mcpServer/saveSetting \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -d '{"mcpId": "4b9ba306-2fc5-4aa6-9e54-22bce834e021", "alias": "date_user_setting_test", "settingContent": {}}'
```

Response（HTTP 400，欄位驗證正確擋下）：

```json
{"code":400,"message":"欄位 settingContent 不可為空值","data":null}
```

### CHAT-MCP-01 customAssistantChat + date MCP（mcpIds 全域路徑）✅

```bash
curl -s -w "\nHTTP:%{http_code}\n" -m 180 -X POST http://localhost:80/llm/customAssistantChat \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -d '{
  "llmId": "583b9222-8cb0-4109-b072-5f0fd1e9fed9",
  "memory": {"id": "gary", "maxSize": 10},
  "message": "請問今天的日期，格式:yyyy-MM-dd",
  "promptContent": "你是一位很專業的AI助手，會精準的回應使用者需求",
  "mcpIds": ["4b9ba306-2fc5-4aa6-9e54-22bce834e021"]
}'
```

Response（HTTP 200）：

```json
{"code":200,"message":"success","data":"今天的日期是：**2026-07-09**"}
```

> 日期正確（測試當日），只能來自 date MCP 工具。後端日誌：`已配置 1 個MCP客戶端`。

### CHAT-MCP-02 customAssistantChat + filesystem MCP（mcpSettingIds 使用者設定路徑，Bug A/B 回歸）✅

```bash
curl -s -w "\nHTTP:%{http_code}\n" -m 180 -X POST http://localhost:80/llm/customAssistantChat \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -d '{
  "llmId": "583b9222-8cb0-4109-b072-5f0fd1e9fed9",
  "memory": {"id": "gary", "maxSize": 10},
  "message": "請使用檔案工具列出 D:/MCP 目錄下所有副檔名為 .jar 的檔名",
  "promptContent": "你是一位很專業的AI助手，會精準的回應使用者需求",
  "mcpSettingIds": ["22294230-5111-4296-8ac2-ca4a47eabc74"]
}'
```

Response（HTTP 200）：

```json
{"code":200,"message":"success","data":"現在讓我從目錄列表中篩選出所有 .jar 檔案。根據列表，我找到了以下 .jar 檔案：\n\n1. **basic-tool-1.0.0-SNAPSHOT-runner.jar**\n2. **bitcoin-mcp-server-0.0.1-SNAPSHOT.jar**\n3. **cwa-tw-1.0-SNAPSHOT-runner.jar**\n4. **date.jar**\n5. **excel-1.0-SNAPSHOT-runner.jar**\n6. **filesystem.jar**\n7. **gmail-1.0-SNAPSHOT-runner.jar**\n8. **gmap-0.0.1-SNAPSHOT.jar**\n9. **google-drive-1.0-SNAPSHOT-runner.jar**\n10. **google-map-1.0-SNAPSHOT-runner.jar**\n11. **mcp-example-1.0-SNAPSHOT-runner.jar**\n12. **mcp-server-filesystem-999-SNAPSHOT.jar**\n13. **mcp-servers-1.0-SNAPSHOT-runner.jar**\n14. **quarkus-1.0-SNAPSHOT-runner.jar**\n15. **spring-0.0.1-SNAPSHOT.jar**\n16. **spring-example-0.0.1-SNAPSHOT.jar**\n17. **tw-stock-1.0-SNAPSHOT-runner.jar**\n\n總共找到 **17 個 .jar 檔案**。..."}
```

> 列出的 17 個 jar 與 D:/MCP 實際內容一致，`${path0}`/`${path1}` 佔位符由使用者設定正確代入。此路徑修復前必失敗（UnknownPathException → 靜默降級為無工具）。

### CHAT-MCP-03 date + filesystem 雙 MCP 同時掛載（postman custom_assistant_chat_mcp 格式）✅

```bash
curl -s -w "\nHTTP:%{http_code}\n" -m 180 -X POST http://localhost:80/llm/customAssistantChat \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -d '{
  "llmId": "583b9222-8cb0-4109-b072-5f0fd1e9fed9",
  "memory": {"id": "gary", "maxSize": 10},
  "message": "請問今天日期（yyyy-MM-dd），以及 D:/tmp 目錄下是否有 bestpartner 資料夾？兩個問題都請用工具確認",
  "promptContent": "你是一位很專業的AI助手，會精準的回應使用者需求",
  "mcpIds": ["4b9ba306-2fc5-4aa6-9e54-22bce834e021"],
  "mcpSettingIds": ["22294230-5111-4296-8ac2-ca4a47eabc74"]
}'
```

Response（HTTP 200）：

```json
{"code":200,"message":"success","data":"根據工具查詢結果：\n\n1. **今天日期**：2026-07-09\n\n2. **D:/tmp 目錄下是否有 bestpartner 資料夾**：**有**，在 D:/tmp 目錄下確實存在一個名為 \"bestpartner\" 的資料夾（在列表中顯示為 `[DIR]  bestpartner`）。\n\n總結：\n- 今天日期：2026年7月9日\n- D:/tmp 目錄下存在 bestpartner 資料夾"}
```

### CHAT-MCP-04 customAssistantChatStreaming + date MCP（Bug C 回歸）✅

```bash
curl -s -N -w "\nHTTP:%{http_code}\n" -m 180 -X POST http://localhost:80/llm/customAssistantChatStreaming \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -d '{
  "llmId": "2b44c811-d38c-417b-89d4-ae675b3093f5",
  "memory": {"id": "mcp-stream-fix-test", "maxSize": 10},
  "message": "請問今天的日期，格式:yyyy-MM-dd",
  "promptContent": "你是一位很專業的AI助手，會精準的回應使用者需求",
  "mcpIds": ["4b9ba306-2fc5-4aa6-9e54-22bce834e021"]
}'
```

Response（HTTP 200，SSE 串流合併）：

```
我來幫您查詢今天的日期。今天的日期是：**2026-07-09**
```

> 修復前：MCP 行程在串流開始後 0.09 秒即被關閉，模型回覆「日期工具暫時無法使用」。
> 修復後日誌時序：`串流聊天完成`（23:55:01,801）→ `開始關閉 1 個MCP客戶端`（23:55:01,837）→ `MCP客戶端關閉完成`。

### 測試資料清理

- 前一輪除錯時建立的臨時 MCP `filesystem_uitest_temp`（d6fbe9ce-...）已刪除，`/llm/mcpServer/list` 復原為 5 筆
- MCP-FIX-03 為 400 驗證案例，無資料寫入
- date MCP 註冊更新屬**修正**（原路徑失效），保留不還原

---

## 問題追蹤區

> 測試失敗（❌）的項目需在此詳細記錄，包括 Test ID、現象、期望行為、實際行為

| Test ID | 現象 | 期望行為 | 實際行為 | 根因 | 狀態 |
|---------|------|---------|---------|------|------|
| CHAT-MCP-02（修復前） | mcpSettingIds 掛 MCP 一律無效且無錯誤回應 | 依使用者設定啟動 MCP 並掛入工具 | UnknownPathException 被吞、0 個 MCP client | `LLMMcpServerEntity` 缺 `description` 欄位（Bug A） | ✅ 已修復 |
| CHAT-MCP-02（修復中） | JPQL 可執行但回報 setting not found | 查得使用者設定 | 映射器 `setEnumValue` 不支援 enum 實例 → type=null 誤判 | `BaseRepository` Tuple→DTO 映射缺 enum 直接指派分支（Bug B） | ✅ 已修復 |
| CHAT-MCP-04（修復前） | 串流模式模型稱「工具暫時無法使用」 | 串流期間 MCP 工具可用 | `finally` 過早關閉 MCP client（`.start()` 非同步） | 關閉時機錯誤，改用 `emitter.onTermination`（Bug C） | ✅ 已修復 |

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
