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
- 視覺化 Workflow 引擎（n8n-like，節點＋連線自訂流程；定義 CRUD、畫布驗證與執行引擎已完成，13 種節點型別含條件分支／迴圈／GraalJS 程式碼／資料轉換；LLM 節點採 Agent 模式，工具／MCP／Skill 以獨立節點掛載到工具埠由 LLM 自主呼叫，提問內容可抽成獨立的提示詞節點連到提示埠，搭配分支即可一顆 LLM 對應多種對話）
- RBAC 權限管理
- Swagger UI（dev / docker / sit 環境；uat / prod 關閉）

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
| AI 函式庫 | Langchain4j 1.17.2 |
| JDK | OpenJDK 21 |
| 資料庫 | PostgreSQL latest |
| 建置工具 | Gradle latest |

---

## 程式執行注意事項

- 目前使用 PostgreSQL Database，連線資訊由 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 環境變數設定（見下方 `.env` 說明）
- 無 Flyway，需手動依序執行 `docs/sql/bestpartner-ddl.sql` 與 `docs/sql/bestpartner-init-data.sql`

### 環境設定與敏感金鑰（.env 檔案）

專案以 `.env` 管理**跨環境會變動的設定**（連線位址、路徑）與 **API Key 等機密**，避免明文寫入版本控制：

```bash
# 1. 複製範本
cp .env.example .env

# 2. 編輯 .env，填入實際值
# OPENROUTER_API_KEY=your-openrouter-api-key-here
```

`application.properties` 以 `${ENV_VAR:預設值}` 引用，**不設定任何環境變數也能直接啟動**（每個鍵都有本機開發預設值）。

多環境各用一份 `.env.<profile>`：

| 檔案 | profile | 用途 |
|------|---------|------|
| `.env` | dev | 本機開發（Quarkus 自動載入） |
| `.env.docker` | docker | 本機容器測試 |
| `.env.sit` | sit | 整合測試 |
| `.env.uat` | uat | 使用者驗收 |
| `.env.prod` | prod | 生產 |

`.env` 與 `.env.*` 已列入 `.gitignore`；只有 `.env.example` 進版控，該檔列出所有可用變數與預設值。

> ⚠️ `CRYPTO_SECRET_KEY` 在 sit 以上環境務必替換為高強度隨機字串，且各環境不同。變更此值會使既有加密資料無法解密。

### 設定檔機密值加密

`.env.<profile>` 中的機密（`DB_PASSWORD`、`OPENROUTER_API_KEY` 等）可寫成密文，啟動時自動解密：

```dotenv
DB_PASSWORD=${enc::<密文>}
```

產生密文（金鑰與明文走環境變數，避免留下 shell 歷史紀錄）：

```powershell
$env:CONFIG_ENCRYPTION_KEY='<該環境的根金鑰>'
$env:CONFIG_SECRET_VALUE='<機密明文>'
cd bestpartner-service; ./gradlew encryptConfigSecret -q
```

> ⚠️ **這個機制擋得住什麼**：解密金鑰 `CONFIG_ENCRYPTION_KEY` 就放在同一個 `.env.<profile>` 內，
> 所以能防的是「明文密碼被瞥見、截圖、誤貼、寫進日誌」與「設定檔不得存明文密碼」的要求；
> **不防 `.env` 檔案本身外洩**（拿到檔案即可解密）。要防到那個層級，金鑰須改為與 `.env` 分離
> （獨立檔案、Vault、KMS），部署時單獨注入。
> ⚠️ 解密根金鑰 `CONFIG_ENCRYPTION_KEY` 與資料庫欄位金鑰 `CRYPTO_SECRET_KEY` **是兩把不同的金鑰**，請勿填成相同值。
> ⚠️ **金鑰遺失即無法解密既有密文**，需以原始明文重新加密。
> ⚠️ `application.properties` 的 datasource 預設值也已密文化（`${DB_USERNAME:${enc::<密文>}}`），
> 使版控中的設定檔不出現 `pguser` / `pgpass` 字樣。該密文以**檔內的預設金鑰**加密，
> 屬**衛生措施而非安全措施**（金鑰與密文同在版控，clone 即可解密）；
> 全新 clone 不帶任何 `.env` 仍可直接啟動。

詳見 [`.claude/rules/configuration-and-profiles.md`](.claude/rules/configuration-and-profiles.md)。

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

BestPartner 提供基於 Vue 3 的前端應用，位於 `bestpartner-ui/` 目錄，提供登入、Workflow 列表與 n8n-like 視覺化 Workflow 編輯器（Vue Flow 深色畫布、分類節點庫、型別化屬性面板與自動排版）。

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
   `${profile}` 可替換為 `dev`、`docker`、`sit`、`uat`、`prod`

2. 打包完成後，JAR 位於：
   ```
   bestpartner-service/build/bestpartner-service-0.1.8-runner.jar
   ```

3. 執行：
   ```bash
   java -jar bestpartner-service-0.1.8-runner.jar
   ```

服務預設埠：**port 80**

---

## Docker 部署

**一份 image 跑所有環境**，環境差異由 runtime 決定，不為個別環境重建 image。
**打包指令不隨環境變化**——環境是在 `docker run` 時才選的。

### 打包

```bash
./scripts/docker-package.sh          # Linux / macOS（原生 bash）
pwsh ./scripts/docker-package.ps1    # Windows
```

一支腳本涵蓋建置、啟動驗證、清理與帶時戳匯出 tar。

常用參數：`--no-tar` / `-NoTar`（只建 image）、`--verify-port <埠>` / `-VerifyPort <埠>`
（驗證埠被占用時）。詳見 [`scripts/README.md`](scripts/README.md)。

手動執行等同於：

```bash
cd bestpartner-service
./gradlew clean build -x test -Dquarkus.package.type=uber-jar \
  -Dorg.gradle.daemon=false -Dquarkus.profile=prod
docker build -f src/main/docker/Dockerfile.uber-jar -t bestpartner-service:latest .
```

### 部署（這裡才分環境）

```bash
docker run -d -p 80:80 \
  -e QUARKUS_PROFILE=uat \
  --env-file .env.uat \
  -v bestpartner-data:/opt/bestpartner \
  bestpartner-service:latest
```

| 參數 | 決定 |
|------|------|
| `-e QUARKUS_PROFILE=<env>` | 行為：Swagger 開關、SQL 日誌、log 等級 |
| `--env-file .env.<env>` | 連線：DB 位址、密文密碼、路徑，以及解密金鑰 |

> `.env` 中 `${enc::...}` 的密文由同檔的 `CONFIG_ENCRYPTION_KEY` 解密，無需額外注入。
> 金鑰錯誤或缺漏時，啟動會以 `AEADBadTagException` 明確失敗，不會靜默使用密文當密碼。

> 建置 profile 固定用 `prod`：它會成為 image 的預設 runtime profile，
> 讓部署時漏帶 `QUARKUS_PROFILE` 也落在「Swagger 關閉、log INFO」的安全側。

搬遷至其他機器：

```bash
docker save -o bestpartner-service-0.1.8-prod-$(date +%Y%m%d%H%M).tar \
  bestpartner-service:latest
# 目標機器
docker load -i bestpartner-service-0.1.8-prod-202607162330.tar
```

檔名格式：`bestpartner-service-<版本>-<建置profile>-<YYYYMMDDHHmm>.tar`

> 檔名的 `-prod` 是**建置 profile**，不是部署目標。這份 tar 全環境通用，
> 跑哪個環境由 `QUARKUS_PROFILE` 與 `.env.<profile>` 決定。

> ⚠️ tar 內含 JWT 簽章私鑰（`privateKey.pem` 隨 uber-jar 打包），等同憑證，勿在不受控管道流通。

存活探測請用 `/view/chat`；`/q/openapi` 與 `/swagger-ui` 在 prod 回 404 屬正確行為。

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
