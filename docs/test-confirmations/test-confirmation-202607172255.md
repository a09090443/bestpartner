# BestPartner API 測試功能確認表

> 本次為**聚焦驗證**：僅涵蓋 workflow `HTTP_REQUEST` 節點的 `secretHeaders` 加密機制。
> 其餘 12 個標準模組本週期不適用（標記 —），完整回歸請另建週期。
> 契約見 `docs/workflow-engine/system-design.md` §2.9；實作 `converter/WorkflowSecretConverter.kt`。

---

## 測試週期資訊

| 項目 | 內容 |
|------|------|
| 測試日期 | 2026-07-17 22:55 |
| 服務版本 | 0.1.8-SNAPSHOT |
| 測試環境 | dev |
| LLM 平台 | 不適用（HTTP_REQUEST 不呼叫 LLM） |
| LLM Setting ID | — |
| Base URL | `http://localhost:80` |
| 測試結束日期 | 2026-07-17 23:20 |

---

## 狀態符號

| 符號 | 意義 |
|------|------|
| ✅ | Pass — 測試通過 |
| ❌ | Fail — 測試失敗（於「問題追蹤區」補充） |
| ⏭️ | Skip — 本次略過 |
| — | 本次週期不適用 |

---

## 測試結果摘要

| 模組 | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|------|------|--------|--------|---------|---------|
| WORKFLOW secretHeaders | 8 | 8 | 0 | 0 | 100.0% |
| （其餘 12 標準模組） | — | — | — | — | 本次不適用 |

> ⚠️ SEC-HDR-008 首輪揭露設計缺口（送遮罩值時舊明文不轉密、永久殘留），已於 `WorkflowSecretConverter.reEncryptLegacyPlaintext` 修正並重編譯、重啟、重驗通過。上表為修正後結果。

---

## WORKFLOW — HTTP_REQUEST `secretHeaders` 加密驗證

> DB 查核：`SELECT node_key, config FROM bestpartner.llm_workflow_node WHERE workflow_id = '<id>';`

| Test ID | 優先 | 案例 | 預期 | 狀態 |
|---------|:---:|------|------|:---:|
| SEC-HDR-001 | P1 | save 帶明文 secretHeaders，查 DB | config 內為 `{iv}$…` 密文，不含明文 | ✅ |
| SEC-HDR-002 | P1 | get 取回同一 workflow | `secretHeaders` 值為 `__SECRET_KEPT__` | ✅ |
| SEC-HDR-003 | P1 | 遮罩值原樣 save 回去 | DB 密文不變（沿用） | ✅ |
| SEC-HDR-004 | P1 | execute 對 mock server 送請求 | 收到 header 為解密明文 | ✅ |
| SEC-HDR-005 | P1 | 查 node_execution.input | `config.secretHeaders` 為 `__SECRET_KEPT__` | ✅ |
| SEC-HDR-006 | P1 | save 填新明文（改金鑰） | DB 密文更新、可解密為新值 | ✅ |
| SEC-HDR-007 | P2 | 改 header 名稱 + 送遮罩值 | 該欄位被移除 | ✅ |
| SEC-HDR-008 | P2 | 舊明文送遮罩 save 後 | 自動轉為密文（不殘留明文） | ✅ |

### 執行記錄（關鍵證據）

| Test ID | 證據 |
|---------|------|
| SEC-HDR-001 | DB `config.secretHeaders.Authorization` = `94xRHvEgIHriv4qj$Xe4x2780…`（密文），不含 `sk-test-plain-001`；url/method/headers 仍明文 |
| SEC-HDR-002 | get 回應 `"secretHeaders":{"Authorization":"__SECRET_KEPT__"}`，明文出現 0 次 |
| SEC-HDR-003 | 送 `__SECRET_KEPT__` 存回後 DB 密文與 001 **逐字元相同**（沿用未重加密；AES-GCM 每次 IV 隨機故可判別） |
| SEC-HDR-004 | mock server 收到 `AUTH=[Bearer sk-test-plain-001]`（解密明文），execution SUCCESS |
| SEC-HDR-005 | `node_execution.input.config.secretHeaders.Authorization` = `__SECRET_KEPT__`，整包 input 不含明文 |
| SEC-HDR-006 | 填新明文後 DB 密文 = `jeNKAHY6yrpZ4hRI$t79…`（≠ 舊密文），execute 送出 `Bearer sk-new-key-002` |
| SEC-HDR-007 | key 由 `Authorization` 改 `X-Custom-Auth` + 送遮罩 → DB `secretHeaders = {}`，遮罩字面值未落地 |
| SEC-HDR-008 | SQL 塞明文 `Bearer legacy-plain-008b` → 送遮罩 save → DB 轉為密文 `QHAN+0iWw5qh8oku$cByZ…`，明文清零；補加密後 execute 仍正確送出明文 |

> **首輪缺陷（已修）**：修正前，SEC-HDR-008 送遮罩值後 DB 仍殘留明文（sentinel 沿用的是明文本身）。
> 加入 `reEncryptLegacyPlaintext`（沿用既有值時偵測非密文格式即補加密）後，重編譯 → 重啟 → 重驗通過。
> 單元測試 `WorkflowSecretConverterTest` 同步新增「沿用既有明文時補加密」「沿用既有密文時不重複加密」兩案。

### 最終資安確認

- http 節點 4 筆執行紀錄的 `input`：明文洩漏 **0 筆**，全部遮罩。

---

## 問題追蹤區

| Test ID | 現象 | 期望行為 | 實際行為 | 根因 | 狀態 |
|---------|------|---------|---------|------|------|
| | | | | | |

---

## 測試資料清理

- [x] 測試建立的 workflow 已刪除（`/llm/workflow/delete`）
- [x] `llm_workflow_node` / `llm_workflow_edge` 連鎖刪除（0 殘留）
- [x] execution 相關表手動清除（delete workflow 不連鎖刪 execution，屬既有行為；已 SQL 清空，0 殘留）
- [x] 服務（port 80）與 mock server（port 18080）已停止
