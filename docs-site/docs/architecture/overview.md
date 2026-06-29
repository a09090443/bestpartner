---
sidebar_position: 1
---

# 架構概覽

## 系統定位

BestPartner 是一個以 **Quarkus** 框架為基礎的雲原生 AI 應用平台，透過 **LangChain4J** 整合各大 AI 模型，提供 REST API 讓前端或外部服務動態建立與調用 AI Agent。

## 整體架構

```
┌─────────────────────────────────────────────────────────┐
│                     外部客戶端                           │
│          (瀏覽器 / Postman / 前端應用)                   │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / WebSocket
┌──────────────────────▼──────────────────────────────────┐
│                BestPartner Service                       │
│                                                          │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │  JWT Filter  │  │ REST Resource│  │  WS Endpoint  │  │
│  └─────────────┘  └──────┬───────┘  └───────┬───────┘  │
│                           │                  │           │
│  ┌────────────────────────▼──────────────────▼───────┐  │
│  │                  Service Layer                     │  │
│  │   AssistantService / KnowledgeBaseService / ...   │  │
│  └────────────────────────┬──────────────────────────┘  │
│                           │                              │
│  ┌────────────────────────▼──────────────────────────┐  │
│  │              LangChain4J AI Layer                  │  │
│  │   LLM Builder / Vector Builder / Tool Provider    │  │
│  └──────┬────────────────────────────────────────────┘  │
│         │                                                │
└─────────┼────────────────────────────────────────────────┘
          │
    ┌─────┴──────────────────────────────────────┐
    │              外部服務                       │
    │                                             │
    │  ┌──────────┐  ┌────────┐  ┌────────────┐  │
    │  │ AI 模型  │  │向量資料│  │ PostgreSQL │  │
    │  │OpenAI    │  │庫      │  │            │  │
    │  │Anthropic │  │Chroma  │  │  MCP Server│  │
    │  │Gemini    │  │Milvus  │  │  (外部工具)│  │
    │  │Ollama    │  │InMemory│  │            │  │
    │  │Grok      │  └────────┘  └────────────┘  │
    │  └──────────┘                               │
    └─────────────────────────────────────────────┘
```

## 核心資料流

### 一般對話流程

```
客戶端
  └─→ POST /api/assistant/chat
        └─→ JWT 驗證
              └─→ AssistantService
                    └─→ LangChain4J (LLM Builder)
                          └─→ AI 模型 (OpenAI / Anthropic / ...)
                                └─→ 回應串流至客戶端
```

### RAG 查詢流程

```
客戶端
  └─→ POST /api/assistant/chat (帶入知識庫ID)
        └─→ AssistantService
              └─→ 向量資料庫查詢相關文件
                    └─→ 組合 Prompt + 文件內容
                          └─→ AI 模型
                                └─→ 基於知識庫的回應
```

### 工具調用流程

```
AI 模型決定需要調用工具
  └─→ Tool Provider 路由
        ├─→ 內建工具 (Google Search / Date / Text2SQL)
        └─→ MCP Server 工具 (外部擴充)
              └─→ 工具結果回傳給 AI 模型
                    └─→ AI 整合結果後回應
```

## Workflow 引擎資料模型

視覺化 Workflow 引擎以 6 張 `llm_workflow*` 資料表落地（Phase 1 已建立定義相關表）：

| 資料表 | 說明 |
|--------|------|
| `llm_workflow` | Workflow 定義主檔（名稱、擁有者、狀態、版本、畫布視口）|
| `llm_workflow_node` | 節點（類型、座標、節點專屬設定 `config`）|
| `llm_workflow_edge` | 連線（來源/目標節點、連接點、條件）|
| `llm_workflow_trigger` | 觸發器（manual/webhook/cron）※後續階段啟用 |
| `llm_workflow_execution` | 執行紀錄主檔 ※後續階段啟用 |
| `llm_workflow_node_execution` | 各節點執行明細 ※後續階段啟用 |

> 詳見 [視覺化 Workflow](../features/workflow.md)。
