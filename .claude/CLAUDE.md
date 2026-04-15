# BestPartner 專案指引

## 專案概述

BestPartner 是一個 AI 應用大平台，可動態建立 AI agent 並支援多種 AI 模型，目標類似 Dify 或 Coze 平台。

**當前版本**: 0.1.7-SNAPSHOT

## 規則檔案索引

詳細規則已拆解至 `.claude/rules/` 目錄，依主題查閱：

| 規則檔案 | 說明 |
|---------|------|
| [tech-stack-and-versions.md](rules/tech-stack-and-versions.md) | 技術堆疊版本、支援 AI 平台、向量資料庫、內建工具 |
| [architecture-and-packages.md](rules/architecture-and-packages.md) | 專案模組結構、各 package 職責說明、完整目錄樹 |
| [naming-conventions.md](rules/naming-conventions.md) | 套件命名（bastpartner 注意）、各層檔案命名規則 |
| [build-and-run.md](rules/build-and-run.md) | Gradle 建置指令、uber-jar 生成、執行方式 |
| [gradle-conventions.md](rules/gradle-conventions.md) | Gradle 版本管理、gradle.properties、build.gradle.kts 規範 |
| [configuration-and-profiles.md](rules/configuration-and-profiles.md) | application.properties 設定項、dev/sit/prod 差異、資料庫設定 |
| [authentication-and-security.md](rules/authentication-and-security.md) | JWT 認證、公私鑰、登入端點、RBAC |
| [api-documentation.md](rules/api-documentation.md) | Swagger UI 路徑、OpenAPI spec、Bearer 認證整合 |
| [api-endpoints.md](rules/api-endpoints.md) | 各模組 API endpoint 清單與功能說明（共 11 模組、49 個端點） |
| [api-testing.md](rules/api-testing.md) | Postman Collection、API 測試計畫、P0/P1/P2 定義 |
| [development-notes.md](rules/development-notes.md) | MCP Server 範例、日誌路徑、開關控制 |
| [documentation-update-policy.md](rules/documentation-update-policy.md) | 套件異動、API 變更、資料表欄位變更時必須更新的文件清單 |

## 快速建置

```bash
# 切換至 bestpartner-service 目錄
cd bestpartner-service

# 建置 uber-jar（指定 profile: dev / sit / prod）
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=${profile}
```

產生的 jar：`bestpartner-service/build/bestpartner-service-0.1.7-SNAPSHOT-runner.jar`

```bash
java -jar bestpartner-service-0.1.7-SNAPSHOT-runner.jar
```

服務預設埠：**port 80**
