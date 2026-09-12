---
name: config-secret
description: Use when encrypting or decrypting configuration secrets in the BestPartner project - producing ${enc::<密文>} values for .env.<profile>, verifying an existing 密文 decrypts correctly, or rotating CONFIG_ENCRYPTION_KEY. Triggered by "加密設定", "產生密文", "解密驗證", "換金鑰", "金鑰輪替", "enc::", "AEADBadTagException", "encrypt config secret", "rotate key".
---

# 設定值加解密（`${enc::<密文>}`）

## ⚠️ 先確認你要處理的是哪一把金鑰

本專案有**兩把互不相干的金鑰**，混用會導致解不開且不易察覺：

| 金鑰 | 加密對象 | 工具 | 本 skill 涵蓋 |
|------|---------|------|--------------|
| `CONFIG_ENCRYPTION_KEY` | **設定值**：`.env.<profile>` 與 `application.properties` 中的 `${enc::<密文>}` | `ConfigSecretUtil` + gradle task | ✅ |
| `CRYPTO_SECRET_KEY` | **資料庫欄位**：`llm_setting.api_key`、tool/MCP 設定 JSON、workflow `secretHeaders` | `CryptoUtils` + Converter，執行期自動 | ❌ 走 [`data-secret`](../data-secret/SKILL.md) |

若要處理的是資料庫欄位（`__SECRET_KEPT__` 遮罩、`SensitiveValueCodec`、`WorkflowSecretConverter`），**不要用本 skill 的指令**——密文格式與金鑰推導方式都不同，改用 [`data-secret`](../data-secret/SKILL.md) skill。

## 機制鏈路

```
.env.<profile>            DB_PASSWORD=${enc::<密文>}
                          CONFIG_ENCRYPTION_KEY=<根金鑰>   ← 與密文同檔
   ↓ docker --env-file（原樣傳入，不做 shell 展開）
application.properties    quarkus.datasource.password=${DB_PASSWORD:${enc::<預設密文>}}
   ↓ SmallRye Config 展開 config expression
EncSecretKeysHandlerFactory（短名 `enc`，等價內建 `aes-gcm-nopadding`）
   ↓ AES-256-GCM 解密（金鑰 = SHA-256(金鑰字串)）
明文
```

密文格式：`Base64URL( [1 byte IV 長度][12 bytes IV][ciphertext+tag] )`，GCM tag 128 bits。
**同一份密文 `${enc::}` 與 `${aes-gcm-nopadding::}` 兩種前綴都能解**，日常一律用短名。

## 各檔案的金鑰歸屬（重要）

**每個 env 檔各有一把自己的 `CONFIG_ENCRYPTION_KEY`**，彼此獨立。
換掉某一檔的金鑰，只需重產該檔的密文，不波及其他檔。

| 檔案 | 金鑰來源 | 目前的密文欄位 |
|------|---------|--------------|
| `.env`（dev 本機） | 檔內 `CONFIG_ENCRYPTION_KEY` | `OPENROUTER_API_KEY`、`DB_USERNAME`、`DB_PASSWORD` |
| `.env.docker` | 檔內 | `DB_PASSWORD` |
| `.env.sit` | 檔內 | `DB_PASSWORD` |
| `.env.uat` | 檔內 | `DB_PASSWORD` |
| `application.properties` | 檔內**預設值** `dev-only-config-key-not-a-real-secret` | datasource `username` / `password` 的預設值 |

> `.env.example` 與 `.env.prod` 不含真實密文（前者為範本，後者不進版控）。
> 開始工作前**先實際 grep 確認**，不要只信這張表：
> `grep -n "enc::" .env*` （輸出含密文，勿貼進對話或工單）

## 流程 A：加密一個新機密

```powershell
# Windows（機密走環境變數，不進命令列參數與 shell 歷史）
$env:CONFIG_ENCRYPTION_KEY='<該檔的根金鑰>'
$env:CONFIG_SECRET_VALUE='<機密明文>'
& "$PWD\bestpartner-service\gradlew.bat" encryptConfigSecret -q -p bestpartner-service
```

```bash
# Linux / macOS
cd bestpartner-service
CONFIG_ENCRYPTION_KEY='<該檔的根金鑰>' CONFIG_SECRET_VALUE='<機密明文>' ./gradlew encryptConfigSecret -q
```

把輸出填回設定檔，**外框自己加**（task 只輸出密文本體）：

```dotenv
DB_PASSWORD=${enc::<剛才的輸出>}
```

接著**必做流程 B 驗證**——填錯外框或漏字元不會在此步報錯，會等到服務啟動才炸。

> ⚠️ **只加密「真實機密」**。未填值的佔位符（如 `OPENAI_API_KEY=your-openai-api-key-here`）
> 維持明文——加密佔位符會讓人誤以為裡面藏著真金鑰，反而降低可讀性。

## 流程 B：驗證既有密文

```powershell
$env:CONFIG_ENCRYPTION_KEY='<該檔的根金鑰>'
$env:CONFIG_SECRET_VALUE='<密文本體，不含 ${enc:: } 外框>'
& "$PWD\bestpartner-service\gradlew.bat" decryptConfigSecret -q -p bestpartner-service
```

輸出應等於原始明文。金鑰或密文不符時以 `javax.crypto.AEADBadTagException: Tag mismatch` 失敗
（GCM 完整性驗證），**不會回傳錯誤明文**——這是設計上的好性質，可安心用來判斷密文是否對應某把金鑰。

> 已實測：以 `dev-only-config-key-not-a-real-secret` 解 `application.properties` 的
> username 密文可正確回傳 `pguser`；換錯金鑰即 `Tag mismatch`。

## 流程 C：輪替 `CONFIG_ENCRYPTION_KEY`

**一個檔案的金鑰與其所有密文必須同一批換掉**，中途狀態（新金鑰 + 舊密文）會直接啟動失敗。

1. **盤點**該檔所有 `${enc::}` 欄位：`grep -n "enc::" .env.<profile>`
2. **逐一以舊金鑰解密**（流程 B），把明文暫存在記憶體／不落地的位置，**不要寫進暫存檔**
3. **產生新金鑰**：高強度隨機字串，且**與 `CRYPTO_SECRET_KEY` 不同值**
4. **逐一以新金鑰加密**（流程 A）
5. **同批寫回**：該檔的 `CONFIG_ENCRYPTION_KEY` 與**全部**密文欄位一起替換
6. **逐欄驗證**（流程 B，用新金鑰），確認每個值都解回步驟 2 的明文
7. **啟動驗證**：以該 profile 實際啟動，確認無 `AEADBadTagException`

### 特例：改動 `application.properties` 的預設金鑰

若動到的是 `smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key` 的**預設值**
（第 148 行的 `dev-only-config-key-not-a-real-secret`），則**必須同步以新預設金鑰重產** datasource
`username` / `password` 的預設密文（第 57–58 行），否則**全新 clone（無任何 .env）會啟動失敗**。

各環境 `.env` 都有提供 `DB_USERNAME` / `DB_PASSWORD`，環境變數存在時預設值不會被求值，故不受影響。

> 這組預設密文是**衛生措施而非安全措施**——金鑰與密文同在版控，clone 即可解密。
> 目的僅是讓版控中的設定檔不出現 `pguser` / `pgpass` 字樣。

## 這個機制實際擋得住什麼

`CONFIG_ENCRYPTION_KEY` **就放在 `.env.<profile>` 內、與密文同檔**（刻意取捨，換取部署簡單）：

| 情境 | 擋得住？ |
|------|---------|
| 明文被瞥見、截圖、誤貼到聊天室／工單 | ✓ |
| 設定檔內容意外寫入日誌或錯誤訊息 | ✓ |
| 「設定檔不得存放明文密碼」的形式要求 | ✓ |
| **`.env.<profile>` 檔案本身外洩** | ✗ 金鑰在同一個檔，拿到即可解密 |
| 已取得主機存取權的攻擊者 | ✗ |

**別在文件、commit 訊息或對話中把它描述成更強的保護。**
要防到「檔案外洩」層級，金鑰必須與 `.env` 分離（獨立檔案／Vault／KMS／CI secret），
部署時以 `-e CONFIG_ENCRYPTION_KEY=...` 單獨注入。

## 排查表

| 症狀 | 原因 | 處置 |
|------|------|------|
| 啟動時 `AEADBadTagException: Tag mismatch` | 金鑰與密文不配對 | 確認用的是**該檔**的金鑰；跑流程 B 逐欄找出解不開的那個 |
| 全新 clone（無 .env）啟動失敗 | `application.properties` 預設金鑰與預設密文不同步 | 見流程 C 特例 |
| 密文看似正確但值錯亂 | 外框寫成 `${enc:<密文>}`（單冒號）或含多餘空白 | 外框固定 `${enc::` 兩個冒號 + `}` |
| 值被當成字面字串沒解密 | 該值不在 config expression 求值路徑上 | 確認鏈路：`.env` → `application.properties` 的 `${VAR:...}` → SmallRye 展開 |
| task 報「缺少明文／缺少根金鑰」 | 環境變數未設定 | PowerShell 用 `$env:X='...'`，同一次 session 內才有效 |
| `./gradlew` 找不到 | wrapper 在 `bestpartner-service/`，不在 repo 根目錄 | 用 `-p bestpartner-service` 或先 `cd bestpartner-service` |

## 收尾檢查

- [ ] 新增／變更的密文已跑過流程 B 驗證
- [ ] 對話、commit 訊息、測試確認表中**沒有出現明文機密或根金鑰**
- [ ] 若改動了設定鍵或各檔密文歸屬 → 呼叫 `documentation-sync` skill
      （需同步 `.claude/rules/configuration-and-profiles.md`、`AGENTS.md` ⇄ `.claude/CLAUDE.md`、
      `docs-site/docs/getting-started/installation.md`、`.env.example`）
- [ ] commit 訊息走 `git-commit-message` skill（如 `設定: 輪替 sit 環境設定加密金鑰`）
