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

Kotlin 2.1.0 · Quarkus 3.21.0 · Langchain4j 1.17.2 · JDK 21 · PostgreSQL · Gradle。
完整版本與支援平台 → [`rules/tech-stack-and-versions.md`](rules/tech-stack-and-versions.md)

## ⚠️ 關鍵雷區（最常踩錯）

- **套件名是 `tw.zipe.bastpartner`，不是 `basepartner`**（少一個 `e`）。寫 import、新增檔案前務必確認。
- **版本號禁止硬編碼在 `build.gradle.kts`**，一律定義於 `gradle.properties` 並以 `$xxxVersion` 引用（plugins 區塊例外）。→ [`gradle-conventions.md`](rules/gradle-conventions.md)
- **業務訊息禁止寫死字串**，一律走 `AppMessage` enum + i18n properties（en_US 為預設語言）。→ [`i18n-messages.md`](rules/i18n-messages.md)
- **無 Flyway 自動遷移**，schema 變更須手動跑 `docs/sql/` 的 SQL。
- **跨環境會變動的設定一律寫成 `${ENV_VAR:預設值}`**（預設值＝本機 Windows 開發值），機密與連線位址不寫死；各環境以 `.env.<profile>` 覆寫，範本為根目錄 `.env.example`。→ [`configuration-and-profiles.md`](rules/configuration-and-profiles.md)
- **不可對 build-time 設定加 `%profile.` 前綴**（image 一律以 prod 建置，會被求值成關閉並烤進 image，連 dev/sit 都失效）。已知 build-time：`swagger-ui.always-include`、`hibernate-orm.log.sql`、`hibernate-orm.log.bind-parameters`。一律「build-time 全域開能力、runtime 控實際輸出」（`swagger-ui.enable`、`log.category."...".level`）。→ [`configuration-and-profiles.md`](rules/configuration-and-profiles.md)
- 服務預設 **port 80**；敏感欄位用 `crypto.secret-key` AES-GCM 加密（預設值僅供本機開發，sit 以上務必以 `CRYPTO_SECRET_KEY` 替換）。
- **設定值與資料欄位是兩把不同的金鑰，別混用**：`CRYPTO_SECRET_KEY` 加密資料庫欄位；`CONFIG_ENCRYPTION_KEY` 解密 `.env.<profile>` 中寫成 `${enc::<密文>}` 的設定值（`./gradlew encryptConfigSecret` 產生）。`application.properties` 的 datasource 預設值亦為密文（`${DB_USERNAME:${enc::<密文>}}`，以**檔內預設金鑰**加密，僅為讓版控不出現 `pgpass` 字樣的衛生措施，非安全措施）——**改動 `CONFIG_ENCRYPTION_KEY` 預設值時必須同步重新產生該密文**，否則全新 clone 會啟動失敗。→ [`configuration-and-profiles.md`](rules/configuration-and-profiles.md)
- **`CONFIG_ENCRYPTION_KEY` 與密文同放 `.env.<profile>`**（刻意取捨，換取部署簡單）：故此加密防的是明文被瞥見／截圖／誤貼／進日誌，**不防 `.env` 檔案本身外洩**（拿到檔即可解密）——別在文件或對話中把它描述成更強的保護。目前所有 env 檔的真實機密皆為密文（各檔金鑰不同）：`.env` 的 `OPENROUTER_API_KEY`／`DB_USERNAME`／`DB_PASSWORD`、`.env.docker`／`.env.sit`／`.env.uat` 的 `DB_PASSWORD`；未填值的佔位符維持明文。金鑰錯誤會以 `AEADBadTagException` 啟動失敗。加解密與金鑰輪替一律走 `config-secret` skill。

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
# profile: dev(預設) / docker / sit / uat / prod
java -jar build/bestpartner-service-0.1.8-SNAPSHOT-runner.jar
```

**Docker 一份 image 跑所有環境**：固定以 `-Dquarkus.profile=prod` 建置（建置 profile 會成為 image 預設 runtime profile，用 prod 才能讓漏帶 `QUARKUS_PROFILE` 時落在「Swagger 關閉、log INFO」的安全側）；環境差異靠 runtime `-e QUARKUS_PROFILE=<profile>` ＋ `--env-file .env.<profile>` 決定，不為個別環境重建 image。存活探測用 `/view/chat`（`/q/openapi`、`/swagger-ui` 在 prod 回 404 是正確行為）。

設定檔與 profile 差異 → [`configuration-and-profiles.md`](rules/configuration-and-profiles.md)、[`build-and-run.md`](rules/build-and-run.md)

## API

13 模組、68 個 endpoint，清單 → [`api-endpoints.md`](rules/api-endpoints.md)。
Swagger UI 與 OpenAPI spec 僅 dev/docker/sit 開放（`/swagger-ui`、`/q/openapi`），uat/prod 與未指定 profile 皆回 404 → [`api-documentation.md`](rules/api-documentation.md)。
JWT / RBAC（預設 `admin`/`admin`）→ [`authentication-and-security.md`](rules/authentication-and-security.md)。

## 完整規則索引（`rules/`）

| 規則檔案 | 說明 |
|---------|------|
| [tech-stack-and-versions.md](rules/tech-stack-and-versions.md) | 技術堆疊版本、支援 AI 平台、向量資料庫、內建工具 |
| [architecture-and-packages.md](rules/architecture-and-packages.md) | 專案模組結構、各 package 職責、完整目錄樹 |
| [naming-conventions.md](rules/naming-conventions.md) | 套件命名（`bastpartner` 注意）、各層檔案命名規則 |
| [build-and-run.md](rules/build-and-run.md) | Gradle 建置指令、uber-jar 生成、執行方式 |
| [gradle-conventions.md](rules/gradle-conventions.md) | Gradle 版本管理、gradle.properties、build.gradle.kts 規範 |
| [configuration-and-profiles.md](rules/configuration-and-profiles.md) | 環境變數對照表、profile 差異（dev/docker/sit/uat/prod）、build-time vs runtime 設定、容器路徑、資料庫設定 |
| [authentication-and-security.md](rules/authentication-and-security.md) | JWT 認證、公私鑰、登入端點、RBAC |
| [api-documentation.md](rules/api-documentation.md) | Swagger UI 路徑、OpenAPI spec、Bearer 認證整合 |
| [api-endpoints.md](rules/api-endpoints.md) | 各模組 API endpoint 清單與功能說明 |
| [api-testing.md](rules/api-testing.md) | Postman Collection、API 測試計畫、P0/P1/P2 定義 |
| [development-notes.md](rules/development-notes.md) | MCP Server 範例、日誌路徑、開關控制 |
| [i18n-messages.md](rules/i18n-messages.md) | i18n 訊息管理、AppMessage enum、MessageUtil 使用方式 |
| [documentation-update-policy.md](rules/documentation-update-policy.md) | 套件／API／欄位變更時必須更新的文件清單 |
| [frontend-conventions.md](rules/frontend-conventions.md) | 前端（bestpartner-ui）技術棧、目錄命名、元件/API/狀態管理慣例 |

## 強制工作流程（skill，違反視同違規）

> 註：以下 skill 為 Claude Code 專屬；其他 CLI 讀到僅供參考，規範本體請見連結的 rules 文件。

| 何時 | 必須呼叫的 skill |
|------|-----------------|
| 改動 API / entity / 資料表 / 設定鍵 / UI / workflow 節點型別 / 執行事件後，宣告完成前 | `documentation-sync`（含測試文件同步檢視：e2e-test-plan / api-test-plan，見 policy「測試文件同步」）→ [`documentation-update-policy.md`](rules/documentation-update-policy.md) |
| 執行任何 API 測試前 | `test-confirmation` → [`api-testing.md`](rules/api-testing.md) |
| 寫 git commit 訊息 | `git-commit-message`（`<類型>(<範圍>): <主旨>`，繁中主旨） |
| 建立 / 匯出 Docker image | `docker-build`（固定 prod profile 建置 → 啟動驗證 → 自動匯出 tar；tar 內含 JWT 私鑰，勿隨意流通） |
| 加密 / 解密設定值（`${enc::}`）、輪替 `CONFIG_ENCRYPTION_KEY` | `config-secret`（勿與資料庫欄位金鑰 `CRYPTO_SECRET_KEY` 混用） |

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

`scripts/harness-drift-scan` 逐項對照黃金規範掃描跨模組漂移，輸出「現況 vs 規範 vs 建議」表，區分歷史基線與新漂移，發現新漂移時 exit 1（可作 CI 把關）：

```bash
./scripts/harness-drift-scan.sh          # Linux / macOS
pwsh ./scripts/harness-drift-scan.ps1    # Windows
```

> 所有可執行腳本集中於 repo 根目錄的 `scripts/`，各腳本用途見 `scripts/README.md`。
> ⚠️ 每支腳本有 `.sh`（Linux/macOS 原生）與 `.ps1`（Windows）**兩份實作，改動時兩份都要改**；
> CI 會同時執行兩者並比對結論，不一致即擋下。

涵蓋：套件拼字（`bastpartner`）、i18n 寫死字串、`build.gradle.kts` 版本硬編碼、`@Entity` 命名後綴、service/repository 的 `@ApplicationScoped`、SQL 金鑰外洩、**AGENTS.md 與 `.claude/CLAUDE.md` 一致性**、**Workflow NodeType 前後端契約一致**（`NodeType.kt` ≡ `bestpartner-ui` 的 `types/workflow.ts`）、**前端測試命名**（新測試一律 `.test.ts`）。既有基線（如 `jsqlparser` 硬編碼版本、`LLMResource.kt` 寫死例外訊息、3 個既存 `.spec.ts`）已登錄於腳本 `$Baseline`，新增程式碼若擴大漂移會被標為 `NEW`。

## CI 把關

`.github/workflows/harness.yml` 在 push 與對 `master` 的 PR 上**必跑** ArchUnit 測試、漂移掃描，以及前端型別檢查（`vue-tsc`）＋單元測試（`vitest`）（任一失敗即擋下）。修改結構、命名、依賴版本、業務訊息或前端程式碼後，先在本機跑過對應項目再推送，避免 CI 紅燈。
