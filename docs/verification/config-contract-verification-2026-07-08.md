# Config 契約功能驗證報告

> **驗證日期**：2026-07-08
> **驗證範圍**：workflow 節點 config 契約 + tool settingSchema 契約 + 前端 schema-driven 消費 + 存檔前即時必填驗證
> **驗證方法**：三個獨立 subagent 以 fresh context 分別查證後端、前端、契約/文件三個維度，各自實跑測試並對抗性檢查程式碼（不繼承既有敘述，只信程式碼與實測結果）
> **對應版本**：0.1.8（分支 `0.1.8`）

---

## 總結論

| 維度 | 結果 | 硬證據 |
|------|:----:|--------|
| 後端功能 | ✅ 可用、無阻斷問題 | 41 測試全過（含需 DB 的 @QuarkusTest 整合測試） |
| 前端功能 | ✅ 可用、無阻斷問題 | 275 測試全過、vue-tsc exit 0 |
| 前後端契約一致性 | ✅ 完全一致 | 10 種 NodeType 必填欄位三方逐一吻合 |
| 文件描述準確性 | ✅ 準確、無誇大 | 端點數 67、版本、Postman、範例皆核對一致 |

**三個維度獨立查證，結論一致：功能確實可用、程式碼與敘述吻合、未發現「該擋沒擋」「靜默吞型別」「有填卻判缺漏」等問題。** 僅有兩個「非缺陷、已載明」的邊界與一個既有安全待辦（見文末）。

---

## 一、後端 config 契約（✅ 可用）

### 實測（硬證據）
DB 就緒，`./gradlew test` **BUILD SUCCESSFUL**，各測試檔 JUnit XML 權威計數：

| 測試檔 | tests | failures | errors |
|--------|:-----:|:--------:|:------:|
| `NodeConfigTest`（純單元） | 10 | 0 | 0 |
| `WorkflowServiceTest`（@QuarkusTest） | 18 | 0 | 0 |
| `WorkflowResourceTest`（@QuarkusTest） | 6 | 0 | 0 |
| `ToolSchemaGeneratorTest`（純單元） | 4 | 0 | 0 |
| `ToolServiceTest`（@QuarkusTest） | 2 | 0 | 0 |
| `LLMToolResourceTest`（@QuarkusTest） | 1 | 0 | 0 |
| **合計** | **41** | **0** | **0** |

### 逐項查證
1. **nodeRequiredFields 端點**（`WorkflowResource.kt:78-81`）→ `WorkflowService.getNodeRequiredFields()`（`:246-249`）以 `NodeType.entries` + `NodeConfigRegistry.parse(type, 空config).missingRequiredFields()` 產生必填清單，**源自契約、非硬編**，涵蓋全部 NodeType。
2. **NodeConfig 契約**（`NodeConfig.kt`）：10 種 NodeType 各一 `@Serializable` config + `missingRequiredFields()`；`NodeConfigRegistry.parse` 的 `when(type)` 對 10 型別逐一分派、**無 `else ->` 兜底**（enum exhaustive，新增型別未補即編譯失敗）。`ignoreUnknownKeys = false`（未知欄位/結構性型別錯誤拋例外）。
3. **兩段式驗證**（`WorkflowService.kt`）：
   - save 依 NodeType 強型別反序列化驗型別（parse 失敗→`WORKFLOW_NODE_CONFIG_INVALID` 400），**必填缺席放行**（DRAFT 可存半成品）。
   - switchStatus 啟用逐節點驗必填，缺漏回 `WORKFLOW_NODE_CONFIG_REQUIRED_MISSING`（帶 nodeKey + 缺漏欄位）。
   - ACTIVE 重存（`:97-99`）對 status 已 ACTIVE 者驗必填，**不誤擋 DRAFT**。
   - 舊欄位遷移 `normalizeLegacyNodeConfigs`（`:301-312`）將 MCP 節點 `mcpSettingId`→`userSettingId`。
4. **settingSchema 反射**（`ToolSchemaGenerator.generate`）：反射 config data class primary constructor 產生 schema（type/required/sensitive/description），失敗則 warn + 回 null（不炸清單）；`tool/list`、`tool/get` 皆回 settingSchema。

### 對抗性檢查
`when` 無 else 掩蓋、NodeType(10) 與 NodeConfig(10) 一一對應、必填判斷一致用 `isNullOrBlank/isNullOrEmpty`、ACTIVE 判斷精確——**未發現任何真實 bug**。唯一觀察（非缺陷、已註解記錄）：kotlinx tree decoding 對 scalar 字串寬鬆轉型（`topK:"5"` 可接受），屬框架已知行為。

---

## 二、前端 schema-driven 功能（✅ 可用）

### 實測（硬證據）
- `npm run test`：**Test Files 32 passed、Tests 275 passed**，exit 0
- `npx vue-tsc -b`：**exit 0**，無型別錯誤

### 逐項查證
1. **api/workflow.ts**：`getNodeRequiredFields()` GET 正確路徑 + module-scope Promise 快取（並發去重、`.catch` 清壞快取可重試）+ `invalidateNodeRequiredFieldsCache()`。快取「非 per-user、不需登出清」判斷合理（靜態契約）。
2. **utils/nodeRequiredFields.ts**：`missingRequiredForNode` 判空涵蓋 undefined/null/空字串/空陣列/**空物件 `{}`**；複合 `a|b` 擇一有值即滿足、全空回原字樣。`formatMissingFields` 中文化 + `a|b`→「擇一」+ 未知鍵 fallback。**FIELD_LABELS 僅顯示標籤、未參與必填判斷**。
3. **useGraphValidation.ts**：`validateGraph` 第三參數可選（不傳跳過必填檢查，保護既有呼叫端）；缺漏 push `REQUIRED_FIELD_MISSING`(warning)。
4. **WorkflowEditorView.handleSave**：快照在 `await getNodeRequiredFields()` **之後**擷取；ACTIVE 必填缺漏升 error 擋存、DRAFT/INACTIVE warning 放行；查詢失敗 try/catch 降級不阻斷。
5. **InspectorPanel.vue**：選中節點必填缺漏即時提示；`requiredWarningSeverity` 依 ACTIVE 切紅(error)/橘(warning)。
6. **表單欄位名**：LLM/Tool/MCP/RAG 表單 emit 欄位名與後端契約**逐一一致**；arguments 欄位經 `parseJsonObjectField` 僅接受合法 JSON 純物件。
7. **auth.ts logout**：清 `invalidateSettingsCache()` + `clearNodeOptionsCache()`，避免跨使用者殘留。

### 對抗性檢查
空物件正確判缺漏、**前端無任何硬編必填清單**（grep 確認驗證邏輯只讀後端 `requiredFields`）、ACTIVE 擋存篩選無誤擋/漏擋、欄位名無不符——**未發現阻斷性問題**。

---

## 三、前後端契約 × 文件一致性（✅ 一致）

### 10 種 NodeType 必填欄位三方對照

| NodeType | 後端 NodeConfig | 前端表單 emit | 文件標「必填」 | |
|----------|-----------------|---------------|----------------|:--:|
| TRIGGER | triggerType | JSON 編輯器 | triggerType | ✅ |
| LLM_ASSISTANT | llmId | llmId | llmId | ✅ |
| TOOL | toolId | toolId | toolId | ✅ |
| MCP_SERVER | mcpId, toolName | mcpId, toolName | mcpId, toolName | ✅ |
| KNOWLEDGE_RAG | knowledgeId, embeddingModelId, query | 三者 | 三者 | ✅ |
| CONDITION | conditions | JSON 編輯器 | conditions | ✅ |
| LOOP | inputArrayPath, loopBodyEntryNodeKey | JSON 編輯器 | 兩者 | ✅ |
| CODE | language, source | JSON 編輯器 | 兩者 | ✅ |
| HTTP_REQUEST | method, url | JSON 編輯器 | 兩者 | ✅ |
| DATA_TRANSFORM | mappings\|template | JSON 編輯器 | 擇一 | ✅ |

> 四種有型別化表單的節點欄位名前後端逐一吻合；其餘六種前端正確走 JSON 編輯器（`InspectorPanel` TYPED_FORMS 僅含前四種），與文件描述一致。

### 逐項查證
- **端點路徑/方法/回傳**：`GET /llm/workflow/nodeRequiredFields`、`Map<String,List<String>>` ↔ `Record<string,string[]>` 吻合。
- **NodeType 完整性**：enum(10) = NodeConfig(10) = when 分支(10) = 文件(10)。
- **回傳範例準確**：workflow.md 的 nodeRequiredFields JSON 範例 10 型別與後端實際輸出一致（含 `DATA_TRANSFORM: ["mappings|template"]`）。
- **端點總數**：實際數得 **67**（WorkflowResource 8 個含 nodeRequiredFields），與 api-endpoints.md 宣稱「13 模組、67 endpoint」一致。
- **版本**：`0.1.8` 全域一致（build.gradle.kts、CLAUDE.md、AGENTS.md、rules、installation、jar 檔名），**無 0.1.7 殘留**（排除 test-confirmations/、build/、node_modules/）。
- **Postman**：含 nodeRequiredFields request，JSON 合法（`JSON.parse` 通過）。
- **arguments 文件**：過時的「尚無表單」描述已清除，TOOL/MCP 欄位表已列 arguments。

### 對抗性檢查
文件無「宣稱已完成但程式碼缺失」；`mcpSettingId`→`userSettingId` 命名遷移前後端一致；`features/workflow.md` 誠實標註「執行引擎將於後續階段推出」（後端確無執行引擎程式碼，僅 CRUD + 驗證）——**未發現不一致**。

---

## 四、已知邊界與待辦（非本次驗證的阻斷問題）

1. **降級邊界（非缺陷，已載明）**：ACTIVE workflow 存檔時若 `getNodeRequiredFields()` 網路失敗，前端降級跳過必填檢查。屬刻意設計（不阻斷存檔），且後端 `/save` 對 ACTIVE 本就會驗必填兜底，該流程既為 ACTIVE 即已於啟用時驗過。
2. **框架行為（非缺陷，已註解）**：kotlinx tree decoding 對 scalar 字串寬鬆轉型（`topK:"5"` 被接受）。
3. **既有功能限制（誠實標註）**：workflow **節點執行引擎尚未實作**——目前只儲存與驗證節點定義，執行邏輯屬 Phase 2 範圍，`features/workflow.md` 已明載。
4. **⚠️ 安全待辦（程式無法代勞）**：先前 ToolServiceTest / Postman 中掩蔽的 Google / Tavily API 金鑰仍存在於 git 歷史，**需自行至兩平台撤銷（revoke）**。

---

## 五、驗證方法說明

本報告由三個獨立 subagent 以互不繼承的 fresh context 分別查證，各自：
- 讀取實際程式碼（非依賴既有註解/敘述）
- 實跑測試取得硬證據（後端 gradle 41 測試、前端 vitest 275 測試 + vue-tsc）
- 對抗性檢查（主動找 bug、契約不符、文件誇大，而非確認存在）

三方結論一致收斂，交叉印證功能可用性與程式碼—文件一致性。

> 註：本報告為靜態 + 測試級驗證。若需 live 端到端 API 測試（啟動服務打實際端點），依專案規範須另循 `test-confirmation` skill 流程。
