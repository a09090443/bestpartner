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

接收 HTTP 請求，進行參數驗證後委派給 Service 層處理。每個 Resource 對應一組功能 API（助手、知識庫、工具、模型設定等）。

### `service/` — 業務邏輯層

核心業務邏輯所在，協調 AI 模型調用、資料庫操作、工具執行等流程。

### `builder/` — AI 元件建構層

| 子目錄 | 用途 |
|--------|------|
| `builder/llm/` | 依資料庫設定動態建立 LLM 模型實例 |
| `builder/vector/` | 依設定建立向量資料庫連線實例 |

### `provider/` — 服務提供者

負責提供 AI 工具實例給 LangChain4J，包含內建工具及 MCP Server 工具的注入。

### `assistant/` — AI 助手實作

封裝 LangChain4J 的 AI Service 介面，定義對話、串流對話等行為。

### `entity/` + `repository/` — 資料持久層

JPA Entity 對應資料庫表格，Repository 使用 Hibernate Panache 提供 CRUD 操作。

### `config/` — 設定類

| 子目錄 | 用途 |
|--------|------|
| `config/chatmodel/` | 各 AI 模型的設定屬性 |
| `config/embedding/` | 嵌入模型設定 |
| `config/vector/` | 向量資料庫設定 |

### `filter/` — 請求過濾器

JWT Token 驗證過濾器，攔截所有請求進行身份驗證。

### `tool/` — 內建工具

內建工具實作：Google 搜尋、Tavily 搜尋、日期工具、Text2SQL 工具。

### `dto/` / `form/` / `model/` — 資料傳輸物件

- `dto/`：API 回應資料物件
- `form/`：API 請求參數物件
- `model/`：業務模型物件

## bestpartner-mcp-servers

提供兩種框架的 MCP Server 範例，開發者可參考這些範例建立自己的 MCP Server，再將其接入 BestPartner 平台。

| 範例 | 框架 | 說明 |
|------|------|------|
| `quarkus-example` | Quarkus | 示範如何用 Quarkus 建立 MCP Server，包含中文姓名（ChineseName）和最愛（Favorite）工具 |
| `spring-example` | Spring Boot | 示範如何用 Spring 建立 MCP Server，包含 MCP 設定與工具實作 |
