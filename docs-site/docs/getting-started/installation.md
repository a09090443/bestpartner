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

確認 MySQL 已啟動，建立資料庫並匯入初始資料：

```sql
CREATE DATABASE bestpartner CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
mysql -u root -p bestpartner < docs/sql/bestpartner.sql
```

## 3. 設定 application.properties

切換至主服務目錄：

```bash
cd bestpartner-service/src/main/resources
```

編輯 `application.properties`（或依環境編輯 `application-dev.properties` / `application-prod.properties`）：

### 資料庫設定

```properties
quarkus.datasource.username=root
quarkus.datasource.password=your_password
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3306/bestpartner
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
mp.jwt.verify.publickey.location=cert/public.pem
smallrye.jwt.sign.key.location=cert/private.pem
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
bestpartner-service/build/bestpartner-service-0.10-SNAPSHOT-runner.jar
```

## 5. 使用 Flyway 初始化資料庫（選用）

如果想使用 Flyway 自動執行資料庫遷移，在 `application.properties` 中開啟：

```properties
quarkus.flyway.migrate-at-start=true
```

:::caution 注意
目前版本（0.1.6 以後）已移除 Flyway，請使用 `docs/sql/bestpartner.sql` 手動初始化資料庫。
:::
