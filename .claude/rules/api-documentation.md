# API 文件（Swagger UI）

## 存取限制

僅在 **dev / sit** 環境開放；**prod** 不對外提供。

## 路徑

| 資源 | 路徑 |
|-----|------|
| Swagger UI | `/swagger-ui` |
| OpenAPI spec | `/q/openapi` |

## 設定說明

- 依賴：`quarkus-smallrye-openapi`（版本由 Quarkus BOM 管理）
- JWT Bearer 認證已整合，可在 Authorize 按鈕填入 token 測試受保護端點
- `auto-add-security=true`：`@RolesAllowed` 標註的端點會自動加上 security 需求
