---
name: documentation-sync
description: Use when completing any code changes involving package/API/table modifications in BestPartner project, before declaring work done. Triggered by completing implementation of new endpoints, entity changes, package restructuring, or dependency updates.
---

# Documentation Sync

## Overview

完成套件異動、API 新增/修改、資料表變更後，自動掃描 git diff 判斷影響範圍，列出需要更新的文件清單，並詢問是否一起更新。

## Process（必須依序執行）

### Step 1：讀取 git diff 判斷變更類型

```bash
git diff --name-only HEAD
git diff --name-only --cached
```

合併結果，依以下規則判定（可複數同時成立）：

| 偵測條件 | 變更類型 |
|---------|---------|
| `resource/` 下 `.kt` 有異動，或 diff 含 `@Path` | ⚡ API 變更 |
| `entity/` 下 `.kt` 有異動，或 `bestpartner.sql` 有異動 | ⚡ 資料表變更 |
| `build.gradle.kts` / `gradle.properties` 有異動，或 package 目錄搬移 | ⚡ 套件異動 |
| 任何 `.kt` 檔案被新增或刪除 | ➕ 目錄樹更新（附加於上述類型） |

---

### Step 2：依變更類型組合文件清單

**套件異動** 時，檢查：
- `rules/architecture-and-packages.md`
- `rules/naming-conventions.md`
- `rules/tech-stack-and-versions.md`
- `docs-site/docs/architecture/modules.md`
- `docs-site/docs/architecture/tech-stack.md`
- `docs-site/docs/architecture/overview.md`
- `README.md`（目錄結構章節）
- `AGENTS.md`（目錄樹區塊）

**API 新增/修改** 時，檢查：
- `rules/api-endpoints.md`
- `rules/api-testing.md`
- `rules/api-documentation.md`
- `docs-site/docs/api/authentication.md`
- `docs-site/docs/api/swagger.md`
- `docs-site/docs/api/assistant.md`
- `docs-site/docs/api/llm-setting.md`
- `docs-site/docs/api/knowledge-base.md`
- `docs-site/docs/features/` 相關功能頁（ai-models.md、mcp-servers.md、tools.md）
- `README.md`（功能章節）
- `docs/api-test-plan.md`
- `docs/postman/basepartner.postman_collection.json`

**資料表/欄位異動** 時，檢查：
- `rules/architecture-and-packages.md`
- `docs-site/docs/architecture/overview.md`
- `docs-site/docs/getting-started/installation.md`
- `README.md`
- `docs/sql/bestpartner.sql`

**有新增或刪除 `.kt` 檔案** 時，額外標記（目錄樹）：
- `rules/architecture-and-packages.md`（目錄樹區塊）
- `AGENTS.md`（目錄樹快照）
- `README.md`（目錄結構章節）

---

### Step 3：逐一讀取文件，標記狀態

對清單中每個文件，讀取當前內容並對照 diff 實際變更，標記：

- ✅ **不需更新**：內容已反映變更
- ⚠️ **需要更新**：內容已過時或遺漏
- ❓ **需人工判斷**：不確定是否受影響

輸出完整清單，格式範例：
```
⚠️ rules/api-endpoints.md — 新 endpoint POST /llm/xxx 未列入
✅ rules/api-documentation.md — 無需異動
❓ docs-site/docs/features/tools.md — 請確認工具說明是否需補充
```

---

### Step 4：詢問是否更新

輸出清單後詢問：

> 「以上標記 ⚠️ 的文件需要更新。要我逐一更新嗎？」

---

### Step 5：逐一更新並確認（若使用者同意）

- 每個文件更新完後確認一次
- 全部完成後輸出更新摘要

## Common Mistakes

| 錯誤 | 修正 |
|------|------|
| 只看 staged 變更 | 同時執行 `git diff HEAD` 和 `git diff --cached` |
| 跳過 `❓` 標記的文件 | 仍須列出讓使用者確認，不可自行略過 |
| 只更新 rules，忽略 docs-site | 兩個目錄都在清單內，缺一不可 |
| 新增 `.kt` 但沒標記目錄樹 | 目錄樹規則為附加規則，適用於任何變更類型 |
