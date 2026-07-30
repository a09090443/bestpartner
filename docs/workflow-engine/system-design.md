# BestPartner 視覺化 Workflow 引擎 v1 — 系統設計（system-design.md）

> 對齊技術棧：Quarkus 3.21 + Kotlin 2.1 + PostgreSQL + JDK 21 ｜ 套件根：`tw.zipe.bastpartner`
> 對齊既有風格：entity 繼承 `BaseEntity`（提供 `created_at` / `updated_at` / `created_by` / `updated_by`）、PK 為 `varchar(36)` UUID（`@GeneratedValue(strategy = GenerationType.UUID)`）、JSON 欄位以 `@JdbcTypeCode(SqlTypes.JSON)` 儲存、enum 以 `@Enumerated(EnumType.STRING)` 存字串、API 回應包 `ApiResponse<T>`、JWT 權限以 `@Authenticated` / `@RolesAllowed`。
> 業務 AC 見同目錄 `requirements.md`。
> 2026-07-03 修訂：§2.2 LLM_ASSISTANT config 補齊為 `ChatRequestDTO` 全集（toolSettingIds / mcpIds+mcpSettingIds / knowledgeId / files / responseFormat+outputSchema / memoryId 語意）；§2.5 補兩條 RAG 路徑取捨；§9 新增 2 個 i18n 訊息鍵。
> 2026-07-12 修訂：**LLM 節點簡化為 Agent 模式**——§2 新增 `SKILL` 型別與「能力掛載（`in:tool` 埠）」機制；§2.2 移除 LLM config 的 toolIds/mcpIds/skillIds/knowledgeId/files（改由 TOOL/MCP/SKILL 獨立節點連接）；新增 §2.4.1 SKILL；新增 i18n `workflow.skill.node.not.mounted`。
> 2026-07-25 修訂：**`KNOWLEDGE_RAG` 可作為 LLM 外掛（自動注入型 RAG）**——加入 `CAPABILITY_SOURCE_TYPES`，連 `in:tool` 時由 langchain4j `RetrievalAugmentor` 於推論前自動檢索注入（可同時掛多個知識庫，`DefaultQueryRouter` 合併），連 `out:main` 維持 §2.5 顯式檢索（並存，依出邊型別二選一）；§2.5 必填契約簡化為僅 `knowledgeId`（`query`/`embeddingModelId` 降為選填）；新增 `dto/KnowledgeMount`、`ChatRequestDTO.knowledgeMounts`。
> 2026-07-30 修訂：**多觸發點各自獨立執行**——同一畫布可有多個 TRIGGER、各接一條下游流程並匯流到同一顆節點；`execute` 新增選填 `triggerNodeKey` 指定入口，未選定的 TRIGGER 與其獨佔下游落 SKIPPED（沿用 CONDITION 分支的活化規則，刻意不做可達性剪枝）。新增 §2.1.1；§1 ERD 與 §1.2 DDL 新增 `llm_workflow_execution.trigger_node_key varchar(64)`；§3 流程圖加入未選定 TRIGGER 分支；§5 契約補參數；§9 新增 i18n `workflow.trigger.node.not.found`、`workflow.trigger.node.invalid`。
> 2026-07-28 修訂：**新增 `PROMPT` 提示詞節點（第 13 種）**——把 LLM 的提問內容抽成獨立節點，經 LLM 新增的**提示輸入埠 `in:prompt`** 餵入；該邊為一般資料流邊（參與活化與拓撲排序），故 CONDITION 分支可擇一驅動同一顆 LLM。§2 開頭新增「提示節點」段、新增 §2.2.1 PROMPT；§2.2 LLM 提問來源改為「PROMPT 節點優先、`userPrompt` 後備」；§9 新增 i18n `workflow.prompt.node.not.connected`、`workflow.llm.prompt.required`。

---

## 0. 命名與對應規劃

| 層 | 檔名（建議） | 套件路徑 | 標註 |
|----|------------|---------|------|
| Entity | `WorkflowEntity.kt` 等（見下） | `tw.zipe.bastpartner.entity` | `@Entity` 繼承 `BaseEntity` |
| Repository | `WorkflowRepository.kt` 等 | `tw.zipe.bastpartner.repository` | `@ApplicationScoped` 繼承 `BaseRepository` |
| Service | `WorkflowService.kt`、`WorkflowExecutionService.kt`、`WorkflowEngine.kt` | `tw.zipe.bastpartner.service` | `@ApplicationScoped` |
| Resource | `WorkflowResource.kt` | `tw.zipe.bastpartner.resource` | `@Path("/llm/workflow")` |
| DTO | `WorkflowDTO.kt`、`WorkflowNodeDTO.kt`、`WorkflowEdgeDTO.kt`、`WorkflowExecutionDTO.kt` 等 | `tw.zipe.bastpartner.dto` | `@Serializable` |
| Enum | `WorkflowStatus.kt`、`NodeType.kt`、`TriggerType.kt`、`ExecutionStatus.kt`、`NodeExecutionStatus.kt` | `tw.zipe.bastpartner.enumerate` | — |
| Model（JSON config 結構） | `model/workflow/*.kt` | `tw.zipe.bastpartner.model.workflow` | — |
| Scheduler | `WorkflowCronScheduler.kt` | `tw.zipe.bastpartner.service` | Quarkus Scheduler |

> 資料表名稱統一前綴 `llm_workflow`（與既有 `llm_*` 表一致）。

---

## 1. Mermaid ERD

```mermaid
erDiagram
    llm_workflow ||--o{ llm_workflow_node : contains
    llm_workflow ||--o{ llm_workflow_edge : contains
    llm_workflow ||--o{ llm_workflow_trigger : has
    llm_workflow ||--o{ llm_workflow_execution : runs
    llm_workflow_execution ||--o{ llm_workflow_node_execution : records
    llm_workflow_node ||..o{ llm_workflow_node_execution : "logical (by node_key)"
    llm_workflow_trigger ||--o{ llm_workflow_execution : triggers

    llm_workflow {
        varchar(36)  id PK
        varchar(36)  user_id "FK->llm_user.id, NOT NULL, index"
        varchar(100) name "NOT NULL"
        varchar(255) description "NULL"
        varchar(10)  status "NOT NULL, ENUM(DRAFT/ACTIVE/INACTIVE), default DRAFT"
        int          version "NOT NULL default 1, 樂觀鎖"
        json         canvas_meta "NULL, 畫布視口/縮放等前端附帶狀態"
        timestamp    created_at "NOT NULL"
        timestamp    updated_at "NULL"
        varchar(50)  created_by "NULL"
        varchar(50)  updated_by "NULL"
    }

    llm_workflow_node {
        varchar(36)  id PK
        varchar(36)  workflow_id FK "NOT NULL, index"
        varchar(64)  node_key "NOT NULL, 前端產生; UNIQUE(workflow_id,node_key)"
        varchar(30)  type "NOT NULL, ENUM NodeType"
        varchar(100) name "NULL, 節點顯示名"
        double       position_x "NOT NULL"
        double       position_y "NOT NULL"
        json         config "NOT NULL, 各節點型別專屬設定(見 §2)"
        timestamp    created_at "NOT NULL"
        timestamp    updated_at "NULL"
        varchar(50)  created_by "NULL"
        varchar(50)  updated_by "NULL"
    }

    llm_workflow_edge {
        varchar(36)  id PK
        varchar(36)  workflow_id FK "NOT NULL, index"
        varchar(64)  source_node_key "NOT NULL"
        varchar(64)  target_node_key "NOT NULL"
        varchar(64)  source_handle "NULL, 連接埠(如 condition true/false)"
        varchar(64)  target_handle "NULL"
        varchar(255) label "NULL"
        json         condition "NULL, 邊上條件(備用)"
        timestamp    created_at "NOT NULL"
        timestamp    updated_at "NULL"
        varchar(50)  created_by "NULL"
        varchar(50)  updated_by "NULL"
    }

    llm_workflow_trigger {
        varchar(36)  id PK
        varchar(36)  workflow_id FK "NOT NULL, index"
        varchar(64)  node_key "NULL, 對應畫布上的 Trigger 節點"
        varchar(10)  type "NOT NULL, ENUM(MANUAL/WEBHOOK/CRON)"
        boolean      enabled "NOT NULL default true"
        varchar(64)  webhook_token "NULL, UNIQUE; WEBHOOK 型才有"
        varchar(120) cron_expression "NULL; CRON 型才有"
        varchar(10)  overlap_policy "NULL, ENUM(SKIP/QUEUE) default SKIP"
        json         config "NULL, 觸發其他設定"
        timestamp    created_at "NOT NULL"
        timestamp    updated_at "NULL"
        varchar(50)  created_by "NULL"
        varchar(50)  updated_by "NULL"
    }

    llm_workflow_execution {
        varchar(36)  id PK
        varchar(36)  workflow_id FK "NOT NULL, index"
        int          workflow_version "NOT NULL, 執行當下版本快照"
        varchar(36)  trigger_id "NULL, FK->llm_workflow_trigger.id"
        varchar(64)  trigger_node_key "NULL, 發起本次執行的 TRIGGER 節點 node_key；NULL=未指定(全部觸發點皆執行)"
        varchar(10)  trigger_type "NOT NULL, ENUM(MANUAL/WEBHOOK/CRON)"
        varchar(80)  triggered_by "NOT NULL, userId 或 cron:{id}/webhook:{id}"
        varchar(12)  status "NOT NULL, ENUM ExecutionStatus, index"
        json         input_payload "NULL"
        json         output_result "NULL, 末端節點彙整輸出"
        varchar(64)  error_node_key "NULL"
        text         error_message "NULL"
        timestamp    started_at "NOT NULL, index"
        timestamp    finished_at "NULL"
        bigint       duration_ms "NULL"
        timestamp    created_at "NOT NULL"
        timestamp    updated_at "NULL"
        varchar(50)  created_by "NULL"
        varchar(50)  updated_by "NULL"
    }

    llm_workflow_node_execution {
        varchar(36)  id PK
        varchar(36)  execution_id FK "NOT NULL, index"
        varchar(36)  workflow_id "NOT NULL"
        varchar(64)  node_key "NOT NULL"
        varchar(30)  node_type "NOT NULL"
        int          seq_no "NOT NULL, 執行序"
        varchar(12)  status "NOT NULL, ENUM NodeExecutionStatus"
        json         input "NULL"
        json         output "NULL"
        text         error_message "NULL"
        int          loop_index "NULL, Loop 子節點的迭代索引"
        timestamp    started_at "NULL"
        timestamp    finished_at "NULL"
        bigint       duration_ms "NULL"
        timestamp    created_at "NOT NULL"
        timestamp    updated_at "NULL"
        varchar(50)  created_by "NULL"
        varchar(50)  updated_by "NULL"
    }
```

### 1.1 索引與約束建議

| 資料表 | 約束 / 索引 |
|--------|------------|
| `llm_workflow` | PK(`id`)；index(`user_id`)；index(`status`) |
| `llm_workflow_node` | PK(`id`)；**UNIQUE(`workflow_id`,`node_key`)**；index(`workflow_id`) |
| `llm_workflow_edge` | PK(`id`)；index(`workflow_id`)；index(`workflow_id`,`source_node_key`) |
| `llm_workflow_trigger` | PK(`id`)；**UNIQUE(`webhook_token`)**；index(`workflow_id`)；index(`type`,`enabled`)（cron 掃描用） |
| `llm_workflow_execution` | PK(`id`)；index(`workflow_id`,`started_at`)；index(`status`) |
| `llm_workflow_node_execution` | PK(`id`)；index(`execution_id`)；index(`execution_id`,`seq_no`) |

> FK 在 PostgreSQL DDL 中可顯式宣告（既有專案多以邏輯關聯為主、未必每張表都建實體 FK；建議至少對 `execution_id`、`workflow_id` 建索引）。刪除 workflow 時於 service 層交易內手動連鎖刪除（對齊既有作法）。

### 1.2 DDL 草稿（PostgreSQL，置於 `docs/sql/bestpartner-ddl.sql`）

```sql
DROP TABLE IF EXISTS "llm_workflow_node_execution";
DROP TABLE IF EXISTS "llm_workflow_execution";
DROP TABLE IF EXISTS "llm_workflow_trigger";
DROP TABLE IF EXISTS "llm_workflow_edge";
DROP TABLE IF EXISTS "llm_workflow_node";
DROP TABLE IF EXISTS "llm_workflow";

CREATE TABLE "llm_workflow" (
    "id"          varchar(36)  NOT NULL,
    "user_id"     varchar(36)  NOT NULL,
    "name"        varchar(100) NOT NULL,
    "description" varchar(255) DEFAULT NULL,
    "status"      varchar(10)  NOT NULL DEFAULT 'DRAFT',
    "version"     int          NOT NULL DEFAULT 1,
    "canvas_meta" json         DEFAULT NULL,
    "created_at"  timestamp    NOT NULL,
    "updated_at"  timestamp    NULL DEFAULT NULL,
    "created_by"  varchar(50)  DEFAULT NULL,
    "updated_by"  varchar(50)  DEFAULT NULL,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_workflow_user" ON "llm_workflow" ("user_id");
CREATE INDEX "idx_workflow_status" ON "llm_workflow" ("status");

CREATE TABLE "llm_workflow_node" (
    "id"          varchar(36)  NOT NULL,
    "workflow_id" varchar(36)  NOT NULL,
    "node_key"    varchar(64)  NOT NULL,
    "type"        varchar(30)  NOT NULL,
    "name"        varchar(100) DEFAULT NULL,
    "position_x"  double precision NOT NULL,
    "position_y"  double precision NOT NULL,
    "config"      json         NOT NULL,
    "created_at"  timestamp    NOT NULL,
    "updated_at"  timestamp    NULL DEFAULT NULL,
    "created_by"  varchar(50)  DEFAULT NULL,
    "updated_by"  varchar(50)  DEFAULT NULL,
    PRIMARY KEY ("id"),
    UNIQUE ("workflow_id", "node_key")
);
CREATE INDEX "idx_workflow_node_wf" ON "llm_workflow_node" ("workflow_id");

CREATE TABLE "llm_workflow_edge" (
    "id"              varchar(36)  NOT NULL,
    "workflow_id"     varchar(36)  NOT NULL,
    "source_node_key" varchar(64)  NOT NULL,
    "target_node_key" varchar(64)  NOT NULL,
    "source_handle"   varchar(64)  DEFAULT NULL,
    "target_handle"   varchar(64)  DEFAULT NULL,
    "label"           varchar(255) DEFAULT NULL,
    "condition"       json         DEFAULT NULL,
    "created_at"      timestamp    NOT NULL,
    "updated_at"      timestamp    NULL DEFAULT NULL,
    "created_by"      varchar(50)  DEFAULT NULL,
    "updated_by"      varchar(50)  DEFAULT NULL,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_workflow_edge_wf" ON "llm_workflow_edge" ("workflow_id");

CREATE TABLE "llm_workflow_trigger" (
    "id"              varchar(36)  NOT NULL,
    "workflow_id"     varchar(36)  NOT NULL,
    "node_key"        varchar(64)  DEFAULT NULL,
    "type"            varchar(10)  NOT NULL,
    "enabled"         boolean      NOT NULL DEFAULT true,
    "webhook_token"   varchar(64)  DEFAULT NULL,
    "cron_expression" varchar(120) DEFAULT NULL,
    "overlap_policy"  varchar(10)  DEFAULT 'SKIP',
    "config"          json         DEFAULT NULL,
    "created_at"      timestamp    NOT NULL,
    "updated_at"      timestamp    NULL DEFAULT NULL,
    "created_by"      varchar(50)  DEFAULT NULL,
    "updated_by"      varchar(50)  DEFAULT NULL,
    PRIMARY KEY ("id"),
    UNIQUE ("webhook_token")
);
CREATE INDEX "idx_workflow_trigger_wf" ON "llm_workflow_trigger" ("workflow_id");
CREATE INDEX "idx_workflow_trigger_scan" ON "llm_workflow_trigger" ("type", "enabled");

CREATE TABLE "llm_workflow_execution" (
    "id"               varchar(36) NOT NULL,
    "workflow_id"      varchar(36) NOT NULL,
    "workflow_version" int         NOT NULL,
    "trigger_id"       varchar(36) DEFAULT NULL,
    "trigger_node_key" varchar(64) DEFAULT NULL,
    "trigger_type"     varchar(10) NOT NULL,
    "triggered_by"     varchar(80) NOT NULL,
    "status"           varchar(12) NOT NULL,
    "input_payload"    json        DEFAULT NULL,
    "output_result"    json        DEFAULT NULL,
    "error_node_key"   varchar(64) DEFAULT NULL,
    "error_message"    text        DEFAULT NULL,
    "started_at"       timestamp   NOT NULL,
    "finished_at"      timestamp   NULL DEFAULT NULL,
    "duration_ms"      bigint      DEFAULT NULL,
    "created_at"       timestamp   NOT NULL,
    "updated_at"       timestamp   NULL DEFAULT NULL,
    "created_by"       varchar(50) DEFAULT NULL,
    "updated_by"       varchar(50) DEFAULT NULL,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_workflow_exec_wf" ON "llm_workflow_execution" ("workflow_id", "started_at");
CREATE INDEX "idx_workflow_exec_status" ON "llm_workflow_execution" ("status");

CREATE TABLE "llm_workflow_node_execution" (
    "id"            varchar(36) NOT NULL,
    "execution_id"  varchar(36) NOT NULL,
    "workflow_id"   varchar(36) NOT NULL,
    "node_key"      varchar(64) NOT NULL,
    "node_type"     varchar(30) NOT NULL,
    "seq_no"        int         NOT NULL,
    "status"        varchar(12) NOT NULL,
    "input"         json        DEFAULT NULL,
    "output"        json        DEFAULT NULL,
    "error_message" text        DEFAULT NULL,
    "loop_index"    int         DEFAULT NULL,
    "started_at"    timestamp   NULL DEFAULT NULL,
    "finished_at"   timestamp   NULL DEFAULT NULL,
    "duration_ms"   bigint      DEFAULT NULL,
    "created_at"    timestamp   NOT NULL,
    "updated_at"    timestamp   NULL DEFAULT NULL,
    "created_by"    varchar(50) DEFAULT NULL,
    "updated_by"    varchar(50) DEFAULT NULL,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_workflow_nodeexec_exec" ON "llm_workflow_node_execution" ("execution_id", "seq_no");
```

### 1.3 資料表用途一句話

| 資料表 | 用途 |
|--------|------|
| `llm_workflow` | workflow 定義主檔（名稱、擁有者、狀態、版本、畫布視口）。 |
| `llm_workflow_node` | 畫布上的節點（type + 座標 + config JSON），以 `node_key` 在 workflow 內唯一識別。 |
| `llm_workflow_edge` | 節點間的連線（source/target node_key + handle），承載分支關係。 |
| `llm_workflow_trigger` | 觸發來源設定（manual / webhook token / cron 表達式）。 |
| `llm_workflow_execution` | 每次執行的整體紀錄（狀態、觸發者、起訖、輸入輸出、錯誤定位）。 |
| `llm_workflow_node_execution` | 單次執行中每個節點的逐筆紀錄（input/output/耗時/狀態/錯誤），供 UI 回放。 |

---

## 2. 節點類型目錄（NodeType）與 config schema

> ⚠️ **事實來源**：config schema 已實作為強型別 DTO——`bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/dto/workflow/config/NodeConfig.kt`（`NodeConfigRegistry` 於 save 驗型別與未知欄位、switchStatus 啟用驗必填）。本節為對照說明文件；兩者不一致時以程式碼為準並回頭修訂本節。

> `type` 存於 `llm_workflow_node.type`（enum string）；`config` 存於 `llm_workflow_node.config`（JSON 欄位，`@JdbcTypeCode(SqlTypes.JSON)`）。所有 config 內字串值支援 `{{nodeKey.outputPath}}` 變數插值（引用上游 node_execution.output）。

`enum class NodeType { TRIGGER, LLM_ASSISTANT, PROMPT, TOOL, MCP_SERVER, SKILL, KNOWLEDGE_RAG, CONDITION, LOOP, CODE, HTTP_REQUEST, DATA_TRANSFORM, OUTPUT }`

> **Agent 模式能力掛載（LLM 節點簡化）**：`LLM_ASSISTANT` 節點只負責呼叫指定 LLM。工具、MCP、Skill 改為獨立節點（`TOOL` / `MCP_SERVER` / `SKILL`），以 `out:main` 連到 LLM 節點的**專用工具輸入埠 `in:tool`**（`targetHandle === "in:tool"`）。引擎於執行前把這些「能力節點」解析為掛載清單放入 `ExecutionContext`（`WorkflowEngine.resolveCapabilityMounts`），由 `LlmAssistantExecutor` 於推論前組裝成 langchain4j 工具集，交 LLM 自主決定何時呼叫。
> - 「純能力節點」（所有出邊皆 `in:tool`）排除主遍歷、不落 `node_execution` 紀錄；仍另接 `out:main` 下游的 TOOL/MCP 為雙用，照常執行並額外充當能力。
> - `in:tool` 邊不參與活化判斷（`WorkflowEngine` 以 `flowEdges` 排除），只靠工具埠連入的 LLM 不會被誤判 SKIPPED。
> - `SKILL` 無獨立 executor；孤兒 SKILL（未連任何 LLM 的 `in:tool`）於驗證期回 `WORKFLOW_SKILL_NODE_NOT_MOUNTED`。
> - `KNOWLEDGE_RAG` 連 `in:tool` 時語義**不同於工具集**：作為**自動注入型 RAG**。`LlmAssistantExecutor` 聚合其 `knowledgeId`/`topK`/`minScore` 為 `ChatRequestDTO.knowledgeMounts`，`LLMService.buildAIService` 為每個知識庫建 `EmbeddingStoreContentRetriever`（各帶 per-id metadata filter），多個時以 `DefaultQueryRouter` 合併，掛為 `RetrievalAugmentor`，推論前依 LLM 問題自動檢索注入。KNOWLEDGE_RAG 有自己的 executor，未掛載時就當 §2.5 pipeline 檢索節點跑，不會像 SKILL 成孤兒。

> **提示節點（PROMPT）— 與能力掛載相對的資料流埠**：`PROMPT` 節點以 `out:main` 連到 LLM 的**提示輸入埠 `in:prompt`**（`targetHandle === "in:prompt"`），提供該次推論的提問內容。
> - ⚠️ 此邊**是一般資料流邊**（`flowEdges` 只排除 `in:tool`，不排除 `in:prompt`）：照常參與活化判斷與拓撲排序，PROMPT 節點也照常執行、落 `node_execution` 紀錄並發 SSE 事件。正因如此，CONDITION 的兩個分支可各接一個 PROMPT 再匯入同一顆 LLM——只有被活化那條有輸出。
> - 引擎於執行前以 `WorkflowEngine.resolvePromptSources` 解析 `llmNodeKey → List<PromptSource(nodeKey, outputKey)>`（依拓撲序排序、含各節點自訂 outputKey）注入 `ExecutionContext`；`LlmAssistantExecutor.resolveMessage` 取**第一個已有輸出**的來源，其次才回退 `userPrompt`。
> - 兩項圖層級驗證（switchStatus 啟用前與 execute 執行前共用純函式，與孤兒 SKILL 同形）：孤兒 PROMPT（未連任何 LLM 的 `in:prompt`）回 `WORKFLOW_PROMPT_NODE_NOT_CONNECTED`；LLM 既無 `userPrompt` 也無 PROMPT 連入回 `WORKFLOW_LLM_PROMPT_REQUIRED`。

### 2.1 TRIGGER（觸發節點，子型 manual / webhook / cron）

用途：標記流程入口，決定誰啟動 execution。實際 webhook token / cron 表達式落在 `llm_workflow_trigger`，節點 config 僅描述子型與輸入 schema。

```json
{
  "triggerType": "WEBHOOK",
  "inputSchema": { "userMessage": "string", "lang": "string" },
  "webhook": { "method": "POST" },
  "cron": { "expression": "0 0 9 * * ?", "overlapPolicy": "SKIP" }
}
```

#### 2.1.1 多觸發點：同一畫布多個入口，各自獨立執行

同一張畫布可放**多個 TRIGGER 節點**，每個各接一條下游流程，且這些流程可**匯流到同一顆節點**
（典型情境：多個入口共用一顆 `LLM_ASSISTANT`，各自搭配自己的 `PROMPT`）。
圖驗證只要求「至少一顆 TRIGGER」（`WorkflowEngine.validateNodes`），**無上限**。

執行時以 `POST /llm/workflow/execute` 的 `triggerNodeKey` 指定入口：

| 情形 | 行為 |
|------|------|
| 帶合法 `triggerNodeKey` | 僅該 TRIGGER 被活化；其餘 TRIGGER 落 `SKIPPED`，其獨佔下游連鎖 `SKIPPED`。值記入 `llm_workflow_execution.trigger_node_key` |
| 省略 / 空白 | **所有 TRIGGER 皆執行**（向後相容；webhook / cron 端點上線後亦沿用此路徑）。`trigger_node_key` 為 NULL |
| nodeKey 不存在 | 400 `workflow.trigger.node.not.found`（含 nodeKey） |
| nodeKey 型別非 TRIGGER | 400 `workflow.trigger.node.invalid`（含 nodeKey 與實際型別） |

驗證位於 `validateNodes`（緊接「至少一顆 TRIGGER」檢查之後），故 `validateForExecution`
與 `execute` 兩條路徑一致，且**於建立執行紀錄之前**即失敗，不留孤兒紀錄。

**實作機制：只擋 TRIGGER 的活化，不做可達性剪枝。**
下游剪枝由既有活化規則自然導出——未選定的 TRIGGER 不執行 → 不活化出邊 →
其獨佔下游「有入邊但無一被活化」→ 走既有 SKIPPED 路徑（與 §2 開頭 CONDITION 分支同一套規則）。

> ⚠️ **刻意不採「計算選定 TRIGGER 的可達集合、集合外一律 SKIPPED」**：那會誤殺 indegree 0 的
> **非** TRIGGER 節點。畫布並未要求 `PROMPT` 必須有入邊，因此「只有 `out:main → LLM in:prompt`
> 的孤立 PROMPT」是合法形狀，今日靠「無入邊則恆 active」而照常執行；常數型
> `HTTP_REQUEST` / `DATA_TRANSFORM` / `CODE` 同理。剪掉它們會讓共用 LLM 取不到提問。

匯流節點的提問來源由既有 `resolvePromptSources` + `LlmAssistantExecutor.resolveMessage`
自動處理：後者取「**第一個已有輸出**」的提示來源，未選中分支的 PROMPT 是 SKIPPED、無輸出，
故自動命中被選中那條——與 CONDITION 分支走的是同一條路，無需額外邏輯。

**最終輸出形狀會隨之改變**（`collectFinalOutput` 只計入實際執行成功的 OUTPUT 節點）：
畫布有 t1→outA、t2→outB 時，不指定觸發點會得到 `{outA: {...}, outB: {...}}` 的合併形狀；
指定 t1 後只剩一個已執行的 OUTPUT，直接回該節點的 map 本身。

> **遺留陷阱**：共用節點若以 `{{t2.field}}` 引用未被選中分支的節點，選 t1 執行時會
> `workflow.variable.not.found`。這與「引用 CONDITION 未活化分支的節點」是同一個既有陷阱。

### 2.2 LLM_ASSISTANT（LLM / 自定義助手，Agent 模式）

用途：呼叫指定 LLM（同步）。工具 / MCP / Skill 改由獨立節點連到本節點的 `in:tool` 埠掛載（見 §2 開頭「Agent 模式能力掛載」），本節點 config 只保留 LLM 呼叫本身相關欄位。executor 重用 `LLMService.buildAIService`（Memory + 掛載工具）與 `LLMService.applyToolProviders`（Skill + MCP 合併掛載，解決 `AiServices.toolProvider` 單插槽覆蓋）。

```json
{
  "llmId": "a1b2c3d4-...",
  "systemPrompt": "你是客服助手",
  "userPrompt": "請回覆：{{trigger.userMessage}}",
  "enableMemory": false,
  "memoryId": "{{trigger.sessionId}}",
  "responseFormat": "TEXT",
  "outputSchema": null,
  "outputKey": "reply"
}
```

欄位說明：

| 欄位 | 對應能力 | 說明 |
|------|---------|------|
| `llmId` | LLM（必填） | 指向 `modelType=CHAT` 的 LLM 設定。 |
| `userPrompt` | 提問內容（後備） | 提問優先取連入 `in:prompt` 埠且已執行成功的 `PROMPT` 節點輸出，沒接才用本欄位（見 §2 開頭「提示節點」與 §2.2.1）。兩者皆無時無法啟用（`WORKFLOW_LLM_PROMPT_REQUIRED`），故本欄位非無條件必填。 |
| `enableMemory` / `memoryId` | Memory | `enableMemory=true` 且 `memoryId` 為 null 時，預設使用 `execution:{executionId}`（同一次執行內多個 LLM 節點共享、跨執行不共享）；要跨執行延續對話須顯式指定 `memoryId`（支援 `{{變數}}` 插值，例如 webhook 傳入的 sessionId）。 |
| `responseFormat` / `outputSchema` | Structured Output | `TEXT`（預設）：output 為 `{outputKey: 純文字}`。`JSON`：以 langchain4j 的 JSON schema response format 要求模型回傳符合 `outputSchema`（JSON Schema 物件）的結構化 JSON。 |

> **工具 / MCP / Skill / 知識庫**：不再是本節點 config 欄位。改以 `TOOL` / `MCP_SERVER` / `SKILL` / `KNOWLEDGE_RAG` 獨立節點連到 `in:tool` 埠（能力掛載）。知識庫有兩種用法：掛 `in:tool` 為**自動注入型 RAG**（見 §2 開頭與 §2.5），或以 `KNOWLEDGE_RAG` 顯式檢索後用 `{{node.outputKey}}` 插值餵回 `userPrompt`。executor 依掛載的能力節點 config 聚合出 `toolIds` / `toolSettingIds`（TOOL）、`mcpIds` / `mcpSettingIds`（MCP）、`skillIds`（SKILL）、`knowledgeMounts`（KNOWLEDGE_RAG）。
>
> ⚠️ 相容性：`NodeConfigRegistry` 為嚴格 JSON（`ignoreUnknownKeys=false`），舊有含 `toolIds` / `mcpIds` / `skillIds` / `knowledgeId` / `files` 的 LLM 節點 config 會於 parse 拋例外。開發期以重建 / 更新種子資料處理（不寫遷移腳本）；前端表單存檔時亦會主動剝除這些殘留鍵。

> ⚠️ 已知限制（v1）：LLM 節點一律走同步 `ChatModel`，不支援 `StreamingChatModel` / SSE token 串流；`llmId` 必須指向 `modelType=CHAT` 的 LLM 設定。

### 2.3 TOOL（內建工具節點）

用途：呼叫 `ToolService` 既有內建工具（Google / Tavily / Date / Text2SQL）。

```json
{
  "toolId": "tool-uuid",
  "toolSettingId": "user-tool-setting-uuid",
  "arguments": { "query": "{{trigger.userMessage}}" },
  "outputKey": "searchResult"
}
```

### 2.4 MCP_SERVER（MCP 伺服器節點）

用途：呼叫 `McpServerService` 已註冊之 MCP server 的某工具。

```json
{
  "mcpId": "mcp-uuid",
  "userSettingId": "mcp-user-setting-uuid",
  "toolName": "list_files",
  "arguments": { "path": "/data" },
  "outputKey": "files"
}
```

### 2.2.1 PROMPT（提示詞節點）

用途：把 LLM 的提問內容抽成獨立節點，讓同一顆 LLM 可由不同分支的提示節點驅動。`PromptExecutor` 將 `prompt` 以 `ExecutionContext.resolveTemplate` 插值一次後輸出（下游 LLM 直接取字串、不再二次插值，避免上游輸出本身含 `{{ }}` 時被重複展開）。

```json
{
  "prompt": "以正式語氣回答：{{trigger.question}}",
  "outputKey": "prompt"
}
```

必填：`prompt`。`outputKey` 預設 `prompt`。輸出亦可被其他節點以 `{{promptNodeKey.prompt}}` 引用。
接線與驗證規則見 §2 開頭「提示節點」段。

### 2.4.1 SKILL（Skill 能力節點，Agent 模式）

用途：作為能力提供者，把一個 Skill 掛載到 LLM 節點。只連到 `LLM_ASSISTANT` 的 `in:tool` 埠，本身無獨立 executor、不落 `node_execution` 紀錄；`LlmAssistantExecutor` 讀取其 `skillId` 併入 `skillIds`，經 `SkillService.buildSkills` + `activate_skill` 工具與系統提示注入。

```json
{
  "skillId": "skill-uuid"
}
```

必填：`skillId`。孤兒 SKILL（未連任何 LLM 的 `in:tool`）於 save/switchStatus/execute 前驗證回 `WORKFLOW_SKILL_NODE_NOT_MOUNTED`（i18n）。

### 2.5 KNOWLEDGE_RAG（向量 / RAG 檢索節點）

用途：呼叫 `EmbeddingService` 依 knowledgeId + query 做向量相似度檢索。

```json
{
  "knowledgeId": "knowledge-uuid",
  "embeddingModelId": "emb-uuid",
  "query": "{{trigger.userMessage}}",
  "topK": 5,
  "minScore": 0.6,
  "outputKey": "contexts"
}
```

> **兩種使用模式（並存，依出邊型別二選一）**：
> - **顯式檢索（pipeline，連 `out:main`）**：檢索結果落地於 node_execution.output，可被 `CONDITION` / `DATA_TRANSFORM` / 任意下游節點引用（可觀測、可分支），或以 `{{node.outputKey}}` 插值餵回 LLM `userPrompt`。此模式 `query` 必填——由 executor 執行時驗證（缺 query 拋明確錯誤）。
> - **LLM 外掛（自動注入，連 `LLM_ASSISTANT` 的 `in:tool`）**：由 langchain4j `RetrievalAugmentor` 在 LLM 呼叫內部依 LLM 問題自動檢索注入，不落地中間結果；可同時掛多個知識庫（`DefaultQueryRouter`）。此模式 `query`/`embeddingModelId` 免填（query 由 LLM 問題帶入、embedding 由知識庫自身設定決定）。
>
> 需要「對檢索結果做流程控制」用顯式檢索；只需要「LLM 回答時參考知識庫」用 LLM 外掛。
>
> **必填契約**：僅 `knowledgeId` 為無條件必填（兩模式共通）；`query`/`embeddingModelId` 不列入 `missingRequiredFields`，故 `nodeRequiredFields` 端點對 KNOWLEDGE_RAG 只回 `["knowledgeId"]`。

### 2.6 CONDITION（條件分支 if-else）

用途：評估條件運算式，決定走 `sourceHandle = "true"` 或 `"false"` 的 edge。

```json
{
  "conditions": [
    { "left": "{{tool.searchResult.length}}", "operator": "GT", "right": "0" }
  ],
  "logic": "AND",
  "trueHandle": "true",
  "falseHandle": "false"
}
```
> operator 列舉建議：EQ / NEQ / GT / GTE / LT / LTE / CONTAINS / IS_EMPTY / IS_NOT_EMPTY / REGEX。

### 2.7 LOOP（迴圈 / 陣列迭代）

用途：對指定陣列逐筆迭代執行 loop body 子圖；以 `loopBodyEntryNodeKey` 標示子圖入口，子節點 node_execution 以 `loop_index` 標記。

```json
{
  "inputArrayPath": "{{rag.contexts}}",
  "itemAlias": "item",
  "loopBodyEntryNodeKey": "node_llm_summarize",
  "maxIterations": 1000,
  "collectOutputKey": "summaries"
}
```

### 2.8 CODE（自訂程式碼節點，v1 預設停用）

用途：執行使用者自訂 script。**v1 僅設計資料模型，feature flag `workflow.node.code.enabled=false` 時拒絕執行（回 `WORKFLOW_CODE_NODE_DISABLED`）。** 沙箱方案為高風險未決項。

```json
{
  "language": "JS",
  "source": "return { doubled: input.value * 2 };",
  "timeoutMs": 3000,
  "outputKey": "result"
}
```
> ⚠️ 風險：未受控執行使用者程式碼等同 RCE。啟用前必須有沙箱（GraalVM Polyglot 受限 context / 子行程隔離 + 資源限制），見 [LOGIC CONFLICT-2]。

### 2.9 HTTP_REQUEST（HTTP 請求節點）

用途：以既有 `OkHttpUtil` 發出外部 REST 請求。敏感 header（Authorization）以 `PasswordEncryptConverter` 加密儲存、回應遮罩。

```json
{
  "method": "POST",
  "url": "https://api.example.com/v1/notify",
  "headers": { "Content-Type": "application/json" },
  "secretHeaders": { "Authorization": "__SECRET_KEPT__" },
  "body": { "text": "{{llm.reply}}" },
  "timeoutMs": 10000,
  "outputKey": "httpResponse"
}
```

#### secretHeaders 的加密與遮罩契約

實作於 `converter/WorkflowSecretConverter.kt`，於 service 邊界對 secretHeaders 逐「值」加解密
（不做成 JPA AttributeConverter：`config` 為 `@JdbcTypeCode(SqlTypes.JSON)` 整包 Map，
掛 `@Convert` 會與 JSON 型別衝突，且會連帶加密所有節點型別的全部設定）。
儲存格式沿用 `PasswordEncryptConverter` 的 `{iv}${encrypted}`；`config` 其餘欄位維持明文 JSON。

| 時機 | 行為 |
|------|------|
| `save` | 逐值 AES-GCM 加密後落地。值為 `__SECRET_KEPT__` 時沿用 DB 既有值（既有為舊明文則於此時補加密）；無既有值（改了 header 名稱或新建 workflow）則移除該欄位，遮罩字面值不會被當成金鑰存入 |
| `get` | 值一律替換為 `__SECRET_KEPT__`，明文與密文皆不外流 |
| `execute` | 解密還原明文後才發出請求；執行紀錄 `llm_workflow_node_execution.input` 內的 config 亦經遮罩 |

> **舊明文相容**：加密上線前存入的明文於讀取時原樣回傳（沿用 `PasswordEncryptConverter` 既有容錯解密），
> 下次 save 自動轉為密文，毋須停機或跑遷移——即使前端原樣送回 `__SECRET_KEPT__`，沿用路徑會偵測到既有值
> 非 `{iv}${encrypted}` 格式而補加密，因此明文不會因使用者從不改動該 header 而永久殘留。
>
> ⚠️ **前端整張覆寫 save 時必須原樣送回 `__SECRET_KEPT__`**，否則該 header 會被視為新值或遭移除。
> HTTP_REQUEST 目前無專屬設定表單，config 走通用 JSON 編輯器，使用者會直接看到此遮罩字串。
>
> ⚠️ 加密強度取決於 `crypto.secret-key`：若未以 `CRYPTO_SECRET_KEY` 覆寫預設值，
> 等同以公開金鑰加密。見 [`configuration-and-profiles.md`](../../.claude/rules/configuration-and-profiles.md)。

### 2.10 DATA_TRANSFORM（資料轉換 / 格式化）

用途：對上游輸出做欄位映射 / 取值 / 字串模板，產生下游可用結構。

```json
{
  "mappings": [
    { "targetKey": "title", "expression": "{{rag.contexts.0.text}}" },
    { "targetKey": "count", "expression": "{{tool.searchResult.length}}" }
  ],
  "template": null,
  "outputKey": "transformed"
}
```

---

## 3. Mermaid 執行流程圖（一次 execution）

```mermaid
flowchart TD
    A[觸發來源: manual/webhook/cron] --> B[建立 workflow_execution\nstatus=RUNNING, started_at=now]
    B --> C[載入 workflow + nodes + edges]
    C --> D{圖驗證: 無環? 有 Trigger?\n節點數<=上限?\n指定的 triggerNodeKey 存在且為 TRIGGER?}
    D -- 否 --> E[execution=FAILED\n寫 error_message] --> Z[回傳結果/落地]
    D -- 是 --> F[拓樸排序 nodes]
    F --> G[依序取下一個 node]
    G --> G1{是未被選定的 TRIGGER?\n(有指定 triggerNodeKey 時)}
    G1 -- 是 --> G2[node_execution=SKIPPED\n不執行、不活化出邊、不發事件\n其獨佔下游隨之連鎖 SKIPPED] --> U
    G1 -- 否 --> H[解析 input: 套用 {{變數}} 引用上游 output]
    H --> I[寫 node_execution\nstatus=RUNNING, started_at]
    I --> J{節點類型分派\nLLM/Tool/MCP/RAG/Condition/Loop/Code/HTTP/Transform}
    J --> K{執行成功?}
    K -- 否 --> L[node_execution=FAILED\n記 error_message]
    L --> M[execution=FAILED\nerror_node_key, error_message\n後續節點標 SKIPPED] --> Z
    K -- 是 --> N[node_execution=SUCCESS\noutput, duration_ms]
    N --> O{Condition?}
    O -- 是 --> P[依結果挑選 true/false 分支 edge\n未走分支節點標 SKIPPED]
    O -- 否 --> Q{Loop?}
    Q -- 是 --> R[對陣列逐筆執行子圖\n各輪寫 node_execution loop_index]
    Q -- 否 --> S{逾時? 全程 > timeout?}
    P --> S
    R --> S
    S -- 逾時 --> T[execution=TIMEOUT\n當前節點 FAILED] --> Z
    S -- 否 --> U{還有未執行節點?}
    U -- 有 --> G
    U -- 無 --> V[彙整末端節點 output\nexecution=SUCCESS\nfinished_at, duration_ms] --> Z
    Z[落地 execution 與所有 node_execution\n回傳 ApiResponse]
```

---

## 4. Mermaid 狀態機

### 4.1 workflow_execution（ExecutionStatus）

```mermaid
stateDiagram-v2
    [*] --> PENDING: 建立紀錄(極短暫,可選)
    PENDING --> RUNNING: 引擎開始執行
    RUNNING --> SUCCESS: 所有節點成功
    RUNNING --> FAILED: 任一節點失敗 / 圖驗證失敗
    RUNNING --> TIMEOUT: 超過 timeout-seconds
    RUNNING --> CANCELLED: 系統關機/reaper 回收(v1不開放主動取消)
    SUCCESS --> [*]
    FAILED --> [*]
    TIMEOUT --> [*]
    CANCELLED --> [*]
```
> `enum class ExecutionStatus { PENDING, RUNNING, SUCCESS, FAILED, TIMEOUT, CANCELLED }`
> v1：cron 重疊跳過時另記一筆 `SKIPPED`（可併入此 enum 或獨立旗標，建議併入：`SKIPPED`）。

### 4.2 node_execution（NodeExecutionStatus）

```mermaid
stateDiagram-v2
    [*] --> PENDING: execution 建立時預登(或執行到才建立)
    PENDING --> RUNNING: 輪到此節點
    RUNNING --> SUCCESS: 節點完成
    RUNNING --> FAILED: 節點拋例外
    PENDING --> SKIPPED: 條件分支未選中 / 上游已失敗
    SUCCESS --> [*]
    FAILED --> [*]
    SKIPPED --> [*]
```
> `enum class NodeExecutionStatus { PENDING, RUNNING, SUCCESS, FAILED, SKIPPED }`

---

## 5. API 契約基礎（路徑前綴 `/llm/workflow`）

> 類別層級建議 `@Authenticated`；webhook 端點獨立 `@PermitAll`。回應一律 `ApiResponse<T>`。多數為 POST（對齊既有專案慣例）。

| # | HTTP | 路徑 | 用途 | Request 主要欄位 | Response 主要欄位 | RBAC |
|---|------|------|------|-----------------|------------------|------|
| 1 | POST | `/llm/workflow/save` | 新增或整張覆寫 workflow（含 nodes/edges） | `id?`, `name`, `description?`, `canvasMeta?`, `nodes[]`, `edges[]`, `triggers[]`, `version?`(樂觀鎖) | `WorkflowDTO`(含新 id、version) | `@Authenticated`（service 檢核擁有權；admin 可代管） |
| 2 | POST | `/llm/workflow/get` | 取得單一 workflow 完整定義 | `id` | `WorkflowDTO` + `nodes[]` + `edges[]` + `triggers[]` | `@Authenticated`（擁有者或 admin） |
| 3 | GET | `/llm/workflow/list` | 列出當前使用者的 workflow | （JWT userId） | `List<WorkflowSummaryDTO>` | `@Authenticated` |
| 4 | POST | `/llm/workflow/update` | 僅更新 meta（name/description/canvasMeta） | `id`, `name?`, `description?`, `canvasMeta?` | `WorkflowDTO` | `@Authenticated`（擁有者或 admin） |
| 5 | POST | `/llm/workflow/delete` | 刪除 workflow（連鎖刪 node/edge/trigger/execution） | `id` | success 訊息 | `@Authenticated`（擁有者或 admin） |
| 6 | POST | `/llm/workflow/switchStatus` | 啟用 / 停用 workflow | `id`, `active`(bool) | `WorkflowDTO` | `@Authenticated`（擁有者或 admin） |
| 7 | POST | `/llm/workflow/execute` | 手動執行（**實作為 SSE 事件流** `Multi<String>`，非表中的單一 DTO 回應） | `id`, `inputPayload?`(JSON), `triggerNodeKey?`（指定入口，見 §2.1.1；省略＝所有 TRIGGER 皆執行） | SSE：`execution.started` / `node.started` / `node.completed` / `node.failed` / `execution.completed` | `@Authenticated`（擁有者或 admin） |
| 8 | POST | `/llm/workflow/webhook/{token}` | Webhook 觸發（外部系統） | path `token`；body = inputPayload | 同步回傳執行結果（200） | `@PermitAll` + token 驗證 |
| 9 | POST | `/llm/workflow/trigger/save` | 新增 / 更新 trigger（webhook 產 token、cron 設表達式） | `workflowId`, `type`, `nodeKey?`, `cronExpression?`, `overlapPolicy?`, `enabled` | `WorkflowTriggerDTO`(含 webhookToken) | `@Authenticated`（擁有者）；**CRON 設定 `@RolesAllowed("admin")`** |
| 10 | POST | `/llm/workflow/trigger/delete` | 刪除 trigger | `triggerId` | success 訊息 | `@Authenticated`（擁有者）；CRON `@RolesAllowed("admin")` |
| 11 | POST | `/llm/workflow/execution/list` | 查詢執行紀錄列表（分頁/篩選） | `workflowId`, `status?`, `page?`, `size?` | `PageDTO<WorkflowExecutionSummaryDTO>` | `@Authenticated`（擁有者或 admin） |
| 12 | POST | `/llm/workflow/execution/get` | 查詢單次執行詳情（含各節點） | `executionId` | `WorkflowExecutionDTO` + `nodeExecutions[]` | `@Authenticated`（擁有者或 admin） |

> 備註：`save`（#1）負責「整張畫布無損覆寫」；`update`（#4）僅輕量改 meta，兩者分離可降低誤覆寫風險。**v1 webhook 一律同步回傳結果（200），不提供 202 非同步受理模式**（已拍板，見 requirements.md §7）。

### 5.1 主要 DTO 結構（`@Serializable`）

```kotlin
// WorkflowDTO.kt
@Serializable
data class WorkflowDTO(
    var id: String? = null,
    var name: String? = null,
    var description: String? = null,
    var status: WorkflowStatus? = null,
    var version: Int = 1,
    var canvasMeta: JsonElement? = null,           // 對應 json 欄位
    var nodes: List<WorkflowNodeDTO> = emptyList(),
    var edges: List<WorkflowEdgeDTO> = emptyList(),
    var triggers: List<WorkflowTriggerDTO> = emptyList()
)

@Serializable
data class WorkflowNodeDTO(
    var id: String? = null,
    var nodeKey: String,                            // 前端產生, workflow 內唯一
    var type: NodeType,
    var name: String? = null,
    var positionX: Double,
    var positionY: Double,
    var config: JsonElement                         // 各節點型別專屬 config
)

@Serializable
data class WorkflowEdgeDTO(
    var id: String? = null,
    var sourceNodeKey: String,
    var targetNodeKey: String,
    var sourceHandle: String? = null,
    var targetHandle: String? = null,
    var label: String? = null
)

@Serializable
data class WorkflowTriggerDTO(
    var id: String? = null,
    var type: TriggerType,
    var nodeKey: String? = null,
    var enabled: Boolean = true,
    var webhookToken: String? = null,               // 回應時可遮罩 / 僅擁有者可見
    var cronExpression: String? = null,
    var overlapPolicy: String? = "SKIP"
)

@Serializable
data class WorkflowExecutionDTO(
    var id: String? = null,
    var workflowId: String? = null,
    var status: ExecutionStatus? = null,
    var triggerType: TriggerType? = null,
    var triggeredBy: String? = null,
    var triggerNodeKey: String? = null,   // 發起本次執行的 TRIGGER 節點；null=未指定（見 §2.1.1）
    var inputPayload: JsonElement? = null,
    var outputResult: JsonElement? = null,
    var errorNodeKey: String? = null,
    var errorMessage: String? = null,
    var startedAt: String? = null,
    var finishedAt: String? = null,
    var durationMs: Long? = null,
    var nodeExecutions: List<NodeExecutionDTO> = emptyList()
)

@Serializable
data class NodeExecutionDTO(
    var nodeKey: String,
    var nodeType: NodeType,
    var seqNo: Int,
    var status: NodeExecutionStatus,
    var input: JsonElement? = null,
    var output: JsonElement? = null,
    var errorMessage: String? = null,
    var loopIndex: Int? = null,
    var durationMs: Long? = null
)
```

---

## 6. 資料型別建議（Kotlin / JPA，對齊 BaseEntity 風格）

| 欄位語意 | PostgreSQL | Kotlin / JPA 對應 | 備註 |
|---------|-----------|-------------------|------|
| 主鍵 id | `varchar(36)` | `var id: String? = null` + `@Id @GeneratedValue(strategy = GenerationType.UUID)` | 與既有 entity 完全一致 |
| 外鍵 / 關聯 id | `varchar(36)` | `var workflowId: String = ""` | 邏輯關聯，service 層維護一致性 |
| node_key | `varchar(64)` | `var nodeKey: String = ""` | workflow 內唯一 |
| 列舉（status/type） | `varchar(n)` | `@Enumerated(EnumType.STRING) var type: NodeType` | 對齊 `LLMSettingEntity.type` |
| JSON config / payload / output | `json` | `@JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="json") var config: SomeModel`（或泛用 `Map<String,Any?>` / `JsonNode`） | 對齊 `LLMSettingEntity.modelSetting`、`LLMMcpServerEntity.commandSetting` |
| 座標 | `double precision` | `var positionX: Double = 0.0` | Vue Flow 座標 |
| 版本（樂觀鎖） | `int` | `@Version var version: Int = 1`（或手動比對） | 防並發覆寫 |
| 時間戳 | `timestamp` | `LocalDateTime`（稽核欄位由 `BaseEntity` 提供） | 不重複宣告 created_at 等 |
| 耗時 | `bigint` | `var durationMs: Long? = null` | 毫秒 |
| 敏感欄位（HTTP Authorization 等） | `varchar` | `@Convert(converter = PasswordEncryptConverter::class)` | 對齊 `LLMSettingEntity.apiKey`（AES-GCM） |
| webhook_token | `varchar(64)` | `var webhookToken: String? = null` | UUID/隨機字串、UNIQUE |
| cron 表達式 | `varchar(120)` | `var cronExpression: String? = null` | 由 Quarkus Scheduler 解析 |

> Entity 一律 `class XxxEntity : BaseEntity()`、`@Entity @Table(name="llm_workflow_...")`；稽核欄位（created_at/by、updated_at/by）由 `BaseEntity` 的 `@PrePersist` / `@PreUpdate` 自動填入，**新表勿重複宣告**。

---

## 7. 執行引擎與排程器設計重點

- **WorkflowEngine（核心，同步）**：輸入 `(workflow, inputPayload, triggerContext)`，輸出落地的 `executionId`。內部：圖驗證 → 拓樸排序 → 序列走訪 → 變數插值 → 節點分派器（`NodeExecutor` 介面，每種 NodeType 一個實作）→ 逐節點 flush `node_execution` → 收斂 execution 終態。逾時以 `CompletableFuture` + `orTimeout(timeoutSeconds)` 或 Quarkus `@Timeout` 包裹整體執行。
- **NodeExecutor 分派**：建議 `interface NodeExecutor { fun supports(): NodeType; fun execute(ctx: NodeExecContext): NodeExecResult }`，以 CDI `@ApplicationScoped` 多實例注入，依 type 路由；LLM/Tool/MCP/RAG executor 直接委派既有 `LLMService` / `ToolService` / `McpServerService` / `EmbeddingService`。
- **變數插值上下文**：以 `Map<nodeKey, output>` 累積；`{{nodeKey.path}}` 以 JSON path 解析。
- **WorkflowCronScheduler**：Quarkus `@Scheduled`（或動態 `Scheduler` API）週期掃描 `llm_workflow_trigger`（type=CRON, enabled=true）並比對 cron；命中即於背景 worker 執行緒呼叫同一 `WorkflowEngine`。背景執行緒池與 HTTP worker 隔離（NFR-7）。重疊以 `overlap_policy=SKIP` 控制（查同 workflow 是否有 RUNNING execution）。

---

## 8. [LOGIC CONFLICT] 區塊

### [LOGIC CONFLICT-1] 同步執行語意 vs Cron 排程觸發（使用者已指定）

- **衝突**：v1 定義為「同步請求-回應式執行」，但 Cron 觸發沒有 HTTP 呼叫端等待回應，本質與「請求-回應」矛盾。
- **採用解法（對齊使用者拍板）**：**將「執行引擎」與「觸發來源」解耦。**
  - 引擎 `WorkflowEngine` 永遠是同步語意：一次呼叫把所有節點依拓樸順序跑到終態並落地。
  - 觸發來源只決定「誰啟動一次 execution」：
    - `MANUAL` / `WEBHOOK`：由 HTTP 請求執行緒直接呼叫引擎，呼叫端同步等待結果（v1 一律同步，無 202 受理模式）。
    - `CRON`：由 `WorkflowCronScheduler` 在背景執行緒呼叫同一引擎；無人等待回應，execution 狀態與 node_execution 全數落地，前端事後以 `/execution/list`、`/execution/get` 查詢。
  - ERD 以 `llm_workflow_trigger.type` + `llm_workflow_execution.trigger_type` / `triggered_by` 抽象化三種來源，引擎不感知來源差異。
- **效果**：同步語意一致、三種觸發共用同一套執行與紀錄，無需引入 MQ / 非同步事件（符合 v1 不做 N1）。

### [LOGIC CONFLICT-2] Code 節點執行 vs 系統安全（RCE 風險）

- **衝突**：Code 節點需執行使用者程式碼，等同遠端程式碼執行（RCE）風險，與平台安全要求衝突。
- **採用解法（已拍板）**：v1 完整設計資料模型與 config schema，但以 feature flag `workflow.node.code.enabled=false` **預設停用實際執行**，執行時回 `WORKFLOW_CODE_NODE_DISABLED`。沙箱技術選型（GraalVM Polyglot 受限 context、或子行程隔離 + CPU/記憶體/網路限制）延後至正式啟用時再決策（見 requirements.md §7）。

### [LOGIC CONFLICT-3] 同步逾時 vs 長流程（LLM / HTTP 串接）

- **衝突**：同步等待整條流程完成，但 LLM、外部 HTTP 可能很慢，HTTP 連線可能被前端 / 反向代理逾時切斷。
- **採用解法（已拍板）**：設定整體 `workflow.execution.timeout-seconds`（預設 300，可設定）；逾時落地 `TIMEOUT` 並中止。**v1 manual 與 webhook 一律同步等待結果，不提供 202 非同步受理模式**（`responseMode=ASYNC_ACCEPTED` 列入後續版本，見 requirements.md §7）。

### [LOGIC CONFLICT-4] save 整張覆寫 vs 並發編輯

- **衝突**：兩個使用者 / 分頁同時編輯同一 workflow，後存者整張覆寫前存者。
- **採用解法**：`llm_workflow.version` 作樂觀鎖；`save` 須帶當前 `version`，不一致回 `WORKFLOW_VERSION_CONFLICT`（code 409），要求重新載入。是否需完整版本控制列為開放問題 2。

---

## 9. 需新增的 i18n 訊息鍵（`AppMessage` + properties）

| Key | 類別 | 用途 |
|-----|------|------|
| `workflow.not.found` | WORKFLOW_NOT_FOUND | workflow 不存在 |
| `workflow.forbidden` | WORKFLOW_FORBIDDEN | 非擁有者存取 |
| `workflow.node.key.duplicated` | WORKFLOW_NODE_KEY_DUPLICATED | nodeKey 重複 |
| `workflow.edge.node.not.found` | WORKFLOW_EDGE_NODE_NOT_FOUND | edge 參照不存在的 node |
| `workflow.graph.has.cycle` | WORKFLOW_GRAPH_HAS_CYCLE | 圖含非法環 |
| `workflow.trigger.node.required` | WORKFLOW_TRIGGER_NODE_REQUIRED | 啟用需 trigger 節點 |
| `workflow.node.limit.exceeded` | WORKFLOW_NODE_LIMIT_EXCEEDED | 超過節點數上限 |
| `workflow.execution.timeout` | WORKFLOW_EXECUTION_TIMEOUT | 執行逾時 |
| `workflow.execution.not.found` | WORKFLOW_EXECUTION_NOT_FOUND | 執行紀錄不存在 |
| `workflow.inactive` | WORKFLOW_INACTIVE | workflow 已停用 |
| `workflow.webhook.token.invalid` | WORKFLOW_WEBHOOK_TOKEN_INVALID | webhook token 無效 |
| `workflow.cron.expression.invalid` | WORKFLOW_CRON_EXPRESSION_INVALID | cron 表達式非法 |
| `workflow.version.conflict` | WORKFLOW_VERSION_CONFLICT | 樂觀鎖版本衝突 |
| `workflow.variable.resolve.failed` | WORKFLOW_VARIABLE_RESOLVE_FAILED | 變數插值解析失敗 |
| `workflow.condition.eval.failed` | WORKFLOW_CONDITION_EVAL_FAILED | 條件評估失敗 |
| `workflow.loop.input.not.array` | WORKFLOW_LOOP_INPUT_NOT_ARRAY | Loop 輸入非陣列 |
| `workflow.loop.limit.exceeded` | WORKFLOW_LOOP_LIMIT_EXCEEDED | 超過迭代上限 |
| `workflow.http.request.failed` | WORKFLOW_HTTP_REQUEST_FAILED | HTTP 節點失敗 |
| `workflow.code.node.disabled` | WORKFLOW_CODE_NODE_DISABLED | Code 節點停用 |
| `workflow.delete.while.running` | WORKFLOW_DELETE_WHILE_RUNNING | 執行中不可刪除 |
| `workflow.llm.output.parse.failed` | WORKFLOW_LLM_OUTPUT_PARSE_FAILED | LLM 結構化輸出不符 outputSchema |
| `workflow.llm.file.not.found` | WORKFLOW_LLM_FILE_NOT_FOUND | LLM 節點引用的上傳檔案不存在 |
| `workflow.prompt.node.not.connected` | WORKFLOW_PROMPT_NODE_NOT_CONNECTED | 孤兒 PROMPT 節點（未連任何 LLM 的 `in:prompt`） |
| `workflow.llm.prompt.required` | WORKFLOW_LLM_PROMPT_REQUIRED | LLM 節點既無 `userPrompt` 也無 PROMPT 連入 |
| `workflow.trigger.node.not.found` | WORKFLOW_TRIGGER_NODE_NOT_FOUND | execute 指定的 `triggerNodeKey` 在圖上找不到（見 §2.1.1） |
| `workflow.trigger.node.invalid` | WORKFLOW_TRIGGER_NODE_INVALID | execute 指定的 nodeKey 存在但型別不是 TRIGGER |

---

## 10. 新增設定項（`application.properties`）

| 設定鍵 | 預設值 | 說明 |
|--------|--------|------|
| `workflow.execution.timeout-seconds` | `300` | 單次 execution 全程逾時上限 |
| `workflow.max-nodes` | `100` | 單一 workflow 節點數上限 |
| `workflow.max-loop-iterations` | `1000` | Loop 節點迭代上限 |
| `workflow.node.output.max-bytes` | `1048576` | 單節點 output JSON 上限（1MB） |
| `workflow.node.code.enabled` | `false` | Code 節點執行開關（v1 預設停用） |
| `workflow.cron.scan-interval` | `30s` | Cron 掃描間隔 |

> 上述設定異動需依 `documentation-update-policy` 同步至相關文件。
