---
title: 安裝與設定
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

## 2. 初始化資料庫 {#init-database}

:::danger 執行前必讀
`bestpartner-ddl.sql` 會 **DROP 並重建所有 BestPartner 資料表**，既有資料會全部清除。
只能對**全新或可清空**的資料庫執行；要保留資料的環境請先自行備份，不要重跑 DDL。
:::

確認 PostgreSQL 已啟動、資料庫 `pgdb` 與帳號 `pguser` 已存在後，於 repo 根目錄**依序**執行兩支 SQL：

1. `docs/sql/bestpartner-ddl.sql`：建立並切換到 `bestpartner` schema，再建立所有資料表
2. `docs/sql/bestpartner-init-data.sql`：寫入種子資料（預設帳號、平台、工具等）

順序不可顛倒——init-data 寫入的是 `bestpartner.*` 資料表，DDL 尚未執行時會失敗。

**方式 A：本機 psql 連線**

```bash
psql -h localhost -U pguser -d pgdb -f docs/sql/bestpartner-ddl.sql
psql -h localhost -U pguser -d pgdb -f docs/sql/bestpartner-init-data.sql
```

**方式 B：PostgreSQL 跑在 Docker 容器內（本機沒有安裝 psql）**

以 [環境需求](./prerequisites.md) 中的 `bestpartner-postgres` 容器為例，透過標準輸入把 SQL 送進容器：

```bash
docker exec -i bestpartner-postgres psql -U pguser -d pgdb < docs/sql/bestpartner-ddl.sql
docker exec -i bestpartner-postgres psql -U pguser -d pgdb < docs/sql/bestpartner-init-data.sql
```

確認 schema 與種子帳號已建立：

```bash
psql -h localhost -U pguser -d pgdb -c "SELECT email FROM bestpartner.llm_user;"
```

### 預設帳號 {#default-accounts}

種子資料建立兩組帳號，**登入一律使用 email**（不是使用者名稱）：

| 角色 | 登入 email | 密碼 |
|------|-----------|------|
| 管理員 | `admin@bestpartner.com.tw` | `admin` |
| 一般使用者 | `test@partmer.com.tw` | `user` |

:::caution
`test@partmer.com.tw` 的網域是 `partmer`（與種子資料一致），不是 `partner`。
sit 以上環境建議登入後立即變更預設密碼。
:::

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

```bash
# Linux / macOS（bash）
export CONFIG_ENCRYPTION_KEY='<該環境的根金鑰>'
export CONFIG_SECRET_VALUE='<機密明文>'
cd bestpartner-service && ./gradlew encryptConfigSecret -q
```

驗證密文可解回原值（`CONFIG_SECRET_VALUE` 改放**密文本體**，不含 `${enc::}` 外框）：

```powershell
$env:CONFIG_ENCRYPTION_KEY='<該環境的根金鑰>'
$env:CONFIG_SECRET_VALUE='<密文本體>'
cd bestpartner-service; ./gradlew decryptConfigSecret -q
```

```bash
# Linux / macOS（bash）
export CONFIG_ENCRYPTION_KEY='<該環境的根金鑰>'
export CONFIG_SECRET_VALUE='<密文本體>'
cd bestpartner-service && ./gradlew decryptConfigSecret -q
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

`application.properties` **既有的系統預設模型只有 OpenRouter**（供 ADMIN CHAT `/llm/admin/*` 使用），
各鍵皆已參數化，只需在 `.env` 填入金鑰：

```dotenv
# .env（填入實際金鑰，不提交）
OPENROUTER_API_KEY=your-openrouter-api-key-here
# 以下為選填，未設定時採用括號內預設值
OPENROUTER_MODEL=openai/gpt-5.5-pro      # 預設 openai/gpt-5.5-pro
OPENROUTER_TEMPERATURE=0.7
OPENROUTER_MAX_TOKENS=4096
OPENROUTER_TIMEOUT=60s
```

對應的 `application.properties` 內容（不需修改）：

```properties
ai-platform.openrouter.api-key=${OPENROUTER_API_KEY:}
ai-platform.openrouter.model-name=${OPENROUTER_MODEL:openai/gpt-5.5-pro}
```

`OPENROUTER_API_KEY` 留空時服務仍可正常啟動，只是呼叫 ADMIN CHAT 會得到 OpenRouter 401。

**選用：OpenAI／Ollama 系統預設模型**

`application.properties` **沒有**預先寫好 OpenAI 或 Ollama 的設定；需要時請自行加入，
鍵名前綴分別為 `ai-platform.openai.*`、`ai-platform.ollama.*`（欄位與 OpenRouter 相同：
`api-key`、`url`、`model-name`、`temperature`、`timeout`、`max-tokens` 等）：

```properties
# 選用：OpenAI（金鑰放 .env 的 OPENAI_API_KEY）
ai-platform.openai.api-key=${OPENAI_API_KEY:}
ai-platform.openai.model-name=gpt-4o
ai-platform.openai.temperature=0.7
ai-platform.openai.timeout=60s

# 選用：Ollama（本地端，無需 API Key）
ai-platform.ollama.url=http://localhost:11434
ai-platform.ollama.model-name=llama3
ai-platform.ollama.temperature=0.7
ai-platform.ollama.timeout=60s
```

> 一般使用者對話用的模型（含 OpenAI、Anthropic、Gemini、Grok、Ollama 等）是透過 LLM Setting API／介面
> 儲存在資料庫（`llm_setting`），不需寫進設定檔。

### 向量資料庫設定

**未設定任何外部向量庫時，預設使用 InMemory**（記憶體，服務重啟後資料消失），不需要額外安裝。
若要改用 Chroma 或 Milvus，於 `application.properties` 選用加入對應的 URL（鍵名前綴為 `vector.chroma`、`vector.milvus`）：

```properties
# 選用：Chroma
vector.chroma.url=http://localhost:8000

# 選用：Milvus
vector.milvus.url=http://localhost:19530
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
bestpartner-service/build/bestpartner-service-0.1.8-runner.jar
```

## 6. 前端安裝與執行（bestpartner-ui） {#frontend}

前端為 Vue 3 + Vite 應用，位於 `bestpartner-ui/`。需要 **Node.js `^20.19.0 || >=22.12.0`**（Vite 的引擎需求），
請先以 `node --version` 確認。

```bash
cd bestpartner-ui
npm ci              # 依 package-lock.json 安裝相依套件
npm run dev         # 開發模式，瀏覽 http://localhost:5173
```

其他常用指令：

```bash
npm run build       # 型別檢查（vue-tsc）＋ 產出 dist/
npm run preview     # 以 dist/ 啟動預覽伺服器（終端機會顯示網址，預設 http://localhost:4173）
```

:::caution 前端依賴後端在 port 80
開發代理（`vite.config.ts`）把 `/llm` 與 `/login/` 固定轉發到 **`http://localhost:80`**。
啟動前端前請先啟動後端並確認它在 port 80；若後端改用其他埠（例如設定 `HTTP_PORT`），
前端會連不到後端，需同步修改 `vite.config.ts` 的 proxy target。
:::

前端開啟後，以預設管理員 **`admin@bestpartner.com.tw`／`admin`** 登入（見 [預設帳號](#default-accounts)）。

---

:::tip 要建立 sit 測試環境？
本頁以本機開發（dev）為主。要在 Linux 從零建立 sit 測試環境（prod 建置、`.env.sit`、port 80 權限、Docker），
請改看 [測試環境建置](./test-environment.md)。
:::
