# quarkus-example — Quarkus MCP Server 範例

本專案示範如何使用 [Quarkus](https://quarkus.io/) 搭配 [quarkus-mcp-server](https://github.com/quarkiverse/quarkus-mcp-server) 擴充套件，快速建立一個符合 MCP（Model Context Protocol）規範的 Server，供 AI 模型動態呼叫自訂工具。

---

## 功能說明

本範例提供兩個 MCP Tool，示範如何以 `@Tool` 標註定義可被 AI 呼叫的工具函式：

### `chineseName`

根據英文用戶名，回傳對應的中文名稱。

| 參數 | 類型 | 說明 |
|------|------|------|
| `username` | String | 英文用戶名 |

**支援對應**：

| 輸入 | 回傳 |
|------|------|
| Gary | 蓋瑞 |
| Bob | 鮑勃 |
| Charlie | 查理 |
| 其他 | Unknown |

---

### `favoriteColor`

根據用戶名，回傳該使用者最喜歡的顏色。

| 參數 | 類型 | 說明 |
|------|------|------|
| `username` | String | 英文用戶名 |

**支援對應**：

| 輸入 | 回傳 |
|------|------|
| Gary | Blue |
| Bob | Green |
| Charlie | Red |
| 其他 | Unknown |

---

## 技術堆疊

| 類別 | 版本 |
|------|------|
| 語言 | Kotlin 2.1.0 |
| 框架 | Quarkus 3.21.0 |
| MCP 擴充 | quarkus-mcp-server-stdio 1.1.1 |
| JDK | OpenJDK 21 |

MCP 傳輸協定使用 **Stdio**，適合整合至 Claude Desktop、BestPartner 等 MCP 主機環境。

---

## 快速開始

### 開發模式（支援熱重載）

```bash
./gradlew quarkusDev
```

### 建置 Uber JAR

```bash
./gradlew build
```

產生的 jar 位於 `build/` 目錄，可直接執行：

```bash
java -jar build/quarkus-example-1.0-SNAPSHOT-runner.jar
```

### 建置 Native Executable（需 GraalVM）

```bash
./gradlew build -Dquarkus.native.enabled=true
```

---

## 設定說明

設定檔位於 `src/main/resources/application.properties`：

| 設定項 | 預設值 | 說明 |
|--------|--------|------|
| `quarkus.package.type` | `uber-jar` | 打包成單一 jar |
| `quarkus.log.file.enable` | `true` | 啟用檔案日誌 |
| `quarkus.log.file.path` | `D:/tmp/quarkus-mcp-server-example.log` | 日誌輸出路徑 |

---

## 演進歷史

| 版本 / Commit | 說明 |
|---------------|------|
| 初始版本 | 建立 Quarkus MCP Server 基礎範例（`b0ce769`） |
| 升級依賴 | 升級 Quarkus 及相關套件版本（`4eb2156`） |
| 重構與擴充 | 新增 `ChineseName` Tool，重構名稱處理邏輯，更新依賴（`e6db628`） |

---

## 相關資源

- [Quarkus MCP Server 擴充文件](https://docs.quarkiverse.io/quarkus-mcp-server/dev/)
- [Model Context Protocol 規範](https://modelcontextprotocol.io/)
- [BestPartner 主專案](../../README.md)
