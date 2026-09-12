# BestPartner API 測試功能確認表

> 本次為**聚焦驗證**：workflow 之外，tool 與 MCP user setting 的敏感欄位加密。
> 其餘標準模組本週期不適用（標記 —）。
> 契約見 `docs-site/docs/features/tools.md`、`mcp-servers.md`；實作 `converter/SensitiveValueCodec.kt`。

---

## 測試週期資訊

| 項目 | 內容 |
|------|------|
| 測試日期 | 2026-07-17 23:50 |
| 服務版本 | 0.1.8-SNAPSHOT |
| 測試環境 | dev |
| LLM 平台 | 不適用（僅驗證設定落地/遮罩，不呼叫 LLM/MCP 執行） |
| Base URL | `http://localhost:80` |
| 測試結束日期 | 2026-07-17 23:55 |

---

## 狀態符號

✅ Pass ／ ❌ Fail ／ ⏭️ Skip ／ — 不適用

---

## 測試結果摘要

| 模組 | 總數 | ✅ Pass | ❌ Fail | ⏭️ Skip | Pass 率 |
|------|------|--------|--------|---------|---------|
| TOOL 敏感欄位加密 | 2 | 2 | 0 | 0 | 100.0% |
| MCP env 值加密 | 3 | 3 | 0 | 0 | 100.0% |
| （其餘標準模組） | — | — | — | — | 本次不適用 |

> 解密使用路徑（`buildTool` / `buildMcpServer`）依賴 chat/MCP 執行流程，本輪以 `SensitiveValueCodec` 單元測試的加密→解密往返覆蓋（9 案），未在此端到端觸發。

---

## TOOL — settingContent 敏感欄位加密（GoogleSearch，敏感=apiKey）

| Test ID | 優先 | 案例 | 預期 | 狀態 |
|---------|:---:|------|------|:---:|
| TOOL-ENC-001 | P1 | saveSetting 帶明文 apiKey，查 DB | apiKey 密文、csi/timeout 明文、不含明文 apiKey | ✅ |
| TOOL-ENC-002 | P1 | updateSetting apiKey 送遮罩 + 改 csi | apiKey 沿用密文、csi 更新、無遮罩落地 | ✅ |

### 證據

| Test ID | 證據 |
|---------|------|
| TOOL-ENC-001 | DB `setting_content` = `{"csi":"017576","apiKey":"Ai7l7AldKxrkLBDj$2+F+…","timeout":5000}`；apiKey 為密文，明文 `AIza-secret-key-001` 出現 0 次 |
| TOOL-ENC-002 | 送 `apiKey:__SECRET_KEPT__` + `csi:999999` → DB apiKey 逐字元沿用原密文、csi=999999、無 `__SECRET_KEPT__` 字面值 |

---

## MCP — settingContent env 值加密（google_drive，env key=CREDENTIALS_FILE_PATH）

| Test ID | 優先 | 案例 | 預期 | 狀態 |
|---------|:---:|------|------|:---:|
| MCP-ENC-001 | P1 | saveSetting 帶 env 值，查 DB | env key 值密文、不含明文 | ✅ |
| MCP-ENC-002 | P1 | getSetting 取回 | env 值回傳 `__SECRET_KEPT__`，明文不外流 | ✅ |
| MCP-ENC-003 | P1 | updateSetting env 送遮罩 | 沿用既有密文、無遮罩落地 | ✅ |

### 證據

| Test ID | 證據 |
|---------|------|
| MCP-ENC-001 | DB `setting_content` = `{"CREDENTIALS_FILE_PATH":"RkQGfzQ7sHUfTng5$Fy/…"}`；密文，明文 `creds-plain-001` 出現 0 次 |
| MCP-ENC-002 | getSetting 回 `{"CREDENTIALS_FILE_PATH":"__SECRET_KEPT__"}`，明文 0 次 |
| MCP-ENC-003 | 送遮罩 → DB 密文逐字元沿用、無 `__SECRET_KEPT__` 字面值 |

---

## 測試資料清理

- [x] MCP user setting 已刪除（`/llm/mcpServer/deleteSetting`）
- [x] Tool user setting 已刪除（無 deleteSetting 端點，SQL 清除，0 殘留）
- [x] 服務（port 80）已停止

> 附記：`llm/tool` 無「刪除 user setting」與「讀回 user setting」端點；MCP `updateSetting` 的 resource 要求 `userSettingId`
> 但 service 以 `settingId` 查詢（既有不一致，非本次引入），測試時兩者帶同一 id 繞過。
