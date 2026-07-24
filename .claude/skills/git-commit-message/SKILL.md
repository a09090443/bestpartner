---
name: git-commit-message
description: Use when writing a git commit message or running git commit in the BestPartner project. Commit subjects must be in 繁體中文 and follow the <類型>(<範圍>): <主旨> format with an allowed type and module scope.
---

# Git Commit 訊息規範

## 格式

```
<類型>(<範圍>): <主旨>

<本文（選填）>
```

## 類型（Type）

| 類型 | 說明 |
|---|---|
| `新增` | 新功能 |
| `修復` | 錯誤修復 |
| `重構` | 程式碼重構（不影響功能） |
| `測試` | 新增或修改測試 |
| `文件` | 文件更新 |
| `設定` | 設定檔、建構工具或依賴套件變更 |
| `效能` | 效能改善 |
| `移除` | 刪除程式碼或檔案 |

## 範圍（Scope）

填入受影響的模組名稱，例如：`bestpartner-service`、`bestpartner-mcp-servers`、`docs-site`。

模組內部可進一步以 package 或功能標示，例如：`resource`、`service`、`entity`、`repository`、`skill`、`tool`、`mcp`、`vector`、`auth`、`i18n`。

若變更不限於單一模組（如升級全域依賴、修改根目錄設定），可省略範圍，格式簡化為 `<類型>: <主旨>`。

## 規則

- 主旨使用**繁體中文**，簡短描述「做了什麼」
- 主旨不超過 50 個字
- 本文說明「為什麼」這樣改，每行不超過 72 個字
- 本文與主旨之間空一行

## 範例

```
新增(skill): 新增 Skill 上傳與管理 API 端點

支援以 zip 壓縮檔上傳個人與 global skill，解壓後存於使用者專屬目錄。
```

```
修復(auth): 修正 JWT refresh 流程缺少簽章驗證的問題
```

```
重構(llm-setting): 為 llm_setting 新增 api_key 欄位並調整 SQL 插入語句
```

```
設定: 升級 Quarkus 至 3.21.0
```

## 執行 git commit（跨 shell 語法，務必先對準工具）

> ⚠️ 本專案環境同時有 **PowerShell（主要）** 與 **Bash** 兩個工具，兩者的引號與多行字串語法**完全不同**。
> 傳多行 commit 訊息前，先確認「這一刀是用哪個工具送出」，再選對應語法——**混用會把語法字元漏進主旨**
> （實際踩過：在 Bash 用 PowerShell here-string `-m @'...'@`，bash 把 `@` 當字面字元，主旨變成 `@ 測試(...)`）。

**最穩妥：主旨與本文各用一個 `-m`（兩種 shell 皆通用，優先採用）**

```bash
git commit -m "測試(e2e): 收錄 RAG 端到端旅程為 J8" -m "本文說明為什麼這樣改……" \
  -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

多個 `-m` 會自動以空行分隔，天然滿足「主旨與本文空一行」，且不需 here-string、不吃跳脫問題。

**若本文較長需 here-string，語法依工具而定：**

| 工具 | 多行字串語法 | 收尾 |
|---|---|---|
| **PowerShell** | `git commit -m @'`＋換行＋內容＋換行＋`'@` | `'@` 必須在**行首第 0 欄**，不可縮排 |
| **Bash** | `git commit -F -` 搭配 heredoc（`<<'EOF' … EOF`），或直接多個 `-m` | 勿用 `@'...'@`（那是 PowerShell 專屬） |

## 提交後必做：立即核對主旨

commit 訊息語法錯誤時 `git commit` **通常不會報錯**（訊息仍「合法」，只是內容錯），會靜默成功。
因此提交後**一律**印出主旨核對，錯了當場 `git commit --amend` 修正：

```bash
git log -1 --pretty=format:"%s%n%n%b"
```

## 常見錯誤

| 錯誤 | 修正 |
|---|---|
| 主旨用英文（如 `fix: ...`、`feat: ...`） | 改用繁體中文類型詞（`修復`、`新增`） |
| 類型不在允許清單內 | 僅能用上表八種類型 |
| 主旨超過 50 字、塞入「為什麼」 | 「為什麼」移到本文，主旨只寫「做了什麼」 |
| 本文與主旨之間沒空一行 | 兩者之間保留一行空白 |
| 在 Bash 工具用 PowerShell here-string `-m @'...'@`，主旨混入 `@` 等字元 | 先對準工具：兩 shell 通用寫法用多個 `-m`；提交後以 `git log -1` 核對主旨 |
