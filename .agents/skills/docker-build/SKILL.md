---
name: docker-build
description: Use when user wants to build a Docker image for bestpartner-service, verify it starts correctly, or export it as a portable tar file for another machine. Triggered by "建立 docker image", "打包 docker", "docker build", "docker 化", "產生 docker image", "docker 匯出", "docker tar", "移植到其他機器", "build docker image".
---

# BestPartner Docker Image 建置流程

## Overview

將 `bestpartner-service` 建置為**單一跨環境 image**，驗證可啟動，並匯出 tar 檔供離線搬遷。
流程：建置 uber-jar → docker build → 啟動驗證 → 清理暫存容器 → 匯出 tar。

**一份 image 跑所有環境**：dev / docker / sit / uat / prod 的差異一律由 runtime 決定——
profile 用 `-e QUARKUS_PROFILE=<profile>`，連線位址與機密用 `--env-file .env.<profile>`。
不為個別環境另建 image。

依據：[`.Codex/rules/build-and-run.md`](../../rules/build-and-run.md)、[`configuration-and-profiles.md`](../../rules/configuration-and-profiles.md)

## When to Use

- 使用者說「建立 docker image」「打包 docker」「docker build」
- 使用者要把服務搬到另一台機器，需要 tar 檔匯出/匯入
- 使用者要驗證 image 能否正常啟動

## 執行方式：優先呼叫 `scripts/docker-package`

**`scripts/docker-package`** 已將下方 Step 1–6 完整實作（含前置檢查、版本讀取、
啟動驗證、容器清理、時戳命名）。**預設一律用它**，不要逐步手打指令。
依所在平台擇一（兩者行為等價）：

```bash
# Linux / macOS
./scripts/docker-package.sh                       # 完整流程
./scripts/docker-package.sh --no-tar              # 只建 image，不匯出
./scripts/docker-package.sh --verify-port 18085   # 驗證埠被占用時改用其他埠
```

```powershell
# Windows
pwsh ./scripts/docker-package.ps1              # 完整流程
pwsh ./scripts/docker-package.ps1 -NoTar       # 只建 image，不匯出
pwsh ./scripts/docker-package.ps1 -VerifyPort 18085   # 驗證埠被占用時改用其他埠
```

⚠️ `.sh`（Linux/macOS 原生 bash）與 `.ps1`（Windows）是同一流程的**兩份實作**，
修改流程時**兩份都要改**，否則兩平台行為分歧。見 `scripts/README.md`。

下方步驟是該腳本的**規格說明**——用於腳本不存在、需修改腳本，或需理解某步驟為何如此設計時。
若腳本與下方步驟衝突，兩者都要修到一致，不可只改一邊。

## Process（腳本規格；手動執行時亦須依序）

### Step 1：確認 Dockerfile 存在

檢查 `bestpartner-service/src/main/docker/Dockerfile.uber-jar` 是否存在。

> ⚠️ **只用這個 Dockerfile**。同目錄下 `Dockerfile` / `Dockerfile.jvm` / `Dockerfile.legacy-jar` / `Dockerfile.native*` 是 Quarkus 預設產生，對應 fast-jar / legacy-jar / native 佈局，與本專案 uber-jar 建置模式不符，不可誤用。若 `Dockerfile.uber-jar` 不存在，停止流程並回報使用者（此 skill 不負責建立 Dockerfile 本身）。

### Step 2：讀取當前版本號

從 `bestpartner-service/build.gradle.kts` 讀取 `version = "x.x.x-SNAPSHOT"`（約在檔案尾端 `group = "tw.zipe.basepartner"` 附近），作為 image tag 使用。不得手動猜測或沿用先前對話中出現過的版本號，因為版本可能已變動。

### Step 3：建置 uber-jar（**一律用 prod profile**）

於 `bestpartner-service` 目錄執行：

```bash
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=prod
```

> ⚠️ **建置 profile 固定為 `prod`，不因目標環境改變，也不要問使用者要哪個 profile。**
> 原因（已實測驗證）：建置時的 profile 會成為 image 的**預設 runtime profile**。
> 用 `prod` 建置，代表萬一部署時忘了帶 `QUARKUS_PROFILE`，會落到「Swagger 關閉、log INFO」的安全預設，
> 而不是「Swagger 對外全開、DEBUG 日誌全寫」。各環境的實際 profile 於 `docker run` 時指定即可。

確認輸出含 `BUILD SUCCESSFUL` 且 `build/bestpartner-service-${version}-runner.jar` 存在。若失敗，停止流程並回報錯誤，不進入下一步。

> ⚠️ 若 build 目錄已存在同名 jar 且被其他行程鎖定（Windows 常見於防毒軟體或 IDE 索引），`clean` 可能失敗，重試一次通常可解決；仍失敗才需要請使用者檢查是否有殘留 process 佔用。

### Step 4：docker build

於 `bestpartner-service` 目錄執行：

```bash
docker build -f src/main/docker/Dockerfile.uber-jar -t bestpartner-service:${version} -t bestpartner-service:latest .
```

確認 `docker images bestpartner-service` 能看到剛建立的 image。

### Step 5：啟動驗證（強制執行，不可省略）

啟動暫時容器並確認服務可正常回應，避免產出「建置成功但實際跑不起來」的 image：

```bash
docker run -d --name bestpartner-verify -p 18080:80 bestpartner-service:latest
```

等待數秒後檢查：

```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:18080/view/chat
```

預期 `HTTP 200`。

> ⚠️ **驗證端點必須用 `/view/chat`，不可用 `/q/openapi` 或 `/swagger-ui`。**
> 後兩者已依 profile 管制：image 預設 profile 為 prod，兩者都會回 **404**（這是正確行為，非故障）。
> `/view/chat` 是公開且不受 profile 影響的端點，才適合當存活探測。
>
> 此驗證刻意**不帶** `--env-file`，用途是確認 image 本身能獨立啟動（服務啟動不需要 DB 連線）。

若非 200，先看 `docker logs bestpartner-verify` 找出原因並回報使用者，不可略過此步驟直接宣告完成。

驗證完成後（無論成功或失敗），**必須清理暫存容器**：

```bash
docker stop bestpartner-verify && docker rm bestpartner-verify
```

> ⚠️ 不得遺留 `bestpartner-verify` 容器在背景執行。

### Step 6：匯出 tar 檔（強制執行，不可省略、不必詢問）

驗證通過即自動匯出，不必等使用者要求。**時戳取當下時間**，不要沿用先前對話出現過的時間：

```bash
docker save -o build/bestpartner-service-${version}-prod-$(date +%Y%m%d%H%M).tar bestpartner-service:latest
```

檔名格式：`bestpartner-service-<版本>-<建置profile>-<YYYYMMDDHHmm>.tar`
例：`bestpartner-service-0.1.8-prod-202607162330.tar`

> 時戳格式與專案既有慣例一致（見 `test-confirmation-YYYYMMDDHHmm.md`），不另創格式。
> 用途是檔案被複製出 `build/` 或歸檔後仍能辨識是哪次建置——
> `build/` 內不會累積，因為 Step 3 的 `gradlew clean` 每次都會清空該目錄。

> ⚠️ **`-prod` 指的是「建置時使用的 profile」，不是「只能部署到 prod」。**
> 這份 tar 是**全環境通用**的——同一份 image 靠 `-e QUARKUS_PROFILE=<profile>`
> ＋ `--env-file .env.<profile>` 跑 dev / docker / sit / uat / prod。
> 因 Step 3 規定建置 profile 固定為 `prod`，此後綴實務上恆為 `-prod`。
> **不得**為個別環境產出 `-uat.tar`、`-sit.tar`——那些檔案會位元組完全相同，徒增誤解。

輸出路徑固定在 `bestpartner-service/build/`（該目錄已在 `.gitignore` 排除，不會誤入版控）；不得另建 `dist/`、`docker-export/` 等目錄。完成後確認檔案存在並記下檔案大小。

> ⚠️ **此 tar 內含 JWT 簽章私鑰**（`privateKey.pem` 隨 uber-jar 打包）。回報時必須提醒使用者：
> 這是帶密鑰的產物，不可當一般建置產物在不受控管道流通。

### Step 7：回報結果

告知使用者：

1. Image tag：`bestpartner-service:${version}` 與 `bestpartner-service:latest`
2. 驗證結果（`/view/chat` HTTP 200）
3. 匯出 tar 的完整路徑與檔案大小 + 私鑰提醒
4. 目標機器匯入與執行指令：

```bash
docker load -i bestpartner-service-${version}-prod-<YYYYMMDDHHmm>.tar

# 檔名的 -prod 是建置 profile、後面是建置時間；同一份 tar 可跑任何環境，
# 由 QUARKUS_PROFILE 與 .env.<profile> 決定（需先備妥於目標機器）
# .env 內的 ${enc::...} 密文由同檔的 CONFIG_ENCRYPTION_KEY 解密，無需額外注入
docker run -d -p 80:80 \
  -e QUARKUS_PROFILE=uat \
  --env-file .env.uat \
  -v bestpartner-data:/opt/bestpartner \
  bestpartner-service:${version}
```

5. 提醒 profile 對應行為（見下表）

## 環境 profile 對照

| profile | 用途 | Swagger UI / OpenAPI | Hibernate SQL 日誌 | log 等級 |
|---------|------|---------------------|-------------------|---------|
| （未指定） | 安全預設＝prod | 關閉 | 關 | INFO |
| `dev` | 本機開發 | 開放 | 開 | DEBUG |
| `docker` | 本機容器測試 | 開放 | 開 | DEBUG |
| `sit` | 整合測試 | 開放 | 開 | DEBUG |
| `uat` | 使用者驗收 | 關閉 | 關 | INFO |
| `prod` | 生產 | 關閉 | 關 | INFO |

各 profile 的實際連線位址與機密由對應的 `.env.<profile>` 提供，範本見根目錄 `.env.example`。

## 常見錯誤

| 錯誤 | 正確做法 |
|------|---------|
| 逐步手打指令而不用現成腳本 | 預設跑 `./scripts/docker-package.sh`（Windows：`pwsh ./scripts/docker-package.ps1`） |
| 只改了 `.sh` 或只改了 `.ps1` | 兩份實作必須同步，CI 會比對結論並擋下 |
| 改了流程只更新腳本或只更新本文件 | 兩者須同步，否則規格與實作漂移 |
| 在 PowerShell 未加引號傳 `-D` 參數 | 須寫成 `'-Dquarkus.profile=prod'`；否則 PowerShell 會拆開參數，Gradle 收到 `.package.type=uber-jar` 當 task 名而失敗 |
| 用 `Dockerfile.jvm` 或 `Dockerfile` 建置 | 一律用 `Dockerfile.uber-jar`（Step 1） |
| 為每個環境各建一份 image | 只建一份（prod profile），環境差異靠 runtime `-e QUARKUS_PROFILE` + `--env-file`（Step 3） |
| 建置時問使用者要哪個 profile / 用 dev 建置 | 固定 `-Dquarkus.profile=prod`，這是刻意的安全預設（Step 3） |
| 未先重新建置 jar 就直接 docker build，用到舊 jar | 每次都先跑 Step 3 建置最新 uber-jar |
| 驗證時打 `/q/openapi` 或 `/swagger-ui`，看到 404 誤判為故障 | 用 `/view/chat`；預設 prod 下前兩者 404 是正確行為（Step 5） |
| docker build 完就宣告完成，不驗證能否啟動 | 必須跑 Step 5，確認 HTTP 200 才算完成 |
| 驗證用的暫存容器忘記清理 | Step 5 結尾強制 stop + rm |
| 等使用者要求才匯出 tar | Step 6 為強制且自動執行，驗證通過就直接匯出 |
| tar 檔存到 `build/` 以外的路徑 | 固定存 `bestpartner-service/build/` |
| 為每個環境各產一份 `-uat.tar` / `-sit.tar` | 只產一份；`-prod` 是建置 profile，內容全環境通用（Step 6） |
| tar 時戳沿用對話中出現過的時間或憑記憶填寫 | 以 `date +%Y%m%d%H%M` 取當下時間（Step 6） |
| image tag 沿用舊版本號或憑記憶猜測 | 每次從 `build.gradle.kts` 重新讀取版本（Step 2） |
