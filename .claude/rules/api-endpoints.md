# API Endpoints 說明

> 共 13 個模組、68 個 endpoint。
> 權限標示：無標示 = 公開或依類別預設；括號內為額外限制。

---

## AUTH - `/login`

| HTTP | 路徑 | 說明 | 權限 |
|------|------|------|------|
| POST | `/login/` | 用戶登入，傳入 email + password，回傳 JWT token | 公開（@PermitAll） |
| POST | `/login/check` | 驗證目前 JWT token 是否有效，回傳 check 字串 | @RolesAllowed("user-read") |

---

## CHAT - `/llm`

> 類別層級：`@Authenticated`（所有 endpoint 皆需登入）

| HTTP | 路徑 | 說明 | 額外權限 |
|------|------|------|---------|
| POST | `/llm/chat` | 同步聊天，傳入 llmId + message，回傳 AI 完整回覆 | - |
| POST | `/llm/chatStreaming` | 串流聊天（SSE），以 text/plain 逐步回傳 AI 回覆 | - |
| POST | `/llm/customAssistantChat` | 自定義助手同步聊天，支援 Memory、Tool、MCP Server、Skill 整合 | - |
| POST | `/llm/customAssistantChatStreaming` | 自定義助手串流聊天（SSE），支援 Memory、Tool、MCP Server、Skill 整合 | - |
| POST | `/llm/uploadFile` | 上傳本地檔案（multipart/form-data），回傳上傳成功的檔案名稱清單 | - |

---

## ADMIN CHAT - `/llm/admin`

> 類別層級：`@RolesAllowed("admin")`（僅限管理員）

| HTTP | 路徑 | 說明 |
|------|------|------|
| POST | `/llm/admin/chat` | 管理員同步聊天，使用系統預設 ChatModel（依 platform 指定），不需 llmId |
| POST | `/llm/admin/customAssistantChat` | 管理員串流聊天，使用系統預設 StreamingChatModel |

---

## USER - `/llm/user`

| HTTP | 路徑 | 說明 | 權限 |
|------|------|------|------|
| POST | `/llm/user/register` | 新用戶自行註冊，傳入 username + password + email | 公開（無認證需求） |
| POST | `/llm/user/get` | 取得目前登入用戶的個人資訊 | @Authenticated |
| POST | `/llm/user/update` | 更新目前登入用戶資訊（email、status 等） | @Authenticated |
| POST | `/llm/user/switchStatus` | 切換指定用戶的啟用 / 停用狀態 | @RolesAllowed("all") |
| DELETE | `/llm/user/delete` | 刪除指定用戶 | @RolesAllowed("all") |

---

## LLM SETTING - `/llm/setting`

> 類別層級：`@Authenticated`（所有 endpoint 皆需登入）

| HTTP | 路徑 | 說明 | 額外權限 |
|------|------|------|---------|
| POST | `/llm/setting/get` | 查詢當前用戶的 LLM 設定，可依 platformId 或 llmId 篩選；`llmModel.apiKey` 回傳遮罩 `__SECRET_KEPT__`（明文不外流） | - |
| POST | `/llm/setting/save` | 新增 LLM 設定（須指定 platformId、modelType、modelName）；回應中的 `apiKey` 同樣遮罩 | - |
| POST | `/llm/setting/update` | 更新現有 LLM 設定（須傳入 id）；`apiKey` 為 `__SECRET_KEPT__` 時沿用既有金鑰 | - |
| POST | `/llm/setting/delete` | 刪除 LLM 設定（須傳入 id） | - |
| POST | `/llm/setting/platform/add` | 新增 AI 平台設定 | @RolesAllowed("admin") |
| POST | `/llm/setting/platform/delete` | 刪除 AI 平台設定（須傳入 id） | @RolesAllowed("admin") |

---

## VECTOR - `/llm/vector`

> 類別層級：`@Authenticated`（所有 endpoint 皆需登入）

| HTTP | 路徑 | 說明 |
|------|------|------|
| POST | `/llm/vector/save` | 新增向量資料庫設定（須指定 collectionName、dimension） |
| POST | `/llm/vector/update` | 更新向量資料庫設定（須傳入 id） |
| POST | `/llm/vector/getKnowledgeStore` | 依 knowledgeId 查詢知識庫清單 |
| POST | `/llm/vector/uploadFiles` | 上傳文件並執行 embedding 存入向量資料庫（multipart/form-data，須傳入 embeddingModelId + embeddingStoreId） |
| POST | `/llm/vector/getDataFromEmbeddingStore` | 向量相似度搜尋，依 knowledgeId + content 搜尋相關片段 |
| DELETE | `/llm/vector/deleteData` | 刪除向量資料（依 knowledgeId，可指定 docIds） |

---

## TOOL - `/llm/tool`

| HTTP | 路徑 | 說明 | 權限 |
|------|------|------|------|
| GET | `/llm/tool/list` | 列出所有已註冊工具及其分類（每筆含 `settingSchema`：由 configObjectPath 反射產生的設定欄位結構，無 config class 者為 null） | 公開 |
| POST | `/llm/tool/get` | 取得特定工具詳細資訊（須傳入 id；回傳含 `settingSchema` 與舊版 `settingArgs`） | 公開 |
| POST | `/llm/tool/register` | 註冊新工具（須傳入 name、classPath、groupId、type；CUSTOMIZE 類型須另加 functionName、functionDescription） | @RolesAllowed("admin") |
| POST | `/llm/tool/delete` | 刪除工具（須傳入 id） | @RolesAllowed("admin") |
| POST | `/llm/tool/saveSetting` | 儲存用戶工具設定（須傳入 id + alias）；settingContent 中被 `@ToolConfigField(sensitive=true)` 標記的欄位（如 apiKey）加密落地 | 公開 |
| POST | `/llm/tool/updateSetting` | 更新用戶工具設定（須傳入 settingId + settingContent）；敏感欄位值為 `__SECRET_KEPT__` 時沿用既有密文 | 公開 |
| POST | `/llm/tool/category/save` | 新增工具分類（須傳入 group） | @RolesAllowed("admin") |
| POST | `/llm/tool/category/update` | 更新工具分類（須傳入 groupId + group） | @RolesAllowed("admin") |
| POST | `/llm/tool/category/delete` | 刪除工具分類（須傳入 groupId） | @RolesAllowed("admin") |

---

## SKILL - `/llm/skill`

> 類別層級：`@Authenticated`（所有 endpoint 皆需登入）

Skill 以 `.zip` 壓縮檔上傳，解壓後存於伺服器使用者專屬目錄（`{upload_dir}/skills/{userId}/{skillName}/`）。zip 檔名（去掉 `.zip`）為 skillName，`skill.md` 為必要主內容，其餘檔案為 resources。

| HTTP | 路徑 | 說明 | 額外權限 |
|------|------|------|---------|
| GET | `/llm/skill/list` | 列出當前使用者的所有 Skill（含 global skills，`isGlobal` 欄位區分） | - |
| POST | `/llm/skill/get` | 取得特定 Skill 詳細資訊（含 resources 內容；可讀自己的或 global skill，須傳入 id） | - |
| POST | `/llm/skill/upload` | 上傳個人 Skill zip 壓縮檔（multipart/form-data；file 必填、description 選填；同名自動覆蓋） | - |
| POST | `/llm/skill/delete` | 刪除個人 Skill（須傳入 id；只能刪除自己的 Skill，同時刪除伺服器目錄） | - |
| POST | `/llm/skill/global/upload` | 上傳 Global Skill zip 壓縮檔（所有使用者皆可讀取；multipart/form-data） | @RolesAllowed("admin") |
| POST | `/llm/skill/global/delete` | 刪除 Global Skill（須傳入 id） | @RolesAllowed("admin") |

---

## MCP SERVER - `/llm/mcpServer`

> 類別層級：`@Authenticated`（所有 endpoint 皆需登入）

| HTTP | 路徑 | 說明 | 額外權限 |
|------|------|------|---------|
| GET | `/llm/mcpServer/list` | 列出所有已註冊的 MCP 伺服器 | - |
| POST | `/llm/mcpServer/get` | 取得特定 MCP 伺服器詳細資訊（須傳入 mcpId） | - |
| POST | `/llm/mcpServer/register` | 註冊新 MCP 伺服器（STDIO 類型需 command + args；SSE 類型需 server） | @RolesAllowed("admin") |
| POST | `/llm/mcpServer/update` | 更新 MCP 伺服器設定 | @RolesAllowed("admin") |
| POST | `/llm/mcpServer/delete` | 刪除 MCP 伺服器（須傳入 mcpId） | @RolesAllowed("admin") |
| POST | `/llm/mcpServer/saveSetting` | 儲存當前用戶的 MCP 個人設定（須傳入 mcpId + settingContent）；`settingContent` 整欄 AES-GCM 加密落地（密文文字、非 JSON） | - |
| POST | `/llm/mcpServer/getSetting` | 取得當前用戶的 MCP 個人設定（須傳入 userSettingId）；env 分類的值回傳遮罩 `__SECRET_KEPT__`（明文不外流） | - |
| POST | `/llm/mcpServer/updateSetting` | 更新當前用戶的 MCP 個人設定（須傳入 userSettingId + settingContent）；env 值為 `__SECRET_KEPT__` 時沿用既有密文 | - |
| DELETE | `/llm/mcpServer/deleteSetting` | 刪除當前用戶的 MCP 個人設定（須傳入 userSettingId） | - |

> **設定類端點的擁有權檢核**：`getSetting` / `updateSetting` / `deleteSetting` 三者皆以
> 「當前使用者 ＋ userSettingId」查詢，只能操作自己的設定。
> 目標設定不存在**或屬於他人**時，一律回 400 `User setting not found`——
> 兩種情況回應相同，不提供判斷設定是否存在的依據。

---

## PERMISSION - `/llm/permission`

> 類別層級：`@RolesAllowed("admin")`（僅限管理員）

| HTTP | 路徑 | 說明 |
|------|------|------|
| POST | `/llm/permission/add` | 新增權限定義（須傳入 name + num） |
| POST | `/llm/permission/update` | 更新權限定義（須傳入 id + name + num） |
| DELETE | `/llm/permission/delete` | 刪除權限（須傳入 id） |

---

## WORKFLOW - `/llm/workflow`

> 類別層級：`@Authenticated`（所有 endpoint 皆需登入）。擁有權與 admin 例外於 service 層檢核。

| HTTP | 路徑 | 說明 |
|------|------|------|
| POST | `/llm/workflow/create` | 建立空白 workflow（狀態 DRAFT、版本 1），須傳入 name |
| POST | `/llm/workflow/save` | 新增或整張覆寫 workflow（含 nodes/edges）；更新時以 version 樂觀鎖檢核；存檔前驗證畫布（nodeKey 唯一、edge 端點存在、節點數上限、無環）與節點 config 型別（依 NodeType 強型別反序列化，未知欄位或結構性型別錯誤回 400；必填缺席放行）；`HTTP_REQUEST` 的 `secretHeaders` 逐值加密落地，值為 `__SECRET_KEPT__` 時沿用既有密文 |
| POST | `/llm/workflow/get` | 取得單一 workflow 完整定義（含 nodes/edges），須傳入 id；`HTTP_REQUEST` 節點的 `secretHeaders` 值回傳遮罩 `__SECRET_KEPT__`（明文不外流） |
| GET | `/llm/workflow/list` | 列出當前使用者擁有的 workflow 摘要清單 |
| GET | `/llm/workflow/nodeRequiredFields` | 回傳各 NodeType 的必填欄位清單（源自 NodeConfig 契約），供前端載入時即時驗證節點設定；複合字樣 `a\|b` 表示擇一必填（例：`LLM_ASSISTANT`→`llmId`、`PROMPT`→`prompt`、`SKILL`→`skillId`）；圖層級的條件必填（如 LLM 的提問來源）不在此清單 |
| POST | `/llm/workflow/update` | 僅更新 meta（name/description/canvasMeta），須傳入 id |
| POST | `/llm/workflow/delete` | 刪除 workflow（連鎖刪 node/edge），須傳入 id |
| POST | `/llm/workflow/switchStatus` | 啟用/停用 workflow（啟用前須具備 Trigger 節點、圖無環、逐節點驗 config 必填欄位——驗不過回報 nodeKey 與缺漏欄位，且不得有孤兒 SKILL 節點（未掛載到任何 LLM `in:tool` 埠）→ 回 `workflow.skill.node.not.mounted`、不得有孤兒 PROMPT 節點（未連到任何 LLM `in:prompt` 埠）→ 回 `workflow.prompt.node.not.connected`、每個 LLM 節點須有提問來源（`userPrompt` 或連入的 PROMPT 節點）→ 否則回 `workflow.llm.prompt.required`，三者訊息皆含 nodeKey；停用不跑圖驗證），須傳入 id + active |
| POST | `/llm/workflow/execute` | 手動執行 workflow（SSE 逐節點事件流：execution.started / node.started / node.completed / node.failed / execution.completed），須傳入 id，可帶 inputPayload |

---

## SYSTEM SETTING - `/systemSetting`

| HTTP | 路徑 | 說明 | 權限 |
|------|------|------|------|
| GET | `/systemSetting/list` | 列出所有系統設定 | 公開 |
| POST | `/systemSetting/get` | 依 key 取得特定系統設定值 | 公開 |
| POST | `/systemSetting/add` | 新增系統設定（須傳入 value） | 公開 |
| POST | `/systemSetting/update` | 更新系統設定（須傳入 value） | 公開 |
| DELETE | `/systemSetting/delete` | 刪除系統設定（須傳入 id） | 公開 |

---

## VIEW - `/view`

| HTTP | 路徑 | 說明 | 權限 |
|------|------|------|------|
| GET | `/view/chat` | 回傳內建聊天測試頁面（HTML），呼叫 `/llm/chatStreaming` 進行串流測試 | 公開 |
