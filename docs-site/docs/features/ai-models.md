---
sidebar_position: 1
---

# AI 模型設定

BestPartner 支援多種 AI 模型，所有模型設定皆儲存於資料庫，可透過 API 動態切換，無需重啟服務。

## 支援的模型提供者

| 提供者 | 類型 | 支援功能 |
|--------|------|---------|
| **OpenAI** | 雲端 | 對話、嵌入、串流 |
| **Anthropic Claude** | 雲端 | 對話、串流 |
| **Google Gemini** | 雲端 | 對話、串流 |
| **Grok (xAI)** | 雲端 | 對話、串流 |
| **Ollama** | 本地端 | 對話、嵌入、串流 |

## 設定模型

### 透過資料庫直接設定

在 `llm_model` 表中新增模型設定：

```sql
INSERT INTO llm_model (name, platform, model_name, api_key, base_url, temperature, max_tokens)
VALUES ('GPT-4o', 'OPENAI', 'gpt-4o', 'sk-xxxxxxxx', NULL, 0.7, 2048);

INSERT INTO llm_model (name, platform, model_name, api_key, base_url, temperature, max_tokens)
VALUES ('Claude 3.5', 'ANTHROPIC', 'claude-3-5-sonnet-20241022', 'sk-ant-xxxxxxxx', NULL, 0.7, 2048);

INSERT INTO llm_model (name, platform, model_name, api_key, base_url, temperature, max_tokens)
VALUES ('Llama 3.2 (本地)', 'OLLAMA', 'llama3.2', NULL, 'http://localhost:11434', 0.7, 2048);
```

### 透過 API 設定

```bash
POST /api/model
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "name": "GPT-4o",
  "platform": "OPENAI",
  "modelName": "gpt-4o",
  "apiKey": "sk-xxxxxxxx",
  "temperature": 0.7,
  "maxTokens": 2048
}
```

## 嵌入模型設定

RAG 知識庫功能需要嵌入模型（Embedding Model）將文字轉換為向量。

| 提供者 | 建議嵌入模型 |
|--------|------------|
| OpenAI | `text-embedding-3-small` |
| Ollama | `nomic-embed-text` |

```sql
INSERT INTO embedding_model (name, platform, model_name, api_key, base_url)
VALUES ('OpenAI Embedding', 'OPENAI', 'text-embedding-3-small', 'sk-xxxxxxxx', NULL);
```

## 在對話中切換模型

每次 API 呼叫時，透過 `modelId` 參數指定要使用的模型：

```json
{
  "message": "你好",
  "modelId": 1
}
```

## 注意事項

:::caution API Key 安全
- API Key 儲存在資料庫中，請確保資料庫存取有適當的安全控制
- 生產環境建議使用加密儲存
:::

:::info Ollama 本地端模型
使用 Ollama 時，`apiKey` 欄位留空，`baseUrl` 填入 Ollama 服務的位址（預設 `http://localhost:11434`）
:::
