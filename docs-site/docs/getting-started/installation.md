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

## 3. 設定 application.properties

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

依照你選擇的 AI 模型填入相對應的設定：

```properties
# OpenAI
openai.api.key=sk-xxxxxxxxxxxxxxxx

# Anthropic
anthropic.api.key=sk-ant-xxxxxxxxxxxxxxxx

# Google Gemini
gemini.api.key=AIzaxxxxxxxxxxxxxxxx

# Grok
grok.api.key=xai-xxxxxxxxxxxxxxxx

# Ollama（本地端，預設無需金鑰）
ollama.base.url=http://localhost:11434
```

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

## 4. 建置專案

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

## 5. 使用 Flyway 初始化資料庫（選用）

如果想使用 Flyway 自動執行資料庫遷移，在 `application.properties` 中開啟：

```properties
quarkus.flyway.migrate-at-start=true
```

:::info Flyway 現況
Flyway 設定預設為關閉（`quarkus.flyway.migrate-at-start=false`），但設定仍保留在 `application-sit.properties` 和 `application-prod.properties` 中。建議使用 `docs/sql/bestpartner-ddl.sql` 手動初始化資料庫。
:::
