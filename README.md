[![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

BestPartner project
======================
## 說明
1. 該專案為一個AI應用大平台，會結合客製 AI agent
2. 目前僅支援 OpenAI 及 Ollama 平台

## 目錄結構
```
D:.
├─bestpartner-service
└─src
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
│  │              ├─dto
│  │              ├─entity
│  │              ├─enumerate
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
│      └─db
│          └─migration
├─native-test
│  └─kotlin
│      └─tw
│          └─zipe
│              └─basepartner
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
* Kotlin 2.0.10
* Quarkus 3.15.1
* Langchain4j 0.35.0
* Gradle latest
* Postman latest
* H2 Database latest

## 程式打包執行
1. 切換至 bestpartner-service 目錄,執行 gradle clean build -x test -Dquarkus.package.type=uber-jar -Dquarkus.profile=${profile}
- ${profile} 可取代 dev, sit, prod
2. 打包完成後，jar file產生位置:bestpartner-service/build/bestpartner-service-0.10-SNAPSHOT-runner.jar
3. 執行 java -jar bestpartner-service-0.10-SNAPSHOT-runner.jar
4. 運行畫面:(http://localhost)
![](docs/images/service-start.png)

## 開發紀錄
* 2024.10.15 BestPartner 0.1.0 初版完成

## 版權聲明
可以免費學習使用，個人可以免費是接取使用，商業應用請聯絡作者授權，測試文件皆為自行建立或者網路公開資料。

## 備註
* Langchain4j 官網說明:[連結](https://docs.langchain4j.dev/)
* 如有有興趣想討論，或有任何想法想加入開發，可跟我聯絡，信箱:zipe.daden@gmail.com
