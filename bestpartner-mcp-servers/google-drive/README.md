# Google Drive MCP 服務器

此 MCP 服務器提供與 Google Drive 整合的功能，允許您管理文件和目錄，包括創建、上傳、讀取、更新、刪除、搜索和移動操作。

## 功能列表

### 文件操作
- **創建文件**: 在 Google Drive 中創建新文件
- **上傳文件**: 將本地文件上傳到 Google Drive
- **讀取文件**: 獲取 Google Drive 文件內容
- **更新文件**: 更新 Google Drive 中的文件內容
- **刪除文件**: 從 Google Drive 中刪除文件
- **重命名文件**: 更改 Google Drive 文件名稱
- **查找文件**: 通過文件名獲取文件 ID
- **搜索文件**: 支持關鍵詞模糊搜索文件
- **列出所有文件**: 獲取 Google Drive 中的文件列表
- **移動文件**: 將文件移動到指定目錄

### 目錄操作
- **創建目錄**: 在 Google Drive 根目錄或指定父目錄中創建目錄
- **刪除目錄**: 刪除 Google Drive 中的目錄
- **重命名目錄**: 更改目錄名稱
- **列出目錄內容**: 顯示目錄中的文件和子目錄

## 技術需求

### 開發環境
- **語言**: Kotlin 2.0.21
- **JDK 版本**: 21
- **構建工具**: Gradle
- **框架**: Quarkus 3.21.0

### 依賴庫
- Quarkus Kotlin
- MCP Server STDIO v1.0.0.CR1
- Google API Client v2.7.2
- Google OAuth Client Jetty v1.39.0
- Google API Services Drive v3-rev20250220-2.0.0
- Apache Commons IO v2.18.0

## 設置與配置

### 前提條件
1. [創建 Google Cloud 項目](https://console.cloud.google.com/projectcreate)
2. [啟用 Google Drive API](https://console.cloud.google.com/apis/library/drive.googleapis.com)
3. [配置 OAuth 同意屏幕](https://console.cloud.google.com/apis/credentials/consent)
4. [創建 OAuth 客戶端 ID](https://console.cloud.google.com/apis/credentials)（應用類型選擇"桌面應用"）
5. 下載 OAuth 客戶端憑證 JSON 文件

### 環境變量設置
- `CREDENTIALS_FILE_PATH`: OAuth 客戶端憑證 JSON 文件的路徑

### 構建與運行

使用 Gradle 構建並運行服務:

```bash
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

使用 JBang 運行服務:

```bash
jbang --quiet google-drive-1.0-SNAPSHOT-runner.jar
```

開發模式:

```bash
./gradlew quarkusDev
```

### MCP 配置文件設置

您可以在 MCP 配置文件中添加 Google Drive 服務，方便統一管理多個 MCP 服務:

```json
{
  "mcpServers": {
    "gdrive": {
      "command": "java",
      "args": [
        "-jar", "D:\\MCP\\google-drive-1.0-SNAPSHOT-runner.jar"
      ]
    }
  }
}
```

配置說明:
- 此設定將 Google Drive MCP 服務命名為 `gdrive`
- `command` 指定要執行的命令 (此處為 java)
- `args` 包含執行命令的參數，指向 Google Drive MCP 服務的 JAR 文件
- 請根據您的實際環境調整 JAR 文件的路徑

## 與 Langchain4j 集成示例

以下是在 LLMResource 中與 Google Drive 工具集成的示例:

```kotlin
val googleDriveTransport = StdioMcpTransport.Builder()
    .command(listOf("java", "-jar", "google-drive-runner.jar"))
    .logEvents(true)
    .build()
val googleDrive = DefaultMcpClient.Builder().transport(googleDriveTransport).build()
val toolProvider = McpToolProvider.builder().mcpClients(listOf(googleDrive)).build()
aiService.toolProvider(toolProvider)
```

使用 JBang 運行的例子:

```kotlin
val googleDriveTransport = StdioMcpTransport.Builder()
    .command(listOf("jbang", "--quiet", "google-drive-1.0-SNAPSHOT-runner.jar"))
    .logEvents(true)
    .build()
val googleDrive = DefaultMcpClient.Builder().transport(googleDriveTransport).build()
val toolProvider = McpToolProvider.builder().mcpClients(listOf(googleDrive)).build()
aiService.toolProvider(toolProvider)
```

## 權限與安全

此服務需要以下 Google Drive 權限:
- `https://www.googleapis.com/auth/drive.file`: 對用戶通過此應用創建或打開的文件的訪問權限
- `https://www.googleapis.com/auth/drive.metadata.readonly`: 查看文件元數據的權限
