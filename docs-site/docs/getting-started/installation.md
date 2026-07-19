---
sidebar_position: 2
description: BestPartner 安裝與設定步驟，涵蓋取得原始碼、初始化 PostgreSQL 資料庫、匯入初始資料與服務設定。
keywords: [安裝, 設定, 資料庫初始化, PostgreSQL, 建置]
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

## 3. 建立 .env 檔案（環境設定與敏感金鑰）

專案以 `.env` 檔案管理**跨環境會變動的設定**（連線位址、路徑）與**敏感金鑰**，避免明文寫入版本控制。

```bash
cp .env.example .env
```

`.env.example` 是唯一進版控的範本，列出所有可用的環境變數與預設值。編輯 `.env` 填入本機實際值：

```dotenv
OPENROUTER_API_KEY=your-openrouter-api-key-here
CRYPTO_SECRET_KEY=changeme-please-replace-in-production
```

Quarkus 啟動時會自動載入 `.env`，`application.properties` 中以 `${VARIABLE_NAME:預設值}` 引用。
**未設定任何環境變數也能直接啟動**——每個鍵都帶有本機開發用的預設值。

### 多環境設定

不同環境各自使用一份 `.env.<profile>`：

```bash
cp .env.example .env.uat    # 再填入 uat 的連線位址與金鑰
```

| 檔案 | 對應 profile | 用途 |
|------|-------------|------|
| `.env` | dev | 本機開發（Quarkus 自動載入） |
| `.env.docker` | docker | 本機容器測試 |
| `.env.sit` | sit | 整合測試 |
| `.env.uat` | uat | 使用者驗收 |
| `.env.prod` | prod | 生產 |

容器執行時以 `--env-file` 指定：

```bash
docker run -d -p 80:80 -e QUARKUS_PROFILE=uat --env-file .env.uat bestpartner-service:latest
```

:::caution
- `.env` 與 `.env.*` 皆已列入 `.gitignore`，不會被提交；只有 `.env.example` 進版控。
- `--env-file` **不做 shell 引號處理**：值一律不加引號，且不支援 `${VAR}` 巢狀展開。
- `CRYPTO_SECRET_KEY` 在 sit 以上環境務必替換為高強度隨機字串，且各環境不同。
  變更此值會使既有加密資料（如 `llm_setting.api_key`）無法解密。
:::

### 機密值加密（sit 以上環境建議）

`.env.<profile>` 中的機密可寫成密文，服務啟動時由 SmallRye Config 自動解密：

```dotenv
DB_PASSWORD=${enc::<密文>}
OPENROUTER_API_KEY=${enc::<密文>}
```

產生密文（金鑰與明文走環境變數，避免留下 shell 歷史紀錄）：

```powershell
$env:CONFIG_ENCRYPTION_KEY='<該環境的根金鑰>'
$env:CONFIG_SECRET_VALUE='<機密明文>'
cd bestpartner-service; ./gradlew encryptConfigSecret -q
```

驗證密文可解回原值（`CONFIG_SECRET_VALUE` 改放**密文本體**，不含 `${enc::}` 外框）：

```powershell
$env:CONFIG_ENCRYPTION_KEY='<該環境的根金鑰>'
$env:CONFIG_SECRET_VALUE='<密文本體>'
cd bestpartner-service; ./gradlew decryptConfigSecret -q
```

金鑰或密文不符時以 `AEADBadTagException: Tag mismatch` 失敗（GCM 完整性驗證），
不會回傳錯誤的明文。

:::info 為什麼這不與上面「不支援 `${VAR}` 巢狀展開」衝突？
那句話指的是 **`docker --env-file` 這一層**：它把值當字面字串原樣傳給容器，不做 shell 展開。
`${enc::...}` 則是 **Quarkus 應用內** 的 config expression，
由 SmallRye Config 在讀取設定值時展開，兩者屬不同層級、互不影響。
:::

:::warning
- 解密根金鑰 `CONFIG_ENCRYPTION_KEY` 與資料欄位金鑰 `CRYPTO_SECRET_KEY` **是兩把不同的金鑰**，
  刻意分離以便各自輪換，請勿填成相同值。
- 根金鑰 `CONFIG_ENCRYPTION_KEY` 就放在同一份 `.env.<profile>` 內。
  因此本機制能防的是「明文密碼被瞥見、截圖、誤貼、寫進日誌」與
  「設定檔不得存放明文密碼」的要求，**但不防 `.env` 檔案本身外洩**——
  拿到該檔即可解密。要防到那個層級，金鑰須與 `.env` 分離
  （獨立檔案、Vault、KMS 或 CI secret）並於部署時單獨注入。
- **金鑰遺失即無法解密既有密文**，需以原始明文重新加密。
- 明文與密文可混用；機制只對 `${enc::...}` 生效。
- 解密失敗會在啟動時明確拋錯（`AEADBadTagException: Tag mismatch`），
  不會把密文當成密碼靜默使用。
:::

---

## 4. 設定 application.properties

切換至主服務目錄：

```bash
cd bestpartner-service/src/main/resources
```

專案**只有一份** `application.properties`（無 `application-sit.properties` 之類的分檔）。
環境差異由兩個機制處理：

- **profile 區塊**（`%dev.` / `%docker.` / `%sit.` / `%uat.` / `%prod.` 前綴）：行為差異，如 log 等級、Swagger 開關
- **環境變數**（`${ENV_VAR:預設值}`）：連線位址與機密，由 `.env.<profile>` 提供

一般情況下**不需要改這個檔案**——調整 `.env` 即可。

### 資料庫設定

連線資訊已參數化，改 `.env` 而非改此檔：

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${DB_USERNAME:${enc::<密文>}}
quarkus.datasource.password=${DB_PASSWORD:${enc::<密文>}}
quarkus.datasource.jdbc.url=${DB_URL:jdbc:postgresql://localhost:5432/pgdb?currentSchema=bestpartner}
```

:::note 為什麼預設值是密文？
帳密的預設值（本機開發用的 `pguser` / `pgpass`）以密文形式存放，讓版控中的設定檔
不出現明文密碼字樣。環境變數缺席時會回退到密文，再由同檔內的預設金鑰解開，
因此**全新 clone 不帶任何 `.env` 仍可直接啟動**。

這是**衛生措施而非安全措施**——金鑰與密文都在版控中，任何人 clone 即可解密。
真正的機密一律只放在不進版控的 `.env.<profile>`。
:::

:::caution 容器環境
容器內的 `localhost` 指向容器自己。連宿主機的 PostgreSQL 要用 `host.docker.internal`，
連 compose 內的 DB 則用服務名稱。
:::

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
# 資料庫敏感欄位（如向量資料庫密碼、llm_setting.api_key）AES-GCM 加密金鑰
# 預設值僅供本機開發；正式環境以 CRYPTO_SECRET_KEY 環境變數覆寫
crypto.secret-key=${CRYPTO_SECRET_KEY:changeme-please-replace-in-production}

# 設定檔機密值解密的根金鑰（與上方資料金鑰刻意分離）
# 正式環境以 CONFIG_ENCRYPTION_KEY 環境變數覆寫
smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key=${CONFIG_ENCRYPTION_KEY:dev-only-config-key-not-a-real-secret}
```

:::warning
正式環境請務必以 `CRYPTO_SECRET_KEY` 與 `CONFIG_ENCRYPTION_KEY` 替換為高強度隨機字串（建議 32 字元以上），
不同環境使用不同金鑰，且**兩者彼此也不同值**——分離的用意是換掉其中一把不會波及另一邊的既有密文。
:::

### 檔案路徑設定

```properties
quarkus.log.file.path=${LOG_DIR:D:/tmp/bestpartner}/bestpartner.log
file.upload.dir=${FILE_UPLOAD_DIR:D:/tmp/bestpartner/upload}
```

:::caution 容器環境
預設值為 Windows 路徑。容器／Linux 務必以 `LOG_DIR`、`FILE_UPLOAD_DIR` 覆寫為 POSIX 路徑，
否則會產生名為 `D:` 的字面目錄。

容器以 uid 185（jboss）執行：`/var/log` **不可寫**，請用 `/opt`、`/tmp` 或 `/deployments`。
檔案需保存時搭配具名 volume（`-v bestpartner-data:/opt/bestpartner`）。
:::

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
bestpartner-service/build/bestpartner-service-0.1.8-SNAPSHOT-runner.jar
```
