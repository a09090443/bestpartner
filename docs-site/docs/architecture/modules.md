---
sidebar_position: 2
description: BestPartner 專案模組結構說明，涵蓋 bestpartner-service 主服務、bestpartner-mcp-servers 範例與 bestpartner-ui 前端各模組的職責劃分。
keywords: [模組, 專案結構, bestpartner-service, bestpartner-ui, MCP Server]
---

# 模組說明

## 專案模組結構

```
bestpartner/
├── bestpartner-service/       # 主服務 (Quarkus)
├── bestpartner-mcp-servers/   # MCP Server 範例
│   ├── quarkus-example/       # Quarkus MCP Server 範例
│   └── spring-example/        # Spring MCP Server 範例
└── bestpartner-ui/            # 前端應用 (Vue 3 + Vite + TS)
```

## bestpartner-ui 前端模組

基於 Vue 3 + Vite + TypeScript，提供登入、Workflow 列表與 n8n-like 視覺化 Workflow 編輯器。

| 目錄 | 職責 |
|------|------|
| `api/` | 後端 API client（axios 實例與認證攔截器；workflow、llmSetting、tool、mcpServer 端點封裝） |
| `components/canvas/` | Vue Flow 節點面板（NodePalette，Trigger／Action／AI／Logic 分類＋搜尋）與自訂節點（WorkflowNode，依 meta 動態渲染多連接點、深色卡片） |
| `components/inspector/` | 屬性面板（InspectorPanel，含節點複製／刪除與未選取時的 Workflow overview 統計）、型別化節點設定表單（`forms/`）與通用 JSON 設定編輯器（JsonConfigEditor，作為 fallback） |
| `composables/` | nodeKey 產生、畫布↔DTO 雙向轉換、輕量畫布驗證、下拉選項快取（useNodeOptions）、dagre 自動排版（useCanvasLayout） |
| `constants/` | 節點型別顯示 meta（label/color/icon/category 與 inputs/outputs 連接點）、handle 編碼工具 |
| `styles/` | 編輯器深色主題（`workflow-theme.css`，n8n 風格；token 以 `.wf-editor` 作用域界定） |
| `router/` | 路由定義與登入守衛 |
| `stores/` | Pinia 狀態（auth、workflow） |
| `types/` | 對應後端 DTO 的 TypeScript 型別、下拉 Option 型別 |
| `views/` | 登入頁、Workflow 列表頁、Workflow 編輯器 |

技術棧：Pinia、Vue Router、Element Plus、Vue Flow（`@vue-flow/core` 及 `background`/`minimap`/`controls`）、`@dagrejs/dagre`（自動排版）、axios、nanoid；測試使用 Vitest + `@vue/test-utils`。

> 完整前端架構（開發啟動、核心設計慣例、測試策略）見 [前端架構（bestpartner-ui）](./frontend.md)。

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
│   ├── Google.kt            # Google 搜尋設定（settingSchema 事實來源）
│   ├── Tavily.kt            # Tavily 搜尋設定（settingSchema 事實來源）
│   └── ToolConfigField.kt   # 設定欄位 UI 提示 annotation（description/sensitive）
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
  - `dto/workflow/config/`：workflow 節點 config 的強型別契約（`NodeConfig.kt`——13 種 NodeType 各一個 `@Serializable` DTO + `NodeConfigRegistry`，為節點 config schema 的唯一事實來源）
  - `service/workflow/`：執行引擎（`WorkflowEngine` 編排＋拓撲活化遍歷、`ExecutionContext` 插值、`executor/` 下 11 種 NodeExecutor，含 CONDITION 分支、LOOP 子圖迭代、CODE GraalJS sandbox、DATA_TRANSFORM、TOOL 動態呼叫）
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
