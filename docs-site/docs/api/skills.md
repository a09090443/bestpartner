---
sidebar_position: 5
---

# Skill API

Skill API 提供個人與全域 Skill 的管理功能，包含上傳、查詢與刪除。所有端點路徑前綴為 `/llm/skill`，皆需登入（`@Authenticated`）。

## Skill 格式說明

Skill 以 `.zip` 壓縮檔形式上傳，解壓後儲存於伺服器用戶專屬目錄：

```
{upload_dir}/skills/{userId}/{skillName}/
```

| 元素 | 說明 |
|------|------|
| `skillName` | zip 檔名（去掉 `.zip`）|
| `skill.md` | 必要主內容檔案 |
| 其他檔案 | 作為 resources |

## 通用回應格式

```json
{
  "code": 200,
  "message": "",
  "data": { ... }
}
```

---

## 列出 Skill

```http
GET /llm/skill/list
Authorization: Bearer YOUR_JWT_TOKEN
```

回傳當前使用者的所有 Skill，包含系統 Global Skill（以 `isGlobal` 欄位區分）。

### 回應範例

```json
{
  "code": 200,
  "message": "",
  "data": [
    {
      "id": "skill-uuid-1",
      "name": "web-search",
      "description": "搜尋網頁並摘要結果",
      "isGlobal": false
    },
    {
      "id": "skill-uuid-2",
      "name": "code-review",
      "description": "程式碼審查助理",
      "isGlobal": true
    }
  ]
}
```

---

## 取得 Skill 詳情

```http
POST /llm/skill/get
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

取得特定 Skill 的詳細資訊，包含 `skill.md` 內容及所有 resources。可讀取自己的 Skill 或 Global Skill。

### 請求參數（SkillDTO）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `id` | string | ✓ | Skill ID |

### 請求範例

```json
{
  "id": "skill-uuid-1"
}
```

### 回應範例

```json
{
  "code": 200,
  "message": "",
  "data": {
    "id": "skill-uuid-1",
    "name": "web-search",
    "description": "搜尋網頁並摘要結果",
    "content": "# Web Search Skill\n...",
    "dirPath": "/upload/skills/user-id/web-search",
    "isGlobal": false,
    "resources": [
      {
        "id": "resource-uuid-1",
        "skillId": "skill-uuid-1",
        "relativePath": "config.json",
        "content": "{ ... }"
      }
    ]
  }
}
```

---

## 上傳個人 Skill

```http
POST /llm/skill/upload
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: multipart/form-data
```

上傳個人 Skill zip 壓縮檔。若已存在同名 Skill，自動覆蓋。

### 表單參數（SkillUploadForm）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `file` | file | ✓ | Skill zip 壓縮檔 |
| `description` | string | | Skill 描述 |

### 請求範例（curl）

```bash
curl -X POST http://localhost/llm/skill/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@web-search.zip" \
  -F "description=搜尋網頁並摘要結果"
```

### 回應範例

```json
{
  "code": 200,
  "message": "",
  "data": {
    "id": "skill-uuid-1",
    "name": "web-search",
    "description": "搜尋網頁並摘要結果",
    "isGlobal": false
  }
}
```

---

## 刪除個人 Skill

```http
POST /llm/skill/delete
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

刪除個人 Skill，同時移除伺服器上的目錄。只能刪除自己的 Skill。

### 請求參數（SkillDTO）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `id` | string | ✓ | Skill ID |

### 請求範例

```json
{
  "id": "skill-uuid-1"
}
```

### 回應範例

```json
{
  "code": 200,
  "message": "",
  "data": "Data deleted successfully"
}
```

---

## 上傳 Global Skill（管理員）

```http
POST /llm/skill/global/upload
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: multipart/form-data
```

> 需要 `admin` 角色

上傳全域 Skill，所有使用者皆可讀取。表單參數與個人 Skill 上傳相同。

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `file` | file | ✓ | Skill zip 壓縮檔 |
| `description` | string | | Skill 描述 |

---

## 刪除 Global Skill（管理員）

```http
POST /llm/skill/global/delete
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

> 需要 `admin` 角色

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `id` | string | ✓ | Global Skill ID |

---

## 端點一覽

| HTTP | 端點 | 說明 | 權限 |
|------|------|------|------|
| GET | `/llm/skill/list` | 列出所有 Skill（含 global） | 登入 |
| POST | `/llm/skill/get` | 取得 Skill 詳情 | 登入 |
| POST | `/llm/skill/upload` | 上傳個人 Skill | 登入 |
| POST | `/llm/skill/delete` | 刪除個人 Skill | 登入 |
| POST | `/llm/skill/global/upload` | 上傳 Global Skill | admin |
| POST | `/llm/skill/global/delete` | 刪除 Global Skill | admin |
