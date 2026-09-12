# BestPartner E2E 測試確認表（UI-driven）

> 本文件為 E2E 測試記錄模板，每次測試週期開始前由 `e2e-test-confirmation` skill 複製並填寫。
> 旅程與案例定義的權威來源：`docs/e2e-test-plan.md`。

---

## 使用說明

### 狀態符號

| 符號 | 意義 |
|------|------|
| ✅ | Pass — 案例通過 |
| ❌ | Fail — 案例失敗（請在「問題追蹤區」補充說明） |
| ⏭️ | Skip — 本次略過（請在證據／備註欄說明原因，如缺 E2E_LLM_ID） |
| — | 本次週期不適用 |

### 填寫流程

1. 複製此檔案，重新命名為 `docs/test-confirmations/e2e-test-confirmation-YYYYMMDDHHmm.md`
2. 填寫「測試週期資訊」
3. 完成「前置檢查清單」（任一失敗即停止）
4. 依 **Step 1.5 選定範圍**逐旅程執行（J 編號由小到大），**先記錄證據再標狀態**；未選旅程整段標 ⏭️「本次範圍外」
5. 每條旅程完成後立即更新「測試結果摘要」與 Pass 率
6. 所有 ❌ 項目在「問題追蹤區」建立記錄，並在「問題分流與修正紀錄」完成前端／後端分流
7. 若使用者核准修正：修完重跑選定範圍全部旅程，結果填入「第二輪重測結果」
8. 收尾清理 `e2e-*` 測試資料、填寫「測試結束時間」

### 證據要求

每個案例的「證據 / 備註」欄**必須**包含（不得留佔位符）：

1. **逐步圖片連結（強制，用 Markdown 圖片語法）**：案例每一個操作步驟一張截圖，**在證據欄以圖片連結依序嵌入**，讓報告可直接預覽／點開，不得只放最終畫面、不得只填純文字路徑。
   命名／存放：`docs/test-confirmations/e2e-shots/<報告時間戳>/<caseId>-<步驟序號>-<簡述>.png`
   證據欄寫法（相對報告檔路徑）：
   ```markdown
   ![J1-02-01 login-page](e2e-shots/202607111530/J1-02-01-login-page.png)
   ![J1-02-02 submit](e2e-shots/202607111530/J1-02-02-submit.png)
   ![J1-02-03 redirected-list](e2e-shots/202607111530/J1-02-03-redirected-list.png)
   ```
2. 另可附：Playwright trace / 錄影檔連結（如 `[trace](test-results/j3/trace.zip)`）、關鍵斷言（如「節點數 3、edge 2、save 後 version=2」）。
3. J5 專用：SSE 事件序列與最終節點狀態（如 `started→node.completed×3→completed(SUCCESS)`），並以圖片連結附各節點狀態轉移截圖。

> ⚠️ 缺少任一步驟圖片連結、或只填純文字路徑未用圖片語法，即視同證據不完整。失敗（❌）案例另附失敗當下截圖與 trace 連結。

---

## 測試週期資訊

| 項目 | 內容 |
|------|------|
| 測試日期 | 2026-08-20 21:39 |
| 服務版本 | 0.1.8-SNAPSHOT |
| runTag（＝報告時間戳） | 202608202139 |
| **本次測試範圍（選定旅程）** | **J11 / J12 / J13 / J14**（26 案例）——`docs/e2e-test-plan.md` 於 2026-08-20 新增的四條資料類節點旅程，**首次實測** |
| **未選旅程與原因** | J1–J10（92 案例）：本次聚焦新增旅程的首跑驗證，既有旅程已於週期 202608192201 全數實測通過，暫不重跑 |
| **測試輪次** | R1（修正後重測則追加 R2、R3） |
| 測試環境 | **dev**（後端 dev profile，port 80） |
| 瀏覽器 | **chromium** |
| 前端 baseURL | `http://localhost:4173`（`npm run preview`） |
| 後端 API URL | `http://localhost:80` |
| LLM 平台（J5） | — （J5 本次範圍外；**J11–J14 全程無 LLM 呼叫**，不需 api_key） |
| E2E_LLM_ID（J5） | — （同上，不適用） |
| 登入身分（設定擁有者） | `admin@bestpartner.com.tw`，userId `c88f57c8-ad26-4ea0-9f71-a65995b49357`（JWT `upn`；groups: admin / user-write / user-read）<br>Step 4.5 smoke：`POST /login/` 200、`POST /login/check` 200、`GET /llm/workflow/list` 200。**J11–J14 不需 LLM / MCP / tool / skill 資源**，故無其他 ID 需解析。<br>⚠️ JWT 有效期 1800s，長時間執行需重新登入 |
| J10 資源 ID（mcp/userSetting/tool/toolSetting/skill） | — （J10 本次範圍外） |
| 測試結束時間 | 2026-08-20 22:26（R1 單輪完成，無需第二輪） |

---

## 前置檢查清單（測試前必須全部 ✅）

```
□ 已完成 Step 1.5 旅程範圍選擇（hook 狀態檔 journeys 非空）
□ 開測前已停掉舊服務並通過實測（phase=services-stopped；未通過無法呼叫 webwright）
□ 後端服務於 port 80 健康回應（HTTP 200）
□ PostgreSQL 已初始化（bestpartner-ddl.sql + bestpartner-init-data.sql）
□ 種子資料存在：admin/admin、test_user 及其 CHAT/STREAMING_CHAT LLM setting
□ J5 可用：具有效 api_key 的 LLM setting（填入 E2E_LLM_ID），否則 J5 真實案例標 ⏭️
□ J8 可用：Milvus 容器運行中，且 `getDataFromEmbeddingStore` 對目標知識庫**實際命中**（回空即不可用）
   ⚠️ 勿只看 `getKnowledgeStore` 列得出知識庫——metadata 在 Postgres、向量在 Milvus，兩者會不同步
□ J11/J12 可用：`E2E_HTTP_TEST_URL` 可通（預設本機 `/systemSetting/list`，服務已啟動即可）
□ J14 已知風險：J14-04／J14-05 具破壞性，須排在該旅程最後，跑完以 `/view/chat` 確認服務存活
□ J10 可用：google_map MCP jar 可啟動（`java -jar` 不報 manifest 錯）、google_map userSetting 與
   TavilySearch toolSetting 皆存在且金鑰有效、pdf skill 已上傳，且四者與 LLM setting **同屬登入者**
   ⚠️ D:/MCP 的 jar 須為 `build/*-runner.jar`（uber-jar），誤放 `build/libs/*.jar`（thin jar）會無 Main-Class
□ 前端已 build 並可由 baseURL 存取
□ Playwright 瀏覽器已安裝（npx playwright install）
□ storageState 已由 API 登入（admin/admin）產生
```

> ⚠️ 任一項未通過即停止測試並回報。

---

## 測試結果摘要

> 每完成一條旅程立即更新本表

| 旅程 | 優先 | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|------|:---:|------|--------|--------|---------|---------|
| J1 認證 | P0/P1/P2 | 9 | 0 | 0 | 9 | — |
| J2 Workflow 列表 | P0/P1 | 4 | 0 | 0 | 4 | — |
| J3 畫布編輯 | P0/P1 | 9 | 0 | 0 | 9 | — |
| J4 驗證與啟用 | P1 | 12 | 0 | 0 | 12 | — |
| J5 執行（真實 LLM） | P0/P1 | 12 | 0 | 0 | 12 | — |
| J6 Inspector 表單 | P0/P1/P2 | 9 | 0 | 0 | 9 | — |
| J7 契約錯誤 | P2 | 2 | 0 | 0 | 2 | — |
| J8-A 知識庫 RAG（pipeline） | P1 | 5 | 0 | 0 | 5 | — |
| J8-B 知識庫 RAG（外掛） | P1 | 5 | 0 | 0 | 5 | — |
| J9 編輯器外觀與節點編輯頁 | P0/P1/P2 | 17 | 0 | 0 | 17 | — |
| J10 複合能力掛載（旅遊行程規劃） | P1/P2 | 8 | 0 | 0 | 8 | — |
| J11 純資料管線（無 LLM，確定性） | P0/P1/P2 | 8 | 8 | 0 | 0 | **100%** |
| J12 迴圈批次處理（LOOP） | P1/P2 | 7 | 7 | 0 | 0 | **100%** |
| J13 條件分流資料管線（CONDITION） | P1/P2 | 5 | 5 | 0 | 0 | **100%** |
| J14 CODE 節點沙箱（含安全性） | P1/P2 | 6 | 6 | 0 | 0 | **100%** |
| **合計** | | **118** | **26** | **0** | **92** | **選定範圍 26/26 = 100%** |

> 未選旅程（J1–J10，92 案例）整段標 ⏭️「本次範圍外」，不計入 Pass 率分母。
> **選定範圍（J11–J14）26 個案例全數通過，R1 單輪完成，無需第二輪重測。**

### 通過標準

| 優先級 | 通過標準 | 說明 |
|--------|--------|------|
| P0 | 100% | 任何失敗均為 Release Blocker |
| P1 | ≥ 95% | 已知失敗需有追蹤 Issue |
| P2 | ≥ 80% | 剩餘列為 Tech Debt |

---

## 旅程測試清單

> 案例定義同步自 `docs/e2e-test-plan.md §3`。狀態與證據欄由測試時填寫。

### J1 認證流程（P0）

> ⏭️ **本次範圍外**（Step 1.5 選定範圍為 J11–J14）。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J1-01 | P0 | 未登入直接訪 `/` | 導向 `/login` | ⏭️ | 本次範圍外 |
| J1-02 | P0 | admin/admin 登入 | 導向列表 `/`，顯示已登入 | ⏭️ | 本次範圍外 |
| J1-03 | P1 | 已登入訪 `/login` | 導回首頁 `/` | ⏭️ | 本次範圍外 |
| J1-04 | P1 | 錯誤帳密登入 | 停留 `/login`，顯示錯誤 | ⏭️ | 本次範圍外 |
| J1-05 | P1 | 列表頁登出（`logout-button`） | 清 token 導向 `/login`；重訪受保護頁被導回登入 | ⏭️ | 本次範圍外 |
| J1-06 | P1 | 編輯器工具列登出（無未存，`logout-button`） | 清 token 導向 `/login`；per-user 快取一併清除 | ⏭️ | 本次範圍外 |
| J1-07 | P1 | 編輯器有未存變更登出 → 確認框選「登出」 | 先跳「尚未存檔」確認；確認後清 token 導向 `/login` | ⏭️ | 本次範圍外 |
| J1-08 | P1 | 編輯器有未存變更登出 → 確認框選「取消」 | 留在編輯器且仍為登入態（token 未清） | ⏭️ | 本次範圍外 |
| J1-09 | P2 | token 清除後訪受保護頁 | 導回 `/login` | ⏭️ | 本次範圍外 |

### J2 Workflow 列表（P0/P1）

> ⏭️ **本次範圍外**（Step 1.5 選定範圍為 J11–J14）。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J2-01 | P0 | 列表載入 | 顯示既有 workflow 清單 | ⏭️ | 本次範圍外 |
| J2-02 | P0 | 建立新 workflow | 進 `/editor/:id`，DRAFT、version 1 | ⏭️ | 本次範圍外 |
| J2-03 | P1 | 點列表項進編輯器 | 載入完整定義 | ⏭️ | 本次範圍外 |
| J2-04 | P1 | 刪除 workflow | 列表移除，連鎖刪 node/edge | ⏭️ | 本次範圍外 |

### J3 畫布編輯（P0）

> ⏭️ **本次範圍外**（Step 1.5 選定範圍為 J11–J14）。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J3-01 | P0 | 拖拉節點入畫布（TRIGGER/LLM_ASSISTANT/OUTPUT） | 節點出現，nodeKey 唯一 | ⏭️ | 本次範圍外 |
| J3-02 | P0 | 連線 edge | edge 建立，兩端點存在 | ⏭️ | 本次範圍外 |
| J3-03 | P0 | 選節點於 Inspector 填 config | 表單值寫回節點 | ⏭️ | 本次範圍外 |
| J3-04 | P0 | save 整張覆寫 | version+1 | ⏭️ | 本次範圍外 |
| J3-05 | P1 | 重整頁面 get 還原 | 畫布與 Inspector 與存檔一致 | ⏭️ | 本次範圍外 |
| J3-06 | P1 | 能力掛載：TOOL / MCP_SERVER / SKILL / **KNOWLEDGE_RAG** 的 `out:main` 連 LLM `in:tool` 並 save | edge 建立（`targetHandle=in:tool`）；重載後連線保留 | ⏭️ | 本次範圍外 |
| J3-07 | P1 | 多個 KNOWLEDGE_RAG 同時掛同一 LLM 的 `in:tool` | 皆放行、無相容性 toast；CONNECTIONS 計數與 edge 數一致 | ⏭️ | 本次範圍外 |
| J3-08 | P1 | 提示接線：PROMPT 的 `out:main` 連 LLM `in:prompt` 並 save | edge 建立（`targetHandle=in:prompt`）；重載後連線保留 | ⏭️ | 本次範圍外 |
| J3-09 | P1 | 兩個 PROMPT 各接 CONDITION 的 `out:true`/`out:false`，再同時連同一 LLM 的 `in:prompt` | 皆放行、無相容性 toast；CONNECTIONS 計數與 edge 數一致 | ⏭️ | 本次範圍外 |

### J4 驗證與啟用（P1）

> ⏭️ **本次範圍外**（Step 1.5 選定範圍為 J11–J14）。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J4-01 | P1 | 缺 TRIGGER 啟用 | 400，對應訊息 | ⏭️ | 本次範圍外 |
| J4-02 | P1 | 圖有環啟用 | 400，對應訊息 | ⏭️ | 本次範圍外 |
| J4-03 | P1 | 缺必填 config 啟用（如缺 llmId） | 400，`workflow.node.config.required.missing`，顯示 nodeKey+欄位 | ⏭️ | 本次範圍外 |
| J4-04 | P1 | 合法圖 switchStatus 啟用 | 狀態轉 ACTIVE | ⏭️ | 本次範圍外 |
| J4-05 | P1 | **非能力來源節點**拉線到 LLM `in:tool`（白名單＝TOOL / MCP_SERVER / SKILL / KNOWLEDGE_RAG） | `onConnect` 即擋：toast「僅工具、MCP、Skill、知識庫節點可連到 LLM 的工具埠」，**edge 不建立**（CONNECTIONS 不變）；建議併測對照組（能力來源連同一埠應成功） | ⏭️ | 本次範圍外 |
| J4-06 | P1 | KNOWLEDGE_RAG 掛 `in:tool`（外掛模式）且 `query` / `embeddingModelId` 留空即啟用 | **啟用成功**（外掛模式僅需 `knowledgeId`） | ⏭️ | 本次範圍外 |
| J4-07 | P1 | 孤兒 SKILL 節點（未連任何 LLM `in:tool`）save → 啟用 | save 放行；**switchStatus 回 400** `workflow.skill.node.not.mounted`（含 nodeKey），開關維持關閉、狀態仍 DRAFT。⚠️ SKILL 須先填 `skillId`，否則先撞 REQUIRED_MISSING 而測不到本案例 | ⏭️ | 本次範圍外 |
| J4-08 | P1 | SKILL 已掛載到 LLM `in:tool` 後啟用（J4-07 對照組） | 啟用成功，狀態轉 ACTIVE | ⏭️ | 本次範圍外 |
| J4-09 | P1 | **非 PROMPT 節點**拉線到 LLM `in:prompt`（如 TRIGGER） | `onConnect` 即擋：toast「僅提示詞節點可連到 LLM 的提示埠」，**edge 不建立**（CONNECTIONS 不變） | ⏭️ | 本次範圍外 |
| J4-10 | P1 | 孤兒 PROMPT 節點（未連任何 LLM `in:prompt`）save → 啟用 | save 放行；**switchStatus 回 400** `workflow.prompt.node.not.connected`（含 nodeKey），狀態仍 DRAFT。⚠️ PROMPT 須先填 `prompt`，否則先撞 REQUIRED_MISSING | ⏭️ | 本次範圍外 |
| J4-11 | P1 | LLM `userPrompt` 留空且無 PROMPT 連入時啟用 | **400** `workflow.llm.prompt.required`（含 LLM 的 nodeKey），狀態仍 DRAFT | ⏭️ | 本次範圍外 |
| J4-12 | P1 | LLM `userPrompt` 留空但已有 PROMPT 連入（J4-10/11 對照組） | 啟用成功，狀態轉 ACTIVE | ⏭️ | 本次範圍外 |

> ⚠️ **KNOWLEDGE_RAG 兩種模式的必填欄位相反，勿寫反預期**：
> 掛 LLM `in:tool`＝外掛模式，僅需 `knowledgeId`；連一般 `in:main`＝pipeline 模式，另需 `query`。
> 白名單事實來源：後端 `WorkflowEngine.CAPABILITY_SOURCE_TYPES` ≡ 前端 `useGraphValidation.CAPABILITY_SOURCE_TYPES`。

> ⚠️ **`in:prompt` 與 `in:tool` 語義相反，勿寫反預期**：
> `in:tool` 允許 TOOL/MCP_SERVER/SKILL/KNOWLEDGE_RAG，**非資料流**（純能力節點不執行、不落紀錄）；
> `in:prompt` 只允許 PROMPT，**是資料流**（參與活化，來源節點照常執行並落紀錄）。
> 驗證順序：必填欄位 → 孤兒 SKILL → 孤兒 PROMPT → LLM 無提問來源。

### J5 執行 workflow（P0，真實 LLM）

> ⏭️ **本次範圍外**（Step 1.5 選定範圍為 J11–J14）。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J5-01 | P0 | 合法 workflow（TRIGGER→LLM_ASSISTANT→OUTPUT）execute | SSE `started`→`node.*`→`completed(SUCCESS)`；DB 寫入 `llm_workflow_execution`/`llm_workflow_node_execution` | ⏭️ | 本次範圍外 |
| J5-02 | P1 | 節點設定錯誤導致失敗 | 該節點 `node.failed` 顯示錯誤，執行標記失敗 | ⏭️ | 本次範圍外 |
| J5-03 | P1 | 執行中途 client 斷線 | 執行 `CANCELLED`，下游未執行節點 `SKIPPED`（commit b24c128 視覺） | ⏭️ | 本次範圍外 |
| J5-04 | P1 | 外掛型 RAG：KNOWLEDGE_RAG 以 `out:main → LLM in:tool` 掛載後 execute | 推論前自動檢索注入（非 LLM 主動呼叫）；**不產生**獨立節點事件、`node_execution` 無其紀錄。內容正確性斷言歸 J8-B | ⏭️ | 本次範圍外 |
| J5-05 | P1 | Agent 模式：TOOL / MCP_SERVER / SKILL 以 `out:main → LLM in:tool` 掛載後 execute | LLM 可自主呼叫掛載工具並回覆；能力節點**不產生**獨立節點事件、`node_execution` 無其紀錄 | ⏭️ | 本次範圍外 |
| J5-06 | P1 | 提示節點驅動：PROMPT → `LLM in:prompt` → OUTPUT（LLM `userPrompt` 留空）execute | PROMPT **照常**產生 `node.started`/`node.completed` 與 `node_execution` 紀錄（與能力節點相反）；LLM 以該提示提問並成功回覆 | ⏭️ | 本次範圍外 |
| J5-07 | P1 | 分支擇一：CONDITION 兩分支各接一 PROMPT 匯入同一 LLM 後 execute | 被活化分支的 PROMPT 為 SUCCESS、另一顆 `node_execution` 為 SKIPPED；回覆風格對應被活化那條 | ⏭️ | 本次範圍外 |
| J5-08 | P1 | 優先序：LLM 同時有 `userPrompt` 與 PROMPT 連入 | 以 PROMPT 輸出為準；刪 `in:prompt` 連線後再執行改用 `userPrompt`。⚠️ 只刪連線會讓 PROMPT 變孤兒而被防線擋下，須連該節點一起刪 | ⏭️ | 本次範圍外 |
| J5-09 | P1 | execute 被後端擋下時（如含孤兒 PROMPT 節點）檢視執行結果面板 | 顯示**後端業務訊息**（含 nodeKey 與應連往的埠），**不得**只顯示 `HTTP 400`。⚠️ execute 是唯一走原生 `fetch`、不經 axios 攔截器的端點，訊息解析在 `api/workflowExecution.ts` 自行實作，改該檔須重驗本條 | ⏭️ | 本次範圍外 |
| J5-10 | P0 | 多觸發點：兩顆 TRIGGER 各接一分支匯流到同一 LLM，點工具列執行鈕 | 出現 `trigger-picker` 列出兩個 `trigger-option-<nodeKey>`（顯示節點名稱），尚未開始執行 | ⏭️ | 本次範圍外 |
| J5-11 | P0 | 選定 t1 後執行 | 只有 t1 分支節點亮起；另一顆 TRIGGER 呈 `.is-exec-skipped`（前端預標，後端對 SKIPPED 不發事件）；LLM 取 t1 分支的提問；`llm_workflow_execution.trigger_node_key`=`t1` | ⏭️ | 本次範圍外 |
| J5-12 | P1 | 改從 t2 執行 / 節點卡 `node-run-from-here` 直接開跑 | 前一輪 skipped 標記不殘留、改為 t1 呈 skipped、`trigger_node_key`=`t2`；節點卡入口不觸發拖曳或取消選取 | ⏭️ | 本次範圍外 |

> J5 斷言不比對 LLM 輸出文字；只驗事件序列、節點狀態、DB 紀錄。缺 E2E_LLM_ID 時 J5-01/J5-03 標 ⏭️。

### J6 Inspector 表單（P1）

> ⏭️ **本次範圍外**（Step 1.5 選定範圍為 J11–J14）。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J6-05 | P0 | 自 palette 拖入 TRIGGER 後直接看 Inspector | `trigger-type` 下拉顯示且值已為 `MANUAL`，**無必填警告**（由 `NodeTypeMeta.defaultConfig` 帶入）；WEBHOOK / CRON 停用 | ⏭️ | 本次範圍外 |
| J6-06 | P1 | 無型別化表單的節點（如 CODE）在 JSON 編輯器**表格模式**新增／移除欄位 | `new-field-key` + `add-field` 可新增（空鍵名／重複鍵顯示 `new-field-error`）、`remove-field-<key>` 可移除；不必切 JSON 模式手打 | ⏭️ | 本次範圍外 |
| J6-07 | P0 | 選取尚未設定的 OUTPUT 節點 | 顯示 `output-both-empty`（擇一必填）與 `output-ref-suggestions`（chip 顯示為「節點名 › 欄位」）；點 chip 插入後提示消失。⚠️ 建議鍵取自實際執行輸出，未執行則用 `nodeOutputKeys.ts` 的型別預設鍵（手抄後端 executor，改後端須同步） | ⏭️ | 本次範圍外 |
| J6-08 | P0 | 檢視含引用的輸出模板 | 引用渲染為 `expr-token-<path>` 色塊（文字＝「節點名 › 欄位」、`data-ref`＝原始 `{{...}}`）；節點改名後文字跟著變、`data-ref` 不變；引用不存在節點時帶 `is-unknown`。⚠️ 此欄位是 **contenteditable 不是 textarea**，斷言用 `textContent()` 而非 `inputValue()` | ⏭️ | 本次範圍外 |
| J6-09 | P1 | 於輸出模板以中文輸入法打字 | 組字不吃字、不寫入半成品；commit 後文字完整且既有色塊不受影響。存檔後 DB 的 `template` 為「原始 `{{...}}` ＋ 中文」，不含畫面顯示文字 | ⏭️ | 本次範圍外 |
| J6-01 | P1 | LlmAssistantForm 選 llmId | 值寫回並可存檔 | ⏭️ | 本次範圍外 |
| J6-02 | P1 | Tool/McpServer/KnowledgeRag/Prompt/Output 表單填寫 | config 正確寫回，save 後重載一致（PromptForm 填 `prompt-text`、選填 `output-key`） | ⏭️ | 本次範圍外 |
| J6-04 | P1 | LLM 接上 PROMPT 後檢視 LlmAssistantForm | 顯示 `prompt-overridden-badge` 與 `prompt-source-hint`；`user-prompt` 仍可編輯（非 disabled） | ⏭️ | 本次範圍外 |
| J6-03 | P2 | settingSchema 動態表單（sensitive 遮罩） | 依 schema 正確渲染欄位型別 | ⏭️ | 本次範圍外 |

### J7 契約錯誤（P2）

> ⏭️ **本次範圍外**（Step 1.5 選定範圍為 J11–J14）。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J7-01 | P2 | save 節點 config 型別錯誤（未知欄位/結構型別錯） | 400，`workflow.node.config.invalid`，含 nodeKey | ⏭️ | 本次範圍外 |
| J7-02 | P2 | 樂觀鎖 version 衝突 | 400，`workflow.version.conflict`，UI 顯示衝突提示 | ⏭️ | 本次範圍外 |

### J8 知識庫 RAG 檢索問答（P1，真實 embedding + Milvus + 真實 LLM）

> ⏭️ **本次範圍外**（Step 1.5 選定範圍為 J11–J14）。

> 前端無向量庫/設定/上傳管理頁（僅 login/列表/編輯器），前置三步走 API 準備，UI 只建 workflow 與執行。
> 與 J5 差異：**有已知來源文件，斷言輸出內容與文件事實相符**（先讀文件挑唯一事實作查詢關鍵字→預期答案）。
> KNOWLEDGE_RAG 有 pipeline / 外掛兩模式，故分 J8-A / J8-B 兩組。詳見 `docs/e2e-test-plan.md` §3-J8、§13（A）、§14（B）。

#### J8-A pipeline 模式：`TRIGGER → KNOWLEDGE_RAG → LLM_ASSISTANT → OUTPUT`

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J8-01 | P1 | 前置(API)：建 EMBEDDING 設定 + Milvus 向量庫 + 上傳文件建知識庫 | 取得 embeddingModelId/embeddingStoreId/knowledgeId；`getDataFromEmbeddingStore` 檢索命中目標事實（維度須對齊） | ⏭️ | 本次範圍外 |
| J8-02 | P1 | UI 建 workflow：拖 TRIGGER/KNOWLEDGE_RAG/LLM_ASSISTANT/OUTPUT，Inspector 選知識庫/embedding/LLM，userPrompt 以 `{{<ragKey>.documents}}` 串接 | 4 節點、3 edge（nodeKey 由 `.vue-flow__node[data-id]` 讀取供插值） | ⏭️ | 本次範圍外 |
| J8-03 | P1 | 存檔 + switchStatus 啟用 | version 1；KNOWLEDGE_RAG 必填驗證通過（pipeline 需 `query`），狀態 ACTIVE | ⏭️ | 本次範圍外 |
| J8-04 | P1 | execute（真實 embedding + LLM） | SSE started→node.*→completed(SUCCESS)；4 節點 node_execution 皆 SUCCESS、DB 落庫 | ⏭️ | 本次範圍外 |
| J8-05 | P1 | **RAG 正確性斷言**：finalOutput 與文件已知事實比對 | LLM 輸出含文件原文事實關鍵字（例：渣打 TNC「年滿二十歲」→ 輸出含「20/二十」） | ⏭️ | 本次範圍外 |

#### J8-B 外掛模式：KNOWLEDGE_RAG `out:main → LLM in:tool`（自動注入型 RAG）

> 圖形為 `TRIGGER → LLM_ASSISTANT → OUTPUT`，另掛 N 個 KNOWLEDGE_RAG 到 LLM `in:tool`。
> **userPrompt 只寫問題，不做任何 `{{...}}` 知識插值**。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J8-06 | P1 | UI 建圖：5 節點（TRIGGER/LLM_ASSISTANT/OUTPUT/2×KNOWLEDGE_RAG）、4 edge，其中 2 條為 RAG `out:main`→LLM `in:tool` | 拉線放行、無相容性 toast、console error=0；Overview 顯示 5 NODES / 4 CONNECTIONS；存檔成功 | ⏭️ | 本次範圍外 |
| J8-07 | P1 | **必填契約**：兩個 RAG 只選知識庫，`query`/`embeddingModelId` 皆留空後啟用 | 啟用成功（不回 `workflow.node.config.required.missing`） | ⏭️ | 本次範圍外 |
| J8-08 | P1 | execute（真實 embedding + LLM） | SSE started→**僅 trigger/llm/output 三節點** node.*→completed(SUCCESS) | ⏭️ | 本次範圍外 |
| J8-09 | P1 | **純能力節點不落主遍歷**：執行後檢視 RAG 節點 | RAG 節點 Inspector **無**「本次執行」區塊（`data-test="node-exec-section"`）；`llm_workflow_node_execution` 無 `KNOWLEDGE_RAG` 紀錄 | ⏭️ | 本次範圍外 |
| J8-10 | P1 | **自動注入正確性斷言**：finalOutput 與文件事實比對，且需能與 LLM 常識答案區分 | 輸出為文件事實而非常識（渣打 TNC「年滿二十歲」→「20歲」，而非民法成年「18歲」） | ⏭️ | 本次範圍外 |

> ⚠️ **J8-10 的鑑別點要求**：所選事實必須**與 LLM 常識答案不同**，否則無法區分「注入生效」與「模型本來就知道」，該案例形同無效。
> 缺有效 embedding 或 CHAT 設定即 J8 全條 skip 並標註。embedding 選型與維度對齊、憑證解密管制等陷阱見 `e2e-test-plan.md` §13；
> 外掛模式的必填契約、Milvus 未啟動樣態、`deleteData` 不連鎖等陷阱見 §14。

### J9 編輯器外觀與節點編輯頁（P0/P1/P2）

> ⏭️ **本次範圍外**（Step 1.5 選定範圍為 J11–J14）。

> 純 UI 旅程、不需後端資料變更，可沿用 J3 建好的 workflow。詳見 `docs/e2e-test-plan.md` §3-J9。
> ⚠️ Node Designer 是**全屏遮罩**：每次回畫布操作前，先確認 `node-designer-modal` 已消失。
> ⚠️ 主題偏好存於 `localStorage.wf-theme`，會跨案例殘留；驗外觀前先明確設定或清除。

| # | 優先 | 案例 | 預期 | 狀態 | 證據 |
|---|:---:|------|------|:---:|------|
| J9-01 | P1 | 清空 `localStorage` 後進編輯器 | `.wf-editor` 的 `data-wf-theme="light"` | ⏭️ | 本次範圍外 |
| J9-02 | P1 | 點 `theme-toggle` 兩次 | 深色↔淺色來回無殘留區塊；`localStorage.wf-theme` 同步；重整後保留 | ⏭️ | 本次範圍外 |
| J9-03 | P1 | 深色下導到 `/login` 與列表頁 | `<html>` 的 `dark` class 已移除，兩頁維持淺色 | ⏭️ | 本次範圍外 |
| J9-04 | P2 | 深色下觸發 `ElMessageBox`（未存離開／登出確認） | 對話框深色配色且文字可讀 | ⏭️ | 本次範圍外 |
| J9-05 | P1 | 點 `zoom-in` / `zoom-out` / `zoom-fit` | 畫布縮放改變，`zoom-value` 百分比同步更新 | ⏭️ | 本次範圍外 |
| J9-06 | P0 | 雙擊節點 | 出現 `node-designer-modal`；header 顯示型別／分類／名稱；該節點同時被選取 | ⏭️ | 本次範圍外 |
| J9-07 | P1 | 點節點卡右上 `node-open-designer` | 開啟 Designer，且未觸發拖曳或取消選取 | ⏭️ | 本次範圍外 |
| J9-08 | P1 | 點 Inspector 的 `open-node-designer-button` | 開啟 Designer | ⏭️ | 本次範圍外 |
| J9-09 | P1 | 三種關閉：Esc、點遮罩、`node-designer-close` | 皆關閉；再開啟時分頁回到 Parameters | ⏭️ | 本次範圍外 |
| J9-10 | P0 | modal 內改 config → 關閉 | Inspector 與節點副標同步更新；工具列標記未存 | ⏭️ | 本次範圍外 |
| J9-11 | P1 | 在 `node-designer-name-input` 打字後按 Delete | 節點**不被刪除** | ⏭️ | 本次範圍外 |
| J9-12 | P1 | 未執行流程時檢視 Input / Output | 兩側皆顯示「尚未執行」空狀態 | ⏭️ | 本次範圍外 |
| J9-13 | P1 | 執行後開啟中段節點的 Designer | Input 顯示 `node-designer-input-json`（含「來自 <上游名>」）；Output 顯示 JSON 與可引用欄位。⚠️ **不可先關結果抽屜**——關閉鈕綁 `executionStore.reset()`，會清空 `nodeStates` | ⏭️ | 本次範圍外 |
| J9-14 | P1 | 開啟 TRIGGER 的 Designer | Input 為虛線空狀態「這是觸發節點…」，不顯示 JSON | ⏭️ | 本次範圍外 |
| J9-15 | P1 | 開啟 SKILL 的 Designer | Input「這是能力提供節點…」；Output「不會產生自己的輸出」。⚠️ SKILL **有** `out:main` 埠（能力掛載用），只看埠數會誤判 | ⏭️ | 本次範圍外 |
| J9-16 | P1 | 開啟已掛能力節點的 LLM 的 Designer | `node-designer-capability-list` 以 chip 列出 `in:tool` 來源，不顯示其 JSON | ⏭️ | 本次範圍外 |
| J9-17 | P2 | Settings 與 Docs 分頁（Docs 逐一檢視 13 種型別） | Settings 顯示 nodeKey／型別／分類／必填檢核／本次執行；Docs 皆有內容且連接埠清單與實際 handle 一致 | ⏭️ | 本次範圍外 |

### J10 複合能力掛載：旅遊行程規劃（P1，真實 MCP + 真實 Web 搜尋 + 真實 LLM）

> ⏭️ **本次範圍外**（Step 1.5 選定範圍為 J11–J14）。

> 圖形：`TRIGGER → LLM_ASSISTANT → OUTPUT`，`PROMPT → in:prompt`，
> `MCP_SERVER(google_map)` / `TOOL(TavilySearch)` / `SKILL(pdf)` 三者皆接 LLM 的 `in:tool`。
> ⚠️ MCP 節點**必須填 `userSettingId`**，否則 env 佔位符不替換、工具不可用（§12 第 1 點）。
> ⚠️ LLM／MCP／Tool／Skill 設定必須**同屬登入者**（§12 第 2 點）。
> ⚠️ `searchPlaces` 必填 `query`／`language`／`maxResults`，PROMPT 須要求模型帶齊。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------------|
| J10-01 | P1 | 拖出 6 節點並連線（3 條 `in:tool`、1 條 `in:prompt`、2 條 `out:main`） | 連線皆成立，畫布無驗證紅框 | ⏭️ | 本次範圍外 |
| J10-02 | P1 | 斷開 SKILL 的 `in:tool` 邊後按啟用 | 擋下並回 `workflow.skill.node.not.mounted`，含 nodeKey | ⏭️ | 本次範圍外 |
| J10-03 | P1 | 補回 SKILL 邊後啟用 | 啟用成功，狀態轉 ACTIVE | ⏭️ | 本次範圍外 |
| J10-04 | P1 | 執行 workflow，觀察 SSE 與 ExecutionResultDrawer | `execution.started`→`node.*`→`execution.completed`，全節點 SUCCESS（不比對輸出文字） | ⏭️ | 本次範圍外 |
| J10-05 | P1 | deep-verify：景點真實性 | 景點為真實臺東地點（地址含「臺東縣」），每點帶 `place_id` 與經緯度 | ⏭️ | 本次範圍外 |
| J10-06 | P1 | deep-verify：活動查證 | 含 TavilySearch 取得的活動／營業資訊與可追溯來源 URL | ⏭️ | 本次範圍外 |
| J10-07 | P2 | DB 落庫檢查 | `llm_workflow_execution` 一筆 SUCCESS；`llm_workflow_node_execution` 恰 4 筆（純能力節點不落紀錄） | ⏭️ | 本次範圍外 |
| J10-08 | P1 | 由 OUTPUT 產出 PDF 行程表 | PDF 含 Day1/Day2 行程、每景點 Google Maps 連結、活動資訊與來源 | ⏭️ | 本次範圍外 |

---

### J11 純資料管線（P0，無 LLM，確定性斷言）

> 圖形：`TRIGGER → HTTP_REQUEST → DATA_TRANSFORM → OUTPUT`（4 節點 3 edge）。
> ⚠️ **唯一一條完全不含 LLM 的執行旅程**，輸出確定：逐欄比對 `finalOutput`，**不放寬逾時、不做模糊比對**。
> ⚠️ `HTTP_REQUEST` / `DATA_TRANSFORM` **無型別化表單**，設定一律走 `JsonConfigEditor`（表格模式或 JSON 模式）。
> ⚠️ 資料來源用 `E2E_HTTP_TEST_URL`（預設本機 `/systemSetting/list`），**不依賴外網**。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------------|
| J11-01 | P0 | 拖出 4 節點並連線、Inspector（JSON 編輯器）設定後存檔 | 存檔成功、version 1 | ✅ | ![J11-01-01-login-page](e2e-shots/202608202139/J11-01-01-login-page.png) ![J11-01-02-workflow-list](e2e-shots/202608202139/J11-01-02-workflow-list.png) ![J11-01-03-editor-named](e2e-shots/202608202139/J11-01-03-editor-named.png) ![J11-01-04-four-nodes-count4](e2e-shots/202608202139/J11-01-04-four-nodes-count4.png) ![J11-01-05-three-edges-count3](e2e-shots/202608202139/J11-01-05-three-edges-count3.png) ![J11-01-06-trigger-form-MANUAL](e2e-shots/202608202139/J11-01-06-trigger-form-MANUAL.png) ![J11-01-07-http-config](e2e-shots/202608202139/J11-01-07-http-config.png) ![J11-01-08-transform-config](e2e-shots/202608202139/J11-01-08-transform-config.png) ![J11-01-09-output-config](e2e-shots/202608202139/J11-01-09-output-config.png) ![J11-01-10-saved](e2e-shots/202608202139/J11-01-10-saved.png)<br>4 節點／3 edge 建立成功；TRIGGER 為型別化表單且 `triggerType` 已預設 `MANUAL`（無需填寫，佐證 J6-01）。`workflow/get` 回：version=1、status=DRAFT、nodes=4、edges=3。console error = 0。<br>⚠️ `HTTP_REQUEST` / `DATA_TRANSFORM` 確認無型別化表單，設定全走 `JsonConfigEditor` 的 JSON 模式（`mode-toggle` + `raw-input`）。 |
| J11-02 | P0 | execute | SSE `started`→4 節點 `node.*`→`completed`(SUCCESS)；`node_execution` 恰 4 筆皆 SUCCESS | ✅ | ![J11-02-11-drawer-status](e2e-shots/202608202139/J11-02-11-drawer-status.png)<br>SSE 實測序列：`execution.started` → `node.started`/`node.completed` ×4 → `execution.completed`。DB：`llm_workflow_execution` status=SUCCESS、trigger_type=MANUAL；`llm_workflow_node_execution` **恰 4 筆**（TRIGGER/HTTP_REQUEST/DATA_TRANSFORM/OUTPUT）**全部 SUCCESS**。 |
| J11-03 | P0 | **確定性輸出斷言（核心）** | `finalOutput` 與該端點實際回應逐欄相符；重跑兩次輸出完全一致 | ✅ | ![J11-03-12-output-vs-direct](e2e-shots/202608202139/J11-03-12-output-vs-direct.png) ![J11-03-13-second-run](e2e-shots/202608202139/J11-03-13-second-run.png)<br>`finalOutput.payload` 與直呼 `GET /systemSetting/list` 的回應**逐字元完全相符**（非模糊比對）。連續執行兩次輸出完全一致（379 字元）。**全程未放寬逾時、未使用任何 LLM**。 |
| J11-04 | P1 | `HTTP_REQUEST.response` 的型別 | 為**原始字串**非解析後 JSON；mappings 無法下鑽欄位（需 CODE 節點 `JSON.parse`） | ✅ | ![J11-04-14-drill-mapping-config](e2e-shots/202608202139/J11-04-14-drill-mapping-config.png) ![J11-04-15-execute-drill](e2e-shots/202608202139/J11-04-15-execute-drill.png)<br>`payload` 型別為 **string**、內容為合法 JSON 文字 → 證實 `HttpRequestExecutor` 直接回 `response.body?.string()`，**不做解析**。<br>反證：`DATA_TRANSFORM` 加 mapping `{{<httpKey>.response.data}}` 後執行 → 該節點 **FAILED**，`error_message = Variable not found: rrMPClY2.response.data`，下游 OUTPUT `SKIPPED`。<br>⚠️ **比計畫預期更嚴厲**：下鑽字串不是回 null，而是**讓節點失敗**。 |
| J11-05 | P1 | `secretHeaders` 加密落地與遮罩 | `workflow/get` 回 `__SECRET_KEPT__`；DB 為密文；明文不出現在回應／日誌／錯誤訊息 | ✅ | ![J11-05-16-secret-header-config](e2e-shots/202608202139/J11-05-16-secret-header-config.png) ![J11-05-17-saved-with-secret](e2e-shots/202608202139/J11-05-17-saved-with-secret.png)<br>`workflow/get` 回 `secretHeaders = {"X-E2E-Secret":"__SECRET_KEPT__"}`；DB `llm_workflow_node.config` 內為 v2 密文 `v2$hMsL/RfO81eGDabNr63d6Q==$a4PclRewNCxJNQ10$…`，**不含明文**（以字串搜尋明文值確認為 false）。 |
| J11-06 | P1 | `__SECRET_KEPT__` 沿用 | 再存一次後 DB 密文不變；再次 execute 仍成功 | ✅ | ![J11-06-18-reopened-from-list](e2e-shots/202608202139/J11-06-18-reopened-from-list.png) ![J11-06-19-masked-in-form](e2e-shots/202608202139/J11-06-19-masked-in-form.png) ![J11-06-20-execute-after-reuse](e2e-shots/202608202139/J11-06-20-execute-after-reuse.png)<br>自列表點「編輯」重新載入後，表單中的 config 顯示 `"X-E2E-Secret": "__SECRET_KEPT__"`（截圖二）。再次存檔後 DB 密文**位元組不變**，且**未誤存字面值** `__SECRET_KEPT__`。再次 execute 仍 SUCCESS → 證實沿用的是真實明文。 |
| J11-07 | P1 | 非 2xx（url 指向必然 404 的路徑） | 節點 `node.failed`、整體 FAILED、下游 SKIPPED；⚠️ 訊息為寫死英文 `Unexpected code 404`（非 i18n，如實記錄） | ✅ | ![J11-07-21-url-404-config](e2e-shots/202608202139/J11-07-21-url-404-config.png) ![J11-07-22-execute-404](e2e-shots/202608202139/J11-07-22-execute-404.png)<br>`node_execution`：TRIGGER SUCCESS → **HTTP_REQUEST FAILED** → DATA_TRANSFORM **SKIPPED** → OUTPUT **SKIPPED**；整體 FAILED。<br>⚠️ `error_message = Unexpected code 404` —— **寫死英文字串（`IOException`），非 i18n 業務訊息**，`AppMessage` 無 `WORKFLOW_HTTP_REQUEST_FAILED`。與 `requirements.md` AC-D7 不符，已如實記錄（見問題追蹤區 #1）。 |
| J11-08 | P2 | `timeoutMs` 設極小值 | 節點 FAILED（call timeout）；服務不受影響，後續案例可繼續 | ✅ | ![J11-08-23-timeout-config](e2e-shots/202608202139/J11-08-23-timeout-config.png) ![J11-08-24-execute-timeout](e2e-shots/202608202139/J11-08-24-execute-timeout.png)<br>`timeoutMs=1` → HTTP_REQUEST FAILED、下游全 SKIPPED。<br>⚠️ `error_message = Node rrMPClY2 execution timed out after 1 ms` —— 攔截點是**引擎層的節點逾時**，非 OkHttp 的 callTimeout；訊息同樣為寫死英文（見問題追蹤區 #2）。<br>執行後 `/view/chat` 回 **200**，服務未受影響。 |

---

### J12 迴圈批次處理（P1，LOOP 首條端到端覆蓋）

> 圖形：`TRIGGER → LOOP`，`out:loop` 接子圖 `DATA_TRANSFORM`、`out:done` 接 `OUTPUT`。
> ⚠️ 輸入陣列由 **TRIGGER 的 inputPayload** 提供（`{{trigger.rows}}` 才是原生 List）。
> ⚠️ `loopBodyEntryNodeKey` 為**必填且須手填 nodeKey 字串**（無 UI 選擇器），nodeKey 自 `.vue-flow__node[data-id]` 讀取。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------------|
| J12-01 | P1 | 建圖並存檔（填 `inputArrayPath` 與 `loopBodyEntryNodeKey`） | 存檔成功 | ✅ | ![J12-01-01-five-nodes-count5](e2e-shots/202608202139/J12-01-01-five-nodes-count5.png) ![J12-01-02-four-edges-count4](e2e-shots/202608202139/J12-01-02-four-edges-count4.png) ![J12-01-03-code-config](e2e-shots/202608202139/J12-01-03-code-config.png) ![J12-01-04-loop-config](e2e-shots/202608202139/J12-01-04-loop-config.png) ![J12-01-05-subgraph-config](e2e-shots/202608202139/J12-01-05-subgraph-config.png) ![J12-01-06-output-config](e2e-shots/202608202139/J12-01-06-output-config.png) ![J12-01-07-saved](e2e-shots/202608202139/J12-01-07-saved.png)<br>5 節點／4 edge（含 `out:loop` 與 `out:done` 兩條）；`workflow/get` 回 version=1、nodes=5、edges=4。<br>⚠️ **原計畫的圖形經 UI 不可達**：計畫寫「陣列由 TRIGGER 的 `inputPayload` 供應」，但**前端從未送 `inputPayload`**（`api/workflowExecution.ts:43` 註解自承「之後也應該放這裡」），從編輯器執行時 TRIGGER 永遠輸出 `{}`。故改由 `CODE` 節點產生陣列（亦更貼近 n8n 實務）。詳見問題追蹤區 #3。<br>⚠️ `loopBodyEntryNodeKey` 為必填且**須手打 nodeKey 字串**（無 UI 選擇器），本次自 `.vue-flow__node[data-id]` 讀取後填入。 |
| J12-02 | P1 | execute（N=3） | 整體 SUCCESS；子圖節點**每迭代各發一次** `node.started`/`node.completed` | ✅ | ![J12-02-08-execute-n3](e2e-shots/202608202139/J12-02-08-execute-n3.png)<br>整體 SUCCESS。SSE 中子圖節點 `DATA_TRANSFORM` 的事件數 = **6**（＝3 迭代 × `node.started`/`node.completed`），證實**每迭代各發一次事件**而非整段一次。 |
| J12-03 | P1 | **`node_execution` 落庫筆數（核心）** | 子圖節點 **N 筆**（帶 `loopIndex`）非 1 筆；N=3 時全圖共 **6 筆**（TRIGGER1＋LOOP1＋子圖3＋OUTPUT1） | ✅ | <br>`node_execution` 實測 **7 筆**：TRIGGER 1 ＋ CODE 1 ＋ LOOP 1 ＋ **DATA_TRANSFORM 3（每迭代一筆，帶 `loop_index` 0/1/2）** ＋ OUTPUT 1，全部 SUCCESS。<br>✅ 釐清了計畫中列為未知的問題：**是 N 筆，不是 1 筆**。<br>⚠️ 計畫 §J12-03 原寫「N=3 時全圖 6 筆」，係未計入本次為供應陣列而加入的 CODE 節點，實際為 7 筆（見問題追蹤區 #3 的計畫修訂）。 |
| J12-04 | P1 | 彙集鍵與 `out:done` 下游取值 | 輸出 `{ items: [...] }`；元素為每迭代**子圖最後節點的完整輸出 map** | ✅ | <br>LOOP **自身**輸出（`node_execution.output`）＝ `{"items": [{"doubled":1},{"doubled":2},{"doubled":3}]}` → `collectOutputKey` 預設 `items` 正確，且元素確為**每迭代子圖最後節點的完整 output map**。<br>⚠️ 附帶發現：同一份資料經 OUTPUT 節點渲染後變成**字串** `"[{\"doubled\":1},…]"`——因 `OutputNodeConfig.mappings` 型別為 `Map<String,String>`，會把原生型別序列化。**斷言必須看 LOOP 自身輸出，不可透過 OUTPUT 節點看**（本案首次斷言即因此誤判為 FAIL）。 |
| J12-05 | P1 | `out:loop` 與 `out:done` 活化語義 | `out:loop` 不參與活化；完成後只活化 `out:done`；子圖節點不在主遍歷被重複執行 | ✅ | <br>`OUTPUT` 僅 1 筆 → `out:done` 只在迭代結束後活化一次；`DATA_TRANSFORM` 恰 3 筆（＝迭代數）→ `out:loop` 只界定子圖入口，**子圖節點未在主遍歷被重複執行**。 |
| J12-06 | P2 | 超過迭代上限（`maxIterations=2`、輸入 5 筆） | ⚠️ **截斷而非報錯**：只跑前 2 筆、整體仍 SUCCESS、僅 logger.warn（與 AC-D6 不符，如實記錄） | ✅ | ![J12-06-09-max-iterations-2](e2e-shots/202608202139/J12-06-09-max-iterations-2.png) ![J12-06-10-execute-truncate](e2e-shots/202608202139/J12-06-10-execute-truncate.png)<br>`maxIterations=2` ＋ 輸入 5 筆 → 實際只跑 **2 次**迭代，LOOP `items` 長度 = 2，**整體仍 SUCCESS**。<br>⚠️ **證實為截斷而非報錯**：`AppMessage` 無 `WORKFLOW_LOOP_LIMIT_EXCEEDED`，引擎僅 `logger.warn`。與 `requirements.md` AC-D6 規定不符（見問題追蹤區 #4）。 |
| J12-07 | P2 | `inputArrayPath` 指向非陣列 | 節點 FAILED，`workflow.loop.input.not.array`；`{{path}}` 與裸 path 兩種寫法皆須驗 | ✅ | ![J12-07-11-non-array-config](e2e-shots/202608202139/J12-07-11-non-array-config.png) ![J12-07-12-execute-non-array](e2e-shots/202608202139/J12-07-12-execute-non-array.png) ![J12-07-13-execute-bare-path](e2e-shots/202608202139/J12-07-13-execute-bare-path.png)<br>兩種寫法皆正確解析並回同一錯誤：<br>`{{path}}` 包裹 → `Loop input path {{__lfTBdo.rows}} did not resolve to an array`<br>裸 path → `Loop input path __lfTBdo.rows did not resolve to an array`<br>LOOP 節點 FAILED、下游 OUTPUT SKIPPED。 |

---

### J13 條件分流資料管線（P1）

> 圖形：`TRIGGER → CONDITION`，true/false 兩側**各接兩節** `DATA_TRANSFORM` 再匯入 `OUTPUT`。
> ⚠️ 分支刻意加長一節，用以區分「只標直接下游」與「標整條下游」。與 J5 的分支案例分工：J5 分流**提示詞**、本旅程分流**資料流**。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------------|
| J13-01 | P1 | 建圖並存檔（填 `conditions` 與 `logic`） | 存檔成功；`conditions` 為唯一無條件必填 | ✅ | ![J13-01-01-eight-nodes-count8](e2e-shots/202608202139/J13-01-01-eight-nodes-count8.png) ![J13-01-02-eight-edges-count8](e2e-shots/202608202139/J13-01-02-eight-edges-count8.png) ![J13-01-03-condition-config](e2e-shots/202608202139/J13-01-03-condition-config.png) ![J13-01-04-branches-config](e2e-shots/202608202139/J13-01-04-branches-config.png) ![J13-01-05-saved](e2e-shots/202608202139/J13-01-05-saved.png)<br>8 節點／8 edge（true / false 兩側各兩節 DATA_TRANSFORM 再匯入同一 OUTPUT）；version=1。<br>⚠️ 8 節點時最右側節點會被 Inspector 面板遮住而點不到，需先按 `zoom-fit` 正規化視野（測試腳本注意事項，已補入問題追蹤區 #6）。 |
| J13-02 | P1 | 命中 true 的 payload execute | A1、A2 皆 SUCCESS；`finalOutput` 對應 A 分支（確定性比對） | ✅ | ![J13-02-06-execute-true](e2e-shots/202608202139/J13-02-06-execute-true.png)<br>CONDITION `eq` 判定為 true：A1 SUCCESS → A2 SUCCESS（A2 輸出 `{"branch": "A1-A2"}`，證實鏈式引用成立）；B1 / B2 皆 SKIPPED；整體 SUCCESS。 |
| J13-03 | P1 | 改 payload 使判定 false 後 execute | B1、B2 皆 SUCCESS；`finalOutput` 對應 B 分支 | ✅ | ![J13-03-07-category-B-config](e2e-shots/202608202139/J13-03-07-category-B-config.png) ![J13-03-08-execute-false](e2e-shots/202608202139/J13-03-08-execute-false.png)<br>改判定為 false：B1 / B2 SUCCESS（B2 輸出 `{"branch": "B1-B2"}`）；A1 / A2 皆 SKIPPED；整體 SUCCESS。與 J13-02 形成完整對照組。 |
| J13-04 | P1 | **SKIPPED 傳播深度（核心）** | 未活化分支**整條下游皆 SKIPPED**——B1 **與 B2 都要是**，不可只標直接相連那顆 | ✅ | <br>**本旅程核心斷言通過**：未活化分支的 **B1 與 B2 都是 SKIPPED**，不是只標直接相連的 B1。反向（J13-03）亦然，A1 / A2 皆 SKIPPED。→ SKIPPED **會沿整條下游傳播**。 |
| J13-05 | P2 | `operator` 填不支援的運算子 | 節點 FAILED，`workflow.condition.operator.not.supported`；⚠️ 非 AC-D5 所寫的 `WORKFLOW_CONDITION_EVAL_FAILED` | ✅ | ![J13-05-09-bad-operator-config](e2e-shots/202608202139/J13-05-09-bad-operator-config.png) ![J13-05-10-execute-bad-operator](e2e-shots/202608202139/J13-05-10-execute-bad-operator.png)<br>`operator` 填 `startsWith` → CONDITION 節點 FAILED，`error_message = Condition operator not supported: startsWith`。<br>⚠️ 證實鍵名為 `workflow.condition.operator.not.supported`，**非** `requirements.md` AC-D5 所寫的 `WORKFLOW_CONDITION_EVAL_FAILED`（該鍵不存在）。<br>支援的運算子實為：`isempty`/`isnotempty`/`eq`/`ne`/`gt`/`gte`/`lt`/`lte`/`contains`/`notcontains`。 |

---

### J14 CODE 節點沙箱（P1 功能 / P2 邊界，含安全性驗證）

> 圖形：`TRIGGER → CODE → OUTPUT`（3 節點）。
> 契約：全域 `input` = 上游所有輸出；**最後一個表達式**為回傳值；預設輸出鍵 `result`；預設逾時 10,000ms；輸出上限 256KB。
> ⚠️ **J14-04 / J14-05 具破壞性風險，排在本旅程最後執行**；跑完以 `/view/chat` 確認服務仍存活。
> ⚠️ 若服務已死，該事實即為 ❌ 並立即走 Step 5.7 失敗分流報告，**不得自行修復後重跑掩蓋**。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------------|
| J14-01 | P1 | 正常 JS 轉換（讀 `input`、最後表達式為物件） | 節點 SUCCESS，輸出落 `result`；`finalOutput` 確定性比對 | ✅ | ![J14-01-01-normal-js-config](e2e-shots/202608202139/J14-01-01-normal-js-config.png) ![J14-01-02-execute-normal](e2e-shots/202608202139/J14-01-02-execute-normal.png)<br>CODE 輸出落於預設鍵 `result`：`{"result": {"sum": 3, "keys": 1}}`。證實①全域 `input` 可讀到上游輸出（`keys` = 1）、②**最後一個表達式**即為回傳值（非 `return`）。 |
| J14-02 | P1 | 自訂 `outputKey` | 下游 `{{<codeKey>.<outputKey>}}` 取值成功；預設鍵 `result` 不再出現 | ✅ | ![J14-02-03-custom-outputkey-config](e2e-shots/202608202139/J14-02-03-custom-outputkey-config.png) ![J14-02-04-execute-custom-key](e2e-shots/202608202139/J14-02-04-execute-custom-key.png)<br>自訂 `outputKey: "payload"` → CODE 輸出 `{"payload": {"sum": 42}}`，**預設鍵 `result` 已不存在**；下游以 `{{<codeKey>.payload.sum}}` 取值成功（OUTPUT 得 `"42"`）。 |
| J14-03 | P2 | `language` 填 `python` | 節點 FAILED，`workflow.code.language.not.supported`；不執行任何腳本 | ✅ | ![J14-03-05-python-config](e2e-shots/202608202139/J14-03-05-python-config.png) ![J14-03-06-execute-python](e2e-shots/202608202139/J14-03-06-execute-python.png)<br>`language: "python"` → 節點 FAILED，`error_message = Code node language not supported: python`，**未執行任何腳本**。 |
| J14-04 | P2 | **逾時**：無窮迴圈＋`timeoutMs=2000` | FAILED `workflow.code.timeout`；**後續案例仍可正常執行**（執行緒與 context 已回收） | ✅ | ![J14-04-11-infinite-loop-config](e2e-shots/202608202139/J14-04-11-infinite-loop-config.png) ![J14-04-12-execute-timeout](e2e-shots/202608202139/J14-04-12-execute-timeout.png) ![J14-04-13-execute-after-timeout](e2e-shots/202608202139/J14-04-13-execute-after-timeout.png)<br>**安全性驗證通過**：`while(true){}` ＋ `timeoutMs=2000` → 節點 FAILED，`error_message = Node kpgvp3aM execution timed out after 2000 ms`，實際耗時 5877ms（含中斷與收尾）。<br>逾時後 `/view/chat` 回 **200**，且**同一張流程改回正常腳本後再執行仍 SUCCESS** → 執行緒與 GraalJS context 確實被回收，未拖垮服務。<br>⚠️ 攔截點是**引擎層的節點逾時**（訊息含 nodeKey），非 `CodeExecutor` 自身的 `workflow.code.timeout`；訊息同為寫死英文（見問題追蹤區 #2）。 |
| J14-05 | P2 | **沙箱越界**：`Java.type('java.io.File')` 等 | 被擋下，FAILED `workflow.code.script.error`；檔案系統無任何副作用 | ✅ | ![J14-05-09-sandbox-escape-config](e2e-shots/202608202139/J14-05-09-sandbox-escape-config.png) ![J14-05-10-execute-sandbox-escape](e2e-shots/202608202139/J14-05-10-execute-sandbox-escape.png)<br>**安全性驗證通過**：`Java.type('java.io.File')` → 節點 FAILED，`error_message = Code script error: ReferenceError: Java is not defined`。<br>`allowAllAccess(false)` 確實讓 host class 完全不可見（不是拋權限例外，而是該符號根本不存在），檔案系統無任何副作用。 |
| J14-06 | P2 | **輸出上限**：產生 >256KB 字串 | FAILED `workflow.code.output.too.large`；非靜默截斷 | ✅ | ![J14-06-07-big-output-config](e2e-shots/202608202139/J14-06-07-big-output-config.png) ![J14-06-08-execute-big-output](e2e-shots/202608202139/J14-06-08-execute-big-output.png)<br>產生 300KB 字串 → 節點 FAILED，`error_message = Code script output exceeds the 256KB limit`，**非靜默截斷**。 |

---

## 旅程完成檢查清單（每完成一條旅程必須立即執行）

> 對應 SKILL.md Step 5.5。進入下一旅程前，逐項勾選並更新摘要表。

```
□ 所有案例的逐步截圖已擷取並存入 e2e-shots/<報告時間戳>/（每步驟一張，無遺漏）
□ 所有案例證據已寫入（逐步圖片連結以 Markdown 圖片語法嵌入 + trace/事件序列，無佔位符、無省略）
□ 所有案例狀態已標記（✅ / ❌ / ⏭️）
□ 失敗案例（❌）已在「問題追蹤區」詳細記錄
□ 摘要表該旅程統計數字已更新（Pass / Fail / Skip / 總數）
□ Pass 率已計算並填入（保留一位小數，如 80.0%）
□ 該旅程新增的 e2e-* 測試資料已清理
```

### 嚴格規則

- **不可延後**：完成一條旅程立即更新，不可等全部測完才統一更新
- **即時暫停**：某旅程 Pass 率過低（P0<100% / P1<95% / P2<80%），立即暫停評估
- **計算精確**：Pass 率 = Pass ÷ 總數 × 100%，保留一位小數
- **與問題追蹤同步**：失敗案例先入問題追蹤區，再更新摘要

---

## 測試資料清理規則

- E2E 建立的 workflow 一律以 `e2e-<caseId>-<runTag>` 前綴命名。
- **只刪本次執行自己建立、且記得 id 的資料**：建立後把 id 記在腳本變數中，收尾逐一 `POST /llm/workflow/delete`。
- 收尾可用 `e2e-<runTag>` 前綴掃描兜底，但**必須帶本次的 runTag**，不可只比對 `e2e-` 或其他泛用樣式。
- **不得另建備份檔／目錄**；種子資料（admin、test_user、LLM setting）為前置條件，不刪除。
- 逐步截圖屬**報告證據**，隨報告保留於 `docs/test-confirmations/e2e-shots/<報告時間戳>/`，**不在清理範圍**。

> ⚠️ 收尾後資料庫殘留本次的 `e2e-<runTag>` 資料，視同流程不完整，需補清理。

### 本次清理紀錄（Step 6，全部依 id 刪除）

| workflow id | 名稱 | 來源 | 刪除結果 |
|---|---|---|:---:|
| `89bdf1a3-b728-4fd6-9ad1-c75c753ff56c` | e2e-J11-202608202139 | J11 final script | ✅ |
| `c50c3582-1b68-47f7-99b0-a8577025b521` | e2e-J12-202608202139 | J12 final script | ✅ |
| `2bcc55cf-c230-4a94-8a41-abb0ec91a05a` | e2e-J13-202608202139 | J13 final script | ✅ |
| `9098d0b0-7b7e-4551-90c1-fa4ced8d0530` | e2e-J14-202608202139 | J14 final script | ✅ |
| `4c608e75-8b61-4bbe-935e-95cb3c169a20` | **未命名流程** | **F4 腳本誤建**（reload 後存檔） | ✅ |

> ⚠️ 最後一筆名稱與使用者自己的流程**完全相同**（使用者的是 `b576cf61-…`）。
> 依本節鐵則，刪除依據**只用本次記錄的 id**，全程未以名稱為條件——
> 使用者的 `b576cf61-…` 未被觸及。清理後列表已與測試前逐筆相同（4 筆）。

> 🚫 **絕不可依「使用者也會自然產生的名稱」批次刪除**（實際事故）：
> 曾為了清掉自己腳本的殘留，把清理條件加上 `name === '未命名流程'`——
> 而那正是使用者按「新建 workflow」未命名就存檔時的預設名稱，
> 導致測試輪次的前置清理**刪掉了使用者自己的流程**。
> 名稱樣式無法區分「測試產生」與「使用者資料」；**唯一安全的依據是本次執行記錄下來的 id**。
>
> 同理，`llm_workflow_execution` / `llm_workflow_node_execution` 不會隨 workflow 連鎖刪除，
> 若要清理必須以「本次執行產生的 execution id」為範圍，不可依日期或「孤兒」條件整批刪。

---

## 問題追蹤區

> 失敗（❌）項目在此詳記

> ⚠️ **本次 26 個案例全數 ✅，無 ❌**。下表記錄的是實測過程中發現的
> **實作與規格／文件不符之處**，皆非本次測試的失敗項，但屬**應追蹤的產品／文件議題**。
> 依 Step 5.8，**未經使用者核准不修改任何產品程式碼**——本次全部僅記錄，未動任何 `src`。

| # | 類別 | 現象 | 規格/文件怎麼寫 | 實際行為 | 根因（檔案） | 建議處置 |
|---|------|------|----------------|---------|------------|---------|
| #1 | 後端（i18n 違規） | HTTP 節點非 2xx 的錯誤訊息為寫死英文 `Unexpected code 404` | `requirements.md` AC-D7 規定回 `WORKFLOW_HTTP_REQUEST_FAILED` | 拋帶英文字串的 `IOException`；`AppMessage` **無**該鍵 | `HttpRequestExecutor.call()` | 補 `AppMessage` 鍵並改用 `ServiceException`；違反 [`i18n-messages.md`](../../.claude/rules/i18n-messages.md)「業務訊息禁止寫死字串」 |
| #2 | 後端（i18n 違規） | 節點逾時訊息為寫死英文 `Node <key> execution timed out after N ms` | 同上規範 | 引擎層節點逾時，非 i18n 訊息 | `WorkflowEngine` 的節點逾時處理 | 同 #1。另注意 `CodeExecutor` 自身的 `workflow.code.timeout` **未被觸發**——攔截點在引擎層，兩層逾時語義需釐清 |
| #3 | 前端（能力缺口） | **UI 無法提供 `inputPayload`**，從編輯器執行時 TRIGGER 永遠輸出 `{}` | `constants/nodeDocs.ts` 教使用者用 `{{觸發節點key.欄位}}` 取值；API 亦支援 `inputPayload` | 前端從未送出該欄位 | `api/workflowExecution.ts:43`（註解自承「之後也應該放這裡」） | 補執行前的 inputPayload 輸入 UI；在此之前 `nodeDocs` 的說明對使用者不可達。**已連帶使 `e2e-test-plan.md` §J12 的原圖形不可達**（本次改用 CODE 供應陣列） |
| #4 | 後端（規格不符） | LOOP 超過 `maxIterations` 為**靜默截斷**，整體仍 SUCCESS | `requirements.md` AC-D6 規定回 `WORKFLOW_LOOP_LIMIT_EXCEEDED` | 僅 `logger.warn` 後取前 N 筆；`AppMessage` **無**該鍵 | `WorkflowEngine.executeLoopNode()` | 決定要「截斷」還是「報錯」，二擇一後同步 AC-D6 與 `e2e-test-plan.md` §J12-06 |
| #5 | 後端（文件漂移，僅鍵名） | CONDITION 運算子錯誤回 `workflow.condition.operator.not.supported` | AC-D5 寫 `WORKFLOW_CONDITION_EVAL_FAILED` | 該鍵不存在，實際鍵名不同 | `ConditionExecutor.evaluate()` | 修正 AC-D5 文字（行為本身正確） |
| #6 | 易用性（設計取捨） | 引用「被 SKIPPED 分支」或「對字串下鑽」的變數會讓**下游節點整顆 FAILED** | 無明文規範 | 回 `Variable not found: <path>`，節點 FAILED、其下游 SKIPPED | `ExecutionContext.resolvePath` | 影響**分支匯流**寫法：OUTPUT 無法同時引用 true/false 兩側分支的輸出。是否提供「容錯引用」（缺值視為空）值得評估 |
| #7 | 易用性（型別損失） | OUTPUT 節點會把原生型別（陣列／物件）序列化成字串 | 無明文規範 | 得到 `{"all": "[{\"doubled\":1},…]"}` 而非陣列 | `OutputNodeConfig.mappings: Map<String,String>` | 若要保留結構需改型別；目前斷言結構化資料**必須看上游節點自身的 output**，不可透過 OUTPUT |

---

## 問題分流與修正紀錄

> 每個 ❌ 都必須在此完成分流（Step 5.7）；未經使用者核准不得修改產品程式碼（Step 5.8）。
> 「分流判定」只能填：`前端` / `後端` / `測試腳本` / `環境`（後兩者不計為產品缺陷）。

> 本次**無產品缺陷（前端／後端）需修正**，故未進入 Step 5.8 核准流程，**未修改任何產品程式碼**。
> 下列為執行過程中出現、且**當場判定為「測試腳本」層級**的失敗（不計為產品缺陷），記錄以供後續撰寫腳本者避雷。

| # | 案例 ID | 發現輪次 | 現象 | 分流判定 | 判定依據 | 根因 | 處置 | 重測結果 |
|---|---------|:-------:|------|---------|---------|------|------|---------|
| F1 | J11-01 | R1 | `getByTestId('create-button')` 找不到，但截圖顯示已成功登入並渲染列表 | `測試腳本` | 截圖顯示畫面正常；console error = 0 | 專案用 `data-test`，Playwright 預設找 `data-testid`；獨立腳本未設 `selectors.setTestIdAttribute('data-test')` | 腳本補設定 | ✅ |
| F2 | J11-01 | R1 | TRIGGER 節點找不到 `mode-toggle` | `測試腳本` | 截圖顯示 Inspector 呈現 TriggerForm 且值已為 MANUAL | TRIGGER **有**型別化表單、不走 JsonConfigEditor（舊 J5 spec 的寫法已過時） | 改為直接讀 `trigger-type`，不填 config | ✅ |
| F3 | J11-01 | R1 | 第二個節點填 config 時 `raw-input` 不可見 | `測試腳本` | 逐步追蹤 toggle 狀態 | JSON 編輯器的模式是**元件層記憶**的，已在 JSON 模式時再按 toggle 會切回表格模式 | 改為「已可見就不按」的幂等寫法 | ✅ |
| F4 | J11-06 | R1 | `page.reload()` 後畫布空白，且產生一筆多餘的 `未命名流程` | `測試腳本` | 列表前後筆數比對；查得新 id `4c608e75…` | **編輯器 URL 不帶 workflow id**（`e2e-test-plan.md` §J2-02 已載明），reload 等同開一張新流程 | 改為從列表點「編輯」鈕載入；殘留資料已於 Step 6 **依 id** 刪除 | ✅ |
| F5 | J13-01 | R1 | 最右側 OUTPUT 節點點不到（Playwright 報 `.inspector-panel` 攔截 pointer events） | `測試腳本` | Playwright call log 明確指出攔截元素 | 8 節點時畫布寬度不足，最右節點落在 Inspector 面板下方 | 節點座標內縮 ＋ 點擊前先按 `zoom-fit` | ✅ |
| F6 | J12-04 / J13-02 / J13-03 | R1 | 斷言失敗，但迴圈／分支邏輯其實完全正確 | `測試腳本` | DB `node_execution.output` 顯示受測節點輸出正確 | 斷言透過 OUTPUT 節點讀值：①OUTPUT 會把原生型別轉字串（見問題追蹤 #7）②引用被 SKIPPED 分支會讓 OUTPUT FAILED（見 #6） | 斷言改讀**受測節點自身**的 `node_execution.output` | ✅ |

---

## 第二輪重測結果

> 僅在使用者核准修正並完成改碼後填寫。**重測範圍＝第一輪選定的全部旅程**（不是只重測失敗案例）。
> 截圖存同一個 `e2e-shots/<報告時間戳>/`，檔名加 `R2-` 前綴。

| 輪次 | 重測範圍 | 觸發原因（修了什麼） | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|:----:|---------|-------------------|------|--------|--------|---------|---------|
| R2 | — | **不適用**：R1 選定範圍 26/26 全數通過，無產品缺陷需修正，未改動任何產品程式碼 | — | — | — | — | — |

### 與第一輪差異對照

| 案例 ID | R1 | R2 | 判定 |
|---------|:--:|:--:|------|
| | | | 已修復 / 未修復 / **新回歸** |

> ⚠️ 出現「新回歸」（R1 ✅ → R2 ❌）必須回到 Step 5.7 重新分流，不得直接收尾。

---

## 服務生命週期紀錄

| 輪次 | 停服務（port 淨空） | 後端啟動 | 前端啟動 | 健康檢查 200 | 測試起訖 | 關閉服務 |
|:----:|------------------|---------|---------|------------|---------|---------|
| R1 | ✅ 21:40（80/5173/4173 皆無 listener，hook 實測通過） | ✅ 21:41 dev profile uber-jar | ✅ 21:41 `npm run preview` 4173 | ✅ `/systemSetting/list` 200、`/view/chat` 200、前端 4173 200 | 21:42 – 22:26 | ✅ 22:27（80/5173/4173 淨空） |

---

## 常見問題排查

| 現象 | 可能原因 | 處理 |
|------|---------|------|
| 前置檢查後端非 200 | 服務未起 / port 80 被占用 | 起服務或釋放 port 後重試 |
| J5 一直逾時 | LLM api_key 無效 / 網路 | 確認 E2E_LLM_ID 有效；逾時放寬至 60–120s |
| 拖拉節點無效 | palette→畫布是 HTML5 原生 DnD，非滑鼠序列 | 合成 `dragstart`/`dragover`/`drop`（共用 `DataTransfer`）；**連線**才用 `mousedown→move→up`。放置後斷言節點數（詳見 `e2e-test-plan.md` §6） |
| 選擇器找不到 | Playwright 預設找 `data-testid`，專案用 `data-test` | config 設 `testIdAttribute:'data-test'` |
| llmId/toolId selectOption 找不到選項 | 運行 DB 的 ID 與種子檔漂移 | 由 global-setup 依 alias/platform 動態解析，勿硬編（詳見 §6） |
| 登入態失效 | storageState 過期 | 重新以 API 登入產生 storageState |
| 殘留 e2e-* 資料 | 清理未執行 | 以前綴掃描 workflow/list 兜底刪除 |
