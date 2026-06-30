[![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

BestPartner project
======================

## 說明
1. 該專案為一個 AI 應用大平台，可動態建立 AI agent 並支援多種 AI 模型
2. 各 API 範例可參考 Postman Collection：[連結](https://github.com/a09090443/bestpartner/blob/master/docs/postman/basepartner.postman_collection.json)
3. 未來目標為類似 Dify 或 Coze 平台，可自行建立 AI agent 並支援多種 AI 模型

---

## 版本變更說明

## 1.7 版本變更說明
1. 升級 Quarkus 至 3.21.0 版本
2. 升級 Langchain4j 至 1.12.2 版本
3. 資料庫由 MySQL 改為 PostgreSQL

## 1.6 版本變更說明
1. 新增 MCP Server 支援
2. 新增 Anthropic 支援
3. 新增 Gemini 支援
4. 新增 Grok 支援
5. Langchain4j 升級至 1.0.0-beta3 版本
6. 移除 Flyway 套件
7. bestpartner-mcp-servers 增加 MCP Server 範例

## 1.5 版本變更說明
1. 修改工具建立邏輯
2. 新增 TEXT2SQL 工具
3. 修改 LLM 工具建立邏輯，當工具需要使用者自定義內容時須帶入 `toolSettingIds` 參數，工具無須自定義內容時帶入 `toolIds` 參數，可參考 Postman 中 `custom_assistant_chat(text2sql)` 範例
4. 修改 RAG 上傳檔案及刪除檔案時的邏輯
5. 修正 LLM 讀取知識庫錯誤問題
6. 新增向量資料庫搜尋功能

## 1.4 版本變更說明
1. 設定各 API 權限管理
2. 修正動態建立 LLM 模型錯誤問題
3. 修正 Tools 工具錯誤問題
4. 新增 Tavily 網路搜尋引擎工具

## 1.3 版本變更說明
1. 廢除 H2 DB，改為使用 MySQL DB
2. 增加 LLM Tools 可註冊自製工具
3. 增加 LLM Tools 使用者自訂工具設定值

## 1.2 版本變更說明
1. 支援 RBAC 功能，請先使用 `POST /login` 取得 JWT 令牌，可參考 Postman 中 auth → login 範例，管理員帳號：`admin/admin`
2. 增加系統設定表，關於系統部分設定未來皆會規劃到該表中

---

## 功能

### 支援的 AI 平台
- OpenAI
- Ollama
- Anthropic
- Gemini
- Grok

### 支援的向量資料庫
- InMemoryEmbeddingStore（預設）
- Chroma
- Milvus

### 其他功能
- 可由資料庫設定 LLM 模型並動態切換
- 支援 MCP Server 整合（[MCP Server 範例](https://github.com/a09090443/mcp-servers)）
- 支援 RAG（檢索增強生成）
- 視覺化 Workflow 引擎（n8n-like，節點＋連線自訂流程；定義 CRUD 與畫布驗證已完成）
- RBAC 權限管理
- Swagger UI（dev / sit 環境）

## 內建 Tools 工具

| 工具名稱 | 工具類型 |
|:---------|:---------|
| Google 搜尋引擎 | Web Search |
| Tavily 搜尋引擎 | Web Search |
| Date 日期 | Date |
| Text2SQL | Other |

---

## 目錄結構

```
bestpartner/
├── bestpartner-service/                    # 主服務模組 (Quarkus)
│   └── src/main/kotlin/tw/zipe/bastpartner/
│       ├── assistant/                      # AI Assistant 介面
│       ├── builder/
│       │   ├── llm/                        # LLM 動態建構器
│       │   └── vector/                     # 向量資料庫建構器
│       ├── config/
│       │   ├── chatmodel/
│       │   ├── embedding/
│       │   ├── security/
│       │   └── vector/
│       ├── constant/
│       ├── converter/
│       ├── dto/
│       ├── entity/
│       ├── enumerate/
│       ├── exception/
│       ├── filter/
│       ├── form/
│       ├── model/
│       ├── properties/
│       ├── provider/
│       ├── repository/
│       ├── resource/
│       ├── service/
│       ├── tool/
│       └── util/
├── bestpartner-mcp-servers/                # MCP Server 範例
│   ├── quarkus-example/                    # Quarkus MCP Server 範例
│   └── spring-example/                     # Spring Boot MCP Server 範例
├── bestpartner-ui/                         # 前端 (Vue 3 + Vite + TS)
│   └── src/
│       ├── api/                            # 後端 API client（axios 攔截器、workflow）
│       ├── components/
│       │   ├── canvas/                     # Vue Flow 節點面板與自訂節點
│       │   └── inspector/                  # 屬性面板與 JSON 設定編輯器
│       ├── composables/                    # nodeKey、畫布↔DTO 轉換、畫布驗證
│       ├── constants/                      # 節點型別顯示 meta
│       ├── router/                         # 路由與登入守衛
│       ├── stores/                         # Pinia（auth、workflow）
│       ├── types/                          # 對應後端 DTO 的 TS 型別
│       └── views/                          # 登入、列表、Workflow 編輯器
├── docs/
│   ├── docker/                             # Docker Compose 設定
│   ├── postman/                            # Postman Collection
│   ├── rag/                                # RAG 測試文件
│   ├── sample/                             # Text2SQL 範例
│   └── sql/                                # 資料庫 Schema
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── README.md
└── settings.gradle.kts
```

---

## 事前準備

### AI 平台（擇一）

**選項 1：Ollama（本地模型）**
- 安裝 Ollama：[參考教學](https://blog.darkthread.net/blog/ollam-open-webui/)
- 下載所需 LLM 模型

**選項 2：雲端 AI 平台**
- OpenAI：申請 API Key，[官網](https://openai.com/)
- Anthropic：申請 API Key，[官網](https://www.anthropic.com/)
- Gemini：申請 API Key，[官網](https://ai.google.dev/)
- Grok：申請 API Key，[官網](https://x.ai/)

### 向量資料庫（擇一，建議使用 Docker）

預設使用 **InMemoryEmbeddingStore**，無需額外安裝。

- **Chroma**：[安裝說明](https://cookbook.chromadb.dev/core/install/#chroma-jsts-client)
- **Milvus**：[安裝說明](https://www.milvus-io.com/getstarted/standalone/install_standalone-docker)

### PostgreSQL 資料庫

- 安裝 PostgreSQL：[官方文件](https://www.postgresql.org/download/)
- 安裝完成後，建立資料庫 `pgdb`，並依序執行 `docs/sql/bestpartner-ddl.sql` 與 `docs/sql/bestpartner-init-data.sql`

---

## 開發環境

| 類別 | 版本 |
|------|------|
| 語言 | Kotlin 2.1.0 |
| 框架 | Quarkus 3.21.0 |
| AI 函式庫 | Langchain4j 1.13.0 |
| JDK | OpenJDK 21 |
| 資料庫 | PostgreSQL latest |
| 建置工具 | Gradle latest |

---

## 程式執行注意事項

- 目前使用 PostgreSQL Database，可在 `application.properties` 中設定連線資訊
- 無自動 Flyway 遷移（`migrate-at-start=false`），需手動依序執行 `docs/sql/bestpartner-ddl.sql` 與 `docs/sql/bestpartner-init-data.sql`

### 敏感金鑰管理（.env 檔案）

專案使用 `.env` 管理 API Key 等敏感設定，避免明文寫入版本控制：

```bash
# 1. 複製範本
cp .env.example .env

# 2. 編輯 .env，填入實際金鑰
# OPENROUTER_API_KEY=your-openrouter-api-key-here
```

`.env` 已列入 `.gitignore`，不會被提交；`.env.example` 為範本，提交至版本控制供參考。Quarkus 啟動時會自動載入 `.env`。

---

## 文件服務系統（docs-site）

BestPartner 提供基於 [Docusaurus](https://docusaurus.io/) 的線上文件網站，位於 `docs-site/` 目錄。

### 環境需求

| 類別 | 版本 |
|------|------|
| Node.js | >= 20.0 |
| npm / yarn | 最新版 |

### 啟動開發伺服器

```bash
cd docs-site
npm install
npm start
```

文件網站預設埠：**port 3000**，啟動後瀏覽器會自動開啟 `http://localhost:3000`

### 建置靜態網站

```bash
cd docs-site
npm run build
```

建置產物輸出至 `docs-site/build/` 目錄。

### 預覽靜態建置結果

```bash
cd docs-site
npm run serve
```

---

## 前端應用（bestpartner-ui）

BestPartner 提供基於 Vue 3 的前端應用，位於 `bestpartner-ui/` 目錄，提供登入、Workflow 列表與 n8n-like 視覺化 Workflow 編輯器（Vue Flow 畫布、拖放節點、屬性面板）。

### 環境需求

| 類別 | 版本 |
|------|------|
| Node.js | >= 20.0 |
| 建置工具 | Vite |
| 框架 | Vue 3 + TypeScript + Pinia + Vue Router + Element Plus + Vue Flow |

### 啟動開發伺服器

```bash
cd bestpartner-ui
npm install
npm run dev
```

開發伺服器透過 Vite proxy 將 API 請求轉發至後端（預設 port 80）。

### 測試與建置

```bash
cd bestpartner-ui
npm run test     # Vitest 單元測試
npm run build    # vue-tsc 型別檢查 + production 建置
```

---

## Swagger UI

在 **dev** 與 **sit** 環境，服務啟動後可透過以下位址存取 API 文件：

| 路徑 | 說明 |
|------|------|
| `http://localhost:80/swagger-ui` | Swagger UI 互動介面 |
| `http://localhost:80/q/openapi` | OpenAPI 規格（YAML） |

### 使用 JWT 認證

1. 呼叫 `POST /login/`（帳號：`admin/admin`）取得 JWT Token
2. 開啟 Swagger UI，點擊右上角 **Authorize** 按鈕
3. 在 `bearerAuth` 欄位貼入 Token（不需加 `Bearer` 前綴）
4. 即可直接測試所有受保護的 API

---

## 程式打包執行

1. 切換至 `bestpartner-service` 目錄，執行：
   ```bash
   ./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=${profile}
   ```
   `${profile}` 可替換為 `dev`、`sit`、`prod`

2. 打包完成後，JAR 位於：
   ```
   bestpartner-service/build/bestpartner-service-0.1.7-SNAPSHOT-runner.jar
   ```

3. 執行：
   ```bash
   java -jar bestpartner-service-0.1.7-SNAPSHOT-runner.jar
   ```

服務預設埠：**port 80**

---

## 開發紀錄

* **2025.xx.xx BestPartner 0.1.7 版本完成**
  + 升級 Quarkus 至 3.21.0 版本
  + 升級 Langchain4j 至 1.4.0 版本
  + 升級 Langchain4j 至 1.12.2 版本

* **2025.04.25 BestPartner 0.1.6 版本完成**
  + 完成 MCP Server 支援
  + 完成 Anthropic 支援
  + 完成 Gemini 支援
  + 完成 Grok 支援
  + 移除 Flyway 資料庫版控功能，只提供完整的 SQL 檔案
  + 提供 Quarkus 和 Spring MCP Server 範例

* **2025.03.14 BestPartner 0.1.5 版本完成**
  + 完成 TEXT2SQL 工具
  + 完成知識庫的功能
  + 完成 RAG 檔案上傳、刪除和搜尋功能
  + 修改動態呼叫及建立工具邏輯

* **2025.01.16 BestPartner 0.1.4 版本完成**
  + 完成 LLM 可動態調用 Tools 工具測試
  + API 新增權限管理
  + 修正 LLM Streaming 錯誤問題
  + 新增 Tavily 網路搜尋引擎工具
  + 修改 log 輸出設定
  + 修正 JWT 錯誤問題

* **2024.12.11 BestPartner 0.1.3 版本完成**
  + 支援自製 LLM 工具註冊
  + 支援動態呼叫自製 LLM 工具
  + Database 由 H2 改為 MySQL

* **2024.11.03 BestPartner 0.1.2 版本完成**
  + 支援 RBAC 功能
  + 增加系統設定表

* **2024.10.23 BestPartner 0.1.1 版本完成**
  + 新增 Chroma、Milvus 向量資料庫支援
  + 新增 RAG 功能
  + 新增 H2 Database 支援

* **2024.10.15 BestPartner 0.1.0 初版完成**

---

## 版權聲明

可以免費學習使用，個人可以免費直接取用，商業應用請聯絡作者授權，測試文件皆為自行建立或網路公開資料。

## 備註

* Langchain4j 官方文件：[連結](https://docs.langchain4j.dev/)
* 如有興趣想討論，或有任何想法想加入開發，可聯絡作者，信箱：zipe.daden@gmail.com
