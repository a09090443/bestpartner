# BestPartner API 測試功能確認表

> 本次為 **Phase 2 workflow 執行引擎實機驗收**（聚焦 `POST /llm/workflow/execute` 各節點型別）。
> 格式沿用 `.claude/skills/test-confirmation/test-confirmation-checklist.md`；因 modules/ 無 WORKFLOW 模組範本，案例為本次針對 Phase 2 設計。

---

## 使用說明

### 狀態符號

| 符號 | 意義 |
|------|------|
| ✅ | Pass — 測試通過 |
| ❌ | Fail — 測試失敗（請在「問題追蹤區」補充說明） |
| ⏭️ | Skip — 本次略過（請在備註欄說明原因） |
| — | 本次週期不適用 |

---

## 測試週期資訊

| 項目 | 內容 |
|------|------|
| 測試日期 | 2026-07-11 12:59 |
| 服務版本 | 0.1.8-SNAPSHOT |
| 測試環境 | dev |
| LLM 平台 | OpenRouter |
| LLM Setting ID | （登入後查詢填入） |
| MCP Server | date（`java -jar D:/MCP/date.jar`） |
| Base URL | `http://localhost:80` |
| 測試帳號 | test_user（`test@partmer.com.tw` / `user`） |
| 測試結束日期 | |
| 使用 jar | `bestpartner-service-0.1.8-SNAPSHOT-runner.jar`（測試前 clean build，含 Phase 2 全部程式與 GraalJS 建置修復 c699dba） |

---

## 測試範圍與驗收重點

Phase 2 新增/變更的執行行為，逐項以實機執行驗證：

| 重點 | 對應節點/機制 |
|------|--------------|
| 條件分支與 SKIPPED 可達性 | CONDITION |
| 迴圈子圖迭代與 loop_index | LOOP |
| GraalJS sandbox（功能 + 限制 + 逾時） | CODE |
| 資料轉換 | DATA_TRANSFORM |
| 工具動態呼叫 | TOOL（date） |
| 節點逾時通用機制 | timeoutMs |
| trigger 輸出扁平化 | TRIGGER |
| 插值失敗語意 | ExecutionContext |
| LLM + MCP 迴歸（Phase 1 仍正常） | LLM_ASSISTANT + MCP_SERVER |

> HTTP 進階欄位（headers/timeout）與 RAG 檢索參數（topK/minScore/embeddingModelId）生效：已由後端單元測試覆蓋（`HttpRequestExecutorTest` / `KnowledgeRagExecutor` 相關），實機需 mock server / 知識庫資料，本次以單元測試佐證、不另跑實機 curl（於備註標示）。

---

## WORKFLOW 執行測試案例

| 編號 | 案例 | 驗證重點 | 狀態 | 備註 |
|------|------|----------|:----:|------|
| WF-00 | 前置：登入 test_user 取 JWT、查 OpenRouter llmId | 認證與環境就緒 | | |
| WF-01 | CODE 節點正常執行（TRIGGER→CODE→OUTPUT，JS 回傳計算物件） | GraalJS 功能、input 讀上游、最後表達式為輸出 | | |
| WF-02 | CODE sandbox 限制（`Java.type` host 存取） | 節點 FAILED，沙箱阻擋 | | |
| WF-03 | CODE 節點逾時（`while(true)`，timeoutMs=500） | 節點 FAILED（逾時），下游 SKIPPED、整體 FAILED | | |
| WF-04 | DATA_TRANSFORM（mappings 原生型別 + template） | 資料轉換、純 {{path}} 保留型別 | | |
| WF-05 | CONDITION 分支（true 走向） | 走對分支、false 側下游 SKIPPED | | |
| WF-06 | CONDITION 分支（false 走向） | 走對分支、true 側下游 SKIPPED | | |
| WF-07 | LOOP 迭代（TRIGGER→LOOP[子圖 CODE]→OUTPUT） | loop_index 逐筆落紀錄、collectOutputKey 彙集、out:done 下游取值 | | |
| WF-08 | TOOL 動態呼叫（date 工具） | 工具實例化與呼叫、輸出結構化 | | |
| WF-09 | trigger 輸出扁平化（`{{triggerKey.欄位}}` 一層） | 扁平化插值路徑生效 | | |
| WF-10 | 插值引用不存在變數 | 節點 FAILED，錯誤訊息含變數路徑 | | |
| WF-11 | LLM + MCP 迴歸（OpenRouter + date，Phase 1 模式） | 完整流程仍 SUCCESS、最終輸出正確 | | |

---

## 測試結果摘要

| 模組 | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|------|:----:|:------:|:------:|:------:|:------:|
| WORKFLOW 執行（Phase 2） | 12 | | | | |
| **合計** | **12** | | | | |

---

## 問題追蹤區

> 每個 ❌ 項目建立一筆記錄。

| 問題編號 | 對應案例 | 現象 | 預期 | 實際 | 處置 |
|----------|----------|------|------|------|------|
| | | | | | |
