---
sidebar_position: 3
---

# 技術選型

## 核心框架

| 技術 | 版本 | 用途 |
|------|------|------|
| **Quarkus** | 3.21.0 | 雲原生 Java/Kotlin 框架，支援 GraalVM 原生編譯 |
| **Kotlin** | 2.1.0 | 主要開發語言 |
| **LangChain4J** | 1.13.0 | AI 模型整合框架 |
| **Hibernate Panache** | — | ORM 資料庫存取 |
| **SmallRye JWT** | — | JWT 認證與授權 |

## AI 模型支援

| 模型提供者 | 類型 | 說明 |
|-----------|------|------|
| **OpenAI** | 雲端 | GPT-4、GPT-3.5 等系列 |
| **Anthropic** | 雲端 | Claude 系列 |
| **Google Gemini** | 雲端 | Gemini 系列 |
| **Grok** | 雲端 | xAI Grok 系列 |
| **Ollama** | 本地端 | 支援在本機運行各種開源模型 |

## 向量資料庫

| 資料庫 | 說明 |
|--------|------|
| **InMemory** | 預設，無需額外安裝，適合開發測試 |
| **Chroma** | 輕量級向量資料庫，支援 Docker 部署 |
| **Milvus** | 高效能向量資料庫，適合生產環境 |

## 資料庫

| 資料庫 | 版本 | 說明 |
|--------|------|------|
| **PostgreSQL** | 最新版 | 主要關聯式資料庫，儲存模型設定、工具設定、使用者資料 |

## 搜尋工具整合

| 工具 | 說明 |
|------|------|
| **Google Custom Search** | 需申請 API Key |
| **Tavily** | AI 搜尋引擎，需申請 API Key |

## 開發工具

| 工具 | 版本 | 用途 |
|------|------|------|
| **OpenJDK** | 21 | Java 執行環境 |
| **Gradle** | 最新版 | 建置工具 |
| **Docker** | — | 本地開發環境（向量資料庫、PostgreSQL） |
| **Postman** | 最新版 | API 測試 |

## 架構決策說明

### 為何選用 Quarkus？

Quarkus 是針對雲原生環境最佳化的 Java 框架，相比 Spring Boot 有更快的啟動速度和更低的記憶體使用量。同時支援 Kotlin，提供更簡潔的程式碼。

### 為何選用 LangChain4J？

LangChain4J 是 Java/Kotlin 生態中最完整的 AI 整合框架，支援多種 AI 模型、向量資料庫、RAG、工具調用等功能，與 Quarkus 有良好的整合支援。

### 為何同時支援多種向量資料庫？

不同環境有不同需求：開發測試用 InMemory 最方便，小規模部署可用 Chroma，大規模生產環境則推薦 Milvus。透過設定切換，不需要修改程式碼。
