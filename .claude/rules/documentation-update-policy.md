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
| UI / workflow 節點型別 / 執行事件變更 | 新增或移除節點型別、改連接埠（handle）、改 SSE 執行事件、改 Inspector 表單欄位或 `data-test` 選擇器 |

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

## 測試文件同步（強制）

> ⚠️ **凡涉及「前端 UI / workflow 節點型別 / 連接埠 / 執行事件 / API」的程式變更，宣告完成前必須逐一檢視以下測試文件是否需同步。**
> 需要則更新對應文件（並同步其確認清單模板）；**判定不需調整時，亦須於變更說明中明述「已檢視、無需調整」**，不可略過此步驟。

| 文件 | 何時需檢視 | 說明 |
|------|-----------|------|
| `docs/e2e-test-plan.md` | UI 旅程 / 節點型別 / 連接埠 / 執行事件 / 表單變更 | 旅程矩陣（§3）、表單清單、節點型別與計數、`data-test` 選擇器（§9 已內建此強制規則） |
| `.claude/skills/e2e-test-confirmation/` | 同上 | E2E 確認清單模板（若存在，須與 `e2e-test-plan.md` 同步） |
| `docs/api-test-plan.md` | API 新增 / 修改 | 各端點測試案例與 P0/P1/P2 定義 |
| `docs/postman/basepartner.postman_collection.json` | API 新增 / 修改 | Postman Collection |

## SQL 資料備份規範

> ⚠️ **強制規定：備份資料表資料時，必須寫入 `docs/sql/bestpartner-init-data.sql`，不得另建備份檔案或目錄。**

- 備份內容以 `INSERT INTO` 語句呈現，並以 `BEGIN` / `COMMIT` 包裹
- 若該資料表的資料已存在於檔案中，更新對應區塊；若尚不存在，在檔案末尾新增
- 不可建立 `docs/sql/backup/` 等子目錄或獨立備份檔
- **api_key 等敏感欄位必須掩蔽**，以 `xxx` 或對應平台的佔位格式取代（如 `sk-or-v1-xxx`、`sk-proj-xxx`）

## 執行方式

呼叫 `documentation-sync` skill，該 skill 會自動讀取 git diff 判斷影響範圍，掃描 `docs-site/`、`.claude/rules/`、`README.md` 等相關文件，列出需要更新的清單並詢問是否一起更新。
