# API 變動時的 Checklist 維護規則

**任何時候 API 發生新增、修改或刪除，必須同時更新 checklist 模板。**

## 觸發條件

| API 變動類型 | 檢查點 | 必要動作 |
|-------------|--------|--------|
| 新增 endpoint | 新的 `@Path` 註解 | 在 checklist 對應模組表格新增測試案例列，並在「已驗證 API 範例」加入 curl 範本 |
| 修改 endpoint 路徑 | 更改 `@Path` 的路徑值 | 更新 checklist 表格中的端點路徑，更新已驗證範例中的 curl 命令 |
| 修改 endpoint 參數 | 更改 request/response 結構 | 更新 checklist 備註欄，更新已驗證範例中的 request body 和 response |
| 修改權限要求 | 改變 `@RolesAllowed` 或 `@Authenticated` | 更新 checklist 表格對應行的備註，說明新的權限要求 |
| 刪除 endpoint | 移除整個方法或 `@Path` | 在 checklist 對應案例旁加註「已廢除」，不刪除列（保留歷史記錄） |

## 詳細規則

### 新增 Endpoint 時

1. **更新 checklist 的模組表格**
   - 在對應模組的測試案例表格中新增行
   - 測試 ID 格式：`{MODULE}-{NUMBER}` (例：`VECTOR-020`)
   - 優先級：根據功能重要性判斷（P0/P1/P2）
   - 方法、端點、狀態、備註欄位如實填寫

2. **添加到已驗證 API 範例部分**
   - 若新 endpoint 已測試過，添加完整 curl 命令和預期響應
   - 格式參照現有的已驗證範例（含 HTTP 狀態碼、JSON body）
   - 若尚未測試，先在相應模組後新增佔位符註記（「待測試」）

### 修改 Endpoint 時

1. **更新 checklist 中的端點行**
   - 修改路徑、參數、備註欄位
   - 若修改涉及認證或權限，在備註欄補充說明

2. **同步更新已驗證範例**
   - 更新對應的 curl 命令和 request body
   - 更新預期的 response（若 response 結構變更）
   - 添加修改說明的註釋

### 刪除 Endpoint 時

1. **保留 checklist 中的案例行，但標記為「已廢除」**
   ```markdown
   | {TEST-ID} | {場景} | {優先級} | {方法} | {端點} ~~（已廢除）~~ | — | 服務版本 X.X.X 起不再支援 |
   ```

2. **移除已驗證範例中對應的 curl 命令**
   - 添加註釋說明該 API 已廢除及廢除版本

## 執行流程

完成任何 API 變動後，**必須在提交 code 前同時提交 checklist 更新**：

```
1. 編寫或修改 API endpoint 代碼
2. 更新 .claude/skills/test-confirmation/test-confirmation-checklist.md
3. 執行 git add & git commit（包含 code + checklist）
4. 若有已驗證的測試結果，同時更新 docs/api-test-examples.md
```

## 檢查清單

提交前確認：
- [ ] checklist 中的測試模組表格已更新
- [ ] 若 endpoint 已測試，已驗證範例部分已包含 curl 命令和響應
- [ ] 若 endpoint 新增了權限要求，在備註欄有清楚說明
- [ ] 已刪除的 endpoint 保留在表格中但標記為「已廢除」
- [ ] checklist 和 code 變更一起提交（同一個 commit）
