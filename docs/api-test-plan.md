# BestPartner API 測試計畫

## 說明

根據 `docs/postman/basepartner.postman_collection.json` 與 `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/resource/` 目錄中的所有 Resource 類別，整理出完整的 API 測試計畫表。目的是確保各模組的正常流程、邊界條件、認證/授權保護、必填欄位驗證均有測試覆蓋。

---

## 優先順序定義

| 等級 | 說明 |
|------|------|
| P0 | 核心阻斷性功能，失敗即無法繼續測試 |
| P1 | 主要業務功能，直接影響使用者體驗 |
| P2 | 次要功能、邊界條件、錯誤處理細節 |

---

## 測試前置條件

- 測試環境 API Base URL 已設定（預設 port 80）
- 所有需要認證的測試，需先執行 `POST /login/` 取得有效 JWT
- 測試資料需在每個 suite 執行前/後進行清理

### 預設測試帳號

以下帳號由 `docs/sql/bestpartner-init-data.sql` 初始化，可直接使用：

| 帳號 | Email | 密碼 | 角色 | JWT groups（permissions） | 適用場景 |
|-----|-------|------|------|--------------------------|---------|
| `admin` | `admin@bestpartner.com.tw` | `admin` | ADMIN | `admin`, `user-read`, `user-write` | 所有端點（含 `@RolesAllowed("admin")`） |
| `user` | `user@bestpartner.com.tw` | `user` | USER | `user-read`, `user-write` | `@Authenticated` 及 `@RolesAllowed("user-read")` 端點；admin 端點預期 403 |
| `test_user` | `test@partmer.com.tw` | `user` | （無角色） | （空） | 僅用於「無角色帳號存取受保護端點」的負向測試 |

> **已知缺陷 — @RolesAllowed("all")**：permission `"all"` (num=2) 在 `llm_role_permission` 中無任何角色映射，所有預設帳號（含 admin）均不具備此 permission。涉及 `@RolesAllowed("all")` 的端點（`/llm/user/switchStatus`、`/llm/user/delete`）以預設帳號呼叫均預期回傳 403，相關測試案例已標記。

> **已知缺陷 — test_user 無角色**：`test_user` 帳號在資料庫中沒有任何角色，所有需要認證的端點（`@Authenticated`）均預期失敗，僅可用於驗證「無權限帳號被拒絕」的場景。

---

## 模組一：AUTH（/login）

| 測試編號 | 測試場景 | 方法 | 端點 | 輸入資料 | 預期結果 | 優先順序 |
|----------|----------|------|------|----------|----------|----------|
| AUTH-001 | 正常登入 — 有效帳密 | POST | /login/ | email: `admin@bestpartner.com.tw`、password: `admin` | HTTP 200，回傳有效 JWT token | P0 |
| AUTH-002 | 驗證 JWT 有效 | POST | /login/check | AUTH-001 取得的 token | HTTP 200，token 驗證通過 | P0 |
| AUTH-003 | 錯誤登入 — 密碼錯誤 | POST | /login/ | email: 有效、password: 錯誤 | HTTP 401 | P1 |
| AUTH-004 | 錯誤登入 — 信箱不存在 | POST | /login/ | email: 不存在 | HTTP 401 或 404 | P1 |
| AUTH-005 | 必填驗證 — 缺少 email | POST | /login/ | 僅傳 password | HTTP 400 | P1 |
| AUTH-006 | 必填驗證 — 缺少 password | POST | /login/ | 僅傳 email | HTTP 400 | P1 |
| AUTH-007 | 必填驗證 — 空白 body | POST | /login/ | `{}` | HTTP 400 | P1 |
| AUTH-008 | email 格式不合法 | POST | /login/ | email: "notanemail" | HTTP 400 | P2 |
| AUTH-009 | JWT check — 無效 token | POST | /login/check | 偽造或過期的 token | HTTP 401 | P1 |
| AUTH-010 | 邊界條件 — 超長 password | POST | /login/ | password 為 1000 字元 | 不應 500，回傳 400 或 401 | P2 |

---

## 模組二：CHAT（/llm）

> **⚠️ 前置條件**：執行本模組 P0 測試（CHAT-001、CHAT-006）前，須先至**模組五：LLM SETTING** 執行 LLMSET-001（新增 LLM 設定）並取得有效的 `llmId`。
> 若尚未建立任何 LLM 設定，可先執行 LLMSET-007（取得設定）確認是否已有可用的 `llmId`。

| 測試編號 | 測試場景 | 方法 | 端點 | 輸入資料 | 預期結果 | 優先順序 |
|----------|----------|------|------|----------|----------|----------|
| CHAT-001 | 同步聊天 — 正常請求 | POST | /llm/chat | 有效 JWT、llmId（見模組五 LLMSET-001）、message | HTTP 200，回傳 AI 回覆 | P0 |
| CHAT-002 | 同步聊天 — 未認證 | POST | /llm/chat | 無 JWT | HTTP 401 | P0 |
| CHAT-003 | 同步聊天 — 缺少 llmId | POST | /llm/chat | 有效 JWT、僅傳 message | HTTP 400 | P1 |
| CHAT-004 | 同步聊天 — 缺少 message | POST | /llm/chat | 有效 JWT、僅傳 llmId | HTTP 400 | P1 |
| CHAT-005 | 同步聊天 — llmId 不存在 | POST | /llm/chat | 有效 JWT、不存在的 llmId | HTTP 404 或業務錯誤碼 | P1 |
| CHAT-006 | 串流聊天 — 正常請求 | POST | /llm/chatStreaming | 有效 JWT、llmId（見模組五 LLMSET-001）等必填欄位 | HTTP 200，stream 格式回應 | P0 |
| CHAT-007 | 串流聊天 — 未認證 | POST | /llm/chatStreaming | 無 JWT | HTTP 401 | P0 |
| CHAT-008 | 自定義助手聊天 — 不帶 mcpIds | POST | /llm/customAssistantChat | 有效 JWT、必填欄位 | HTTP 200 | P1 |
| CHAT-009 | 自定義助手聊天 — 帶有效 mcpIds | POST | /llm/customAssistantChat | 有效 JWT、有效 mcpIds | HTTP 200 | P1 |
| CHAT-010 | 自定義助手聊天 — 帶無效 mcpIds | POST | /llm/customAssistantChat | 有效 JWT、不存在的 mcpIds | HTTP 404 或業務錯誤 | P2 |
| CHAT-011 | 自定義助手串流 — 正常 | POST | /llm/customAssistantChatStreaming | 有效 JWT、必填欄位 | HTTP 200，stream 格式 | P1 |
| CHAT-012 | 上傳檔案 — 正常 | POST | /llm/uploadFile | 有效 JWT、multipart 檔案 | HTTP 200，回傳識別資訊 | P1 |
| CHAT-013 | 上傳檔案 — 未認證 | POST | /llm/uploadFile | 無 JWT | HTTP 401 | P1 |
| CHAT-014 | 上傳檔案 — 無附件 | POST | /llm/uploadFile | 有效 JWT、無檔案 | HTTP 400 | P2 |
| CHAT-015 | 上傳檔案 — 超大檔案 | POST | /llm/uploadFile | 有效 JWT、超過限制大小 | 應回傳 413，不應 500 | P2 |
| CHAT-016 | 同步聊天 — message 為空字串 | POST | /llm/chat | 有效 JWT、message: "" | HTTP 400 | P2 |

---

## 模組三：ADMIN CHAT（/llm/admin）

| 測試編號 | 測試場景 | 方法 | 端點 | 輸入資料 | 預期結果 | 優先順序 |
|----------|----------|------|------|----------|----------|----------|
| ADCHAT-001 | 管理員聊天 — admin 角色正常 | POST | /llm/admin/chat | admin JWT、必填欄位 | HTTP 200 | P0 |
| ADCHAT-002 | 管理員聊天 — 一般用戶存取 | POST | /llm/admin/chat | user JWT | HTTP 403 | P0 |
| ADCHAT-003 | 管理員聊天 — 未認證 | POST | /llm/admin/chat | 無 JWT | HTTP 401 | P0 |
| ADCHAT-004 | 管理員自定義助手聊天 — admin 正常 | POST | /llm/admin/customAssistantChat | admin JWT、必填欄位 | HTTP 200 | P1 |
| ADCHAT-005 | 管理員自定義助手聊天 — 一般用戶 | POST | /llm/admin/customAssistantChat | user JWT | HTTP 403 | P1 |

---

## 模組四：USER（/llm/user）

| 測試編號 | 測試場景 | 方法 | 端點 | 輸入資料 | 預期結果 | 優先順序 |
|----------|----------|------|------|----------|----------|----------|
| USER-001 | 註冊 — 正常新帳號 | POST | /llm/user/register | username, password, email 均有效 | HTTP 200 | P0 |
| USER-002 | 註冊 — 重複 email | POST | /llm/user/register | 已存在的 email | HTTP 409 或業務錯誤 | P1 |
| USER-003 | 註冊 — 缺少 username | POST | /llm/user/register | 僅傳 password, email | HTTP 400 | P1 |
| USER-004 | 註冊 — 缺少 password | POST | /llm/user/register | 僅傳 username, email | HTTP 400 | P1 |
| USER-005 | 註冊 — 缺少 email | POST | /llm/user/register | 僅傳 username, password | HTTP 400 | P1 |
| USER-006 | 註冊 — 非法 email 格式 | POST | /llm/user/register | email: "badformat" | HTTP 400 | P2 |
| USER-007 | 取得用戶資訊 — 已認證 | POST | /llm/user/get | 有效 JWT | HTTP 200 | P0 |
| USER-008 | 取得用戶資訊 — 未認證 | POST | /llm/user/get | 無 JWT | HTTP 401 | P0 |
| USER-009 | 更新用戶 — 正常 | POST | /llm/user/update | 有效 JWT、id, email, status | HTTP 200 | P1 |
| USER-010 | 更新用戶 — 缺少 id | POST | /llm/user/update | 有效 JWT、缺少 id | HTTP 400 | P1 |
| USER-011 | 更新用戶 — 不存在的 id | POST | /llm/user/update | 有效 JWT、不存在的 id | HTTP 404 | P1 |
| USER-012 | 切換狀態 — 正常 | POST | /llm/user/switchStatus | 有效 JWT、id, status ⚠️ 見備註 | HTTP 200（需先修復 "all" 權限映射，目前預設帳號預期 403） | P1 |
| USER-013 | 切換狀態 — 缺少 status | POST | /llm/user/switchStatus | 有效 JWT、僅傳 id ⚠️ 見備註 | HTTP 400（需先修復 "all" 權限映射） | P1 |
| USER-014 | 刪除用戶 — 正常 | DELETE | /llm/user/delete | 有效 JWT、id 有效 ⚠️ 見備註 | HTTP 200（需先修復 "all" 權限映射，目前預設帳號預期 403） | P1 |
| USER-015 | 刪除用戶 — 缺少 id | DELETE | /llm/user/delete | 有效 JWT、無 id | HTTP 400 | P1 |
| USER-016 | 刪除用戶 — 不存在的 id | DELETE | /llm/user/delete | 有效 JWT、不存在的 id | HTTP 404 | P2 |
| USER-017 | 業務邏輯 — 刪除後無法取得 | POST → DELETE → POST | /llm/user/get | 建立用戶，刪除後再查詢 | 回傳 404 或空結果 | P1 |

> **USER-012 / USER-013 / USER-014 備註**：`/llm/user/switchStatus` 與 `/llm/user/delete` 的安全註解為 `@RolesAllowed("all")`。`llm_role_permission` 中無任何角色擁有 permission `"all"` (num=2)，因此所有預設帳號（含 admin）呼叫均預期 HTTP 403。須在資料庫中為 ADMIN 角色補充 `(0, 2)` 的 role_permission 映射後，才能執行正向測試。

---

## 模組五：LLM SETTING（/llm/setting）

| 測試編號 | 測試場景 | 方法 | 端點 | 輸入資料 | 預期結果 | 優先順序 |
|----------|----------|------|------|----------|----------|----------|
| LLMSET-001 | 新增設定 — 正常 | POST | /llm/setting/save | 有效 JWT、modelType, platformId, llmModel.modelName | HTTP 200 | P0 |
| LLMSET-002 | 新增設定 — 未認證 | POST | /llm/setting/save | 無 JWT | HTTP 401 | P0 |
| LLMSET-003 | 新增設定 — 缺少 modelType | POST | /llm/setting/save | 有效 JWT、缺少 modelType | HTTP 400 | P1 |
| LLMSET-004 | 新增設定 — 缺少 platformId | POST | /llm/setting/save | 有效 JWT、缺少 platformId | HTTP 400 | P1 |
| LLMSET-005 | 新增設定 — 缺少 modelName | POST | /llm/setting/save | 有效 JWT、缺少 llmModel.modelName | HTTP 400 | P1 |
| LLMSET-006 | 更新設定 — 正常 | POST | /llm/setting/update | 有效 JWT、存在的設定資料 | HTTP 200 | P1 |
| LLMSET-007 | 取得設定 — 正常 | POST | /llm/setting/get | 有效 JWT、有效 llmId | HTTP 200 | P0 |
| LLMSET-008 | 取得設定 — 缺少 llmId | POST | /llm/setting/get | 有效 JWT、無 llmId | HTTP 400 | P1 |
| LLMSET-009 | 取得設定 — 不存在的 llmId | POST | /llm/setting/get | 有效 JWT、不存在的 llmId | HTTP 404 | P2 |
| LLMSET-010 | 刪除設定 — 正常 | POST | /llm/setting/delete | 有效 JWT、有效 id | HTTP 200 | P1 |
| LLMSET-011 | 刪除設定 — 缺少 id | POST | /llm/setting/delete | 有效 JWT、無 id | HTTP 400 | P1 |
| LLMSET-012 | 新增平台 — admin 正常 | POST | /llm/setting/platform/add | admin JWT、platform 有效 | HTTP 200 | P1 |
| LLMSET-013 | 新增平台 — 一般用戶拒絕 | POST | /llm/setting/platform/add | user JWT | HTTP 403 | P1 |
| LLMSET-014 | 刪除平台 — admin 正常 | POST | /llm/setting/platform/delete | admin JWT、有效 id | HTTP 200 | P1 |
| LLMSET-015 | 刪除平台 — 一般用戶拒絕 | POST | /llm/setting/platform/delete | user JWT | HTTP 403 | P1 |
| LLMSET-016 | 業務邏輯 — 刪除後查詢 | POST → DELETE → POST | /llm/setting/get | 建立後刪除，再查詢同 llmId | HTTP 404 | P1 |

---

## 模組六：VECTOR（/llm/vector）

| 測試編號 | 測試場景 | 方法 | 端點 | 輸入資料 | 預期結果 | 優先順序 |
|----------|----------|------|------|----------|----------|----------|
| VEC-001 | 新增向量設定 — 正常 | POST | /llm/vector/save | 有效 JWT、collectionName, dimension | HTTP 200 | P0 |
| VEC-002 | 新增向量設定 — 未認證 | POST | /llm/vector/save | 無 JWT | HTTP 401 | P0 |
| VEC-003 | 更新向量設定 — 正常 | POST | /llm/vector/update | 有效 JWT、存在的設定 | HTTP 200 | P1 |
| VEC-004 | 取得知識庫 — 正常 | POST | /llm/vector/getKnowledgeStore | 有效 JWT | HTTP 200 | P1 |
| VEC-005 | 上傳向量文件 — 正常 | POST | /llm/vector/uploadFiles | 有效 JWT、embeddingModelId, embeddingStoreId, files | HTTP 200 | P0 |
| VEC-006 | 上傳向量文件 — 未認證 | POST | /llm/vector/uploadFiles | 無 JWT | HTTP 401 | P0 |
| VEC-007 | 上傳向量文件 — 無附件 | POST | /llm/vector/uploadFiles | 有效 JWT、無檔案 | HTTP 400 | P2 |
| VEC-008 | 搜尋向量資料 — 正常 | POST | /llm/vector/getDataFromEmbeddingStore | 有效 JWT、knowledgeId, content | HTTP 200，回傳相關結果 | P0 |
| VEC-009 | 搜尋向量資料 — 缺少 knowledgeId | POST | /llm/vector/getDataFromEmbeddingStore | 有效 JWT、僅傳 content | HTTP 400 | P1 |
| VEC-010 | 搜尋向量資料 — 缺少 content | POST | /llm/vector/getDataFromEmbeddingStore | 有效 JWT、僅傳 knowledgeId | HTTP 400 | P1 |
| VEC-011 | 搜尋向量資料 — 不存在的 knowledgeId | POST | /llm/vector/getDataFromEmbeddingStore | 有效 JWT、不存在的 knowledgeId | HTTP 404 或空結果 | P2 |
| VEC-012 | 刪除向量資料 — 正常 | DELETE | /llm/vector/deleteData | 有效 JWT、knowledgeId | HTTP 200 | P1 |
| VEC-013 | 刪除向量資料 — 未認證 | DELETE | /llm/vector/deleteData | 無 JWT | HTTP 401 | P1 |
| VEC-014 | 業務邏輯 — 上傳後可搜尋 | uploadFiles → getDataFromEmbeddingStore | 上傳含特定關鍵字的文件，再以該關鍵字搜尋 | 搜尋結果應包含相關內容 | P1 |

---

## 模組七：TOOL（/llm/tool）

| 測試編號 | 測試場景 | 方法 | 端點 | 輸入資料 | 預期結果 | 優先順序 |
|----------|----------|------|------|----------|----------|----------|
| TOOL-001 | 列出工具 — 正常 | GET | /llm/tool/list | 無 | HTTP 200，回傳工具清單 | P0 |
| TOOL-002 | 取得工具 — 正常 | POST | /llm/tool/get | id 有效 | HTTP 200 | P1 |
| TOOL-003 | 取得工具 — 缺少 id | POST | /llm/tool/get | 無 id | HTTP 400 | P1 |
| TOOL-004 | 取得工具 — 不存在的 id | POST | /llm/tool/get | 不存在的 id | HTTP 404 | P2 |
| TOOL-005 | 註冊工具 — 正常 | POST | /llm/tool/register | admin JWT、name, classPath, groupId, type | HTTP 200 | P1 |
| TOOL-006 | 註冊工具 — 缺少 name | POST | /llm/tool/register | admin JWT、缺少 name | HTTP 400 | P1 |
| TOOL-007 | 註冊工具 — 缺少 classPath | POST | /llm/tool/register | admin JWT、缺少 classPath | HTTP 400 | P1 |
| TOOL-008 | 註冊工具 — 缺少 type | POST | /llm/tool/register | admin JWT、缺少 type | HTTP 400 | P1 |
| TOOL-009 | 刪除工具 — 正常 | POST | /llm/tool/delete | admin JWT、有效 id | HTTP 200 | P1 |
| TOOL-010 | 刪除工具 — 缺少 id | POST | /llm/tool/delete | admin JWT、無 id | HTTP 400 | P1 |
| TOOL-011 | 儲存工具設定 — 正常 | POST | /llm/tool/saveSetting | id, alias 有效 | HTTP 200 | P1 |
| TOOL-012 | 儲存工具設定 — 缺少 alias | POST | /llm/tool/saveSetting | 僅傳 id | HTTP 400 | P2 |
| TOOL-013 | 更新工具設定 — 正常 | POST | /llm/tool/updateSetting | settingId, settingContent | HTTP 200 | P1 |
| TOOL-014 | 更新工具設定 — 缺少 settingContent | POST | /llm/tool/updateSetting | 僅傳 settingId | HTTP 400 | P2 |
| TOOL-015 | 新增分類 — 正常 | POST | /llm/tool/category/save | admin JWT、group 有效 | HTTP 200 | P1 |
| TOOL-016 | 新增分類 — 缺少 group | POST | /llm/tool/category/save | admin JWT、無 group | HTTP 400 | P2 |
| TOOL-017 | 更新分類 — 正常 | POST | /llm/tool/category/update | admin JWT、groupId, group | HTTP 200 | P1 |
| TOOL-018 | 刪除分類 — 正常 | POST | /llm/tool/category/delete | admin JWT、groupId 有效 | HTTP 200 | P1 |
| TOOL-019 | 業務邏輯 — 刪除後不出現於清單 | register → delete → list | 確認 id 不再出現 | P1 |

> **TOOL 模組權限備註**：`/llm/tool/register`、`/llm/tool/delete`、`/llm/tool/category/save`、`/llm/tool/category/update`、`/llm/tool/category/delete` 均使用 `@RolesAllowed("admin")` 保護，必須使用 `admin@bestpartner.com.tw` 帳號的 JWT。以 `user` JWT 呼叫此類端點，預期 HTTP 403。

---

## 模組八：MCP（/llm/mcpServer）

| 測試編號 | 測試場景 | 方法 | 端點 | 輸入資料 | 預期結果 | 優先順序 |
|----------|----------|------|------|----------|----------|----------|
| MCP-001 | 列出 MCP — 已認證 | GET | /llm/mcpServer/list | 有效 JWT | HTTP 200 | P0 |
| MCP-002 | 列出 MCP — 未認證 | GET | /llm/mcpServer/list | 無 JWT | HTTP 401 | P0 |
| MCP-003 | 取得 MCP — 正常 | POST | /llm/mcpServer/get | 有效 JWT、mcpId | HTTP 200 | P1 |
| MCP-004 | 取得 MCP — 缺少 mcpId | POST | /llm/mcpServer/get | 有效 JWT、無 mcpId | HTTP 400 | P1 |
| MCP-005 | 註冊 MCP — admin 正常 | POST | /llm/mcpServer/register | admin JWT、name, type | HTTP 200 | P1 |
| MCP-006 | 註冊 MCP — 一般用戶拒絕 | POST | /llm/mcpServer/register | user JWT | HTTP 403 | P1 |
| MCP-007 | 註冊 MCP — 缺少 name | POST | /llm/mcpServer/register | admin JWT、僅傳 type | HTTP 400 | P1 |
| MCP-008 | 更新 MCP — admin 正常 | POST | /llm/mcpServer/update | admin JWT、存在的資料 | HTTP 200 | P1 |
| MCP-009 | 更新 MCP — 一般用戶拒絕 | POST | /llm/mcpServer/update | user JWT | HTTP 403 | P1 |
| MCP-010 | 刪除 MCP — admin 正常 | POST | /llm/mcpServer/delete | admin JWT、有效 id | HTTP 200 | P1 |
| MCP-011 | 刪除 MCP — 一般用戶拒絕 | POST | /llm/mcpServer/delete | user JWT | HTTP 403 | P1 |
| MCP-012 | 儲存使用者設定 — 正常 | POST | /llm/mcpServer/saveSetting | 有效 JWT、mcpId, settingContent | HTTP 200 | P1 |
| MCP-013 | 儲存使用者設定 — 缺少 mcpId | POST | /llm/mcpServer/saveSetting | 有效 JWT、缺少 mcpId | HTTP 400 | P1 |
| MCP-014 | 取得使用者設定 — 正常 | POST | /llm/mcpServer/getSetting | 有效 JWT、userSettingId | HTTP 200 | P1 |
| MCP-015 | 取得使用者設定 — 缺少 id | POST | /llm/mcpServer/getSetting | 有效 JWT、無參數 | HTTP 400 | P2 |
| MCP-016 | 更新使用者設定 — 正常 | POST | /llm/mcpServer/updateSetting | 有效 JWT、存在的設定 | HTTP 200 | P1 |
| MCP-017 | 刪除使用者設定 — 正常 | DELETE | /llm/mcpServer/deleteSetting | 有效 JWT、userSettingId | HTTP 200 | P1 |
| MCP-018 | 刪除使用者設定 — 缺少 id | DELETE | /llm/mcpServer/deleteSetting | 有效 JWT、無參數 | HTTP 400 | P2 |
| MCP-019 | 業務邏輯 — 設定後於聊天中使用 | saveSetting → customAssistantChat | 儲存後以對應 mcpSettingIds 發送聊天 | MCP 功能被正確呼叫 | P1 |

---

## 模組九：PERMISSION（/llm/permission）

| 測試編號 | 測試場景 | 方法 | 端點 | 輸入資料 | 預期結果 | 優先順序 |
|----------|----------|------|------|----------|----------|----------|
| PERM-001 | 新增權限 — admin 角色正常 | POST | /llm/permission/add | admin JWT（`admin@bestpartner.com.tw`）、name, num | HTTP 200 | P1 |
| PERM-001b | 新增權限 — 一般用戶拒絕 | POST | /llm/permission/add | user JWT（`user@bestpartner.com.tw`） | HTTP 403 | P1 |
| PERM-002 | 新增權限 — 未認證 | POST | /llm/permission/add | 無 JWT | HTTP 401 | P1 |
| PERM-003 | 新增權限 — 缺少 name | POST | /llm/permission/add | 有效 JWT、僅傳 num | HTTP 400 | P1 |
| PERM-004 | 新增權限 — 缺少 num | POST | /llm/permission/add | 有效 JWT、僅傳 name | HTTP 400 | P1 |
| PERM-005 | 新增權限 — num 為非數字 | POST | /llm/permission/add | 有效 JWT、num: "abc" | HTTP 400 | P2 |
| PERM-006 | 更新權限 — 正常 | POST | /llm/permission/update | admin JWT、id, name, num | HTTP 200 | P1 |
| PERM-007 | 更新權限 — 缺少 id | POST | /llm/permission/update | 有效 JWT、缺少 id | HTTP 400 | P1 |
| PERM-008 | 更新權限 — 不存在的 id | POST | /llm/permission/update | 有效 JWT、不存在的 id | HTTP 404 | P2 |
| PERM-009 | 刪除權限 — 正常 | DELETE | /llm/permission/delete | admin JWT、id | HTTP 200 | P1 |
| PERM-010 | 刪除權限 — 缺少 id | DELETE | /llm/permission/delete | 有效 JWT、無 id | HTTP 400 | P1 |
| PERM-011 | 刪除權限 — 未認證 | DELETE | /llm/permission/delete | 無 JWT | HTTP 401 | P1 |

---

## 模組十：SYSTEM SETTING（/systemSetting）

| 測試編號 | 測試場景 | 方法 | 端點 | 輸入資料 | 預期結果 | 優先順序 |
|----------|----------|------|------|----------|----------|----------|
| SYS-001 | 列出設定 — 正常 | GET | /systemSetting/list | 無 | HTTP 200 | P0 |
| SYS-002 | 取得設定 — 正常 | POST | /systemSetting/get | key 有效 | HTTP 200 | P1 |
| SYS-003 | 取得設定 — 缺少 key | POST | /systemSetting/get | 無 key | HTTP 400 | P1 |
| SYS-004 | 取得設定 — key 不存在 | POST | /systemSetting/get | 不存在的 key | HTTP 404 或空結果 | P2 |
| SYS-005 | 新增設定 — 正常 | POST | /systemSetting/add | value 有效 | HTTP 200 | P1 |
| SYS-006 | 新增設定 — 缺少 value | POST | /systemSetting/add | 空 body | HTTP 400 | P1 |
| SYS-007 | 更新設定 — 正常 | POST | /systemSetting/update | value 有效 | HTTP 200 | P1 |
| SYS-008 | 更新設定 — 缺少 value | POST | /systemSetting/update | 空 body | HTTP 400 | P1 |
| SYS-009 | 刪除設定 — 正常 | DELETE | /systemSetting/delete | id 有效 | HTTP 200 | P1 |
| SYS-010 | 刪除設定 — 缺少 id | DELETE | /systemSetting/delete | 無 id | HTTP 400 | P1 |
| SYS-011 | 刪除設定 — 不存在的 id | DELETE | /systemSetting/delete | 不存在的 id | HTTP 404 | P2 |
| SYS-012 | 業務邏輯 — 新增後可查詢 | add → get | 新增後以相同 key 查詢 | 取得剛新增的值 | P1 |
| SYS-013 | 業務邏輯 — 更新後值反映改變 | update → get | 更新後查詢同 key | 取得更新後的值 | P1 |

---

## 跨模組整合測試

| 測試編號 | 測試場景 | 涉及端點 | 執行步驟 | 預期結果 | 優先順序 |
|----------|----------|----------|----------|----------|----------|
| INT-001 | 完整聊天流程 | login → setting/get → chat | 登入 → 取得 llmId → 發送聊天 | 每步驟均 200，取得 AI 回覆 | P0 |
| INT-002 | 向量知識庫輔助聊天 | login → vector/save → uploadFiles → customAssistantChat | 登入 → 建立知識庫 → 上傳文件 → 聊天 | 聊天回覆應引用上傳文件內容 | P1 |
| INT-003 | MCP 整合聊天 | login → mcpServer/saveSetting → customAssistantChat | 登入 → 儲存 MCP 設定 → 帶 mcpSettingIds 聊天 | MCP 工具被正確呼叫 | P1 |
| INT-004 | 用戶生命週期 | register → login → get → update → delete | 完整 CRUD 流程 | 每步驟正確，刪除後登入失敗 | P1 |
| INT-005 | JWT 過期保護 | login → wait(expire) → chat | 等待 token 過期後使用舊 token | HTTP 401 | P1 |
| INT-006 | admin 工作流 | login(admin) → platform/add → setting/save → chat | admin 新增平台 → 設定 → 聊天 | 整體流程順利 | P1 |

---

## 通用安全性測試

| 測試編號 | 測試場景 | 適用端點 | 說明 | 預期結果 | 優先順序 |
|----------|----------|----------|------|----------|----------|
| SEC-001 | SQL Injection | /login/ | email: `' OR '1'='1` | 不應登入成功，回傳 400/401 | P1 |
| SEC-002 | XSS Payload | /llm/chat、/llm/user/register | message 帶 `<script>` | 不應 500，應正常處理或過濾 | P2 |
| SEC-003 | 過期 JWT | 所有 @Authenticated | 使用已過期 token | HTTP 401 | P1 |
| SEC-004 | 竄改 JWT Payload | 所有 @Authenticated | 修改 role 欄位後傳送 | HTTP 401 | P1 |
| SEC-005 | 水平越權 | /llm/user/get、/llm/mcpServer/getSetting | user A 的 token 嘗試取得 user B 的設定 | HTTP 403 或僅回傳自身資料 | P1 |
| SEC-006 | 大量請求壓力 | /llm/chat | 短時間內大量發送請求 | 不應 500，應有 429 或排隊機制 | P2 |

---

## 測試執行順序

```
Phase 1 — P0 核心流程
  AUTH-001 → AUTH-002 → CHAT-001 → CHAT-006
  → VEC-001 → VEC-005 → VEC-008 → MCP-001
  → SYS-001 → LLMSET-001 → LLMSET-007

Phase 2 — P1 主要功能與認證邊界
  各模組正向流程 → 未認證存取 → 角色不足 → 必填欄位驗證

Phase 3 — P1 業務邏輯整合
  INT-001 → INT-002 → INT-003 → INT-004

Phase 4 — P2 邊界條件與安全測試
  各模組邊界值 → SEC-001 ~ SEC-006
```

---

## 測試完成標準

| 等級 | 通過標準 |
|------|----------|
| P0 | 100% 通過，任何失敗均為 Release Blocker |
| P1 | 95% 以上通過，已知失敗需有追蹤 Issue |
| P2 | 80% 以上通過，剩餘項目列為 Tech Debt |

---

## 統計摘要

| 模組 | 測試案例數 |
|------|-----------|
| AUTH | 10 |
| CHAT | 16 |
| ADMIN CHAT | 5 |
| USER | 17 |
| LLM SETTING | 16 |
| VECTOR | 14 |
| TOOL | 19 |
| MCP | 19 |
| PERMISSION | 11 |
| SYSTEM SETTING | 13 |
| 跨模組整合 | 6 |
| 安全性 | 6 |
| **總計** | **152** |
