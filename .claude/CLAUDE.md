# AGENTS.md — BestPartner Agent 導覽地圖

> 本檔是給 AI agent 的**導覽地圖**，不是百科。細節一律連回 `rules/` 與 `docs-site/`，此處只負責「往哪查、別踩什麼雷」。
> 規範以 `rules/` 為單一事實來源（source of truth）；本檔若與其衝突，以 rules 為準。
>
> **⚠️ 本檔與另一份導覽文件（`AGENTS.md` ⇄ `.claude/CLAUDE.md`）內容必須逐字一致**，僅連結前綴因所在目錄不同（根目錄用 `.claude/rules/`、`.claude/` 內用 `rules/`）。兩者供不同 AI CLI 讀取，改動時**兩份都要改**；漂移掃描會正規化前綴後逐位元組比對，不一致即擋下 CI。

## 這是什麼專案

AI 應用大平台（類似 Dify / Coze），可動態建立 AI agent、支援多種 AI 模型。當前版本 **0.1.8-SNAPSHOT**。

## 模組總覽

| 路徑 | 說明 |
|------|------|
| `bestpartner-service/` | 主服務（Quarkus + Kotlin），所有業務程式碼在此 |
| `bestpartner-mcp-servers/` | MCP server 範例（quarkus-example / spring-example） |
| `docs/` | SQL schema、Postman、測試計畫、Docker compose |
| `docs-site/` | Docusaurus 文件網站（`cd docs-site && npm start`，port 3000） |

## 技術棧（變更前先讀 rules）

Kotlin 2.1.0 · Quarkus 3.21.0 · Langchain4j 1.13.0 · JDK 21 · PostgreSQL · Gradle。
完整版本與支援平台 → [`rules/tech-stack-and-versions.md`](rules/tech-stack-and-versions.md)

## ⚠️ 關鍵雷區（最常踩錯）

- **套件名是 `tw.zipe.bastpartner`，不是 `basepartner`**（少一個 `e`）。寫 import、新增檔案前務必確認。
- **版本號禁止硬編碼在 `build.gradle.kts`**，一律定義於 `gradle.properties` 並以 `$xxxVersion` 引用（plugins 區塊例外）。→ [`gradle-conventions.md`](rules/gradle-conventions.md)
- **業務訊息禁止寫死字串**，一律走 `AppMessage` enum + i18n properties（en_US 為預設語言）。→ [`i18n-messages.md`](rules/i18n-messages.md)
- **無 Flyway 自動遷移**，schema 變更須手動跑 `docs/sql/` 的 SQL。
- 服務預設 **port 80**；敏感欄位用 `crypto.secret-key` AES-GCM 加密。

## 程式碼放哪（分層 + 命名）

依賴方向：`resource → service → repository → entity`。各層命名與放置原則：

| 層 | 命名 | CDI 標註 |
|----|------|---------|
| REST 控制器 | `XxxResource.kt` | `@Path` |
| 業務邏輯 | `XxxService.kt` | `@ApplicationScoped` |
| 資料存取 | `XxxRepository.kt` | 繼承 `BaseRepository`、`@ApplicationScoped` |
| JPA 實體 | `XxxEntity.kt` | 繼承 `BaseEntity`、`@Entity` |
| 傳輸物件 | `XxxDTO.kt` | — |

完整 19 個 package 職責與目錄樹 → [`architecture-and-packages.md`](rules/architecture-and-packages.md)、命名規則 → [`naming-conventions.md`](rules/naming-conventions.md)

> **已知漂移基線（勿在此次清理）**：少數 entity 不帶 `Entity` 後綴（`LLMRolePermissionId`、`LLMUserRoleId` 為嵌入式複合主鍵；`LLMMcpUserSetting`）。新增 entity 一律遵循 `XxxEntity.kt`。

## 建置與執行

```bash
cd bestpartner-service
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=${profile}
# profile: dev(預設) / sit / prod
java -jar build/bestpartner-service-0.1.8-SNAPSHOT-runner.jar
```

設定檔與 profile 差異 → [`configuration-and-profiles.md`](rules/configuration-and-profiles.md)、[`build-and-run.md`](rules/build-and-run.md)

## API

13 模組、68 個 endpoint，清單 → [`api-endpoints.md`](rules/api-endpoints.md)。
Swagger UI 僅 dev/sit 開放（`/swagger-ui`）→ [`api-documentation.md`](rules/api-documentation.md)。
JWT / RBAC（預設 `admin`/`admin`）→ [`authentication-and-security.md`](rules/authentication-and-security.md)。

## 完整規則索引（`rules/`）

| 規則檔案 | 說明 |
|---------|------|
| [tech-stack-and-versions.md](rules/tech-stack-and-versions.md) | 技術堆疊版本、支援 AI 平台、向量資料庫、內建工具 |
| [architecture-and-packages.md](rules/architecture-and-packages.md) | 專案模組結構、各 package 職責、完整目錄樹 |
| [naming-conventions.md](rules/naming-conventions.md) | 套件命名（`bastpartner` 注意）、各層檔案命名規則 |
| [build-and-run.md](rules/build-and-run.md) | Gradle 建置指令、uber-jar 生成、執行方式 |
| [gradle-conventions.md](rules/gradle-conventions.md) | Gradle 版本管理、gradle.properties、build.gradle.kts 規範 |
| [configuration-and-profiles.md](rules/configuration-and-profiles.md) | application.properties 設定項、dev/sit/prod 差異、資料庫設定 |
| [authentication-and-security.md](rules/authentication-and-security.md) | JWT 認證、公私鑰、登入端點、RBAC |
| [api-documentation.md](rules/api-documentation.md) | Swagger UI 路徑、OpenAPI spec、Bearer 認證整合 |
| [api-endpoints.md](rules/api-endpoints.md) | 各模組 API endpoint 清單與功能說明 |
| [api-testing.md](rules/api-testing.md) | Postman Collection、API 測試計畫、P0/P1/P2 定義 |
| [development-notes.md](rules/development-notes.md) | MCP Server 範例、日誌路徑、開關控制 |
| [i18n-messages.md](rules/i18n-messages.md) | i18n 訊息管理、AppMessage enum、MessageUtil 使用方式 |
| [documentation-update-policy.md](rules/documentation-update-policy.md) | 套件／API／欄位變更時必須更新的文件清單 |

## 強制工作流程（skill，違反視同違規）

> 註：以下 skill 為 Claude Code 專屬；其他 CLI 讀到僅供參考，規範本體請見連結的 rules 文件。

| 何時 | 必須呼叫的 skill |
|------|-----------------|
| 改動 API / entity / 資料表 / 設定鍵 / UI / workflow 節點型別 / 執行事件後，宣告完成前 | `documentation-sync`（含測試文件同步檢視：e2e-test-plan / api-test-plan，見 policy「測試文件同步」）→ [`documentation-update-policy.md`](rules/documentation-update-policy.md) |
| 執行任何 API 測試前 | `test-confirmation` → [`api-testing.md`](rules/api-testing.md) |
| 寫 git commit 訊息 | `git-commit-message`（`<類型>(<範圍>): <主旨>`，繁中主旨） |

## SQL 資料備份鐵則

備份資料表資料**只能**寫入 `docs/sql/bestpartner-init-data.sql`（`BEGIN`/`COMMIT` 包裹），不得另建備份檔／目錄；`api_key` 等敏感欄位必須掩蔽（如 `sk-or-v1-xxx`）。→ [`documentation-update-policy.md`](rules/documentation-update-policy.md)

## 機械化強制（ArchUnit）

結構慣例由 `bestpartner-service/src/test/kotlin/.../architecture/ArchitectureTest.kt` 自動驗證（ArchUnit，純 bytecode 分析、非 @QuarkusTest）。目前 9 條不變量：

- 分層依賴方向：`service`/`repository`/`entity` 不可反向依賴上層
- 命名：`@Path`→`*Resource`、`repository` 套件→`*Repository`、`service` 套件→`*Service`、`workflow.executor` 套件→`*Executor`
- 位置：`@Entity` 一律放在 `entity` 套件
- 繼承：`repository` 套件類別一律繼承 `BaseRepository`

執行：
```bash
cd bestpartner-service
./gradlew test --tests "tw.zipe.bastpartner.architecture.ArchitectureTest"
```

> 規則皆「先通過現狀」設計，刻意容納既有漂移基線（如 `LLMMcpUserSetting` 帶 `@Entity` 但無後綴）；新增程式碼須遵循。
> ⚠️ 一般建置用 `-x test` 會跳過此驗證，但 CI 會單獨必跑（見下）。其他測試框架為 QuarkusTest + JUnit5 + rest-assured。

## 漂移掃描（補 ArchUnit 不易檢查的慣例）

`harness-drift-scan.ps1`（repo 根目錄）逐項對照黃金規範掃描跨模組漂移，輸出「現況 vs 規範 vs 建議」表，區分歷史基線與新漂移，發現新漂移時 exit 1（可作 CI 把關）：

```powershell
pwsh ./harness-drift-scan.ps1
```

涵蓋：套件拼字（`bastpartner`）、i18n 寫死字串、`build.gradle.kts` 版本硬編碼、`@Entity` 命名後綴、service/repository 的 `@ApplicationScoped`、SQL 金鑰外洩、**AGENTS.md 與 `.claude/CLAUDE.md` 一致性**。既有基線（如 `jsqlparser` 硬編碼版本、`LLMResource.kt` 寫死例外訊息）已登錄於腳本 `$Baseline`，新增程式碼若擴大漂移會被標為 `NEW`。

## CI 把關

`.github/workflows/harness.yml` 在 push 與對 `master` 的 PR 上**必跑** ArchUnit 測試與漂移掃描（任一失敗即擋下）。修改結構、命名、依賴版本或業務訊息後，先在本機跑過上述兩項再推送，避免 CI 紅燈。
