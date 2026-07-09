---
sidebar_position: 4
description: BestPartner 集中式 i18n 訊息管理架構，透過 AppMessage enum 與 properties 檔提供 type-safe 訊息存取，支援英文與繁體中文動態切換。
keywords: [i18n, 國際化, AppMessage, 訊息管理, 繁體中文, 多語系]
---

# i18n 訊息管理

## 概述

BestPartner 採用集中式 i18n 訊息管理架構，將所有業務訊息（錯誤、成功、驗證）統一存放於 properties 檔案，並透過 `AppMessage` enum 提供 type-safe 的存取方式，避免程式碼中出現寫死的字串。

**預設語言：英文（`en_US`）**，可動態切換至繁體中文（`zh_TW`）。

---

## 架構組成

```
src/main/resources/messages/
├── messages_en_US.properties   # 英文訊息（預設）
└── messages_zh_TW.properties   # 繁體中文訊息

src/main/kotlin/.../
├── enumerate/AppMessage.kt     # 訊息 key 的 enum 定義
└── util/MessageUtil.kt         # 訊息查詢與格式化工具
```

### AppMessage enum

每個 enum entry 對應 properties 中的一個 key，依功能模組分組：

| 分組前綴 | 說明 |
|---------|------|
| `AUTH_*` | 認證與 JWT 相關 |
| `LLM_*` | LLM 設定與模型相關 |
| `FILE_*` | 檔案上傳相關 |
| `EMBEDDING_*` | 向量資料庫相關 |
| `MCP_*` | MCP Server 相關 |
| `TOOL_*` | 工具管理相關 |
| `SYSTEM_*` | 系統/基礎設施層 |
| `HTTP_*` | HTTP 例外對應訊息 |
| `SUCCESS_*` | 操作成功訊息 |

### MessageUtil

`MessageUtil` 是一個 Kotlin `object`（單例），負責：

- 讀取 `ResourceBundle`（依目前 locale 選擇對應的 properties）
- 使用 `java.text.MessageFormat` 格式化帶 `{0}`, `{1}` 佔位符的訊息
- 以 `ThreadLocal<Locale>` 儲存當前語言，支援 per-request 切換

---

## Properties Key 命名規則

格式：`{category}.{subject}.{description}`

帶參數的訊息使用 Java MessageFormat 的 `{0}`, `{1}` 佔位符：

```properties
# 無參數
tool.not.found=Tool not found

# 一個參數
llm.setting.type.mismatch=Please verify the LLM setting type is {0}

# 兩個參數
file.upload.failed=File upload failed: {0} - {1}
```

---

## 使用方式

### 在 Service 層拋出例外

`ServiceException` 提供接受 `AppMessage` 的建構子，無需額外呼叫 `MessageUtil`：

```kotlin
// 無參數
throw ServiceException(AppMessage.TOOL_NOT_FOUND)

// 帶一個參數
throw ServiceException(AppMessage.LLM_SETTING_TYPE_MISMATCH, type)

// 帶兩個參數
throw ServiceException(AppMessage.FILE_UPLOAD_FAILED, fileName, errorMsg)
```

### 直接取得訊息字串

```kotlin
// 透過 MessageUtil
val msg = MessageUtil.get(AppMessage.AUTH_NOT_LOGGED_IN)
val msg = MessageUtil.get(AppMessage.EMBEDDING_VECTOR_STORE_NOT_FOUND, id)

// 透過擴充函數（更簡潔）
val msg = AppMessage.TOOL_NOT_FOUND.msg()
val msg = AppMessage.FILE_TYPE_UNSUPPORTED.msg(mimeType)
```

### 用於 Resource 層回傳成功訊息

```kotlin
return ApiResponse.success(MessageUtil.get(AppMessage.SUCCESS_DATA_DELETED))
```

---

## 語言切換

`MessageUtil.setLocale()` 可切換當前 Thread 的語言。建議在 JAX-RS filter 中讀取 `Accept-Language` header 後設定：

```kotlin
// 切換為繁體中文
MessageUtil.setLocale(Locale.TAIWAN)

// 切換回英文（預設）
MessageUtil.setLocale(Locale.US)

// 請求結束後清除，避免 ThreadLocal 洩漏
MessageUtil.clearLocale()
```

---

## 新增訊息步驟

1. 在 `messages_en_US.properties` 加入新 key（英文內容）
2. 在 `messages_zh_TW.properties` 加入相同 key（中文內容）
3. 在 `AppMessage.kt` enum 的對應分組加入新 entry
4. 在程式碼中使用 `AppMessage.NEW_KEY` 取代原本的字串

```properties
# messages_en_US.properties
my.new.message=Something went wrong: {0}

# messages_zh_TW.properties
my.new.message=發生錯誤: {0}
```

```kotlin
// AppMessage.kt
MY_NEW_MESSAGE("my.new.message"),

// 使用
throw ServiceException(AppMessage.MY_NEW_MESSAGE, detail)
```

---

## 注意事項

- **Logger 訊息不納入 i18n**：`logger.info/warn/error` 保持原始字串，方便除錯
- **Key 找不到時的行為**：`MessageUtil` 使用 `runCatching`，找不到 key 時回傳 key 本身（不拋出例外）
- **未來擴充語言**：只需新增對應的 `messages_{locale}.properties` 檔案，現有程式碼無需任何修改
