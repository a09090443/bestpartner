# 技術堆疊與版本

## 核心版本

| 類別 | 技術 | 版本 |
|------|------|------|
| 語言 | Kotlin | 2.1.0 |
| 框架 | Quarkus | 3.21.0 |
| AI 函式庫 | Langchain4j | 1.13.0 |
| JDK | OpenJDK | 21 |
| 資料庫 | PostgreSQL | - |
| 建置工具 | Gradle | - |

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
