[![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

BestPartner project
======================
## 說明
1. 該專案為一個AI應用大平台，可動態建立 AI agent 並支援多種 AI 模型
2. 各API範例可參考Postman Collection: [連結](https://github.com/a09090443/bestpartner/blob/master/docs/postman/basepartner.postman_collection.json)
3. 未來目標為類似 Dify 或 Coze 平台，可自行建立 AI agent 並支援多種 AI 模型

## 1.5 版本變更說明
1. 修改工具建立邏輯
2. 新增 TEXT2SQL 工具
3. 修改LLM工具建立邏輯，當工具需要使用者自定義內容時須帶入toolSettingIds參數，而工具無須自定義內容時帶入參數toolIds，可參考 postman 中 custom_assistant_chat(text2sql) 範例
4. 修改RAG上傳檔案及刪除檔案時的邏輯
5. 修改LLM讀取知識庫錯誤問題
6. 新增對於向量資料庫搜尋功能

## 1.4 版本變更說明
1. 設定各API權限管理
2. 修正動態建立LLM模型錯誤問題
3. 修正Tools工具錯誤問題
4. 新增 tavily 網路搜尋引擎工具

## 1.3 版本變更說明
1. 廢除 H2 db 改為使用 Mysql db
2. 增加 llm tools 可註冊自製工具
3. 增加 llm tools 使用者自訂一個工具設定值

## 1.2 版本變更說明
1. 支援 RBAC 功能，請先使用 http://localhost/login/ 取得 jwt 令牌，可參考 postman 中 auth -> login 範例，管理員(admin/admin)
2. 所有 API 還未有權限管理，之後版本會加入權限管理
3. 增加系統設定表，關於系統部分設定未來皆會規劃到該表中

## 功能
1. 支援 OpenAI 及 Ollama 平台
2. 支援 Chroma 及 Milvus 向量資料庫
3. 可由資料庫設定 LLM 模型並可動態切換

## 內建 Tools 工具
| 工具名稱        | 工具類型       |
|:------------|:-----------|
| Google 搜尋引擎 | Web Search |
| Tavily 搜尋引擎 | Web Search |
| Date 日期     | Date       |
| Text2SQL    | Other      |

## 目錄結構
```
bestpartner
├─bestpartner-service
├─src
├─main
│  ├─docker
│  ├─java
│  │  └─tw
│  │      └─zipe
│  │          └─basepartner
│  ├─kotlin
│  │  └─tw
│  │      └─zipe
│  │          └─basepartner
│  │              ├─assistant
│  │              ├─builder
│  │              │  ├─aigcmodel
│  │              │  └─vector
│  │              ├─config
│  │              │  ├─chatmodel
│  │              │  ├─embedding
│  │              │  └─vector
│  │              ├─constatnt
│  │              ├─converter
│  │              ├─dto
│  │              ├─entity
│  │              ├─enumerate
│  │              ├─exception
│  │              ├─filter
│  │              ├─form
│  │              ├─model
│  │              ├─properties
│  │              ├─provider
│  │              ├─repository
│  │              ├─resource
│  │              ├─service
│  │              ├─tool
│  │              └─util
│  └─resources
│      ├─cert
│      └─db
│          └─migration
├─docs
├─ .gitignore
├─ build.gradle.kts
├─ gradle.properties
├─ gradlew
├─ gradlew.bat
├─ README.md
└─ settings.gradle.kts
```

## 事前準備
模組平台:

選擇1. 安裝 Ollama 並下載 LLM models
- Ollama 安裝: [連結](https://blog.darkthread.net/blog/ollam-open-webui/)
- Docker compose 運行: [連結](https://blog.darkthread.net/blog/ollam-open-webui/)

選擇2. OpenAI 申請 api key [官網](https://openai.com/)

向量資料庫(建議使用 docker 並可擇一使用):

預設使用InMemoryEmbeddingStore

安裝 Chroma
- Chroma 安裝: [連結](https://cookbook.chromadb.dev/core/install/#chroma-jsts-client)

安裝 Milvus
- Milvus 安裝: [連結](https://www.milvus-io.com/getstarted/standalone/install_standalone-docker)

## 開發環境
* OpenJDK 21
* Ollama latest
* LLM models
* Chroma latest
* Milvus latest
* Kotlin 2.1.0
* Quarkus 3.18.4
* Langchain4j 1.0.0-beta1
* Gradle latest
* Postman latest
* Mysql Database latest

## 程式執行注意事項
- 目前使用Mysql Database，可在application.properties中設定
- 初始化資料庫可使用Flyway，可在application.properties中設定，quarkus.flyway.migrate-at-start = true

## 程式打包執行
1. 切換至 bestpartner-service 目錄,執行 gradle clean build -x test -Dquarkus.package.type=uber-jar -Dquarkus.profile=${profile}
- ${profile} 可取代 dev, sit, prod
2. 打包完成後，jar file產生位置:bestpartner-service/build/bestpartner-service-0.10-SNAPSHOT-runner.jar
3. 執行 java -jar bestpartner-service-0.10-SNAPSHOT-runner.jar
4. 運行畫面:(http://localhost)
![](docs/images/service-start.png)

## 開發紀錄
* 2025.03.14 BestPartner 0.1.5 版本完成
  + 完成 TEXT2SQL 工具
  + 完成知識庫的功能
  + 完成 RAG 檔案上傳，刪除和搜尋功能
  + 修改動態呼叫及建立工具邏輯

* 2025.01.16 BestPartner 0.1.4 版本完成
  + 完成 LLM 可動態調用 Tools 工具測試
  + API 新增權限管理
  + 修正 LLM streaming 錯誤問題
  + 新增 tavily 網路搜尋引擎工具
  + 修改 log 輸出設定
  + 修正 jwt 錯誤問題

* 2024.12.11 BestPartner 0.1.3 版本完成
  + 支援自製LLM工具註冊
  + 支援動態呼叫自製LLM工具
  + Database 由 H2 改為 Mysql

* 2024.11.3 BestPartner 0.1.2 版本完成
  + 支援 RBAC 功能
  + 增加系統設定表

* 2024.10.23 BestPartner 0.1.1 版本完成
  + 新增 Chroma、Milvus 向量資料庫支援
  + 新增 RAG 功能
  + 新增 H2 Database 支援

---
* 2024.10.15 BestPartner 0.1.0 初版完成

## 版權聲明
可以免費學習使用，個人可以免費是接取使用，商業應用請聯絡作者授權，測試文件皆為自行建立或者網路公開資料。

## 備註
* Langchain4j 官網說明:[連結](https://docs.langchain4j.dev/)
* 如有有興趣想討論，或有任何想法想加入開發，可跟我聯絡，信箱:zipe.daden@gmail.com
