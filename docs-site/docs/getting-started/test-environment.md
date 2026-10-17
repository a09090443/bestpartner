---
title: "測試環境建置（Linux / sit）"
sidebar_position: 4
description: 在 Linux 上從零建立 BestPartner sit 測試環境的完整步驟，涵蓋前置軟體、資料庫初始化、prod 建置、.env.sit 設定、jar 與 Docker 啟動、port 80 權限與存活檢查。
keywords: [測試環境, sit, Linux, 部署, .env.sit, Docker, port 80]
---

# 測試環境建置（Linux / sit）

本頁帶你在一台 Linux 主機上**從零**建立 sit（整合測試）環境。依序做完 1～8 步即可；
本機開發（Windows、dev profile）請看 [安裝與設定](./installation.md)。

> 以下指令皆假設在 repo 根目錄（`bestpartner/`）執行，除非另有 `cd`。

## 1. 前置軟體

| 軟體 | 版本 | 用途 | 確認指令 |
|------|------|------|---------|
| JDK | **21** | 建置與執行後端 | `java -version` |
| PostgreSQL | 任一現行版本（範例用 16） | 資料庫 | `psql --version` |
| Node.js（含 npm） | **`^20.19.0 \|\| >=22.12.0`** | 前端 `bestpartner-ui` | `node --version` |
| Git | — | 取得原始碼 | `git --version` |
| Docker | 選用 | 以容器跑 PostgreSQL 或服務本身 | `docker --version` |

取得原始碼：

```bash
git clone https://github.com/a09090443/bestpartner.git
cd bestpartner
```

## 2. 初始化資料庫

完整流程與兩種連線方式（本機 psql／`docker exec`）見
[安裝與設定 › 初始化資料庫](./installation.md#init-database)，重點如下：

- `bestpartner-ddl.sql` 會先 `CREATE SCHEMA IF NOT EXISTS bestpartner` 並切換過去，不必手動建 schema。
- **先 DDL、後 init-data**，順序不可顛倒。
- ⚠️ DDL 會 **DROP 並重建所有資料表**，只能對全新或可清空的資料庫執行。

```bash
psql -h localhost -U pguser -d pgdb -f docs/sql/bestpartner-ddl.sql
psql -h localhost -U pguser -d pgdb -f docs/sql/bestpartner-init-data.sql
```

> 帳號、密碼、資料庫名稱請換成 sit 環境的實際值；之後在第 4 步的 `DB_URL`／`DB_USERNAME`／`DB_PASSWORD` 填入相同值。

## 3. 建置後端（一律用 prod profile）

```bash
cd bestpartner-service
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=prod
cd ..
```

產物：`bestpartner-service/build/bestpartner-service-0.1.8-runner.jar`

:::info 為什麼 sit 也用 prod 建置？
建置時的 profile 會成為 jar／image 的**預設 runtime profile**。以 prod 建置，萬一啟動時忘了指定 profile，
會落在「Swagger 關閉、log INFO」的安全側。實際跑哪個環境由啟動時的 `sit` profile 決定（第 5、7 步）。
:::

## 4. 建立 `.env.sit`

```bash
cp .env.example .env.sit
```

編輯 `.env.sit`，至少修改下列項目：

```dotenv
# 資料庫：依連線方式填寫（jar 直接跑在主機上可用 localhost；容器請見第 7 步）
DB_URL=jdbc:postgresql://localhost:5432/pgdb?currentSchema=bestpartner
DB_USERNAME=pguser
DB_PASSWORD=<sit 的資料庫密碼>

# 路徑：Linux 一律改成 POSIX 路徑（預設的 D:/... 會在 Linux 產生名為 "D:" 的字面目錄）
LOG_DIR=/opt/bestpartner/logs
FILE_UPLOAD_DIR=/opt/bestpartner/upload

# 兩把金鑰：各自換成高強度隨機值，且「彼此不同」
CRYPTO_SECRET_KEY=<第一個隨機值>
CONFIG_ENCRYPTION_KEY=<第二個隨機值>
```

產生隨機值（執行兩次，各取一個）：

```bash
openssl rand -base64 32
```

確認路徑可寫：

```bash
sudo mkdir -p /opt/bestpartner/logs /opt/bestpartner/upload
sudo chown -R "$USER" /opt/bestpartner
```

:::warning 兩把金鑰的用途不同
- `CRYPTO_SECRET_KEY`：加密**資料庫欄位**（如 `llm_setting.api_key`）。上線後更換會使既有密文無法解密。
- `CONFIG_ENCRYPTION_KEY`：解密 `.env.sit` 中寫成 `${enc::<密文>}` 的**設定值**。
- 兩者刻意分離，**不可填成相同值**；各環境也應不同。
- 要把 `DB_PASSWORD` 等機密改成密文，見 [機密值加密](./installation.md)（bash 範例：`export CONFIG_ENCRYPTION_KEY=...`、
  `export CONFIG_SECRET_VALUE=...` 後執行 `./gradlew encryptConfigSecret -q`）。
- 金鑰與密文放在同一份檔案，此加密只防明文被瞥見／誤貼／進日誌，**不防 `.env.sit` 檔案本身外洩**，請限制檔案權限（`chmod 600 .env.sit`）。
:::

## 5. 以 jar 啟動

Quarkus **只會自動載入「工作目錄」下名為 `.env` 的檔案**，不會讀 `.env.sit`。
因此請在執行目錄把 `.env.sit` 複製成 `.env`，再以 sit profile 啟動：

```bash
cp .env.sit .env
QUARKUS_PROFILE=sit java -jar bestpartner-service/build/bestpartner-service-0.1.8-runner.jar
# 或等價寫法（-D 參數必須放在 -jar 之前）：
# java -Dquarkus.profile=sit -jar bestpartner-service/build/bestpartner-service-0.1.8-runner.jar
```

:::danger 不要用 `source` 載入 env 檔
**不可**用 `source .env.sit`、`set -a; . .env.sit` 之類的方式把檔案載入 shell。
shell 會把 `${enc::<密文>}` 當成變數展開而破壞密文，服務會以錯誤的值啟動或解密失敗。
一律讓 Quarkus 自行讀取工作目錄的 `.env`（jar），或用 `docker --env-file`（容器）。
:::

## 6. Linux 的 port 80 權限

服務預設監聽 **port 80**。Linux 上一般（非 root）使用者無法綁定 1024 以下的埠，
直接 `java -jar` 會因權限不足啟動失敗。擇一處理：

**方式 A：授權 java 綁定低埠（建議用於 jar 方式）**

```bash
sudo setcap 'cap_net_bind_service=+ep' "$(readlink -f "$(command -v java)")"
```

> 只授予「綁定低埠」這一項權限，不需以 root 執行服務。升級或更換 JDK 後需重新執行。

**方式 B：改用 Docker**（見第 7 步），容器內固定 port 80，以 `-p 80:80` 對映即可。

:::caution 不建議改 `HTTP_PORT`
雖可在 `.env.sit` 設定 `HTTP_PORT=8080` 避開權限問題，但前端的 Vite 代理（`bestpartner-ui/vite.config.ts`）
把 `/llm`、`/login/` **固定轉發到 `http://localhost:80`**；後端改埠後前端會連不到後端，
必須同步修改 `vite.config.ts` 的 proxy target。
:::

## 7. 以 Docker 啟動（替代第 5、6 步）

先建 image（固定 prod 建置），詳見 repo 的 `scripts/docker-package.sh`：

```bash
./scripts/docker-package.sh --no-tar
```

再以 sit profile ＋ `.env.sit` 啟動，並用**具名 volume** 保存日誌與上傳檔：

```bash
docker run -d --name bestpartner-sit -p 80:80 \
  -e QUARKUS_PROFILE=sit \
  --env-file .env.sit \
  -v bestpartner-data:/opt/bestpartner \
  bestpartner-service:latest
```

:::caution 容器內的 DB_URL 不可用 localhost
容器內的 `localhost` 指向**容器自己**，不是主機。`.env.sit` 的 `DB_URL` 要改成：
- 連主機上的 PostgreSQL：`jdbc:postgresql://host.docker.internal:5432/pgdb?currentSchema=bestpartner`
  （Linux 需在 `docker run` 加上 `--add-host=host.docker.internal:host-gateway`）
- 連同一 Docker 網路／compose 內的 PostgreSQL：改用該容器的服務名稱，例如 `jdbc:postgresql://bestpartner-postgres:5432/pgdb?currentSchema=bestpartner`

容器以 uid 185 執行，`LOG_DIR`／`FILE_UPLOAD_DIR` 請用 `/opt/bestpartner/...`（`/var/log` 不可寫）。
`--env-file` 不做引號處理：值一律不加引號。
:::

## 8. 存活檢查

```bash
curl -i http://localhost/view/chat
# 預期 HTTP/1.1 200
```

`/view/chat` 是公開頁面、不受 profile 影響，**存活檢查一律用它**。

`/swagger-ui` 與 `/q/openapi` **只在 dev／docker／sit profile 開放**；uat、prod 或未指定 profile（預設等同 prod）皆回 404，這是正確行為，因此**不可拿這兩者當存活檢查**。sit 下可再確認：

```bash
curl -i http://localhost/swagger-ui    # sit：302 導向 Swagger UI 屬正常；uat/prod：404
curl -i http://localhost/q/openapi     # sit：200；uat/prod：404
```

接著以 email 登入取得 Token（預設帳號見 [安裝與設定 › 預設帳號](./installation.md#default-accounts)）：

```bash
curl -X POST http://localhost/login/ \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw","password":"admin"}'
```

需要網頁介面時，依 [前端安裝與執行](./installation.md#frontend) 啟動 `bestpartner-ui`。

## 常見問題

| 症狀 | 原因與處理 |
|------|-----------|
| 啟動時 `Permission denied` 綁定 80 失敗 | 非 root 綁低埠，見第 6 步 |
| 啟動時 `AEADBadTagException` | `CONFIG_ENCRYPTION_KEY` 與產生密文時用的金鑰不一致，或曾用 `source` 載入 env 檔 |
| init-data 報 `relation "bestpartner.xxx" does not exist` | 未先執行 DDL，或執行的不是目前版本的 DDL |
| 容器連不到資料庫 | `DB_URL` 仍用 `localhost`，見第 7 步 |
| 登入回 400／401 | 請用 email（`admin@bestpartner.com.tw`）而非 `admin` 登入 |
