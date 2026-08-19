---
sidebar_position: 1
description: BestPartner 認證 API 說明，介紹以 JWT 進行身份驗證的登入端點、Token 取得方式與請求 Header 帶入規則。
keywords: [認證 API, JWT, 登入, Token, 身份驗證, RBAC]
---

# 認證 API

BestPartner 使用 JWT（JSON Web Token）進行身份驗證。所有 API 請求（除登入外）都需要在 Header 中帶入有效的 JWT Token。

## 登入取得 Token

```http
POST /login/
Content-Type: application/json
```

### 請求參數

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `email` | string | ✓ | 使用者電子郵件 |
| `password` | string | ✓ | 使用者密碼 |

### 請求範例

```json
{
  "email": "admin",
  "password": "admin"
}
```

### 回應範例

```json
{
  "code": 200,
  "message": "",
  "data": "eyJhbGciOiJSUzI1NiJ9.eyJ..."
}
```

### 回應欄位

| 欄位 | 類型 | 說明 |
|------|------|------|
| `code` | int | 狀態碼，200 表示成功 |
| `message` | string | 訊息說明 |
| `data` | string | JWT Token |

## 使用 Token

在後續所有 API 請求的 Header 中帶入 Token：

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

## Token 內容

JWT Payload 包含以下關鍵欄位：

| 欄位 | 說明 |
|------|------|
| `upn` | 使用者識別（User Principal Name）|
| `groups` | 使用者擁有的權限列表 |
| `iss` | Token 發行者（`bast-partner`）|

## 預設帳號

初始化 SQL 內含以下預設帳號：

| email | 密碼 | 角色 |
|-------|------|------|
| `admin` | `admin` | 管理員 |
| `test_user` | `test_user` | 一般使用者 |

:::caution 生產環境安全提醒
請務必在正式上線前更改預設密碼。
:::

## 權限（Permission）

系統定義以下四種權限：

| 權限 | 說明 |
|------|------|
| `VIEW_ADMIN_DETAILS` | 查看管理員詳細資訊 |
| `VIEW_USER_DETAILS` | 查看使用者詳細資訊 |
| `SEND_MESSAGE` | 發送訊息（對話）|
| `CREATE_USER` | 建立使用者 |

## Token 刷新

JwtFilter 支援自動 Token 刷新。若設定 `jwt.refresh.switch=true`，接近過期的 Token 會自動刷新，新 Token 會附在回應的 Header 中回傳。

## 錯誤回應

| HTTP 狀態碼 | 說明 |
|------------|------|
| `401 Unauthorized` | Token 無效或已過期；**登入端點 `POST /login/` 帳密錯誤時亦回此碼**，body 帶原因（如 `{"code":401,"message":"密碼錯誤"}`） |
| `403 Forbidden` | 無操作權限 |

:::note 前端如何區分這兩種 401
`401` 同時代表「既有登入態失效」與「這次登入的帳密不對」，兩者處置不同：前者要清 token 並導回登入頁，
後者只需在登入頁顯示原因。前端因此在 axios response 攔截器排除登入端點自身的 401，
不對它觸發登出重導——否則整頁重載會把剛顯示的錯誤訊息一起沖掉。
詳見 [前端架構](../architecture/frontend.md#核心設計慣例)。
:::
