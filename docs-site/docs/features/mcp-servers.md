---
sidebar_position: 4
---

# MCP Server

MCP（Model Context Protocol）是 Anthropic 提出的開放協定，讓 AI 模型可以透過標準化介面調用外部工具。BestPartner 支援整合外部 MCP Server，擴充 AI 的工具能力。

## 支援的傳輸模式（McpType）

| 類型 | 說明 |
|------|------|
| `SSE` | Server-Sent Events，透過 HTTP URL 連線 |
| `STDIO` | 標準輸入輸出，透過本地命令啟動 |

## MCP Server API 端點

所有 MCP Server 相關 API 路徑前綴為 `/llm/mcpServer`。

| HTTP | 路徑 | 說明 |
|------|------|------|
| GET | `/llm/mcpServer/list` | 取得所有 MCP Server 列表 |
| POST | `/llm/mcpServer/get` | 取得特定 MCP Server |
| POST | `/llm/mcpServer/register` | 註冊 MCP Server |
| POST | `/llm/mcpServer/update` | 更新 MCP Server 設定 |
| POST | `/llm/mcpServer/delete` | 刪除 MCP Server |
| POST | `/llm/mcpServer/saveSetting` | 儲存 MCP Server 使用設定 |
| POST | `/llm/mcpServer/getSetting` | 取得 MCP Server 使用設定 |
| POST | `/llm/mcpServer/updateSetting` | 更新 MCP Server 使用設定 |
| DELETE | `/llm/mcpServer/deleteSetting` | 刪除 MCP Server 使用設定 |

## 資料庫表格結構

MCP Server 設定儲存在 `llm_mcp_server` 表：

| 欄位 | 說明 |
|------|------|
| `id` | UUID 主鍵 |
| `name` | MCP Server 名稱 |
| `type` | McpType（STDIO / SSE）|
| `command_setting` | JSON，包含連線設定（URL 或命令）|

## 範例：Quarkus MCP Server（STDIO 模式）

專案中的 `bestpartner-mcp-servers/quarkus-example` 使用 **STDIO** 傳輸模式。

**依賴：**
```kotlin
// build.gradle.kts
implementation("io.quarkiverse.mcp:quarkus-mcp-server-stdio:1.1.1")
```

**內含工具：**

```kotlin
// ChineseName.kt
@Tool(description = "Get the Chinese name of a user.")
fun chineseName(@ToolArg username: String): String {
    // 回傳使用者對應的中文姓名
}
```

```kotlin
// Favorite.kt
@Tool(description = "Get the favorite color of a user.")
fun favoriteColor(@ToolArg username: String): String {
    // 回傳使用者最喜歡的顏色
}
```

**啟動範例：**

```bash
cd bestpartner-mcp-servers/quarkus-example
./gradlew quarkusDev
```

## 範例：Spring MCP Server（STDIO 模式）

`bestpartner-mcp-servers/spring-example` 同樣使用 **STDIO** 傳輸模式。

**依賴：**
```kotlin
implementation("org.springframework.ai:spring-ai-mcp-server-spring-boot-starter")
```

**工具實作：**

```kotlin
// ChineseName.kt
@Component
class ChineseName {
    @Tool(description = "Get the Chinese name of a user.")
    fun getChineseName(@ToolParam username: String): String {
        // 回傳使用者對應的中文姓名
    }
}
```

**McpConfiguration 設定：**

```kotlin
@Configuration
class McpConfiguration {
    @Bean
    fun mcpTools(chineseName: ChineseName): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder()
            .toolObjects(chineseName)
            .build()
    }
}
```

**application.properties：**

```properties
spring.ai.mcp.server.stdio=true
```

**啟動範例：**

```bash
cd bestpartner-mcp-servers/spring-example
./gradlew bootRun
```

## 建立自己的 MCP Server

1. 參考上述範例選擇 Quarkus 或 Spring Boot
2. 定義工具方法（加上 `@Tool` 注解）
3. 依需求選擇 STDIO 或 SSE 傳輸模式
4. 在 BestPartner 中透過 `/llm/mcpServer/register` API 登錄

:::info 傳輸模式選擇
- **STDIO**：MCP Server 與 BestPartner 在同一台機器上，透過命令列啟動
- **SSE**：MCP Server 部署在遠端，透過 HTTP URL 連線，需設定正確的 URL
:::
