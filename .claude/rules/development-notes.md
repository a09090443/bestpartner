# 開發注意事項

## MCP Server 範例

`bestpartner-mcp-servers` 提供兩種實作：

| 範例 | 路徑 | 執行指令 |
|------|------|---------|
| Quarkus | `quarkus-example/` | `./gradlew quarkusDev` |
| Spring Boot | `spring-example/` | 標準 Spring Boot 啟動 |

## 開發環境路徑（Windows 預設值）

以下為**未設定環境變數時的預設值**，僅適用於本機 Windows 開發：

| 用途 | 路徑 | 由哪個環境變數覆寫 |
|------|------|------------------|
| 一般日誌 | `D:/tmp/bestpartner/bestpartner.log` | `LOG_DIR` |
| 錯誤日誌 | `D:/tmp/bestpartner/bestpartner_error.log` | `LOG_DIR` |
| 上傳檔案目錄 | `D:/tmp/bestpartner/upload` | `FILE_UPLOAD_DIR` |

⚠️ **容器 / Linux 環境務必以環境變數覆寫為 POSIX 路徑**，否則會產生名為 `D:` 的字面目錄
（實測落點 `/deployments/D:/tmp/bestpartner/`）。容器路徑建議見 [`configuration-and-profiles.md`](configuration-and-profiles.md)。

## 開關控制

| 設定 | 說明 | 環境變數 |
|------|------|---------|
| `mcp.server.log.enable` | MCP server 日誌（dev / docker / sit 開啟） | — |
| `jwt.refresh.switch` | JWT 自動刷新 | `JWT_REFRESH_SWITCH` |
| `quarkus.swagger-ui.enable` | Swagger UI 對外開關（runtime） | `QUARKUS_SWAGGER_UI_ENABLE` |

## 設定注入原則

跨環境會變動的值一律以 `${ENV_VAR:預設值}` 撰寫，機密與連線位址不寫死在 `application.properties`。
完整環境變數對照表與 profile 說明 → [`configuration-and-profiles.md`](configuration-and-profiles.md)
