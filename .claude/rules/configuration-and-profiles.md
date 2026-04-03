# 設定檔與 Profile

## 設定檔位置

`bestpartner-service/src/main/resources/application.properties`

編譯時透過 `-Dquarkus.profile=${profile}` 指定環境。

## Profile 說明

| Profile | 說明 | 主要差異 |
|---------|------|----------|
| dev（預設） | 本地開發環境 | log level = DEBUG，包含 MCP server log 與上傳檔案配置 |
| sit | 測試環境 | log level = DEBUG，含 Flyway 設定 |
| prod | 生產環境 | log level = INFO，含 Flyway 設定 |

### Profile 差異說明

- **dev**：無 Flyway 設定，包含 `mcp.server.log.enable` 與 `file.upload.dir` 配置，log level = DEBUG
- **sit**：包含 Flyway 設定（`migrate-at-start=false`，手動執行），log level = DEBUG
- **prod**：包含 Flyway 設定（`migrate-at-start=false`，手動執行），log level = INFO

## 主要設定項目

| 設定項 | 預設值 | 說明 |
|--------|--------|------|
| `quarkus.http.port` | `80` | HTTP 服務埠 |
| `quarkus.datasource.jdbc.url` | `jdbc:postgresql://localhost:5432/pgdb` | 資料庫連線 |
| `quarkus.datasource.username` | `pguser` | 資料庫帳號 |
| `quarkus.datasource.password` | `pgpass` | 資料庫密碼 |
| `quarkus.datasource.jdbc.min-size` | `5` | 連線池最小連線數 |
| `quarkus.datasource.jdbc.max-size` | `15` | 連線池最大連線數 |
| `quarkus.log.file.path` | `D:/tmp/bestpartner/bestpartner.log` | 一般日誌路徑 |
| `quarkus.log.handler.file."ERROR_LOG".path` | `D:/tmp/bestpartner/bestpartner_error.log` | 錯誤日誌路徑（獨立檔案） |
| `mp.jwt.verify.issuer` | `bast-partner` | JWT 簽發者 |
| `mp.jwt.verify.publickey.location` | `publicKey.pem` | JWT 公鑰位置 |
| `smallrye.jwt.sign.key.location` | `privateKey.pem` | JWT 私鑰位置 |
| `jwt.refresh.switch` | `true` | JWT 自動刷新開關 |
| `mcp.server.log.enable` | `true` | MCP server 日誌開關（僅 dev） |
| `file.upload.dir` | `D:/tmp/bestpartner/upload` | 上傳檔案目錄（僅 dev） |

## 資料庫設定

- DB：PostgreSQL，預設連線 `localhost:5432/pgdb`
- 帳號：`pguser` / 密碼：`pgpass`
- 初始化：執行 `docs/sql/bestpartner.sql`
- 無 Flyway 自動遷移（`migrate-at-start=false`），須手動執行 SQL
