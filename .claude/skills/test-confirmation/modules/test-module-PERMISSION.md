### PERMISSION — 權限模組（`/llm/permission`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| PERM-001 | 新增權限（admin 正常） | P1 | POST | `/llm/permission/add` | | 需 `ADMIN_TOKEN`；測試後刪除 |
| PERM-002 | 新增權限（未認證） | P1 | POST | `/llm/permission/add` | | 預期 401 |
| PERM-003 | 新增權限（缺 name） | P1 | POST | `/llm/permission/add` | | |
| PERM-004 | 新增權限（缺 num） | P1 | POST | `/llm/permission/add` | | |
| PERM-005 | 新增權限（非 admin 拒絕） | P2 | POST | `/llm/permission/add` | | 預期 403 |
| PERM-006 | 更新權限（admin 正常） | P1 | POST | `/llm/permission/update` | | 需 `ADMIN_TOKEN` |
| PERM-007 | 更新權限（缺 id） | P1 | POST | `/llm/permission/update` | | |
| PERM-008 | 更新權限（非 admin 拒絕） | P2 | POST | `/llm/permission/update` | | 預期 403 |
| PERM-009 | 刪除權限（admin 正常） | P1 | DELETE | `/llm/permission/delete` | | 需 `ADMIN_TOKEN` |
| PERM-010 | 刪除權限（缺 id） | P1 | DELETE | `/llm/permission/delete` | | |
| PERM-011 | 刪除權限（未認證） | P1 | DELETE | `/llm/permission/delete` | | 預期 401 |

#### PERMISSION curl 範本

```bash
# PERM-001 admin 新增權限（測試後刪除）
curl -s -X POST "http://localhost:80/llm/permission/add" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_permission","num":9999}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"<PERM_ID>","name":"test_permission","num":9999,"description":null}}
# 記下 id，測試後用 PERM-009 刪除

# PERM-002 未認證
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/permission/add" \
  -H "Content-Type: application/json" -d '{"name":"test","num":1}'
# 預期 HTTP 401

# PERM-003 缺 name
curl -s -X POST "http://localhost:80/llm/permission/add" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"num":100}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 name 不可為空值","data":null}

# PERM-004 缺 num（測試系統行為）
curl -s -X POST "http://localhost:80/llm/permission/add" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test_perm"}'
# 預期 HTTP 400 或 200（num 為 nullable 時）

# PERM-005 非 admin 新增
curl -s -X POST "http://localhost:80/llm/permission/add" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"hack","num":1}'
# 預期 HTTP 403 → {"code":403,"message":"權限錯誤","data":null}

# PERM-006 更新權限（admin）
curl -s -X POST "http://localhost:80/llm/permission/update" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<PERM_ID>","name":"test_permission_updated","num":8888}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"<PERM_ID>","name":"test_permission_updated","num":8888}}

# PERM-007 更新缺 id
curl -s -X POST "http://localhost:80/llm/permission/update" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test","num":100}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 id 不可為空值","data":null}

# PERM-008 非 admin 更新
curl -s -X POST "http://localhost:80/llm/permission/update" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<PERM_ID>","name":"hack","num":1}'
# 預期 HTTP 403

# PERM-009 刪除權限（admin，清理測試資料）
curl -s -X DELETE "http://localhost:80/llm/permission/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<PERM_ID>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}

# PERM-010 刪除缺 id
curl -s -X DELETE "http://localhost:80/llm/permission/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 400

# PERM-011 未認證刪除
curl -s -o /dev/null -w "%{http_code}" -X DELETE "http://localhost:80/llm/permission/delete" \
  -H "Content-Type: application/json" -d '{"id":"xxx"}'
# 預期 HTTP 401
```

---

