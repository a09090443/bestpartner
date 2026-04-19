---
sidebar_position: 2
---

# 模組說明

## 專案模組結構

```
bestpartner/
├── bestpartner-service/       # 主服務
└── bestpartner-mcp-servers/   # MCP Server 範例
    ├── quarkus-example/       # Quarkus MCP Server 範例
    └── spring-example/        # Spring MCP Server 範例
```

## bestpartner-service 核心 Package

### `resource/` — REST API 端點層

接收 HTTP 請求，進行參數驗證後委派給 Service 層處理。

| 檔案 | 說明 |
|------|------|
| `LLMResource.kt` | AI 對話相關 API |
| `LLMAdminResource.kt` | 管理員操作 API |
| `LLMMcpServerResource.kt` | MCP Server 管理 API |
| `LLMPermissionResource.kt` | 權限管理 API |
| `LLMSettingResource.kt` | 模型設定 API |
| `LLMSkillResource.kt` | Skill 管理 API |
| `LLMToolResource.kt` | 工具管理 API |
| `LLMUserResource.kt` | 使用者管理 API |
| `LLMVectorResource.kt` | 向量知識庫 API |
| `LoginResource.kt` | 登入認證 API |
| `SystemSettingResource.kt` | 系統設定 API |
| `BaseLLMResource.kt` | Resource 基底類別 |
| `ViewResource.kt` | 視圖端點 |

### `service/` — 業務邏輯層

核心業務邏輯所在，協調 AI 模型調用、資料庫操作、工具執行等流程。

| 檔案 | 說明 |
|------|------|
| `LLMService.kt` | AI 模型調用核心邏輯 |
| `EmbeddingService.kt` | 向量嵌入服務 |
| `ToolService.kt` | 工具管理服務 |
| `McpServerService.kt` | MCP Server 整合服務 |
| `SkillService.kt` | Skill 管理服務 |
| `JwtService.kt` | JWT Token 產生與驗證 |
| `LLMPermissionService.kt` | 權限控制服務 |
| `LLMUserService.kt` | 使用者管理服務 |
| `SystemService.kt` | 系統設定服務 |

### `builder/` — AI 元件建構層

依資料庫設定動態建立 AI 相關元件實例。

| 子目錄 | 檔案 | 說明 |
|--------|------|------|
| `builder/llm/` | `OpenaiModelBuilder.kt` | OpenAI 模型建構器 |
| `builder/llm/` | `AnthropicModelBuilder.kt` | Anthropic Claude 模型建構器 |
| `builder/llm/` | `GeminiModelBuilder.kt` | Google Gemini 模型建構器 |
| `builder/llm/` | `OllamaModelBuilder.kt` | Ollama 本地端模型建構器 |
| `builder/llm/` | `GrokModelBuilder.kt` | Grok 模型建構器 |
| `builder/vector/` | `ChromaBuilder.kt` | Chroma 向量資料庫建構器 |
| `builder/vector/` | `MilvusBuilder.kt` | Milvus 向量資料庫建構器 |

### `assistant/` — AI 助手實作

封裝 LangChain4J 的 AI Service 介面。

| 檔案 | 說明 |
|------|------|
| `AIAssistant.kt` | 基本 AI 助手介面 |
| `DynamicAssistant.kt` | 動態配置助手（支援工具、RAG 等） |
| `SqlDatabaseContentRetriever.kt` | Text2SQL 內容擷取器 |

### `config/` — 設定類

| 子目錄 | 說明 |
|--------|------|
| `config/chatmodel/` | Chat 模型設定（OpenAI、Ollama 等） |
| `config/embedding/` | 嵌入模型設定 |
| `config/vector/` | 向量資料庫設定（Chroma、Milvus） |
| `config/security/` | 安全性設定與驗證器 |

### `filter/` — 請求過濾器

`JwtFilter.kt`：JWT Token 驗證過濾器，攔截所有請求進行身份驗證。

### `tool/` — 內建工具

```
tool/
├── DateTool.kt              # 日期時間工具
├── config/
│   ├── Google.kt            # Google 搜尋設定
│   └── Tavily.kt            # Tavily 搜尋設定
└── text2sql/
    ├── Text2SQLTool.kt      # Text2SQL 工具實作
    ├── config/
    │   ├── DataSourceConfig.kt   # 資料來源設定
    │   └── Text2SQL.kt           # Text2SQL 設定
    └── enumerate/
        └── DatabaseType.kt      # 支援的資料庫類型
```

### `entity/` + `repository/` — 資料持久層

JPA Entity 對應資料庫表格，Repository 使用 Hibernate Panache 提供 CRUD 操作。

### `dto/` / `form/` / `model/` — 資料傳輸物件

- `dto/`：API 回應資料物件
- `form/`：API 請求參數物件
- `model/`：業務模型物件

### 其他 Package

| Package | 說明 |
|---------|------|
| `constant/` | 應用程式常數定義 |
| `converter/` | 物件轉換邏輯 |
| `enumerate/` | 列舉類型定義 |
| `exception/` | 自訂例外類別 |
| `properties/` | 設定屬性類別 |
| `provider/` | 服務提供者（工具注入等）|
| `util/` | 通用工具方法 |

## bestpartner-mcp-servers

提供兩種框架的 MCP Server 範例，開發者可參考這些範例建立自己的 MCP Server，再將其接入 BestPartner 平台。

| 範例 | 框架 | 說明 |
|------|------|------|
| `quarkus-example` | Quarkus | 包含 ChineseName 和 Favorite 工具範例 |
| `spring-example` | Spring Boot | 包含 MCP 設定（McpConfiguration）與工具實作 |
