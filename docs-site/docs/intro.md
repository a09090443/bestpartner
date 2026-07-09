---
slug: /
sidebar_position: 1
description: BestPartner 是一個 AI 應用大平台，可動態建立 AI Agent 並支援 OpenAI、Anthropic、Gemini、Ollama、Grok 等多種模型，內建 RAG 知識庫、工具調用、MCP Server 與視覺化 Workflow。
keywords: [BestPartner, AI 平台, AI Agent, LLM, Dify, Coze, LangChain4J, Quarkus]
---

# BestPartner 簡介

BestPartner 是一個 AI 應用大平台，讓使用者可以動態建立 AI Agent 並支援多種 AI 模型，目標是打造類似 [Dify](https://dify.ai) 或 [Coze](https://coze.com) 的自助式 AI 平台。

## 主要特色

- **多模型支援**：OpenAI、Anthropic Claude、Google Gemini、Ollama（本地端）、Grok
- **向量資料庫整合**：支援 Chroma、Milvus 及記憶體內嵌入儲存
- **RAG 知識庫**：上傳文件、建立知識庫，讓 AI 回答基於你的資料
- **動態工具調用**：內建 Google/Tavily 搜尋、日期查詢、Text2SQL，並支援自訂工具
- **MCP Server 支援**：整合 Model Context Protocol，擴充 AI 的外部工具能力
- **RBAC 權限管理**：JWT 認證與角色型存取控制
- **WebSocket 串流**：支援即時串流回應

## 版本歷程

| 版本 | 日期 | 重點功能 |
|------|------|---------|
| 0.1.6 | 2025.04.25 | MCP server 支援、Anthropic/Gemini/Grok 整合 |
| 0.1.5 | 2025.03.14 | TEXT2SQL 工具、RAG 知識庫完整功能 |
| 0.1.4 | 2025.01.16 | Tavily 搜尋、API 權限管理 |
| 0.1.3 | 2024.12.11 | 自製工具註冊、Mysql 資料庫 |
| 0.1.2 | 2024.11.03 | RBAC 功能、系統設定表 |
| 0.1.1 | 2024.10.23 | Chroma/Milvus 支援、RAG 功能 |
| 0.1.0 | 2024.10.15 | 初版發布 |

## 快速導覽

- 想了解系統設計？ → [架構概覽](./architecture/overview)
- 想立即上手？ → [快速開始](./getting-started/prerequisites)
- 想了解各功能？ → [功能說明](./features/ai-models)
- 想串接 API？ → [API 文件](./api/authentication)
