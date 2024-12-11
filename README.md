[![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

BestPartner project
======================
## 說明
1. 該專案為一個AI應用大平台，可動態建立 AI agent 並支援多種 AI 模型
2. 各API範例可參考Postman Collection: [連結](https://github.com/a09090443/bestpartner/blob/master/docs/postman/basepartner.postman_collection.json)
3. 未來目標為類似 Dify 或 Langflow 平台，可自行建立 AI agent 並支援多種 AI 模型

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
* Quarkus 3.15.1
* Langchain4j 0.36.2
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
* 目前以 h2 database 為預設資料庫，之後會支援 Mysql 資料庫
