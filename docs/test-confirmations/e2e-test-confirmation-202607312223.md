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
4. 依 J1→J9 逐旅程執行，**先記錄證據再標狀態**
5. 每條旅程完成後立即更新「測試結果摘要」與 Pass 率
6. 所有 ❌ 項目在「問題追蹤區」建立記錄
7. 收尾清理 `e2e-*` 測試資料

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
| 測試日期 | 2026-07-31 22:23 |
| 服務版本 | 0.1.8-SNAPSHOT（runner jar 重編於 2026-07-31 22:14） |
| 測試環境 | dev |
| 瀏覽器 | chromium |
| 前端 baseURL | `http://localhost:4173`（dist 重建於 2026-07-31 22:24） |
| 後端 API URL | `http://localhost:80` |
| LLM 平台（J10） | OpenRouter · `deepseek/deepseek-v3.2` |
| E2E_LLM_ID（J10） | `583b9222-8cb0-4109-b072-5f0fd1e9fed9` |
| 登入身分（設定擁有者） | `admin@bestpartner.com.tw`（`c88f57c8-ad26-4ea0-9f71-a65995b49357`） |
| J10 資源 ID | mcpId `7c173def-f3f8-4cc1-90db-5c7ede6a21c8`／userSettingId `1e575cec-3229-4da6-94d1-924dc56060cb`／toolId `f95fda5f-4632-4a1a-9a21-2d4facbd4279`／toolSettingId `d7f3f706-1a86-477c-a119-4b77e98bf788`／skillId `e39a44eb-34f3-44b6-a9c1-36eb969af953` |
| 本次範圍 | **定向切片**：僅執行 J10（複合能力掛載），J1–J9 本次未執行 |
| 測試結束時間 | 2026-07-31 22:55（服務已於此時關閉） |

---

## 前置檢查清單（測試前必須全部 ✅）

```
☑ 後端服務於 port 80 健康回應（GET /systemSetting/list → HTTP 200）
☑ PostgreSQL 已初始化（bestpartner-ddl.sql + bestpartner-init-data.sql）
☑ 種子資料存在：admin、test_user 及其 LLM setting
☑ J10 LLM 可用：`583b9222…`（OpenRouter deepseek-v3.2）最小 /llm/chat 回 200 "OK"
—  J8 不適用（本次為 J10 定向切片）
☑ J10 可用：google_map MCP jar 可啟動（重建 uber-jar 後 initialize 成功、tools/list 回 7 工具）、
   google_map userSetting 與 TavilySearch toolSetting 皆已建立且金鑰實測有效、pdf skill 已上傳，
   四者與 LLM setting 同屬 admin
☑ 前端已 build（dist 22:24 重建）並可由 baseURL 存取（HTTP 200）
☑ Playwright chromium 已安裝（ms-playwright/chromium-1228）
☑ 已由 API 登入取得 admin JWT
```

> ⚠️ 任一項未通過即停止測試並回報。

### 前置修復紀錄（本次測試前執行）

| 項目 | 問題 | 處置 |
|------|------|------|
| google_map MCP jar | `D:/MCP/google-map-1.0-SNAPSHOT.jar` 為 24,935 bytes 的 **thin jar**、無 `Main-Class`，`java -jar` 報「沒有主要資訊清單屬性」 | 於 `D:\projects\mcp-servers` 執行 `./gradlew :google-map:clean :google-map:build -x test`，將 **`build/google-map-1.0-SNAPSHOT-runner.jar`（67MB uber-jar）** 複製覆蓋；原檔備份為 `.thin-bak`。根因：當初複製了 `build/libs/` 的 thin jar 而非 `build/` 的 runner jar |
| 後端 runner jar | 建置時間 07-30 21:31 早於最後一次後端 commit `6f7b846`（07-30 21:51） | 重編 uber-jar（07-31 22:14），符合 §12 IS-1 教訓 |
| 前端 dist | 建置時間 07-30 21:32 早於最後一次前端 commit | `npm run build` 重建（07-31 22:24） |
| TavilySearch 設定 | `llm_tool_user_setting` 無任何 Tavily 筆數（等於無 api_key） | 以 `/llm/tool/saveSetting` 建立 `e2e-j10-tavily` |
| google_map 使用者設定 | admin 名下無 google_map userSetting（原有的屬 test_user） | 以 `/llm/mcpServer/saveSetting` 建立 `e2e-j10-googlemap` |
| pdf skill | `llm_skill` 為 0 筆 | 將 anthropics `skills/pdf` 打包為 `pdf.zip`（`SKILL.md`→`skill.md`）上傳 |
| 執行身分 | 原訂 test_user，但其 OpenAI／Gemini／Anthropic 金鑰皆已失效（401 `invalid_api_key`、`API_KEY_INVALID`） | 改用 admin（OpenRouter deepseek-v3.2 實測可用），三項資源一併建於 admin 名下以滿足「執行身分＝設定擁有者」 |

---

## 測試結果摘要

> 每完成一條旅程立即更新本表

| 旅程 | 優先 | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|------|:---:|------|--------|--------|---------|---------|
| J1 認證 | P0/P1/P2 | 9 | — | — | — | 本次未執行 |
| J2 Workflow 列表 | P0/P1 | 4 | — | — | — | 本次未執行 |
| J3 畫布編輯 | P0/P1 | 7 | — | — | — | 本次未執行 |
| J4 驗證與啟用 | P1 | 8 | — | — | — | 本次未執行 |
| J5 執行（真實 LLM） | P0/P1 | 12 | — | — | — | 本次未執行 |
| J6 Inspector 表單 | P0/P1/P2 | 9 | — | — | — | 本次未執行 |
| J7 契約錯誤 | P2 | 2 | — | — | — | 本次未執行 |
| J8-A 知識庫 RAG（pipeline） | P1 | 5 | — | — | — | 本次未執行 |
| J8-B 知識庫 RAG（外掛） | P1 | 5 | — | — | — | 本次未執行 |
| J9 編輯器外觀與節點編輯頁 | P0/P1/P2 | 17 | — | — | — | 本次未執行 |
| **J10 複合能力掛載（旅遊行程規劃）** | P1/P2 | 8 | **8** | **0** | 0 | **100%** |
| **合計（本次執行範圍）** | | **8** | **8** | **0** | **0** | **100%** |

> 本次為 J10 定向切片，J1–J9 未執行。
>
> **首輪 7 ✅ / 1 ❌（87.5%）**，失敗的 J10-06 定位出兩個後端缺陷（ISSUE-1／ISSUE-2）；
> **修復後重驗 J10-06 通過，本旅程達 100%**（P1 標準 ≥95%）。
>
> ✅ **7 節點完整配置（三種能力節點全部掛到 LLM 的 `in:tool`）已於 run_14 驗證通過**：
> `MCP_SERVER(google_map)` ＋ `TOOL(TavilySearch)` ＋ `SKILL(pdf)` 同時掛載，
> 30 秒完成、execution SUCCESS、`node_execution` 恰 4 筆（三個純能力節點不落紀錄），
> 且同時取得**真實景點（place_id ＋ 座標）與真實活動（含來源網址）**。
>
> ⚠️ 但完整配置的**工作量上限很低**：run_14 是「2 景點 ＋ 1 次搜尋」（3 次工具呼叫，30 秒）。
> 一旦放大到「4 景點 ＋ 1 次搜尋」，API 層實測需 **253 秒**，必然撞上 LLM 節點的 120 秒硬逾時
> （ISSUE-3）；工作量一大時模型也更容易被 pdf skill 帶去輸出 Python 腳本（ISSUE-5）。
> 兩者皆非本次修復範圍，已登錄問題追蹤區。

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

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J1-01 | P0 | 未登入直接訪 `/` | 導向 `/login` | | |
| J1-02 | P0 | admin/admin 登入 | 導向列表 `/`，顯示已登入 | | |
| J1-03 | P1 | 已登入訪 `/login` | 導回首頁 `/` | | |
| J1-04 | P1 | 錯誤帳密登入 | 停留 `/login`，顯示錯誤 | | |
| J1-05 | P1 | 列表頁登出（`logout-button`） | 清 token 導向 `/login`；重訪受保護頁被導回登入 | | |
| J1-06 | P1 | 編輯器工具列登出（無未存，`logout-button`） | 清 token 導向 `/login`；per-user 快取一併清除 | | |
| J1-07 | P1 | 編輯器有未存變更登出 → 確認框選「登出」 | 先跳「尚未存檔」確認；確認後清 token 導向 `/login` | | |
| J1-08 | P1 | 編輯器有未存變更登出 → 確認框選「取消」 | 留在編輯器且仍為登入態（token 未清） | | |
| J1-09 | P2 | token 清除後訪受保護頁 | 導回 `/login` | | |

### J2 Workflow 列表（P0/P1）

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J2-01 | P0 | 列表載入 | 顯示既有 workflow 清單 | | |
| J2-02 | P0 | 建立新 workflow | 進 `/editor/:id`，DRAFT、version 1 | | |
| J2-03 | P1 | 點列表項進編輯器 | 載入完整定義 | | |
| J2-04 | P1 | 刪除 workflow | 列表移除，連鎖刪 node/edge | | |

### J3 畫布編輯（P0）

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J3-01 | P0 | 拖拉節點入畫布（TRIGGER/LLM_ASSISTANT/OUTPUT） | 節點出現，nodeKey 唯一 | | |
| J3-02 | P0 | 連線 edge | edge 建立，兩端點存在 | | |
| J3-03 | P0 | 選節點於 Inspector 填 config | 表單值寫回節點 | | |
| J3-04 | P0 | save 整張覆寫 | version+1 | | |
| J3-05 | P1 | 重整頁面 get 還原 | 畫布與 Inspector 與存檔一致 | | |
| J3-06 | P1 | 能力掛載：TOOL / MCP_SERVER / SKILL / **KNOWLEDGE_RAG** 的 `out:main` 連 LLM `in:tool` 並 save | edge 建立（`targetHandle=in:tool`）；重載後連線保留 | | |
| J3-07 | P1 | 多個 KNOWLEDGE_RAG 同時掛同一 LLM 的 `in:tool` | 皆放行、無相容性 toast；CONNECTIONS 計數與 edge 數一致 | | |
| J3-08 | P1 | 提示接線：PROMPT 的 `out:main` 連 LLM `in:prompt` 並 save | edge 建立（`targetHandle=in:prompt`）；重載後連線保留 | | |
| J3-09 | P1 | 兩個 PROMPT 各接 CONDITION 的 `out:true`/`out:false`，再同時連同一 LLM 的 `in:prompt` | 皆放行、無相容性 toast；CONNECTIONS 計數與 edge 數一致 | | |

### J4 驗證與啟用（P1）

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J4-01 | P1 | 缺 TRIGGER 啟用 | 400，對應訊息 | | |
| J4-02 | P1 | 圖有環啟用 | 400，對應訊息 | | |
| J4-03 | P1 | 缺必填 config 啟用（如缺 llmId） | 400，`workflow.node.config.required.missing`，顯示 nodeKey+欄位 | | |
| J4-04 | P1 | 合法圖 switchStatus 啟用 | 狀態轉 ACTIVE | | |
| J4-05 | P1 | **非能力來源節點**拉線到 LLM `in:tool`（白名單＝TOOL / MCP_SERVER / SKILL / KNOWLEDGE_RAG） | `onConnect` 即擋：toast「僅工具、MCP、Skill、知識庫節點可連到 LLM 的工具埠」，**edge 不建立**（CONNECTIONS 不變）；建議併測對照組（能力來源連同一埠應成功） | | |
| J4-06 | P1 | KNOWLEDGE_RAG 掛 `in:tool`（外掛模式）且 `query` / `embeddingModelId` 留空即啟用 | **啟用成功**（外掛模式僅需 `knowledgeId`） | | |
| J4-07 | P1 | 孤兒 SKILL 節點（未連任何 LLM `in:tool`）save → 啟用 | save 放行；**switchStatus 回 400** `workflow.skill.node.not.mounted`（含 nodeKey），開關維持關閉、狀態仍 DRAFT。⚠️ SKILL 須先填 `skillId`，否則先撞 REQUIRED_MISSING 而測不到本案例 | | |
| J4-08 | P1 | SKILL 已掛載到 LLM `in:tool` 後啟用（J4-07 對照組） | 啟用成功，狀態轉 ACTIVE | | |
| J4-09 | P1 | **非 PROMPT 節點**拉線到 LLM `in:prompt`（如 TRIGGER） | `onConnect` 即擋：toast「僅提示詞節點可連到 LLM 的提示埠」，**edge 不建立**（CONNECTIONS 不變） | | |
| J4-10 | P1 | 孤兒 PROMPT 節點（未連任何 LLM `in:prompt`）save → 啟用 | save 放行；**switchStatus 回 400** `workflow.prompt.node.not.connected`（含 nodeKey），狀態仍 DRAFT。⚠️ PROMPT 須先填 `prompt`，否則先撞 REQUIRED_MISSING | | |
| J4-11 | P1 | LLM `userPrompt` 留空且無 PROMPT 連入時啟用 | **400** `workflow.llm.prompt.required`（含 LLM 的 nodeKey），狀態仍 DRAFT | | |
| J4-12 | P1 | LLM `userPrompt` 留空但已有 PROMPT 連入（J4-10/11 對照組） | 啟用成功，狀態轉 ACTIVE | | |

> ⚠️ **KNOWLEDGE_RAG 兩種模式的必填欄位相反，勿寫反預期**：
> 掛 LLM `in:tool`＝外掛模式，僅需 `knowledgeId`；連一般 `in:main`＝pipeline 模式，另需 `query`。
> 白名單事實來源：後端 `WorkflowEngine.CAPABILITY_SOURCE_TYPES` ≡ 前端 `useGraphValidation.CAPABILITY_SOURCE_TYPES`。

> ⚠️ **`in:prompt` 與 `in:tool` 語義相反，勿寫反預期**：
> `in:tool` 允許 TOOL/MCP_SERVER/SKILL/KNOWLEDGE_RAG，**非資料流**（純能力節點不執行、不落紀錄）；
> `in:prompt` 只允許 PROMPT，**是資料流**（參與活化，來源節點照常執行並落紀錄）。
> 驗證順序：必填欄位 → 孤兒 SKILL → 孤兒 PROMPT → LLM 無提問來源。

### J5 執行 workflow（P0，真實 LLM）

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J5-01 | P0 | 合法 workflow（TRIGGER→LLM_ASSISTANT→OUTPUT）execute | SSE `started`→`node.*`→`completed(SUCCESS)`；DB 寫入 `llm_workflow_execution`/`llm_workflow_node_execution` | | |
| J5-02 | P1 | 節點設定錯誤導致失敗 | 該節點 `node.failed` 顯示錯誤，執行標記失敗 | | |
| J5-03 | P1 | 執行中途 client 斷線 | 執行 `CANCELLED`，下游未執行節點 `SKIPPED`（commit b24c128 視覺） | | |
| J5-04 | P1 | 外掛型 RAG：KNOWLEDGE_RAG 以 `out:main → LLM in:tool` 掛載後 execute | 推論前自動檢索注入（非 LLM 主動呼叫）；**不產生**獨立節點事件、`node_execution` 無其紀錄。內容正確性斷言歸 J8-B | | |
| J5-05 | P1 | Agent 模式：TOOL / MCP_SERVER / SKILL 以 `out:main → LLM in:tool` 掛載後 execute | LLM 可自主呼叫掛載工具並回覆；能力節點**不產生**獨立節點事件、`node_execution` 無其紀錄 | | |
| J5-06 | P1 | 提示節點驅動：PROMPT → `LLM in:prompt` → OUTPUT（LLM `userPrompt` 留空）execute | PROMPT **照常**產生 `node.started`/`node.completed` 與 `node_execution` 紀錄（與能力節點相反）；LLM 以該提示提問並成功回覆 | | |
| J5-07 | P1 | 分支擇一：CONDITION 兩分支各接一 PROMPT 匯入同一 LLM 後 execute | 被活化分支的 PROMPT 為 SUCCESS、另一顆 `node_execution` 為 SKIPPED；回覆風格對應被活化那條 | | |
| J5-08 | P1 | 優先序：LLM 同時有 `userPrompt` 與 PROMPT 連入 | 以 PROMPT 輸出為準；刪 `in:prompt` 連線後再執行改用 `userPrompt`。⚠️ 只刪連線會讓 PROMPT 變孤兒而被防線擋下，須連該節點一起刪 | | |
| J5-09 | P1 | execute 被後端擋下時（如含孤兒 PROMPT 節點）檢視執行結果面板 | 顯示**後端業務訊息**（含 nodeKey 與應連往的埠），**不得**只顯示 `HTTP 400`。⚠️ execute 是唯一走原生 `fetch`、不經 axios 攔截器的端點，訊息解析在 `api/workflowExecution.ts` 自行實作，改該檔須重驗本條 | | |
| J5-10 | P0 | 多觸發點：兩顆 TRIGGER 各接一分支匯流到同一 LLM，點工具列執行鈕 | 出現 `trigger-picker` 列出兩個 `trigger-option-<nodeKey>`（顯示節點名稱），尚未開始執行 | | |
| J5-11 | P0 | 選定 t1 後執行 | 只有 t1 分支節點亮起；另一顆 TRIGGER 呈 `.is-exec-skipped`（前端預標，後端對 SKIPPED 不發事件）；LLM 取 t1 分支的提問；`llm_workflow_execution.trigger_node_key`=`t1` | | |
| J5-12 | P1 | 改從 t2 執行 / 節點卡 `node-run-from-here` 直接開跑 | 前一輪 skipped 標記不殘留、改為 t1 呈 skipped、`trigger_node_key`=`t2`；節點卡入口不觸發拖曳或取消選取 | | |

> J5 斷言不比對 LLM 輸出文字；只驗事件序列、節點狀態、DB 紀錄。缺 E2E_LLM_ID 時 J5-01/J5-03 標 ⏭️。

### J6 Inspector 表單（P1）

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J6-05 | P0 | 自 palette 拖入 TRIGGER 後直接看 Inspector | `trigger-type` 下拉顯示且值已為 `MANUAL`，**無必填警告**（由 `NodeTypeMeta.defaultConfig` 帶入）；WEBHOOK / CRON 停用 | | |
| J6-06 | P1 | 無型別化表單的節點（如 CODE）在 JSON 編輯器**表格模式**新增／移除欄位 | `new-field-key` + `add-field` 可新增（空鍵名／重複鍵顯示 `new-field-error`）、`remove-field-<key>` 可移除；不必切 JSON 模式手打 | | |
| J6-07 | P0 | 選取尚未設定的 OUTPUT 節點 | 顯示 `output-both-empty`（擇一必填）與 `output-ref-suggestions`（chip 顯示為「節點名 › 欄位」）；點 chip 插入後提示消失。⚠️ 建議鍵取自實際執行輸出，未執行則用 `nodeOutputKeys.ts` 的型別預設鍵（手抄後端 executor，改後端須同步） | | |
| J6-08 | P0 | 檢視含引用的輸出模板 | 引用渲染為 `expr-token-<path>` 色塊（文字＝「節點名 › 欄位」、`data-ref`＝原始 `{{...}}`）；節點改名後文字跟著變、`data-ref` 不變；引用不存在節點時帶 `is-unknown`。⚠️ 此欄位是 **contenteditable 不是 textarea**，斷言用 `textContent()` 而非 `inputValue()` | | |
| J6-09 | P1 | 於輸出模板以中文輸入法打字 | 組字不吃字、不寫入半成品；commit 後文字完整且既有色塊不受影響。存檔後 DB 的 `template` 為「原始 `{{...}}` ＋ 中文」，不含畫面顯示文字 | | |
| J6-01 | P1 | LlmAssistantForm 選 llmId | 值寫回並可存檔 | | |
| J6-02 | P1 | Tool/McpServer/KnowledgeRag/Prompt/Output 表單填寫 | config 正確寫回，save 後重載一致（PromptForm 填 `prompt-text`、選填 `output-key`） | | |
| J6-04 | P1 | LLM 接上 PROMPT 後檢視 LlmAssistantForm | 顯示 `prompt-overridden-badge` 與 `prompt-source-hint`；`user-prompt` 仍可編輯（非 disabled） | | |
| J6-03 | P2 | settingSchema 動態表單（sensitive 遮罩） | 依 schema 正確渲染欄位型別 | | |

### J7 契約錯誤（P2）

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J7-01 | P2 | save 節點 config 型別錯誤（未知欄位/結構型別錯） | 400，`workflow.node.config.invalid`，含 nodeKey | | |
| J7-02 | P2 | 樂觀鎖 version 衝突 | 400，`workflow.version.conflict`，UI 顯示衝突提示 | | |

### J8 知識庫 RAG 檢索問答（P1，真實 embedding + Milvus + 真實 LLM）

> 前端無向量庫/設定/上傳管理頁（僅 login/列表/編輯器），前置三步走 API 準備，UI 只建 workflow 與執行。
> 與 J5 差異：**有已知來源文件，斷言輸出內容與文件事實相符**（先讀文件挑唯一事實作查詢關鍵字→預期答案）。
> KNOWLEDGE_RAG 有 pipeline / 外掛兩模式，故分 J8-A / J8-B 兩組。詳見 `docs/e2e-test-plan.md` §3-J8、§13（A）、§14（B）。

#### J8-A pipeline 模式：`TRIGGER → KNOWLEDGE_RAG → LLM_ASSISTANT → OUTPUT`

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J8-01 | P1 | 前置(API)：建 EMBEDDING 設定 + Milvus 向量庫 + 上傳文件建知識庫 | 取得 embeddingModelId/embeddingStoreId/knowledgeId；`getDataFromEmbeddingStore` 檢索命中目標事實（維度須對齊） | | |
| J8-02 | P1 | UI 建 workflow：拖 TRIGGER/KNOWLEDGE_RAG/LLM_ASSISTANT/OUTPUT，Inspector 選知識庫/embedding/LLM，userPrompt 以 `{{<ragKey>.documents}}` 串接 | 4 節點、3 edge（nodeKey 由 `.vue-flow__node[data-id]` 讀取供插值） | | |
| J8-03 | P1 | 存檔 + switchStatus 啟用 | version 1；KNOWLEDGE_RAG 必填驗證通過（pipeline 需 `query`），狀態 ACTIVE | | |
| J8-04 | P1 | execute（真實 embedding + LLM） | SSE started→node.*→completed(SUCCESS)；4 節點 node_execution 皆 SUCCESS、DB 落庫 | | |
| J8-05 | P1 | **RAG 正確性斷言**：finalOutput 與文件已知事實比對 | LLM 輸出含文件原文事實關鍵字（例：渣打 TNC「年滿二十歲」→ 輸出含「20/二十」） | | |

#### J8-B 外掛模式：KNOWLEDGE_RAG `out:main → LLM in:tool`（自動注入型 RAG）

> 圖形為 `TRIGGER → LLM_ASSISTANT → OUTPUT`，另掛 N 個 KNOWLEDGE_RAG 到 LLM `in:tool`。
> **userPrompt 只寫問題，不做任何 `{{...}}` 知識插值**。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J8-06 | P1 | UI 建圖：5 節點（TRIGGER/LLM_ASSISTANT/OUTPUT/2×KNOWLEDGE_RAG）、4 edge，其中 2 條為 RAG `out:main`→LLM `in:tool` | 拉線放行、無相容性 toast、console error=0；Overview 顯示 5 NODES / 4 CONNECTIONS；存檔成功 | | |
| J8-07 | P1 | **必填契約**：兩個 RAG 只選知識庫，`query`/`embeddingModelId` 皆留空後啟用 | 啟用成功（不回 `workflow.node.config.required.missing`） | | |
| J8-08 | P1 | execute（真實 embedding + LLM） | SSE started→**僅 trigger/llm/output 三節點** node.*→completed(SUCCESS) | | |
| J8-09 | P1 | **純能力節點不落主遍歷**：執行後檢視 RAG 節點 | RAG 節點 Inspector **無**「本次執行」區塊（`data-test="node-exec-section"`）；`llm_workflow_node_execution` 無 `KNOWLEDGE_RAG` 紀錄 | | |
| J8-10 | P1 | **自動注入正確性斷言**：finalOutput 與文件事實比對，且需能與 LLM 常識答案區分 | 輸出為文件事實而非常識（渣打 TNC「年滿二十歲」→「20歲」，而非民法成年「18歲」） | | |

> ⚠️ **J8-10 的鑑別點要求**：所選事實必須**與 LLM 常識答案不同**，否則無法區分「注入生效」與「模型本來就知道」，該案例形同無效。
> 缺有效 embedding 或 CHAT 設定即 J8 全條 skip 並標註。embedding 選型與維度對齊、憑證解密管制等陷阱見 `e2e-test-plan.md` §13；
> 外掛模式的必填契約、Milvus 未啟動樣態、`deleteData` 不連鎖等陷阱見 §14。

### J9 編輯器外觀與節點編輯頁（P0/P1/P2）

> 純 UI 旅程、不需後端資料變更，可沿用 J3 建好的 workflow。詳見 `docs/e2e-test-plan.md` §3-J9。
> ⚠️ Node Designer 是**全屏遮罩**：每次回畫布操作前，先確認 `node-designer-modal` 已消失。
> ⚠️ 主題偏好存於 `localStorage.wf-theme`，會跨案例殘留；驗外觀前先明確設定或清除。

| # | 優先 | 案例 | 預期 | 狀態 | 證據 |
|---|:---:|------|------|:---:|------|
| J9-01 | P1 | 清空 `localStorage` 後進編輯器 | `.wf-editor` 的 `data-wf-theme="light"` | | |
| J9-02 | P1 | 點 `theme-toggle` 兩次 | 深色↔淺色來回無殘留區塊；`localStorage.wf-theme` 同步；重整後保留 | | |
| J9-03 | P1 | 深色下導到 `/login` 與列表頁 | `<html>` 的 `dark` class 已移除，兩頁維持淺色 | | |
| J9-04 | P2 | 深色下觸發 `ElMessageBox`（未存離開／登出確認） | 對話框深色配色且文字可讀 | | |
| J9-05 | P1 | 點 `zoom-in` / `zoom-out` / `zoom-fit` | 畫布縮放改變，`zoom-value` 百分比同步更新 | | |
| J9-06 | P0 | 雙擊節點 | 出現 `node-designer-modal`；header 顯示型別／分類／名稱；該節點同時被選取 | | |
| J9-07 | P1 | 點節點卡右上 `node-open-designer` | 開啟 Designer，且未觸發拖曳或取消選取 | | |
| J9-08 | P1 | 點 Inspector 的 `open-node-designer-button` | 開啟 Designer | | |
| J9-09 | P1 | 三種關閉：Esc、點遮罩、`node-designer-close` | 皆關閉；再開啟時分頁回到 Parameters | | |
| J9-10 | P0 | modal 內改 config → 關閉 | Inspector 與節點副標同步更新；工具列標記未存 | | |
| J9-11 | P1 | 在 `node-designer-name-input` 打字後按 Delete | 節點**不被刪除** | | |
| J9-12 | P1 | 未執行流程時檢視 Input / Output | 兩側皆顯示「尚未執行」空狀態 | | |
| J9-13 | P1 | 執行後開啟中段節點的 Designer | Input 顯示 `node-designer-input-json`（含「來自 <上游名>」）；Output 顯示 JSON 與可引用欄位。⚠️ **不可先關結果抽屜**——關閉鈕綁 `executionStore.reset()`，會清空 `nodeStates` | | |
| J9-14 | P1 | 開啟 TRIGGER 的 Designer | Input 為虛線空狀態「這是觸發節點…」，不顯示 JSON | | |
| J9-15 | P1 | 開啟 SKILL 的 Designer | Input「這是能力提供節點…」；Output「不會產生自己的輸出」。⚠️ SKILL **有** `out:main` 埠（能力掛載用），只看埠數會誤判 | | |
| J9-16 | P1 | 開啟已掛能力節點的 LLM 的 Designer | `node-designer-capability-list` 以 chip 列出 `in:tool` 來源，不顯示其 JSON | | |
| J9-17 | P2 | Settings 與 Docs 分頁（Docs 逐一檢視 13 種型別） | Settings 顯示 nodeKey／型別／分類／必填檢核／本次執行；Docs 皆有內容且連接埠清單與實際 handle 一致 | | |

### J10 複合能力掛載：旅遊行程規劃（P1，真實 MCP + 真實 Web 搜尋 + 真實 LLM）

> 圖形：`TRIGGER → LLM_ASSISTANT → OUTPUT`，`PROMPT → in:prompt`，
> `MCP_SERVER(google_map)` / `TOOL(TavilySearch)` / `SKILL(pdf)` 三者皆接 LLM 的 `in:tool`。
> ⚠️ MCP 節點**必須填 `userSettingId`**，否則 env 佔位符不替換、工具不可用（§12 第 1 點）。
> ⚠️ LLM／MCP／Tool／Skill 設定必須**同屬登入者**（§12 第 2 點）。
> ⚠️ `searchPlaces` 必填 `query`／`language`／`maxResults`，PROMPT 須要求模型帶齊。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------------|
| J10-01 | P1 | 拖出 7 節點並連線（3 條 `in:tool`、1 條 `in:prompt`、2 條 `out:main`） | 連線皆成立，畫布無驗證紅框 | ✅ | 節點數 7、edge 數先 5（SKILL 待接）後 6。<br>![J10-01-01 login-page](e2e-shots/202607312223/J10-01-01-login-page.png)<br>![J10-01-02 workflow-list](e2e-shots/202607312223/J10-01-02-workflow-list.png)<br>![J10-01-03 seven-nodes](e2e-shots/202607312223/J10-01-03-seven-nodes.png)<br>![J10-01-04 edges-without-skill](e2e-shots/202607312223/J10-01-04-edges-without-skill.png)<br>![J10-01-05 llm-config](e2e-shots/202607312223/J10-01-05-llm-config.png)<br>![J10-01-06 mcp-config](e2e-shots/202607312223/J10-01-06-mcp-config.png)<br>![J10-01-07 tool-config](e2e-shots/202607312223/J10-01-07-tool-config.png)<br>![J10-01-08 skill-config](e2e-shots/202607312223/J10-01-08-skill-config.png)<br>![J10-01-09 saved](e2e-shots/202607312223/J10-01-09-saved.png) |
| J10-02 | P1 | SKILL 未接 `in:tool` 即按啟用 | 擋下並回 `workflow.skill.node.not.mounted`，含 nodeKey | ✅ | 實際訊息：`SKILL node "tywRROJx" must be connected to an LLM assistant nodes tool input port (in:tool)`<br>![J10-02-10 activate-blocked](e2e-shots/202607312223/J10-02-10-activate-blocked.png) |
| J10-03 | P1 | 補回 SKILL 邊後啟用 | 啟用成功，狀態轉 ACTIVE | ✅ | edge 數 6；訊息「流程已啟用」，工具列 Active 開關轉綠。<br>![J10-03-11 skill-edge-connected](e2e-shots/202607312223/J10-03-11-skill-edge-connected.png)<br>![J10-03-12 activated](e2e-shots/202607312223/J10-03-12-activated.png) |
| J10-04 | P1 | 執行 workflow，觀察 SSE 與 ExecutionResultDrawer | `execution.started`→`node.*`→`execution.completed`，全節點 SUCCESS（不比對輸出文字） | ✅ | 抽屜由執行中轉 SUCCESS，耗時 48.05 秒。<br>![J10-04-13 drawer-running](e2e-shots/202607312223/J10-04-13-drawer-running.png)<br>![J10-04-14 status-SUCCESS](e2e-shots/202607312223/J10-04-14-status-SUCCESS.png) |
| J10-05 | P1 | deep-verify：景點真實性 | 景點為真實臺東地點（地址含「臺東縣」），每點帶 `place_id` 與經緯度 | ✅ | 4 個景點全部帶回真實資料：三仙台 `ChIJ5RcBzEhwbzQRSWjpXLndbWU`(23.120628, 121.402757)、鐵花村音樂聚落慢市集 `ChIJAzKH2hW5bzQRD7rKWL4N5KU`(22.753357, 121.146050, ★4.5, 950臺東縣臺東市新生路135巷26號)、伯朗大道 `ChIJZatyKu4JbzQRyHuSojQ6qQ0`(23.098806, 121.213002, ★4.4, 958臺東縣池上鄉)、知本溫泉 `ChIJ7yDuP97JbzQRr_mzjOYs7tc`(22.693044, 121.018124, ★4.2, 954臺東縣太麻里鄉)。<br>![J10-05-15 output-json](e2e-shots/202607312223/J10-05-15-output-json.png) |
| J10-06 | P1 | deep-verify：活動查證 | 含 TavilySearch 取得的活動／營業資訊與可追溯來源 URL | ✅<br>（修復後重驗） | **首輪 ❌ → 修復 ISSUE-1／ISSUE-2 後重驗通過。** 首輪 TavilySearch 完全未掛給模型（輸出 `"events": []`），以三段實驗定位（只掛 Tavily → `NO_TOOLS`；只掛 google_map MCP → 列出 7 工具；改掛 `DateTool` → 列出 `getCurrentTime`）。修復後重跑取得真實活動：**台灣國際熱氣球嘉年華 2026/7/4–8/20（`https://udn.com/news/story/7266/9635510`）**、**台東博覽會 2026/7/3–8/20（`https://eventgo.tw/event/71a20bb0-…`）**，與直接呼叫 Tavily API 的結果一致。<br>首次重驗（run_13）為求最快落地用了 6 節點變體（不掛 SKILL），**已於 run_14 補測完整 7 節點配置**（三種能力全掛 `in:tool`，30 秒 SUCCESS，活動同樣取得）。<br>![FIX J10-06 edges](e2e-shots/202607312223/FIX-J10-06-04-edges.png)<br>![FIX J10-04 SUCCESS](e2e-shots/202607312223/FIX-J10-04-11-status-SUCCESS.png)<br>![FIX J10-05 output](e2e-shots/202607312223/FIX-J10-05-12-output-json.png)<br>**完整配置（run_14）**：<br>![FULL 七節點](e2e-shots/202607312223/FULL-J10-01-03-seven-nodes.png)<br>![FULL SKILL 掛載](e2e-shots/202607312223/FULL-J10-03-11-skill-edge-connected.png)<br>![FULL SUCCESS](e2e-shots/202607312223/FULL-J10-04-14-status-SUCCESS.png)<br>![FULL 輸出含活動](e2e-shots/202607312223/FULL-J10-05-15-output-json.png) |
| J10-07 | P2 | DB 落庫檢查 | `llm_workflow_execution` 一筆 SUCCESS；`llm_workflow_node_execution` 恰 4 筆（純能力節點不落紀錄） | ✅ | execution `5e5b64fb-8958-414d-b3e6-9ec679a1d1a2` SUCCESS、`trigger_type=MANUAL`、`trigger_node_key=veDmLmOJ`；node_execution 恰 4 筆（TRIGGER/PROMPT/LLM_ASSISTANT/OUTPUT）皆 SUCCESS，**無 MCP_SERVER / TOOL / SKILL 列**，符合純能力節點不落紀錄的設計。 |
| J10-08 | P1 | 由 OUTPUT 產出 PDF 行程表 | PDF 含 Day1/Day2 行程、每景點 Google Maps 連結、活動資訊與來源 | ✅ | 產出 `e2e-artifacts/202607312223/taitung-2day-itinerary.pdf`（190KB、A4）。含 Day1/Day2 各 2 景點、每點 Place ID／座標／評分／`https://www.google.com/maps/search/?api=1&query=...&query_place_id=...` 連結與行車提示。**活動區塊因 ISSUE-2 留空並註明原因，未以任何外部資料填補。**<br>![PDF 預覽](e2e-artifacts/202607312223/taitung-2day-itinerary-preview.png) |

---

## 旅程完成檢查清單（每完成一條旅程必須立即執行）

> 對應 SKILL.md Step 5.5。進入下一旅程前，逐項勾選並更新摘要表。

```
☑ 所有案例的逐步截圖已擷取並存入 e2e-shots/202607312223/（15 張，每步驟一張）
☑ 所有案例證據已寫入（逐步圖片連結以 Markdown 圖片語法嵌入 + 實測值 / DB 斷言，無佔位符）
☑ 所有案例狀態已標記（7 ✅ / 1 ❌ / 0 ⏭️）
☑ 失敗案例（J10-06）已在「問題追蹤區」詳細記錄（ISSUE-2），另附 3 則過程中發現（ISSUE-1/3/4）
☑ 摘要表該旅程統計數字已更新（7 / 1 / 0 / 8）
☑ Pass 率已計算並填入（87.5%）
☑ 該旅程新增的 e2e-J10-202607312223 workflow 已依 id 逐一刪除（4 筆，HTTP 200）
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

> 🚫 **絕不可依「使用者也會自然產生的名稱」批次刪除**（實際事故）：
> 曾為了清掉自己腳本的殘留，把清理條件加上 `name === '未命名流程'`——
> 而那正是使用者按「新建 workflow」未命名就存檔時的預設名稱，
> 導致測試輪次的前置清理**刪掉了使用者自己的流程**。
> 名稱樣式無法區分「測試產生」與「使用者資料」；**唯一安全的依據是本次執行記錄下來的 id**。
>
> 同理，`llm_workflow_execution` / `llm_workflow_node_execution` 不會隨 workflow 連鎖刪除，
> 若要清理必須以「本次執行產生的 execution id」為範圍，不可依日期或「孤兒」條件整批刪。

### 本次清理結果

| 項目 | 處置 |
|------|------|
| workflow `e2e-J10-202607312223`（4 筆，含重跑產生的） | 依 id 逐一 `POST /llm/workflow/delete`，皆 HTTP 200 |
| 刪除後列表確認 | 殘留 0 筆；使用者原有 4 條流程（`未命名流程`、`手動測試-請問你是甚麼模型`、`手動測試-運算式編輯器`、`多觸發點驗證-202607302129`）**完好未動** |

### 保留資料（刻意不清理）

| 類型 | 識別碼 | 說明 |
|------|--------|------|
| MCP userSetting | `1e575cec-3229-4da6-94d1-924dc56060cb`（alias `e2e-j10-googlemap`） | admin 的 google_map 金鑰設定，屬環境資源，保留供後續重跑 |
| Tool setting | `d7f3f706-1a86-477c-a119-4b77e98bf788`（alias `e2e-j10-tavily`） | 同上；ISSUE-2 修復後可直接驗證 |
| Skill | `e39a44eb-34f3-44b6-a9c1-36eb969af953`（`pdf`） | 上傳的 anthropics pdf skill，檔案位於 `D:\tmp\bestpartner\upload\skills\c88f57c8-…\pdf` |
| MCP jar 備份 | `D:/MCP/google-map-1.0-SNAPSHOT.jar.thin-bak` | 修復前的 thin jar，確認無誤後可自行刪除 |

> ⚠️ 上述三項設定含**真實金鑰**。若日後要備份到 `docs/sql/bestpartner-init-data.sql`，
> `api_key` / `GOOGLE_MAPS_API_KEY` 必須掩蔽為 `tvly-xxx` / `AIza-xxx`。本次**未**寫入任何 SQL 備份。

---

## 問題追蹤區

> 失敗（❌）項目在此詳記

| 案例 ID | 現象 | 期望行為 | 實際行為 | 根因 | 狀態 |
|---------|------|---------|---------|------|------|
| ISSUE-1<br>（J10-01 前置） | TOOL 節點填了 `toolSettingId` 後執行必失敗 | 依 settingId 取得該筆工具設定並建構工具 | 執行即 FAILED，`error_message = User tool setting not found`（execution `10f19299…`，62ms 內失敗） | `ToolService.buildToolWithSetting(toolSettingId)` 把 **settingId** 傳進 `findSettingByUserIdAndToolId(userId, toolId)`（查詢條件為 `userId = ? AND toolId = ?`）——比對對象是 `tool_id` 欄位，settingId 永遠不可能相等。能力掛載路徑亦相同（`LLMService.kt`） | ✅ **已修復**（見下方修復紀錄） |
| ISSUE-2<br>（J10-06） | BUILT_IN 且有 config class 的工具（TavilySearch）不會出現在模型可用工具中 | 模型能呼叫 `searchWeb` 查詢台東活動 | 模型回報 `NO_TOOLS`；輸出 `"events": []` | **`toolIds` 走的是 `ToolService.buildToolWithoutSetting`，該方法對 `configObjectPath` 非空的工具一律 `return null`**（`工具 X 無法使用`）。TavilySearch／GoogleSearch 皆有 config class 故被靜默丟棄；`DateTool` 無 config class 才能通過。與 ISSUE-1 合併效果是「有設定的 BUILT_IN 工具完全無法使用」——只填 toolId 被拒、改填 toolSettingId 又查不到 | ✅ **已修復**（見下方修復紀錄） |
| ISSUE-5<br>（重驗時發現） | 掛上 pdf skill 後，模型放棄 JSON 契約改去輸出 Python 腳本 | 依 PROMPT 要求只輸出行程 JSON | 輸出 8,065 字的 `reportlab` 程式碼，並自行改成「三天兩夜」，且未呼叫搜尋工具。API 層以精簡版 skill 重測仍複現（模型自述「根據PDF技能指南，我需要使用reportlab」） | `LLMService.applyToolProviders` 會以 skill 指引**覆寫 systemMessageProvider**（追加 `activate_skill` 引導），其優先權高於 PROMPT 節點的使用者訊息。而平台的 SKILL 節點**不執行程式碼**，因此把腳本型 skill（anthropics pdf）掛上去，只會誘導模型產出永遠跑不了的程式碼 | 🟡 未修復（本次以「不掛 SKILL 的變體」完成 J10-06 重驗）<br>![ISSUE-5 模型改輸出 Python](e2e-shots/202607312223/ISSUE5-01-skill-hijacks-output.png) |
| ISSUE-3<br>（J10-04 首次嘗試） | 景點數提高到 6 個時 LLM 節點逾時 | Agent 模式多輪工具呼叫可完成 | node_execution 記 `Node Y4qyHzeg execution timed out after 120000 ms`，OUTPUT 落 SKIPPED、execution FAILED | `WorkflowEngine.DEFAULT_NODE_TIMEOUT_MS = 120_000`（`WorkflowEngine.kt:70`），而 `timeoutMs` **只有 `HttpRequestNodeConfig` / `CodeNodeConfig` 可覆寫**（`WorkflowEngine.kt:688-690`），`LLM_ASSISTANT` 無法調整。Agent 模式每次工具呼叫都是一輪 LLM 往返，景點一多必然撞牆 | 🟡 設計限制（本次以景點數降為 4 個規避，48 秒完成）<br>![ISSUE-3 執行中逾時](e2e-shots/202607312223/ISSUE3-01-node-timeout-running.png) |
| ISSUE-4<br>（觀察） | 瀏覽器關閉（SSE 中斷）後，執行紀錄長時間停在 RUNNING | 客戶端斷線不影響最終狀態落庫 | execution `5a97c455…` 於 22:46 起停留 RUNNING，直到 22:48:36 節點逾時才轉 FAILED | 引擎不因 SSE 客戶端斷線而中止，狀態要等節點逾時才收斂；期間 UI 無從得知 | 🟡 待評估（非本次阻斷） |

> ISSUE-1 的失敗證據：![ISSUE-1 tool setting not found](e2e-shots/202607312223/ISSUE1-01-tool-setting-not-found.png)

### 修復紀錄（2026-07-31 23:06，ISSUE-1 / ISSUE-2）

| 檔案 | 變更 |
|------|------|
| `repository/LLMToolUserSettingRepository.kt` | 新增 `findSettingByIdAndUserId(settingId, userId)`——依**設定 id ＋ 擁有者**查詢；原 `findSettingByUserIdAndToolId` 保留供依 toolId 查詢的路徑使用 |
| `service/ToolService.kt` | `buildToolWithSetting` 改用上述新方法，並以**指定的那一筆設定**建構（不再退回 `buildTool(toolId)`，避免同一工具有多筆設定時取錯）；抽出共用的 `buildToolFromSetting(tool, userSetting)`，並在實例化回 null 時**留下 error 日誌**（原本靜默丟棄，是本次誤判方向的主因）；移除只會拒絕有 config class 工具的 `buildToolWithoutSetting`（全 repo 僅一處呼叫） |
| `service/LLMService.kt` | `toolIds` 路徑由 `buildToolWithoutSetting` 改為 `buildTool`——後者會依 `(登入者, toolId)` 帶出使用者設定，工具本來就不需設定時退回無參數建構，行為相容 |

**驗證**（ArchUnit 通過、漂移掃描 exit 0）：

| 驗證 | 修復前 | 修復後 |
|------|--------|--------|
| 只掛 `toolIds`（Tavily），問模型可用工具 | `NO_TOOLS` | 列出 **`searchWeb`** |
| 只掛 `toolSettingIds`（settingId） | HTTP 400 `User tool setting not found` | 決定性搜尋題答對：**孫淑媚**（2026 台東最美星空音樂會 8/22 壓軸）並附兩個來源網址 |
| workflow UI 端到端（TOOL 節點填回 `toolSettingId`） | execution FAILED（62ms） | execution SUCCESS，`events` 帶回真實活動與來源 URL |

> ⚠️ 修復前後皆以 `docs/sql/bestpartner-init-data.sql` 未變更、無新增測試資料的方式驗證；
> 相關單元測試尚未補（`ToolService` 目前無對應測試），建議後續補一支涵蓋
> 「settingId 查得到／查不到（他人設定）／有 config class 的工具可建構」三案。

### 靜默失敗掃描與補強（2026-07-31 23:57）

ISSUE-1／ISSUE-2 之所以難查，根源是「建構失敗回 null → 呼叫端 `mapNotNull` 靜默丟棄 → 無任何日誌」。
據此對能力建構鏈（Tool／MCP／Skill／KnowledgeRAG）做同型掃描，**補上三處診斷日誌**：

| 檔案 | 情境 | 補強 |
|------|------|------|
| `service/McpServerService.kt` | `userSettingId` 查無資料或不屬於當前使用者 → MCP 靜靜地沒掛上 | 新增 warn（含 settingId／userId 與排查提示）；另補 `type` 缺失時的 warn |
| `service/SkillService.kt` | `skillId` 不存在 → skill 靜靜地沒掛上 | `mapNotNull` 內補 warn（含 skillId） |
| `service/LLMService.kt` | 知識庫不存在 → RAG 來源靜靜地沒掛上，模型照樣作答但無檢索脈絡 | `buildKnowledgeRetriever` 補 warn（含 knowledgeId） |

已具備日誌、不需處理者：skill resource 檔案不存在、`skill.md` 找不到、MCP client 建立拋例外、
`buildDefaultMcpClient` 找不到 server（拋 `MCP_SERVER_SETTING_NOT_FOUND`）。
另查 `McpServerService.createTransport` 雖宣告為可空，但 `when` 對 `McpType` 窮盡且兩分支必定 build，
**不可能回 null**，屬誤報，未改動。

> 皆為純日誌變更、無行為改動；`compileKotlin`、ArchUnit、漂移掃描（exit 0）皆通過，
> uber-jar 已同步重編（23:57），避免產物落後原始碼（§12 IS-1 教訓）。

#### 未處理：SKILL 掛載缺擁有權檢核（待決策）

`SkillService.buildSkills(skillIds)` 以 `skillRepository.findById(id)` 取 skill，**未比對 userId**，
而同檔的讀取路徑 `findReadableSkill(id, userId)` 會擋掉「非 GLOBAL 且非本人」——掛載比讀取寬鬆。
`skillIds` 直接來自請求 body（`POST /llm/customAssistantChat`）與 workflow 的 `SkillNodeConfig.skillId`，
皆為使用者可控，故已登入者若取得他人私有 skill 的 UUID，可令其 `skill.md` 與 resource 全文注入自己的對話。
（MCP 無此問題：`findByCondition(settingId, userId)` 有帶擁有者。）
**修法涉及行為變更（誤填他人 id 由「靜默略過」變「明確報錯」）且屬資安性質，保留待決策，本次未動。**

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

---

## webwright 執行產物

| 項目 | 路徑 |
|------|------|
| workspace | `%TEMP%\claude\D--projects-bestpartner\<session>\scratchpad\webwright-j10\` |
| plan.md（critical points） | `webwright-j10/plan.md`（CP1–CP12） |
| 最終腳本 | `webwright-j10/final_runs/run_7/final_script.mjs` |
| 操作記錄 | `webwright-j10/final_runs/run_7/final_script_log.txt`（28 個 step） |
| 原始截圖 | `webwright-j10/final_runs/run_7/screenshots/` |
| PDF 產生腳本 | `webwright-j10/render-pdf.mjs` |
| 報告證據截圖 | `docs/test-confirmations/e2e-shots/202607312223/`（15 張） |
| 交付產物 | `docs/test-confirmations/e2e-artifacts/202607312223/`（PDF + 預覽圖 + itinerary.json） |

### 契約偏離說明

| 項目 | webwright 預設 | 本次實際 | 原因 |
|------|---------------|---------|------|
| 執行語言 | Python `final_script.py` | Node `final_script.mjs` | 本機無可用 Python（`python` 為 Microsoft Store 佔位符、`.venv` base 直譯器失效）；沿用週期 202607302129 已登錄之偏離 |
| 瀏覽器 | firefox | chromium | 本機 ms-playwright 僅安裝 chromium；與本報告 metadata 的瀏覽器欄一致 |
| viewport | 1280×1800 | 1920×1400 | 7 個節點在 1280 寬時會被右側 Inspector 遮住、handle 取不到 boundingBox 而連線漏接（run_3 實測 edge 少 1）；改寬並先按「整理版面」+「zoom-fit」後穩定 |
| PDF 產製 | anthropics pdf skill 的 Python 腳本 | chromium `page.pdf()` | 該 skill 全篇為 pypdf / reportlab / pdfplumber，無 HTML→PDF 路徑，無 Python 即不可執行 |

### 執行歷程（失敗嘗試皆保留於各 run 資料夾）

| run | 結果 | 原因 |
|-----|------|------|
| run_1 / run_2 | 失敗 | `getByTestId` 預設找 `data-testid`，未設 `testIdAttribute:'data-test'` |
| run_3 | 失敗 | 路由為 `/editor/:id?`，新建時 URL 尚無 id；且 1280 寬導致 edge 漏接、TRIGGER 已改型別化表單（`trigger-type`）而非 raw 模式 |
| run_4 | 失敗 | ISSUE-1：TOOL 節點填 `toolSettingId` → `User tool setting not found` |
| run_5 | 成功 | 但輸出為散文、未照 JSON schema |
| run_6 | 失敗 | ISSUE-3：景點數 6 個 → LLM 節點 120 秒逾時 |
| **run_7** | **成功（主旅程證據）** | 景點數降為 4，48 秒完成，輸出符合 schema；`events: []`（Tavily 當時仍壞） |
| — | — | **↓ 以下為 ISSUE-1／ISSUE-2 修復後的重驗（後端 jar 重編於 23:06）** |
| run_8 | 失敗 | ISSUE-5 首次顯現：TOOL 節點填回 `toolSettingId` 後執行成功，但模型改去輸出 Python `reportlab` 腳本、自行改成三日、未搜尋 |
| run_9 | 失敗 | 提示明文禁止產生程式碼後，改為 ISSUE-3 逾時 |
| run_10 | 失敗 | 加上「工具呼叫上限 5 次」仍逾時 |
| run_11 | 失敗 | pdf skill 精簡為僅 `skill.md`（93KB → 8.5KB）仍逾時 |
| run_12 | 失敗 | 移除 SKILL 節點（6 節點變體）仍逾時——確認瓶頸是總工具輪數而非 skill 大小 |
| **run_13** | **成功（J10-06 首次重驗）** | 6 節點變體 ＋ 景點降為 2、工具呼叫 3 次；取得真實活動與來源 URL |
| **run_14** | **成功（完整配置證據）** | **7 節點、三種能力全掛 `in:tool`**，沿用 run_13 的最小工作量；30 秒 SUCCESS，同時取得真實景點與真實活動。J10-08 的 PDF 以此輪輸出重產 |

> 期間另以 API 層量測完整配置（4 景點＋searchWeb＋SKILL）耗時 **253 秒**，
> 是判定「ISSUE-3 才是 J10 完整配置真正瓶頸」的關鍵數據。
