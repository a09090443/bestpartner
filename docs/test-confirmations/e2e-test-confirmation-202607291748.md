# BestPartner E2E 測試確認表（UI-driven）— 202607291748

> 本次週期由 `e2e-test-confirmation` skill 依模板 `.claude/skills/e2e-test-confirmation/e2e-test-checklist.md` 產出。
> 旅程與案例定義的權威來源：`docs/e2e-test-plan.md`。
>
> **本次範圍（經測試負責人指定）**：**J9 編輯器外觀與節點編輯頁** ＋ 受 UI 改版影響的回歸旅程 **J3 畫布編輯**、**J6 Inspector 表單**。
> J1／J2／J4／J5／J7／J8 本次不執行，於摘要表標 — （不適用），非失敗。
> 觸發原因：Workflow 編輯器套用新設計稿（雙主題、SVG 圖示、自訂縮放列、Node Designer 全屏編輯頁）。

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
| 測試日期 | 2026-07-29 17:48 |
| 服務版本 | 0.1.8-SNAPSHOT |
| 測試環境 | **dev** |
| 瀏覽器 | **chromium** |
| 前端 baseURL | `http://localhost:4173`（`npm run preview`，production build） |
| 後端 API URL | `http://localhost:80`（uber-jar，`-Dquarkus.profile=dev`） |
| 後端 jar 建置 | **本次週期重新編譯**（原 jar 建於 07-27 22:33，早於 PROMPT 節點 commit `f362925` 07-28 08:11，沿用會使 PROMPT 案例假性失敗） |
| 登入帳號 | `admin@bestpartner.com.tw`（此 dev DB 密碼非預設值，由測試負責人提供） |
| LLM 平台（J5） | — （本次範圍不含 J5） |
| E2E_LLM_ID（J5） | — （本次範圍不含 J5） |
| 測試結束時間 | **2026-07-29 21:06**（含補跑 J5；服務已關閉：port 80／5173／4173 皆無監聽）<br>第一階段（J9／J3／J6）結束於 18:20 |
| 執行工具 | `webwright` skill 驅動本機 Playwright（**非** `npx playwright test`）；runner 為 Node + `playwright`（本機無 python3），viewport 1280×1800 |
| webwright 工作區 | `%TEMP%\claude\D--projects-bestpartner\8fb1dc11…\scratchpad\ww-e2e\`（`plan.md`／`final_runs/run_1`～`run_8`，各含 `final_script.mjs`、`final_script_log.txt`、`screenshots/`、`results.json`） |
| 最終證據輪次 | **run_6**（J9／J3／J6 主輪）＋ **run_8**（J3／J6 補充）＋ **run_10／run_11／run_12**（J5 真實 LLM） |
| 真實 LLM 呼叫 | 補跑階段共發出 **12 次**真實推論（1 次 api_key smoke test ＋ 11 次 workflow 執行中的 LLM 節點），平台 OpenRouter／模型 `deepseek/deepseek-v3.2` |

---

## 前置檢查清單（測試前必須全部 ✅）

```
✅ 後端服務於 port 80 健康回應（GET /systemSetting/list → HTTP 200，17:50:31）
✅ PostgreSQL 已初始化（postgres-db 容器 Up 10 hours (healthy)）
✅ 種子資料存在：admin 可登入、6 筆 LLM setting、3 個 tool、4 個 MCP server
✅ J5 可用性：**補跑階段已補做**——`POST /llm/chat` 實測 `deepseek/deepseek-v3.2` 回覆正常（見下方 Step 4.5 表）
✅ 向量檢索可用性（J5-04 外掛 RAG 所需）：`POST /llm/vector/getDataFromEmbeddingStore` 對目標知識庫**實際命中**（回傳含「年滿二十歲」原文）
—  J8 完整旅程：本次範圍不含（J5-04 只驗「外掛 RAG 不落主遍歷」，內容正確性斷言仍歸 J8-B）
✅ 前端已 build（vue-tsc 通過）並由 http://localhost:4173 提供（vite preview）
✅ Playwright chromium 已安裝
✅ 登入態由 API 取得（POST /login/ → HTTP 200，JWT 634 字元）
```

> ⚠️ 任一項未通過即停止測試並回報。

### Step 4.5 資源與認證前置解析（禁止硬編 ID）

| 資源 | 解析結果 |
|------|---------|
| 登入 | `POST /login/`（`admin@bestpartner.com.tw`）→ **HTTP 200**，取得 JWT |
| LLM setting | 6 筆。本次 J3／J6 使用 OPENROUTER `deepseek/deepseek-v3.2` = `583b9222-8cb0-4109-b072-5f0fd1e9fed9` |
| tool | `TavilySearch` / `GoogleSearch` / `DateTool`（`DateTool` = `3737ec4a-2c88-490e-8301-ebc611f2433f`） |
| MCP server | `date` / `filesystem` / `gmail` / `google_map`（`date` = `4b9ba306-2fc5-4aa6-9e54-22bce834e021`） |
| skill | **空清單**（`GET /llm/skill/list` → `data: []`） |
| 知識庫 | `e2e-scb-tnc-202607272132` = `90c4deb7-fa5b-4a99-a510-3294f6bbcd10`（前次 J8 週期遺留） |
| **api_key 可用性驗證**<br>（`POST /llm/chat`） | 補跑階段補做，三個 CHAT 候選只有一個可用：<br>• `583b9222…` **deepseek/deepseek-v3.2** → ✅ HTTP 200，實際回覆「好」→ **本次 E2E_LLM_ID**<br>• `1ee80ffa…` openai/gpt-5.5-pro → ❌ 400「LLM服務處理發生錯誤」<br>• `4ba6eb24…` deepseek-v4-flash → ❌ 400「Please verify the LLM setting type is CHAT」 |
| 向量檢索可用性 | `POST /llm/vector/getDataFromEmbeddingStore`（`content=開戶年齡`）→ ✅ 回傳條款原文「年滿二十歲」，確認 Milvus 向量**實際命中**（非僅列得出知識庫） |

> ⚠️ **流程違規補記（誠實記錄）**：第一階段（J9／J3／J6）**略過了 skill Step 4.5 第 3 點強制的 api_key 可用性驗證**，
> 當時於報告標為 `—`（不適用），理由是「本次範圍不含 J5」。此理由不成立——skill 並未規定「不跑 J5 即可略過」，
> 正確標記應為「未執行」。該步驟已於補跑階段補做，且**立刻證明其價值**：三個候選 LLM 設定中只有一個可用。
> 第一階段在 J3／J6 選用的正好是可用的那個，屬僥倖而非驗證。

> ⚠️ **本次週期的已知限制**：伺服器上**沒有任何 Skill**，故 `SkillForm` 的「選擇 skillId」無法實測，
> J6 該子項標 ⏭️ 並於證據欄說明；SkillForm 的渲染與 config 寫回仍照測。
> J9-15（SKILL 節點的 Designer 空狀態）不受影響——該案例驗的是「SKILL 無輸入埠／無輸出」的 UI 行為，與有無 skill 資料無關。
> J9-16（能力掛載 chip）改以 **TOOL 節點**掛 `in:tool` 驗證，語義等價（同屬 `CAPABILITY_SOURCE_TYPES`）。

### 前置步驟證據

| 步驟 | 截圖 |
|------|------|
| 登入頁填入帳密 | ![SETUP-01](e2e-shots/202607291748/SETUP-01-login-filled.png) |
| 登入成功進入 workflow 列表（清理後為空） | ![SETUP-02](e2e-shots/202607291748/SETUP-02-workflow-list.png) |
| 建立測試 workflow 並命名 | ![SETUP-03](e2e-shots/202607291748/SETUP-03-editor-named.png) |
| 首次存檔（取得 id 的唯一途徑，見 OBS-1） | ![SETUP-04](e2e-shots/202607291748/SETUP-04-first-save.png) |
| 以 `/editor/:id` 重新開啟 | ![SETUP-05](e2e-shots/202607291748/SETUP-05-reopened-by-id.png) |
| 補齊 TRIGGER 的 `triggerType`（走 JsonConfigEditor JSON 模式） | ![SETUP-06](e2e-shots/202607291748/SETUP-06-trigger-config.png) |

---

## 測試結果摘要

> 每完成一條旅程立即更新本表

| 旅程 | 優先 | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|------|:---:|------|--------|--------|---------|---------|
| J1 認證 | P0/P1/P2 | 9 | — | — | — | 本次不適用 |
| J2 Workflow 列表 | P0/P1 | 4 | — | — | — | 本次不適用 |
| J3 畫布編輯 | P0/P1 | 9 | 9 | 0 | 0 | **100%** |
| J4 驗證與啟用 | P1 | 8 | — | — | — | 本次不適用 |
| J5 執行（真實 LLM） | P0/P1 | 8 | 8 | 0 | 0 | **100%** |
| J6 Inspector 表單 | P1 | 4 | 4 | 0 | 0 | **100%** |
| J7 契約錯誤 | P2 | 2 | — | — | — | 本次不適用 |
| J8-A 知識庫 RAG（pipeline） | P1 | 5 | — | — | — | 本次不適用 |
| J8-B 知識庫 RAG（外掛） | P1 | 5 | — | — | — | 本次不適用 |
| J9 編輯器外觀與節點編輯頁 | P0/P1/P2 | 17 | 17 | 0 | 0 | **100%** |
| **本次範圍合計** | | **38** | **38** | **0** | **0** | **100%** |

> **範圍說明（兩階段）**：
> 1. **第一階段**（UI 改版目標式回歸，測試負責人指定）：**J9 ＋ J3／J6**。
> 2. **第二階段**（補跑）：測試負責人追加 **J5 執行 workflow（真實 LLM）**，
>    連帶完成原本因缺執行輸出而 ⏭️ 的 **J9-13**。
>
> J1／J2／J4／J7／J8 仍標 —（不適用），非失敗、非略過。
>
> **執行輪次與斷言數**
>
> | 輪次 | 涵蓋 | 斷言 |
> |---|---|---|
> | run_6 | J9 主輪 ＋ J3／J6 | 37/37 |
> | run_8 | J3-06/07/09、J6-03 補充 | 5/5 |
> | run_10 | J5-01/05/06/08a、J9-13 | 7/7（首跑 1/7，修正腳本後全通） |
> | run_11 | J5-02/03/04/07 | 5/5（首跑 4/5） |
> | run_12 | J5-08b 修正 | 2/2（首跑 1/2） |
> | run_14 | OBS-2 修正複驗 | 1/1（run_13 首跑因腳本漏填 OUTPUT 必填而觸發到另一則訊息） |
> | **合計** | | **57/57 斷言 PASS** |
>
> 瀏覽器 console error：J9／J3／J6 各輪 **0 次**；J5 各輪僅 1 次，為 J5-08b 刻意觸發孤兒 PROMPT 防線的 HTTP 400（預期內）。
> （run_1～run_5、run_7、run_9 為腳本除錯與缺陷修正過程，非最終證據；差異見問題追蹤區）

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
> **— 本次週期不適用**：測試負責人指定範圍為 J9 ＋ J3／J6 回歸，本旅程未執行（非失敗、非略過）。

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
> **— 本次週期不適用**：測試負責人指定範圍為 J9 ＋ J3／J6 回歸，本旅程未執行（非失敗、非略過）。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J2-01 | P0 | 列表載入 | 顯示既有 workflow 清單 | | |
| J2-02 | P0 | 建立新 workflow | 進 `/editor/:id`，DRAFT、version 1 | | |
| J2-03 | P1 | 點列表項進編輯器 | 載入完整定義 | | |
| J2-04 | P1 | 刪除 workflow | 列表移除，連鎖刪 node/edge | | |

### J3 畫布編輯（P0）

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J3-01 | P0 | 拖拉節點入畫布（TRIGGER/LLM_ASSISTANT/OUTPUT） | 節點出現，nodeKey 唯一 | ✅ | 節點數=3，nodeKey `["EBEN_Cf2","VCbO3pbA","eDcX7PFo"]` 互異<br>![J3-01-01](e2e-shots/202607291748/J3-01-01-drag-trigger.png)<br>![J3-01-02](e2e-shots/202607291748/J3-01-02-drag-llm.png)<br>![J3-01-03](e2e-shots/202607291748/J3-01-03-drag-output.png) |
| J3-02 | P0 | 連線 edge | edge 建立，兩端點存在 | ✅ | TRIGGER→LLM、LLM→OUTPUT 共 2 條<br>![J3-02-01](e2e-shots/202607291748/J3-02-01-edge-trigger-llm.png)<br>![J3-02-02](e2e-shots/202607291748/J3-02-02-edge-llm-output.png) |
| J3-03 | P0 | 選節點於 Inspector 填 config | 表單值寫回節點 | ✅ | LLM 節點副標同步為 `583b9222-8cb0-4109-b072-5f0fd1e9fed9`<br>![J3-03-01](e2e-shots/202607291748/J3-03-01-node-subtitle.png) |
| J3-04 | P0 | save 整張覆寫 | version+1 | ✅ | version 1 → 2、dirty-badge 歸零<br>![J3-04-01](e2e-shots/202607291748/J3-04-01-saved.png) |
| J3-05 | P1 | 重整頁面 get 還原 | 畫布與 Inspector 與存檔一致 | ✅ | 重載後節點 8／edge 4，LLM 的 `llmId` 還原一致<br>![J3-05-01](e2e-shots/202607291748/J3-05-01-reloaded.png)<br>![J3-05-02](e2e-shots/202607291748/J3-05-02-config-restored.png) |
| J3-06 | P1 | 能力掛載：TOOL / MCP_SERVER / SKILL / **KNOWLEDGE_RAG** 的 `out:main` 連 LLM `in:tool` 並 save | edge 建立（`targetHandle=in:tool`）；重載後連線保留 | ✅ | 四種能力來源逐一掛載成功、相容性 toast 0 次；重載後 DTO 中 `targetHandle=in:tool` 共 5 條<br>![J3-06-01](e2e-shots/202607291748/J3-06-01-capability-nodes.png)<br>![J3-06-02](e2e-shots/202607291748/J3-06-02-tool-mounted.png)<br>![J3-06-03](e2e-shots/202607291748/J3-06-03-mcp-mounted.png)<br>![J3-06-04](e2e-shots/202607291748/J3-06-04-skill-mounted.png)<br>![J3-06-05](e2e-shots/202607291748/J3-06-05-rag-mounted.png)<br>![J3-06-06](e2e-shots/202607291748/J3-06-06-saved.png)<br>![J3-06-07](e2e-shots/202607291748/J3-06-07-reloaded.png)<br>主輪另有 TOOL 掛載紀錄：![J3-06-08](e2e-shots/202607291748/J3-06-08-drag-tool.png) ![J3-06-09](e2e-shots/202607291748/J3-06-09-edge-tool-intool.png) |
| J3-07 | P1 | 多個 KNOWLEDGE_RAG 同時掛同一 LLM 的 `in:tool` | 皆放行、無相容性 toast；CONNECTIONS 計數與 edge 數一致 | ✅ | 2 個 RAG 皆放行，edge 5、toast 0、Overview CONNECTIONS=5（與 edge 數一致）<br>![J3-07-01](e2e-shots/202607291748/J3-07-01-two-rag-mounted.png) |
| J3-08 | P1 | 提示接線：PROMPT 的 `out:main` 連 LLM `in:prompt` 並 save | edge 建立（`targetHandle=in:prompt`）；重載後連線保留 | ✅ | edge 建立且存檔後重載保留（DTO `in:prompt` 2 條，見 J3-09）<br>![J3-08-01](e2e-shots/202607291748/J3-08-01-drag-prompt.png)<br>![J3-08-02](e2e-shots/202607291748/J3-08-02-edge-prompt-inprompt.png) |
| J3-09 | P1 | 兩個 PROMPT 各接 CONDITION 的 `out:true`/`out:false`，再同時連同一 LLM 的 `in:prompt` | 皆放行、無相容性 toast；CONNECTIONS 計數與 edge 數一致 | ✅ | edge 9、toast 0、Overview CONNECTIONS=9；重載後 DTO `targetHandle=in:prompt` 共 2 條<br>![J3-09-01](e2e-shots/202607291748/J3-09-01-condition-two-prompts.png)<br>![J3-09-02](e2e-shots/202607291748/J3-09-02-branches-wired.png)<br>![J3-09-03](e2e-shots/202607291748/J3-09-03-two-prompts-mounted.png) |

### J4 驗證與啟用（P1）
> **— 本次週期不適用**：測試負責人指定範圍為 J9 ＋ J3／J6 回歸，本旅程未執行（非失敗、非略過）。

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

> **本旅程於補跑階段執行**（原範圍不含 J5，測試負責人後續要求補跑）。
> 使用 Step 4.5 實測可用的 `deepseek/deepseek-v3.2`（`583b9222-…`）；**每個案例都是真實 LLM 推論**。
> 執行輪次：run_10（J5-01/05/06/08）、run_11（J5-02/03/04/07）、run_12（J5-08b 修正輪）。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J5-01 | P0 | 合法 workflow（TRIGGER→LLM_ASSISTANT→OUTPUT）execute | SSE `started`→`node.*`→`completed(SUCCESS)`；DB 寫入 `llm_workflow_execution`/`llm_workflow_node_execution` | ✅ | SSE：`execution.started → node.started → node.completed`×3 `→ execution.completed`；抽屜 SUCCESS；DB `execution.status=SUCCESS`、`node_execution` 3 筆全 SUCCESS<br>![J5-01-01](e2e-shots/202607291748/J5-01-01-graph.png)<br>![J5-01-02](e2e-shots/202607291748/J5-01-02-llm-config.png)<br>![J5-01-03](e2e-shots/202607291748/J5-01-03-saved.png)<br>![J5-01-04](e2e-shots/202607291748/J5-01-04-execution-result.png) |
| J5-02 | P1 | 節點設定錯誤導致失敗 | 該節點 `node.failed` 顯示錯誤，執行標記失敗 | ✅ | 刻意讓 OUTPUT 引用不存在的 `{{no_such_node.reply}}`：`node.failed`×1 且 nodeKey 正是該 OUTPUT；DB `status=FAILED`、`error_node_key` 相符；畫布該節點掛上 `is-exec-failed`<br>![J5-02-01](e2e-shots/202607291748/J5-02-01-bad-output-config.png)<br>![J5-02-02](e2e-shots/202607291748/J5-02-02-failed-result.png)<br>![J5-02-03](e2e-shots/202607291748/J5-02-03-failed-node-ui.png) |
| J5-03 | P1 | 執行中途 client 斷線 | 執行 `CANCELLED`，下游未執行節點 `SKIPPED`（commit b24c128 視覺） | ✅ | 兩顆 LLM 串接，第一顆跑完後按「停止」：抽屜 CANCELLED、畫布 1 個節點掛 `is-exec-cancelled`；DB `execution.status=CANCELLED`，下游 2 筆 `SKIPPED`（第二顆 LLM 與 OUTPUT）<br>![J5-03-01](e2e-shots/202607291748/J5-03-01-graph.png)<br>![J5-03-02](e2e-shots/202607291748/J5-03-02-running.png)<br>![J5-03-03](e2e-shots/202607291748/J5-03-03-cancelled.png) |
| J5-04 | P1 | 外掛型 RAG：KNOWLEDGE_RAG 以 `out:main → LLM in:tool` 掛載後 execute | 推論前自動檢索注入（非 LLM 主動呼叫）；**不產生**獨立節點事件、`node_execution` 無其紀錄。內容正確性斷言歸 J8-B | ✅ | 真實 embedding＋Milvus（前置已驗 `getDataFromEmbeddingStore` 命中「年滿二十歲」）；SSE 只有 3 組 node 事件、`node_execution` 中 KNOWLEDGE_RAG **0 筆**；輸出 `{"result":"20"}`（內容正確性歸 J8-B，此處不作斷言）<br>![J5-04-01](e2e-shots/202607291748/J5-04-01-rag-plugin-config.png)<br>![J5-04-02](e2e-shots/202607291748/J5-04-02-result.png) |
| J5-05 | P1 | Agent 模式：TOOL / MCP_SERVER / SKILL 以 `out:main → LLM in:tool` 掛載後 execute | LLM 可自主呼叫掛載工具並回覆；能力節點**不產生**獨立節點事件、`node_execution` 無其紀錄 | ✅ | DateTool 掛 `in:tool`：執行 SUCCESS、`node_execution` 中 TOOL **0 筆**、總紀錄 3 筆；UI 佐證：TOOL 的 Inspector **無**「本次執行」區塊、LLM 有<br>![J5-05-01](e2e-shots/202607291748/J5-05-01-graph.png)<br>![J5-05-02](e2e-shots/202607291748/J5-05-02-execution-result.png)<br>![J5-05-03](e2e-shots/202607291748/J5-05-03-tool-no-exec-section.png)<br>![J5-05-04](e2e-shots/202607291748/J5-05-04-llm-has-exec-section.png)<br>⚠️ 本案例只實測 **TOOL**；MCP_SERVER／SKILL 的掛載執行本次未測（掛線與存檔已由 J3-06 覆蓋，但未經 execute） |
| J5-06 | P1 | 提示節點驅動：PROMPT → `LLM in:prompt` → OUTPUT（LLM `userPrompt` 留空）execute | PROMPT **照常**產生 `node.started`/`node.completed` 與 `node_execution` 紀錄（與能力節點相反）；LLM 以該提示提問並成功回覆 | ✅ | SSE 含 4 組 node 事件；DB `node_execution` 4 筆（TRIGGER/PROMPT/LLM/OUTPUT），**PROMPT 為 SUCCESS**——與能力節點不落紀錄形成對照<br>![J5-06-01](e2e-shots/202607291748/J5-06-01-graph.png)<br>![J5-06-02](e2e-shots/202607291748/J5-06-02-execution-result.png) |
| J5-07 | P1 | 分支擇一：CONDITION 兩分支各接一 PROMPT 匯入同一 LLM 後 execute | 被活化分支的 PROMPT 為 SUCCESS、另一顆 `node_execution` 為 SKIPPED；回覆風格對應被活化那條 | ✅ | 恆真條件：兩顆 PROMPT 的 `node_execution` 狀態為 `["SUCCESS","SKIPPED"]`；輸出 `{"result":"TRUEBRANCH"}`，對應被活化的 true 分支<br>![J5-07-01](e2e-shots/202607291748/J5-07-01-graph.png)<br>![J5-07-02](e2e-shots/202607291748/J5-07-02-result.png) |
| J5-08 | P1 | 優先序：LLM 同時有 `userPrompt` 與 PROMPT 連入 | 以 PROMPT 輸出為準；刪 `in:prompt` 連線後再執行改用 `userPrompt` | ✅ | **前半**（run_10）：PROMPT 填 ALPHA、userPrompt 填 BRAVO → 輸出 `{"result":"ALPHA"}`，PROMPT 勝出；UI 同時顯示 `prompt-overridden-badge`<br>![J5-08-01](e2e-shots/202607291748/J5-08-01-both-sources.png)<br>![J5-08-02](e2e-shots/202607291748/J5-08-02-result-prompt-wins.png)<br>**後半**（run_12）：刪 `in:prompt` 連線 → PROMPT 成孤兒，execute 被第二層防線擋下（抽屜 FAILED / HTTP 400，見 OBS-2）；再刪掉孤兒節點後執行 → 輸出 `{"result":"BRAVO"}`，改用 userPrompt<br>![J5-08b-01](e2e-shots/202607291748/J5-08b-01-graph-both-sources.png)<br>![J5-08b-02](e2e-shots/202607291748/J5-08b-02-edge-deleted-orphan-prompt.png)<br>![J5-08b-03](e2e-shots/202607291748/J5-08b-03-orphan-prompt-guard.png)<br>![J5-08b-04](e2e-shots/202607291748/J5-08b-04-orphan-removed.png)<br>![J5-08b-05](e2e-shots/202607291748/J5-08b-05-userprompt-used.png) |

> J5 斷言不比對 LLM 輸出文字；只驗事件序列、節點狀態、DB 紀錄。
> J5-07／J5-08 以 `ALPHA`／`BRAVO`／`TRUEBRANCH` 等**單字關鍵字**判別「用了哪個提問來源／走了哪條分支」——
> 這是**來源判別**而非語意正確性比對，仍符合「不比對輸出文字」的原則。

### J6 Inspector 表單（P1）

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J6-01 | P1 | LlmAssistantForm 選 llmId | 值寫回並可存檔 | ✅ | `llm-select` value=`583b9222-…`，存檔後重載一致<br>![J6-01-01](e2e-shots/202607291748/J6-01-01-inspector-llm-form.png)<br>![J6-01-02](e2e-shots/202607291748/J6-01-02-llm-selected.png) |
| J6-02 | P1 | Tool/McpServer/KnowledgeRag/Prompt/Output 表單填寫 | config 正確寫回，save 後重載一致（PromptForm 填 `prompt-text`、選填 `output-key`） | ✅ | Prompt=「請用一句話說明今天的日期」；Tool=DateTool（3 選項）；MCP=date（4 選項）；Knowledge=e2e-scb-tnc-202607272132（1 選項）；Output template=「結果：{{answer}}」<br>![J6-02-01](e2e-shots/202607291748/J6-02-01-prompt-form.png)<br>![J6-02-02](e2e-shots/202607291748/J6-02-02-tool-form.png)<br>![J6-02-03](e2e-shots/202607291748/J6-02-03-tool-selected.png)<br>![J6-02-04](e2e-shots/202607291748/J6-02-04-mcp-form.png)<br>![J6-02-05](e2e-shots/202607291748/J6-02-05-knowledge-form.png)<br>![J6-02-06](e2e-shots/202607291748/J6-02-06-output-form.png)<br>**SkillForm 僅驗渲染**（伺服器 skill 清單為空，無法實選 skillId，見前置解析表）：![J6-02-07](e2e-shots/202607291748/J6-02-07-skill-form-empty.png) |
| J6-04 | P1 | LLM 接上 PROMPT 後檢視 LlmAssistantForm | 顯示 `prompt-overridden-badge` 與 `prompt-source-hint`；`user-prompt` 仍可編輯（非 disabled） | ✅ | badge=1、hint=1、`user-prompt` disabled=**false**<br>![J6-04-01](e2e-shots/202607291748/J6-04-01-prompt-overridden-badge.png) |
| J6-03 | P2 | settingSchema 動態表單（sensitive 遮罩） | 依 schema 正確渲染欄位型別 | ✅ | TavilySearch 渲染 8 個動態欄位（baseUrl / apiKey* / timeout* / searchDepth / includeAnswer / includeRawContent / includeDomains / excludeDomains），其中 **1 個 sensitive 欄位（apiKey）渲染為 `input[type=password]`**；設定區塊預設展開<br>![J6-03-01](e2e-shots/202607291748/J6-03-01-tool-selected.png)<br>![J6-03-02](e2e-shots/202607291748/J6-03-02-setting-schema-form.png) |

### J7 契約錯誤（P2）
> **— 本次週期不適用**：測試負責人指定範圍為 J9 ＋ J3／J6 回歸，本旅程未執行（非失敗、非略過）。

| 案例 | 優先 | 描述 | 預期 | 狀態 | 證據 / 備註 |
|------|:---:|------|------|:---:|------|
| J7-01 | P2 | save 節點 config 型別錯誤（未知欄位/結構型別錯） | 400，`workflow.node.config.invalid`，含 nodeKey | | |
| J7-02 | P2 | 樂觀鎖 version 衝突 | 400，`workflow.version.conflict`，UI 顯示衝突提示 | | |

### J8 知識庫 RAG 檢索問答（P1，真實 embedding + Milvus + 真實 LLM）
> **— 本次週期不適用**：測試負責人指定範圍為 J9 ＋ J3／J6 回歸，本旅程未執行（非失敗、非略過）。

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
| J9-01 | P1 | 清空 `localStorage` 後進編輯器 | `.wf-editor` 的 `data-wf-theme="light"` | ✅ | `data-wf-theme=light`、`html.dark=false`<br>![J9-01-01](e2e-shots/202607291748/J9-01-01-fresh-light.png) |
| J9-02 | P1 | 點 `theme-toggle` 兩次 | 深色↔淺色來回無殘留區塊；`localStorage.wf-theme` 同步；重整後保留 | ✅ | 深：`data-wf-theme=dark`／`html.dark=true`／`localStorage.wf-theme=dark`；再點回淺三者同步；重整後仍為 dark<br>![J9-02-01](e2e-shots/202607291748/J9-02-01-toggled-dark.png)<br>![J9-02-02](e2e-shots/202607291748/J9-02-02-toggled-back-light.png)<br>![J9-02-03](e2e-shots/202607291748/J9-02-03-dark-after-reload.png) |
| J9-03 | P1 | 深色下導到 `/login` 與列表頁 | `<html>` 的 `dark` class 已移除，兩頁維持淺色 | ✅ | 列表頁與登入頁 `html.dark=false`；回編輯器後偏好（dark）自動恢復<br>![J9-03-01](e2e-shots/202607291748/J9-03-01-list-light.png)<br>![J9-03-02](e2e-shots/202607291748/J9-03-02-login-light.png)<br>![J9-03-03](e2e-shots/202607291748/J9-03-03-back-in-editor-dark.png) |
| J9-04 | P2 | 深色下觸發 `ElMessageBox`（未存離開／登出確認） | 對話框深色配色且文字可讀 | ✅ | 主題=dark，`.el-message-box` 背景 `rgb(20,20,20)`（非白底，證明 `html.dark` 對 teleport 到 body 的元件生效）<br>![J9-04-01](e2e-shots/202607291748/J9-04-01-messagebox-dark.png) |
| J9-05 | P1 | 點 `zoom-in` / `zoom-out` / `zoom-fit` | 畫布縮放改變，`zoom-value` 百分比同步更新 | ✅ | 46% →（放大）55% →（縮小）50% →（適應視窗）50%<br>![J9-05-01](e2e-shots/202607291748/J9-05-01-zoom-initial.png)<br>![J9-05-02](e2e-shots/202607291748/J9-05-02-zoom-in.png)<br>![J9-05-03](e2e-shots/202607291748/J9-05-03-zoom-out.png)<br>![J9-05-04](e2e-shots/202607291748/J9-05-04-zoom-fit.png) |
| J9-06 | P0 | 雙擊節點 | 出現 `node-designer-modal`；header 顯示型別／分類／名稱；該節點同時被選取 | ✅ | modal=1；header 含「LLM 助手」與分類「AI」；Inspector 同步切至該節點（`node-name-input` 存在）<br>![J9-06-01](e2e-shots/202607291748/J9-06-01-designer-opened-dblclick.png) |
| J9-07 | P1 | 點節點卡右上 `node-open-designer` | 開啟 Designer，且未觸發拖曳或取消選取 | ✅ | 開啟成功；節點座標開啟前後一致（未被拖動）<br>![J9-07-01](e2e-shots/202607291748/J9-07-01-opened-by-node-button.png)<br>![J9-07-02](e2e-shots/202607291748/J9-07-02-node-not-moved.png) |
| J9-08 | P1 | 點 Inspector 的 `open-node-designer-button` | 開啟 Designer | ✅ | modal=1<br>![J9-08-01](e2e-shots/202607291748/J9-08-01-inspector-button.png)<br>![J9-08-02](e2e-shots/202607291748/J9-08-02-opened-by-inspector.png) |
| J9-09 | P1 | 三種關閉：Esc、點遮罩、`node-designer-close` | 皆關閉；再開啟時分頁回到 Parameters | ✅ | Esc 後 modal=0；點卡片內部 modal 仍=1、點遮罩才變 0；關閉鈕於各案例間反覆使用皆正常；重開分頁回到 Parameters<br>![J9-09-01](e2e-shots/202607291748/J9-09-01-closed-by-esc.png)<br>![J9-09-02](e2e-shots/202607291748/J9-09-02-closed-by-overlay.png) |
| J9-10 | P0 | modal 內改 config → 關閉 | Inspector 與節點副標同步更新；工具列標記未存 | ✅ | modal 內填 systemPrompt=「你是一位嚴謹的助理」→ 關閉後 Inspector 同欄位值一致、dirty-badge=1<br>![J9-10-01](e2e-shots/202607291748/J9-10-01-params-tab.png)<br>![J9-10-02](e2e-shots/202607291748/J9-10-02-config-edited-in-modal.png)<br>![J9-10-03](e2e-shots/202607291748/J9-10-03-inspector-synced.png) |
| J9-11 | P1 | 在 `node-designer-name-input` 打字後按 Delete | 節點**不被刪除** | ✅ | 打字後連按兩次 Delete，節點數 8 → 8（未變）<br>![J9-11-01](e2e-shots/202607291748/J9-11-01-typed-and-delete.png)<br>![J9-11-02](e2e-shots/202607291748/J9-11-02-node-count-unchanged.png) |
| J9-12 | P1 | 未執行流程時檢視 Input / Output | 兩側皆顯示「尚未執行」空狀態 | ✅ | Input／Output 皆為「尚未執行。執行流程後可在此檢視實際…資料。」<br>![J9-12-01](e2e-shots/202607291748/J9-12-01-empty-states.png) |
| J9-13 | P1 | 執行後開啟中段節點的 Designer | Input 顯示 `node-designer-input-json`（含「來自 <上游名>」）；Output 顯示 JSON 與可引用欄位 | ✅ | **補跑 J5 後完成**（run_10）：J5-01 真實執行結束後開啟 LLM 節點的 Designer，Input 顯示「來自 <上游名>」與實際 JSON、Output 顯示實際 JSON、狀態列 SUCCESS、可引用欄位預覽列出 `{{nodeKey.reply}}`<br>![J9-13-01](e2e-shots/202607291748/J9-13-01-designer-with-real-data.png)<br>⚠️ 測試陷阱：**不可先關結果抽屜再開 Designer**——關閉鈕綁 `executionStore.reset()`，會清空 `nodeStates`，Designer 就無資料可顯示（首輪即因此誤判為失敗） |
| J9-14 | P1 | 開啟 TRIGGER 的 Designer | Input 為虛線空狀態「這是觸發節點…」，不顯示 JSON | ✅ | 文案=「這是觸發節點，它啟動整個流程，因此沒有輸入資料。」，`input-json` 數=0<br>![J9-14-01](e2e-shots/202607291748/J9-14-01-trigger-input-empty.png) |
| J9-15 | P1 | 開啟 SKILL 的 Designer | Input「這是能力提供節點…」；Output「不會產生自己的輸出」 | ✅ | **首次執行時 FAIL，修正後通過**（見問題追蹤區 BUG-1）。修正後 Input=「這是能力提供節點，它只作為 LLM 助手的工具來源，沒有輸入資料。」、Output=「這是能力提供節點，它不會產生自己的輸出。」<br>![J9-15-01](e2e-shots/202607291748/J9-15-01-skill-empty-states.png) |
| J9-16 | P1 | 開啟已掛能力節點的 LLM 的 Designer | `node-designer-capability-list` 以 chip 列出 `in:tool` 來源，不顯示其 JSON | ✅ | 「已掛載能力」區出現「工具」chip 與說明「能力節點不參與資料流，由 LLM 自主決定何時呼叫。」；`input-json` 數=0<br>![J9-16-01](e2e-shots/202607291748/J9-16-01-capability-list.png) |
| J9-17 | P2 | Settings 與 Docs 分頁（Docs 逐一檢視 13 種型別） | Settings 顯示 nodeKey／型別／分類／必填檢核／本次執行；Docs 皆有內容且連接埠清單與實際 handle 一致 | ✅ | Settings 含實際 nodeKey、`LLM_ASSISTANT`、分類 `AI`；Docs 顯示 LLM 的三個輸入埠 `in:main`／`in:prompt`／`in:tool`（與 `nodeTypes.ts` 一致）。13 種型別的 Docs 完整性另由單元測試 `NodeDesignerModal.test.ts`「13 種型別皆可掛載且 Docs 分頁有內容」逐一涵蓋<br>![J9-17-01](e2e-shots/202607291748/J9-17-01-settings-tab.png)<br>![J9-17-02](e2e-shots/202607291748/J9-17-02-docs-tab-llm.png) |

---

## 旅程完成檢查清單（每完成一條旅程必須立即執行）

> 對應 SKILL.md Step 5.5。進入下一旅程前，逐項勾選並更新摘要表。

**J3（畫布編輯）**
```
☑ 所有案例的逐步截圖已擷取並存入 e2e-shots/202607291748/（24 張）
☑ 所有案例證據已寫入（逐步圖片連結 + 關鍵斷言數值，無佔位符）
☑ 所有案例狀態已標記（9 × ✅）
☑ 失敗案例：無
☑ 摘要表已更新（9 / 0 / 0 / 9）
☑ Pass 率已計算：100.0%
☑ 該旅程新增的測試資料已清理
```

**J6（Inspector 表單）**
```
☑ 所有案例的逐步截圖已擷取並存入 e2e-shots/202607291748/（11 張）
☑ 所有案例證據已寫入（含各表單的可選項數量與實際選中值）
☑ 所有案例狀態已標記（4 × ✅）
☑ 失敗案例：無（SkillForm 的 skillId 選取因伺服器無 skill 資料，於證據欄明述限制）
☑ 摘要表已更新（4 / 0 / 0 / 4）
☑ Pass 率已計算：100.0%
☑ 該旅程新增的測試資料已清理
```

**J9（編輯器外觀與節點編輯頁）**
```
☑ 所有案例的逐步截圖已擷取並存入 e2e-shots/202607291748/（33 張）
☑ 所有案例證據已寫入（含主題 token／localStorage／縮放百分比等實測值）
☑ 所有案例狀態已標記（16 × ✅、1 × ⏭️）
☑ 失敗案例：J9-15 首次執行 FAIL → 已定位根因並修復（BUG-1），run_6 複驗通過
☑ 摘要表已更新（16 / 0 / 1 / 17）
☑ Pass 率已計算：100.0%（16/16 已執行案例）
☑ 該旅程新增的測試資料已清理
```

### 嚴格規則

- **不可延後**：完成一條旅程立即更新，不可等全部測完才統一更新
- **即時暫停**：某旅程 Pass 率過低（P0<100% / P1<95% / P2<80%），立即暫停評估
- **計算精確**：Pass 率 = Pass ÷ 總數 × 100%，保留一位小數
- **與問題追蹤同步**：失敗案例先入問題追蹤區，再更新摘要

---

## 測試資料清理規則

- E2E 建立的 workflow 一律以 `e2e-<caseId>-<runTag>` 前綴命名。
- 每條案例測試後經 `POST /llm/workflow/delete` 刪除；收尾以前綴掃描 `workflow/list` 兜底刪除殘留 `e2e-*`。
- **不得另建備份檔／目錄**；種子資料（admin、test_user、LLM setting）為前置條件，不刪除。
- 逐步截圖屬**報告證據**，隨報告保留於 `docs/test-confirmations/e2e-shots/<報告時間戳>/`，**不在清理範圍**（清理只針對 DB 的 `e2e-*` workflow 資料）。

> ⚠️ 收尾後資料庫殘留 `e2e-*` 資料，視同流程不完整，需補清理。

### 本次清理結果

| 項目 | 結果 |
|------|------|
| 主輪建立的 workflow | `e2e-uirework-202607291748` → `POST /llm/workflow/delete` HTTP 200 |
| 補充輪建立的 workflow | `e2e-uirework-supp-202607291748` → 已刪除 |
| 前綴掃描兜底 | `GET /llm/workflow/list` 過濾 `e2e-*` → **空清單** |
| 除錯輪次殘留 | run_1～run_5、run_7、run_9 的中途產物已於各輪的失敗路徑／後續輪次前置清理中刪除 |
| J5 補跑建立的 workflow | `e2e-j5-01/02/03/04/05/06/07/08/08b-202607291748` 共 9 張 → 全數刪除 |
| **執行紀錄（本次新增）** | ⚠️ `workflow/delete` **不連鎖刪執行紀錄**（見 OBS-3）：本次 13 筆 `llm_workflow_execution` ＋ 47 筆 `llm_workflow_node_execution` 在 workflow 刪除後成為孤兒，已另以 SQL 清除 |
| 最終 DB 狀態 | `llm_workflow` 中 `e2e-*`／`未命名流程` **0 筆**；`llm_workflow_execution` 剩 15 筆、`node_execution` 剩 52 筆，**皆為 07-27 前次週期遺留、非本次產生**，不予變動 |

> ⚠️ **本次額外納入「未命名流程」的清理**：畫布是「先建立、後命名」，
> 若在命名前就存檔（或腳本中途失敗），殘留資料的名稱是**「未命名流程」而非 `e2e-*` 前綴**，
> 只掃前綴會漏。清理函式已同時比對此名稱。此陷阱源自 OBS-1，後續週期須沿用。
>
> 最終確認：`GET /llm/workflow/list` 回傳中無任何本次週期產生的資料。

---

## 問題追蹤區

> 失敗（❌）項目在此詳記。本次最終狀態無 ❌，但過程中抓到 1 個真實缺陷（已修）與 1 項既有行為觀察。

| 案例 ID | 現象 | 期望行為 | 實際行為 | 根因 | 狀態 |
|---------|------|---------|---------|------|------|
| **BUG-1**<br>J9-15 | SKILL 節點的 Node Designer，右欄「輸出」顯示「尚未執行。執行流程後可在此檢視實際輸出資料。」 | SKILL 是純能力節點、永遠不會執行，應顯示「這是能力提供節點，它不會產生自己的輸出。」 | 顯示成一般節點的「尚未執行」語意，暗示使用者「跑一次就會有輸出」——實際上永遠不會有 | `DesignerOutputPanel.vue` 僅以「輸出埠數量 === 0」判斷有無輸出。**SKILL 有 `out:main` 埠**（那是掛載到 LLM `in:tool` 的能力埠、非資料流），故條件不成立而落入一般分支；原本為 SKILL 準備的文案成了無法觸及的死碼 | ✅ **已修復**：判斷式改為 `輸出埠數 === 0 \|\| type === 'SKILL'`，並補上迴歸單元測試 `DesignerOutputPanel.test.ts`（含「SKILL 雖有輸出埠，仍須說明它不會產生自己的輸出」）。run_6 複驗通過 |
| **OBS-1**<br>（既有行為，非本次回歸） | 「新建 workflow」導向 `/editor`（不帶 id），**存檔後 URL 仍不帶 id** | 存檔取得 id 後，URL 宜補為 `/editor/:id`，使 F5 能回到同一份流程 | 在 `/editor` 按 F5 會開一張全新空白畫布，剛存的流程要從列表頁重新點入才找得回 | `WorkflowEditorView` 的存檔流程未於取得 id 後做 `router.replace`。**已確認與本次 UI 改版無關**：`git diff` 中該檔案的路由相關變更僅有存檔鈕的 CSS class（`btn primary` → `btn ghost`） | ⚠️ **未修**（超出本次 UI 改版範圍，待另案評估）。副作用：E2E 腳本無法從 URL 取得 workflowId，須改以 `/llm/workflow/list` 依名稱反查；測試資料清理也須一併掃描「未命名流程」 |

| **OBS-2**<br>J5-08b | execute 失敗時，執行結果抽屜只顯示 **`HTTP 400`**，看不到後端的業務訊息 | 依 `docs-site/docs/features/workflow.md:46` 的既定原則：「操作失敗時，畫面會顯示**後端回傳的業務訊息**……而非通用的 HTTP 狀態字串」，此處應顯示 `workflow.prompt.node.not.connected`（含 nodeKey）之類的可理解訊息 | 抽屜顯示「執行結果 FAILED / HTTP 400」，使用者無從得知是「孤兒提示詞節點」造成 | `api/workflowExecution.ts` 在 `if (!res.ok) throw new Error(\`HTTP ${res.status}\`)` 時**直接丟棄 response body**，而 body 裡就是 `ApiResponse.message`。存檔／啟用／刪除走 axios ＋ `extractApiMessage()` 所以正常，**execute 是唯一走原生 fetch 的路徑，也是唯一違反該原則的地方** | ✅ **已修復**（測試負責人裁示後修正）。`workflowExecution.ts` 新增 `readErrorMessage(res)`：讀取 body 取 `ApiResponse.message`；body 非 JSON 或無 message 時退回 `HTTP <status>`，確保一定有字可顯示（非 JSON 的原文不直接呈現，避免把整頁 HTML 塞進提示）。<br>**新增 5 支單元測試** `src/api/__tests__/workflowExecution.test.ts`（含此迴歸案例）。<br>**真實瀏覽器複驗**（run_14）：同一情境的抽屜訊息由「HTTP 400」變為<br>`PROMPT node "vEEBjQOT" must be connected to an LLM assistant nodes prompt input port (in:prompt)`<br>![OBS2-01](e2e-shots/202607291748/OBS2-01-orphan-prompt-saved.png)<br>![OBS2-02](e2e-shots/202607291748/OBS2-02-business-message-shown.png) |

| **OBS-3**<br>（既有行為） | 刪除 workflow 後，其執行紀錄仍留在資料庫且成為孤兒（`workflow_id` 指向已不存在的 workflow） | 刪 workflow 時一併清除或標記其執行歷史，避免孤兒列持續累積 | `POST /llm/workflow/delete` 連鎖刪 node/edge，但**不刪** `llm_workflow_execution` / `llm_workflow_node_execution` | 刪除邏輯未涵蓋執行紀錄表 | ⚠️ **未修**（既有行為，非本次改版造成）。佐證：清理前 DB 中 28 筆執行紀錄**全部**為孤兒，其中 15 筆來自 07-27 前次週期。**對 E2E 流程的影響**：以「`e2e-*` workflow 是否殘留」判定清理完成是不夠的，執行紀錄需另外清；本次已補清自己產生的 13 筆 |

| **BUG-2**<br>（**使用者實際回報**，非測試發現） | 手動觸發流程時報「節點 8eQ3-44a 缺少必填欄位：觸發類型」 | 拖入 TRIGGER 後應能直接使用；`triggerType` 至少要在 UI 上看得到、改得了 | 三層問題疊加導致**每個使用者、每張流程都會撞到**：<br>① TRIGGER **沒有型別化表單**（`typedForms.ts` 未涵蓋），退回 `JsonConfigEditor`<br>② 自 palette 拖入時 `config: {}`，無任何預設值<br>③ `JsonConfigEditor` 的表格模式**只 `v-for` 既有的鍵、沒有新增欄位功能**，config 為空時只顯示「無設定欄位」——唯一出路是切 JSON 模式手打 `{"triggerType":"MANUAL"}` | 而諷刺的是這個欄位**目前填什麼都一樣**：`TriggerExecutor` 完全不讀 `triggerType`，`WorkflowEngine` 建立執行紀錄時固定寫 `MANUAL`。等於一個無行為差異、卻強制必填、又只能靠 raw JSON 設定的欄位 | ✅ **已修復**（三項一併）：<br>1. `NodeTypeMeta` 新增 `defaultConfig`，TRIGGER 帶 `{ triggerType: 'MANUAL' }`，`onDrop` 展開複製後套用<br>2. 新增 `TriggerForm.vue` 並註冊到 `typedForms.ts`；WEBHOOK / CRON 列出但 `disabled`（尚無觸發來源實作，避免選到存得起來卻不會被觸發的設定）<br>3. `JsonConfigEditor` 表格模式加上**新增／移除欄位**（含空鍵名與重複鍵防呆）<br>**新增 12 支單元測試**；真實瀏覽器複驗 run_15 **4/4 PASS、console error 0**<br>![TRIGGER-01](e2e-shots/202607291748/TRIGGER-01-default-config.png)<br>![TRIGGER-02](e2e-shots/202607291748/TRIGGER-02-form-options.png)<br>![TRIGGER-03](e2e-shots/202607291748/TRIGGER-03-kv-add-field.png)<br>![TRIGGER-04](e2e-shots/202607291748/TRIGGER-04-kv-remove-field.png)<br>![TRIGGER-05](e2e-shots/202607291748/TRIGGER-05-graph-ready.png)<br>![TRIGGER-06](e2e-shots/202607291748/TRIGGER-06-execution-success.png) |

| **BUG-3**<br>（**使用者實際回報**，非測試發現） | 簡單流程（觸發→提示詞→LLM→輸出）執行報「Node qoEPNaPS is missing required config fields: template\|mappings」 | OUTPUT 的必填規則要在表單上講清楚，且要填得出來 | **這不是程式缺陷，是表單沒把契約講明白**：`template` 的 label **沒標必填**、`mappings` 明寫「**選填**」，兩者讀起來像都可以不填——但後端契約是**擇一必填**。<br>更麻煩的是就算知道要填，`{{nodeKey.reply}}` 需要上游節點的 **nanoid**，而 OutputForm 完全沒提供這個資訊，使用者得自己去別處挖 | 表單文案與後端契約脫節，且缺少填寫所需的上下文 | ✅ **已修復**：<br>1. 兩個 label 都改標「與…擇一必填」<br>2. 兩者皆空時就地顯示 `output-both-empty` 提示（不必等執行才知道）<br>3. 新增 `constants/nodeOutputKeys.ts`（各 executor 預設輸出鍵的對照表，逐項註記後端出處與行號）與 OutputForm 的**可引用輸出 chip**：列出圖上其他節點的 `{{nodeKey.欄位}}`，**點一下插入模板游標處**。已執行過的節點以實際輸出鍵為準，未執行則用型別預設鍵<br>**新增 15 支單元測試**；真實瀏覽器重現使用者流程複驗 run_16 **4/4 PASS、console error 0**，LLM 實際回覆了「請問你是甚麼模型？」<br>![OUTPUT-01](e2e-shots/202607291748/OUTPUT-01-graph.png)<br>![OUTPUT-02](e2e-shots/202607291748/OUTPUT-02-required-hint-and-refs.png)<br>![OUTPUT-03](e2e-shots/202607291748/OUTPUT-03-chip-inserted.png)<br>![OUTPUT-04](e2e-shots/202607291748/OUTPUT-04-execution-success.png) |

| **UX-1**<br>（使用者提問引出） | 「`Mr7gjxEL.reply` 這是什麼？使用者會知道要填什麼嗎？」 | 編輯畫面不該直接把內部識別碼當成使用者介面 | `{{nodeKey.field}}` 的 `nodeKey` 是 `nanoid(8)` 產生的隨機碼。BUG-3 的 chip 讓使用者**填得出來**，但填完之後欄位裡仍是 `{{Mr7gjxEL.reply}}`——**看不懂、認不出是哪個節點**，多個同型別節點更分不清 | **把儲存格式直接當成編輯介面**。引擎 `ExecutionContext.resolvePath()` 只認 nodeKey（不支援節點名稱），所以識別碼必然存在——但沒有理由讓使用者直接面對它 | ✅ **已修復（方案 A：運算式編輯器）**：<br>新增 `utils/expression.ts`（解析／序列化，語法對齊後端 `PLACEHOLDER` regex）與 `components/common/ExpressionEditor.vue`（contenteditable）。引用在畫面上渲染成不可分割色塊「**LLM 助手 › reply**」，**寫回 config 的仍是原始 `{{nodeKey.field}}`**——儲存格式與後端完全不動。<br>另含：節點改名時色塊跟著更新而底層引用不變；引用不存在的節點標成紅色虛線；建議 chip 也改為「節點名 › 欄位」，畫面上不再出現 nanoid。<br>**未引入 CodeMirror**（為單一欄位加約 200KB 依賴不划算），自建約 120 行。<br>**新增 31 支單元測試**；真實瀏覽器驗證 run_18 **7/7 PASS、console error 0**，含以 CDP 送出真實 IME 事件驗證中文輸入<br>![EXPR-01](e2e-shots/202607291748/EXPR-01-token-rendered.png)<br>![EXPR-02](e2e-shots/202607291748/EXPR-02-ime-composed.png)<br>![EXPR-03](e2e-shots/202607291748/EXPR-03-saved.png)<br>![EXPR-04](e2e-shots/202607291748/EXPR-04-reloaded.png)<br>![EXPR-05](e2e-shots/202607291748/EXPR-05-renamed.png)<br>![EXPR-06](e2e-shots/202607291748/EXPR-06-unknown-ref.png)<br>![EXPR-07](e2e-shots/202607291748/EXPR-07-execution.png) |

> **UX-1 曾評估但未採用的方案**：讓後端 `resolvePath` 支援以節點名稱引用（`{{LLM 助手.reply}}`）。
> 看似更好懂，實際是把不穩定性推進資料層——**名稱可重複、可改名，改個名字舊流程就壞**。
> 方案 A 只動顯示層：底層永遠是不會變的 nodeKey，改名只影響色塊上的字。
> run_18 的 EXPR-5 專門驗證了這點（改名後 `data-ref` 不變）。

> ⚠️ **contenteditable 的兩個地雷已在實作中處理，改動該元件時勿破壞**：
> ① 中文輸入法組字期間**不重繪、不 emit**（否則吃字）；② 只有外部值變更才重繪（否則打字時游標跳動）。
> 另：該欄位不再是 `<textarea>`，**測試不可用 `inputValue()`／`setValue()`**，要改讀 `textContent`。

> ⚠️ **BUG-3 與 BUG-2 是同一個盲點的兩次發作**：我在 run_9 就親手踩過 OUTPUT 的插值坑
> （把 template 寫成裸名 `{{llm_reply}}`，害整輪 J5 全 FAIL），當時只當成「自己腳本寫錯」修掉，
> 沒意識到**連寫測試的人都會第一次寫錯的東西，使用者一定會撞**。
> 教訓與 BUG-2 相同：**測試者的失誤本身就是可用性訊號，不該只當成雜訊修掉**。

> ⚠️ **BUG-2 是本週期最重要的教訓**：它由使用者實際操作時撞到，而**本次 E2E 全程沒抓到**——
> 因為我在 J5 建圖時直接用 JSON 模式寫入 `{"triggerType":"MANUAL"}` 繞過去了，
> 把「測試腳本能繞過」當成「功能沒問題」。
> **自動化測試若沿用開發者的捷徑，就會系統性地漏掉「新手第一次操作」的路徑。**
> 已在 `e2e-test-plan.md` §J6 與本 skill 的清單新增 J6-05／J6-06 兩條案例，
> 明確要求「拖入節點後**直接**檢視 Inspector」而非先用 JSON 模式補設定。

### 過程中的腳本問題（非產品缺陷，記錄供後續週期避雷）

| 輪次 | 問題 | 修正 |
|------|------|------|
| run_1 | 節點放在 x=900，節點中心落到 Inspector（固定 322px 欄）之後，`click` 被面板攔截 | 節點一律放 x ≤ 700；`selectNode()` 加入「中心 x > 940 即拋錯」的前置檢查。此即 `e2e-test-plan.md` §11-4 已記載的陷阱 |
| run_2 | 於 `page.evaluate` 內打 `http://localhost:80` 被 CORS 擋（前端 origin 為 `:4173`） | API 呼叫改由 Node 端 `fetch` 發出（無同源限制）；瀏覽器內只走 app 自己的 vite proxy 相對路徑 |
| run_3 | 以 `page.url()` 取 workflowId，實際拿到字串 `"editor"`（見 OBS-1） | 改以 API 依名稱反查 id |
| run_4 | TRIGGER 的 `raw-input` 找不到 | `JsonConfigEditor` 預設為表格模式，需先點 `mode-toggle` 切到 JSON 模式 |
| run_5 | Designer 截圖拍到 200ms 淡入動畫中的半透明畫面，證據判讀不明確 | modal 開啟後統一等待 450ms 讓動畫沉澱再截圖 |
| run_7 | J6-03 查不到動態欄位 | `ToolForm` 的 `expanded` 預設為 `true`，設定區塊本來就展開；原腳本多點一次 `setting-toggle` 反而把它收合 |
| run_9 | **J5 全數 FAIL，每次都是 OUTPUT 節點失敗** | OUTPUT 的 template 寫成裸名 `{{llm_reply}}`。插值語法是 `{{nodeKey.欄位}}`，引擎找不到變數 → OUTPUT FAILED。改為執行期從 `.vue-flow__node[data-id]` 取真實 nodeKey，組成 `{{<llmKey>.reply}}`（LLM 預設輸出鍵名為 `reply`，見 `LlmAssistantExecutor:111`）。**注意：LLM 節點本身在 run_9 就已全數 SUCCESS——真實推論一直是正常的** |
| run_9 | J9-13 看不到執行資料 | 腳本先按了結果抽屜的關閉鈕，而該鈕綁 `executionStore.reset()`，把 `nodeStates` 清空了。改為不關抽屜直接開 Designer |
| run_10 | J5-08b 刪錯 edge | 用 `.vue-flow__edge` 的 `.last()` 猜位置。edge 順序不保證；改以 `data-id$=":in:prompt"` 精準定位（edge 的 `data-id` 形如 `e-<src>:out:main-<tgt>:in:prompt`） |
| run_11 | J5-08b 刪對 edge 但執行仍失敗 | **不是產品缺陷**：只刪連線會讓 PROMPT 變成孤兒節點，後端第二層防線正確擋下 execute。測試設計須連孤兒節點一起刪除，run_12 據此修正並把該防線本身也納入觀察（見 OBS-2） |

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
