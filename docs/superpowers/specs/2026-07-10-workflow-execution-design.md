# Workflow 執行引擎與畫布執行體驗設計

- 日期：2026-07-10
- 狀態：待審
- 範圍：bestpartner-service（執行引擎 + API）、bestpartner-ui（執行按鈕、即時狀態、結果呈現、Executions 頁）

## 1. 背景與目標

畫布目前僅支援「建立 → 設定 → 連線 → 存檔 → 啟用」，Executions 頁籤為停用佔位，後端沒有任何執行引擎（`ExecutionStatus` / `NodeExecutionStatus` enum 與 `llm_workflow_execution` / `llm_workflow_node_execution` DDL 已預留但無程式引用）。

目標：提供 n8n 式的執行體驗——

1. 畫布工具列有「▶ 執行」按鈕，手動觸發整條 workflow
2. 執行中節點逐一亮起（即時狀態），完成後可逐節點檢視 input/output
3. 新增 OUTPUT 節點型別作為流程最終結果的明確出口
4. 支援全部節點型別（含 CONDITION / LOOP / CODE / DATA_TRANSFORM）
5. Executions 頁籤啟用：歷史清單與回放

已確認的三項決策：

- CODE 節點引入 GraalJS sandbox（同意）
- 執行不要求 ACTIVE，已存檔即可測試執行（同意）
- 分三期交付（同意）

## 2. 後端設計

### 2.1 新增 OUTPUT 節點型別

- `NodeType.OUTPUT`，分類 `Action`；輸入埠 1、**無輸出埠**；畫布上允許多個上游匯入
- `OutputNodeConfig`（加入 `NodeConfigRegistry`）：
  - `template: String?` — 文字模板，支援 `{{nodeKey.outputKey}}` 插值
  - `mappings: Map<String, String>?` — 鍵值對映，值支援插值
  - 必填契約：`template|mappings` 擇一（沿用 DATA_TRANSFORM 的複合字樣機制）
- workflow 可有 0..N 個 OUTPUT 節點；執行結果 `output_result` 為各 OUTPUT 節點輸出的合併物件（key = nodeKey，僅一個時直接取其值）；無 OUTPUT 節點時取拓撲序最後一個節點的輸出（相容既有流程）

### 2.2 執行引擎（`service/workflow/`）

```
WorkflowEngine                 # 編排：載圖、驗證、拓撲排序、逐節點執行、事件發送、持久化
├── ExecutionContext           # 變數上下文：nodeKey → output map；提供 {{nodeKey.path}} 插值解析
├── ExecutionEventSink         # 事件介面（SSE emitter 實作；未來 CRON/WEBHOOK 可另接）
└── NodeExecutor（介面）        # execute(node, context): NodeOutput，每 NodeType 一實作
    ├── TriggerExecutor        # MANUAL：把 input payload 放入 context（{{trigger.*}}）
    ├── LlmAssistantExecutor   # 重用 LLMService.buildAIService + MCP/Tool/Skill 掛載
    ├── ToolExecutor           # 重用 ToolService 實例化 @Tool 並呼叫
    ├── McpServerExecutor      # 重用 McpServerService，呼叫指定 toolName
    ├── KnowledgeRagExecutor   # 重用 EmbeddingService 相似度搜尋
    ├── HttpRequestExecutor    # 重用 OkHttpUtil（method/url/headers/body 插值）
    ├── ConditionExecutor      # 條件求值 → 走 out:true / out:false 分支
    ├── LoopExecutor           # 對 inputArrayPath 陣列迭代執行迴圈子圖（loop_index 落紀錄）
    ├── CodeExecutor           # GraalJS sandbox（見 2.5）
    ├── DataTransformExecutor  # mappings / template 轉換
    └── OutputExecutor         # 組裝最終輸出
```

編排規則：

- 拓撲排序與環檢查沿用存檔驗證同義邏輯（後端已有）；執行前先跑「啟用同級」的必填欄位驗證，驗不過回 400 並指出 nodeKey 與缺漏欄位
- CONDITION：依既有 `ConditionNodeConfig` 契約求值——`conditions`（欄位/運算子/比較值，左右值皆支援插值）以 `logic`（and/or，預設 and）聚合，true 走 `out:true`、false 走 `out:false`；未走分支的下游整段標 `SKIPPED`
- LOOP：依既有 `LoopNodeConfig` 契約——`inputArrayPath`（插值取得陣列）逐項迭代，當前項以 `{{<itemAlias>}}`（預設 `item`）注入 context；迴圈子圖 = 自 `loopBodyEntryNodeKey` 起沿 edge 可達、不越過 LOOP 節點本身的區域，每迭代執行一次（節點紀錄帶 `loop_index`）；各迭代輸出彙集於 `collectOutputKey`（預設 `items`），迭代完走 `out:done`；`maxIterations` 預設 100
- 單節點失敗 → 整體 `FAILED`，未執行下游標 `SKIPPED`；每節點逾時（config `timeoutMs`，預設 120s）計為該節點失敗
- MCP client 生命週期比照本次修復後的 streaming 模式：執行終止（完成/失敗/取消）才關閉

### 2.3 變數插值（全引擎統一）

- 語法 `{{nodeKey.path}}`，path 支援巢狀（`{{nodeA.reply}}`、`{{trigger.input.name}}`）
- 每節點輸出以 `outputKey`（預設依節點型別，如 LLM 為 `reply`）組成物件存入 context
- 解析失敗（引用不存在）→ 該節點失敗並回報缺漏的變數名，不靜默代入空字串

### 2.4 API 契約（前綴 `/llm/workflow`，類別層級 `@Authenticated`）

| 端點 | 說明 |
|------|------|
| `POST /execute` | body `{id, input?}`；回 SSE 串流（`@RestStreamElementType`），每事件一個 JSON |
| `POST /execution/list` | body `{workflowId, page?, size?}`；回執行摘要清單（狀態/起訖/耗時/觸發者） |
| `POST /execution/get` | body `{executionId}`；回單次執行完整紀錄（含全部節點 input/output） |

SSE 事件格式（單行 JSON）：

```json
{"event":"execution.started","executionId":"...","workflowId":"...","ts":"..."}
{"event":"node.started","executionId":"...","nodeKey":"...","seqNo":1,"ts":"..."}
{"event":"node.completed","executionId":"...","nodeKey":"...","status":"SUCCESS","output":{...},"durationMs":123,"ts":"..."}
{"event":"node.failed","executionId":"...","nodeKey":"...","error":"...","ts":"..."}
{"event":"execution.completed","executionId":"...","status":"SUCCESS|FAILED","output":{...},"durationMs":456,"ts":"..."}
```

- HTTP 連線中斷（前端按「停止」）→ 引擎取消剩餘節點，整體標 `CANCELLED`
- 擁有權檢核比照既有 WorkflowService（owner 或 admin）

### 2.5 CODE 節點 sandbox

- 依賴：`org.graalvm.polyglot:js`（版本進 `gradle.properties`，禁止硬編碼）
- 限制：`allowAllAccess(false)`、無 host class/IO 存取、單次執行逾時強制中斷（預設 10s）、輸出大小上限（預設 256KB）
- 介面：腳本以 `input`（上游 context 已插值資料）為輸入，`return` 值為節點輸出
- config 契約維持 `language`（僅接受 `"js"`）+ `source` 必填

### 2.6 持久化（entity 對齊既有 DDL）

- `WorkflowExecutionEntity` ↔ `llm_workflow_execution`（含 `workflow_version`、`trigger_type=MANUAL`、`triggered_by`、`input_payload`、`output_result`、`error_node_key` 等）
- `WorkflowNodeExecutionEntity` ↔ `llm_workflow_node_execution`（含 `seq_no`、`loop_index`、input/output json）
- 事件發送與寫庫同步進行；寫庫失敗不中斷執行（記 error log），確保體驗優先

## 3. 前端設計（bestpartner-ui）

### 3.1 執行按鈕與狀態機

- 工具列新增「▶ 執行」（`data-test="run-button"`）：
  - `dirty` 時提示「先存檔再執行」（或一鍵存檔後執行）
  - 執行中變「■ 停止」，點擊即中斷 SSE（後端標 CANCELLED）
- 新增 `stores/execution.ts`：`status`、`nodeStates: Map<nodeKey, {status, output, error, durationMs}>`、`finalOutput`、`events[]`；SSE 以 `fetch` + ReadableStream 消費（帶 Authorization header，EventSource 不支援自訂 header）

### 3.2 畫布即時狀態（n8n 風格）

- `WorkflowNode.vue` 依 execution store 上色：RUNNING 脈動邊框、SUCCESS 綠勾徽章、FAILED 紅叉、SKIPPED 半透明；執行路徑上的 edge 加流動動畫
- 新一次執行開始時清除前次狀態

### 3.3 逐節點資料檢視與結果面板

- Inspector 新增「本次執行」區塊（選中節點且該節點有執行紀錄時顯示）：input / output JSON 樹（可折疊）、耗時、錯誤訊息
- 底部結果抽屜（執行結束自動展開）：最終輸出（OUTPUT 節點結果）+ 節點時間軸（各節點狀態與耗時）；失敗時高亮錯誤節點並可點擊跳轉

### 3.4 OUTPUT 節點與 Inspector 表單

- `nodeTypes.ts` 新增 OUTPUT（icon 📤、色 `#52c41a` 系、無輸出埠）
- `OutputForm.vue`：template 文字域（支援插值提示）與 mappings 鍵值編輯，二擇一

### 3.5 Executions 頁籤

- 啟用工具列 Executions 頁籤：清單（狀態、開始時間、耗時、觸發者，分頁）
- 點入回放：以該次紀錄為畫布上色 + 逐節點資料檢視（唯讀，不連 SSE）

## 4. 交付分期

| Phase | 內容 | 驗收 |
|-------|------|------|
| 1 | 引擎骨架 + ExecutionContext/插值 + 線性節點（TRIGGER/LLM/MCP/TOOL/HTTP/RAG/OUTPUT）+ `POST /execute` SSE + 持久化 + 前端執行按鈕/畫布上色/結果面板/OUTPUT 表單 | UI 執行「觸發→LLM(OpenRouter+date MCP)→OUTPUT」看到即時上色與最終結果；紀錄落庫 |
| 2 | CONDITION / LOOP / CODE（GraalJS）/ DATA_TRANSFORM | 分支/迴圈/程式碼節點案例執行正確，SKIPPED 與 loop_index 落紀錄 |
| 3 | `execution/list`、`execution/get` + Executions 頁籤與回放 | 歷史清單可查、可回放上色 |

每 Phase 完成：走 `test-confirmation` 測試週期 + `documentation-sync`（api-endpoints.md、docs-site、postman collection、DDL 說明等）。

## 5. 錯誤處理摘要

| 情境 | 行為 |
|------|------|
| 執行前驗證失敗（必填缺漏/有環/無 Trigger） | 400，指出 nodeKey 與原因，不建立執行紀錄 |
| 節點執行失敗/逾時 | 該節點 FAILED，下游 SKIPPED，整體 FAILED，SSE 發 node.failed + execution.completed |
| 前端中斷（停止鍵/斷線） | 引擎取消，整體 CANCELLED |
| 插值引用不存在 | 該節點 FAILED，錯誤訊息含變數名 |
| 寫庫失敗 | 不中斷執行，error log 記錄 |

## 6. 測試策略

- 後端：`WorkflowEngine` 編排單元測試（線性/分支/迴圈/失敗中止/取消）、各 Executor 單元測試（外部依賴 mock）、插值解析測試、CODE sandbox 限制測試（逾時/禁 IO）
- 前端：execution store 測試（SSE 事件 → 狀態機）、WorkflowNode 上色快照、OutputForm 表單測試
- E2E：Playwright（webwright）走「建立 → 執行 → 看結果」全流程，比照本次 UI 測試模式錄製畫面

## 7. 非目標（本設計不含）

- CRON / WEBHOOK 觸發的排程器與對外 webhook 端點（DDL 的 `llm_workflow_trigger` 表保留給後續）
- 執行的斷線重連 / 背景執行（方案 B 演進項）
- 多人協作與執行並發控制（單 workflow 同時多次執行允許，各自獨立紀錄）

## Phase 1 驗收後追記：Phase 2 待辦

Phase 1 最終審查（task-14）裁決 TOOL 動態呼叫與節點逾時延後，以下項目列入 Phase 2 範圍：

- **TOOL 動態呼叫**：`ToolNodeExecutor` 目前為佔位實作，執行時一律拋「尚未支援執行」；Phase 2 需依 `config.arguments` 插值後實際呼叫工具並回傳結構化結果。
- **每節點逾時**：`timeoutMs`（預設 120s）尚未生效，Phase 2 需在 executor 執行外層加上逾時控制，逾時視同節點失敗。
- **RAG 檢索參數生效**：`KNOWLEDGE_RAG` 的 `topK` / `minScore` / `embeddingModelId` 於 Phase 1 執行時未生效，僅供存檔與畫布驗證使用。
- **HTTP 節點進階欄位生效**：`HTTP_REQUEST` 的 `headers` / `secretHeaders` / `timeoutMs` 於 Phase 1 執行時未生效。
- **SKIPPED 於分支圖的可達性判斷**：目前失敗/取消後「未執行節點」一律標 `SKIPPED`；分支（CONDITION/LOOP）落地後需改為僅標記真正「本應執行但未執行」的可達節點。
- **LLM memory 以 executionId 累積的清理策略**：目前 Memory 未清理，需規劃保留期限或執行結束後的清除時機。
- **前端停止後 RUNNING 殘留視覺**：client 中途斷線（`CANCELLED`）時，畫布上尚在 `RUNNING` 的節點視覺未回收，需補上終止態樣式處理。
- **`__input__` 包裝層級檢討**：`TriggerExecutor.INPUT_KEY` 目前以 `{"input": {...}}` 包一層輸出，插值路徑需多一節 `trigger.input.xxx`，Phase 2 檢討是否扁平化。
