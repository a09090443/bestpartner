---
sidebar_position: 2
---

# 安裝與設定

## 1. 取得原始碼

```bash
git clone https://github.com/a09090443/bestpartner.git
cd bestpartner
```

## 2. 初始化資料庫

確認 PostgreSQL 已啟動，建立資料庫並匯入初始資料：

```bash
psql -U pguser -d pgdb -f docs/sql/bestpartner-ddl.sql
psql -U pguser -d pgdb -f docs/sql/bestpartner-init-data.sql
```

## 3. 建立 .env 檔案（敏感金鑰）

專案使用 `.env` 檔案管理敏感的 API Key，避免明文寫入版本控制。

```bash
cp .env.example .env
```

編輯 `.env`，填入實際金鑰：

```dotenv
# OpenRouter API Key
OPENROUTER_API_KEY=your-openrouter-api-key-here
```

> `.env` 已列入 `.gitignore`，不會被提交。`.env.example` 為範本，提交至版本控制供參考。

Quarkus 啟動時會自動載入 `.env`，`application.properties` 中以 `${VARIABLE_NAME}` 引用對應的值。

---

## 4. 設定 application.properties

切換至主服務目錄：

```bash
cd bestpartner-service/src/main/resources
```

編輯 `application.properties`（或依環境編輯 `application-sit.properties` / `application-prod.properties`）：

### 資料庫設定

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=pguser
quarkus.datasource.password=pgpass
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/pgdb
```

### AI 模型設定

API Key 請透過 `.env` 檔案管理（參考步驟 3），`application.properties` 以 `${VARIABLE_NAME}` 引用。

**`.env`（填入實際金鑰，不提交）**

```dotenv
# OpenAI
OPENAI_API_KEY=your-openai-api-key-here

# OpenRouter
OPENROUTER_API_KEY=your-openrouter-api-key-here
```

**`application.properties`（使用環境變數引用）**

```properties
# OpenAI 系統預設模型設定
%dev.ai-platform.openai.api-key=${OPENAI_API_KEY}
%dev.ai-platform.openai.model-name=gpt-4o
%dev.ai-platform.openai.temperature=0.7
%dev.ai-platform.openai.max-tokens=4096
%dev.ai-platform.openai.timeout=60s

# OpenRouter 系統預設模型設定
%dev.ai-platform.openrouter.api-key=${OPENROUTER_API_KEY}
%dev.ai-platform.openrouter.model-name=openai/gpt-4o
%dev.ai-platform.openrouter.temperature=0.7
%dev.ai-platform.openrouter.max-tokens=4096
%dev.ai-platform.openrouter.timeout=60s

# Ollama（本地端，無需 API Key）
%dev.ai-platform.ollama.url=http://localhost:11434
%dev.ai-platform.ollama.model-name=llama3
%dev.ai-platform.ollama.timeout=60s
```

> 其他平台（Anthropic、Gemini、Grok）的 API Key 透過 ADMIN 介面的 LLM Setting 儲存於資料庫，無需在設定檔中設定。

### 向量資料庫設定

```properties
# 預設使用 InMemory，若要切換請設定以下其一：

# Chroma
chroma.url=http://localhost:8000

# Milvus
milvus.host=localhost
milvus.port=19530
```

### JWT 安全設定

```properties
# JWT 相關設定（建議在 prod 環境更換密鑰）
mp.jwt.verify.issuer=bast-partner
mp.jwt.verify.publickey.location=publicKey.pem
smallrye.jwt.sign.key.location=privateKey.pem
```

### 加解密設定

```properties
# 敏感欄位（如向量資料庫密碼）AES-GCM 加密金鑰
# ⚠️ 正式環境請務必替換為高強度隨機字串（建議 32 字元以上），且不同環境使用不同金鑰
crypto.secret-key=changeme-please-replace-in-production
```

## 5. 建置專案

回到 `bestpartner-service` 目錄執行建置：

```bash
cd bestpartner-service

# 開發環境
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=dev

# 測試環境
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=sit

# 生產環境
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=prod
```

建置完成後，JAR 檔案位置：

```
bestpartner-service/build/bestpartner-service-0.1.7-SNAPSHOT-runner.jar
```
