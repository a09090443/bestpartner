---
sidebar_position: 1
---

# 認證 API

BestPartner 使用 JWT（JSON Web Token）進行身份驗證。所有 API 請求（除登入外）都需要在 Header 中帶入有效的 JWT Token。

## 登入取得 Token

```http
POST /login
Content-Type: application/json
```

### 請求參數

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `username` | string | ✓ | 使用者帳號 |
| `password` | string | ✓ | 使用者密碼 |

### 請求範例

```json
{
  "username": "admin",
  "password": "admin"
}
```

### 回應範例

```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJi..."
}
```

### 回應欄位

| 欄位 | 類型 | 說明 |
|------|------|------|
| `token` | string | JWT Token，有效期預設 24 小時 |

## 使用 Token

在後續所有 API 請求的 Header 中帶入 Token：

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

## 預設帳號

| 帳號 | 密碼 | 角色 |
|------|------|------|
| `admin` | `admin` | 管理員（Admin） |

:::caution 生產環境安全提醒
在生產環境中，請務必更改預設密碼並設定適當的 JWT 密鑰。
:::

## RBAC 權限說明

BestPartner 實作基於角色的存取控制（RBAC）：

| 角色 | 說明 | 可執行操作 |
|------|------|-----------|
| `Admin` | 管理員 | 所有操作，包含系統設定 |
| `User` | 一般使用者 | 對話、查詢知識庫 |

## Token 錯誤處理

| HTTP 狀態碼 | 說明 |
|------------|------|
| `401 Unauthorized` | Token 無效或已過期，需重新登入 |
| `403 Forbidden` | 無操作權限 |
