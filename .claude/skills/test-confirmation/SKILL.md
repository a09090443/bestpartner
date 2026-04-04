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

先向使用者確認：
```
- 測試日期（預設：今天）
- 測試環境（dev / sit）
- 測試人員姓名
```

### Step 2：建立帶日期的新檔案

**檔名格式固定**：`docs/test-confirmation-YYYYMMDD.md`

例：今天為 2026-04-04 → `docs/test-confirmation-20260404.md`

**重要**：以 `.claude/skills/test-confirmation/test-confirmation-checklist.md` 的完整內容為基礎複製，不要自行設計表格格式。

### Step 3：填入 metadata

只修改「測試週期資訊」表格：

```markdown
| 測試日期 | YYYY-MM-DD |
| 測試環境 | dev 或 sit |
| 測試人員 | [名字] |
```

其餘所有模組表格的「狀態」與「備註」欄位保持空白，供測試人員現場填寫。

### Step 4：告知使用者

回報：
1. 建立的檔案路徑
2. 狀態填寫符號說明（✅ / ❌ / ⏭️ / —）
3. 提醒測試完成後更新「測試結果摘要」統計表
4. 提醒 ❌ 項目需在「問題追蹤區」記錄

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
| 建立 `api-test-checklist.md` 固定名稱 | 使用帶日期的 `test-confirmation-YYYYMMDD.md` |
| 自行設計表格格式 | 從 `.claude/skills/test-confirmation/test-confirmation-checklist.md` 複製 |
| 只列出主要 7 個模組 | 必須包含整合測試與安全性測試（共 12 個模組） |
| 先建檔再詢問 | 先詢問 metadata，再建立檔案 |
