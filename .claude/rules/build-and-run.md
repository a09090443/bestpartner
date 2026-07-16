# 建置與執行

## 建置

切換至 `bestpartner-service` 目錄，執行：

```bash
./gradlew clean build -x test \
  -Dquarkus.package.type=uber-jar \
  -Dorg.gradle.daemon=false \
  -Dquarkus.profile=${profile}
```

`${profile}` 可選值：`dev`（預設）、`sit`、`prod`

產生的 jar：`bestpartner-service/build/bestpartner-service-0.1.8-SNAPSHOT-runner.jar`

## 執行

```bash
java -jar bestpartner-service-0.1.8-SNAPSHOT-runner.jar
```

服務預設埠：**port 80**

## Docker Image

**一份 image 跑所有環境**，環境差異全部在 runtime 決定，不為個別環境重建 image。
**打包指令不隨環境變化**——沒有「sit 的打包方式」這種東西。

### 建置：用 `docker-package.ps1`（推薦）

repo 根目錄的腳本已涵蓋建置、驗證、清理與帶時戳匯出：

```bash
pwsh ./docker-package.ps1                      # 完整流程
pwsh ./docker-package.ps1 -NoTar               # 只建 image
pwsh ./docker-package.ps1 -VerifyPort 18085    # 驗證埠被占用時
```

以 PowerShell Core 撰寫，Windows / Linux / macOS 通用（同 `harness-drift-scan.ps1` 慣例）。

### 建置：手動（profile 固定 prod）

```bash
# 1. 建 uber-jar —— profile 一律 prod，不因目標環境改變
./gradlew clean build -x test -Dquarkus.package.type=uber-jar \
  -Dorg.gradle.daemon=false -Dquarkus.profile=prod

# 2. 建 image
docker build -f src/main/docker/Dockerfile.uber-jar \
  -t bestpartner-service:0.1.8-SNAPSHOT -t bestpartner-service:latest .
```

> ⚠️ 在 PowerShell 手動執行第 1 步時，`-D` 參數必須加引號
> （`'-Dquarkus.package.type=uber-jar'`），否則 PowerShell 會拆開參數，
> Gradle 會收到 `.package.type=uber-jar` 並當成 task 名稱而失敗。

> ⚠️ **為何固定用 `prod` 建置**：建置時的 profile 會成為 image 的**預設 runtime profile**。
> 用 prod 建置，代表部署時忘記帶 `QUARKUS_PROFILE` 也會落到「Swagger 關閉、log INFO」的安全預設。
> 用 dev 建置則相反——實測顯示 Swagger 會在 prod 執行時仍對外開放（`always-include` 是 build-time 設定，切 profile 關不掉）。

### 執行（依環境指定 profile 與 env 檔）

```bash
docker run -d -p 80:80 \
  -e QUARKUS_PROFILE=uat \
  --env-file .env.uat \
  -v bestpartner-data:/opt/bestpartner \
  bestpartner-service:latest
```

profile 對照與環境變數清單 → [`configuration-and-profiles.md`](configuration-and-profiles.md)

### 說明

- `Dockerfile.uber-jar` 專為此專案的 uber-jar 建置模式撰寫，`COPY build/*-runner.jar` 對應上方產生的單一 jar；`src/main/docker/` 下其餘 Quarkus 預設產生的 `Dockerfile` / `Dockerfile.jvm` / `Dockerfile.legacy-jar` / `Dockerfile.native*` 對應 fast-jar / legacy-jar / native 佈局，與本專案建置指令不符，**不適用**。
- Image 內 `EXPOSE 80` 對應 `quarkus.http.port`（預設 80），非 Quarkus 樣板預設的 8080。
- Base image：`registry.access.redhat.com/ubi8/openjdk-21:1.20`，以 `run-java.sh` 啟動，可透過 `JAVA_OPTS_APPEND` 等環境變數調整 JVM 參數（詳見 `Dockerfile.jvm` 註解）。
- 容器以 **uid 185（jboss）** 執行，`/var/log` 不可寫；日誌與上傳目錄請用 `/opt`、`/tmp` 或 `/deployments`。
- **存活探測用 `/view/chat`**（公開、不受 profile 影響）。`/q/openapi` 與 `/swagger-ui` 在預設 prod 下回 404 是正確行為，不可用來判斷服務是否啟動。

### 匯出至其他機器

```bash
docker save -o build/bestpartner-service-0.1.8-SNAPSHOT-prod-$(date +%Y%m%d%H%M).tar \
  bestpartner-service:latest
# 目標機器
docker load -i bestpartner-service-0.1.8-SNAPSHOT-prod-202607162330.tar
```

檔名格式：`bestpartner-service-<版本>-<建置profile>-<YYYYMMDDHHmm>.tar`
（時戳格式同專案既有慣例，見 `docs/test-confirmations/`）

> **`-prod` 是建置 profile，不是部署目標。** 這份 tar 全環境通用，跑哪個環境由
> `-e QUARKUS_PROFILE=<profile>` ＋ `--env-file .env.<profile>` 決定。
> 不要為個別環境另存 `-uat.tar` / `-sit.tar`——內容會位元組相同，只會造成誤解。

> ⚠️ **tar 檔內含 JWT 簽章私鑰**（`privateKey.pem` 隨 uber-jar 打包），等同憑證，不可在不受控管道流通。

> 完整流程（建置、啟動驗證、清理、匯出 tar）已自動化為 `docker-build` skill，見 [`.claude/skills/docker-build/SKILL.md`](../skills/docker-build/SKILL.md)。
