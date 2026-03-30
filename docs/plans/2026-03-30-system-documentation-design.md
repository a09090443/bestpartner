# BestPartner 系統文件設計方案

## 概述

為 BestPartner 專案建立一套完整的 Docusaurus 文件網站，涵蓋架構、快速開始、功能說明及 API 文件。

## 目標讀者

- 開發者（程式碼架構、功能整合）
- 維運/DevOps（部署、環境設定）
- 新進成員（快速上手）

## 技術選型

- **框架：** Docusaurus
- **語言：** 繁體中文
- **位置：** `docs-site/`（避免與現有 `docs/` 目錄衝突）

## 文件結構

```
docs-site/
├── docs/
│   ├── intro.md
│   ├── architecture/
│   │   ├── overview.md
│   │   ├── modules.md
│   │   └── tech-stack.md
│   ├── getting-started/
│   │   ├── prerequisites.md
│   │   ├── installation.md
│   │   └── first-run.md
│   ├── features/
│   │   ├── ai-models.md
│   │   ├── rag.md
│   │   ├── tools.md
│   │   └── mcp-servers.md
│   └── api/
│       ├── authentication.md
│       ├── assistant.md
│       └── knowledge-base.md
├── docusaurus.config.ts
├── sidebars.ts
└── package.json
```

## 各文件內容規劃

### architecture/overview.md
- 系統定位（類 Dify/Coze 的 AI 應用平台）
- 整體架構圖
- 核心資料流說明

### getting-started/
- `prerequisites.md`：JDK 21、MySQL、向量資料庫（擇一）、AI 平台金鑰
- `installation.md`：git clone → 資料庫初始化 → application.properties 設定 → gradle build
- `first-run.md`：啟動服務、登入取得 JWT、第一個 Chat 請求

### features/
- `ai-models.md`：OpenAI、Anthropic、Gemini、Ollama、Grok 設定方式
- `rag.md`：上傳文件、建立知識庫、查詢流程
- `tools.md`：內建工具（Google/Tavily 搜尋、Date、Text2SQL）及自訂工具
- `mcp-servers.md`：Quarkus 與 Spring MCP server 範例說明

### api/
- 基於現有 Postman Collection 整理
- 每個端點的用途、參數、回應格式

## 建立順序

1. 初始化 Docusaurus 網站
2. architecture（架構概覽）
3. getting-started（快速開始）
4. features（功能說明）
5. api（API 文件）
