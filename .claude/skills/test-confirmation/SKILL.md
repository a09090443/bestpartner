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

在 `bestpartner-service` 目錄執行：
```bash
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=dev
```

確認編譯成功（BUILD SUCCESSFUL）並產生 jar 檔案。若失敗，立即停止測試並回報錯誤。

> ⚠️ 若未重新編譯即啟動服務，視同測試流程違規。

### Step 6：強制重啟服務（測試前必要步驟）

停止現有服務（port 80），重新啟動並確認服務已正常回應（HTTP 200）後才開始測試。

> ⚠️ 若未重啟服務即開始測試，視同測試流程違規。

### Step 7：逐案執行循環（每個測試案例都必須照此順序）

1. **讀入模組檔案**：開啟對應的 `modules/test-module-{MODULE}.md` 檔案（見 checklist 「模組測試清單」章節）
2. **執行測試**：對該模組每個案例依序執行：
   - 準備 curl 指令（根據模組檔案中的 curl 範本）
   - 執行 curl 指令，取得 HTTP status code 與 response body
   - **立即寫入確認表**：附上完整的 curl 記錄和 response（不得使用佔位符、不得省略或截斷）
   - 標記狀態（✅ / ❌ / ⏭️）

> ⚠️ **必須先寫入 curl 記錄，再標記狀態；狀態欄填寫前若無記錄，視同違規。**

### Step 7.5：模組完成後立即更新測試結果摘要（強制執行，不可延後）

**每完成一個模組的所有測試案例後，必須立即更新確認表中的「測試結果摘要」統計表，嚴格執行以下檢查清單：**

#### 模組測試完成檢查清單

在進入下一個模組前，必須依序確認（打勾）以下項目：

```
□ 所有案例 curl 記錄已寫入確認表（無佔位符、無省略）
□ 所有案例狀態已標記（✅ / ❌ / ⏭️）
□ 失敗案例（❌）已在「問題追蹤區」詳細記錄（Test ID、現象、期望、實際、根因）
□ 摘要表中該模組的統計數字已更新：
  - ✅ Pass：符合狀態數
  - ❌ Fail：失敗案例數
  - ⏭️ Skip：跳過案例數
  - 總數：該模組的總案例數
□ Pass 率已計算並填入（格式：XX.X%）
□ 測試資料清理完成（若該模組有新增資料的 API 測試，務必呼叫刪除 API 清除）
```

#### 更新摘要表的方式

在「測試結果摘要」表中找到剛完成的模組行，填寫統計數字與 Pass 率：

```markdown
| 模組 | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|------|------|--------|--------|---------|---------|
| AUTH | 10 | 8 | 1 | 1 | 80.0% |  ← 範例：此模組完成
```

#### 嚴格規則

- **不可延後**：必須完成一個模組的所有測試後立即更新，不可等到所有模組測完才統一更新
- **即時發現問題**：若某模組 Pass 率過低（P0 應 100%、P1 應 ≥95%、P2 應 ≥80%），立即暫停並評估是否繼續
- **與問題追蹤區同步**：失敗案例必須在摘要更新前寫入問題追蹤區，缺一不可
- **計算必須精確**：Pass 率 = Pass 案例數 ÷ 總案例數 × 100%，不可四捨五入到整數（應保留一位小數）

> ⚠️ **若發現任何模組未按此步驟立即更新，視同測試流程違規，需撤銷摘要表並重新執行此步驟。**

### Step 8：測試完成後關閉服務

**所有測試案例執行完畢後，必須強制關閉服務。**

執行順序：
1. 確認所有測試已記錄至確認表
2. 強制停止服務（port 80）
3. 確認服務已停止，不再回應請求

> ⚠️ 測試完成後不得遺留服務在背景執行。


---

## API 維護規則

任何時候 API 發生新增、修改或刪除，必須同時更新 checklist 模板。詳細規則見 [api-maintenance.md](api-maintenance.md)。

---

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
| 先標記狀態才記錄 curl | 必須先執行 curl 並寫入記錄，才能標記狀態；先標記視同違規（Step 7） |
| response body 只記錄摘要或省略 | 必須記錄完整 response body 原始內容，不得截斷（Step 7） |
| **延後更新**：測試完成後才更新摘要 | **模組級強制更新**：每完成一個模組立即更新摘要表（Step 7.5） |
| **只更新一次**：所有模組測完後統一更新 | **即時更新**：必須完成每一個模組後立即填寫統計數字、Pass 率（Step 7.5） |
| **漏記模組統計**：忘記更新某個模組數字 | **檢查清單確認**：勾選「模組測試完成檢查清單」，確保統計完整（Step 7.5） |
| **摘要表與問題追蹤區脫節**：失敗案例未記錄 | **同步維護**：更新摘要表前先記錄所有 ❌ 項目（Test ID、現象、期望、實際、根因）（Step 7.5） |
