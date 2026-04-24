# 文件更新規範

**完成以下任何異動後，宣告完成前必須呼叫 `documentation-sync` skill。**

## 觸發條件

| 異動類型 | 觸發條件範例 |
|---------|------------|
| API 新增 / 修改 | 新增 `@Path`、修改 endpoint 路徑或參數 |
| 資料表 / 欄位變更 | 修改 `entity/`、更新 `bestpartner-ddl.sql` |
| 套件 / 模組異動 | 新增或刪除 `.kt` 檔案、搬移 package |
| 設定項異動 | 新增或修改 `application.properties` 設定鍵值 |

## 執行方式

呼叫 `documentation-sync` skill，該 skill 會自動讀取 git diff 判斷影響範圍，掃描 `docs-site/`、`.claude/rules/`、`README.md` 等相關文件，列出需要更新的清單並詢問是否一起更新。
