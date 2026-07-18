# 設定檔與 Profile

## 設定檔位置

`bestpartner-service/src/main/resources/application.properties`

## 核心原則（三條，改設定前必讀）

1. **跨環境會變動的值一律寫成 `${ENV_VAR:預設值}`**，預設值＝本機 Windows 開發值。
   不帶任何環境變數時 `./gradlew quarkusDev` 可直接跑。
2. **profile 只負責「行為差異」**（log 等級、Swagger 開關）；
   **連線位址與機密一律走環境變數**，不寫死在 properties。
3. **一份 image 跑所有環境**：不為個別環境重建 image，
   差異由 `-e QUARKUS_PROFILE=<profile>` ＋ `--env-file .env.<profile>` 在 runtime 決定。

## Profile 一覽

| Profile | 用途 | log 等級 | Swagger UI / OpenAPI | Hibernate SQL 日誌 | MCP log |
|---------|------|---------|---------------------|-------------------|---------|
| `dev` | 本機開發（預設用於 quarkusDev） | DEBUG | 開放 | 開 | 開 |
| `docker` | 本機容器測試 | DEBUG | 開放 | 開 | 開 |
| `sit` | 整合測試 | DEBUG | 開放 | 開 | 開 |
| `uat` | 使用者驗收 | INFO | 關閉 | 關 | 關 |
| `prod` | 生產 | INFO | 關閉 | 關 | 關 |
| （未指定） | ＝ prod（安全預設） | INFO | 關閉 | 關 | 關 |

> **未指定 profile 時為何是 prod？** image 一律以 `-Dquarkus.profile=prod` 建置，
> 建置時的 profile 會成為 image 的預設 runtime profile。
> 這是刻意設計：部署時忘記帶 `QUARKUS_PROFILE`，落點是「Swagger 關閉、log INFO」的安全側，
> 而非「Swagger 對外全開」。

## ⚠️ build-time vs runtime 設定（單一 image 能成立的關鍵）

Quarkus 的設定分兩類，混淆會導致「以為關掉了其實還開著」：

| 設定 | 性質 | runtime 可切換？ | 本專案作法 |
|------|------|----------------|-----------|
| `quarkus.swagger-ui.always-include` | **build-time** | ✗ 由建置時 profile 凍結 | 設全域 `true`，一律打包進 image |
| `quarkus.swagger-ui.enable` | **runtime** | ✓ | 全域 `false`，dev/docker/sit 才開 |
| `quarkus.smallrye-openapi.enable` | **runtime** | ✓ | 同上 |
| `quarkus.hibernate-orm.log.sql` | **build-time** | ✗ | 設全域 `true`（開啟能力） |
| `quarkus.hibernate-orm.log.bind-parameters` | **build-time** | ✗ | 同上 |
| `quarkus.log.category."...".level` | **runtime** | ✓ | 實際輸出的把關點 |
| `quarkus.log.level` 等 | runtime | ✓ | 各 profile 分別設定 |

> 已實測：對 dev 建置的 image 下 `-e QUARKUS_PROFILE=prod`，log 等級會切成 INFO，
> 但 Swagger 仍回 302（因 `always-include` 是 build-time）。
> 故 Swagger 的對外與否**只能靠 `enable`**（runtime）控制，不可依賴 `always-include`。

### 通用模式：build-time 開能力、runtime 控輸出

**不可對 build-time 設定加 `%profile.` 前綴。** 因為 image 一律以 prod 建置，
`%dev.quarkus.hibernate-orm.log.sql=true` 這種寫法會在建置時被求值成 `false` 並烤進 image，
結果連 dev/sit 都失去該功能——與意圖相反。

正確作法（Swagger 與 Hibernate SQL 日誌都採此模式）：

| 步驟 | 設定性質 | 作法 |
|------|---------|------|
| 1. 開啟「能力」 | build-time | 設**全域**開啟，一律打包進 image |
| 2. 控制「實際生效」 | runtime | 全域安全預設（關），僅特定 profile 打開 |

### Hibernate SQL 日誌

`log.sql` / `log.bind-parameters` 是 build-time，故全域開啟；實際輸出由 **log 類別等級**（runtime）把關：

```properties
quarkus.log.category."org.hibernate.SQL".level=OFF              # 安全預設
quarkus.log.category."org.hibernate.orm.jdbc.bind".level=OFF
%dev.quarkus.log.category."org.hibernate.SQL".level=DEBUG       # dev/docker/sit 才開
%dev.quarkus.log.category."org.hibernate.orm.jdbc.bind".level=TRACE
```

> 全域 `OFF` 的用意是**縱深防禦**：僅靠 `quarkus.log.level=INFO` 擋 SQL 是脆弱的——
> 一旦有人為排查在 uat/prod 設 `QUARKUS_LOG_LEVEL=DEBUG`，SQL 與繫結參數值（可能含個資）就會全部寫入日誌。
> 明確把類別設為 `OFF` 後，即使強開 DEBUG 也擋得住（已實測驗證）。
>
> 註：強開 DEBUG 時，`org.hibernate.persister.entity.AbstractEntityPersister` 仍會在**啟動階段**
> 印出 SQL 樣板（`select id from xxx where id=?`），內容僅含表名欄名與 `?` 佔位符、**無實際資料值**，屬可接受範圍。

## 環境變數對照表

範本：根目錄 `.env.example`（唯一進版控的 env 檔）。
各環境複製為 `.env.<profile>` 後填值，已列入 `.gitignore`。

| 環境變數 | 對應設定鍵 | 預設值 | 說明 |
|---------|-----------|--------|------|
| `HTTP_PORT` | `quarkus.http.port` | `80` | 服務埠 |
| `DB_URL` | `quarkus.datasource.jdbc.url` | `jdbc:postgresql://localhost:5432/pgdb?currentSchema=bestpartner` | ⚠️ 容器內不可用 localhost |
| `DB_USERNAME` | `quarkus.datasource.username` | `pguser` | |
| `DB_PASSWORD` | `quarkus.datasource.password` | `pgpass` | |
| `DB_POOL_MIN` | `quarkus.datasource.jdbc.min-size` | `5` | |
| `DB_POOL_MAX` | `quarkus.datasource.jdbc.max-size` | `15` | |
| `LOG_DIR` | `quarkus.log.file.path` 的目錄部分 | `D:/tmp/bestpartner` | ⚠️ 容器須覆寫為 POSIX 路徑 |
| `FILE_UPLOAD_DIR` | `file.upload.dir` | `D:/tmp/bestpartner/upload` | ⚠️ 同上 |
| `CRYPTO_SECRET_KEY` | `crypto.secret-key` | `changeme-please-replace-in-production` | ⚠️ sit 以上務必替換。**資料庫**敏感欄位加密 |
| `CONFIG_ENCRYPTION_KEY` | `smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key` | `dev-only-config-key-not-a-real-secret` | ⚠️ sit 以上務必替換。**設定值**解密根金鑰，與上者刻意分離 |
| `JWT_REFRESH_SWITCH` | `jwt.refresh.switch` | `true` | |
| `OPENROUTER_API_KEY` | `ai-platform.openrouter.api-key` | 空字串 | ADMIN CHAT 預設模型 |
| `OPENROUTER_MODEL` | `ai-platform.openrouter.model-name` | `openai/gpt-5.5-pro` | |
| `OPENROUTER_TEMPERATURE` / `_TOP_P` / `_MAX_TOKENS` / `_TIMEOUT` | 對應 openrouter 設定 | `0.7` / `0.5` / `4096` / `60s` | |

> `.env.<profile>` 會被 `docker --env-file` 讀取，**它不做 shell 引號處理**：
> 值一律不加引號，且不支援 `${VAR}` 巢狀展開。

## 設定檔機密值加密（`${enc::<密文>}`）

`.env.<profile>` 中的機密可寫成密文，啟動時由 SmallRye Config 的
`AESGCMNoPaddingSecretKeysHandler` 自動解密（依賴 `io.smallrye.config:smallrye-config-crypto`，
版本由 Quarkus BOM 管理）。

```dotenv
# .env.prod
DB_PASSWORD=${enc::<密文>}
OPENROUTER_API_KEY=${enc::<密文>}
```

> `enc` 是本專案的短名 handler（`config/EncSecretKeysHandlerFactory.kt`，繼承內建 factory
> 只覆寫名稱），等價於 SmallRye 內建的 `aes-gcm-nopadding`。
> **密文格式相同，同一份密文兩種前綴都能解**；日常一律用短名。
> 以 `META-INF/services/io.smallrye.config.SecretKeysHandlerFactory` 註冊，
> 已實測 uber-jar 打包會**合併**兩份註冊而非覆蓋。

產生密文（金鑰與明文走環境變數，不進命令列參數與 shell 歷史紀錄）：

```powershell
$env:CONFIG_ENCRYPTION_KEY='<該環境的根金鑰>'
$env:CONFIG_SECRET_VALUE='<機密明文>'
cd bestpartner-service; ./gradlew encryptConfigSecret -q
# 驗證既有密文可解回原值：ConfigSecretUtil 加上 -d 旗標
```

### ⚠️ 這個機制實際擋得住什麼（務必先讀）

**`CONFIG_ENCRYPTION_KEY` 就放在 `.env.<profile>` 內，與密文同檔**。
因此請正確理解其安全等級：

| 情境 | 擋得住？ |
|------|---------|
| 明文密碼被瞥見、截圖、誤貼到聊天室／工單 | ✓ |
| 設定檔內容意外寫入日誌或錯誤訊息 | ✓ |
| 「設定檔不得存放明文密碼」的形式要求 | ✓ |
| **`.env.<profile>` 檔案本身外洩** | ✗ 金鑰在同一個檔案裡，拿到即可解密 |
| 已取得主機存取權的攻擊者 | ✗ |

若需要防到「檔案外洩」層級，金鑰必須與 `.env` 分離——存於獨立檔案、Vault、KMS
或 CI secret，並於部署時以 `-e CONFIG_ENCRYPTION_KEY=...` 單獨注入。
本專案目前刻意選擇「同檔」以簡化部署流程，屬明確的取捨而非疏漏。

> **目前狀態**：所有 env 檔的真實機密皆已加密，各檔使用不同金鑰。
>
> | 檔案 | 已加密的項目 |
> |------|------------|
> | `.env`（dev，本機） | `OPENROUTER_API_KEY` |
> | `.env.docker` | `DB_PASSWORD` |
> | `.env.sit` | `DB_PASSWORD` |
> | `.env.uat` | `DB_PASSWORD` |
>
> ⚠️ **只加密「真實機密」**：未填值的佔位符（如 `.env` 的
> `OPENAI_API_KEY=your-openai-api-key-here`）維持明文——加密佔位符會讓人
> 誤以為裡面藏著真金鑰，反而降低可讀性。
>
> ⚠️ **金鑰遺失即無法解密既有密文**，需以原始明文重新加密。
> ⚠️ 根目錄 `.env` 由 Quarkus 依**工作目錄**自動載入，本機開發不需額外設定；
> 未帶任何 env 時仍可跑 `quarkusDev`（`application.properties` 的預設值即本機開發值）。

### ⚠️ 與上方「不支援 `${VAR}` 巢狀展開」不衝突

那句話講的是 **`docker --env-file` 這一層**：它把值當字面字串原樣傳給容器，不做 shell 展開。
而 `${enc::...}` 是 **Quarkus 應用內** 的 config expression，
由 SmallRye Config 在讀取設定值時展開——兩者是不同層級，互不影響。

實際鏈路是巢狀的，且已實測可正確展開：

```
application.properties   quarkus.datasource.password=${DB_PASSWORD:pgpass}
.env.<profile>           DB_PASSWORD=${enc::<密文>}
                         → 展開 DB_PASSWORD 得到的值本身仍是 expression，會繼續展開為明文
```

### 設計要點

- **兩把金鑰刻意分離**：`CONFIG_ENCRYPTION_KEY`（設定值）與 `CRYPTO_SECRET_KEY`（資料庫欄位）
  互不相干，換掉其中一把不會波及另一邊的既有密文。
- **根金鑰本身無法被加密**（bootstrap secret），是 `.env.<profile>` 中必須保持明文的值，
  只能靠檔案權限與環境隔離保護；其後果見上方「實際擋得住什麼」。
- **`application.properties` 的 datasource 預設值為密文**，寫成
  `${DB_USERNAME:${enc::<密文>}}`：環境變數缺席時（全新 clone、無任何 .env）
  回退到密文，由**同檔內的預設金鑰**解開，故「免設定啟動」維持不變（已實測）。
  目的是讓版控中的設定檔不出現 `pguser` / `pgpass` 字樣。
  ⚠️ 這是**衛生措施而非安全措施**——金鑰與密文同在版控，clone 即可解密。
  真實機密仍只存在於不進版控的 `.env.<profile>`。
- ⚠️ **改動 `CONFIG_ENCRYPTION_KEY` 的預設值時**，`application.properties` 的
  datasource 密文必須以新金鑰重新產生，否則全新 clone 會解密失敗而啟動不了。
  各環境 `.env` 皆已提供 `DB_USERNAME`/`DB_PASSWORD`，其金鑰不同不受影響
  （環境變數存在時預設值不會被求值，已有測試覆蓋）。
- **明文與密文可混用**：機制只對 `${enc::...}` 生效，其餘值原樣處理。
- **handler 為 lazy 初始化**：未使用任何密文時不會被觸發，不影響本機開發。
- **解密失敗會明確拋錯**（GCM 完整性驗證），不會靜默把密文當成密碼使用。

> 格式與展開行為由 `ConfigSecretUtilTest` / `ConfigSecretExpressionTest` 把關
> （純 JUnit，非 QuarkusTest）。升級 `smallrye-config-crypto` 後若密文格式變動，
> 這兩支測試會先紅燈，而非等到服務啟動解密失敗才發現。

## 容器路徑注意事項

- 容器以 **uid 185（jboss）** 執行：`/var/log` **不可寫**，`/tmp`、`/opt`、`/deployments` 可寫。
  `.env.docker` 採用 `/opt/bestpartner/{logs,upload}`。
- `LOG_DIR` / `FILE_UPLOAD_DIR` 若沿用 Windows 預設值，容器內會產生名為 `D:` 的**字面目錄**
  （實測落點 `/deployments/D:/tmp/bestpartner/`），務必覆寫。
- 上傳檔案與日誌需保存時，用**具名 volume**（`-v bestpartner-data:/opt/bestpartner`）；
  bind mount 的宿主目錄擁有者常非 185，會導致寫入失敗。

## 執行方式

```bash
# 本機開發（自動載入根目錄 .env）
cd bestpartner-service && ./gradlew quarkusDev

# 容器（依環境切換；解密金鑰含在 .env.<profile> 內，無需另外注入）
docker run -d -p 80:80 \
  -e QUARKUS_PROFILE=uat \
  --env-file .env.uat \
  -v bestpartner-data:/opt/bestpartner \
  bestpartner-service:latest
```

Docker image 建置與匯出 → [`build-and-run.md`](build-and-run.md)、`docker-build` skill

## 資料庫設定

- DB：PostgreSQL，預設連線 `localhost:5432/pgdb`（schema `bestpartner`）
- 帳號：`pguser` / 密碼：`pgpass`（皆可由環境變數覆寫）
- 初始化：手動執行 `docs/sql/` 的 SQL
- **無 Flyway**：`build.gradle.kts` 未引入 flyway 依賴，schema 變更一律手動執行 SQL

> 歷史說明：此檔曾記載 sit / prod「含 Flyway 設定」，但 `application.properties` 中的
> `quarkus.flyway.*` 因無對應 extension，被 Quarkus 判為 "Unrecognized configuration key" 而忽略，
> 屬從未生效的死設定，已於 0.1.8 移除。
