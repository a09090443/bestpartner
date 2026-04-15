# 文件更新規範

當程式碼發生以下變更時，**必須同步更新對應文件**。

## 完成前驗證（強制）

**完成任何套件異動、API 新增/修改、資料表變更後，宣告完成前必須執行 `superpowers:verification-before-completion` skill。**

驗證要點：逐一對照下方對應章節的「必須更新的文件」清單，確認每個文件已實際修改，而非假設已更新。

---

---

## 1. 套件（Package）有異動時

適用情境：新增、移除、改名任何 package 或 import 路徑。

| 必須更新的文件 | 更新內容 |
|--------------|---------|
| `rules/architecture-and-packages.md` | 更新 package 說明表格與目錄樹 |
| `rules/naming-conventions.md` | 如命名慣例改變，同步更新命名規則表格 |
| `rules/tech-stack-and-versions.md` | 如依賴函式庫版本異動（`build.gradle.kts`），更新版本號 |
| `CLAUDE.md`（目錄樹區塊） | 如有目錄樹快照，同步更新 |

---

## 2. 新增或修改 API 時

適用情境：新增、移除、改名任何 REST endpoint，或變更 HTTP 方法、路徑、請求/回應結構。

| 必須更新的文件 | 更新內容 |
|--------------|---------|
| `rules/api-endpoints.md` | 新增、修改或刪除對應 endpoint 的說明列（路徑、HTTP 方法、功能、權限） |
| `rules/api-testing.md` | 更新測試模組表格（路徑前綴），新增對應測試案例 |
| `docs/api-test-plan.md` | 新增或修改對應模組的測試項目（含 P0/P1/P2 優先順序） |
| `docs/postman/basepartner.postman_collection.json` | 新增或修改對應的 Postman request |
| Swagger 自動產生 | 確認 `@Path`、`@Operation`、`@APIResponse` 等標註正確反映變更 |

> 若 API 涉及 JWT 保護（`@RolesAllowed`），需確認 Swagger Bearer 認證設定正確。

---

## 3. 資料表或欄位有異動時

適用情境：新增資料表、移除資料表、改名資料表或欄位、新增/移除欄位、變更欄位型別或約束。

| 必須更新的文件 | 更新內容 |
|--------------|---------|
| `docs/sql/bestpartner.sql` | 更新對應的 `CREATE TABLE` 或 `ALTER TABLE` 陳述式 |
| 對應 `XxxEntity.kt` | 更新 `@Column`、`@Table` 等 JPA 標註 |
| 對應 `XxxRepository.kt` | 如有 JPQL/原生 SQL 查詢，更新欄位名稱 |
| 對應 `XxxDTO.kt` | 如回應欄位有異動，同步更新 DTO 結構 |
| sit / prod Flyway | 在 `resources/db/migration/` 建立新的 `V{版本}__描述.sql` migration 腳本 |

> dev 環境無 Flyway，直接修改 `bestpartner.sql` 並手動重建資料庫即可。
> sit / prod 須透過 Flyway migration 腳本（`migrate-at-start=false`，手動執行）。
