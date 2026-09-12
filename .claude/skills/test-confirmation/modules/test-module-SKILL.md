### SKILL — Skill 模組（`/llm/skill`）

> 類別層級 `@Authenticated`。Skill 以 `.zip` 上傳，解壓後存於 `{upload_dir}/skills/{userId}/{skillName}/`；
> zip 檔名（去 `.zip`）即 skillName，`skill.md` 為必要主內容，其餘檔案為 resources。

> ⚠️ multipart 欄位名為 **`file`**（與 `/llm/vector/uploadFiles`、`/llm/chat/uploadFile` 一致）。

| 測試 ID | 測試場景 | 優先級 | 方法 | 端點 | 狀態 | 備註 |
|---------|---------|--------|------|------|------|------|
| SKILL-001 | 列出 Skill（已認證） | P0 | GET | `/llm/skill/list` | | 含 global skills，以 `isGlobal` 區分 |
| SKILL-002 | 列出 Skill（未認證） | P0 | GET | `/llm/skill/list` | | 預期 401 |
| SKILL-003 | 上傳個人 Skill | P1 | POST | `/llm/skill/upload` | | multipart；`file` 必填、`description` 選填；同名自動覆蓋 |
| SKILL-004 | 取得 Skill（含 resources） | P1 | POST | `/llm/skill/get` | | `content` 為 skill.md 全文；`resources[]` 含 relativePath 與 content |
| SKILL-005 | **擁有權**：讀他人的 Skill | P1 | POST | `/llm/skill/get` | | 預期 400 `Skill not found`（不洩漏存在性） |
| SKILL-006 | **擁有權**：刪他人的 Skill | P1 | POST | `/llm/skill/delete` | | 預期 400 `Skill not found` |
| SKILL-007 | 刪除個人 Skill（擁有者） | P1 | POST | `/llm/skill/delete` | | 同時刪除伺服器目錄；清單確認消失 |
| SKILL-008 | Global 上傳（非 admin 拒絕） | P1 | POST | `/llm/skill/global/upload` | | 預期 403 |
| SKILL-009 | Global 上傳（admin） | P2 | POST | `/llm/skill/global/upload` | | 所有使用者可讀；測試後以 global/delete 清理 |
| SKILL-010 | Global 刪除（admin） | P2 | POST | `/llm/skill/global/delete` | | |

#### SKILL curl 範本

```bash
# 準備測試 zip（Windows）
mkdir -p apitest-skill && printf '# 測試 Skill\n\n內容。\n' > apitest-skill/skill.md \
  && printf 'reference content\n' > apitest-skill/reference.txt
powershell -c "Compress-Archive -Path 'apitest-skill\*' -DestinationPath 'apitest-skill.zip' -Force"

# SKILL-001 / 002
curl -s "http://localhost:80/llm/skill/list" -H "Authorization: Bearer $ADMIN_TOKEN"
curl -s -o /dev/null -w "%{http_code}" "http://localhost:80/llm/skill/list"

# SKILL-003 上傳（⚠️ 欄位名 file）
curl -s -X POST "http://localhost:80/llm/skill/upload" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "file=@apitest-skill.zip" -F "description=API 回歸測試 skill"

# SKILL-004 / 005
curl -s -X POST "http://localhost:80/llm/skill/get" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" -d '{"id":"<SKILL_ID>"}'
curl -s -X POST "http://localhost:80/llm/skill/get" -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" -d '{"id":"<ADMIN_SKILL_ID>"}'   # → 400 Skill not found

# SKILL-007 / 008
curl -s -X POST "http://localhost:80/llm/skill/delete" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" -d '{"id":"<SKILL_ID>"}'
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:80/llm/skill/global/upload" \
  -H "Authorization: Bearer $USER_TOKEN" -F "file=@apitest-skill.zip"   # → 403
```
