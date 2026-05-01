### MCP — MCP Server 模組（`/llm/mcpServer`）

> #### 範例 MCP 設定參考
>
> **STDIO 類型**：`command` 和 `args` 為頂層欄位（非巢狀在 `commandSetting` 內）
>
> ```json
> {
>   "name": "mcp_name",
>   "type": "STDIO",
>   "command": "java",
>   "args": ["-jar", "path/to/server.jar"]
> }
> ```
>
> **SSE 類型**：
> ```json
> {
>   "name": "mcp_name",
>   "type": "SSE",
>   "server": "http://localhost:8080/sse"
> }
> ```

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| MCP-001 | 列出 MCP（已認證） | P0 | GET | `/llm/mcpServer/list` | | |
| MCP-002 | 列出 MCP（未認證） | P0 | GET | `/llm/mcpServer/list` | | 預期 401 |
| MCP-003 | 取得 MCP（正常） | P1 | POST | `/llm/mcpServer/get` | | |
| MCP-004 | 取得 MCP（mcpId 不存在） | P1 | POST | `/llm/mcpServer/get` | | |
| MCP-005 | 註冊 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/register` | | `command`/`args` 在頂層；測試後刪除 |
| MCP-006 | 註冊 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/register` | | 預期 403 |
| MCP-007 | 註冊 MCP（缺必填欄位） | P1 | POST | `/llm/mcpServer/register` | | |
| MCP-008 | 更新 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/update` | | |
| MCP-009 | 更新 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/update` | | 預期 403 |
| MCP-010 | 刪除 MCP（admin 正常） | P1 | POST | `/llm/mcpServer/delete` | | |
| MCP-011 | 刪除 MCP（一般用戶拒絕） | P1 | POST | `/llm/mcpServer/delete` | | 預期 403 |
| MCP-012 | 儲存使用者設定（正常） | P1 | POST | `/llm/mcpServer/saveSetting` | | `settingContent` 不可為空物件 `{}`；測試後刪除 |
| MCP-013 | 儲存使用者設定（缺 mcpId） | P1 | POST | `/llm/mcpServer/saveSetting` | | |
| MCP-014 | 取得使用者設定（正常） | P1 | POST | `/llm/mcpServer/getSetting` | | |
| MCP-015 | 取得使用者設定（id 不存在） | P2 | POST | `/llm/mcpServer/getSetting` | | |
| MCP-016 | 更新使用者設定（正常） | P1 | POST | `/llm/mcpServer/updateSetting` | | |
| MCP-017 | 刪除使用者設定（正常） | P1 | DELETE | `/llm/mcpServer/deleteSetting` | | |
| MCP-018 | 未認證取得清單 | P2 | GET | `/llm/mcpServer/list` | | 預期 401（與 MCP-002 相同，P2 邊界測試） |
| MCP-019 | 業務邏輯（設定後於聊天使用） | P1 | 多步 | `/llm/mcpServer/*` | | saveSetting → customAssistantChat（含 mcpSettingIds） → deleteSetting |

#### MCP curl 範本

```bash
# MCP-001 列出 MCP（已認證）
curl -s -X GET "http://localhost:80/llm/mcpServer/list" \
  -H "Authorization: Bearer $USER_TOKEN"
# 預期 HTTP 200 → {"code":200,"message":"success","data":[{"name":"google_drive","type":"STDIO","mcpId":"06a7fb6d-..."},{"name":"date","mcpId":"4b9ba306-..."},...]}

# MCP-002 未認證
curl -s -o /dev/null -w "%{http_code}" -X GET "http://localhost:80/llm/mcpServer/list"
# 預期 HTTP 401

# MCP-003 取得特定 MCP
curl -s -X POST "http://localhost:80/llm/mcpServer/get" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"name":"date","type":"STDIO","mcpId":"4b9ba306-...","args":["D:/MCP/date-1.0-SNAPSHOT-runner.jar"],...}}

# MCP-004 mcpId 不存在
curl -s -X POST "http://localhost:80/llm/mcpServer/get" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"00000000-0000-0000-0000-000000000000"}'
# 預期 HTTP 400 → {"code":400,"message":"MCP server not found","data":null}

# MCP-005 admin 註冊 MCP（⚠️ command/args 在頂層，非 commandSetting 巢狀）
curl -s -X POST "http://localhost:80/llm/mcpServer/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_mcp","type":"STDIO","command":"java","args":["-jar","test.jar"]}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"name":"test_mcp","command":"java","args":["-jar","test.jar"],"type":"STDIO","mcpId":"<NEW_MCP_ID>",...}}
# 記下 mcpId，測試後用 MCP-010 刪除

# MCP-006 一般用戶註冊 MCP
curl -s -X POST "http://localhost:80/llm/mcpServer/register" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"hack_mcp","type":"STDIO","command":"malware","args":[]}'
# 預期 HTTP 403 → {"code":403,"message":"權限錯誤","data":null}

# MCP-007 缺必填欄位（STDIO 需 command + args）
curl -s -X POST "http://localhost:80/llm/mcpServer/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_mcp","type":"STDIO"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 command 不可為空值\n欄位 args 不可為空值","data":null}

# MCP-008 更新 MCP（admin）
curl -s -X POST "http://localhost:80/llm/mcpServer/update" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"<MCP_ID>","name":"test_mcp_updated","type":"STDIO","command":"java","args":["-jar","updated.jar"]}'
# 預期 HTTP 200

# MCP-009 一般用戶更新
curl -s -X POST "http://localhost:80/llm/mcpServer/update" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"<MCP_ID>","name":"hack","type":"STDIO","command":"malware","args":[]}'
# 預期 HTTP 403

# MCP-010 刪除 MCP（admin）
curl -s -X POST "http://localhost:80/llm/mcpServer/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"<MCP_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}

# MCP-011 一般用戶刪除
curl -s -X POST "http://localhost:80/llm/mcpServer/delete" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"<MCP_ID>"}'
# 預期 HTTP 403

# MCP-012 儲存使用者 MCP 設定（⚠️ settingContent 不可為空物件）
curl -s -X POST "http://localhost:80/llm/mcpServer/saveSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mcpId":"4b9ba306-2fc5-4aa6-9e54-22bce834e021","settingContent":{"timezone":"Asia/Taipei"}}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"userSettingId":"<SETTING_ID>","settingContent":{"timezone":"Asia/Taipei"},...}}
# 記下 userSettingId，測試後用 MCP-017 刪除

# MCP-013 缺 mcpId
curl -s -X POST "http://localhost:80/llm/mcpServer/saveSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"settingContent":{"timezone":"UTC"}}'
# 預期 HTTP 400

# MCP-014 取得使用者設定
curl -s -X POST "http://localhost:80/llm/mcpServer/getSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"<SETTING_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"settingId":"<SETTING_ID>","settingContent":{...},"mcpId":"4b9ba306-...",...}}

# MCP-015 userSettingId 不存在
curl -s -X POST "http://localhost:80/llm/mcpServer/getSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"00000000-0000-0000-0000-000000000000"}'
# 預期 HTTP 400 → {"code":400,"message":"User setting not found","data":null}

# MCP-016 更新使用者設定
curl -s -X POST "http://localhost:80/llm/mcpServer/updateSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"<SETTING_ID>","settingContent":{"timezone":"UTC"}}'
# 預期 HTTP 200

# MCP-017 刪除使用者設定
curl -s -X DELETE "http://localhost:80/llm/mcpServer/deleteSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userSettingId":"<SETTING_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}
```

---

