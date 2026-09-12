# Workflow 執行引擎 Phase 2 實作計畫

> **For agentic workers:** 本計畫由多代理編排逐任務執行（實作 → 審查 → 修復）。每個任務的實作者只需閱讀「Global Constraints」與自己的「### Task N」章節。

**Goal:** 補齊執行引擎的邏輯節點（CONDITION / LOOP / CODE / DATA_TRANSFORM）與 Phase 1 延後項（TOOL 動態呼叫、節點逾時、HTTP/RAG 參數生效、trigger 扁平化、memory 清理、前端取消視覺）。

**Architecture:** 沿用 Phase 1 的 `WorkflowEngine` + `NodeExecutor` 架構；Task 1 將平鋪拓撲迴圈改為「邊活化（activation）」遍歷以支援分支；Task 2 讓 LOOP 以子圖迭代執行；其餘任務為個別 executor 強化。

**Tech Stack:** Kotlin 2.1 / Quarkus 3.21 / kotlinx.serialization / GraalJS（Task 3 新增）/ Vue 3 + Pinia + vitest（Task 9）。

**規格依據:** `docs/superpowers/specs/2026-07-10-workflow-execution-design.md`（§2.2 編排規則、§2.5 CODE sandbox、§5 錯誤處理、文末「Phase 1 驗收後追記」）。

## Global Constraints

- 套件根為 `tw.zipe.bastpartner`（**bastpartner**，不是 basepartner）。
- 所有新業務訊息走 i18n：`messages_en_US.properties` + `messages_zh_TW.properties` + `AppMessage` enum，程式碼用 `ServiceException(AppMessage.XXX, ...)`；logger 訊息不納入 i18n。
- 依賴版本一律進 `bestpartner-service/gradle.properties`，`build.gradle.kts` 以 `val xxx: String by project` 讀取，禁止硬編碼版本。
- TDD：先寫失敗測試再實作；引擎測試沿用 `WorkflowEngineTest` 的手寫 fake repo + `FakeNodeExecutorInstance`（Java）模式，executor 測試為純單元測試（外部依賴 mock 或不觸網）。
- 測試指令（於 `bestpartner-service`）：`./gradlew test --tests "tw.zipe.bastpartner.<TestClass>" -Dorg.gradle.daemon=false`。只跑與本任務相關的測試類 + `ArchitectureTest`；全量建置留給最終驗證任務。
- Commit 訊息格式：`<類型>(<範圍>): <繁體中文主旨>`（範圍 `bestpartner-service` 或 `bestpartner-ui`），結尾加 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`。每任務至少一個 commit。
- 不建立 git worktree、不切分支（直接在 branch `0.1.8` 主目錄工作）。
- `NodeExecutor` 介面签名不可變：`fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?>`。
- 插值一律走 `ExecutionContext.resolvePath` / `resolveTemplate`；解析失敗拋 `VariableNotFoundException`，不得靜默代入空字串。

---

### Task 1: 條件分支端到端（引擎活化遍歷 + ConditionExecutor + SKIPPED 可達性）

**Files:**
- Modify: `service/workflow/WorkflowEngine.kt`
- Create: `service/workflow/executor/ConditionExecutor.kt`
- Modify: `enumerate/AppMessage.kt` + 兩個 messages properties（新增條件運算子不支援等訊息）
- Test: `src/test/kotlin/.../service/workflow/WorkflowEngineTest.kt`（擴充）、`ConditionExecutorTest.kt`（新建）

**引擎遍歷改造（活化模型）：**
- 仍以既有 Kahn 拓撲序為處理順序，但每個節點處理前判斷是否「active」：
  - indegree 0 的起點恆 active。
  - 其餘節點：至少一條入邊被「活化」才 active。邊被活化 = 來源節點執行成功，且（來源非 CONDITION，或 `edge.sourceHandle` 等於 CONDITION 判定的分支 handle）。
- 非 active 的節點：落一筆 `SKIPPED` 節點紀錄（含 seqNo），不執行、不活化其出邊，SSE 不需發事件（維持 Phase 1 行為：SKIPPED 只落庫）。
- 失敗 / 取消後：其餘未處理節點一律落 `SKIPPED` 紀錄（與 Phase 1 相同的收尾迴圈可沿用，但需排除已落過 SKIPPED 紀錄的節點，避免重複記錄）。
- `collectFinalOutput` 的「零 OUTPUT 節點取最後輸出」需改為取「實際執行成功的最後一個節點」。

**ConditionExecutor 契約：**
- `type = NodeType.CONDITION`，config 為既有 `ConditionNodeConfig`。
- 每條 `ConditionExpressionConfig`：`left` / `right` 先經 `context.resolveTemplate` 插值（純 `{{path}}` 時以 `resolvePath` 取原生型別），再依 `operator` 求值。
- 支援運算子（字串，忽略大小寫）：`eq`、`ne`、`gt`、`gte`、`lt`、`lte`、`contains`、`notContains`、`isEmpty`、`isNotEmpty`。數值比較：兩側可轉 Double 時以數值比較，否則字串比較；`isEmpty`/`isNotEmpty` 忽略 `right`。不支援的運算子拋 ServiceException（新 AppMessage）。
- `logic`：`and`（預設）/ `or` 聚合。
- 輸出：`mapOf("result" to Boolean, "branch" to "out:true"|"out:false")`；`trueHandle`/`falseHandle` config 有值時覆蓋預設 handle 字串。引擎讀取輸出中的 `branch` 決定活化哪些出邊。

**Tests（至少）：** true 分支走向與 false 側 SKIPPED 落紀錄、false 分支反向、or 邏輯、數值 gt、插值 left、不支援運算子失敗、分支下游失敗時僅其餘節點補 SKIPPED 不重複記錄。

---

### Task 2: LOOP 端到端（LoopExecutor + 引擎子圖迭代）

**Files:**
- Modify: `service/workflow/WorkflowEngine.kt`、`service/workflow/ExecutionContext.kt`
- Create: `service/workflow/executor/LoopExecutor.kt`（可為薄殼；迭代編排邏輯放引擎，因需要執行其他節點）
- Modify: `AppMessage.kt` + messages（迴圈輸入非陣列、巢狀 LOOP 不支援等）
- Test: `WorkflowEngineTest.kt` 擴充（迴圈案例）

**語意（spec §2.2）：**
- `inputArrayPath` 經 `resolvePath` 取得 List（非 List → 節點 FAILED，訊息含 path）。
- 迴圈子圖 = 自 `loopBodyEntryNodeKey` 起沿出邊可達、不含 LOOP 節點本身的節點集合；子圖節點不在主遍歷執行（主遍歷跳過它們，由 LOOP 迭代驅動）。子圖內含另一 LOOP → 於 `validateForExecution` 即報錯（巢狀不支援，新 AppMessage）。
- 每迭代：以 `itemAlias`（預設 `item`）將當前項注入 context（`ExecutionContext` 需支援 `putValue(key, Any?)` 存原生值，`{{item}}` 單段路徑可解析出該值；`{{item.name}}` 巢狀亦可），依子圖拓撲序執行各節點；節點紀錄帶 `loopIndex`（entity 既有欄位 `loop_index`）；SSE 事件照發（node.started/completed 每迭代）。
- 迭代上限 `maxIterations` 預設 100，超出以上限截斷（log warn）。
- 各迭代取「子圖拓撲序最後節點」的輸出，彙集為 List，LOOP 節點輸出 `mapOf(collectOutputKey(預設 "items") to list)`；完成後僅活化 `sourceHandle == "out:done"` 的出邊（`out:loop` 邊只用於界定子圖入口）。
- 迭代中任一節點失敗 → LOOP 節點 FAILED，整體 FAILED（沿用單節點失敗語意）。

**Tests（至少）：** 3 項陣列迭代（驗 loop_index 三筆×子圖節點數、collectOutputKey 彙集、out:done 下游拿到彙集值）、非陣列失敗、maxIterations 截斷、迭代中失敗中止、`{{item.xxx}}` 巢狀插值。

---

### Task 3: CodeExecutor（GraalJS sandbox）

**Files:**
- Modify: `bestpartner-service/gradle.properties`（`graalJsVersion`，採 GraalJS 24.x community artifacts）、`build.gradle.kts`（`org.graalvm.polyglot:polyglot` + `org.graalvm.polyglot:js-community`，型別 pom 者用 `implementation(...)` 對應寫法）
- Create: `service/workflow/executor/CodeExecutor.kt`
- Modify: `AppMessage.kt` + messages（不支援語言、腳本逾時、輸出過大、腳本錯誤）
- Test: `CodeExecutorTest.kt`

**契約（spec §2.5）：**
- config `CodeNodeConfig`：`language` 僅接受 `"js"`（其他值 → ServiceException）；`source` 必填；`timeoutMs` 預設 10_000；輸出上限 256KB（序列化後字串長度）。
- Sandbox：`Context.newBuilder("js").allowAllAccess(false)`，不開 host access / IO / 檔案 / 執行緒；以獨立執行緒跑 `eval` + `future.get(timeout)`，逾時呼叫 `context.close(true)` 強制中斷並判節點失敗。
- 輸入介面：`input` 全域變數 = `context.allOutputs()` 序列化 JSON 後在 JS 內 `JSON.parse` 還原（避免 host object 穿透）；腳本本體包成 `(function(){ ... })()` 或以最後表達式為回傳值——實作採：`const input = JSON.parse('<json>'); <source>`，取 eval 結果值。
- 輸出：eval 結果經 JS `JSON.stringify` 轉字串、Kotlin 端 Jackson 反序列化為 Map/List/scalar；節點輸出 `mapOf(outputKey(預設 "result") to value)`。
- 超出輸出上限或腳本拋錯 → 節點 FAILED，訊息含 JS 錯誤摘要。

**Tests（至少）：** 回傳物件、回傳 scalar、讀取 input 上游值、語法錯誤失敗、`while(true)` 逾時（timeoutMs 給 500ms）、輸出超限失敗、非 js 語言拒絕。

---

### Task 4: DataTransformExecutor

**Files:**
- Create: `service/workflow/executor/DataTransformExecutor.kt`
- Test: `DataTransformExecutorTest.kt`

**契約：** config `DataTransformNodeConfig`。`template` 有值 → 輸出 `mapOf(outputKey(預設 "result") to resolveTemplate(template))`。`mappings` 有值 → 對每筆 `DataTransformMappingConfig` 求值：`expression` 為純 `{{path}}` 時以 `resolvePath` 保留原生型別，否則 `resolveTemplate` 成字串；輸出 map（key=targetKey）。兩者皆有時 mappings 優先、template 併入 `outputKey` 鍵。targetKey/expression 為 null 的項目跳過。

**Tests：** template 插值、mappings 原生型別保留、混合、引用不存在變數失敗。

---

### Task 5: 節點逾時通用機制

**Files:**
- Modify: `service/workflow/WorkflowEngine.kt`、`AppMessage.kt` + messages（節點逾時）
- Test: `WorkflowEngineTest.kt` 擴充

**契約（spec §2.2 + 追記）：** 引擎在呼叫 `executor.execute(...)` 外層加逾時：config 有 `timeoutMs`（HTTP/CODE 型別）用之，否則預設 120_000ms。**注意 CDI/security context**：executor 內部依賴 request context 與登入身分，須以 Quarkus `ManagedExecutor`（`org.eclipse.microprofile.context.ManagedExecutor`，自動傳播 CDI + security context）submit，`future.get(timeout, TimeUnit.MILLISECONDS)`；`TimeoutException` → `future.cancel(true)` + 節點 FAILED（訊息含逾時毫秒數）。CODE 節點自身已有內層逾時，引擎外層仍套用（取 config timeoutMs）。預設值 120_000 以常數定義於引擎。

**Tests：** fake executor sleep 超過小逾時（測試 workflow 節點 config 帶 timeoutMs 300）→ 節點 FAILED 且訊息含逾時；正常節點不受影響。驗證逾時後執行整體 FAILED、下游 SKIPPED。

---

### Task 6: TOOL 動態呼叫

**Files:**
- Modify: `service/workflow/executor/ToolNodeExecutor.kt`（移除佔位拋錯）、必要時 `service/ToolService.kt`（若需抽公用實例化方法）
- Test: `ToolNodeExecutorTest.kt`

**契約（追記第 1 項）：** 依 `ToolNodeConfig.toolId`（+ 可選 `toolSettingId`）取得工具定義並實例化（參考 `LLMService.buildAIService` / ToolService 於 customAssistantChat 掛載工具的既有路徑——先讀懂該程式再重用，不可另造平行機制）。`arguments`（JsonObject）每個值先經 `resolveTemplate` 插值，再以反射呼叫該工具的 `@Tool` 方法（CUSTOMIZE 型別為其 functionName 對應方法；內建工具取其唯一 @Tool 方法；多個 @Tool 方法且 arguments 未指名 → 取第一個並 log warn）。參數依方法簽名做基本型別轉換（String/Int/Long/Double/Boolean）。輸出 `mapOf(outputKey(預設 "result") to 回傳值)`（非 scalar 以 Jackson 轉 Map）。工具不存在 → 既有 `TOOL_NOT_FOUND`。
- 若研究後發現既有 ToolService 無法在不大改下重用，允許在 executor 內以 `Class.forName(classPath)` + CDI lookup 實例化（與 ToolService 相同機制），但須在報告中說明取捨。

**Tests：** 以內建 DateTool（無需外部金鑰的工具；若 Date 工具需參數則 mock 一個測試用 @Tool 類）驗證呼叫與輸出、arguments 插值、工具不存在失敗。

---

### Task 7: HTTP 進階欄位 + RAG 檢索參數生效

**Files:**
- Modify: `service/workflow/executor/HttpRequestExecutor.kt`、`service/workflow/executor/KnowledgeRagExecutor.kt`
- Test: 兩個 executor 的既有/新建測試

**HTTP（追記第 4 項）：** `headers` 與 `secretHeaders` 逐值 `resolveTemplate` 後全部套用到請求（secretHeaders 僅差在「節點紀錄的 input 快照與錯誤訊息中不得輸出其值」——引擎 input 快照存 config 原文，config 內即為密文欄位名，故 executor 只需確保 log 不印值）；`timeoutMs` 用於 OkHttp client 的 call timeout（每次執行依 config 建 client 或用既有 OkHttpUtil 若已支援 per-call timeout——先讀 OkHttpUtil 再決定）。
**RAG（追記第 3 項）：** `topK`（預設 5）、`minScore`（預設 0.0）、`embeddingModelId` 實際傳入 EmbeddingService 檢索呼叫（先讀 `EmbeddingService` 既有簽名，若搜尋 API 已收這些參數只是 executor 未傳，補上即可）。

**Tests：** HTTP headers 插值後出現在請求（以本機 mock server 或攔截層驗證，參考既有 HttpRequestExecutorTest 的做法）、timeoutMs 生效（mock server 延遲）；RAG 參數傳遞（mock EmbeddingService 驗參數）。

---

### Task 8: trigger 輸出扁平化 + LLM memory 清理

**Files:**
- Modify: `service/workflow/executor/TriggerExecutor.kt`、`WorkflowEngine.kt`、`service/workflow/executor/LlmAssistantExecutor.kt`
- Test: 相關測試調整 + 新案例

**trigger 扁平化（追記第 8 項，決策：扁平化）：** TriggerExecutor 輸出與引擎預置的 `__input__` 皆改為 input map 本身（`{{<triggerNodeKey>.name}}`、不再需要 `.input.` 一層）。input 為 null 時輸出空 map。同步修正受影響測試與（若有引用範例的）docs-site workflow 文件段落。
**memory 清理（追記第 6 項）：** 執行結束（成功/失敗/取消皆然，引擎 finally）呼叫 LlmAssistantExecutor 既有 memory 機制的清除（先讀 LlmAssistantExecutor 看 memory 以什麼 id 註冊——Phase 1 以 executionId 為 key 累積；提供 `clearMemory(executionId)` 之類靜態/注入方法，引擎收尾呼叫）。

**Tests：** 扁平化插值路徑（`{{trigger 節點 key.xxx}}` 一層可取）、執行完成後 memory 已清除（可觀察的清除入口被呼叫）。

---

### Task 9: 前端——停止/取消後 RUNNING 節點終止態視覺

**Files:**
- Modify: `bestpartner-ui/src/stores/execution.ts`、`src/components/canvas/WorkflowNode.vue`（+ 對應樣式）
- Test: `src/stores/__tests__/execution.test.ts` 擴充（依既有測試檔實際路徑）

**契約（追記第 7 項）：** store 的 `stop()`（使用者按停止）與 SSE 中斷（onError/onDone 而 finalStatus 為 CANCELLED 或無 execution.completed 事件）時，將所有仍為 RUNNING 的 nodeStates 轉為 `CANCELLED` 狀態；`WorkflowNode.vue` 新增 `is-exec-cancelled` 樣式（灰橘斜紋或半透明+邊框虛線，與 SKIPPED 半透明區隔）。收到 `execution.completed status=CANCELLED` 亦同。

**Tests（vitest，於 `bestpartner-ui` 跑 `npm run test -- --run` 相關檔案）：** stop() 後 RUNNING→CANCELLED、completed CANCELLED 事件處理、正常 SUCCESS 不受影響。

---

### Task 10: 全量驗證（由編排腳本最後執行）

於 `bestpartner-service`：`./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=dev` 確認建置成功，再 `./gradlew test -Dorg.gradle.daemon=false` 全量測試（既有 4 個環境依賴失敗：Text2Sql/GoogleDrive/UserDelete/SystemSetting 相關，非本分支造成，可忽略但需列出實際失敗清單比對）；於 `bestpartner-ui`：`npm run test -- --run` 全綠。回報各項結果與新增測試數。
