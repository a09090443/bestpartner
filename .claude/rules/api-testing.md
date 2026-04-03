# API 測試

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
| TOOL | `/llm/tool` |

### 優先順序定義

| 等級 | 說明 |
|------|------|
| P0 | 核心阻斷性功能，失敗即無法繼續測試 |
| P1 | 主要業務功能，直接影響使用者體驗 |
| P2 | 次要功能、邊界條件、錯誤處理細節 |
