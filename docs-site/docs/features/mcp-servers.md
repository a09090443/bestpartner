---
sidebar_position: 4
---

# MCP Server

MCP（Model Context Protocol）是 Anthropic 提出的開放協定，讓 AI 模型可以透過標準化介面調用外部工具與資源。BestPartner 支援整合外部 MCP Server，擴充 AI 的工具能力。

## 概念說明

```
BestPartner Service
      │
      ├─→ 內建工具（Google Search / Tavily / Date / Text2SQL）
      │
      └─→ MCP Server（外部）
                ├─→ 任意工具 A
                ├─→ 任意工具 B
                └─→ 任意工具 C
```

透過 MCP Server，任何開發者都可以建立自己的工具集，並接入 BestPartner 平台，無需修改主服務程式碼。

## 設定 MCP Server

在 BestPartner 中新增 MCP Server 連線：

```bash
POST /api/mcp-server
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "name": "我的工具伺服器",
  "url": "http://localhost:8081/mcp/sse",
  "transportType": "SSE"
}
```

目前支援 **SSE（Server-Sent Events）** 傳輸模式。

## 範例：Quarkus MCP Server

專案中的 `bestpartner-mcp-servers/quarkus-example` 提供了完整範例。

### 內含工具

| 工具 | 說明 |
|------|------|
| `ChineseName` | 取得中文姓名相關資訊 |
| `Favorite` | 管理最愛清單 |

### 啟動範例

```bash
cd bestpartner-mcp-servers/quarkus-example
./gradlew quarkusDev
# 服務啟動於 http://localhost:8081
```

### 關鍵程式碼結構

```kotlin
// ChineseName.kt - MCP 工具範例
@ApplicationScoped
class ChineseName {

    @Tool("取得常見中文姓氏列表")
    fun getCommonSurnames(): List<String> {
        return listOf("陳", "林", "黃", "張", "李", ...)
    }
}
```

## 範例：Spring MCP Server

`bestpartner-mcp-servers/spring-example` 提供 Spring Boot 版本範例。

### 啟動範例

```bash
cd bestpartner-mcp-servers/spring-example
./gradlew bootRun
# 服務啟動於 http://localhost:8082
```

### 關鍵設定

```kotlin
// McpConfiguration.kt
@Configuration
class McpConfiguration {
    @Bean
    fun mcpAsyncServerCustomizer(): McpAsyncServerCustomizer {
        // MCP Server 設定
    }
}
```

## 建立自己的 MCP Server

參考上述範例，你可以：

1. 選擇 Quarkus 或 Spring Boot 框架
2. 定義工具方法（加上 `@Tool` 注解）
3. 啟動 MCP Server
4. 在 BestPartner 中登錄 MCP Server URL
5. 在對話中透過 `mcpServerIds` 參數啟用

```bash
POST /api/assistant/chat
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "message": "幫我查詢常見中文姓氏",
  "modelId": 1,
  "mcpServerIds": [1]
}
```

:::info 協定版本
BestPartner 使用 LangChain4J 整合 MCP，支援 SSE 傳輸模式。確保你的 MCP Server 使用相容的協定版本。
:::
