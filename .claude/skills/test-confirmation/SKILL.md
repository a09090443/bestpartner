---
name: test-confirmation
description: Use when user wants to start a test cycle, record API test results, or generate a test checklist for BestPartner. Triggered by "執行測試", "開始測試", "測試確認表", "test confirmation", "run tests".
---

# BestPartner 測試確認表產生器

## Overview

每次測試週期建立一份帶日期的確認記錄檔。**必須基於現有模板** `./test-confirmation-checklist.md`，並使用帶日期的命名，不得另行設計格式。

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

### Step 5：重新編譯專案（測試前必要步驟）

**在啟動服務前，必須先重新編譯專案，確保測試的是最新程式碼。**

執行指令（於 `bestpartner-service` 目錄）：
```bash
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=dev
```

執行順序：
1. 切換至 `bestpartner-service` 目錄
2. 執行上述編譯指令，等待完成
3. 確認編譯成功（BUILD SUCCESSFUL），產生 `build/bestpartner-service-0.1.7-SNAPSHOT-runner.jar`
4. 若編譯失敗，**立即停止測試流程**，回報錯誤給使用者

> ⚠️ 若未重新編譯即啟動服務，視同測試流程違規，需重新執行此步驟。

### Step 6：強制重啟服務（測試前必要步驟）

**在開始任何 API 測試前，必須強制停止並重新啟動服務。**

執行順序：
1. 確認服務正在執行（port 80）
2. 強制停止服務
3. 重新啟動服務並等待啟動完成（回應 HTTP 200）
4. 確認服務已正常啟動後，才開始進行測試

> ⚠️ 若未重啟服務即開始測試，視同測試流程違規，需重新執行此步驟。

### Step 7：逐案執行循環（每個測試案例都必須照此順序）

**對每個測試案例，依序執行以下動作，缺一不可：**

1. **準備 curl 指令**：根據測試場景組合完整的 curl 指令（含真實 token、真實 id、真實 request body）
2. **執行 curl 指令**：實際發送請求，取得 HTTP status code 與 response body
3. **立即寫入確認表**：在該案例對應位置附上以下區塊，**不得使用佔位符**

```bash
# [TEST-ID] 測試場景名稱
curl -s -X METHOD http://localhost:80/path \
  -H "Authorization: Bearer eyJhbGci...（完整 token）" \
  -H "Content-Type: application/json" \
  -d '{"field":"實際值"}'
# HTTP 200
# {"data":...（實際回應摘要）}
```

4. **標記狀態**：確認 curl 已寫入後，才在狀態欄填入 ✅ / ❌ / ⏭️

> ⚠️ **封鎖規則**：狀態欄填寫任何符號前，若該案例尚未附上真實 curl 記錄，視同非法操作，必須先補記錄。

#### 模組結束檢核

每個模組最後一個案例完成後，執行自我檢查：
- 本模組所有案例是否都有 curl 記錄？
- curl 記錄中是否有未替換的佔位符（`<token>`、`<id>` 等）？
- 若有遺漏，**立即補寫**，不得繼續下一模組。

### Step 8：測試完成後關閉服務

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

## 測試模組覆蓋範圍

**覆蓋範圍以 `.claude/skills/test-confirmation/test-confirmation-checklist.md` 的「測試結果摘要」表為準。**

執行測試前，必須讀取該 checklist 檔案以確認最新的模組清單、案例數與通過標準，不得沿用任何硬編碼的模組數或案例總計。

## 常見錯誤

| 錯誤 | 正確做法 |
|------|---------|
| 建立 `api-test-checklist.md` 固定名稱 | 使用帶日期的 `docs/test-confirmations/test-confirmation-YYYYMMDD.md` |
| 自行設計表格格式 | 從 `.claude/skills/test-confirmation/test-confirmation-checklist.md` 複製 |
| 只列出主要 7 個模組 | 必須包含整合測試與安全性測試（共 12 個模組） |
| 先建檔再詢問 | 先詢問 metadata，再建立檔案 |
| 未重新編譯即啟動服務 | 測試前必須先執行 Gradle 編譯（Step 5） |
| 未重啟服務即開始測試 | 測試前必須強制停止並重新啟動服務（Step 6） |
| 測試完成後未關閉服務 | 測試結束後必須強制停止服務（Step 8） |
| 新增資料測試後未清理 | 每次新增資料的 API 測試後，必須呼叫對應刪除 API 清除測試資料 |
| 先標記狀態才記錄 curl | 必須先執行 curl 並寫入記錄，才能標記狀態；先標記視同違規，需撤銷並補記錄（Step 7） |
| 直接使用記憶中的模組數與案例數 | 每次執行前讀取 `.claude/skills/test-confirmation/test-confirmation-checklist.md` 取得最新數字 |
