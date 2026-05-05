# 文件更新規範

> ⚠️ **強制規定：完成以下任何異動後，宣告完成前必須呼叫 `documentation-sync` skill。**
> 未執行即宣告完成視同違規，不可接受「稍後再更新」。

## 觸發條件

| 異動類型 | 觸發條件範例 |
|---------|------------|
| API 新增 / 修改 | 新增 `@Path`、修改 endpoint 路徑或參數 |
| 資料表 / 欄位變更 | 修改 `entity/`、更新 `bestpartner-ddl.sql` |
| 套件 / 模組異動 | 新增或刪除 `.kt` 檔案、搬移 package |
| 設定項異動 | 新增或修改 `application.properties` 設定鍵值 |

## 必須更新的文件對照表

`documentation-sync` skill 執行時，**必須掃描以下對應文件**，不得只更新其中一部分：

### 資料表 / 欄位變更

| 文件 | 說明 |
|------|------|
| `docs/sql/bestpartner-ddl.sql` | DDL 定義本身 |
| `docs-site/docs/features/ai-models.md` | `llm_setting` / `llm_platform` 資料表結構說明與 INSERT 範例 |
| `docs-site/docs/getting-started/installation.md` | 初始化 SQL 說明 |
| `docs-site/docs/architecture/overview.md` | 若有資料表架構圖或說明 |
| `.claude/rules/architecture-and-packages.md` | 若有 entity 結構說明 |

### API 新增 / 修改

| 文件 | 說明 |
|------|------|
| `.claude/rules/api-endpoints.md` | endpoint 清單與說明 |
| `docs-site/docs/api/` 對應頁面 | assistant.md / llm-setting.md / knowledge-base.md 等 |
| `docs-site/docs/features/` 對應頁面 | ai-models.md / mcp-servers.md / tools.md 等 |
| `docs/api-test-plan.md` | 測試計畫 |
| `docs/postman/basepartner.postman_collection.json` | Postman Collection |

### 套件 / 模組異動

| 文件 | 說明 |
|------|------|
| `.claude/rules/architecture-and-packages.md` | 目錄樹與 package 說明 |
| `docs-site/docs/architecture/modules.md` | 模組說明頁 |
| `README.md` | 目錄結構章節 |

## 執行方式

呼叫 `documentation-sync` skill，該 skill 會自動讀取 git diff 判斷影響範圍，掃描 `docs-site/`、`.claude/rules/`、`README.md` 等相關文件，列出需要更新的清單並詢問是否一起更新。
