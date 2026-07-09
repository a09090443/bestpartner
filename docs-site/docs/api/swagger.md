---
sidebar_position: 7
description: BestPartner 整合 SmallRye OpenAPI 提供互動式 Swagger UI，於 dev 與 sit 環境開放瀏覽 API 並發送測試請求。
keywords: [Swagger, OpenAPI, API 文件, SmallRye, 互動測試]
---

# Swagger UI

BestPartner 整合了 SmallRye OpenAPI（`quarkus-smallrye-openapi`），在開發與測試環境提供互動式 Swagger UI，可直接在瀏覽器中瀏覽 API 文件並發送請求。

## 存取位址

| 路徑 | 說明 |
|------|------|
| `/swagger-ui` | Swagger UI 互動介面 |
| `/q/openapi` | OpenAPI 規格（YAML 格式） |

:::info 環境限制
Swagger UI 僅在 **dev** 與 **sit** 環境開放。生產環境（prod）不對外提供 Swagger UI。
:::

## 使用 JWT 認證

Swagger UI 已整合 JWT Bearer 認證方案，可直接在介面中授權後測試受保護的 API。

### 步驟一：取得 JWT Token

先呼叫登入 API 取得 Token：

```http
POST /login/
Content-Type: application/json

{
  "email": "admin",
  "password": "admin"
}
```

回應中的 `data` 欄位即為 JWT Token：

```json
{
  "code": 200,
  "message": "",
  "data": "eyJhbGciOiJSUzI1NiJ9..."
}
```

### 步驟二：在 Swagger UI 中授權

1. 開啟 `http://localhost:80/swagger-ui`
2. 點擊右上角的 **Authorize** 按鈕
3. 在 `bearerAuth` 欄位輸入 Token（不需加 `Bearer` 前綴）
4. 點擊 **Authorize** 完成設定

完成後，所有後續請求都會自動帶入 `Authorization: Bearer <token>` Header。

## 相關設定

Swagger 功能透過以下設定管理，位於 `application.properties`：

| 設定項 | 值 | 說明 |
|--------|----|------|
| `quarkus.smallrye-openapi.info-title` | `BestPartner API` | API 文件標題 |
| `quarkus.smallrye-openapi.info-version` | `0.1.7` | API 版本 |
| `quarkus.smallrye-openapi.security-scheme` | `jwt` | 安全認證方案類型 |
| `quarkus.smallrye-openapi.security-scheme-name` | `bearerAuth` | 安全方案名稱 |
| `quarkus.smallrye-openapi.auto-add-security` | `true` | 自動為受保護端點加上安全需求 |
| `%dev.quarkus.swagger-ui.always-include` | `true` | dev 環境啟用 Swagger UI |
| `%sit.quarkus.swagger-ui.always-include` | `true` | sit 環境啟用 Swagger UI |
