---
name: data-secret
description: Use when encrypting or decrypting BestPartner database-column secrets protected by CRYPTO_SECRET_KEY - producing/verifying v2 ciphertext (v2$salt$iv$ct) for columns like llm_setting.api_key or llm_mcp_user_setting.setting_content, bulk-encrypting existing plaintext columns, or rotating CRYPTO_SECRET_KEY. Triggered by "資料庫欄位加密", "加密欄位", "解密欄位", "api_key 加密", "setting_content 加密", "__SECRET_KEPT__", "輪替 CRYPTO_SECRET_KEY", "encryptDataSecret", "decryptDataSecret", "AEADBadTagException", "encrypt data column". NOT for .env ${enc::} config values — that is the config-secret skill.
---

# 資料庫欄位加解密（`CRYPTO_SECRET_KEY`）

## ⚠️ 先確認你要處理的是哪一把金鑰

本專案有**兩把互不相干的金鑰**，密文格式與金鑰推導方式都不同，混用會解不開且不易察覺：

| 金鑰 | 加密對象 | 工具 | 本 skill 涵蓋 |
|------|---------|------|--------------|
| `CRYPTO_SECRET_KEY` | **資料庫欄位** | `CryptoUtils` + JPA Converter（執行期自動）／`encryptDataSecret`·`decryptDataSecret`（CLI） | ✅ |
| `CONFIG_ENCRYPTION_KEY` | **設定值**：`.env.<profile>` 與 `application.properties` 的 `${enc::<密文>}` | `ConfigSecretUtil` + `encryptConfigSecret`·`decryptConfigSecret` | ❌ 走 [`config-secret`](../config-secret/SKILL.md) |

若要處理的是 `${enc::}` 設定值，**不要用本 skill 的指令**。

## 涵蓋的欄位與機制

執行期由 JPA Converter 透明加解密，金鑰來自 `crypto.secret-key`（環境變數 `CRYPTO_SECRET_KEY`）：

| 欄位 | 機制 | 加密範圍 |
|------|------|---------|
| `llm_setting.api_key` | `PasswordEncryptConverter` | 整欄 |
| `llm_mcp_user_setting.setting_content` | `McpSettingEncryptConverter` | 整欄（text，非 JSON） |
| `llm_tool_user_setting.setting_content` | `SensitiveValueCodec`（`@ToolConfigField(sensitive=true)` 標記的 key） | JSON 內逐值 |
| workflow 節點 `config.secretHeaders`（`HTTP_REQUEST`） | `WorkflowSecretConverter` | JSON 內逐值 |

> **對外遮罩**：上述機密經 REST 回應時一律遮罩為 `__SECRET_KEPT__`，明文不跨 HTTP 邊界；
> 客戶端存回時原樣傳回遮罩值即**沿用既有值**（見各 service 的 update 邏輯）。遮罩是 HTTP 邊界防護，與落地加密正交。

## 密文格式

`PasswordEncryptConverter` / `CryptoUtils` 目前輸出 **v2**：

```
v2$<salt_b64>$<iv_b64>$<ciphertext+tag_b64>     ← 四段，$ 分隔
```

- 金鑰以 **PBKDF2-HMAC-SHA256 + per-record 隨機 salt** 由 `CRYPTO_SECRET_KEY` 衍生 AES-256 金鑰。
- 相容 **legacy** 兩段 `<iv>$<ct>`（單次 SHA-256、無 salt）：仍可解密，下次存檔升級為 v2。
- 非上述格式者視為**明文**原樣回傳（相容加密上線前的舊資料）。

CLI（`DataSecretUtil`）與 converter 共用 `CryptoUtils`，故 CLI 產生的密文與服務執行期完全互通（已實測 CLI 可解服務寫入的欄位）。

## 流程 A：加密一個值（產生 v2 密文）

用於手動 seed 欄位、或確認某明文加密後的格式。

```powershell
# Windows（機密走環境變數，不進命令列參數與 shell 歷史）
$env:CRYPTO_SECRET_KEY='<該環境的資料欄位金鑰>'
$env:CRYPTO_SECRET_VALUE='<機密明文>'
& "$PWD\bestpartner-service\gradlew.bat" encryptDataSecret -q -p bestpartner-service
```

```bash
# Linux / macOS
cd bestpartner-service
CRYPTO_SECRET_KEY='<金鑰>' CRYPTO_SECRET_VALUE='<機密明文>' ./gradlew encryptDataSecret -q
```

輸出即 `v2$...$...$...`，可直接寫入對應欄位。本機開發預設金鑰為
`changeme-please-replace-in-production`（`application.properties` 的 `crypto.secret-key` 預設值）。

## 流程 B：解密／驗證既有密文

troubleshoot「這串密文是不是這把金鑰加的」、或輪替前取出原值。

```bash
cd bestpartner-service
CRYPTO_SECRET_KEY='<金鑰>' CRYPTO_SECRET_VALUE='<密文本體，v2 或 legacy>' ./gradlew decryptDataSecret -q
```

輸出應等於原始明文。金鑰或密文不符時以 `javax.crypto.AEADBadTagException: Tag mismatch` 失敗
（GCM 完整性驗證），**不會回傳錯誤明文**——可安心用來判斷密文是否對應某把金鑰。

## 流程 C：批次加密既有明文欄位

加密機制上線前存入的明文列**不會自動加密**（converter 讀取容錯，僅下次存檔才轉密文）。要一次補齊：

**沿用專案既有一次性維護慣例**，寫 pure-JDBC 的 migration test（**非** `@QuarkusTest`），
範本見 `src/test/kotlin/tw/zipe/bastpartner/migration/EncryptMcpSettingContentMigrationTest.kt`：

- 逐列讀原始欄位值 → `isCiphertext` 已是密文者跳過（冪等）→ 明文以 `CryptoUtils.encryptAesGCMv2` 加密回寫。
- **備份** rollback SQL 到 `build/`、逐列 **round-trip 驗證**、單一交易 commit（任一步失敗整批 rollback）。
- 以環境變數守門（如 `ENCRYPT_MCP_SETTING_RUN=true`），預設不執行，避免 CI／一般建置誤觸。

```bash
cd bestpartner-service
ENCRYPT_MCP_SETTING_RUN=true ./gradlew test --tests "tw.zipe.bastpartner.migration.EncryptXxxMigrationTest"
```

> ⚠️ 務必使用**與執行中的服務相同**的 `CRYPTO_SECRET_KEY`，否則服務將無法解密既有列。
> 本機未設 `CRYPTO_SECRET_KEY` 時，服務用預設值，migration 也須用同一把。

## 流程 D：輪替 `CRYPTO_SECRET_KEY`（舊→新）

範本見 `migration/ReEncryptApiKeyMigrationTest.kt`（逐欄 re-encrypt）。**兩把金鑰刻意分離**：
換 `CRYPTO_SECRET_KEY` 不影響 `CONFIG_ENCRYPTION_KEY` 的既有設定值密文。

1. **盤點**所有受 `CRYPTO_SECRET_KEY` 保護的欄位（見上表；每張表各寫一支 migration 或合併處理）。
2. 逐列以**舊金鑰**解密（`decryptAesGCMv2`／`decryptAesGCM`）→ 記憶體暫存，**不落地**。
3. 逐列以**新金鑰**加密（`encryptAesGCMv2`）→ round-trip 驗證。
4. **冪等**：先試新金鑰解密，成功代表已輪替過即跳過；再試舊金鑰，成功才重加密；兩者皆失敗（明文佔位）則不動。
5. 逐表單一交易 commit，附 rollback 備份。
6. 更新部署的 `CRYPTO_SECRET_KEY`（env／`.env`），以新金鑰重啟服務驗證讀寫正常。

## 這個機制實際擋得住什麼

`CRYPTO_SECRET_KEY` 保護的是**資料庫欄位在靜態（at-rest）與備份中的機密**：

| 情境 | 擋得住？ |
|------|---------|
| DB dump／備份檔外流時機密為密文 | ✓ |
| 直接 `SELECT` 欄位看到的是密文 | ✓ |
| 機密經 REST 回應外流（另有 `__SECRET_KEPT__` 遮罩把關） | ✓ |
| **同時取得 DB 內容與 `CRYPTO_SECRET_KEY`** | ✗ 拿到金鑰即可解密 |
| 已取得服務主機／執行期記憶體的攻擊者 | ✗ |

金鑰預設值 `changeme-please-replace-in-production` **僅供本機**；sit 以上務必以 `CRYPTO_SECRET_KEY` 替換。
**別在文件、commit 訊息或對話中貼出明文機密或金鑰。**

## 排查表

| 症狀 | 原因 | 處置 |
|------|------|------|
| 服務讀欄位得到密文而非明文 | 該值非本機制格式、或金鑰不符 | 跑流程 B 確認密文可用該金鑰解；查是否 legacy／明文殘留 |
| `AEADBadTagException: Tag mismatch` | 金鑰與密文不配對 | 確認用的是**該環境**的 `CRYPTO_SECRET_KEY`（非 `CONFIG_ENCRYPTION_KEY`） |
| CLI 產的密文服務解不開 | CLI 與服務用了不同 `CRYPTO_SECRET_KEY` | 兩邊金鑰須一致（本機皆為預設值） |
| migration 後仍有明文列 | 該列在 `isCiphertext` 判定或守門變數上被跳過 | 確認守門 env 已設、且該列非既有密文 |
| `無法辨識的密文格式` | decrypt 傳入的不是 v2 四段或 legacy 兩段 | 確認貼的是密文本體、未含多餘引號／空白 |
| `./gradlew` 找不到 | wrapper 在 `bestpartner-service/` | 用 `-p bestpartner-service` 或先 `cd bestpartner-service` |

## 收尾檢查

- [ ] 加密／輪替的欄位已跑過流程 B（或 migration 內建 round-trip）驗證
- [ ] 對話、commit 訊息、測試確認表中**沒有出現明文機密或金鑰**
- [ ] migration test 屬本機一次性工具，依既有慣例**可不進版控**（比照 `ReEncryptApiKeyMigrationTest`）
- [ ] 若新增受保護欄位或改動金鑰歸屬 → 呼叫 `documentation-sync` skill
      （需同步 `.claude/rules/configuration-and-profiles.md`、`.claude/rules/architecture-and-packages.md`、
      `AGENTS.md` ⇄ `.claude/CLAUDE.md`）
- [ ] commit 訊息走 `git-commit-message` skill（如 `設定: 輪替 sit 環境資料欄位加密金鑰`）
