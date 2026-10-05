# 命名慣例

## 套件命名（重要）

主要程式碼位於 `tw.zipe.bastpartner`

⚠️ 注意：是 `bastpartner`，**不是** `basepartner`（少了 'e'）

Gradle 專案 group 是 `tw.zipe.bestpartner`，刻意與 Kotlin 套件名 `tw.zipe.bastpartner` 不同，兩者不需互相對齊。新增程式碼的 package 與 import 仍一律使用 `tw.zipe.bastpartner`。

## 各層命名規則

| Package | 命名格式 | 範例 | CDI 標註 |
|---------|---------|------|---------|
| `resource` | `XxxResource.kt` | `LLMResource.kt` | `@Path` |
| `service` | `XxxService.kt` | `LLMService.kt` | `@ApplicationScoped` |
| `entity` | `XxxEntity.kt` | `LLMSettingEntity.kt` | `@Entity`，繼承 `BaseEntity` |
| `repository` | `XxxRepository.kt` | `LLMUserRepository.kt` | `@ApplicationScoped`，繼承 `BaseRepository` |
| `dto` | `XxxDTO.kt` | `ChatRequestDTO.kt` | - |
| `enumerate` | `XxxType.kt` 或 `Xxx.kt` | `Platform.kt`、`ModelType.kt` | - |
| `exception` | `XxxException.kt` | `LLMException.kt` | - |
| `constant` | `XxxConstant.kt` | `LLMConstant.kt` | - |
| `util` | `XxxUtil.kt` 或 `XxxUtils.kt` | `CryptoUtils.kt` | - |
