# 架構與 Package 結構

## 專案模組結構

```
bestpartner/
├── bestpartner-service/          # 主服務模組 (Quarkus)
├── bestpartner-mcp-servers/      # MCP server 範例
│   ├── quarkus-example/          # Quarkus MCP server 範例
│   └── spring-example/           # Spring MCP server 範例
├── bestpartner-ui/               # 前端應用 (Vue 3 + Vite + TS)
│   └── src/                      # api / components(canvas,inspector) / composables / constants / stores / styles / views
└── docs/                         # 文件資料
    ├── docker/                   # Docker Compose 設定 (chroma, milvus, mysql, ollama)
    ├── postman/                  # Postman Collection
    ├── rag/                      # RAG 測試文件
    ├── sample/                   # Text2SQL 範例
    └── sql/                      # 資料庫 Schema (bestpartner-ddl.sql, bestpartner-init-data.sql)
```

## 資源檔架構（bestpartner-service）

```
bestpartner-service/src/main/resources/
├── messages/
│   ├── messages_en_US.properties   # 英文訊息（預設語言）
│   └── messages_zh_TW.properties   # 繁體中文訊息
├── application.properties
├── publicKey.pem
└── privateKey.pem
```

> i18n 訊息管理詳見 [i18n-messages.md](i18n-messages.md)

---

## 原始碼架構（bestpartner-service）

基底路徑：`bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/`

### 各 Package 說明與放置規則

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

## 目錄樹

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
├── converter/          # PermissionSetConverter, PasswordEncryptConverter
├── dto/            # ApiResponse, ChatRequestDTO, LLMDTO, McpDTO, SkillDTO, ToolDTO, WorkflowDTO ...
│   └── workflow/
│       └── config/ # NodeConfig（11 種 NodeType 的強型別 config DTO + NodeConfigRegistry，節點 config schema 的事實來源）
├── entity/         # BaseEntity, LLMSettingEntity, LLMPlatformEntity, LLMSkillEntity, LLMSkillResourceEntity, LLMToolEntity, WorkflowEntity, WorkflowNodeEntity, WorkflowEdgeEntity, WorkflowExecutionEntity, WorkflowNodeExecutionEntity ...
├── enumerate/      # Platform, ModelType, VectorStore, ToolsType, AppMessage, WorkflowStatus, NodeType, TriggerType, ExecutionStatus, NodeExecutionStatus ...
├── exception/      # LLMException, ServiceException, GlobalExceptionMapper ...
├── filter/         # JwtFilter
├── form/           # FilesFromRequest, SkillUploadForm
├── model/
│   └── tool/
├── properties/     # OllamaProp, OpenaiProp, ChromaProp, MilvusProp ...
├── provider/       # ModelProvider, VectorStoreProvider
├── repository/     # LLMUserRepository, LLMSkillRepository, LLMSkillResourceRepository, VectorStoreSettingRepository, WorkflowRepository, WorkflowNodeRepository, WorkflowEdgeRepository ...
├── resource/       # LLMResource, LLMSettingResource, LLMSkillResource, LLMToolResource, LoginResource, WorkflowResource ...
├── service/        # LLMService, EmbeddingService, SkillService, ToolService, McpServerService, WorkflowService ...
│   └── workflow/   # WorkflowEngine, ExecutionContext, ExecutionEvent(+ExecutionEventSink), NodeExecutor
│       └── executor/ # TriggerExecutor, OutputExecutor, ConditionExecutor, HttpRequestExecutor, ToolNodeExecutor, McpServerNodeExecutor, KnowledgeRagExecutor, LlmAssistantExecutor（共 8 個）
├── tool/
│   ├── config/     # Google, Tavily, ToolConfigField（settingSchema 的 UI 提示 annotation）
│   └── text2sql/
└── util/           # ChatModelBuilder, LLMBuilder, CryptoUtils, OkHttpUtil, MessageUtil, ToolSchemaGenerator ...
```
