### WORKFLOW — Workflow 模組（`/llm/workflow`）

> 類別層級 `@Authenticated`；擁有權與 admin 例外於 service 層檢核。
> 權威案例來源：`docs/api-test-plan.md` 的 WORKFLOW 章節（含「多觸發點各自獨立執行」小節）。

> ⚠️ **DTO 欄位名易踩雷**（週期 202609122031 實測）：
> - 節點用 **`type`**，不是 `nodeType`
> - edge **沒有 `edgeKey` 欄位**（只有 sourceNodeKey / targetNodeKey / sourceHandle / targetHandle / label / condition）
> - config 為嚴格 JSON（`ignoreUnknownKeys=false`），多帶一個鍵就 400

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| WF-001 | create 建立空白 workflow | P0 | POST | `/llm/workflow/create` | | 預期 DRAFT、version 1、nodes/edges 為空 |
| WF-002 | save 合法 nodes/edges 整張覆寫 | P0 | POST | `/llm/workflow/save` | | version+1，get 可完整還原 |
| WF-003 | save 節點 config 含未知欄位 | P1 | POST | `/llm/workflow/save` | | 400，訊息含 nodeKey 與該未知鍵 |
| WF-004 | save 允許草稿缺必填 | P1 | POST | `/llm/workflow/save` | | LLM_ASSISTANT 缺 llmId 仍可存 DRAFT → 200 |
| WF-005 | save 樂觀鎖（version 不符） | P1 | POST | `/llm/workflow/save` | | 400，`Workflow has been modified by another session, please reload` |
| WF-006 | get 取得完整定義 | P0 | POST | `/llm/workflow/get` | | `HTTP_REQUEST` 的 `secretHeaders` 值須為 `__SECRET_KEPT__` |
| WF-007 | list 列出擁有的 workflow | P1 | GET | `/llm/workflow/list` | | |
| WF-008 | update 僅更新 meta | P1 | POST | `/llm/workflow/update` | | name/description/canvasMeta |
| WF-009 | 非擁有者存取 | P2 | POST | `/llm/workflow/get` | | 400 `No permission to access this workflow` |
| WF-010 | nodeRequiredFields 取必填清單 | P2 | GET | `/llm/workflow/nodeRequiredFields` | | 須含全 **13** 種 NodeType |
| WF-011 | switchStatus 啟用驗必填 | P1 | POST | `/llm/workflow/switchStatus` | | 缺必填 → 400 `workflow.node.config.required.missing`，含 nodeKey 與欄位 |
| WF-012 | switchStatus 啟用驗提示接線 | P1 | POST | `/llm/workflow/switchStatus` | | 孤兒 PROMPT → 400 `workflow.prompt.node.not.connected` |
| WF-013 | switchStatus 啟用驗提問來源 | P1 | POST | `/llm/workflow/switchStatus` | | LLM 無 userPrompt 也無 PROMPT 連入 → 400 `workflow.llm.prompt.required` |
| WF-014 | **execute happy path** | P0 | POST | `/llm/workflow/execute` | | SSE：`execution.started` → `node.started`/`node.completed` → `execution.completed`(SUCCESS)；DB 落紀錄 |
| WF-015 | execute 帶不存在的 id | P1 | POST | `/llm/workflow/execute` | | 400 `Workflow not found` |
| WF-016 | execute 未帶 token | P1 | POST | `/llm/workflow/execute` | | 401 |
| WF-017 | **指定 `triggerNodeKey` 只跑該分支** | P0 | POST | `/llm/workflow/execute` | | 未選中的 TRIGGER 與其獨佔下游落 SKIPPED；`llm_workflow_execution.trigger_node_key` 記錄入口 |
| WF-018 | `triggerNodeKey` 不存在 | P1 | POST | `/llm/workflow/execute` | | 400 `Specified trigger node not found: <key>`，**不建立**執行紀錄 |
| WF-019 | `triggerNodeKey` 型別非 TRIGGER | P1 | POST | `/llm/workflow/execute` | | 400，含 nodeKey 與實際型別，**不建立**執行紀錄 |
| WF-020 | **LLM 回覆完整性（finishReason）** | P2 | POST | `/llm/workflow/execute` | | 見下方專項說明 |
| WF-021 | delete 刪除 workflow | P1 | POST | `/llm/workflow/delete` | | 連鎖刪 node/edge |

#### WF-020 LLM 回覆完整性專項（2026-09-12 新增把關）

`LlmAssistantExecutor` 會記錄 `finishReason` 與 `tokenUsage`，並在 `LENGTH` / `CONTENT_FILTER` 時讓節點 FAILED。

| 驗證 | 作法 | 預期 |
|------|------|------|
| 正常結束不受影響 | 一般執行 | 日誌有 `推論結束：finishReason=STOP, tokenUsage=…`；節點 SUCCESS |
| 截斷偵測 | **暫時**把該 LLM 設定的 `maxTokens` 調到極小（如 5，`apiKey` 傳 `__SECRET_KEPT__` 沿用），提示要求長輸出 | 日誌 `finishReason=LENGTH`；SSE `node.failed`，error 為 `LLM response was cut off before completion (finish reason: LENGTH); the answer may be truncated`；下游 SKIPPED；整體 FAILED |
| 還原 | 把 `maxTokens` 改回原值 | 以 `/llm/chat` 確認恢復正常 |

> ⚠️ 調 `maxTokens` 前**務必記下原值**，測完立刻還原（週期 202609122031 原值為 32768）。

#### WORKFLOW curl 範本

```bash
# WF-001 create
curl -s -X POST "http://localhost:80/llm/workflow/create" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"apitest-wf","description":"測試"}'

# WF-002 save（雙觸發點匯流同一 LLM；注意 type / 無 edgeKey）
curl -s -X POST "http://localhost:80/llm/workflow/save" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{
  "id":"<WF_ID>","name":"apitest-wf","version":1,
  "nodes":[
    {"nodeKey":"t1","type":"TRIGGER","config":{"triggerType":"MANUAL"},"positionX":0,"positionY":0},
    {"nodeKey":"t2","type":"TRIGGER","config":{"triggerType":"MANUAL"},"positionX":0,"positionY":200},
    {"nodeKey":"promptA","type":"PROMPT","config":{"prompt":"Reply with exactly: BRANCH-A"},"positionX":250,"positionY":0},
    {"nodeKey":"promptB","type":"PROMPT","config":{"prompt":"Reply with exactly: BRANCH-B"},"positionX":250,"positionY":200},
    {"nodeKey":"llm1","type":"LLM_ASSISTANT","config":{"llmId":"<CHAT_LLM_ID>"},"positionX":500,"positionY":100},
    {"nodeKey":"out1","type":"OUTPUT","config":{"template":"{{llm1.reply}}"},"positionX":750,"positionY":100}],
  "edges":[
    {"sourceNodeKey":"t1","targetNodeKey":"promptA","sourceHandle":"out:main","targetHandle":"in:main"},
    {"sourceNodeKey":"t2","targetNodeKey":"promptB","sourceHandle":"out:main","targetHandle":"in:main"},
    {"sourceNodeKey":"promptA","targetNodeKey":"llm1","sourceHandle":"out:main","targetHandle":"in:prompt"},
    {"sourceNodeKey":"promptB","targetNodeKey":"llm1","sourceHandle":"out:main","targetHandle":"in:prompt"},
    {"sourceNodeKey":"llm1","targetNodeKey":"out1","sourceHandle":"out:main","targetHandle":"in:main"}]}'

# WF-010 nodeRequiredFields
curl -s "http://localhost:80/llm/workflow/nodeRequiredFields" -H "Authorization: Bearer $ADMIN_TOKEN"

# WF-014 / WF-017 execute（-N 不緩衝才看得到 SSE）
curl -s -N -X POST "http://localhost:80/llm/workflow/execute" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"<WF_ID>","triggerNodeKey":"t1"}'

# DB 查核
docker exec postgres-db psql -U pguser -d pgdb -t -A -c \
  "SELECT trigger_node_key, status FROM bestpartner.llm_workflow_execution ORDER BY started_at DESC LIMIT 1;"
docker exec postgres-db psql -U pguser -d pgdb -t -A -c \
  "SELECT node_key, status, left(error_message,60) FROM bestpartner.llm_workflow_node_execution WHERE execution_id='<EXEC_ID>' ORDER BY seq_no;"
```
