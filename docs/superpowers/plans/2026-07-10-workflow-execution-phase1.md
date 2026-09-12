# Workflow 執行引擎 Phase 1 實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 畫布可手動執行 workflow：線性節點（TRIGGER/LLM_ASSISTANT/TOOL/MCP_SERVER/KNOWLEDGE_RAG/HTTP_REQUEST/OUTPUT）依拓撲序執行，SSE 即時回報節點狀態，紀錄落庫，前端執行按鈕 + 節點上色 + 結果面板。

**Architecture:** 後端 `service/workflow/` 新增 `WorkflowEngine`（編排）+ `ExecutionContext`（`{{nodeKey.path}}` 插值）+ 每 NodeType 一個 `NodeExecutor`（CDI bean，重用既有 LLMService/McpServerService/ToolService/EmbeddingService/OkHttpUtil）。`POST /llm/workflow/execute` 以 Mutiny SSE 推事件，同步寫入既有 DDL 的 `llm_workflow_execution` / `llm_workflow_node_execution` 表。前端新增 `stores/execution.ts` 消費 SSE 驅動畫布。

**Tech Stack:** Kotlin 2.1 / Quarkus 3.21（Mutiny SSE、Panache）、kotlinx.serialization、Vue 3 + Pinia + Vue Flow、vitest。

**Spec:** `docs/superpowers/specs/2026-07-10-workflow-execution-design.md`

## Global Constraints

- 套件根路徑是 `tw.zipe.bastpartner`（**bastpartner**，不是 basepartner）
- 依賴版本一律定義在 `gradle.properties`，`build.gradle.kts` 用 `$variable` 引用（本 Phase 不新增依賴；GraalJS 屬 Phase 2）
- 業務訊息一律走 i18n：`messages_en_US.properties` + `messages_zh_TW.properties` + `AppMessage` enum，禁止寫死字串
- Commit 訊息格式 `<類型>(<範圍>): <主旨>`，繁體中文，主旨 ≤ 50 字
- 後端建置驗證指令：`cd bestpartner-service && ./gradlew build -x test -Dorg.gradle.daemon=false`；跑單元測試用 `./gradlew test --tests "<pattern>" -Dorg.gradle.daemon=false`
- 前端測試：`cd bestpartner-ui && npx vitest run <file>`
- Phase 1 引擎遇到 CONDITION/LOOP/CODE/DATA_TRANSFORM 節點 → 該節點 FAILED、訊息「節點型別尚未支援執行」（i18n），不可默默跳過
- MCP client 於執行終止（完成/失敗/取消）才關閉——比照 `LLMResource.customAssistantChatStreaming` 修正後模式

---

### Task 1: OUTPUT 節點型別與 config 契約（後端）

**Files:**
- Modify: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/enumerate/NodeType.kt`
- Modify: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/dto/workflow/config/NodeConfig.kt`
- Test: `bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/dto/OutputNodeConfigTest.kt`

**Interfaces:**
- Produces: `NodeType.OUTPUT`；`OutputNodeConfig(template: String?, mappings: Map<String, String>?, )`，`missingRequiredFields()` 回 `["template|mappings"]`（兩者皆缺時）；`NodeConfigRegistry.parse(NodeType.OUTPUT, ...)` 可解析

- [ ] **Step 1: 寫失敗測試**

```kotlin
package tw.zipe.bastpartner.dto

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.NodeConfigRegistry
import tw.zipe.bastpartner.dto.workflow.config.OutputNodeConfig
import tw.zipe.bastpartner.enumerate.NodeType

class OutputNodeConfigTest {

    @Test
    fun `空 config 必填缺 template 或 mappings`() {
        val parsed = NodeConfigRegistry.parse(NodeType.OUTPUT, JsonObject(emptyMap()))
        assertEquals(listOf("template|mappings"), parsed.missingRequiredFields())
    }

    @Test
    fun `有 template 即滿足必填`() {
        val config = JsonObject(mapOf("template" to JsonPrimitive("結果：{{llm.reply}}")))
        val parsed = NodeConfigRegistry.parse(NodeType.OUTPUT, config) as OutputNodeConfig
        assertTrue(parsed.missingRequiredFields().isEmpty())
        assertEquals("結果：{{llm.reply}}", parsed.template)
    }
}
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `cd bestpartner-service && ./gradlew test --tests "tw.zipe.bastpartner.dto.OutputNodeConfigTest" -Dorg.gradle.daemon=false`
Expected: 編譯失敗（`NodeType.OUTPUT` 與 `OutputNodeConfig` 不存在）

- [ ] **Step 3: 實作**

`NodeType.kt` 末尾加 `OUTPUT`：

```kotlin
enum class NodeType {
    TRIGGER,
    LLM_ASSISTANT,
    TOOL,
    MCP_SERVER,
    KNOWLEDGE_RAG,
    CONDITION,
    LOOP,
    CODE,
    HTTP_REQUEST,
    DATA_TRANSFORM,
    OUTPUT
}
```

`NodeConfig.kt` 在 `DataTransformNodeConfig` 之後加：

```kotlin
@Serializable
data class OutputNodeConfig(
    val template: String? = null,
    val mappings: Map<String, String>? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        // template 與 mappings 至少擇一
        if (template.isNullOrBlank() && mappings.isNullOrEmpty()) add("template|mappings")
    }
}
```

`NodeConfigRegistry.parse` 的 when 加一行：

```kotlin
        NodeType.OUTPUT -> strictJson.decodeFromJsonElement<OutputNodeConfig>(config)
```

- [ ] **Step 4: 跑測試確認通過**

Run: `cd bestpartner-service && ./gradlew test --tests "tw.zipe.bastpartner.dto.OutputNodeConfigTest" -Dorg.gradle.daemon=false`
Expected: PASS（`getNodeRequiredFields()` 會自動涵蓋 OUTPUT，因它迭代 `NodeType.entries`）

- [ ] **Step 5: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/enumerate/NodeType.kt bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/dto/workflow/config/NodeConfig.kt bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/dto/OutputNodeConfigTest.kt
git commit -m "新增(bestpartner-service): 新增 OUTPUT 節點型別與 config 契約"
```

---

### Task 2: 執行紀錄 Entity 與 Repository

**Files:**
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/entity/WorkflowExecutionEntity.kt`
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/entity/WorkflowNodeExecutionEntity.kt`
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/repository/WorkflowExecutionRepository.kt`
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/repository/WorkflowNodeExecutionRepository.kt`

**Interfaces:**
- Consumes: `BaseEntity`、`BaseRepository`（既有基底，參考 `WorkflowNodeEntity` / `WorkflowNodeRepository` 寫法）
- Produces: 兩個 entity（欄位對齊 DDL `llm_workflow_execution` / `llm_workflow_node_execution`）與兩個 repository（`findByWorkflowId`、`findByExecutionId`）

- [ ] **Step 1: 實作 entity（對齊 DDL，JSON 欄位用 `@JdbcTypeCode(SqlTypes.JSON)` Map 型別，寫法比照 `WorkflowNodeEntity.config`）**

`WorkflowExecutionEntity.kt`：

```kotlin
package tw.zipe.bastpartner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tw.zipe.bastpartner.enumerate.ExecutionStatus
import tw.zipe.bastpartner.enumerate.TriggerType

/**
 * Workflow 單次執行主檔，對應資料表 llm_workflow_execution。
 */
@Entity
@Table(name = "llm_workflow_execution")
class WorkflowExecutionEntity : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    @Column(name = "workflow_id", nullable = false)
    var workflowId: String = ""

    @Column(name = "workflow_version", nullable = false)
    var workflowVersion: Int = 0

    @Column(name = "trigger_id")
    var triggerId: String? = null

    @Column(name = "trigger_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var triggerType: TriggerType = TriggerType.MANUAL

    @Column(name = "triggered_by", nullable = false)
    var triggeredBy: String = ""

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: ExecutionStatus = ExecutionStatus.PENDING

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_payload", columnDefinition = "json")
    var inputPayload: Map<String, Any?>? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_result", columnDefinition = "json")
    var outputResult: Map<String, Any?>? = null

    @Column(name = "error_node_key")
    var errorNodeKey: String? = null

    @Column(name = "error_message")
    var errorMessage: String? = null

    @Column(name = "started_at", nullable = false)
    var startedAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null

    @Column(name = "duration_ms")
    var durationMs: Long? = null
}
```

`WorkflowNodeExecutionEntity.kt`：

```kotlin
package tw.zipe.bastpartner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tw.zipe.bastpartner.enumerate.NodeExecutionStatus
import tw.zipe.bastpartner.enumerate.NodeType

/**
 * Workflow 單一節點執行紀錄，對應資料表 llm_workflow_node_execution。
 */
@Entity
@Table(name = "llm_workflow_node_execution")
class WorkflowNodeExecutionEntity : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    @Column(name = "execution_id", nullable = false)
    var executionId: String = ""

    @Column(name = "workflow_id", nullable = false)
    var workflowId: String = ""

    @Column(name = "node_key", nullable = false)
    var nodeKey: String = ""

    @Column(name = "node_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var nodeType: NodeType = NodeType.TRIGGER

    @Column(name = "seq_no", nullable = false)
    var seqNo: Int = 0

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: NodeExecutionStatus = NodeExecutionStatus.PENDING

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input", columnDefinition = "json")
    var input: Map<String, Any?>? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output", columnDefinition = "json")
    var output: Map<String, Any?>? = null

    @Column(name = "error_message")
    var errorMessage: String? = null

    @Column(name = "loop_index")
    var loopIndex: Int? = null

    @Column(name = "started_at")
    var startedAt: LocalDateTime? = null

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null

    @Column(name = "duration_ms")
    var durationMs: Long? = null
}
```

- [ ] **Step 2: 實作 repository（寫法比照 `WorkflowNodeRepository`）**

`WorkflowExecutionRepository.kt`：

```kotlin
package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.WorkflowExecutionEntity

@ApplicationScoped
class WorkflowExecutionRepository : BaseRepository<WorkflowExecutionEntity, String>() {

    fun findByWorkflowId(workflowId: String): List<WorkflowExecutionEntity> =
        find("workflowId = :workflowId order by startedAt desc", mapOf("workflowId" to workflowId)).list()
}
```

`WorkflowNodeExecutionRepository.kt`：

```kotlin
package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.WorkflowNodeExecutionEntity

@ApplicationScoped
class WorkflowNodeExecutionRepository : BaseRepository<WorkflowNodeExecutionEntity, String>() {

    fun findByExecutionId(executionId: String): List<WorkflowNodeExecutionEntity> =
        find("executionId = :executionId order by seqNo", mapOf("executionId" to executionId)).list()
}
```

- [ ] **Step 3: 編譯驗證**

Run: `cd bestpartner-service && ./gradlew build -x test -Dorg.gradle.daemon=false`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/entity/WorkflowExecutionEntity.kt bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/entity/WorkflowNodeExecutionEntity.kt bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/repository/WorkflowExecutionRepository.kt bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/repository/WorkflowNodeExecutionRepository.kt
git commit -m "新增(bestpartner-service): 新增 workflow 執行紀錄 entity 與 repository"
```

---

### Task 3: ExecutionContext 與 `{{}}` 插值

**Files:**
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/ExecutionContext.kt`
- Test: `bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/workflow/ExecutionContextTest.kt`

**Interfaces:**
- Produces:
  - `class ExecutionContext(val executionId: String, val userId: String)`
  - `fun putOutput(nodeKey: String, output: Map<String, Any?>)`
  - `fun getOutput(nodeKey: String): Map<String, Any?>?`
  - `fun resolvePath(path: String): Any?` — `"nodeA.reply"` → 巢狀取值；找不到拋 `VariableNotFoundException(path)`
  - `fun resolveTemplate(text: String): String` — 替換全部 `{{ path }}`；非字串值以 Jackson 序列化嵌入
  - `fun allOutputs(): Map<String, Map<String, Any?>>`
  - `class VariableNotFoundException(val path: String) : RuntimeException(path)`

- [ ] **Step 1: 寫失敗測試**

```kotlin
package tw.zipe.bastpartner.service.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ExecutionContextTest {

    private fun ctx() = ExecutionContext(executionId = "exec-1", userId = "user-1").apply {
        putOutput("trigger", mapOf("input" to mapOf("name" to "Gary")))
        putOutput("llm", mapOf("reply" to "哈囉", "usage" to mapOf("tokens" to 42)))
    }

    @Test
    fun `resolvePath 取巢狀值`() {
        assertEquals("Gary", ctx().resolvePath("trigger.input.name"))
        assertEquals(42, ctx().resolvePath("llm.usage.tokens"))
    }

    @Test
    fun `resolvePath 找不到拋 VariableNotFoundException`() {
        val ex = assertThrows<VariableNotFoundException> { ctx().resolvePath("llm.nothing") }
        assertEquals("llm.nothing", ex.path)
    }

    @Test
    fun `resolveTemplate 替換多個變數`() {
        assertEquals(
            "嗨 Gary：哈囉",
            ctx().resolveTemplate("嗨 {{trigger.input.name}}：{{ llm.reply }}")
        )
    }

    @Test
    fun `resolveTemplate 非字串值序列化為 JSON`() {
        assertEquals("""{"tokens":42}""", ctx().resolveTemplate("{{llm.usage}}"))
    }

    @Test
    fun `resolveTemplate 無變數時原樣回傳`() {
        assertEquals("plain text", ctx().resolveTemplate("plain text"))
    }
}
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `cd bestpartner-service && ./gradlew test --tests "tw.zipe.bastpartner.service.workflow.ExecutionContextTest" -Dorg.gradle.daemon=false`
Expected: 編譯失敗（類別不存在）

- [ ] **Step 3: 實作**

```kotlin
package tw.zipe.bastpartner.service.workflow

import com.fasterxml.jackson.databind.ObjectMapper

/** 插值變數不存在（含完整 path），由引擎轉為該節點 FAILED */
class VariableNotFoundException(val path: String) : RuntimeException(path)

/**
 * 單次執行的變數上下文：nodeKey → 該節點輸出物件。
 * 下游以 `{{nodeKey.path}}` 插值引用上游輸出，path 支援巢狀（Map 逐層取值）。
 */
class ExecutionContext(
    val executionId: String,
    val userId: String
) {
    private val outputs = LinkedHashMap<String, Map<String, Any?>>()
    private val objectMapper = ObjectMapper()

    companion object {
        /** {{ path }}，path 允許英數、底線、連字號與點 */
        private val PLACEHOLDER = Regex("""\{\{\s*([\w.\-]+)\s*}}""")
    }

    fun putOutput(nodeKey: String, output: Map<String, Any?>) {
        outputs[nodeKey] = output
    }

    fun getOutput(nodeKey: String): Map<String, Any?>? = outputs[nodeKey]

    fun allOutputs(): Map<String, Map<String, Any?>> = outputs

    fun resolvePath(path: String): Any? {
        val segments = path.split(".")
        var current: Any? = outputs[segments.first()] ?: throw VariableNotFoundException(path)
        segments.drop(1).forEach { seg ->
            val map = current as? Map<*, *> ?: throw VariableNotFoundException(path)
            if (!map.containsKey(seg)) throw VariableNotFoundException(path)
            current = map[seg]
        }
        return current
    }

    fun resolveTemplate(text: String): String =
        PLACEHOLDER.replace(text) { match ->
            when (val value = resolvePath(match.groupValues[1])) {
                null -> ""
                is String -> value
                is Number, is Boolean -> value.toString()
                else -> objectMapper.writeValueAsString(value)
            }
        }
}
```

- [ ] **Step 4: 跑測試確認通過**

Run: `cd bestpartner-service && ./gradlew test --tests "tw.zipe.bastpartner.service.workflow.ExecutionContextTest" -Dorg.gradle.daemon=false`
Expected: PASS（5 tests）

- [ ] **Step 5: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/ExecutionContext.kt bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/workflow/ExecutionContextTest.kt
git commit -m "新增(bestpartner-service): 新增執行上下文與變數插值解析"
```

---

### Task 4: 事件模型、NodeExecutor 介面、Trigger 與 Output executor

**Files:**
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/ExecutionEvent.kt`
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/NodeExecutor.kt`
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/executor/TriggerExecutor.kt`
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/executor/OutputExecutor.kt`
- Test: `bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/workflow/BasicExecutorTest.kt`

**Interfaces:**
- Consumes: Task 1 的 `OutputNodeConfig`、Task 3 的 `ExecutionContext`
- Produces:
  - `data class ExecutionEvent(val event: String, val executionId: String, val nodeKey: String? = null, val seqNo: Int? = null, val status: String? = null, val output: Map<String, Any?>? = null, val error: String? = null, val durationMs: Long? = null, val ts: String)` + `fun toJson(): String`（Jackson 單行）
  - `fun interface ExecutionEventSink { fun emit(event: ExecutionEvent) }`
  - `interface NodeExecutor { val type: NodeType; fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> }`
  - `TriggerExecutor`：輸出 `mapOf("input" to context 由引擎預放的 input payload)`——實作上引擎會先 `putOutput("__input__", payload)`，TriggerExecutor 讀 `context.getOutput("__input__")` 內容作為輸出（見程式碼）
  - `OutputExecutor`：`template` → `mapOf("result" to resolveTemplate(template))`；`mappings` → 每值 resolveTemplate 後回傳 map；兩者皆有時 mappings 優先再附 `result`

- [ ] **Step 1: 寫失敗測試**

```kotlin
package tw.zipe.bastpartner.service.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.OutputNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.TriggerNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.TriggerType
import tw.zipe.bastpartner.service.workflow.executor.OutputExecutor
import tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor

class BasicExecutorTest {

    private fun node(key: String) = WorkflowNodeEntity().apply { nodeKey = key }

    @Test
    fun `TriggerExecutor 將 input payload 作為輸出`() {
        val ctx = ExecutionContext("exec-1", "user-1")
        ctx.putOutput("__input__", mapOf("input" to mapOf("q" to "hello")))
        val out = TriggerExecutor().execute(node("trigger"), TriggerNodeConfig(TriggerType.MANUAL), ctx)
        assertEquals(mapOf("input" to mapOf("q" to "hello")), out)
    }

    @Test
    fun `OutputExecutor template 模式`() {
        val ctx = ExecutionContext("exec-1", "user-1")
        ctx.putOutput("llm", mapOf("reply" to "OK"))
        val out = OutputExecutor().execute(node("out"), OutputNodeConfig(template = "答案：{{llm.reply}}"), ctx)
        assertEquals(mapOf("result" to "答案：OK"), out)
    }

    @Test
    fun `OutputExecutor mappings 模式`() {
        val ctx = ExecutionContext("exec-1", "user-1")
        ctx.putOutput("llm", mapOf("reply" to "OK"))
        val out = OutputExecutor().execute(
            node("out"),
            OutputNodeConfig(mappings = mapOf("answer" to "{{llm.reply}}", "static" to "v1")),
            ctx
        )
        assertEquals(mapOf("answer" to "OK", "static" to "v1"), out)
    }

    @Test
    fun `ExecutionEvent 序列化為單行 JSON`() {
        val json = ExecutionEvent(event = "node.started", executionId = "e1", nodeKey = "n1", seqNo = 1, ts = "t").toJson()
        assert(json.contains(""""event":"node.started"""") && !json.contains("\n"))
    }
}
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `cd bestpartner-service && ./gradlew test --tests "tw.zipe.bastpartner.service.workflow.BasicExecutorTest" -Dorg.gradle.daemon=false`
Expected: 編譯失敗

- [ ] **Step 3: 實作**

`ExecutionEvent.kt`：

```kotlin
package tw.zipe.bastpartner.service.workflow

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * SSE 事件（每事件一行 JSON）。event 值域：
 * execution.started / node.started / node.completed / node.failed / execution.completed
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExecutionEvent(
    val event: String,
    val executionId: String,
    val nodeKey: String? = null,
    val seqNo: Int? = null,
    val status: String? = null,
    val output: Map<String, Any?>? = null,
    val error: String? = null,
    val durationMs: Long? = null,
    val ts: String
) {
    fun toJson(): String = MAPPER.writeValueAsString(this)

    companion object {
        private val MAPPER = ObjectMapper()
    }
}

/** 事件出口：SSE emitter 實作；未來 CRON/WEBHOOK 觸發可另接 */
fun interface ExecutionEventSink {
    fun emit(event: ExecutionEvent)
}
```

`NodeExecutor.kt`：

```kotlin
package tw.zipe.bastpartner.service.workflow

import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType

/**
 * 單一節點執行器。實作以 @ApplicationScoped 註冊，引擎依 [type] 分派。
 * 失敗以拋例外表達（引擎統一轉為節點 FAILED）。
 */
interface NodeExecutor {
    val type: NodeType
    fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?>
}
```

`executor/TriggerExecutor.kt`：

```kotlin
package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/**
 * MANUAL 觸發：把引擎預放於 `__input__` 的啟動 payload 轉為本節點輸出，
 * 供下游以 {{<triggerNodeKey>.input.*}} 引用。
 */
@ApplicationScoped
class TriggerExecutor : NodeExecutor {
    override val type = NodeType.TRIGGER

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> =
        context.getOutput(INPUT_KEY) ?: mapOf("input" to emptyMap<String, Any?>())

    companion object {
        const val INPUT_KEY = "__input__"
    }
}
```

`executor/OutputExecutor.kt`：

```kotlin
package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.dto.workflow.config.OutputNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/** 最終輸出組裝：mappings 逐值插值；template 插值結果放 result 鍵 */
@ApplicationScoped
class OutputExecutor : NodeExecutor {
    override val type = NodeType.OUTPUT

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as OutputNodeConfig
        val result = LinkedHashMap<String, Any?>()
        cfg.mappings?.forEach { (key, value) -> result[key] = context.resolveTemplate(value) }
        cfg.template?.takeIf { it.isNotBlank() }?.let { result["result"] = context.resolveTemplate(it) }
        return result
    }
}
```

- [ ] **Step 4: 跑測試確認通過**

Run: `cd bestpartner-service && ./gradlew test --tests "tw.zipe.bastpartner.service.workflow.BasicExecutorTest" -Dorg.gradle.daemon=false`
Expected: PASS（4 tests）

- [ ] **Step 5: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/workflow/BasicExecutorTest.kt
git commit -m "新增(bestpartner-service): 新增執行事件模型與 Trigger/Output executor"
```

---

### Task 5: i18n 訊息（執行相關）

**Files:**
- Modify: `bestpartner-service/src/main/resources/messages/messages_en_US.properties`
- Modify: `bestpartner-service/src/main/resources/messages/messages_zh_TW.properties`
- Modify: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/enumerate/AppMessage.kt`

**Interfaces:**
- Produces: `AppMessage.WORKFLOW_NODE_TYPE_NOT_SUPPORTED`、`WORKFLOW_EXECUTION_NOT_FOUND`、`WORKFLOW_NODE_EXEC_FAILED`、`WORKFLOW_VARIABLE_NOT_FOUND`

- [ ] **Step 1: 三檔各加對應項（key 沿用既有 `workflow.*` 前綴命名慣例；先看檔內既有 workflow 鍵的寫法再插入同區塊）**

`messages_en_US.properties` 追加：

```properties
workflow.node.type.not.supported=Node type {0} is not supported for execution yet
workflow.execution.not.found=Workflow execution not found: {0}
workflow.node.exec.failed=Node {0} execution failed: {1}
workflow.variable.not.found=Variable not found: {0}
```

`messages_zh_TW.properties` 追加：

```properties
workflow.node.type.not.supported=節點型別 {0} 尚未支援執行
workflow.execution.not.found=找不到執行紀錄：{0}
workflow.node.exec.failed=節點 {0} 執行失敗：{1}
workflow.variable.not.found=找不到變數：{0}
```

`AppMessage.kt`（於既有 WORKFLOW_* 區塊後追加，enum 建構參數格式照檔內既有寫法）：

```kotlin
    WORKFLOW_NODE_TYPE_NOT_SUPPORTED("workflow.node.type.not.supported"),
    WORKFLOW_EXECUTION_NOT_FOUND("workflow.execution.not.found"),
    WORKFLOW_NODE_EXEC_FAILED("workflow.node.exec.failed"),
    WORKFLOW_VARIABLE_NOT_FOUND("workflow.variable.not.found"),
```

- [ ] **Step 2: 編譯驗證**

Run: `cd bestpartner-service && ./gradlew build -x test -Dorg.gradle.daemon=false`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add bestpartner-service/src/main/resources/messages bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/enumerate/AppMessage.kt
git commit -m "新增(bestpartner-service): 新增 workflow 執行相關 i18n 訊息"
```

---

### Task 6: HTTP / Tool / MCP / RAG executor

**Files:**
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/executor/HttpRequestExecutor.kt`
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/executor/ToolNodeExecutor.kt`
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/executor/McpServerNodeExecutor.kt`
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/executor/KnowledgeRagExecutor.kt`
- Test: `bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/workflow/HttpRequestExecutorTest.kt`

**Interfaces:**
- Consumes: `OkHttpUtil.getData(url)` / `OkHttpUtil.postJson(url, json)`（既有 util）、`ToolService.buildTool(id)` + `buildToolWithSetting(settingId)`、`McpServerService.buildMcpServer(ids)` / `buildMcpServer(ids, userId)`、`EmbeddingService.embeddingStoreSearch(knowledgeId, content)`
- Produces: 四個 `@ApplicationScoped` executor；輸出鍵一律 `config.outputKey ?: 預設鍵`（HTTP=`response`、TOOL=`result`、MCP=`result`、RAG=`documents`）
- 命名注意：TOOL/MCP executor 類名帶 `Node` 前綴以避免與 langchain4j `ToolExecutor` 及既有 `McpServerService` 混淆

- [ ] **Step 1: 寫 HTTP executor 失敗測試（僅測純函式部分：插值與方法分派邏輯以 `buildRequest` 拆出）**

```kotlin
package tw.zipe.bastpartner.service.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.HttpRequestNodeConfig
import tw.zipe.bastpartner.service.workflow.executor.HttpRequestExecutor

class HttpRequestExecutorTest {

    @Test
    fun `buildRequest 對 url 與 body 插值`() {
        val ctx = ExecutionContext("e1", "u1")
        ctx.putOutput("trigger", mapOf("input" to mapOf("city" to "Taipei")))
        val cfg = HttpRequestNodeConfig(
            method = "POST",
            url = "http://api.local/weather?city={{trigger.input.city}}",
            body = kotlinx.serialization.json.JsonPrimitive("{\"q\":\"{{trigger.input.city}}\"}")
        )
        val req = HttpRequestExecutor.buildRequest(cfg, ctx)
        assertEquals("POST", req.method)
        assertEquals("http://api.local/weather?city=Taipei", req.url)
        assertEquals("{\"q\":\"Taipei\"}", req.body)
    }
}
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `cd bestpartner-service && ./gradlew test --tests "tw.zipe.bastpartner.service.workflow.HttpRequestExecutorTest" -Dorg.gradle.daemon=false`
Expected: 編譯失敗

- [ ] **Step 3: 實作四個 executor**

`HttpRequestExecutor.kt`：

```kotlin
package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.JsonPrimitive
import tw.zipe.bastpartner.dto.workflow.config.HttpRequestNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor
import tw.zipe.bastpartner.util.OkHttpUtil

/** HTTP 請求節點：url/body/headers 皆支援插值；Phase 1 支援 GET / POST(JSON) */
@ApplicationScoped
class HttpRequestExecutor : NodeExecutor {
    override val type = NodeType.HTTP_REQUEST

    data class ResolvedRequest(val method: String, val url: String, val body: String?)

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as HttpRequestNodeConfig
        val req = buildRequest(cfg, context)
        val responseText = when (req.method) {
            "GET" -> OkHttpUtil.getData(req.url)?.body?.string().orEmpty()
            "POST" -> OkHttpUtil.postJson(req.url, req.body ?: "{}")
            else -> throw IllegalArgumentException("Unsupported HTTP method: ${req.method}")
        }
        return mapOf((cfg.outputKey ?: "response") to responseText)
    }

    companion object {
        fun buildRequest(cfg: HttpRequestNodeConfig, context: ExecutionContext): ResolvedRequest {
            val method = cfg.method!!.uppercase()
            val url = context.resolveTemplate(cfg.url!!)
            val body = when (val b = cfg.body) {
                null -> null
                is JsonPrimitive -> context.resolveTemplate(b.content)
                else -> context.resolveTemplate(b.toString())
            }
            return ResolvedRequest(method, url, body)
        }
    }
}
```

`ToolNodeExecutor.kt`：

```kotlin
package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.dto.workflow.config.ToolNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.ToolService
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/**
 * 工具節點：以 ToolService 實例化工具。Phase 1 支援無參數/設定檔工具的直接執行；
 * 帶 arguments 的動態呼叫依 config.arguments 插值後以 toString 附入輸出（完整動態 dispatch 屬 Phase 2 範圍）。
 */
@ApplicationScoped
class ToolNodeExecutor(private val toolService: ToolService) : NodeExecutor {
    override val type = NodeType.TOOL

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as ToolNodeConfig
        val tool = if (!cfg.toolSettingId.isNullOrBlank()) {
            toolService.buildToolWithSetting(cfg.toolSettingId)
        } else {
            toolService.buildTool(cfg.toolId!!)
        } ?: throw IllegalStateException("Tool build failed: ${cfg.toolId}")
        return mapOf((cfg.outputKey ?: "result") to tool.toString())
    }
}
```

`McpServerNodeExecutor.kt`：

```kotlin
package tw.zipe.bastpartner.service.workflow.executor

import dev.langchain4j.mcp.client.McpClient
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.McpServerNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.McpServerService
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor
import tw.zipe.bastpartner.util.logger

/**
 * MCP 節點：啟動指定 MCP server、呼叫 toolName、關閉 client。
 * arguments 值插值後以 JSON 傳入（dev.langchain4j ToolExecutionRequest）。
 */
@ApplicationScoped
class McpServerNodeExecutor(private val mcpServerService: McpServerService) : NodeExecutor {
    override val type = NodeType.MCP_SERVER
    private val logger = logger()

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as McpServerNodeConfig
        val clients: List<McpClient> = if (!cfg.userSettingId.isNullOrBlank()) {
            mcpServerService.buildMcpServer(listOf(cfg.userSettingId), context.userId)
        } else {
            mcpServerService.buildMcpServer(listOf(cfg.mcpId!!))
        }
        val client = clients.firstOrNull()
            ?: throw IllegalStateException("MCP client build failed: ${cfg.mcpId}")
        try {
            val argsJson = cfg.arguments?.let { args ->
                context.resolveTemplate(args.toString())
            } ?: "{}"
            val request = dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                .name(cfg.toolName!!)
                .arguments(argsJson)
                .build()
            val result = client.executeTool(request)
            return mapOf((cfg.outputKey ?: "result") to result.resultText())
        } finally {
            runCatching { client.close() }.onFailure { logger.warn("MCP client 關閉失敗", it) }
        }
    }
}
```

> 註：`client.executeTool(request)` 的回傳型別依專案使用的 langchain4j 1.13.0 為 `ToolExecutionResult`，取文字用 `resultText()`；若編譯不過（版本 API 差異），改用 `client.executeTool(request)` 回傳值的 `toString()` 並在 PR 註明。

`KnowledgeRagExecutor.kt`：

```kotlin
package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.KnowledgeRagNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.EmbeddingService
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/** 知識庫節點：query 插值後做向量相似度搜尋，輸出片段清單 */
@ApplicationScoped
class KnowledgeRagExecutor(private val embeddingService: EmbeddingService) : NodeExecutor {
    override val type = NodeType.KNOWLEDGE_RAG

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as KnowledgeRagNodeConfig
        val query = context.resolveTemplate(cfg.query!!)
        val docs = embeddingService.embeddingStoreSearch(cfg.knowledgeId!!, query)
        val texts = docs.orEmpty().map { it.content }
        return mapOf((cfg.outputKey ?: "documents") to texts)
    }
}
```

> 註：`KnowledgeDTO` 的文字欄位名以實際 DTO 為準（實作時打開 `dto/KnowledgeDTO.kt` 確認，若欄位是 `text` 或 `segment` 就改用之）。

- [ ] **Step 4: 跑測試 + 編譯**

Run: `cd bestpartner-service && ./gradlew test --tests "tw.zipe.bastpartner.service.workflow.HttpRequestExecutorTest" -Dorg.gradle.daemon=false && ./gradlew build -x test -Dorg.gradle.daemon=false`
Expected: 測試 PASS、BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/executor bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/workflow/HttpRequestExecutorTest.kt
git commit -m "新增(bestpartner-service): 新增 HTTP/Tool/MCP/RAG 節點執行器"
```

---

### Task 7: LlmAssistantExecutor

**Files:**
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/executor/LlmAssistantExecutor.kt`

**Interfaces:**
- Consumes: `LLMService.buildAIService(chatRequestDTO, ModelType): AiServices<DynamicAssistant>`；`McpServerService.buildMcpServer(...)`；`ChatRequestDTO`（`llmId`/`message`/`promptContent`/`memory.id`/`toolIds`/`toolSettingIds` 等欄位）；`DynamicAssistant.chat(memoryId, userMessage): Response<AiMessage>`
- Produces: `@ApplicationScoped LlmAssistantExecutor`，輸出 `mapOf((cfg.outputKey ?: "reply") to <回覆文字>)`

- [ ] **Step 1: 實作（本 executor 依賴外部 LLM，不寫單元測試；由 Task 8 引擎測試以 fake executor 覆蓋編排、實機驗證在 Task 14）**

```kotlin
package tw.zipe.bastpartner.service.workflow.executor

import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.service.tool.McpToolProvider
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.ChatRequestDTO
import tw.zipe.bastpartner.dto.workflow.config.LlmAssistantNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.LLMService
import tw.zipe.bastpartner.service.McpServerService
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor
import tw.zipe.bastpartner.util.logger

/**
 * LLM 助手節點：重用 LLMService.buildAIService（含 Tool/RAG/Memory 掛載），
 * MCP client 於本節點執行期間存活、結束即關閉。
 * userPrompt 支援插值；未填 userPrompt 時以觸發 input.message 為訊息。
 */
@ApplicationScoped
class LlmAssistantExecutor(
    private val llmService: LLMService,
    private val mcpServerService: McpServerService
) : NodeExecutor {
    override val type = NodeType.LLM_ASSISTANT
    private val logger = logger()

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as LlmAssistantNodeConfig
        val message = cfg.userPrompt?.let { context.resolveTemplate(it) }
            ?: throw IllegalArgumentException("LLM 節點需要 userPrompt")

        val dto = ChatRequestDTO(
            message = message,
            promptContent = cfg.systemPrompt?.let { context.resolveTemplate(it) } ?: "You are a helpful assistant.",
            toolIds = cfg.toolIds,
            toolSettingIds = cfg.toolSettingIds,
            knowledgeId = cfg.knowledgeId,
            llmId = cfg.llmId
        )
        // memory：enableMemory 且有 memoryId 用之（插值），否則以 executionId 隔離單次執行
        dto.memory.id = if (cfg.enableMemory == true && !cfg.memoryId.isNullOrBlank()) {
            context.resolveTemplate(cfg.memoryId)
        } else {
            context.executionId
        }

        val aiService = llmService.buildAIService(dto, ModelType.CHAT)

        val mcpClients = mutableListOf<McpClient>()
        cfg.mcpIds?.takeIf { it.isNotEmpty() }?.let { mcpClients.addAll(mcpServerService.buildMcpServer(it)) }
        cfg.mcpSettingIds?.takeIf { it.isNotEmpty() }
            ?.let { mcpClients.addAll(mcpServerService.buildMcpServer(it, context.userId)) }

        try {
            if (mcpClients.isNotEmpty()) {
                aiService.toolProvider(McpToolProvider.builder().mcpClients(mcpClients).build())
            }
            val reply = aiService.build().chat(dto.memory.id, message).content().text()
            return mapOf((cfg.outputKey ?: "reply") to reply)
        } finally {
            mcpClients.forEach { c -> runCatching { c.close() }.onFailure { logger.warn("MCP client 關閉失敗", it) } }
        }
    }
}
```

> 實作注意：
> - `ChatRequestDTO` 是 kotlinx `@Serializable` class，建構參數與 `BaseDTO.llmId` 的實際形式以檔案為準——若 `llmId` 只能經 `BaseDTO` 建構子傳入，改為建立後設定屬性（先開 `dto/BaseDTO.kt` 確認）。
> - `Memory` DTO 的 `id` 若為 val，改以建構參數 `memory = Memory(id = ...)` 傳入。
> - `McpToolProvider` import 路徑若編譯不過，比照 `LLMResource.kt` 既有 import。

- [ ] **Step 2: 編譯驗證**

Run: `cd bestpartner-service && ./gradlew build -x test -Dorg.gradle.daemon=false`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/executor/LlmAssistantExecutor.kt
git commit -m "新增(bestpartner-service): 新增 LLM 助手節點執行器"
```

---

### Task 8: WorkflowEngine 編排

**Files:**
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/WorkflowEngine.kt`
- Test: `bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/workflow/WorkflowEngineTest.kt`

**Interfaces:**
- Consumes: Task 2 repositories、Task 3 `ExecutionContext`、Task 4 `NodeExecutor`/`ExecutionEvent`/`ExecutionEventSink`、Task 5 `AppMessage`、既有 `WorkflowNodeRepository.findByWorkflowId` / `WorkflowEdgeRepository.findByWorkflowId` / `WorkflowRepository.findOptionalById`、`NodeConfigRegistry.parse`、`WorkflowService` 的 `mapToJsonObject` 等價轉換（引擎內自備 Jackson↔kotlinx 橋接，寫法複製 `WorkflowService` 私有方法）
- Produces:

```kotlin
class WorkflowEngine {
    /**
     * 執行整條 workflow。同步阻塞直到完成；事件經 sink 即時發出。
     * @param cancelled 每節點執行前檢查；true 則中止並標 CANCELLED
     * @return 落庫後的 execution id
     */
    fun execute(
        workflowId: String,
        userId: String,
        input: Map<String, Any?>?,
        sink: ExecutionEventSink,
        cancelled: () -> Boolean
    ): String
}
```

編排規則（Phase 1）：

1. 載入 workflow + nodes + edges；執行前逐節點跑必填驗證（呼叫 `NodeConfigRegistry.parse` + `missingRequiredFields()`，缺漏拋 `ServiceException(WORKFLOW_NODE_CONFIG_REQUIRED_MISSING, ...)`）；無 TRIGGER 節點拋 `WORKFLOW_TRIGGER_NODE_REQUIRED`
2. Kahn 拓撲排序取得執行順序（演算法複製 `WorkflowService.detectCycle` 改為回傳順序；同層依 nodeKey 排序保證 determinism）
3. 建 `WorkflowExecutionEntity`（status=RUNNING）落庫 → 發 `execution.started`
4. 逐節點：`cancelled()` → 標 CANCELLED 收尾；型別無 executor（CONDITION/LOOP/CODE/DATA_TRANSFORM）→ 節點 FAILED（訊息 `WORKFLOW_NODE_TYPE_NOT_SUPPORTED`）；否則發 `node.started` → executor.execute → `context.putOutput(nodeKey, output)` → 節點紀錄落庫（含 input 快照 = 該節點 config 插值前原文 + context 現有鍵清單）→ 發 `node.completed`
5. 節點拋例外 → 節點 FAILED 落庫、發 `node.failed`、其餘未執行節點標 SKIPPED 落庫、整體 FAILED（`error_node_key` / `error_message`）
6. 全部成功 → `output_result` = 所有 OUTPUT 節點輸出合併（key=nodeKey；恰一個 OUTPUT 時直接用其輸出；零個 OUTPUT 時用拓撲序最後節點輸出）→ 整體 SUCCESS → 發 `execution.completed`
7. 落庫（execution 與 node execution 寫入）皆以 `runCatching` 包裹：失敗記 error log 不中斷（spec §2.6）
8. 事務：引擎不掛 `@Transactional`（長執行不可佔連線）；每次落庫走 repository 既有 `@Transactional` 方法

- [ ] **Step 1: 寫失敗測試（fake executor + in-memory sink；repository 以 mock/stub——依專案測試慣例，若 `src/test` 既有測試用 mockito/mockk 就沿用同款）**

測試涵蓋（每個為一個 @Test，程式碼骨架如下，斷言事件序列與狀態）：

```kotlin
package tw.zipe.bastpartner.service.workflow

// 測試以「純編排」為對象：以可控的 fake NodeExecutor 與攔截的 sink 驗證事件序列。
// repository 與 workflow 載入以 mock 回傳固定圖：trigger -> A -> output
class WorkflowEngineTest {

    // 具體 mock 建構依專案既有測試風格（src/test 下 service/ 目錄有現成範例可抄）

    @Test fun `線性圖依拓撲序執行且事件序列正確`() {
        // 期望事件序：execution.started, node.started(trigger), node.completed(trigger),
        // node.started(A), node.completed(A), node.started(out), node.completed(out), execution.completed(SUCCESS)
    }

    @Test fun `節點失敗時下游標 SKIPPED 且整體 FAILED`() {
        // A 的 fake executor 拋例外 → 事件含 node.failed(A) 與 execution.completed(FAILED)；out 無 node.started
    }

    @Test fun `cancelled 回傳 true 時中止並標 CANCELLED`()

    @Test fun `不支援的節點型別回報 FAILED 且訊息含型別名`()

    @Test fun `無 OUTPUT 節點時最終輸出取最後節點輸出`()
}
```

（實作者須把上述骨架填成可執行測試：圖固定三節點、fake executor 以 `Map<NodeType, (ctx) -> Map<String, Any?>>` 注入、sink 收集 `List<ExecutionEvent>` 後斷言 `events.map { it.event to it.nodeKey }` 序列。）

- [ ] **Step 2: 跑測試確認失敗**

Run: `cd bestpartner-service && ./gradlew test --tests "tw.zipe.bastpartner.service.workflow.WorkflowEngineTest" -Dorg.gradle.daemon=false`
Expected: 編譯失敗

- [ ] **Step 3: 實作 WorkflowEngine**

```kotlin
package tw.zipe.bastpartner.service.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import tw.zipe.bastpartner.dto.workflow.config.NodeConfigRegistry
import tw.zipe.bastpartner.entity.WorkflowExecutionEntity
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.entity.WorkflowNodeExecutionEntity
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.ExecutionStatus
import tw.zipe.bastpartner.enumerate.NodeExecutionStatus
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.enumerate.TriggerType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.repository.WorkflowEdgeRepository
import tw.zipe.bastpartner.repository.WorkflowExecutionRepository
import tw.zipe.bastpartner.repository.WorkflowNodeExecutionRepository
import tw.zipe.bastpartner.repository.WorkflowNodeRepository
import tw.zipe.bastpartner.repository.WorkflowRepository
import tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor
import tw.zipe.bastpartner.util.MessageUtil
import tw.zipe.bastpartner.util.logger

/**
 * Workflow 執行引擎（Phase 1：線性節點）。
 * 事件經 [ExecutionEventSink] 即時發出，紀錄同步落庫；寫庫失敗不中斷執行。
 */
@ApplicationScoped
class WorkflowEngine(
    private val workflowRepository: WorkflowRepository,
    private val workflowNodeRepository: WorkflowNodeRepository,
    private val workflowEdgeRepository: WorkflowEdgeRepository,
    private val executionRepository: WorkflowExecutionRepository,
    private val nodeExecutionRepository: WorkflowNodeExecutionRepository,
    private val executors: Instance<NodeExecutor>
) {
    private val logger = logger()
    private val objectMapper = ObjectMapper()
    private val json = Json { ignoreUnknownKeys = true }
    private val executorMap: Map<NodeType, NodeExecutor> by lazy { executors.associateBy { it.type } }

    fun execute(
        workflowId: String,
        userId: String,
        input: Map<String, Any?>?,
        sink: ExecutionEventSink,
        cancelled: () -> Boolean
    ): String {
        val workflow = workflowRepository.findOptionalById(workflowId)
            ?: throw ServiceException(AppMessage.WORKFLOW_NOT_FOUND)
        val nodes = workflowNodeRepository.findByWorkflowId(workflowId)
        val edges = workflowEdgeRepository.findByWorkflowId(workflowId)

        if (nodes.none { it.type == NodeType.TRIGGER }) {
            throw ServiceException(AppMessage.WORKFLOW_TRIGGER_NODE_REQUIRED)
        }
        // 執行前驗證：型別 + 必填（同啟用等級）
        nodes.forEach { n ->
            val configObj = mapToJsonObject(n.config) ?: JsonObject(emptyMap())
            val parsed = runCatching { NodeConfigRegistry.parse(n.type, configObj) }
                .getOrElse { e -> throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_INVALID, n.nodeKey, e.message.orEmpty()) }
            val missing = parsed.missingRequiredFields()
            if (missing.isNotEmpty()) {
                throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_REQUIRED_MISSING, n.nodeKey, missing.joinToString(", "))
            }
        }

        val order = topologicalOrder(nodes, edges)
        val startedAt = LocalDateTime.now()

        val execution = WorkflowExecutionEntity().apply {
            this.workflowId = workflowId
            workflowVersion = workflow.version
            triggerType = TriggerType.MANUAL
            triggeredBy = userId
            status = ExecutionStatus.RUNNING
            inputPayload = input
            this.startedAt = startedAt
        }
        persist { executionRepository.saveOrUpdate(execution) }
        val executionId = execution.id ?: java.util.UUID.randomUUID().toString()

        sink.emit(ExecutionEvent("execution.started", executionId, ts = now()))

        val context = ExecutionContext(executionId, userId)
        context.putOutput(TriggerExecutor.INPUT_KEY, mapOf("input" to (input ?: emptyMap<String, Any?>())))

        var failedNode: WorkflowNodeEntity? = null
        var failureMessage: String? = null
        var wasCancelled = false
        val executedKeys = mutableSetOf<String>()

        for ((index, node) in order.withIndex()) {
            if (cancelled()) { wasCancelled = true; break }
            val seqNo = index + 1
            sink.emit(ExecutionEvent("node.started", executionId, node.nodeKey, seqNo, ts = now()))
            val nodeStart = System.currentTimeMillis()
            val record = WorkflowNodeExecutionEntity().apply {
                this.executionId = executionId
                this.workflowId = workflowId
                nodeKey = node.nodeKey
                nodeType = node.type
                this.seqNo = seqNo
                status = NodeExecutionStatus.RUNNING
                input = node.config
                startedAt = LocalDateTime.now()
            }
            try {
                val executor = executorMap[node.type]
                    ?: throw ServiceException(AppMessage.WORKFLOW_NODE_TYPE_NOT_SUPPORTED, node.type.name)
                val configObj = mapToJsonObject(node.config) ?: JsonObject(emptyMap())
                val parsed = NodeConfigRegistry.parse(node.type, configObj)
                val output = executor.execute(node, parsed, context)
                context.putOutput(node.nodeKey, output)
                executedKeys.add(node.nodeKey)

                val duration = System.currentTimeMillis() - nodeStart
                record.apply {
                    status = NodeExecutionStatus.SUCCESS
                    this.output = output
                    finishedAt = LocalDateTime.now()
                    durationMs = duration
                }
                persist { nodeExecutionRepository.saveOrUpdate(record) }
                sink.emit(ExecutionEvent("node.completed", executionId, node.nodeKey, seqNo, "SUCCESS", output, durationMs = duration, ts = now()))
            } catch (e: Exception) {
                val message = e.message ?: e.javaClass.simpleName
                logger.error("節點 ${node.nodeKey} 執行失敗", e)
                record.apply {
                    status = NodeExecutionStatus.FAILED
                    errorMessage = message
                    finishedAt = LocalDateTime.now()
                    durationMs = System.currentTimeMillis() - nodeStart
                }
                persist { nodeExecutionRepository.saveOrUpdate(record) }
                sink.emit(ExecutionEvent("node.failed", executionId, node.nodeKey, seqNo, "FAILED", error = message, ts = now()))
                failedNode = node
                failureMessage = message
                break
            }
        }

        // 未執行節點標 SKIPPED（失敗或取消時）
        if (failedNode != null || wasCancelled) {
            order.filter { it.nodeKey !in executedKeys && it.nodeKey != failedNode?.nodeKey }.forEach { skipped ->
                persist {
                    nodeExecutionRepository.saveOrUpdate(WorkflowNodeExecutionEntity().apply {
                        this.executionId = executionId
                        this.workflowId = workflowId
                        nodeKey = skipped.nodeKey
                        nodeType = skipped.type
                        seqNo = order.indexOf(skipped) + 1
                        status = NodeExecutionStatus.SKIPPED
                    })
                }
            }
        }

        val finalStatus = when {
            wasCancelled -> ExecutionStatus.CANCELLED
            failedNode != null -> ExecutionStatus.FAILED
            else -> ExecutionStatus.SUCCESS
        }
        val finalOutput = if (finalStatus == ExecutionStatus.SUCCESS) collectFinalOutput(order, context) else null

        execution.apply {
            status = finalStatus
            outputResult = finalOutput
            errorNodeKey = failedNode?.nodeKey
            errorMessage = failureMessage
            finishedAt = LocalDateTime.now()
            durationMs = java.time.Duration.between(startedAt, LocalDateTime.now()).toMillis()
        }
        persist { executionRepository.update(execution) }
        sink.emit(
            ExecutionEvent(
                "execution.completed", executionId, status = finalStatus.name,
                output = finalOutput, error = failureMessage,
                durationMs = execution.durationMs, ts = now()
            )
        )
        return executionId
    }

    /** OUTPUT 節點輸出合併；恰一個直接用；零個取拓撲序最後節點輸出 */
    private fun collectFinalOutput(order: List<WorkflowNodeEntity>, context: ExecutionContext): Map<String, Any?> {
        val outputNodes = order.filter { it.type == NodeType.OUTPUT }
        return when {
            outputNodes.size == 1 -> context.getOutput(outputNodes[0].nodeKey) ?: emptyMap()
            outputNodes.size > 1 -> outputNodes.associate { it.nodeKey to context.getOutput(it.nodeKey) }
            else -> order.lastOrNull()?.let { context.getOutput(it.nodeKey) } ?: emptyMap()
        }
    }

    /** Kahn 拓撲排序（同層依 nodeKey 排序，結果 deterministic）；有環拋既有例外 */
    private fun topologicalOrder(
        nodes: List<WorkflowNodeEntity>,
        edges: List<tw.zipe.bastpartner.entity.WorkflowEdgeEntity>
    ): List<WorkflowNodeEntity> {
        val nodeMap = nodes.associateBy { it.nodeKey }
        val indegree = nodes.associateTo(HashMap()) { it.nodeKey to 0 }
        val adj = HashMap<String, MutableList<String>>()
        edges.forEach { e ->
            adj.getOrPut(e.sourceNodeKey) { mutableListOf() }.add(e.targetNodeKey)
            indegree[e.targetNodeKey] = (indegree[e.targetNodeKey] ?: 0) + 1
        }
        val queue = sortedSetOf<String>()
        indegree.filterValues { it == 0 }.keys.forEach { queue.add(it) }
        val order = mutableListOf<WorkflowNodeEntity>()
        while (queue.isNotEmpty()) {
            val key = queue.first().also { queue.remove(it) }
            nodeMap[key]?.let { order.add(it) }
            adj[key]?.forEach { next ->
                val deg = (indegree[next] ?: 0) - 1
                indegree[next] = deg
                if (deg == 0) queue.add(next)
            }
        }
        if (order.size != nodes.size) throw ServiceException(AppMessage.WORKFLOW_GRAPH_HAS_CYCLE)
        return order
    }

    /** 落庫失敗不中斷執行（spec §2.6） */
    private fun persist(block: () -> Unit) {
        runCatching(block).onFailure { logger.error("執行紀錄落庫失敗", it) }
    }

    private fun now(): String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    private fun mapToJsonObject(map: Map<String, Any?>?): JsonObject? {
        if (map == null) return null
        return json.parseToJsonElement(objectMapper.writeValueAsString(map)).jsonObject
    }
}
```

> 實作注意：`WorkflowNodeEntity.config` 的實際型別（`Map<String, Any?>?`）與 `WorkflowEdgeEntity` 欄位名以檔案為準。

- [ ] **Step 4: 跑測試確認通過**

Run: `cd bestpartner-service && ./gradlew test --tests "tw.zipe.bastpartner.service.workflow.WorkflowEngineTest" -Dorg.gradle.daemon=false`
Expected: PASS（5 tests）

- [ ] **Step 5: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/workflow/WorkflowEngine.kt bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/workflow/WorkflowEngineTest.kt
git commit -m "新增(bestpartner-service): 新增 workflow 執行引擎編排"
```

---

### Task 9: `POST /llm/workflow/execute` SSE 端點

**Files:**
- Modify: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/resource/WorkflowResource.kt`
- Modify: `.claude/rules/api-endpoints.md`（WORKFLOW 表加一列）

**Interfaces:**
- Consumes: Task 8 `WorkflowEngine.execute(...)`；既有 `WorkflowService.get(id)`（先呼叫以觸發擁有權檢核）；`SecurityValidator.validateLoggedInUser()`（取 userId，須在進 async 前解析——SecurityIdentity 是 request scope）
- Produces: `POST /llm/workflow/execute`，body `WorkflowDTO`（用 `id` + `inputPayload`），回 `Multi<String>`（每元素一行 ExecutionEvent JSON）

- [ ] **Step 1: 實作端點（比照 `LLMResource.customAssistantChatStreaming` 的 Multi emitter 模式；`WorkflowDTO` 若無 `inputPayload` 欄位則在 DTO 加 `var inputPayload: JsonObject? = null`）**

```kotlin
    @POST
    @Path("/execute")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    fun execute(dto: WorkflowDTO): Multi<String> {
        val id = dto.id ?: throw ServiceException(AppMessage.WORKFLOW_NOT_FOUND)
        // request scope 內先完成擁有權檢核與 userId 解析，再進背景執行
        workflowService.get(id)
        val userId = securityValidator.validateLoggedInUser()
        val input: Map<String, Any?>? = dto.inputPayload?.let { workflowService.jsonObjectToMapPublic(it) }

        return Multi.createFrom().emitter { emitter ->
            val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
            emitter.onTermination { cancelled.set(true) }
            CompletableFuture.runAsync {
                try {
                    workflowEngine.execute(id, userId, input, { event -> emitter.emit(event.toJson()) }) { cancelled.get() }
                    emitter.complete()
                } catch (e: Exception) {
                    logger.error("workflow 執行失敗: $id", e)
                    emitter.fail(e)
                }
            }
        }
    }
```

同時：
- `WorkflowResource` 建構子注入 `workflowEngine: WorkflowEngine` 與 `securityValidator: SecurityValidator`（若尚未注入）
- `WorkflowService.jsonObjectToMap` 改為 `internal`/public 供 resource 用（或在 resource 內自備同款轉換，擇一，不重複三份）
- `WorkflowDTO` 加欄位 `var inputPayload: JsonObject? = null`（kotlinx `@Serializable` 相容）

- [ ] **Step 2: 編譯 + 手動 smoke（起服務後以 curl 驗證）**

```bash
cd bestpartner-service && ./gradlew build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=dev
# 起服務後：
TOKEN=$(curl -s -X POST http://localhost:80/login/ -H "Content-Type: application/json" -d '{"email":"test@partmer.com.tw","password":"user"}' | grep -o '"data":"[^"]*' | cut -d'"' -f4)
curl -s -N -X POST http://localhost:80/llm/workflow/execute -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{"id":"<現有 workflow id>"}'
```

Expected: 依序吐出 `data:{"event":"execution.started"...}` … `execution.completed`

- [ ] **Step 3: 更新 `.claude/rules/api-endpoints.md`（WORKFLOW 段加一列）**

```markdown
| POST | `/llm/workflow/execute` | 手動執行 workflow（SSE 逐節點事件流：execution.started / node.started / node.completed / node.failed / execution.completed），須傳入 id，可帶 inputPayload |
```

- [ ] **Step 4: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/resource/WorkflowResource.kt bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/dto/WorkflowDTO.kt bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/WorkflowService.kt .claude/rules/api-endpoints.md
git commit -m "新增(bestpartner-service): 新增 workflow 手動執行 SSE 端點"
```

---

### Task 10: 前端 OUTPUT 節點型別與表單

**Files:**
- Modify: `bestpartner-ui/src/types/workflow.ts`（NodeType union 加 `'OUTPUT'`）
- Modify: `bestpartner-ui/src/constants/nodeTypes.ts`
- Create: `bestpartner-ui/src/components/inspector/forms/OutputForm.vue`
- Modify: `bestpartner-ui/src/components/inspector/InspectorPanel.vue`（TYPED_FORMS 加 OUTPUT）
- Test: `bestpartner-ui/src/components/inspector/forms/__tests__/OutputForm.spec.ts`

**Interfaces:**
- Consumes: 既有表單模式——props `{ config: Record<string, unknown> }`、emit `'update:config'`（參考 `McpServerForm.vue`）
- Produces: palette 可拖 OUTPUT 節點；表單支援 `template`（textarea）與 `mappings`（key/value 列表，簡化為每行 `key=value` 的 textarea 亦可，但須雙向轉換無損）

- [ ] **Step 1: 寫失敗測試（比照 `__tests__` 內既有表單測試寫法；核心斷言）**

```typescript
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OutputForm from '../OutputForm.vue'

describe('OutputForm', () => {
  it('輸入 template 後 emit update:config 帶 template 鍵', async () => {
    const wrapper = mount(OutputForm, { props: { config: {} } })
    await wrapper.find('[data-test="output-template"]').setValue('結果：{{llm.reply}}')
    const events = wrapper.emitted('update:config')!
    expect(events.at(-1)![0]).toEqual({ template: '結果：{{llm.reply}}' })
  })

  it('template 清空時移除鍵', async () => {
    const wrapper = mount(OutputForm, { props: { config: { template: 'x' } } })
    await wrapper.find('[data-test="output-template"]').setValue('')
    const events = wrapper.emitted('update:config')!
    expect(events.at(-1)![0]).toEqual({})
  })
})
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `cd bestpartner-ui && npx vitest run src/components/inspector/forms/__tests__/OutputForm.spec.ts`
Expected: FAIL（找不到 OutputForm.vue）

- [ ] **Step 3: 實作**

`types/workflow.ts`：NodeType union 追加 `| 'OUTPUT'`。

`constants/nodeTypes.ts` `NODE_TYPE_METAS` 追加（放最後）：

```typescript
  {
    type: 'OUTPUT',
    label: '輸出',
    color: '#52c41a',
    icon: '📤',
    category: 'Action',
    inputs: DEFAULT_INPUTS,
    outputs: [],
  },
```

`OutputForm.vue`（樣式 class 沿用其他表單的 `.form/.field/.text-input`，直接複製 McpServerForm 的 `<style scoped>`）：

```vue
<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{ config: Record<string, unknown> }>()
const emit = defineEmits<{ 'update:config': [config: Record<string, unknown>] }>()

const template = ref<string>((props.config.template as string) ?? '')
// mappings 以每行 key=value 編輯
const mappingsText = ref<string>(
  Object.entries((props.config.mappings as Record<string, string>) ?? {})
    .map(([k, v]) => `${k}=${v}`)
    .join('\n'),
)

function parseMappings(text: string): Record<string, string> {
  const result: Record<string, string> = {}
  text.split('\n').forEach((line) => {
    const idx = line.indexOf('=')
    if (idx > 0) result[line.slice(0, idx).trim()] = line.slice(idx + 1).trim()
  })
  return result
}

function emitConfig() {
  const next: Record<string, unknown> = { ...props.config }
  if (template.value) next.template = template.value
  else delete next.template
  const mappings = parseMappings(mappingsText.value)
  if (Object.keys(mappings).length > 0) next.mappings = mappings
  else delete next.mappings
  emit('update:config', next)
}
</script>

<template>
  <div class="form">
    <div class="field">
      <!-- v-pre 避免 {{變數}} 被 Vue 當插值 -->
      <label v-pre>輸出模板（支援 {{nodeKey.path}} 插值，結果放 result 鍵）</label>
      <textarea
        v-model="template"
        data-test="output-template"
        class="text-input"
        rows="4"
        @input="emitConfig"
      />
    </div>
    <div class="field">
      <label>欄位對映（選填，每行 key=value，value 支援插值）</label>
      <textarea
        v-model="mappingsText"
        data-test="output-mappings"
        class="text-input"
        rows="3"
        @input="emitConfig"
      />
    </div>
  </div>
</template>
```

`InspectorPanel.vue`：import OutputForm 並在 `TYPED_FORMS` 加 `OUTPUT: OutputForm`。

- [ ] **Step 4: 跑測試確認通過**

Run: `cd bestpartner-ui && npx vitest run src/components/inspector/forms/__tests__/OutputForm.spec.ts && npx vitest run`
Expected: 新測試 PASS，且既有全部測試不退步

- [ ] **Step 5: Commit**

```bash
git add bestpartner-ui/src/types/workflow.ts bestpartner-ui/src/constants/nodeTypes.ts bestpartner-ui/src/components/inspector/forms/OutputForm.vue bestpartner-ui/src/components/inspector/InspectorPanel.vue bestpartner-ui/src/components/inspector/forms/__tests__/OutputForm.spec.ts
git commit -m "新增(bestpartner-ui): 新增 OUTPUT 節點型別與設定表單"
```

---

### Task 11: 前端 SSE client 與 execution store

**Files:**
- Create: `bestpartner-ui/src/api/workflowExecution.ts`
- Create: `bestpartner-ui/src/stores/execution.ts`
- Test: `bestpartner-ui/src/stores/__tests__/execution.spec.ts`

**Interfaces:**
- Produces:

```typescript
// api/workflowExecution.ts
export interface ExecutionEvent {
  event: 'execution.started' | 'node.started' | 'node.completed' | 'node.failed' | 'execution.completed'
  executionId: string
  nodeKey?: string
  seqNo?: number
  status?: string
  output?: Record<string, unknown>
  error?: string
  durationMs?: number
  ts: string
}
/** POST /llm/workflow/execute，逐事件回呼；回傳 abort 函式 */
export function executeWorkflow(
  workflowId: string,
  onEvent: (e: ExecutionEvent) => void,
  onDone: () => void,
  onError: (err: unknown) => void,
): () => void
```

```typescript
// stores/execution.ts
export type NodeRunStatus = 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'
export interface NodeRunState { status: NodeRunStatus; output?: Record<string, unknown>; error?: string; durationMs?: number }
export const useExecutionStore = defineStore('execution', ...)
// state: running: boolean; executionId: string | null;
//        nodeStates: Record<string, NodeRunState>; finalStatus: string | null;
//        finalOutput: Record<string, unknown> | null; errorMessage: string | null
// actions: start(workflowId): void（清空 → 呼叫 executeWorkflow 並套事件）; stop(): void; applyEvent(e): void; reset(): void
```

- [ ] **Step 1: 寫失敗測試（store 純邏輯：直接餵 applyEvent）**

```typescript
import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useExecutionStore } from '../execution'

describe('execution store', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('node.started 設 RUNNING，node.completed 設 SUCCESS 與 output', () => {
    const store = useExecutionStore()
    store.applyEvent({ event: 'execution.started', executionId: 'e1', ts: 't' })
    store.applyEvent({ event: 'node.started', executionId: 'e1', nodeKey: 'n1', ts: 't' })
    expect(store.nodeStates['n1'].status).toBe('RUNNING')
    store.applyEvent({ event: 'node.completed', executionId: 'e1', nodeKey: 'n1', status: 'SUCCESS', output: { reply: 'hi' }, durationMs: 5, ts: 't' })
    expect(store.nodeStates['n1']).toMatchObject({ status: 'SUCCESS', output: { reply: 'hi' }, durationMs: 5 })
  })

  it('node.failed 記錯誤，execution.completed 記最終狀態與輸出', () => {
    const store = useExecutionStore()
    store.applyEvent({ event: 'node.failed', executionId: 'e1', nodeKey: 'n1', error: 'boom', ts: 't' })
    expect(store.nodeStates['n1']).toMatchObject({ status: 'FAILED', error: 'boom' })
    store.applyEvent({ event: 'execution.completed', executionId: 'e1', status: 'FAILED', error: 'boom', ts: 't' })
    expect(store.finalStatus).toBe('FAILED')
    expect(store.running).toBe(false)
  })

  it('reset 清空全部狀態', () => {
    const store = useExecutionStore()
    store.applyEvent({ event: 'node.started', executionId: 'e1', nodeKey: 'n1', ts: 't' })
    store.reset()
    expect(Object.keys(store.nodeStates)).toHaveLength(0)
    expect(store.executionId).toBeNull()
  })
})
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `cd bestpartner-ui && npx vitest run src/stores/__tests__/execution.spec.ts`
Expected: FAIL

- [ ] **Step 3: 實作**

`api/workflowExecution.ts`：

```typescript
import type { ExecutionEvent } from '../types/execution' // 若無獨立型別檔，直接在本檔 export interface（如 Interfaces 區塊）

/** 後端 SSE 每行格式為 `data:{json}`；空行分隔 */
export function executeWorkflow(
  workflowId: string,
  onEvent: (e: ExecutionEvent) => void,
  onDone: () => void,
  onError: (err: unknown) => void,
): () => void {
  const controller = new AbortController()
  const token = localStorage.getItem('token')

  void (async () => {
    try {
      const res = await fetch('/llm/workflow/execute', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ id: workflowId }),
        signal: controller.signal,
      })
      if (!res.ok || !res.body) throw new Error(`HTTP ${res.status}`)
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      for (;;) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''
        for (const line of lines) {
          const text = line.startsWith('data:') ? line.slice(5).trim() : line.trim()
          if (!text) continue
          onEvent(JSON.parse(text) as ExecutionEvent)
        }
      }
      onDone()
    } catch (err) {
      if ((err as Error).name !== 'AbortError') onError(err)
      else onDone()
    }
  })()

  return () => controller.abort()
}
```

> 注意：既有 axios 走 Vite dev proxy（查 `vite.config.ts` 的 proxy 設定與 `api/http.ts` 的 baseURL，`fetch` 的 URL 前綴須一致；若 axios 用 `http://localhost:80` 絕對路徑則此處比照）。

`stores/execution.ts`：

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { executeWorkflow, type ExecutionEvent } from '../api/workflowExecution'

export type NodeRunStatus = 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'

export interface NodeRunState {
  status: NodeRunStatus
  output?: Record<string, unknown>
  error?: string
  durationMs?: number
}

export const useExecutionStore = defineStore('execution', () => {
  const running = ref(false)
  const executionId = ref<string | null>(null)
  const nodeStates = ref<Record<string, NodeRunState>>({})
  const finalStatus = ref<string | null>(null)
  const finalOutput = ref<Record<string, unknown> | null>(null)
  const errorMessage = ref<string | null>(null)
  let abortFn: (() => void) | null = null

  function reset(): void {
    running.value = false
    executionId.value = null
    nodeStates.value = {}
    finalStatus.value = null
    finalOutput.value = null
    errorMessage.value = null
  }

  function applyEvent(e: ExecutionEvent): void {
    executionId.value = e.executionId
    switch (e.event) {
      case 'execution.started':
        running.value = true
        break
      case 'node.started':
        if (e.nodeKey) nodeStates.value[e.nodeKey] = { status: 'RUNNING' }
        break
      case 'node.completed':
        if (e.nodeKey)
          nodeStates.value[e.nodeKey] = {
            status: 'SUCCESS',
            output: e.output,
            durationMs: e.durationMs,
          }
        break
      case 'node.failed':
        if (e.nodeKey) nodeStates.value[e.nodeKey] = { status: 'FAILED', error: e.error }
        break
      case 'execution.completed':
        running.value = false
        finalStatus.value = e.status ?? null
        finalOutput.value = e.output ?? null
        errorMessage.value = e.error ?? null
        break
    }
  }

  function start(workflowId: string): void {
    reset()
    running.value = true
    abortFn = executeWorkflow(
      workflowId,
      applyEvent,
      () => {
        running.value = false
      },
      (err) => {
        running.value = false
        errorMessage.value = err instanceof Error ? err.message : String(err)
        finalStatus.value = finalStatus.value ?? 'FAILED'
      },
    )
  }

  function stop(): void {
    abortFn?.()
    abortFn = null
    running.value = false
    finalStatus.value = finalStatus.value ?? 'CANCELLED'
  }

  return { running, executionId, nodeStates, finalStatus, finalOutput, errorMessage, applyEvent, start, stop, reset }
})
```

- [ ] **Step 4: 跑測試確認通過**

Run: `cd bestpartner-ui && npx vitest run src/stores/__tests__/execution.spec.ts`
Expected: PASS（3 tests）

- [ ] **Step 5: Commit**

```bash
git add bestpartner-ui/src/api/workflowExecution.ts bestpartner-ui/src/stores/execution.ts bestpartner-ui/src/stores/__tests__/execution.spec.ts
git commit -m "新增(bestpartner-ui): 新增執行 SSE client 與 execution store"
```

---

### Task 12: 執行按鈕與畫布狀態上色

**Files:**
- Modify: `bestpartner-ui/src/views/WorkflowEditorView.vue`（工具列加執行/停止按鈕 + 接 execution store）
- Modify: `bestpartner-ui/src/components/canvas/WorkflowNode.vue`（依 nodeStates 上色）
- Modify: `bestpartner-ui/src/styles/workflow-theme.css`（新增狀態樣式變數，如已有結構就地擴充）

**Interfaces:**
- Consumes: Task 11 `useExecutionStore`
- Produces: `[data-test="run-button"]`（執行/停止切換）、節點 class `is-exec-running` / `is-exec-success` / `is-exec-failed` / `is-exec-skipped`

- [ ] **Step 1: WorkflowEditorView 加按鈕與邏輯（工具列「存檔」按鈕左側）**

```vue
<!-- template（toolbar-right 內、整理版面之前） -->
<button
  type="button"
  class="btn primary"
  data-test="run-button"
  @click="handleRun"
>
  {{ executionStore.running ? '■ 停止' : '▶ 執行' }}
</button>
```

```typescript
// script setup 內
import { useExecutionStore } from '../stores/execution'
const executionStore = useExecutionStore()

async function handleRun() {
  if (executionStore.running) {
    executionStore.stop()
    return
  }
  const id = store.current?.id
  if (!id) {
    ElMessage.warning('請先存檔後再執行')
    return
  }
  if (store.dirty) {
    ElMessage.warning('有未存變更，請先存檔再執行')
    return
  }
  executionStore.start(id)
}
```

離開編輯器（`onBeforeUnmount`）時呼叫 `executionStore.stop()` 與 `executionStore.reset()`。

- [ ] **Step 2: WorkflowNode.vue 上色（讀 store，依自身 nodeKey 取狀態掛 class；WorkflowNode 內以 `props.id`（Vue Flow node id = nodeKey）查 `executionStore.nodeStates`）**

```typescript
import { computed } from 'vue'
import { useExecutionStore } from '../../stores/execution'
const executionStore = useExecutionStore()
const execClass = computed(() => {
  const s = executionStore.nodeStates[props.id]?.status
  return s ? `is-exec-${s.toLowerCase()}` : ''
})
```

外層節點 div 綁 `:class="execClass"`，CSS（加在 WorkflowNode 的 scoped style 或 workflow-theme.css）：

```css
.is-exec-running { border-color: #e6a23c !important; animation: exec-pulse 1.2s infinite; }
.is-exec-success { border-color: #67c23a !important; }
.is-exec-failed  { border-color: #f56c6c !important; }
.is-exec-skipped { opacity: 0.45; }
@keyframes exec-pulse { 50% { box-shadow: 0 0 0 6px rgba(230, 162, 60, 0.25); } }
```

- [ ] **Step 3: 手動驗證（前後端啟動，對既有 workflow 按執行，觀察節點依序變色）**

Run: 開 http://localhost:5173 → 開啟 workflow → ▶ 執行
Expected: trigger → LLM 依序 RUNNING(橘) → SUCCESS(綠)；按鈕執行中顯示「■ 停止」

- [ ] **Step 4: 跑既有前端測試不退步**

Run: `cd bestpartner-ui && npx vitest run`
Expected: 全部 PASS

- [ ] **Step 5: Commit**

```bash
git add bestpartner-ui/src/views/WorkflowEditorView.vue bestpartner-ui/src/components/canvas/WorkflowNode.vue bestpartner-ui/src/styles/workflow-theme.css
git commit -m "新增(bestpartner-ui): 畫布執行按鈕與節點即時狀態上色"
```

---

### Task 13: Inspector「本次執行」區塊與結果面板

**Files:**
- Modify: `bestpartner-ui/src/components/inspector/InspectorPanel.vue`
- Create: `bestpartner-ui/src/components/canvas/ExecutionResultDrawer.vue`
- Modify: `bestpartner-ui/src/views/WorkflowEditorView.vue`（掛 drawer）
- Test: `bestpartner-ui/src/components/canvas/__tests__/ExecutionResultDrawer.spec.ts`

**Interfaces:**
- Consumes: `useExecutionStore`（nodeStates / finalStatus / finalOutput / errorMessage）
- Produces:
  - InspectorPanel：選中節點且 `nodeStates[nodeKey]` 存在時，Parameters 下方顯示「本次執行」：狀態、耗時、output JSON（`<pre>` + `JSON.stringify(output, null, 2)`）、error
  - `ExecutionResultDrawer`：props 無（直接讀 store）；`finalStatus` 非 null 時顯示底部面板：整體狀態、最終輸出 JSON、錯誤訊息、關閉按鈕（`[data-test="result-drawer"]`、`[data-test="result-close"]`）

- [ ] **Step 1: 寫 drawer 失敗測試**

```typescript
import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ExecutionResultDrawer from '../ExecutionResultDrawer.vue'
import { useExecutionStore } from '../../../stores/execution'

describe('ExecutionResultDrawer', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('無執行結果時不顯示', () => {
    const wrapper = mount(ExecutionResultDrawer)
    expect(wrapper.find('[data-test="result-drawer"]').exists()).toBe(false)
  })

  it('執行完成顯示狀態與最終輸出', async () => {
    const store = useExecutionStore()
    store.applyEvent({ event: 'execution.completed', executionId: 'e1', status: 'SUCCESS', output: { result: '答案' }, ts: 't' })
    const wrapper = mount(ExecutionResultDrawer)
    expect(wrapper.find('[data-test="result-drawer"]').text()).toContain('SUCCESS')
    expect(wrapper.find('[data-test="result-drawer"]').text()).toContain('答案')
  })
})
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `cd bestpartner-ui && npx vitest run src/components/canvas/__tests__/ExecutionResultDrawer.spec.ts`
Expected: FAIL

- [ ] **Step 3: 實作**

`ExecutionResultDrawer.vue`：

```vue
<script setup lang="ts">
import { useExecutionStore } from '../../stores/execution'

const store = useExecutionStore()
</script>

<template>
  <div v-if="store.finalStatus" class="result-drawer" data-test="result-drawer">
    <div class="drawer-header">
      <span class="drawer-title">執行結果</span>
      <span class="status" :class="`status-${store.finalStatus.toLowerCase()}`">{{ store.finalStatus }}</span>
      <button type="button" class="close-btn" data-test="result-close" @click="store.reset()">✕</button>
    </div>
    <p v-if="store.errorMessage" class="error-text">{{ store.errorMessage }}</p>
    <pre v-if="store.finalOutput" class="output-json">{{ JSON.stringify(store.finalOutput, null, 2) }}</pre>
  </div>
</template>

<style scoped>
.result-drawer {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  max-height: 40%;
  overflow: auto;
  padding: 12px 16px;
  background: var(--wf-surface-2, #17171c);
  border-top: 1px solid var(--wf-border, #29292f);
  z-index: 10;
}
.drawer-header { display: flex; align-items: center; gap: 10px; }
.drawer-title { font-weight: 700; font-size: 13px; color: var(--wf-text, #e7e7ec); }
.status { font-size: 11px; font-weight: 700; }
.status-success { color: #67c23a; }
.status-failed { color: #f56c6c; }
.status-cancelled { color: #e6a23c; }
.close-btn { margin-left: auto; background: none; border: none; color: var(--wf-text-dim, #8a8a95); cursor: pointer; }
.error-text { color: #f56c6c; font-size: 12px; }
.output-json { font-size: 12px; color: var(--wf-text, #e7e7ec); white-space: pre-wrap; }
</style>
```

`WorkflowEditorView.vue`：`.canvas` div 內（VueFlow 之後）加 `<ExecutionResultDrawer />` 並 import。

`InspectorPanel.vue`：selected-node 區塊的 Parameters 之後加：

```vue
<div v-if="nodeRun" class="exec-section" data-test="node-exec-section">
  <div class="group-title">本次執行</div>
  <p>狀態：{{ nodeRun.status }}<span v-if="nodeRun.durationMs != null">（{{ nodeRun.durationMs }} ms）</span></p>
  <p v-if="nodeRun.error" class="error-text">{{ nodeRun.error }}</p>
  <pre v-if="nodeRun.output" class="output-json">{{ JSON.stringify(nodeRun.output, null, 2) }}</pre>
</div>
```

```typescript
import { useExecutionStore } from '../../stores/execution'
const executionStore = useExecutionStore()
const nodeRun = computed(() =>
  props.selectedNode ? executionStore.nodeStates[props.selectedNode.id] : undefined,
)
```

> 注意：`FlowNode` 的 id 欄位名以 `useWorkflowSync.ts` 的 FlowNode 型別為準（`id` 即 nodeKey）。

- [ ] **Step 4: 跑測試確認通過 + 全量不退步**

Run: `cd bestpartner-ui && npx vitest run`
Expected: 全部 PASS

- [ ] **Step 5: Commit**

```bash
git add bestpartner-ui/src/components/canvas/ExecutionResultDrawer.vue bestpartner-ui/src/components/canvas/__tests__/ExecutionResultDrawer.spec.ts bestpartner-ui/src/components/inspector/InspectorPanel.vue bestpartner-ui/src/views/WorkflowEditorView.vue
git commit -m "新增(bestpartner-ui): 執行結果面板與節點執行資料檢視"
```

---

### Task 14: 端對端驗收與文件同步

**Files:**
- Modify: `docs/postman/basepartner.postman_collection.json`（workflow 資料夾加 execute 請求）
- 其餘文件由 documentation-sync skill 掃描決定

- [ ] **Step 1: 全量測試**

```bash
cd bestpartner-service && ./gradlew test -Dorg.gradle.daemon=false
cd ../bestpartner-ui && npx vitest run
```

Expected: 全部 PASS

- [ ] **Step 2: 建置 uber-jar 並重啟服務（dev profile），以 webwright（Playwright）走 UI E2E：登入 → 開啟「觸發→LLM(OpenRouter+date MCP)→輸出」workflow → ▶ 執行 → 斷言節點依序變色、結果面板出現最終輸出（含日期）→ 錄製截圖/GIF**

Expected: 全流程通過、GIF 產出

- [ ] **Step 3: 呼叫 `test-confirmation` skill 執行測試週期（範圍：WORKFLOW 模組 + 新 execute 端點），呼叫 `documentation-sync` skill 同步文件（api-endpoints.md 已於 Task 9 更新；docs-site、api-test-plan.md、postman collection 依掃描結果補）**

- [ ] **Step 4: Commit（文件）**

```bash
git add docs .claude/rules
git commit -m "文件: 同步 workflow 執行功能相關文件與測試確認表"
```

---

## Self-Review 紀錄

- **Spec 覆蓋**：spec §2.1（Task 1/10）、§2.2 編排（Task 8）、§2.3 插值（Task 3）、§2.4 API（Task 9；`execution/list`/`get` 屬 Phase 3 不在本計畫）、§2.6 持久化（Task 2/8）、§3.1-3.4 前端（Task 10-13）、§3.5 Executions 頁屬 Phase 3。CODE sandbox（§2.5）屬 Phase 2。
- **佔位符**：Task 8 Step 1 測試以骨架 + 明確填寫指示呈現（fake executor/斷言方式已具體說明）；其餘任務皆含完整程式碼。
- **型別一致性**：`ExecutionEvent.toJson()`（Task 4 定義、Task 9 使用）、`ExecutionContext.resolveTemplate/putOutput`（Task 3 定義、Task 4/6/7 使用）、`NodeRunState`（Task 11 定義、Task 12/13 使用）、`TriggerExecutor.INPUT_KEY`（Task 4 定義、Task 8 使用）已核對。
- **既有 API 依賴風險**：Task 6/7 標註了 langchain4j 版本 API 與 DTO 欄位的「實作時開檔確認」點，避免計畫錯誤直接傳染到實作。
