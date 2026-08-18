# API 測試

## 執行測試週期：必須使用 `test-confirmation` skill

**每次執行 API 測試，必須先呼叫 `test-confirmation` skill**，該 skill 會：
1. 詢問測試環境、人員、LLM 平台等 metadata
2. 自動以當下時間建立 `docs/test-confirmations/test-confirmation-YYYYMMDDHHmm.md`
3. 強制重啟服務（port 80）再開始測試
4. 測試完成後強制停止服務
5. 提醒清理測試產生的資料

> ⚠️ 若未透過此 skill 啟動測試流程，視同違規。

> **與 UI / E2E 測試互斥**：本文件只管 API 測試。UI（E2E）測試走 `e2e-test-confirmation` skill，
> 並由 `.claude/hooks/e2e-flow-guard.ps1` 機械化守門（開測前停服務、失敗分流後先報告再修、收尾關服務）。
> 「執行測試」觸發 API 流程、「跑 E2E / UI 測試」觸發 E2E 流程，兩者不共用狀態，不可混用模板。

## Postman Collection

位置：`docs/postman/basepartner.postman_collection.json`

建置時跳過自動化測試（使用 `-x test`）。

## API 測試計畫文件

詳細文件：`docs/api-test-plan.md`

### 測試模組

| 模組 | 路徑前綴 |
|------|----------|
| AUTH | `/login` |
| CHAT | `/llm` |
| ADMIN CHAT | `/llm/admin` |
| USER | `/llm/user` |
| LLM SETTING | `/llm/setting` |
| VECTOR | `/llm/vector` |
| SKILL | `/llm/skill` |
| TOOL | `/llm/tool` |
| MCP SERVER | `/llm/mcpServer` |
| PERMISSION | `/llm/permission` |
| WORKFLOW | `/llm/workflow` |
| SYSTEM SETTING | `/systemSetting` |
| VIEW | `/view` |

### 優先順序定義

| 等級 | 說明 |
|------|------|
| P0 | 核心阻斷性功能，失敗即無法繼續測試 |
| P1 | 主要業務功能，直接影響使用者體驗 |
| P2 | 次要功能、邊界條件、錯誤處理細節 |
