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

## 常見錯誤

| 錯誤 | 修正 |
|---|---|
| 主旨用英文（如 `fix: ...`、`feat: ...`） | 改用繁體中文類型詞（`修復`、`新增`） |
| 類型不在允許清單內 | 僅能用上表八種類型 |
| 主旨超過 50 字、塞入「為什麼」 | 「為什麼」移到本文，主旨只寫「做了什麼」 |
| 本文與主旨之間沒空一行 | 兩者之間保留一行空白 |
