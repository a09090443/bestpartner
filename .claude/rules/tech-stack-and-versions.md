# 技術堆疊與版本

## 核心版本

| 類別 | 技術 | 版本 |
|------|------|------|
| 語言 | Kotlin | 2.1.0 |
| 框架 | Quarkus | 3.21.0 |
| AI 函式庫 | Langchain4j | 1.17.2 |
| JDK | OpenJDK | 21 |
| 資料庫 | PostgreSQL | - |
| 建置工具 | Gradle | - |
| JS 沙箱 | GraalJS（org.graalvm.polyglot） | 24.2.1 |
| 設定檔機密加密 | SmallRye Config Crypto（io.smallrye.config:smallrye-config-crypto） | 由 Quarkus BOM 管理（3.21.0 → 3.12.3） |

> GraalJS 用於 Workflow `CODE` 節點的 JavaScript sandbox（`allowAllAccess(false)`、禁 host class/IO、逾時與輸出上限）。版本進 `gradle.properties`（`graalJsVersion`），並以 `resolutionStrategy` 對齊 `org.graalvm.*`。truffle-api 為 multi-release jar 且含 `META-INF/resources`，會觸發 Quarkus 3.21 `StaticResourcesProcessor` 越界。`build.gradle.kts` 以獨立 configuration（`trufflePristine`）解析原始 truffle-api，於 `stripTruffleResources` 任務剝除 `META-INF/resources` 產出乾淨 jar，並自應用 classpath 排除原始 truffle-api、改掛剝除版檔案依賴避開。**不可改用 artifact transform**：transform 屬性一旦套到 Quarkus `quarkusDev*` classpath configuration，會擾動 dev 模式依賴解析，使 `CapabilityAggregationStep` 將同一 jar 誤判為多提供者而啟動失敗。

> SmallRye Config Crypto 提供機密值解密的 config expression，讓 `.env.<profile>` 的機密值以密文存放、啟動時自動解密；本專案以 `config/EncSecretKeysHandlerFactory.kt` 將前綴縮短為 `${enc::<密文>}`（等價於內建的 `aes-gcm-nopadding`，密文格式相同）。版本由 Quarkus BOM 管理，**不在 `gradle.properties` 定義版本號**（依 [`gradle-conventions.md`](gradle-conventions.md)，BOM 管理的依賴不寫版本）。用法見 [`configuration-and-profiles.md`](configuration-and-profiles.md)。

**當前服務版本**: 0.1.8-SNAPSHOT

## 支援的 AI 平台

- OpenAI
- Ollama
- Anthropic
- Gemini
- Grok
- OpenRouter

## 向量資料庫

- Chroma
- Milvus
- InMemoryEmbeddingStore（預設）

## 版本異動規範

當任何核心版本變更時，**必須同步更新以下文件**：

| 文件 | 路徑 |
|------|------|
| doc 文件技術選型頁 | `docs-site/docs/architecture/tech-stack.md` |
| README 開發環境表格 | `README.md` |

## 內建工具

| 工具名稱 | 類型 |
|----------|------|
| Google 搜尋引擎 | Web Search |
| Tavily 搜尋引擎 | Web Search |
| Date 日期 | Date |
| Text2SQL | Other |
