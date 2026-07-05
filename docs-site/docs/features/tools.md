---
sidebar_position: 3
---

# 工具（Tools）

BestPartner 支援 AI 工具調用（Tool Calling），讓 AI 可以執行外部動作，例如搜尋網路、查詢資料庫等。

## 內建工具

| 工具 | 類別 | 分類（ToolsCategory）|
|------|------|-------------------|
| **Google 搜尋** | `tw.zipe.bastpartner.tool.config.Google` | `WEB_SEARCH` |
| **Tavily 搜尋** | `tw.zipe.bastpartner.tool.config.Tavily` | `WEB_SEARCH` |
| **Date 日期** | `tw.zipe.bastpartner.tool.DateTool` | `DATE` |
| **Text2SQL** | `tw.zipe.bastpartner.tool.text2sql.Text2SQLTool` | `OTHER` |

## 工具類型（ToolsType）

| 類型 | 說明 |
|------|------|
| `BUILT_IN` | 系統內建工具 |
| `CUSTOMIZE` | 使用者自訂工具 |

## 工具 API 端點

所有工具相關 API 路徑前綴為 `/llm/tool`。

| HTTP | 路徑 | 說明 |
|------|------|------|
| GET | `/llm/tool/list` | 取得所有工具列表（每筆含 `settingSchema`） |
| POST | `/llm/tool/get` | 取得特定工具（含 `settingSchema`） |
| POST | `/llm/tool/register` | 註冊工具 |
| POST | `/llm/tool/delete` | 刪除工具 |
| POST | `/llm/tool/saveSetting` | 儲存工具自訂設定 |
| POST | `/llm/tool/updateSetting` | 更新工具自訂設定 |
| POST | `/llm/tool/category/save` | 建立工具分類 |
| POST | `/llm/tool/category/update` | 更新工具分類 |
| POST | `/llm/tool/category/delete` | 刪除工具分類 |

## 設定欄位結構（settingSchema）

工具的設定欄位由後端**反射自動產生**：`config_object_path` 指向的 Kotlin data class（如 `tool/config/Google.kt`）即欄位的唯一事實來源，`ToolSchemaGenerator` 反射其建構子產生結構化 schema，隨 `list`/`get` 回傳：

```json
{
  "apiKey":  { "type": "string",  "required": true, "sensitive": true, "description": "Google Custom Search API 金鑰" },
  "csi":     { "type": "string",  "required": true, "description": "Custom Search Engine ID" },
  "siteRestrict": { "type": "boolean", "required": false },
  "timeout": { "type": "integer", "required": true, "description": "逾時毫秒數" }
}
```

- `type`：`string` / `integer` / `number` / `boolean` / `array`（由 Kotlin 型別推導）
- `required`：nullable 欄位為 `false`（由型別推導，不需手動維護）
- `sensitive` / `description`：由 config class 欄位上的 `@ToolConfigField` annotation 標註
- 無 config class 的工具（如 Date）`settingSchema` 為 `null`

前端 workflow 編輯器的 TOOL 節點表單依此 schema 動態渲染設定欄位（sensitive 欄位以密碼框呈現），填寫後可一鍵建立工具設定（`saveSetting` + `updateSetting`）並自動帶入節點的 `toolSettingId`。

> 新增工具的 config class 欄位時，schema 自動更新，**無需改前端或文件**；下方欄位表僅為快速參考。

## Google 搜尋設定

Google 搜尋工具設定欄位：

| 欄位 | 說明 |
|------|------|
| `apiKey` | Google Custom Search API Key |
| `csi` | Custom Search Engine ID |
| `siteRestrict` | 限制搜尋的網站（選填）|
| `includeImages` | 是否包含圖片結果 |
| `timeout` | 請求逾時（毫秒；純數字視為毫秒，亦接受 `5s`/`1m` 帶單位格式）|

## Tavily 搜尋設定

Tavily 為專為 AI 設計的搜尋引擎，設定欄位：

| 欄位 | 說明 |
|------|------|
| `baseUrl` | Tavily API 基礎 URL |
| `apiKey` | Tavily API Key |
| `searchDepth` | 搜尋深度 |
| `includeAnswer` | 是否包含摘要答案 |
| `includeRawContent` | 是否包含原始內容 |
| `includeDomains` | 限制搜尋的網域 |
| `excludeDomains` | 排除的網域 |

## Text2SQL 工具

Text2SQL 支援以下資料庫類型（DatabaseType）：

| 類型 | 說明 |
|------|------|
| `MYSQL` | MySQL 資料庫 |
| `POSTGRESQL` | PostgreSQL 資料庫 |

Text2SQL 工具需要自訂設定，使用時需透過 `toolSettingIds` 參數帶入設定 ID。

## 自訂工具

除了內建工具，BestPartner 支援自訂工具。在 `llm_tool` 表中的關鍵欄位：

| 欄位 | 說明 |
|------|------|
| `class_path` | 工具類別的完整路徑 |
| `config_object_path` | 設定物件路徑 |
| `function_name` | 工具方法名稱 |
| `function_description` | 工具功能描述（AI 用來決定何時調用）|
| `function_params` | 方法參數定義（JSON）|

## 透過 MCP Server 擴充工具

更靈活的工具擴充方式是使用 MCP Server，詳見 [MCP Server 說明](./mcp-servers)。
