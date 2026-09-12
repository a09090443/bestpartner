### USER — 用戶模組（`/llm/user`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| USER-001 | 註冊（正常新帳號） | P0 | POST | `/llm/user/register` | | 測試後呼叫 DELETE 清理 |
| USER-002 | 註冊（重複 email） | P1 | POST | `/llm/user/register` | | |
| USER-003 | 註冊（缺 username） | P1 | POST | `/llm/user/register` | | |
| USER-004 | 註冊（缺 password） | P1 | POST | `/llm/user/register` | | |
| USER-005 | 註冊（缺 email） | P1 | POST | `/llm/user/register` | | |
| USER-006 | 註冊（非法 email） | P2 | POST | `/llm/user/register` | | |
| USER-007 | 取得用戶資訊（user token，正常） | P0 | POST | `/llm/user/get` | | 使用 `USER_TOKEN`；test_user token 會返回 400 |
| USER-008 | 取得用戶資訊（未認證） | P0 | POST | `/llm/user/get` | | 預期 401 |
| USER-009 | 更新用戶（正常） | P1 | POST | `/llm/user/update` | | |
| USER-010 | 更新用戶（缺 id） | P1 | POST | `/llm/user/update` | | |
| USER-011 | 更新用戶（id 不存在） | P1 | POST | `/llm/user/update` | | |
| USER-012 | 切換狀態（admin 正常） | P1 | POST | `/llm/user/switchStatus` | | 需 `ADMIN_TOKEN`（`@RolesAllowed("admin")`） |
| USER-013 | 切換狀態（缺 status） | P1 | POST | `/llm/user/switchStatus` | | |
| USER-014 | 刪除用戶（admin 正常） | P1 | DELETE | `/llm/user/delete` | | 需 `ADMIN_TOKEN`（`@RolesAllowed("admin")`） |
| USER-015 | 刪除用戶（缺 id） | P1 | DELETE | `/llm/user/delete` | | |
| USER-016 | 刪除用戶（id 不存在） | P2 | DELETE | `/llm/user/delete` | | |
| USER-017 | 業務邏輯（刪除後無法取得） | P1 | 多步 | `/llm/user/*` | | register → login → get → update → delete |

#### USER curl 範本

```bash
# USER-001 正常註冊（使用時間戳避免 email 衝突）
TS=$(date +%s)
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test_reg_$TS\",\"password\":\"testpass\",\"email\":\"test_reg_$TS@test.com\"}"
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"...","username":"test_reg_...","status":"ACTIVE"}}
# 記下 id，測試後用 USER-014 刪除

# USER-002 重複 email（先執行 USER-001 後再執行）
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"another_user","password":"testpass","email":"test_reg_<同上TS>@test.com"}'
# 預期 HTTP 400 → {"code":400,"message":"Database processing error","data":null}

# USER-003 缺 username
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d '{"password":"testpass","email":"missing_username@test.com"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 username 不可為空值","data":null}

# USER-004 缺 password
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"nopw","email":"nopw@test.com"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 password 不可為空值","data":null}

# USER-005 缺 email
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"noemail","password":"testpass"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 email 不可為空值","data":null}

# USER-006 非法 email
curl -s -X POST "http://localhost:80/llm/user/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"bademail","password":"testpass","email":"not-an-email"}'
# 預期 HTTP 400

# USER-007 取得用戶資訊（使用 USER_TOKEN）
curl -s -X POST "http://localhost:80/llm/user/get" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":"...","email":"user@bestpartner.com.tw","role":{"roleName":"USER"},...}}

# USER-008 取得資訊（未認證）
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/user/get" \
  -H "Content-Type: application/json" -d '{}'
# 預期 HTTP 401

# USER-009 更新用戶（先用 USER-007 取得 id）
curl -s -X POST "http://localhost:80/llm/user/update" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<USER_ID>","email":"user@bestpartner.com.tw","status":"ACTIVE"}'
# 預期 HTTP 200

# USER-010 更新缺 id
curl -s -X POST "http://localhost:80/llm/user/update" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@bestpartner.com.tw","status":"ACTIVE"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 id 不可為空值","data":null}

# USER-012 切換狀態（admin）
curl -s -X POST "http://localhost:80/llm/user/switchStatus" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<TARGET_USER_ID>","status":"INACTIVE"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{...}}
# 注意：測試後記得切回 ACTIVE

# USER-013 切換狀態缺 status
curl -s -X POST "http://localhost:80/llm/user/switchStatus" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<TARGET_USER_ID>"}'
# 預期 HTTP 400

# USER-014 刪除用戶（admin）
curl -s -X DELETE "http://localhost:80/llm/user/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"<USER_ID_TO_DELETE>"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":true}

# USER-015 刪除缺 id
curl -s -X DELETE "http://localhost:80/llm/user/delete" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 400
```

---

