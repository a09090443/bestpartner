---
sidebar_position: 7
description: BestPartner 整合 SmallRye OpenAPI 提供互動式 Swagger UI，於 dev、docker 與 sit 環境開放瀏覽 API 並發送測試請求。
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
Swagger UI 與 OpenAPI spec 僅在 **dev**、**docker**、**sit** 環境開放；**uat** 與 **prod** 不對外提供。

未指定 profile 時視同 prod（安全預設），兩者皆回 **404**。
:::

| Profile | `/swagger-ui` | `/q/openapi` |
|---------|--------------|-------------|
| dev / docker / sit | 開放 | 開放 |
| uat / prod / 未指定 | 404 | 404 |

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
| `quarkus.swagger-ui.always-include` | `true` | **build-time**：一律將 Swagger UI 打包進 image |
| `quarkus.swagger-ui.enable` | `false` | **runtime**：全域預設關閉（安全預設） |
| `%dev` / `%docker` / `%sit`.`quarkus.swagger-ui.enable` | `true` | 這三個 profile 才對外開放 |
| `quarkus.smallrye-openapi.enable` | `false` | **runtime**：`/q/openapi` 同步管制，規則同上 |

### 為何分成 always-include 與 enable？

本專案採「一份 image 跑所有環境」，環境差異由 runtime 的 `QUARKUS_PROFILE` 決定。
但 `quarkus.swagger-ui.always-include` 是 **build-time** 設定——它由建置當下的 profile 凍結，
之後切 profile 改不動（實測：以 dev 建置的 image 用 `QUARKUS_PROFILE=prod` 執行，Swagger 仍可存取）。

因此改為：`always-include` 全域 `true`（一律打包），實際是否對外交由 **runtime** 的
`quarkus.swagger-ui.enable` 依 profile 決定。

:::warning
這代表 Swagger UI 的靜態資源**實際存在於 prod image 內**，只是被設定關閉，並非物理移除。
修改設定時務必確認 `enable` 的全域預設維持 `false`。
:::
