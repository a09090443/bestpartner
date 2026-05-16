---
sidebar_position: 1
---

# AI 模型設定

BestPartner 支援多種 AI 模型，所有模型設定皆儲存於資料庫，可透過 API 動態切換，無需重啟服務。

## 支援的模型提供者

| 提供者（Platform）| 類型 | 支援功能 |
|-----------------|------|---------|
| `OPENAI` | 雲端 | 對話、串流對話、嵌入 |
| `ANTHROPIC` | 雲端 | 對話、串流對話 |
| `GEMINI` | 雲端 | 對話、串流對話、嵌入 |
| `GROK` | 雲端 | 對話、串流對話（相容 OpenAI API）|
| `OLLAMA` | 本地端 | 對話、串流對話、嵌入 |

:::info 嵌入模型支援
Anthropic 目前不支援嵌入模型（Embedding），RAG 知識庫需使用 OpenAI、Gemini 或 Ollama 的嵌入模型。
:::

## 模型類型（ModelType）

每個模型設定都需要指定類型：

| 類型 | 說明 |
|------|------|
| `CHAT` | 一般對話模式 |
| `STREAMING_CHAT` | 串流對話模式（逐字輸出）|
| `EMBEDDING` | 嵌入模型（用於 RAG 知識庫）|

## 資料庫表格結構

模型設定儲存在 `llm_setting` 和 `llm_platform` 兩張表：

**`llm_platform` 表（平台定義）：**
```sql
id       -- UUID 主鍵
name     -- Platform 類型（OPENAI / ANTHROPIC / GEMINI / GROK / OLLAMA）
```

**`llm_setting` 表（模型設定）：**
```sql
id               -- UUID 主鍵
user_id          -- 使用者 ID
platform_id      -- 對應 llm_platform.id
type             -- ModelType（CHAT / STREAMING_CHAT / EMBEDDING）
alias            -- 自訂別名
model_setting    -- JSON，包含模型細節設定（模型名稱、溫度等）
api_key          -- API Key（AES-GCM 加密儲存）
```

## 設定模型範例

### OpenAI

```sql
INSERT INTO llm_setting (id, platform_id, type, alias, model_setting, api_key)
VALUES (
  UUID(),
  (SELECT id FROM llm_platform WHERE name = 'OPENAI'),
  'CHAT',
  'GPT-4o',
  '{"modelName":"gpt-4o","temperature":0.7,"maxTokens":2048}',
  'sk-xxxxxxxx'
);
```

### Ollama（本地端）

```sql
INSERT INTO llm_setting (id, platform_id, type, alias, model_setting)
VALUES (
  UUID(),
  (SELECT id FROM llm_platform WHERE name = 'OLLAMA'),
  'STREAMING_CHAT',
  'Llama 3.2 本地',
  '{"modelName":"llama3.2","baseUrl":"http://localhost:11434","temperature":0.7}'
);
```

### 嵌入模型（用於 RAG）

```sql
INSERT INTO llm_setting (id, platform_id, type, alias, model_setting, api_key)
VALUES (
  UUID(),
  (SELECT id FROM llm_platform WHERE name = 'OPENAI'),
  'EMBEDDING',
  'OpenAI Embedding',
  '{"modelName":"text-embedding-3-small"}',
  'sk-xxxxxxxx'
);
```
