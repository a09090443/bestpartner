---
sidebar_position: 2
description: BestPartner LLM 設定 API，提供 AI 模型設定的 CRUD 與 AI 平台管理，端點前綴 /llm/setting，需帶入有效 JWT Token。
keywords: [LLM 設定, API, 模型設定, AI 平台, CRUD]
---

# LLM 設定 API

LLM 設定 API 提供 AI 模型設定的 CRUD 功能，以及 AI 平台的管理。所有端點路徑前綴為 `/llm/setting`，需帶入有效 JWT Token。

## 通用回應格式

```json
{
  "code": 200,
  "message": "",
  "data": { ... }
}
```

---

## 查詢 LLM 設定

```http
POST /llm/setting/get
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

`llmId` 與 `platformId` 皆為選填，可彈性組合查詢條件。

### 請求參數（LLMDTO）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `llmId` | string | | 指定查詢單一 LLM 設定的 ID |
| `platformId` | string | | 篩選指定平台下的所有 LLM 設定 |

### 查詢組合說明

| 傳入條件 | 結果 |
|---------|------|
| `llmId` | 回傳符合該 ID 的單筆設定（List 包含一筆）|
| `platformId` | 回傳該平台下所有 LLM 設定 |
| `llmId` + `platformId` | 回傳同時符合兩條件的設定 |
| 空 body `{}` | 回傳該使用者的所有 LLM 設定 |

### 請求範例

**以 llmId 查詢：**
```json
{
  "llmId": "9fc35d01-65e7-43af-9780-a591bd644bf0"
}
```

**以 platformId 查詢該平台所有設定：**
```json
{
  "platformId": "a1b2c3d4-0000-0000-0000-000000000001"
}
```

### 回應範例

```json
{
  "code": 200,
  "message": "",
  "data": [
    {
      "id": "9fc35d01-65e7-43af-9780-a591bd644bf0",
      "alias": "GPT-4o 主模型",
      "platformId": "a1b2c3d4-0000-0000-0000-000000000001",
      "modelType": "CHAT",
      "llmModel": { ... }
    }
  ]
}
```

---

## 新增 LLM 設定

```http
POST /llm/setting/save
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 請求參數（LLMDTO）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `platformId` | string | ✓ | 所屬平台 ID |
| `modelType` | string | ✓ | 模型類型：`CHAT`、`EMBEDDING`、`STREAMING` |
| `alias` | string | | 設定別名 |
| `llmModel.modelName` | string | ✓ | 模型名稱 |

### 請求範例

```json
{
  "platformId": "a1b2c3d4-0000-0000-0000-000000000001",
  "modelType": "CHAT",
  "alias": "GPT-4o 主模型",
  "llmModel": {
    "modelName": "gpt-4o"
  }
}
```

---

## 更新 LLM 設定

```http
POST /llm/setting/update
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 請求參數（LLMDTO）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `id` | string | ✓ | 要更新的設定 ID |
| `platformId` | string | ✓ | 所屬平台 ID |
| `modelType` | string | ✓ | 模型類型 |
| `llmModel` | object | ✓ | 模型設定 |

---

## 刪除 LLM 設定

```http
POST /llm/setting/delete
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### 請求參數（LLMDTO）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `id` | string | ✓ | 要刪除的設定 ID |

---

## 新增平台（管理員）

```http
POST /llm/setting/platform/add
Authorization: Bearer YOUR_JWT_TOKEN（需 admin 角色）
Content-Type: application/json
```

### 請求參數（PlatformDTO）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `platform` | string | ✓ | 平台名稱（對應 `Platform` enum）|

---

## 刪除平台（管理員）

```http
POST /llm/setting/platform/delete
Authorization: Bearer YOUR_JWT_TOKEN（需 admin 角色）
Content-Type: application/json
```

### 請求參數（PlatformDTO）

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `id` | string | ✓ | 要刪除的平台 ID |

---

## 端點一覽

| 端點 | 說明 | 權限 |
|------|------|------|
| `POST /llm/setting/get` | 查詢 LLM 設定（支援 llmId / platformId 條件） | 一般使用者 |
| `POST /llm/setting/save` | 新增 LLM 設定 | 一般使用者 |
| `POST /llm/setting/update` | 更新 LLM 設定 | 一般使用者 |
| `POST /llm/setting/delete` | 刪除 LLM 設定 | 一般使用者 |
| `POST /llm/setting/platform/add` | 新增平台 | admin |
| `POST /llm/setting/platform/delete` | 刪除平台 | admin |
