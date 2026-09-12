---
name: test-confirmation
description: Use when user wants to start a test cycle, record API test results, or generate a test checklist for BestPartner. Triggered by "執行測試", "開始測試", "測試確認表", "API 測試", "回歸測試", "煙霧測試", "test confirmation", "run tests".
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

**重要**：以 `.Codex/skills/test-confirmation/test-confirmation-checklist.md` 的完整內容為基礎複製，不要自行設計表格格式。

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

每完成一模組的所有測試案例後，必須立即依 [`test-confirmation-checklist.md`](test-confirmation-checklist.md) 中的「模組完成檢查清單」逐項勾選，並更新摘要表的統計數字與 Pass 率。

> ⚠️ 不可等到所有模組測完才統一更新；違反視同測試流程違規。詳細規則與範例見 checklist「模組完成檢查清單」章節。

### Step 8：測試資料處置詢問（強制停點，清理前必做）

**所有測試案例執行完畢、清理測試資料之前，必須用 `AskUserQuestion` 詢問使用者本次資料要清理還是保留**
（保留是為了讓使用者接著自行手動測試）。不得逕自清理，也不得逕自保留。

1. 先整理**本次建立的資料清單**（資源類型／名稱／id），來源是各案例執行時逐筆記錄的 id，不是事後掃描猜的。
2. 以 `AskUserQuestion` 提問，至少提供三個選項：
   - 全部清理（預設建議）
   - 全部保留（供後續手動測試）
   - 部分保留（列出清單讓使用者挑）
3. 依使用者決議執行：
   - **清理**：呼叫對應刪除 API，只刪清單內的 id（見「測試資料清理規則」）。
   - **保留**：不清理；把決議、保留清單與理由寫入確認表的「測試資料處置」欄位。
4. 無論保留與否，**服務仍一律關閉**（Step 9）；回報時附上重啟指令供使用者手動測試時自行啟動。

> ⚠️ 未詢問即清理或即保留，視同測試流程違規。完整規則見 [`test-data-retention.md`](../../rules/test-data-retention.md)。

### Step 9：測試完成後關閉服務

**所有測試案例執行完畢後，必須強制關閉服務。**

執行順序：
1. 確認所有測試已記錄至確認表
2. 確認 Step 8 的資料處置決議已執行並寫入確認表
3. 強制停止服務（port 80）
4. 確認服務已停止，不再回應請求

> ⚠️ 測試完成後不得遺留服務在背景執行。
> ⚠️ **保留資料 ≠ 保留服務**：使用者選擇保留資料時服務照關，回報中附上重啟指令：
> `cd bestpartner-service && java -Dquarkus.profile=dev -jar build/bestpartner-service-0.1.8-runner.jar`


---

## API 維護規則

任何時候 API 發生新增、修改或刪除，必須同時更新 checklist 模板。詳細規則見 [api-maintenance.md](api-maintenance.md)。

---

## 常見錯誤

> 步驟內 ⚠️ 違規警告為必讀，本表僅列出步驟未涵蓋的概念性錯誤。

| 錯誤 | 正確做法 |
|------|---------|
| 建立 `api-test-checklist.md` 固定名稱 | 使用帶日期的 `docs/test-confirmations/test-confirmation-YYYYMMDDHHmm.md` |
| 自行設計表格格式 | 從 `.Codex/skills/test-confirmation/test-confirmation-checklist.md` 完整複製 |
| 只列出主要 7 個模組 | 必須包含整合測試與安全性測試（共 12 個模組） |
| 先建檔再詢問 metadata | 先詢問 metadata（Step 1），再建立檔案（Step 2） |
| 先標記狀態才記錄 curl | 先執行 curl 並寫入完整記錄，才能標記狀態（Step 7） |
| response body 只記錄摘要或省略 | 必須記錄完整 response body 原始內容，不得截斷（Step 7） |
| 測完直接清理測試資料 | Step 8 先用 `AskUserQuestion` 問清理或保留，等使用者回覆再動手 |
| 使用者要保留資料就順手把服務留著 | 服務一律關閉（Step 9），回報附重啟指令 |
