### AUTH — 認證模組（`/login`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| AUTH-001 | 正常登入（有效帳密） | P0 | POST | `/login/` | | |
| AUTH-002 | 驗證 JWT 有效 | P0 | POST | `/login/check` | | 需 `user-read` 角色；test_user 預期 403 |
| AUTH-003 | 錯誤登入（密碼錯誤） | P1 | POST | `/login/` | | |
| AUTH-004 | 錯誤登入（信箱不存在） | P1 | POST | `/login/` | | |
| AUTH-005 | 必填驗證（缺 email） | P1 | POST | `/login/` | | |
| AUTH-006 | 必填驗證（缺 password） | P1 | POST | `/login/` | | |
| AUTH-007 | 必填驗證（空 body） | P1 | POST | `/login/` | | |
| AUTH-008 | email 格式不合法 | P2 | POST | `/login/` | | |
| AUTH-009 | JWT check（無效 token） | P1 | POST | `/login/check` | | |
| AUTH-010 | 邊界條件（超長 password） | P2 | POST | `/login/` | | |

#### AUTH curl 範本

```bash
# AUTH-001 正常登入（admin）
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw","password":"admin"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":"eyJ0eXAi...（JWT token）"}

# AUTH-001b 正常登入（user）
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@bestpartner.com.tw","password":"user"}'
# 預期 HTTP 200

# AUTH-002 驗證 JWT
curl -s -X POST "http://localhost:80/login/check" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":"check"}

# AUTH-002b test_user 呼叫 check（缺 user-read 角色）
curl -s -X POST "http://localhost:80/login/check" \
  -H "Authorization: Bearer $TEST_USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 403 → {"code":403,"message":"權限錯誤","data":null}

# AUTH-003 密碼錯誤
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw","password":"wrongpassword"}'
# 預期 HTTP 401 → {"code":401,"message":"密碼錯誤","data":null}

# AUTH-004 信箱不存在
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"notexist@example.com","password":"anypassword"}'
# 預期 HTTP 401 → {"code":401,"message":"帳號不存在","data":null}

# AUTH-005 缺 email
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"password":"admin"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 email 不可為空值","data":null}

# AUTH-006 缺 password
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bestpartner.com.tw"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 password 不可為空值","data":null}

# AUTH-007 空 body
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{}'
# 預期 HTTP 400

# AUTH-008 email 格式錯誤
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d '{"email":"not-an-email","password":"admin"}'
# 預期 HTTP 400 or 401

# AUTH-009 無效 token 呼叫 check
curl -s -X POST "http://localhost:80/login/check" \
  -H "Authorization: Bearer invalidtoken" \
  -H "Content-Type: application/json" -d '{}'
# 預期 HTTP 401

# AUTH-010 超長 password
curl -s -X POST "http://localhost:80/login/" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@bestpartner.com.tw\",\"password\":\"$(python3 -c 'print("x"*1000)')\"}"
# 預期 HTTP 400 或 401（系統行為）
```

---

