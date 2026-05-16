# i18n 訊息管理

## 架構概覽

所有業務訊息（錯誤、成功、驗證）集中於 i18n properties 檔案，透過 `AppMessage` enum 提供 type-safe 的存取方式，避免程式碼中出現寫死的字串。

## 檔案位置

| 檔案 | 說明 |
|------|------|
| `src/main/resources/messages/messages_en_US.properties` | 英文訊息（預設） |
| `src/main/resources/messages/messages_zh_TW.properties` | 繁體中文訊息 |
| `src/main/kotlin/.../enumerate/AppMessage.kt` | 訊息 enum，每個 entry 對應一個 properties key |
| `src/main/kotlin/.../util/MessageUtil.kt` | 訊息查詢工具，支援帶參數格式化 |

## 預設語言

**預設使用英文（`Locale.US`）**。

## Key 命名規則

格式：`{category}.{subject}.{description}`

| 類別前綴 | 用途 |
|---------|------|
| `auth.*` | 認證與 JWT 相關 |
| `llm.*` | LLM 設定與模型相關 |
| `file.*` | 檔案上傳相關 |
| `embedding.*` | 向量資料庫相關 |
| `mcp.*` | MCP Server 相關 |
| `tool.*` | 工具管理相關 |
| `system.*` | 系統/基礎設施層 |
| `http.*` | HTTP 例外對應訊息（GlobalExceptionMapper 使用） |
| `success.*` | 操作成功訊息 |

帶參數的訊息使用 `{0}`, `{1}` 佔位符（`java.text.MessageFormat` 格式）。

## 使用方式

### 拋出例外（最常見）

```kotlin
// 無參數
throw ServiceException(AppMessage.TOOL_NOT_FOUND)

// 帶一個參數
throw ServiceException(AppMessage.LLM_SETTING_TYPE_MISMATCH, type)

// 帶兩個參數
throw ServiceException(AppMessage.FILE_UPLOAD_FAILED, fileName, errorMsg)
```

### 直接取得字串

```kotlin
// 無參數
val msg = MessageUtil.get(AppMessage.AUTH_NOT_LOGGED_IN)

// 帶參數
val msg = MessageUtil.get(AppMessage.EMBEDDING_VECTOR_STORE_NOT_FOUND, id)

// 使用擴充函數
val msg = AppMessage.TOOL_NOT_FOUND.msg()
val msg = AppMessage.FILE_TYPE_UNSUPPORTED.msg(mimeType)
```

### 用於 ApiResponse 成功訊息

```kotlin
return ApiResponse.success(MessageUtil.get(AppMessage.SUCCESS_DATA_DELETED))
```

## 切換語言

`MessageUtil` 以 `ThreadLocal` 儲存 locale，可在 JAX-RS filter 中讀取 `Accept-Language` header 後切換：

```kotlin
// 切換為繁體中文
MessageUtil.setLocale(Locale.TAIWAN)

// 切換回英文（預設）
MessageUtil.setLocale(Locale.US)

// 請求結束後清除（避免 ThreadLocal 洩漏）
MessageUtil.clearLocale()
```

## 新增訊息的步驟

1. 在 `messages_en_US.properties` 加入新 key（英文）
2. 在 `messages_zh_TW.properties` 加入相同 key（中文）
3. 在 `AppMessage.kt` 加入對應的 enum entry
4. 在程式碼中使用 `AppMessage.NEW_KEY` 取代寫死字串

## 注意事項

- **Logger 訊息不納入 i18n**：`logger.info/warn/error` 保持原始字串，方便除錯
- **key 找不到時的行為**：`MessageUtil` 使用 `runCatching`，找不到 key 時回傳 key 本身，不拋出例外
- **不要在 properties 中寫死語言專屬縮寫**（如 `"ch"`）；使用標準 Java `Locale` 常數
