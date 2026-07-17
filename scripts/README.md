# scripts/

BestPartner 專案的可執行腳本集中於此目錄。**每支腳本有兩份實作**：

| 平台 | 用 | 需要 |
|------|-----|------|
| Linux / macOS | `.sh`（原生 bash） | 無額外安裝 |
| Windows | `.ps1`（PowerShell） | 無額外安裝 |

```bash
# Linux / macOS — 直接執行，不需安裝 PowerShell
./scripts/harness-drift-scan.sh
./scripts/docker-package.sh
```

```powershell
# Windows
pwsh ./scripts/harness-drift-scan.ps1
pwsh ./scripts/docker-package.ps1
```

> ⚠️ 腳本一律**從 repo 根目錄呼叫**。兩種實作皆自行推導 repo 根目錄
> （bash 用 `$BASH_SOURCE`、pwsh 用 `$PSScriptRoot`，各取上一層），不依賴當前工作目錄，
> 但文件與 CI 的呼叫慣例統一為根目錄相對路徑。

> 註：`bestpartner-service/gradlew` 等 Gradle wrapper 由 Gradle 產生並固定在各模組目錄，
> 不屬於本目錄管轄，不可搬移。

## ⚠️ 雙份實作的維護契約

`.sh` 與 `.ps1` 是**同一套規則/流程的兩份實作**。修改任一份時**另一份必須同步**——
包含規則、基線清單、參數與輸出格式。

這份重複是刻意接受的成本（換取 Linux/macOS 免裝 pwsh），但重複必然漂移，
所以漂移本身有機械化把關：

- **CI 同時執行 `.sh` 與 `.ps1`，並比對兩者的 exit code 與結論摘要**
  （`.github/workflows/harness.yml` 的「兩份實作結論一致」步驟）。
  只改一邊 → 結論分歧 → CI 紅燈擋下。
- 此守衛已用反向測試驗證：抽掉 `.sh` 的一項基線後，
  bash 回報「基線 4 項、新漂移 1 項 / rc=1」而 pwsh 回報「基線 5 項、新漂移 0 項 / rc=0」，
  比對步驟正確失敗。

> 此步驟不可移除——它是雙份實作能被信任的唯一理由。
> 若哪天決定收斂回單一實作，先刪腳本再刪守衛，順序不可反。

## 腳本一覽

| 腳本 | 用途 | 是否進 CI |
|------|------|----------|
| [`harness-drift-scan.sh`](harness-drift-scan.sh) / [`.ps1`](harness-drift-scan.ps1) | 慣例漂移掃描，發現新漂移時 exit 1 | ✅ 必跑（兩份都跑） |
| [`docker-package.sh`](docker-package.sh) / [`.ps1`](docker-package.ps1) | 建置 / 驗證 / 匯出 Docker image | ✗ 本機執行 |
| [`run_api_tests.ps1`](run_api_tests.ps1) | 早期 API 冒煙測試草稿（**已不建議使用**，見下） | ✗ |

### 行尾（CRLF）與執行位元

根目錄 `.gitattributes` 釘死 `*.sh`、`gradlew` 為 `eol=lf`。**這不是可選項**：
本 repo 以 Windows 為主（`core.autocrlf` 常為 `true`），若 `.sh` 帶 CRLF 進版控，
Linux 的 bash 會在 shebang 就失敗，訊息還極難懂：

```
/usr/bin/env: 'bash\r': No such file or directory
```

`.sh` 與 `gradlew` 在 git 索引中須為 `100755`（帶執行位元），否則 Linux/macOS
全新 clone 後會 `Permission denied`。檢查與修復：

```bash
git ls-files -s scripts/ | grep -E '\.sh$'      # 應為 100755
git update-index --chmod=+x scripts/foo.sh
```

> ⚠️ CI 的 ArchUnit 步驟自帶 `chmod +x ./gradlew`，所以 gradlew 權限掉了**不會在 CI 紅燈**，
> 只會打到本機 Linux/macOS 使用者。Gradle 重新產生 wrapper 後尤其要留意。

## 腳本一覽

| 腳本 | 用途 | 是否進 CI |
|------|------|----------|
| [`harness-drift-scan.ps1`](harness-drift-scan.ps1) | 慣例漂移掃描，發現新漂移時 exit 1 | ✅ 必跑 |
| [`docker-package.ps1`](docker-package.ps1) | 建置 / 驗證 / 匯出 Docker image | ✗ 本機執行 |
| [`run_api_tests.ps1`](run_api_tests.ps1) | 早期 API 冒煙測試草稿（**已不建議使用**，見下） | ✗ |

---

## `harness-drift-scan` — 慣例漂移掃描

```bash
./scripts/harness-drift-scan.sh          # Linux / macOS
pwsh ./scripts/harness-drift-scan.ps1    # Windows
```

逐項對照 `.claude/rules/` 的黃金規範掃描跨模組漂移，輸出「規則 / 狀態 / 現況 / 建議」表。
補足 ArchUnit（bytecode 層結構不變量）不易檢查的慣例，並涵蓋前後端跨界契約。

現有 9 條規則：

| # | 規則 | 檢查內容 |
|---|------|---------|
| 1 | 套件拼字 | 誤用 `tw.zipe.basepartner`（正確為 `bastpartner`） |
| 2 | i18n 寫死字串 | `ServiceException("...")` 字面訊息，應改用 `AppMessage` |
| 3 | 版本硬編碼 | `build.gradle.kts` 內硬編碼版本，應移至 `gradle.properties` |
| 4 | Entity 命名 | `@Entity` 類別須以 `Entity` 結尾（嵌入式 `*Id` 排除） |
| 5 | CDI 標註 | `service` / `repository` 套件須標 `@ApplicationScoped` |
| 6 | 金鑰外洩 | `docs/sql/` 含真實 API 金鑰 |
| 7 | 導覽一致性 | `AGENTS.md` ≡ `.claude/CLAUDE.md`（正規化後逐位元組比對） |
| 8 | NodeType 契約 | 後端 `NodeType.kt` ≡ 前端 `types/workflow.ts` |
| 9 | 測試命名 | 前端新測試須用 `.test.ts` |

**基線機制**：腳本內 `$Baseline` 雜湊表登錄「既存且刻意容忍的漂移」（如 `jsqlparser` 硬編碼版本、
`LLMResource.kt` 的寫死訊息、3 個既存 `.spec.ts`），標為 `BASELINE` 而不失敗；
新增的漂移標為 `NEW` 並 exit 1。**規則 6 / 7 / 8 無基線**——金鑰外洩、導覽文件分歧、
前後端節點型別失聯在任何時候都不該存在。

> 確認某項新漂移可接受時，正確做法是**登錄進 `$Baseline`**，而不是拿掉規則。

結構性不變量（分層依賴、命名、`@Entity` 位置）另由 ArchUnit 強制：

```bash
cd bestpartner-service
./gradlew test --tests "tw.zipe.bastpartner.architecture.ArchitectureTest"
```

---

## `docker-package` — Docker 打包

```bash
# Linux / macOS
./scripts/docker-package.sh                     # 完整流程
./scripts/docker-package.sh --no-tar            # 只建 image，不匯出
./scripts/docker-package.sh --verify-port 18085 # 驗證埠被占用時
./scripts/docker-package.sh --skip-verify       # 跳過啟動驗證（不建議）
```

```powershell
# Windows
pwsh ./scripts/docker-package.ps1                    # 完整流程
pwsh ./scripts/docker-package.ps1 -NoTar             # 只建 image，不匯出
pwsh ./scripts/docker-package.ps1 -VerifyPort 18085  # 驗證埠被占用時
pwsh ./scripts/docker-package.ps1 -SkipVerify        # 跳過啟動驗證（不建議）
```

流程：讀版本 → 建 uber-jar(prod) → `docker build` → 啟動驗證 → 清理暫存容器 → 匯出帶時戳 tar。

| bash | PowerShell | 預設 | 說明 |
|------|-----------|------|------|
| `--verify-port N` | `-VerifyPort N` | `18080` | 啟動驗證用的宿主機埠 |
| `--skip-verify` | `-SkipVerify` | off | 跳過啟動驗證——略過即無法保證 image 跑得起來 |
| `--no-tar` | `-NoTar` | off | 只建 image，不匯出 tar |

> bash 版的啟動驗證需要 `curl`（Linux/macOS 一般內建）。

**建置 profile 固定為 prod，且沒有「環境」參數**——這是刻意設計。建置時的 profile 會成為 image
的預設 runtime profile，用 prod 才能讓部署時漏帶 `QUARKUS_PROFILE` 落在「Swagger 關閉、log INFO」
的安全側。一份 image 跑所有環境，環境差異在 `docker run` 時才決定：

```bash
docker run -d -p 80:80 -e QUARKUS_PROFILE=uat --env-file .env.uat \
  -v bestpartner-data:/opt/bestpartner bestpartner-service:latest
```

存活探測用 `/view/chat`（公開、不受 profile 影響）；`/swagger-ui` 與 `/q/openapi` 在 uat/prod
回 404 是正確行為，不可用來判斷服務是否啟動。

> ⚠️ 匯出的 tar **內含 JWT 簽章私鑰**（`privateKey.pem` 隨 uber-jar 打包），等同憑證，
> 不可在不受控管道流通。

詳見 [`.claude/rules/build-and-run.md`](../.claude/rules/build-and-run.md) 與 `docker-build` skill。

---

## `run_api_tests.ps1` — API 冒煙測試（已不建議使用）

早期手寫的 API 測試草稿，對 AUTH / CHAT / USER / PERMISSION / SYSTEM SETTING / TOOL / MCP /
VECTOR 送出請求並比對 HTTP 狀態碼，結果寫成 JSON。

**正式的 API 測試流程請走 `test-confirmation` skill**（見
[`.claude/rules/api-testing.md`](../.claude/rules/api-testing.md)）——該 skill 會記錄測試 metadata、
強制重啟服務、產生確認表並提醒清理測試資料。本腳本僅保留作為歷史參考。

已修正（原本此腳本在**所有平台**都跑不起來）：

- 第 10 行的 `(Get-Date).ToIsoString()` 方法不存在，一執行即拋 `MethodNotFound` ——
  代表這支腳本自寫成以來從未成功跑完過。已改為 `ToString('o')`。
- 輸出路徑原硬編碼為 `D:\projects\bestpartner\test_results_manual.json`，已改為以
  `$PSScriptRoot` 推導的 repo 相對路徑。

仍存在（未修，因這支腳本已被取代）：

- 帳密與 `BASE_URL` 寫死在檔案開頭
- 期望狀態碼為當時實測值，未必反映現行 API 行為

> 由於它已被 `test-confirmation` skill 完全取代且從未實際運作，**建議直接刪除**。
> 目前僅為保留脈絡而留存。
