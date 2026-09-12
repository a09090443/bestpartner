### SYSTEM SETTING — 系統設定模組（`/systemSetting`）

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| SYSSET-001 | 列出設定（公開） | P0 | GET | `/systemSetting/list` | | 無需認證 |
| SYSSET-002 | 取得設定（正常） | P1 | POST | `/systemSetting/get` | | 無需認證 |
| SYSSET-003 | 取得設定（key 不存在） | P2 | POST | `/systemSetting/get` | | 預期返回空字串 |
| SYSSET-004 | 新增設定（正常） | P1 | POST | `/systemSetting/add` | | 無需認證；測試後刪除 |
| SYSSET-005 | 新增設定（缺 value） | P1 | POST | `/systemSetting/add` | | 預期 400 |
| SYSSET-006 | 更新設定（正常） | P1 | POST | `/systemSetting/update` | | 無需認證 |
| SYSSET-007 | 更新設定（缺 id） | P1 | POST | `/systemSetting/update` | | ⚠️ 已知缺陷：預期 400，實際 500 |
| SYSSET-008 | 刪除設定（正常） | P1 | DELETE | `/systemSetting/delete` | | 無需認證；`delete` 需帶 `key` 欄位 |
| SYSSET-009 | 刪除設定（id 不存在） | P2 | DELETE | `/systemSetting/delete` | | 預期返回 0 |
| SYSSET-010 | 公開端點確認（list 無需認證） | P1 | GET | `/systemSetting/list` | | |
| SYSSET-011 | 公開端點確認（get 無需認證） | P1 | POST | `/systemSetting/get` | | |
| SYSSET-012 | 公開端點確認（add 無需認證） | P1 | POST | `/systemSetting/add` | | 安全性注意：任何人可新增 |
| SYSSET-013 | 業務邏輯（新增 → 更新 → 刪除） | P1 | 多步 | `/systemSetting/*` | | 驗證整個生命週期 |

#### SYSTEM SETTING curl 範本

```bash
# SYSSET-001 列出所有設定（公開，無需認證）
curl -s -X GET "http://localhost:80/systemSetting/list"
# 預期 HTTP 200 → {"code":200,"message":"success","data":[{"id":1,"key":"default_llm_platform","value":"OPENROUTER",...},{"id":2,"key":"skill.global.dir","value":"D:/tmp/...",...}]}

# SYSSET-002 依 key 取得設定
curl -s -X POST "http://localhost:80/systemSetting/get" \
  -H "Content-Type: application/json" \
  -d '{"key":"default_llm_platform"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":"OPENROUTER"}

# SYSSET-003 key 不存在
curl -s -X POST "http://localhost:80/systemSetting/get" \
  -H "Content-Type: application/json" \
  -d '{"key":"nonexistent_key"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":""}（空字串）

# SYSSET-004 新增設定（公開，測試後刪除）
curl -s -X POST "http://localhost:80/systemSetting/add" \
  -H "Content-Type: application/json" \
  -d '{"key":"test_setting","value":"test_value","description":"Test setting"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":null,"key":"test_setting","value":"test_value",...}}
# 注意：response 中 id 為 null，需用 list 查詢取得 id

# 取得新增設定的 id
curl -s -X GET "http://localhost:80/systemSetting/list" | python3 -c "
import sys,json
data=json.load(sys.stdin)['data']
[print(f'id={item[\"id\"]}') for item in data if item.get('key')=='test_setting']
"

# SYSSET-005 缺 value
curl -s -X POST "http://localhost:80/systemSetting/add" \
  -H "Content-Type: application/json" \
  -d '{"key":"incomplete_setting"}'
# 預期 HTTP 400 → {"code":400,"message":"欄位 value 不可為空值","data":null}

# SYSSET-006 更新設定（需帶 id + key）
curl -s -X POST "http://localhost:80/systemSetting/update" \
  -H "Content-Type: application/json" \
  -d '{"id":<SETTING_ID>,"key":"test_setting","value":"updated_value","description":"Updated"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":{"id":<SETTING_ID>,"key":"test_setting","value":"updated_value",...}}

# SYSSET-007 更新缺 id（⚠️ 已知缺陷：預期 400，實際 500）
curl -s -X POST "http://localhost:80/systemSetting/update" \
  -H "Content-Type: application/json" \
  -d '{"key":"test_setting","value":"some_value"}'
# 預期 HTTP 400（缺陷：實際返回 500）

# SYSSET-008 刪除設定（⚠️ DELETE 端點需帶 key 欄位，否則反序列化失敗）
curl -s -X DELETE "http://localhost:80/systemSetting/delete" \
  -H "Content-Type: application/json" \
  -d '{"id":<SETTING_ID>,"key":"test_setting"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":1}

# SYSSET-009 刪除不存在的 id（需帶 key）
curl -s -X DELETE "http://localhost:80/systemSetting/delete" \
  -H "Content-Type: application/json" \
  -d '{"id":99999,"key":"fake_key"}'
# 預期 HTTP 200 → {"code":200,"message":"success","data":0}（0 rows affected）
```

---

