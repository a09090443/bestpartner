# BestPartner 專案指引

## 專案概述

BestPartner 是一個 AI 應用大平台，可動態建立 AI agent 並支援多種 AI 模型，目標類似 Dify 或 Coze 平台。

**當前版本**: 0.1.7-SNAPSHOT

## 模組結構

```
bestpartner/
├── bestpartner-service/          # 主服務模組 (Quarkus)
├── bestpartner-mcp-servers/      # MCP server 範例
│   ├── quarkus-example/          # Quarkus MCP server 範例
│   └── spring-example/           # Spring MCP server 範例
└── docs/                         # 文件資料
    ├── docker/                   # Docker Compose 設定 (chroma, milvus, mysql, ollama)
    ├── postman/                  # Postman Collection
    ├── rag/                      # RAG 測試文件
    ├── sample/                   # Text2SQL 範例
    └── sql/                      # 資料庫 Schema (bestpartner.sql)
```

## 技術堆疊

| 類別 | 技術 |
|------|------|
| 語言 | Kotlin 2.1.0 |
| 框架 | Quarkus 3.18.4 |
| AI 函式庫 | Langchain4j 1.0.0-beta3 |
| JDK | OpenJDK 21 |
| 資料庫 | PostgreSQL |
| 建置工具 | Gradle |

## 套件命名

主要程式碼位於 `tw.zipe.bastpartner`（注意：是 `bastpartner`，非 `basepartner`）

## 支援的 AI 平台

- OpenAI
- Ollama
- Anthropic
- Gemini
- Grok

## 向量資料庫

- Chroma
- Milvus
- InMemoryEmbeddingStore（預設）

## 內建工具

| 工具名稱 | 類型 |
|----------|------|
| Google 搜尋引擎 | Web Search |
| Tavily 搜尋引擎 | Web Search |
| Date 日期 | Date |
| Text2SQL | Other |

## 建置與執行

### 建置

```bash
# 切換至 bestpartner-service 目錄
cd bestpartner-service

# 建置 uber-jar（指定 profile: dev / sit / prod）
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=${profile}
```

產生的 jar：`bestpartner-service/build/bestpartner-service-0.1.7-SNAPSHOT-runner.jar`

### 執行

```bash
java -jar bestpartner-service-0.1.7-SNAPSHOT-runner.jar
```

服務預設埠：**port 80**

## 專案設定檔

設定檔位於 `bestpartner-service/src/main/resources/application.properties`，根據編譯環境（profile）調整設定項目：

| Profile | 說明 | 主要差異 |
|---------|------|----------|
| dev（預設） | 本地開發環境 | log level = DEBUG，包含 MCP server log 與上傳檔案配置 |
| sit | 測試環境 | log level = DEBUG，含 Flyway 設定 |
| prod | 生產環境 | log level = INFO，含 Flyway 設定 |

編譯時透過 `-Dquarkus.profile=${profile}` 指定對應環境的設定。

### 主要設定項目

| 設定項 | 預設值 | 說明 |
|--------|--------|------|
| `quarkus.http.port` | `80` | HTTP 服務埠 |
| `quarkus.datasource.jdbc.url` | `jdbc:postgresql://localhost:5432/pgdb` | 資料庫連線 |
| `quarkus.datasource.username` | `pguser` | 資料庫帳號 |
| `quarkus.datasource.password` | `pgpass` | 資料庫密碼 |
| `quarkus.datasource.jdbc.min-size` | `5` | 連線池最小連線數 |
| `quarkus.datasource.jdbc.max-size` | `15` | 連線池最大連線數 |
| `quarkus.log.file.path` | `D:/tmp/bestpartner/bestpartner.log` | 一般日誌路徑 |
| `quarkus.log.handler.file."ERROR_LOG".path` | `D:/tmp/bestpartner/bestpartner_error.log` | 錯誤日誌路徑（獨立檔案） |
| `mp.jwt.verify.issuer` | `bast-partner` | JWT 簽發者 |
| `mp.jwt.verify.publickey.location` | `publicKey.pem` | JWT 公鑰位置 |
| `smallrye.jwt.sign.key.location` | `privateKey.pem` | JWT 私鑰位置 |
| `jwt.refresh.switch` | `true` | JWT 自動刷新開關 |
| `mcp.server.log.enable` | `true` | MCP server 日誌開關（僅 dev） |
| `file.upload.dir` | `D:/tmp/bestpartner/upload` | 上傳檔案目錄（僅 dev） |

### Profile 差異說明

編譯時根據 profile 調整 `application.properties` 中的設定：

- **dev**：無 Flyway 設定，包含 `mcp.server.log.enable` 與 `file.upload.dir` 配置，log level = DEBUG
- **sit**：包含 Flyway 設定（`migrate-at-start=false`，手動執行），log level = DEBUG
- **prod**：包含 Flyway 設定（`migrate-at-start=false`，手動執行），log level = INFO

## 資料庫設定

- DB: PostgreSQL，預設連線 `localhost:5432/pgdb`
- 帳號：`pguser` / 密碼：`pgpass`
- 初始化：執行 `docs/sql/bestpartner.sql`
- 無 Flyway 自動遷移（`migrate-at-start=false`），須手動執行 SQL

## 認證

- 使用 JWT（SmallRye JWT）
- 公私鑰位於 `src/main/resources/`（`publicKey.pem` / `privateKey.pem`）
- 登入取得 token：`POST /login`，預設管理員帳號 `admin/admin`
- RBAC 權限管理已啟用

## 原始碼架構（bestpartner-service）

基底路徑：`bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/`

### 各 package 說明與檔案放置規則

| Package | 完整路徑 | 說明 | 放置原則 |
|---------|----------|------|----------|
| `resource` | `.../resource/` | REST API endpoints | 新增 API 控制器放這裡，以 `@Path` 標註，命名為 `XxxResource.kt` |
| `service` | `.../service/` | 業務邏輯層 | 業務邏輯封裝，以 `@ApplicationScoped` 標註，命名為 `XxxService.kt` |
| `entity` | `.../entity/` | JPA 實體（對應資料庫表格）| 新增資料庫對應實體放這裡，繼承 `BaseEntity`，以 `@Entity` 標註，命名為 `XxxEntity.kt` |
| `repository` | `.../repository/` | 資料存取層 | 資料庫查詢操作，繼承 `BaseRepository`，以 `@ApplicationScoped` 標註，命名為 `XxxRepository.kt` |
| `dto` | `.../dto/` | 資料傳輸物件（Request / Response）| API 輸入輸出的資料結構，命名為 `XxxDTO.kt` |
| `form` | `.../form/` | 表單請求物件 | HTTP multipart/form-data 請求物件 |
| `model` | `.../model/` | 內部資料模型 | 非實體、非 DTO 的內部資料結構 |
| `enumerate` | `.../enumerate/` | 列舉型別 | 專案共用的 enum class，命名為 `XxxType.kt` 或 `Xxx.kt` |
| `exception` | `.../exception/` | 例外類別與例外處理器 | 自訂例外（`XxxException.kt`）與 ExceptionMapper |
| `filter` | `.../filter/` | HTTP 過濾器 | JWT 驗證等請求攔截邏輯 |
| `converter` | `.../converter/` | 資料轉換器 | Entity ↔ DTO 轉換邏輯 |
| `properties` | `.../properties/` | 外部設定屬性類別 | 對應 application.properties 的設定 bean |
| `provider` | `.../provider/` | CDI Provider | 動態提供 LLM model 或向量資料庫實例 |
| `constant` | `.../constant/` | 常數定義 | 全域常數，命名為 `XxxConstant.kt` |
| `assistant` | `.../assistant/` | AI Assistant 介面 | Langchain4j AI Service 介面定義 |
| `builder` | `.../builder/` | 動態建構器 | LLM model 與向量資料庫的動態實例化邏輯 |
| `config` | `.../config/` | Quarkus 設定類別 | chatmodel / embedding / vector / security 設定 |
| `tool` | `.../tool/` | LLM 工具實作 | 實作 `@Tool` 的工具類別（Google、Tavily、Date、Text2SQL）|
| `util` | `.../util/` | 通用工具函式 | 無狀態的 helper 函式，命名為 `XxxUtil.kt` 或 `XxxUtils.kt` |

### 目錄樹

```
bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/
├── assistant/
├── builder/
│   ├── llm/        # AnthropicModelBuilder, GeminiModelBuilder, GrokModelBuilder, OllamaModelBuilder, OpenaiModelBuilder
│   └── vector/     # ChromaBuilder, MilvusBuilder
├── config/
│   ├── chatmodel/
│   ├── embedding/
│   ├── security/
│   └── vector/
├── constant/       # LLMConstant
├── converter/
├── dto/            # ApiResponse, ChatRequestDTO, LLMDTO, McpDTO, ToolDTO ...
├── entity/         # BaseEntity, LLMSettingEntity, LLMPlatformEntity, LLMToolEntity ...
├── enumerate/      # Platform, ModelType, VectorStore, ToolsType ...
├── exception/      # LLMException, ServiceException, GlobalExceptionMapper ...
├── filter/         # JwtFilter
├── form/
├── model/
│   └── tool/
├── properties/     # OllamaProp, OpenaiProp, ChromaProp, MilvusProp ...
├── provider/       # ModelProvider, VectorStoreProvider
├── repository/     # LLMUserRepository, VectorStoreSettingRepository ...
├── resource/       # LLMResource, LLMSettingResource, LLMToolResource, LoginResource ...
├── service/        # LLMService, EmbeddingService, ToolService, McpServerService ...
├── tool/
│   ├── config/     # Google, Tavily
│   └── text2sql/
└── util/           # ChatModelBuilder, LLMBuilder, CryptoUtils, OkHttpUtil ...
```

## 測試

API 測試請使用 Postman Collection：
`docs/postman/basepartner.postman_collection.json`

執行測試時跳過（建置時使用 `-x test`）。

## MCP Server 範例

`bestpartner-mcp-servers` 提供兩種 MCP server 實作範例：
- **quarkus-example**：以 Quarkus 實作，執行 `./gradlew quarkusDev`
- **spring-example**：以 Spring Boot 實作

## 開發注意事項

- 日誌檔案輸出至 `D:/tmp/bestpartner/`（Windows 環境預設路徑，部署前請修改）
- 上傳檔案目錄：`D:/tmp/bestpartner/upload`
- MCP server log 可透過 `mcp.server.log.enable` 開關控制
- JWT refresh 可透過 `jwt.refresh.switch` 控制
