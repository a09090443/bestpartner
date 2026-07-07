# API 測試計畫

> 本文件為 BestPartner API 測試計畫，涵蓋 13 個模組、66 個 endpoint。
> 端點清單與權限規格見 `.claude/rules/api-endpoints.md`；執行測試週期須透過 `test-confirmation` skill（見 `.claude/rules/api-testing.md`）。
> Postman Collection：`docs/postman/basepartner.postman_collection.json`。

## 優先順序定義

| 等級 | 說明 |
|------|------|
| P0 | 核心阻斷性功能，失敗即無法繼續測試 |
| P1 | 主要業務功能，直接影響使用者體驗 |
| P2 | 次要功能、邊界條件、錯誤處理細節 |

## 測試前置

1. PostgreSQL（`localhost:5432/pgdb`）已初始化（`docs/sql/bestpartner-ddl.sql` + `bestpartner-init-data.sql`）
2. 服務以 dev profile 啟動於 port 80
3. 以 `admin` / `admin` 登入取得 JWT token（P0；此步失敗全部阻斷）

---

## AUTH - `/login`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | 正確帳密登入 | 200，回傳 JWT token |
| P1 | 錯誤密碼登入 | 401 / 業務錯誤訊息 |
| P1 | `/login/check` 帶有效 token | 200 |
| P2 | `/login/check` 帶過期或偽造 token | 401 |

## USER - `/llm/user`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | register 新用戶 | 成功建立，可登入 |
| P1 | get / update 當前用戶資訊 | 資料正確回寫 |
| P2 | switchStatus / delete 權限檢核（非 admin 拒絕） | 403 |

## LLM SETTING - `/llm/setting`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | save 建立 CHAT 模型設定（含 platformId、modelType、modelName） | 成功，回傳 id |
| P1 | get 依 platformId / llmId 篩選 | 清單正確 |
| P1 | update / delete | 生效 |
| P2 | platform/add、platform/delete 非 admin 拒絕 | 403 |

## CHAT - `/llm`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | chat 同步聊天（有效 llmId，modelType=CHAT） | 回傳 AI 完整回覆 |
| P1 | chatStreaming SSE 串流 | 逐段回傳 |
| P1 | customAssistantChat 掛 Tool / MCP / Skill / RAG | 整合生效 |
| P1 | uploadFile multipart 上傳 | 回傳檔名清單 |
| P2 | llmId 指向 STREAMING_CHAT 類型呼叫同步端點 | 業務錯誤（型別不符） |

## ADMIN CHAT - `/llm/admin`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | admin 角色呼叫 chat（依 platform 用系統預設模型） | 成功 |
| P2 | 非 admin 呼叫 | 403 |

## VECTOR - `/llm/vector`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | save 建立向量庫設定（collectionName、dimension） | 成功 |
| P1 | uploadFiles 上傳文件並 embedding | 知識庫可查得 |
| P1 | getDataFromEmbeddingStore 相似度搜尋 | 回傳相關片段 |
| P2 | deleteData 依 knowledgeId / docIds 刪除 | 資料移除 |

## SKILL - `/llm/skill`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | upload 個人 skill zip（含 skill.md） | 解壓至用戶目錄，list 可見 |
| P1 | get 讀取內容（含 resources） | 內容正確 |
| P2 | global/upload 非 admin 拒絕；delete 他人 skill 拒絕 | 403 |

## TOOL - `/llm/tool`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | list 列出內建工具 | 含 GoogleSearch、Tavily、DateTool、Text2SQL |
| P1 | **list / get 回傳 `settingSchema`**：GoogleSearch 的 `apiKey` 為 `{type:"string", required:true, sensitive:true}`；DateTool（無 config class）為 null | 結構化 schema 正確（wire format 為純值，非物件包裝） |
| P1 | saveSetting（id + alias）→ updateSetting（settingId + settingContent） | settingId 回傳並可更新內容 |
| P2 | register / delete / category CRUD 非 admin 拒絕 | 403 |

## MCP SERVER - `/llm/mcpServer`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | register STDIO 類型（command + args）與 SSE 類型（server） | 成功，list 可見 |
| P1 | saveSetting / getSetting / updateSetting / deleteSetting 個人設定 CRUD | 生效 |
| P2 | register / update / delete 非 admin 拒絕 | 403 |

## PERMISSION - `/llm/permission`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P2 | add / update / delete（admin） | 生效 |
| P2 | 非 admin 呼叫 | 403 |

## WORKFLOW - `/llm/workflow`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | create 建立空白 workflow | DRAFT、version 1 |
| P0 | save 合法 nodes/edges 整張覆寫 | version+1，get 可還原 |
| P1 | **save 節點 config 型別驗證**：config 含未知欄位（如 `bogusField`）或結構性型別錯誤（`toolIds` 給字串） | 400，`workflow.node.config.invalid`，訊息含 nodeKey |
| P1 | **save 允許草稿缺必填**：LLM_ASSISTANT 缺 `llmId` 仍可存 DRAFT | 200 |
| P1 | **switchStatus 啟用驗必填**：節點缺必填（如 `llmId`）時啟用 | 400，`workflow.node.config.required.missing`，訊息含 nodeKey 與欄位 |
| P1 | switchStatus 啟用前置：無 TRIGGER 節點或圖有環 | 400 對應訊息 |
| P1 | TRIGGER 的 `triggerType` 非法 enum 值（非 MANUAL/WEBHOOK/CRON） | save 即 400 |
| P1 | save 樂觀鎖：version 不符 | 400，`workflow.version.conflict` |
| P2 | 圖驗證：nodeKey 重複、edge 端點不存在、節點數超上限 | 400 對應訊息 |
| P2 | 非擁有者存取（非 admin） | 403，`workflow.forbidden` |
| P2 | nodeRequiredFields 取必填清單 | 200，含全 10 種 NodeType；`LLM_ASSISTANT=["llmId"]`、`DATA_TRANSFORM=["mappings\|template"]` |

## SYSTEM SETTING - `/systemSetting`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P2 | list / get / add / update / delete | CRUD 正常 |

## VIEW - `/view`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P2 | GET /view/chat 回傳聊天測試頁 HTML | 200，可對 chatStreaming 測試 |

---

## 維護規則

- 新增或修改 endpoint 時，本文件與 `.claude/rules/api-endpoints.md`、Postman Collection 須同步更新（由 `documentation-sync` skill 把關）
- 測試執行紀錄寫入 `docs/test-confirmations/test-confirmation-YYYYMMDDHHmm.md`（由 `test-confirmation` skill 產生）
