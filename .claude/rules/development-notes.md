# 開發注意事項

## MCP Server 範例

`bestpartner-mcp-servers` 提供兩種實作：

| 範例 | 路徑 | 執行指令 |
|------|------|---------|
| Quarkus | `quarkus-example/` | `./gradlew quarkusDev` |
| Spring Boot | `spring-example/` | 標準 Spring Boot 啟動 |

## 開發環境路徑（Windows）

| 用途 | 路徑 |
|------|------|
| 一般日誌 | `D:/tmp/bestpartner/bestpartner.log` |
| 錯誤日誌 | `D:/tmp/bestpartner/bestpartner_error.log` |
| 上傳檔案目錄 | `D:/tmp/bestpartner/upload` |

⚠️ 部署前請修改以上路徑為對應環境的實際路徑。

## 開關控制

| 設定 | 說明 |
|------|------|
| `mcp.server.log.enable` | MCP server 日誌（僅 dev 環境） |
| `jwt.refresh.switch` | JWT 自動刷新 |
