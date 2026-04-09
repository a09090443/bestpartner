---
name: test-confirmation
description: Use when user wants to start a test cycle, record API test results, or generate a test checklist for BestPartner. Triggered by "執行測試", "開始測試", "測試確認表", "test confirmation", "run tests".
---

# BestPartner 測試確認表產生器

## Overview

每次測試週期建立一份帶日期的確認記錄檔。**必須基於現有模板** `docs/test-confirmation-checklist.md`，並使用帶日期的命名，不得另行設計格式。

## When to Use

- 使用者說「執行測試」、「開始測試」、「建立測試確認表」
- 使用者要記錄本次 API 測試結果
- 進行定期回歸測試前

## Process（必須依序執行）

### Step 1：詢問 metadata

**測試日期與檔名自動取當下時間，不需詢問使用者。**

向使用者確認以下項目：
```
- 測試環境（dev / sit）
- 測試人員姓名
- LLM 平台（OpenAI / Ollama / Anthropic / Gemini / Grok / OpenRouter，可多選）
  ※ 若找不到對應的 LLM 設定資料，請同時提供 llm_setting id
```

### Step 2：建立帶日期時間的新檔案

**檔名格式固定**：`docs/test-confirmations/test-confirmation-YYYYMMDDHHmm.md`

例：當下時間為 2026-04-04 14:30 → `docs/test-confirmations/test-confirmation-202604041430.md`

**重要**：以 `.claude/skills/test-confirmation/test-confirmation-checklist.md` 的完整內容為基礎複製，不要自行設計表格格式。

### Step 3：填入 metadata

只修改「測試週期資訊」表格：

```markdown
| 測試日期 | YYYY-MM-DD HH:mm |
| 測試環境 | dev 或 sit |
| 測試人員 | [名字] |
| LLM 平台 | [平台名稱] |
| LLM Setting ID | [id，若找不到平台資料時填入，否則留空] |
```

其餘所有模組表格的「狀態」與「備註」欄位保持空白，供測試人員現場填寫。

### Step 4：告知使用者

回報：
1. 建立的檔案路徑
2. 狀態填寫符號說明（✅ / ❌ / ⏭️ / —）
3. 提醒測試完成後更新「測試結果摘要」統計表
4. 提醒 ❌ 項目需在「問題追蹤區」記錄

### Step 5：強制重啟服務（測試前必要步驟）

**在開始任何 API 測試前，必須強制停止並重新啟動服務。**

執行順序：
1. 確認服務正在執行（port 80）
2. 強制停止服務
3. 重新啟動服務並等待啟動完成（回應 HTTP 200）
4. 確認服務已正常啟動後，才開始進行測試

> ⚠️ 若未重啟服務即開始測試，視同測試流程違規，需重新執行此步驟。

### Step 6：測試完成後關閉服務

**所有測試案例執行完畢後，必須強制關閉服務。**

執行順序：
1. 確認所有測試已記錄至確認表
2. 強制停止服務（port 80）
3. 確認服務已停止，不再回應請求

> ⚠️ 測試完成後不得遺留服務在背景執行。

## 測試資料清理規則

**凡是測試新增資料的 API（如 POST 建立資源），測試完成後必須立即呼叫對應的刪除 API，以相同的條件（id、名稱、參數等）刪除該筆測試資料。**

清理原則：
- 新增成功（✅）：記錄後立即呼叫刪除 API 清除資料
- 新增失敗（❌）：確認資料未寫入，無需清理；在問題追蹤區記錄
- 若刪除 API 尚未實作或測試失敗：在備註欄說明，手動清除或標記為待清理

> ⚠️ 測試完成後若資料庫殘留測試資料，視同測試流程不完整，需補執行清理步驟。

## 測試中斷處理

### 遇到無效 API Key 時

若測試過程中任何端點回傳 API Key 無效（如 HTTP 401、403，或錯誤訊息含 `invalid api key`、`unauthorized`、`authentication failed` 等），**立即暫停測試**並向使用者詢問：

```
測試發現 API Key 無效，無法繼續呼叫 LLM。
請問 LLM SETTING 中，目前哪個 llmId 的設定是有效可測試的？
請提供可用的 llmId，後續測試將改用該設定。
```

取得使用者提供的 `llmId` 後：
1. 將該 `llmId` 填入已建立的確認表「測試週期資訊」中的 `LLM Setting ID` 欄位
2. 後續所有需要 LLM 的測試案例改用此 `llmId` 指定的設定繼續執行

## 狀態符號（不可更改）

| 符號 | 意義 |
|------|------|
| ✅ | Pass — 測試通過 |
| ❌ | Fail — 填入問題追蹤區 |
| ⏭️ | Skip — 備註欄說明原因 |
| — | 不適用 |

## 測試模組覆蓋範圍（共 12 個，152 案例）

| 模組 | 案例數 |
|------|-------|
| AUTH | 10 |
| CHAT | 16 |
| ADMIN CHAT | 5 |
| USER | 17 |
| LLM SETTING | 16 |
| VECTOR | 14 |
| TOOL | 19 |
| MCP | 19 |
| PERMISSION | 11 |
| SYSTEM SETTING | 13 |
| 跨模組整合 | 6 |
| 通用安全性 | 6 |

## 常見錯誤

| 錯誤 | 正確做法 |
|------|---------|
| 建立 `api-test-checklist.md` 固定名稱 | 使用帶日期的 `docs/test-confirmations/test-confirmation-YYYYMMDD.md` |
| 自行設計表格格式 | 從 `.claude/skills/test-confirmation/test-confirmation-checklist.md` 複製 |
| 只列出主要 7 個模組 | 必須包含整合測試與安全性測試（共 12 個模組） |
| 先建檔再詢問 | 先詢問 metadata，再建立檔案 |
| 未重啟服務即開始測試 | 測試前必須強制停止並重新啟動服務（Step 5） |
| 測試完成後未關閉服務 | 測試結束後必須強制停止服務（Step 6） |
| 新增資料測試後未清理 | 每次新增資料的 API 測試後，必須呼叫對應刪除 API 清除測試資料 |
