# API 文件（Swagger UI）

## 存取限制

僅在 **dev / docker / sit** 環境開放；**uat / prod 不對外提供**。
未指定 profile 時視同 prod（安全預設），一律關閉。

| Profile | `/swagger-ui` | `/q/openapi` |
|---------|--------------|-------------|
| dev / docker / sit | 200（302 導向） | 200 |
| uat / prod / 未指定 | 404 | 404 |

## 路徑

| 資源 | 路徑 |
|-----|------|
| Swagger UI | `/swagger-ui` |
| OpenAPI spec | `/q/openapi` |

## 設定說明

- 依賴：`quarkus-smallrye-openapi`（版本由 Quarkus BOM 管理）
- JWT Bearer 認證已整合，可在 Authorize 按鈕填入 token 測試受保護端點
- `auto-add-security=true`：`@RolesAllowed` 標註的端點會自動加上 security 需求

## ⚠️ 管制機制（build-time vs runtime）

存取限制**不是**靠建置時排除，而是靠 runtime 設定：

| 設定鍵 | 性質 | 值 |
|-------|------|----|
| `quarkus.swagger-ui.always-include` | build-time | 全域 `true`（一律打包進 image） |
| `quarkus.swagger-ui.enable` | **runtime** | 全域 `false`，`%dev` / `%docker` / `%sit` 才 `true` |
| `quarkus.smallrye-openapi.enable` | **runtime** | 同上 |

原因：本專案採「一份 image 跑所有環境」，而 `always-include` 是 build-time 設定、切 profile 改不動
（實測：dev 建置的 image 以 `QUARKUS_PROFILE=prod` 執行，Swagger 仍回 302）。
因此 Swagger UI 一律打包進 image，實際是否對外由 runtime 的 `enable` 依 profile 決定。

> ⚠️ **這代表 Swagger UI 的靜態資源實際存在於 prod image 內，只是被設定關閉**，並非物理移除。
> 修改此處設定時務必確認 `enable` 的全域預設維持 `false`。

詳見 [`configuration-and-profiles.md`](configuration-and-profiles.md)
