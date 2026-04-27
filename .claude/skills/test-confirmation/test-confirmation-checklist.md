# BestPartner API 測試功能確認表

> 本文件為定期測試記錄模板，每次測試週期開始前請複製此文件並填寫狀態欄位。
> 詳細測試說明請參考 [docs/api-test-plan.md](../../../docs/api-test-plan.md)。

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

1. 複製此檔案，重新命名為 `test-confirmation-YYYYMMDD.md`
2. 填寫「測試週期資訊」
3. 按模組依序執行測試，逐項填入狀態
4. 測試完成後更新「測試結果摘要」
5. 所有 ❌ 項目需在「問題追蹤區」建立記錄

---

## 測試週期資訊

| 項目 | 內容 |
|------|------|
| 測試日期 | |
| 服務版本 | 0.1.7-SNAPSHOT |
| 測試環境 | dev / sit |
| LLM 平台 | |
| LLM Setting ID | |
| Base URL | `http://localhost:80` |
| 測試結束日期 | |

---

## 預設測試帳號參考

> 以下資訊來源：`docs/sql/bestpartner-init-data.sql`。使用前請確認初始化腳本已執行。

| 帳號 | Email | 密碼 | JWT groups | 可測試場景 |
|------|-------|------|-----------|-----------|
| `admin` | `admin@bestpartner.com.tw` | `admin` | `admin`, `user-read`, `user-write` | 所有端點（公開、`@Authenticated`、`@RolesAllowed("admin")`） |
| `user` | `user@bestpartner.com.tw` | `user` | `user-read`, `user-write` | `@Authenticated` 及 `@RolesAllowed("user-read")` 端點；admin 端點預期 403 |
| `test_user` | `test@partmer.com.tw` | `user` | （空） | 僅限「無角色被拒絕」場景；`@Authenticated` 端點亦預期失敗 |

### 已知系統配置缺陷（測試前必讀）

| 缺陷 | 影響端點 | 影響測試 ID | 預期行為 |
|------|---------|------------|---------|
| `@RolesAllowed("all")` 的 permission 無角色映射 | `POST /llm/user/switchStatus`、`DELETE /llm/user/delete` | USER-012、USER-014 | 所有預設帳號均回傳 403，正向測試無法執行，記錄為「已知缺陷-Blocked」 |
| TOOL 管理端點需 admin，舊文件未標明 | `/llm/tool/register`、`/llm/tool/delete`、`/llm/tool/category/*` | TOOL-005~010、TOOL-015~018 | 須使用 admin JWT；user JWT 呼叫預期 403 |
| PERMISSION 端點需 admin，舊描述誤寫「已認證」 | `/llm/permission/add`、`/llm/permission/update`、`/llm/permission/delete` | PERM-001、PERM-006、PERM-009 | 須使用 admin JWT |

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

## 模組測試清單

---

### AUTH — 認證模組（`/login`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| AUTH-001 | 正常登入（有效帳密） | P0 | POST | `/login/` | | |
| AUTH-002 | 驗證 JWT 有效 | P0 | POST | `/login/check` | | |
| AUTH-003 | 錯誤登入（密碼錯誤） | P1 | POST | `/login/` | | |
| AUTH-004 | 錯誤登入（信箱不存在） | P1 | POST | `/login/` | | |
| AUTH-005 | 必填驗證（缺 email） | P1 | POST | `/login/` | | |
| AUTH-006 | 必填驗證（缺 password） | P1 | POST | `/login/` | | |
| AUTH-007 | 必填驗證（空 body） | P1 | POST | `/login/` | | |
| AUTH-008 | email 格式不合法 | P2 | POST | `/login/` | | |
| AUTH-009 | JWT check（無效 token） | P1 | POST | `/login/check` | | |
| AUTH-010 | 邊界條件（超長 password） | P2 | POST | `/login/` | | |

---

### CHAT — 聊天模組（`/llm`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| CHAT-001 | 同步聊天（正常請求） | P0 | POST | `/llm/chat` | | |
| CHAT-002 | 同步聊天（未認證） | P0 | POST | `/llm/chat` | | |
| CHAT-003 | 同步聊天（缺 llmId） | P1 | POST | `/llm/chat` | | |
| CHAT-004 | 同步聊天（缺 message） | P1 | POST | `/llm/chat` | | |
| CHAT-005 | 同步聊天（llmId 不存在） | P1 | POST | `/llm/chat` | | |
| CHAT-006 | 串流聊天（正常請求） | P0 | POST | `/llm/chatStreaming` | | |
| CHAT-007 | 串流聊天（未認證） | P0 | POST | `/llm/chatStreaming` | | |
| CHAT-008 | 自定義助手聊天（不帶 mcpIds） | P1 | POST | `/llm/customAssistantChat` | | |
| CHAT-009 | 自定義助手聊天（有效 mcpIds） | P1 | POST | `/llm/customAssistantChat` | | |
| CHAT-010 | 自定義助手聊天（無效 mcpIds） | P2 | POST | `/llm/customAssistantChat` | | |
| CHAT-011 | 自定義助手串流（正常） | P1 | POST | `/llm/customAssistantChatStreaming` | | |
| CHAT-012 | 上傳檔案（正常） | P1 | POST | `/llm/uploadFile` | | |
| CHAT-013 | 上傳檔案（未認證） | P1 | POST | `/llm/uploadFile` | | |
| CHAT-014 | 上傳檔案（無附件） | P2 | POST | `/llm/uploadFile` | | |
| CHAT-015 | 上傳檔案（超大檔案） | P2 | POST | `/llm/uploadFile` | | |
| CHAT-016 | 同步聊天（message 為空） | P2 | POST | `/llm/chat` | | |
| CHAT-017 | 同步聊天（admin 帳號） | P1 | POST | `/llm/chat` | | 驗證 admin 身分可正常使用聊天功能 |
| CHAT-018 | 串流聊天（admin 帳號） | P1 | POST | `/llm/chatStreaming` | | 驗證 admin 身分可正常使用串流聊天 |

---

### ADMIN CHAT — 管理員聊天模組（`/llm/admin`）

> ⚙️ **此模組使用 `application.properties` 設定檔中的系統預設模型**（`ai-platform.openrouter.*` 等），與資料庫中的使用者 LLM Setting 無關。
> Request body 需傳入 `platform` 欄位（如 `"OPENROUTER"`）以指定系統模型，測試前請確認設定檔已正確設定對應平台的 `api-key`、`model-name` 等參數。

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| ADCHAT-001 | 管理員聊天（admin 正常） | P0 | POST | `/llm/admin/chat` | | |
| ADCHAT-002 | 管理員聊天（一般用戶存取） | P0 | POST | `/llm/admin/chat` | | |
| ADCHAT-003 | 管理員聊天（未認證） | P0 | POST | `/llm/admin/chat` | | |
| ADCHAT-004 | 管理員自定義助手聊天（admin 正常） | P1 | POST | `/llm/admin/customAssistantChat` | | |
| ADCHAT-005 | 管理員自定義助手聊天（一般用戶） | P1 | POST | `/llm/admin/customAssistantChat` | | |

---

### USER — 用戶模組（`/llm/user`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| USER-001 | 註冊（正常新帳號） | P0 | POST | `/llm/user/register` | | |
| USER-002 | 註冊（重複 email） | P1 | POST | `/llm/user/register` | | |
| USER-003 | 註冊（缺 username） | P1 | POST | `/llm/user/register` | | |
| USER-004 | 註冊（缺 password） | P1 | POST | `/llm/user/register` | | |
| USER-005 | 註冊（缺 email） | P1 | POST | `/llm/user/register` | | |
| USER-006 | 註冊（非法 email） | P2 | POST | `/llm/user/register` | | |
| USER-007 | 取得用戶資訊（已認證） | P0 | POST | `/llm/user/get` | | |
| USER-008 | 取得用戶資訊（未認證） | P0 | POST | `/llm/user/get` | | |
| USER-009 | 更新用戶（正常） | P1 | POST | `/llm/user/update` | | |
| USER-010 | 更新用戶（缺 id） | P1 | POST | `/llm/user/update` | | |
| USER-011 | 更新用戶（id 不存在） | P1 | POST | `/llm/user/update` | | |
| USER-012 | 切換狀態（正常） | P1 | POST | `/llm/user/switchStatus` | | ⚠️ @RolesAllowed("all") — 預設帳號均無此 permission，預期 403，記錄為 Blocked |
| USER-013 | 切換狀態（缺 status） | P1 | POST | `/llm/user/switchStatus` | | ⚠️ 同 USER-012，權限缺陷導致難以正常測試 |
| USER-014 | 刪除用戶（正常） | P1 | DELETE | `/llm/user/delete` | | ⚠️ @RolesAllowed("all") — 預設帳號均無此 permission，預期 403，記錄為 Blocked |
| USER-015 | 刪除用戶（缺 id） | P1 | DELETE | `/llm/user/delete` | | |
| USER-016 | 刪除用戶（id 不存在） | P2 | DELETE | `/llm/user/delete` | | |
| USER-017 | 業務邏輯（刪除後無法取得） | P1 | 多步 | `/llm/user/*` | | |

---

### LLM SETTING — LLM 設定模組（`/llm/setting`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| LLMSET-001 | 新增設定（正常） | P0 | POST | `/llm/setting/save` | | |
| LLMSET-002 | 新增設定（未認證） | P0 | POST | `/llm/setting/save` | | |
| LLMSET-003 | 新增設定（缺 modelType） | P1 | POST | `/llm/setting/save` | | |
| LLMSET-004 | 新增設定（缺 platformId） | P1 | POST | `/llm/setting/save` | | |
| LLMSET-005 | 新增設定（缺 modelName） | P1 | POST | `/llm/setting/save` | | |
| LLMSET-006 | 更新設定（正常） | P1 | POST | `/llm/setting/update` | | |
| LLMSET-007 | 取得設定（正常） | P0 | POST | `/llm/setting/get` | | |
| LLMSET-008 | 取得設定（缺 llmId） | P1 | POST | `/llm/setting/get` | | |
| LLMSET-009 | 取得設定（llmId 不存在） | P2 | POST | `/llm/setting/get` | | |
| LLMSET-010 | 刪除設定（正常） | P1 | POST | `/llm/setting/delete` | | |
| LLMSET-011 | 刪除設定（缺 id） | P1 | POST | `/llm/setting/delete` | | |
| LLMSET-012 | 新增平台（admin 正常） | P1 | POST | `/llm/setting/platform/add` | | |
| LLMSET-013 | 新增平台（一般用戶拒絕） | P1 | POST | `/llm/setting/platform/add` | | |
| LLMSET-014 | 刪除平台（admin 正常） | P1 | POST | `/llm/setting/platform/delete` | | |
| LLMSET-015 | 刪除平台（一般用戶拒絕） | P1 | POST | `/llm/setting/platform/delete` | | |
| LLMSET-016 | 業務邏輯（刪除後查詢） | P1 | 多步 | `/llm/setting/*` | | |

---

### VECTOR — 向量模組（`/llm/vector`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| VEC-001 | 新增向量設定（正常） | P0 | POST | `/llm/vector/save` | | |
| VEC-002 | 新增向量設定（未認證） | P0 | POST | `/llm/vector/save` | | |
| VEC-003 | 更新向量設定（正常） | P1 | POST | `/llm/vector/update` | | |
| VEC-004 | 取得知識庫（正常） | P1 | POST | `/llm/vector/getKnowledgeStore` | | |
| VEC-005 | 上傳向量文件（正常） | P0 | POST | `/llm/vector/uploadFiles` | | |
| VEC-006 | 上傳向量文件（未認證） | P0 | POST | `/llm/vector/uploadFiles` | | |
| VEC-007 | 上傳向量文件（無附件） | P2 | POST | `/llm/vector/uploadFiles` | | |
| VEC-008 | 搜尋向量資料（正常） | P0 | POST | `/llm/vector/getDataFromEmbeddingStore` | | |
| VEC-009 | 搜尋向量資料（缺 knowledgeId） | P1 | POST | `/llm/vector/getDataFromEmbeddingStore` | | |
| VEC-010 | 搜尋向量資料（缺 content） | P1 | POST | `/llm/vector/getDataFromEmbeddingStore` | | |
| VEC-011 | 搜尋向量資料（knowledgeId 不存在） | P2 | POST | `/llm/vector/getDataFromEmbeddingStore` | | |
| VEC-012 | 刪除向量資料（正常） | P1 | DELETE | `/llm/vector/deleteData` | | |
| VEC-013 | 刪除向量資料（未認證） | P1 | DELETE | `/llm/vector/deleteData` | | |
| VEC-014 | 業務邏輯（上傳後可搜尋） | P1 | 多步 | `/llm/vector/*` | | |

---

### TOOL — 工具模組（`/llm/tool`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| TOOL-001 | 列出工具（正常） | P0 | GET | `/llm/tool/list` | | |
| TOOL-002 | 取得工具（正常） | P1 | POST | `/llm/tool/get` | | |
| TOOL-003 | 取得工具（缺 id） | P1 | POST | `/llm/tool/get` | | |
| TOOL-004 | 取得工具（id 不存在） | P2 | POST | `/llm/tool/get` | | |
| TOOL-005 | 註冊工具（正常） | P1 | POST | `/llm/tool/register` | | 需 admin JWT |
| TOOL-006 | 註冊工具（缺 name） | P1 | POST | `/llm/tool/register` | | 需 admin JWT |
| TOOL-007 | 註冊工具（缺 classPath） | P1 | POST | `/llm/tool/register` | | 需 admin JWT |
| TOOL-008 | 註冊工具（缺 type） | P1 | POST | `/llm/tool/register` | | 需 admin JWT |
| TOOL-009 | 刪除工具（正常） | P1 | POST | `/llm/tool/delete` | | 需 admin JWT |
| TOOL-010 | 刪除工具（缺 id） | P1 | POST | `/llm/tool/delete` | | 需 admin JWT |
| TOOL-011 | 儲存工具設定（正常） | P1 | POST | `/llm/tool/saveSetting` | | |
| TOOL-012 | 儲存工具設定（缺 alias） | P2 | POST | `/llm/tool/saveSetting` | | |
| TOOL-013 | 更新工具設定（正常） | P1 | POST | `/llm/tool/updateSetting` | | |
| TOOL-014 | 更新工具設定（缺 settingContent） | P2 | POST | `/llm/tool/updateSetting` | | |
| TOOL-015 | 新增分類（正常） | P1 | POST | `/llm/tool/category/save` | | 需 admin JWT |
| TOOL-016 | 新增分類（缺 group） | P2 | POST | `/llm/tool/category/save` | | 需 admin JWT |
| TOOL-017 | 更新分類（正常） | P1 | POST | `/llm/tool/category/update` | | 需 admin JWT |
| TOOL-018 | 刪除分類（正常） | P1 | POST | `/llm/tool/category/delete` | | 需 admin JWT |
| TOOL-019 | 業務邏輯（刪除後不出現清單） | P1 | 多步 | `/llm/tool/*` | | |

---

### MCP — MCP Server 模組（`/llm/mcpServer`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| MCP-001 | 列出 MCP（已認證） | P0 | GET | `/llm/mcpServer/list` | | |
| MCP-002 | 列出 MCP（未認證） | P0 | GET | `/llm/mcpServer/list` | | |
| MCP-003 | 取得 MCP（正常） | P1 | POST | `/llm/mcpServer/get` | | |
| MCP-004 | 取得 MCP（缺 mcpId） | P1 | POST | `/llm/mcpServer/get` | | |
| MCP-005 | 註冊 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/register` | | |
| MCP-006 | 註冊 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/register` | | |
| MCP-007 | 註冊 MCP（缺 name） | P1 | POST | `/llm/mcpServer/register` | | |
| MCP-008 | 更新 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/update` | | |
| MCP-009 | 更新 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/update` | | |
| MCP-010 | 刪除 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/delete` | | |
| MCP-011 | 刪除 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/delete` | | |
| MCP-012 | 儲存使用者設定（正常） | P1 | POST | `/llm/mcpServer/saveSetting` | | |
| MCP-013 | 儲存使用者設定（缺 mcpId） | P1 | POST | `/llm/mcpServer/saveSetting` | | |
| MCP-014 | 取得使用者設定（正常） | P1 | POST | `/llm/mcpServer/getSetting` | | |
| MCP-015 | 取得使用者設定（缺 id） | P2 | POST | `/llm/mcpServer/getSetting` | | |
| MCP-016 | 更新使用者設定（正常） | P1 | POST | `/llm/mcpServer/updateSetting` | | |
| MCP-017 | 刪除使用者設定（正常） | P1 | DELETE | `/llm/mcpServer/deleteSetting` | | |
| MCP-018 | 刪除使用者設定（缺 id） | P2 | DELETE | `/llm/mcpServer/deleteSetting` | | |
| MCP-019 | 業務邏輯（設定後於聊天使用） | P1 | 多步 | `/llm/mcpServer/*` | | |

---

### PERMISSION — 權限模組（`/llm/permission`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| PERM-001 | 新增權限（admin 角色正常） | P1 | POST | `/llm/permission/add` | | 需 admin JWT；原描述「已認證正常」有誤 |
| PERM-002 | 新增權限（未認證） | P1 | POST | `/llm/permission/add` | | |
| PERM-003 | 新增權限（缺 name） | P1 | POST | `/llm/permission/add` | | |
| PERM-004 | 新增權限（缺 num） | P1 | POST | `/llm/permission/add` | | |
| PERM-005 | 新增權限（num 非數字） | P2 | POST | `/llm/permission/add` | | |
| PERM-006 | 更新權限（正常） | P1 | POST | `/llm/permission/update` | | 需 admin JWT |
| PERM-007 | 更新權限（缺 id） | P1 | POST | `/llm/permission/update` | | |
| PERM-008 | 更新權限（id 不存在） | P2 | POST | `/llm/permission/update` | | |
| PERM-009 | 刪除權限（正常） | P1 | DELETE | `/llm/permission/delete` | | 需 admin JWT |
| PERM-010 | 刪除權限（缺 id） | P1 | DELETE | `/llm/permission/delete` | | |
| PERM-011 | 刪除權限（未認證） | P1 | DELETE | `/llm/permission/delete` | | |

---

### SYSTEM SETTING — 系統設定模組（`/systemSetting`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| SYS-001 | 列出設定（正常） | P0 | GET | `/systemSetting/list` | | |
| SYS-002 | 取得設定（正常） | P1 | POST | `/systemSetting/get` | | |
| SYS-003 | 取得設定（缺 key） | P1 | POST | `/systemSetting/get` | | |
| SYS-004 | 取得設定（key 不存在） | P2 | POST | `/systemSetting/get` | | |
| SYS-005 | 新增設定（正常） | P1 | POST | `/systemSetting/add` | | |
| SYS-006 | 新增設定（缺 value） | P1 | POST | `/systemSetting/add` | | |
| SYS-007 | 更新設定（正常） | P1 | POST | `/systemSetting/update` | | |
| SYS-008 | 更新設定（缺 value） | P1 | POST | `/systemSetting/update` | | |
| SYS-009 | 刪除設定（正常） | P1 | DELETE | `/systemSetting/delete` | | |
| SYS-010 | 刪除設定（缺 id） | P1 | DELETE | `/systemSetting/delete` | | |
| SYS-011 | 刪除設定（id 不存在） | P2 | DELETE | `/systemSetting/delete` | | |
| SYS-012 | 業務邏輯（新增後可查詢） | P1 | 多步 | `/systemSetting/*` | | |
| SYS-013 | 業務邏輯（更新後反映改變） | P1 | 多步 | `/systemSetting/*` | | |

---

### 跨模組整合測試

| 測試 ID | 測試場景 | 優先級 | 涉及端點 | 狀態 | 備註 |
|---------|---------|--------|----------|------|------|
| INT-001 | 完整聊天流程 | P0 | `login → setting/get → chat` | | |
| INT-002 | 向量知識庫輔助聊天 | P1 | `login → vector/save → uploadFiles → customAssistantChat` | | |
| INT-003 | MCP 整合聊天 | P1 | `login → mcpServer/saveSetting → customAssistantChat` | | |
| INT-004 | 用戶生命週期 | P1 | `register → login → get → update → delete` | | |
| INT-005 | JWT 過期保護 | P1 | `login → (等待過期) → chat` | | |
| INT-006 | admin 工作流 | P1 | `login(admin) → platform/add → setting/save → chat` | | |

---

### 通用安全性測試

| 測試 ID | 測試場景 | 優先級 | 適用端點 | 狀態 | 備註 |
|---------|---------|--------|----------|------|------|
| SEC-001 | SQL Injection | P1 | `/login/` | | |
| SEC-002 | XSS Payload | P2 | `/llm/chat`、`/llm/user/register` | | |
| SEC-003 | 過期 JWT | P1 | 所有需認證端點 | | |
| SEC-004 | 竄改 JWT Payload | P1 | 所有需認證端點 | | |
| SEC-005 | 水平越權 | P1 | `/llm/user/get`、`/llm/mcpServer/getSetting` | | |
| SEC-006 | 大量請求壓力 | P2 | `/llm/chat` | | |

---

## 問題追蹤區

> 所有 ❌ Fail 項目請在此登記，格式如下：

| 測試 ID | 發現日期 | 問題描述 | 重現步驟 | 預期結果 | 實際結果 | 嚴重度 | 負責人 | 狀態 |
|---------|---------|---------|---------|---------|---------|--------|--------|------|
| | | | | | | | | |

---

*最後更新：2026-04-04*
