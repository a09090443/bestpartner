### TOOL — 工具模組（`/llm/tool`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| TOOL-001 | 列出工具（正常） | P0 | GET | `/llm/tool/list` | | 公開，無需認證 |
| TOOL-002 | 取得工具（正常） | P1 | POST | `/llm/tool/get` | | 公開，無需認證 |
| TOOL-003 | 取得工具（缺 id） | P1 | POST | `/llm/tool/get` | | |
| TOOL-004 | 取得工具（id 不存在） | P2 | POST | `/llm/tool/get` | | |
| TOOL-005 | 註冊工具（admin 正常） | P1 | POST | `/llm/tool/register` | | 需 `ADMIN_TOKEN`；測試後刪除 |
| TOOL-006 | 註冊工具（缺 name） | P1 | POST | `/llm/tool/register` | | |
| TOOL-007 | 註冊工具（缺 classPath） | P1 | POST | `/llm/tool/register` | | |
| TOOL-008 | 註冊工具（缺 type） | P1 | POST | `/llm/tool/register` | | |
| TOOL-009 | 刪除工具（admin 正常） | P1 | POST | `/llm/tool/delete` | | 需 `ADMIN_TOKEN` |
| TOOL-010 | 刪除工具（缺 id） | P1 | POST | `/llm/tool/delete` | | |
| TOOL-011 | 儲存工具設定（正常） | P1 | POST | `/llm/tool/saveSetting` | | ⚠️ 文件標公開但實際需要認證（帶 USER_TOKEN）；測試後刪除 |
| TOOL-012 | 儲存工具設定（缺 alias） | P2 | POST | `/llm/tool/saveSetting` | | |
| TOOL-013 | 更新工具設定（正常） | P1 | POST | `/llm/tool/updateSetting` | | ⚠️ `settingContent` 必須為 JSON 字串（非物件） |
| TOOL-014 | 更新工具設定（缺 settingContent） | P2 | POST | `/llm/tool/updateSetting` | | |
| TOOL-015 | 新增分類（admin 正常） | P1 | POST | `/llm/tool/category/save` | | 需 `ADMIN_TOKEN`；測試後刪除 |
| TOOL-016 | 新增分類（缺 group） | P2 | POST | `/llm/tool/category/save` | | |
| TOOL-017 | 更新分類（admin 正常） | P1 | POST | `/llm/tool/category/update` | | 需 `ADMIN_TOKEN` |
| TOOL-018 | 刪除分類（admin 正常） | P1 | POST | `/llm/tool/category/delete` | | 需 `ADMIN_TOKEN` |
| TOOL-019 | 業務邏輯（刪除後不出現清單） | P1 | 多步 | `/llm/tool/*` | | register → list → delete → list（確認已消失） |

#### TOOL curl 範本

```bash
# TOOL-001 列出工具（公開）
curl -s -X GET "http://localhost:80/llm/tool/list"
# 預期 HTTP 200 → {"code":200,"message":"success","data":[{"id":"3737ec4a-...","name":"date","type":"SYSTEM",...},{"id":"c14e82ca-...","name":"google_search",...}]}

# TOOL-002 取得特定工具
curl -s -X POST "http://localhost:80/llm/tool/get" \
  -H "Content-Type: application/json" \
  -d '{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"3737ec4a-...","name":"date","type":"SYSTEM",...}}

# TOOL-003 缺 id
curl -s -X POST "http://localhost:80/llm/tool/get" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 400

# TOOL-004 id 不存在
curl -s -X POST "http://localhost:80/llm/tool/get" \
  -H "Content-Type: application/json" \
  -d '{"id":"00000000-0000-0000-0000-000000000000"}'
# 預期 HTTP 400 → {"code":400,"message":"Tool not found","data":null}

# TOOL-005 admin 註冊工具（測試後刪除）
curl -s -X POST "http://localhost:80/llm/tool/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_tool","classPath":"tw.zipe.bastpartner.tool.DateTool","groupId":"ea8a08e2-342d-4ade-9579-127d2d1443c5","type":"SYSTEM"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"<NEW_TOOL_ID>",...}}

# TOOL-006 缺 name
curl -s -X POST "http://localhost:80/llm/tool/register" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"classPath":"tw.zipe.bastpartner.tool.DateTool","groupId":"ea8a08e2-...","type":"SYSTEM"}'
# 預期 HTTP 400

# TOOL-009 刪除工具（admin）
curl -s -X POST "http://localhost:80/llm/tool/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<TOOL_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}

# TOOL-011 儲存工具設定（⚠️ 需認證，雖文件標公開）
curl -s -X POST "http://localhost:80/llm/tool/saveSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f","alias":"my_date_tool"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"settingId":"<SETTING_ID>","alias":"my_date_tool"}}
# 記下 settingId，測試後用 updateSetting 或直接不再使用

# TOOL-012 saveSetting 缺 alias
curl -s -X POST "http://localhost:80/llm/tool/saveSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"3737ec4a-2c88-490e-8301-ebc611f2433f"}'
# 預期 HTTP 400

# TOOL-013 更新工具設定（⚠️ settingContent 必須為 JSON 字串，非物件）
curl -s -X POST "http://localhost:80/llm/tool/updateSetting" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"settingId":"<SETTING_ID>","settingContent":"{\"timezone\":\"Asia/Taipei\"}"}'
# 預期 HTTP 200

# TOOL-015 新增工具分類（admin，測試後刪除）
curl -s -X POST "http://localhost:80/llm/tool/category/save" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"group":"test_category"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"<CATEGORY_ID>","group":"test_category"}}

# TOOL-017 更新分類
curl -s -X POST "http://localhost:80/llm/tool/category/update" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"groupId":"<CATEGORY_ID>","group":"updated_category"}'
# 預期 HTTP 200

# TOOL-018 刪除分類
curl -s -X POST "http://localhost:80/llm/tool/category/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"groupId":"<CATEGORY_ID>"}'
# 預期 HTTP 200
```

---

