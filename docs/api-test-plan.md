# API 測試計畫

> 本文件為 BestPartner API 測試計畫，涵蓋 13 個模組、68 個 endpoint。
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
| P1 | **敏感欄位加密**：saveSetting GoogleSearch 帶明文 apiKey，查 DB `llm_tool_user_setting.setting_content` | apiKey 為 `{iv}$…` 密文、不含明文；csi/timeout 仍明文 |
| P1 | **敏感欄位沿用**：updateSetting apiKey 送 `__SECRET_KEPT__` 並改 csi | apiKey 沿用既有密文、csi 更新、無遮罩字面值落地 |
| P2 | register / delete / category CRUD 非 admin 拒絕 | 403 |

## MCP SERVER - `/llm/mcpServer`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | register STDIO 類型（command + args）與 SSE 類型（server） | 成功，list 可見 |
| P1 | saveSetting / getSetting / updateSetting / deleteSetting 個人設定 CRUD | 生效 |
| P1 | **setting_content 整欄加密**：saveSetting 帶 env 值（如 CREDENTIALS_FILE_PATH）與 args，查 DB `llm_mcp_user_setting.setting_content` | 整欄為 `{iv}${encrypted}` 密文文字（非 JSON），不含任何明文（含 env 值與 args） |
| P1 | **getSetting 遮罩**：對上述設定 getSetting | env 值回傳 `__SECRET_KEPT__`，明文不外流 |
| P1 | **env 值沿用**：updateSetting env 送 `__SECRET_KEPT__` | 沿用既有值、無遮罩字面值落地 |
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
| P1 | **switchStatus 啟用驗提示接線**：孤兒 PROMPT 節點（未連任何 LLM `in:prompt`） | 400，`workflow.prompt.node.not.connected`，訊息含 nodeKey；狀態仍 DRAFT |
| P1 | **switchStatus 啟用驗提問來源**：LLM 節點既無 `userPrompt` 也無 PROMPT 連入 `in:prompt` | 400，`workflow.llm.prompt.required`，訊息含 nodeKey；對照組（有其一）啟用成功 |
| P1 | TRIGGER 的 `triggerType` 非法 enum 值（非 MANUAL/WEBHOOK/CRON） | save 即 400 |
| P1 | save 樂觀鎖：version 不符 | 400，`workflow.version.conflict` |
| P2 | 圖驗證：nodeKey 重複、edge 端點不存在、節點數超上限 | 400 對應訊息 |
| P2 | 非擁有者存取（非 admin） | 403，`workflow.forbidden` |
| P2 | nodeRequiredFields 取必填清單 | 200，含全 13 種 NodeType；`LLM_ASSISTANT=["llmId"]`、`PROMPT=["prompt"]`、`DATA_TRANSFORM=["mappings\|template"]`、`OUTPUT=["template\|mappings"]`（圖層級條件必填不在此清單） |
| P0 | **execute happy path**：已存檔 workflow（TRIGGER→…→OUTPUT，DRAFT 即可、不需 ACTIVE）呼叫 execute | SSE 事件流依序 `execution.started` → `node.started`/`node.completed` → `execution.completed`（SUCCESS）；紀錄寫入 `llm_workflow_execution` / `llm_workflow_node_execution` |
| P1 | execute 帶不存在的 id | 400，`workflow.not.found` |
| P1 | execute 未帶 token（未認證） | 401 |
| P2 | execute 中途 client 斷線 | 執行標記 `CANCELLED`，未執行的下游節點標記 `SKIPPED` |

### 多觸發點各自獨立執行（`triggerNodeKey`）

> 契約見 `docs/workflow-engine/system-design.md` §2.1.1；實作為 `WorkflowEngine` 主遍歷的活化閘門。
> DB 查核指令：`SELECT trigger_node_key, output_result FROM bestpartner.llm_workflow_execution ORDER BY started_at DESC LIMIT 1;`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P0 | **指定觸發點只跑該分支**：畫布 t1→A、t2→B，execute 帶 `triggerNodeKey="t1"` | SSE 只出現 t1／A 的 `node.started`/`node.completed`，**完全無** t2／B 的事件；`llm_workflow_node_execution` 中 t2 與 B 各一筆 `SKIPPED`；整體 SUCCESS |
| P0 | **匯流的同一顆 LLM 取被選中分支的提問**：t1→promptA→LLM(`in:prompt`)、t2→promptB→LLM，指定 t1 | LLM 恰執行一次且 SUCCESS，回覆對應 promptA 的提問；promptB 為 `SKIPPED` |
| P1 | **未指定時全部觸發點皆執行**（向後相容）：同上圖形省略 `triggerNodeKey` | t1 與 t2 皆 `node.completed`；`trigger_node_key` 為 NULL |
| P1 | execute 帶不存在的 `triggerNodeKey` | 400，`workflow.trigger.node.not.found`，訊息含該 nodeKey；**不建立**執行紀錄 |
| P1 | execute 帶非 TRIGGER 型別的 nodeKey（如 TOOL 節點） | 400，`workflow.trigger.node.invalid`，訊息含 nodeKey 與實際型別；**不建立**執行紀錄 |
| P1 | **執行紀錄記錄發起入口**：指定 t1 後查 DB | `llm_workflow_execution.trigger_node_key` = `t1` |
| P2 | `triggerNodeKey` 傳空字串或空白 | 視同未指定，不報錯且所有 TRIGGER 皆執行 |
| P2 | **最終輸出形狀**：t1→outA、t2→outB，指定 t1 | `output_result` 為 outA 的 map 本身（非 `{outA:…, outB:…}` 的 nodeKey 合併形狀） |
| P2 | **未選中分支的 LOOP 子圖不落紀錄**：t2→LOOP→(`out:loop`)B1，指定 t1 | LOOP 節點為 `SKIPPED`，子圖節點 B1 **零筆**紀錄 |

### HTTP_REQUEST 機密欄位（`secretHeaders`）

> 契約見 `docs/workflow-engine/system-design.md` §2.9；實作為 `converter/WorkflowSecretConverter.kt`。
> DB 查核指令：`SELECT config FROM bestpartner.llm_workflow_node WHERE node_key = '<nodeKey>';`

| 優先 | 案例 | 預期 |
|:---:|------|------|
| P1 | **save 加密落地**：HTTP_REQUEST 節點帶 `secretHeaders: {"Authorization": "Bearer sk-test-plain"}` 存檔後直接查 DB | `config` 內該值為 `{iv}${encrypted}` 格式密文，**不含明文** `sk-test-plain`；`method` / `url` / `headers` 仍為明文 |
| P1 | **get 回傳遮罩**：對上述 workflow 呼叫 get | `secretHeaders.Authorization` 為 `__SECRET_KEPT__`，明文與密文皆不出現 |
| P1 | **sentinel 沿用**：把 get 的回應原樣送回 save（值仍為 `__SECRET_KEPT__`） | 200；DB 密文與前次相同（未被覆寫），execute 仍能以原金鑰送出 |
| P1 | **改金鑰**：save 時填入新明文 `Bearer sk-new` | DB 密文更新，execute 送出的 header 為 `Bearer sk-new` |
| P1 | **execute 送出明文**：以 mock server 接收請求 | 收到的 `Authorization` 為解密後明文，非密文亦非遮罩 |
| P1 | **執行紀錄不外洩**：execute 後查 `llm_workflow_node_execution.input` | 其中 `config.secretHeaders` 為 `__SECRET_KEPT__`，無明文 |
| P2 | **改 header 名稱邊界**：將 key 由 `Authorization` 改為 `X-Auth` 但值仍送 `__SECRET_KEPT__` | 該欄位被移除（遮罩字面值不得存入 DB） |
| P2 | **舊明文相容**：手動以 SQL 將 `secretHeaders` 值改為明文後 execute | 執行正常（原樣使用）；再 save 一次後查 DB 已轉為密文 |

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
