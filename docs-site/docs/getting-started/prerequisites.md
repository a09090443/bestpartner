---
sidebar_position: 1
---

# 環境需求

在開始使用 BestPartner 之前，請確認以下環境已準備就緒。

## 必要環境

### Java 執行環境

- **OpenJDK 21** 或以上版本

```bash
java -version
# 預期輸出: openjdk version "21.x.x"
```

### 資料庫

**PostgreSQL**（必要）

BestPartner 使用 PostgreSQL 儲存所有設定資料。

```bash
# 使用 Docker 快速啟動（建議）
docker run -d \
  --name bestpartner-postgres \
  -e POSTGRES_USER=pguser \
  -e POSTGRES_PASSWORD=pgpass \
  -e POSTGRES_DB=pgdb \
  -p 5432:5432 \
  postgres:16
```

或參考 [PostgreSQL 官方安裝文件](https://www.postgresql.org/download/) 手動安裝。

安裝完成後，匯入初始化 SQL：

```bash
psql -U pguser -d pgdb -f docs/sql/bestpartner-ddl.sql
psql -U pguser -d pgdb -f docs/sql/bestpartner-init-data.sql
```

## AI 模型（擇一）

### 選項一：Ollama（本地端，免費）

適合不想申請雲端 API Key 的開發者。

```bash
# 安裝 Ollama 後下載模型
ollama pull llama3.2
ollama pull nomic-embed-text  # 嵌入模型
```

使用 Docker 啟動：

```bash
cd docs/docker/ollama
docker-compose up -d
```

### 選項二：OpenAI（雲端）

申請 API Key：[https://platform.openai.com/](https://platform.openai.com/)

### 選項三：其他雲端模型

| 提供者 | 申請頁面 |
|--------|---------|
| Anthropic Claude | https://console.anthropic.com/ |
| Google Gemini | https://aistudio.google.com/ |
| Grok | https://console.x.ai/ |

## 向量資料庫（擇一，可選）

預設使用 **InMemory**（記憶體），無需額外安裝，重啟後資料會消失。

若需要持久化向量資料，擇一安裝：

### Chroma

```bash
cd docs/docker/chroma
docker-compose up -d
# 預設連接埠: 8000
```

### Milvus

```bash
cd docs/docker/milvus
docker-compose up -d
# 預設連接埠: 19530
```

## 建置工具

- **Gradle** — 已包含在專案中（`gradlew`），無需另外安裝
- **Git** — 用於取得原始碼

## 開發工具（建議）

- **IntelliJ IDEA** — 最佳 Kotlin/Quarkus 開發環境
- **Postman** — 測試 API，可匯入 `docs/postman/basepartner.postman_collection.json`
- **Docker Desktop** — 方便管理本地服務
