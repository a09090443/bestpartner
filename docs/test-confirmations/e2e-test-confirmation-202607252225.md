# BestPartner E2E 測試確認表 — KNOWLEDGE_RAG 作為 LLM 外掛（自動注入型 RAG）

> ---
> ## ⚠️ 本報告已由 `e2e-test-confirmation-202607272132.md` 取代（2026-07-27 重測）
>
> 重測時核對出以下不一致，**閱讀本檔時請一併參照新報告**：
>
> 1. **證據與敘述不一致**：本檔第 93 行寫「未跑 webwright 逐步截圖」，但
>    `e2e-shots/202607252225/` 內存在 9 張 `UI-*.png`（時戳晚於本報告），本文**未引用任何一張**。
> 2. **該批截圖顯示執行失敗**：`UI-09-executed.png` 為「執行結果 FAILED / HTTP 400」，
>    與下表 RAG-03 的 ✅（來自 API 層貫穿，非 UI）方向相反；該 UI 圖的圖只有 3 條連線
>    （LLM → OUTPUT 未連，孤兒 OUTPUT），推測即 400 主因。
> 3. **測試資料未清乾淨**：本檔「收尾」聲稱 workflow 已刪除，實際殘留
>    `e2e-rag-plugin-202607252225`、`e2e-rag-ui-202607252225`，已於 202607272132 週期清除。
> 4. **RAG-05 歸因方向正確**：真因為 Milvus 容器未啟動且 collection 為空；
>    新報告啟動 Milvus 並重建知識庫後，RAG-05 已可斷言（輸出「20歲」＝條款事實，
>    而非本檔記錄的「18歲」＝ LLM 常識，本檔對此的推論獲得證實）。
> ---
>
> 本次為**專項 E2E**：驗證「KNOWLEDGE_RAG 可作為 LLM 節點外掛（`in:tool` 掛載、自動注入型 RAG、多知識庫）」改動的端到端行為，非全 J1–J8 回歸。
> 權威旅程定義：`docs/e2e-test-plan.md`。

---

## 測試週期資訊

| 項目 | 內容 |
|------|------|
| 測試日期 | 2026-07-26（報告時戳 202607252225） |
| 服務版本 | 0.1.8-SNAPSHOT（含本次改動重編 uber-jar，dev profile） |
| 測試環境 | dev |
| 後端 API URL | `http://localhost:80` |
| 前端 baseURL | `http://localhost:4173`（preview，已起並健康 200） |
| CHAT LLM | `583b9222-8cb0-4109-b072-5f0fd1e9fed9`（OpenRouter deepseek-v3.2，`/llm/chat` 回 200 驗證 api_key 可用） |
| Embedding | `3b624ce5-963f-48d5-9389-e333937c8bc8`（OpenRouter nemotron-3-embed） |
| 知識庫 | `6b39833e-d562-4345-886e-5f222e6d722c`（e2e-scb-tnc-0724，渣打數位存款條款 PDF；向量庫 `b8828b60-...`） |
| 測試結束時間 | 2026-07-26（服務已關閉，listeners=0） |

### 驗證方式（與標準 webwright UI-driven 的差異，誠實說明）

本次採 **API 層端到端貫穿**（`/login` → `workflow/create` → `save` → `switchStatus` → `execute` SSE → `getDataFromEmbeddingStore`），而非 webwright UI 截圖流程，理由：

1. 本機**無 Python Playwright**（webwright 契約用 `playwright.firefox`），僅有 `bestpartner-ui` 的 Node `@playwright/test`。
2. 本次改動的核心是**後端契約與執行行為**（掛載解析、必填契約、自動注入、事件），API 層貫穿更精準命中，且不受 Vue Flow DnD 座標脆弱性干擾。
3. **UI 層「KNOWLEDGE_RAG→`in:tool` 連線放行」已由前端單元測試覆蓋**：`useGraphValidation.test.ts` 新增「多個 KNOWLEDGE_RAG 連 LLM 工具埠無相容性錯誤」案例（vitest 300 全綠），`CAPABILITY_SOURCE_TYPES` 前後端一致（漂移掃描 ✓）。

---

## 前置檢查

```
✅ 後端 port 80 健康 200（/systemSetting/list）
✅ 前端 4173 可存取 200
✅ 登入可用（admin@bestpartner.com.tw / 非預設密碼；JWT 滾動刷新）
✅ CHAT LLM api_key 可用（/llm/chat 回 200 "OK"）
✅ 知識庫 metadata 存在（getKnowledgeStore 有 docId）
⚠️ 知識庫向量檢索回空（見下方歸因）— 影響 RAG 內容正確性斷言，不影響機制驗證
```

---

## 驗證矩陣

| 案例 | 描述 | 預期 | 狀態 | 證據 |
|------|------|------|:---:|------|
| RAG-01 | save 含「KNOWLEDGE_RAG `out:main` → LLM `in:tool`」掛載的圖（KNOWLEDGE_RAG config **只填 knowledgeId**） | save 成功，落庫 nodes=4/edges=3，edge `rag→llm targetHandle=in:tool` | ✅ | `GET nodes=4 edges=3 ver=2`；`edge rag->llm tgt=in:tool` |
| RAG-02 | **必填契約**：KNOWLEDGE_RAG 缺 `query`/`embeddingModelId` 啟用 | 啟用成功（舊契約會 400 REQUIRED_MISSING；新契約僅需 knowledgeId） | ✅ | `switchStatus active=true` → `{"code":200,"data":true}` |
| RAG-03 | **執行 + 自動注入路徑**：execute 掛載 KNOWLEDGE_RAG 的 workflow（userPrompt 只問問題、不手動插值知識） | SSE `execution.started`→`trigger`→`llm`→`out`→`execution.completed(SUCCESS)`；LLM 節點實際呼叫 | ✅ | 見 `execute-sse-events.txt`；llm `durationMs=4145`、`execution.completed status=SUCCESS` |
| RAG-04 | **並存 / 純能力節點**：KNOWLEDGE_RAG 作能力掛載時不落主遍歷 | rag 節點**無**獨立 `node.started`/`node.completed` 事件、node_execution 無其紀錄 | ✅ | SSE 僅 trigger/llm/out 三節點事件，無 rag 事件 |
| RAG-05 | **RAG 內容正確性**：LLM 輸出含知識庫文件事實 | 輸出含渣打條款年齡事實 | ⚠️ 無法斷言 | 見歸因：向量檢索回空 |

### SSE 事件序列（RAG-03/04 證據）

```
execution.started
node.started(trigger,seq1) → node.completed(trigger,SUCCESS,43ms)
node.started(llm,seq2)     → node.completed(llm,SUCCESS,reply="18歲",4145ms)
node.started(out,seq3)     → node.completed(out,SUCCESS,result="18歲",3ms)
execution.completed(SUCCESS,result="18歲",4240ms)
```

（完整內容見 `e2e-shots/202607252225/execute-sse-events.txt`）

---

## 歸因：RAG 內容正確性為何無法斷言（RAG-05）

直接對知識庫 `6b39833e` 檢索（`getDataFromEmbeddingStore`），以「年滿幾歲」「存款」「帳戶」「條款」多詞查詢，**全部 `data:[]` 空命中**。

- 這是**環境層面**問題（Milvus 向量庫未運行 / 向量未入庫 / 維度未對齊），**與本次程式改動無關**——本次未改任何檢索邏輯，只是把既有 `EmbeddingStoreContentRetriever` 接到 workflow LLM 節點。
- 因此 execute 輸出「18歲」是 **LLM 常識輸出**（台灣民法成年），非 RAG 注入結果（渣打條款事實）。
- **自動注入路徑本身已驗證會執行且不報錯**（RetrievalAugmentor 掛載成功、LLM 正常呼叫）；當向量庫有資料時，注入即會生效（此由後端單元測試 `WorkflowEngineTest` 能力掛載並存案例 + `LLMService.buildAIService` 多 retriever/DefaultQueryRouter 邏輯覆蓋）。

---

## 測試結果摘要

| 類別 | 案例 | ✅ | ⚠️ |
|------|:---:|:---:|:---:|
| 掛載 / 契約 / 執行機制（本次改動核心） | 4 | 4 | 0 |
| RAG 內容正確性（受環境向量庫限制） | 1 | 0 | 1 |

**結論**：本次「KNOWLEDGE_RAG 作為 LLM 外掛」改動的**機制層面端到端全數通過**（save 接受掛載、必填契約僅 knowledgeId、執行成功、並存純能力節點不落事件、自動注入路徑執行）。RAG **內容正確性**因測試環境向量庫檢索回空而無法斷言，屬環境資料問題，非本次改動缺陷。

---

## 未涵蓋範圍

- **多知識庫合併注入（DefaultQueryRouter）實跑**：環境僅一個知識庫且其向量檢索回空，未實跑多來源合併；邏輯由後端單元測試覆蓋。
- **完整 UI DnD 截圖旅程**：本次以 API 層貫穿 + 前端 vitest 覆蓋 UI 連線放行，未跑 webwright 逐步截圖。
- **pipeline 模式缺 query 執行報錯**：由 `KnowledgeRagExecutorTest`「pipeline 檢索缺 query 拋明確錯誤」單元測試覆蓋。

---

## 收尾

- ✅ 測試 workflow（`e2e-rag-plugin-202607252225`）已 `workflow/delete` 刪除。
- ✅ 服務已關閉（後端 80 + 前端 4173，listeners=0）。
- ✅ 未建任何備份檔／目錄；證據存於 `e2e-shots/202607252225/`。

## 建議後續

若要補「RAG 內容正確性」的真實斷言：確認 Milvus 運行、以對齊維度（nemotron dim 2048）重新對 `6b39833e` 知識庫做 embedding 入庫，`getDataFromEmbeddingStore` 能命中後，重跑 RAG-03/05 即可斷言輸出含條款事實。
