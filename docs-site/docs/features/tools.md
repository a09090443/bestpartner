---
sidebar_position: 3
---

# 工具（Tools）

BestPartner 支援 AI 工具調用（Tool Calling / Function Calling），讓 AI 可以執行外部動作，例如搜尋網路、查詢資料庫等。

## 內建工具

| 工具名稱 | 類型 | 說明 |
|---------|------|------|
| **Google 搜尋引擎** | Web Search | 使用 Google Custom Search API 搜尋網路 |
| **Tavily 搜尋引擎** | Web Search | 專為 AI 設計的搜尋引擎，結果品質較佳 |
| **Date 日期工具** | Utility | 取得當前日期時間 |
| **Text2SQL** | Database | 將自然語言轉換為 SQL 查詢並執行 |

## 啟用工具

### 方式一：使用 `toolIds`（無需自訂設定的工具）

適用於 Google 搜尋、Tavily、Date 等不需要使用者特殊設定的工具：

```bash
POST /api/assistant/chat
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "message": "幫我搜尋最新的 AI 新聞",
  "modelId": 1,
  "toolIds": [1, 2]
}
```

### 方式二：使用 `toolSettingIds`（需要自訂設定的工具）

適用於 Text2SQL 等需要使用者自訂資料庫連線資訊的工具：

```bash
POST /api/assistant/chat
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "message": "查詢上個月的銷售總額",
  "modelId": 1,
  "toolSettingIds": [1]
}
```

## 設定 Google 搜尋工具

需申請 Google Custom Search API：

```sql
-- 在 tool_config 表中設定
UPDATE tool_config
SET api_key = 'YOUR_GOOGLE_API_KEY',
    search_engine_id = 'YOUR_SEARCH_ENGINE_ID'
WHERE tool_type = 'GOOGLE_SEARCH';
```

## 設定 Tavily 搜尋工具

```sql
UPDATE tool_config
SET api_key = 'tvly-xxxxxxxxxxxxxxxx'
WHERE tool_type = 'TAVILY';
```

## Text2SQL 工具

Text2SQL 讓 AI 可以理解自然語言並轉換為 SQL 查詢，適合讓非技術使用者查詢資料庫。

### 建立 Text2SQL 工具設定

```bash
POST /api/tool-setting
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "toolType": "TEXT2SQL",
  "name": "銷售資料庫",
  "dbUrl": "jdbc:mysql://localhost:3306/sales",
  "dbUsername": "readonly_user",
  "dbPassword": "password",
  "schema": "CREATE TABLE orders (id INT, amount DECIMAL, created_at DATETIME)..."
}
```

:::tip 提供 Schema 資訊
在 `schema` 欄位提供資料庫的表格結構定義，可以大幅提升 AI 生成 SQL 的準確度。
:::

## 自訂工具開發

除了內建工具，你也可以透過 MCP Server 擴充工具。詳見 [MCP Server 說明](./mcp-servers)。
