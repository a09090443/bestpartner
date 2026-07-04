# 節點 config 與 Tool settingSchema 契約實作計畫

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.
> 注意：依使用者偏好**不建立 git worktree**，直接在主目錄 `D:\projects\bestpartner` 進行。

**Goal:** 消滅前端「靠猜」的 JSON config——workflow 節點 config 以後端強型別 DTO 驗證，tool 設定欄位由後端反射產生 settingSchema 供前端動態渲染表單。

**Architecture:** 後端為 10 種 NodeType 各建 `@Serializable` config DTO（欄位全 nullable），save 時嚴格反序列化驗型別與未知欄位、switchStatus 啟用時驗必填；tool 端以 `ToolSchemaGenerator` 從 `configObjectPath` data class 反射產生結構化 schema（type/required/sensitive/description），隨 `/llm/tool/list`、`/llm/tool/get` 回傳；前端新增 `SettingSchemaForm.vue` 依 schema 動態渲染設定表單，整合進 `ToolForm.vue`。

**Tech Stack:** Kotlin 2.1.0 / Quarkus 3.21.0 / kotlinx.serialization / kotlin-reflect；Vue 3 + TypeScript + Vitest。

**設計依據:** `docs/plans/2026-07-04-config-schema-contract-design.md`；節點 config 欄位定義以 `docs/workflow-engine/system-design.md` §2 為準。

**測試前置條件:**
- 後端 `@QuarkusTest` 需本地 PostgreSQL（`localhost:5432/pgdb`，`pguser`/`pgpass`）運行中。
- 後端測試指令均在 `bestpartner-service/` 目錄執行；前端在 `bestpartner-ui/`。
- Commit 訊息遵守 `.claude/skills/git-commit-message` 規範（繁中類型詞）。

---

## Task 1: 節點 config 強型別 DTO（10 種 NodeType）

**Files:**
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/dto/workflow/config/NodeConfig.kt`
- Test: `bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/dto/workflow/config/NodeConfigTest.kt`

**Step 1: 寫失敗測試**

純序列化單元測試，不需 `@QuarkusTest`、不需 DB：

```kotlin
package tw.zipe.bastpartner.dto.workflow.config

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.enumerate.NodeType

/**
 * NodeConfigRegistry 序列化契約測試：合法 config 可解析、型別錯與未知欄位被拒、
 * missingRequiredFields 正確回報缺席必填欄位。
 */
class NodeConfigTest {

    @Test
    fun `LLM_ASSISTANT 合法 config 解析成功且無缺必填`() {
        val config = buildJsonObject {
            put("llmId", "llm-uuid")
            put("systemPrompt", "你是客服助手")
            putJsonArray("toolIds") { add(kotlinx.serialization.json.JsonPrimitive("tool-1")) }
        }
        val parsed = NodeConfigRegistry.parse(NodeType.LLM_ASSISTANT, config)
        assertTrue(parsed is LlmAssistantNodeConfig)
        assertEquals(emptyList<String>(), parsed.missingRequiredFields())
    }

    @Test
    fun `LLM_ASSISTANT 缺 llmId 可解析但回報缺必填`() {
        val config = buildJsonObject { put("systemPrompt", "hi") }
        val parsed = NodeConfigRegistry.parse(NodeType.LLM_ASSISTANT, config)
        assertEquals(listOf("llmId"), parsed.missingRequiredFields())
    }

    @Test
    fun `未知欄位被拒`() {
        val config = buildJsonObject {
            put("llmId", "llm-uuid")
            put("notAField", "x")
        }
        assertThrows(SerializationException::class.java) {
            NodeConfigRegistry.parse(NodeType.LLM_ASSISTANT, config)
        }
    }

    @Test
    fun `型別錯誤被拒 - toolIds 給字串而非陣列`() {
        val config = buildJsonObject {
            put("llmId", "llm-uuid")
            put("toolIds", "not-an-array")
        }
        assertThrows(SerializationException::class.java) {
            NodeConfigRegistry.parse(NodeType.LLM_ASSISTANT, config)
        }
    }

    @Test
    fun `全部 10 種 NodeType 空 config 皆可解析`() {
        NodeType.entries.forEach { type ->
            NodeConfigRegistry.parse(type, buildJsonObject { })
        }
    }

    @Test
    fun `各 NodeType 必填欄位清單正確`() {
        val empty = buildJsonObject { }
        assertEquals(listOf("triggerType"), NodeConfigRegistry.parse(NodeType.TRIGGER, empty).missingRequiredFields())
        assertEquals(listOf("toolId"), NodeConfigRegistry.parse(NodeType.TOOL, empty).missingRequiredFields())
        assertEquals(listOf("mcpId", "toolName"), NodeConfigRegistry.parse(NodeType.MCP_SERVER, empty).missingRequiredFields())
        assertEquals(
            listOf("knowledgeId", "embeddingModelId", "query"),
            NodeConfigRegistry.parse(NodeType.KNOWLEDGE_RAG, empty).missingRequiredFields()
        )
        assertEquals(listOf("conditions"), NodeConfigRegistry.parse(NodeType.CONDITION, empty).missingRequiredFields())
        assertEquals(
            listOf("inputArrayPath", "loopBodyEntryNodeKey"),
            NodeConfigRegistry.parse(NodeType.LOOP, empty).missingRequiredFields()
        )
        assertEquals(listOf("language", "source"), NodeConfigRegistry.parse(NodeType.CODE, empty).missingRequiredFields())
        assertEquals(listOf("method", "url"), NodeConfigRegistry.parse(NodeType.HTTP_REQUEST, empty).missingRequiredFields())
        assertEquals(listOf("mappings"), NodeConfigRegistry.parse(NodeType.DATA_TRANSFORM, empty).missingRequiredFields())
    }
}
```

**Step 2: 執行測試確認失敗**

```bash
cd bestpartner-service
./gradlew test --tests "tw.zipe.bastpartner.dto.workflow.config.NodeConfigTest" -Dorg.gradle.daemon=false
```
預期：編譯失敗（`NodeConfigRegistry` 不存在）。

**Step 3: 實作 NodeConfig.kt**

欄位定義完全對照 `docs/workflow-engine/system-design.md` §2.1–2.10：

```kotlin
package tw.zipe.bastpartner.dto.workflow.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import tw.zipe.bastpartner.enumerate.NodeType

/**
 * Workflow 節點 config 的強型別契約。
 *
 * 事實來源：本檔案即各 NodeType config schema 的唯一事實來源，
 * `docs/workflow-engine/system-design.md` §2 為對照說明文件。
 *
 * 欄位一律 nullable 以支援兩段式驗證：save（DRAFT）僅驗型別與未知欄位，
 * 必填檢核（[NodeConfig.missingRequiredFields]）於 switchStatus 啟用時執行。
 */
sealed interface NodeConfig {
    /** 回傳缺席的必填欄位名稱；空清單代表滿足啟用條件 */
    fun missingRequiredFields(): List<String>
}

@Serializable
data class TriggerNodeConfig(
    val triggerType: String? = null,
    val inputSchema: JsonObject? = null,
    val webhook: JsonObject? = null,
    val cron: JsonObject? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        if (triggerType.isNullOrBlank()) add("triggerType")
    }
}

@Serializable
data class LlmAssistantNodeConfig(
    val llmId: String? = null,
    val systemPrompt: String? = null,
    val userPrompt: String? = null,
    val enableMemory: Boolean? = null,
    val memoryId: String? = null,
    val toolIds: List<String>? = null,
    val toolSettingIds: List<String>? = null,
    val mcpIds: List<String>? = null,
    val mcpSettingIds: List<String>? = null,
    val skillIds: List<String>? = null,
    val knowledgeId: String? = null,
    val files: List<String>? = null,
    val responseFormat: String? = null,
    val outputSchema: JsonObject? = null,
    val outputKey: String? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        if (llmId.isNullOrBlank()) add("llmId")
    }
}

@Serializable
data class ToolNodeConfig(
    val toolId: String? = null,
    val toolSettingId: String? = null,
    val arguments: JsonObject? = null,
    val outputKey: String? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        if (toolId.isNullOrBlank()) add("toolId")
    }
}

@Serializable
data class McpServerNodeConfig(
    val mcpId: String? = null,
    val userSettingId: String? = null,
    val toolName: String? = null,
    val arguments: JsonObject? = null,
    val outputKey: String? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        if (mcpId.isNullOrBlank()) add("mcpId")
        if (toolName.isNullOrBlank()) add("toolName")
    }
}

@Serializable
data class KnowledgeRagNodeConfig(
    val knowledgeId: String? = null,
    val embeddingModelId: String? = null,
    val query: String? = null,
    val topK: Int? = null,
    val minScore: Double? = null,
    val outputKey: String? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        if (knowledgeId.isNullOrBlank()) add("knowledgeId")
        if (embeddingModelId.isNullOrBlank()) add("embeddingModelId")
        if (query.isNullOrBlank()) add("query")
    }
}

@Serializable
data class ConditionExpressionConfig(
    val left: String? = null,
    val operator: String? = null,
    val right: String? = null
)

@Serializable
data class ConditionNodeConfig(
    val conditions: List<ConditionExpressionConfig>? = null,
    val logic: String? = null,
    val trueHandle: String? = null,
    val falseHandle: String? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        if (conditions.isNullOrEmpty()) add("conditions")
    }
}

@Serializable
data class LoopNodeConfig(
    val inputArrayPath: String? = null,
    val itemAlias: String? = null,
    val loopBodyEntryNodeKey: String? = null,
    val maxIterations: Int? = null,
    val collectOutputKey: String? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        if (inputArrayPath.isNullOrBlank()) add("inputArrayPath")
        if (loopBodyEntryNodeKey.isNullOrBlank()) add("loopBodyEntryNodeKey")
    }
}

@Serializable
data class CodeNodeConfig(
    val language: String? = null,
    val source: String? = null,
    val timeoutMs: Long? = null,
    val outputKey: String? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        if (language.isNullOrBlank()) add("language")
        if (source.isNullOrBlank()) add("source")
    }
}

@Serializable
data class HttpRequestNodeConfig(
    val method: String? = null,
    val url: String? = null,
    val headers: Map<String, String>? = null,
    val secretHeaders: Map<String, String>? = null,
    val body: JsonElement? = null,
    val timeoutMs: Long? = null,
    val outputKey: String? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        if (method.isNullOrBlank()) add("method")
        if (url.isNullOrBlank()) add("url")
    }
}

@Serializable
data class DataTransformMappingConfig(
    val targetKey: String? = null,
    val expression: String? = null
)

@Serializable
data class DataTransformNodeConfig(
    val mappings: List<DataTransformMappingConfig>? = null,
    val template: String? = null,
    val outputKey: String? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        // mappings 與 template 至少擇一
        if (mappings.isNullOrEmpty() && template.isNullOrBlank()) add("mappings")
    }
}

/**
 * NodeType -> config 反序列化的映射入口。
 * 嚴格模式（ignoreUnknownKeys = false）：未知欄位與型別錯誤一律拋 [kotlinx.serialization.SerializationException]。
 */
object NodeConfigRegistry {

    private val strictJson = Json { ignoreUnknownKeys = false }

    fun parse(type: NodeType, config: JsonObject): NodeConfig = when (type) {
        NodeType.TRIGGER -> strictJson.decodeFromJsonElement<TriggerNodeConfig>(config)
        NodeType.LLM_ASSISTANT -> strictJson.decodeFromJsonElement<LlmAssistantNodeConfig>(config)
        NodeType.TOOL -> strictJson.decodeFromJsonElement<ToolNodeConfig>(config)
        NodeType.MCP_SERVER -> strictJson.decodeFromJsonElement<McpServerNodeConfig>(config)
        NodeType.KNOWLEDGE_RAG -> strictJson.decodeFromJsonElement<KnowledgeRagNodeConfig>(config)
        NodeType.CONDITION -> strictJson.decodeFromJsonElement<ConditionNodeConfig>(config)
        NodeType.LOOP -> strictJson.decodeFromJsonElement<LoopNodeConfig>(config)
        NodeType.CODE -> strictJson.decodeFromJsonElement<CodeNodeConfig>(config)
        NodeType.HTTP_REQUEST -> strictJson.decodeFromJsonElement<HttpRequestNodeConfig>(config)
        NodeType.DATA_TRANSFORM -> strictJson.decodeFromJsonElement<DataTransformNodeConfig>(config)
    }
}
```

**Step 4: 執行測試確認通過**

```bash
./gradlew test --tests "tw.zipe.bastpartner.dto.workflow.config.NodeConfigTest" -Dorg.gradle.daemon=false
```
預期：全部 PASS。

**Step 5: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/dto/workflow/ bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/dto/workflow/
git commit -m "新增(workflow): 節點 config 強型別 DTO 與序列化契約"
```

---

## Task 2: save 時驗型別、switchStatus 啟用時驗必填

**Files:**
- Modify: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/WorkflowService.kt`
- Modify: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/enumerate/AppMessage.kt`（`WORKFLOW_NODE_LIMIT_EXCEEDED` 之後，約 line 95）
- Modify: `bestpartner-service/src/main/resources/messages/messages_en_US.properties`
- Modify: `bestpartner-service/src/main/resources/messages/messages_zh_TW.properties`
- Test: `bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/WorkflowServiceTest.kt`（追加測試）

**Step 1: 寫失敗測試**

追加到 `WorkflowServiceTest`（沿用既有 `node()`/`saveRequest` helper 慣例，實作時先閱讀該檔既有 helper 簽名並比照使用；`touchedWorkflowIds` 記得登記）：

```kotlin
@Test
fun `save 拒絕含未知欄位的節點 config`() {
    val req = WorkflowSaveRequestDTO().apply {
        name = "config-驗證-未知欄位"
        nodes = listOf(node("n1", NodeType.LLM_ASSISTANT) {
            put("llmId", "llm-1")
            put("bogusField", "x")
        })
        edges = emptyList()
    }
    val ex = assertThrows(ServiceException::class.java) { workflowService.save(req) }
    assertTrue(ex.message!!.contains("n1"))
}

@Test
fun `save 允許必填缺席的草稿 config`() {
    val req = WorkflowSaveRequestDTO().apply {
        name = "config-驗證-草稿"
        nodes = listOf(node("n1", NodeType.LLM_ASSISTANT) { put("systemPrompt", "hi") })
        edges = emptyList()
    }
    val dto = workflowService.save(req)   // 不應拋例外
    touchedWorkflowIds.add(dto.id!!)
}

@Test
fun `switchStatus 啟用時擋下缺必填的節點並指出欄位`() {
    val req = WorkflowSaveRequestDTO().apply {
        name = "config-驗證-啟用"
        nodes = listOf(
            node("t1", NodeType.TRIGGER) { put("triggerType", "MANUAL") },
            node("n1", NodeType.LLM_ASSISTANT) { put("systemPrompt", "hi") }  // 缺 llmId
        )
        edges = listOf(edge("t1", "n1"))
    }
    val dto = workflowService.save(req)
    touchedWorkflowIds.add(dto.id!!)
    val ex = assertThrows(ServiceException::class.java) { workflowService.switchStatus(dto.id!!, true) }
    assertTrue(ex.message!!.contains("n1"))
    assertTrue(ex.message!!.contains("llmId"))
}

@Test
fun `switchStatus 啟用時所有節點 config 完整則成功`() {
    val req = WorkflowSaveRequestDTO().apply {
        name = "config-驗證-啟用成功"
        nodes = listOf(
            node("t1", NodeType.TRIGGER) { put("triggerType", "MANUAL") },
            node("n1", NodeType.LLM_ASSISTANT) { put("llmId", "llm-1") }
        )
        edges = listOf(edge("t1", "n1"))
    }
    val dto = workflowService.save(req)
    touchedWorkflowIds.add(dto.id!!)
    val activated = workflowService.switchStatus(dto.id!!, true)
    assertEquals(tw.zipe.bastpartner.enumerate.WorkflowStatus.ACTIVE, activated.status)
}
```

**Step 2: 執行測試確認失敗**

```bash
./gradlew test --tests "tw.zipe.bastpartner.service.WorkflowServiceTest" -Dorg.gradle.daemon=false
```
預期：新增的 4 個測試中「save 拒絕未知欄位」「switchStatus 擋缺必填」FAIL（現行完全不驗 config）。

**Step 3: 加 AppMessage 與 i18n 訊息**

`AppMessage.kt` 在 `WORKFLOW_NODE_LIMIT_EXCEEDED` 後追加：

```kotlin
WORKFLOW_NODE_CONFIG_INVALID("workflow.node.config.invalid"),
WORKFLOW_NODE_CONFIG_REQUIRED_MISSING("workflow.node.config.required.missing"),
```

`messages_en_US.properties`（workflow 區塊尾端）：

```properties
workflow.node.config.invalid=Config of node {0} is invalid: {1}
workflow.node.config.required.missing=Node {0} is missing required config fields: {1}
```

`messages_zh_TW.properties`：

```properties
workflow.node.config.invalid=節點 {0} 的設定內容格式錯誤：{1}
workflow.node.config.required.missing=節點 {0} 缺少必填設定欄位：{1}
```

> 實作時先確認兩份 properties 既有格式（是否已用 unicode escape 或 UTF-8 原文），比照既有 workflow.* 條目寫法。

**Step 4: 修改 WorkflowService**

`save()` 中 `validateGraph(req.nodes, req.edges)` 之後加一行：

```kotlin
validateNodeConfigs(req.nodes)
```

`switchStatus()` 的 `if (active)` 區塊內（`detectCycle` 檢查之後）追加：

```kotlin
nodes.forEach { n ->
    val configObj = mapToJsonObject(n.config) ?: JsonObject(emptyMap())
    val parsed = runCatching { NodeConfigRegistry.parse(n.type, configObj) }
        .getOrElse { e ->
            throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_INVALID, n.nodeKey, e.message.orEmpty())
        }
    val missing = parsed.missingRequiredFields()
    if (missing.isNotEmpty()) {
        throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_REQUIRED_MISSING, n.nodeKey, missing.joinToString(", "))
    }
}
```

新增 private 函式（放在 `validateGraph` 之後）：

```kotlin
/**
 * 節點 config 型別驗證（DRAFT 亦執行）：依 NodeType 嚴格反序列化，
 * 型別錯誤或未知欄位一律拒絕；必填缺席放行（啟用時才驗，見 [switchStatus]）。
 */
private fun validateNodeConfigs(nodes: List<WorkflowNodeDTO>) {
    nodes.forEach { n ->
        val type = n.type ?: return@forEach   // type 缺席由既有 save 流程處理
        runCatching { NodeConfigRegistry.parse(type, n.config) }
            .onFailure { e ->
                throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_INVALID, n.nodeKey, e.message.orEmpty())
            }
    }
}
```

import 追加：`tw.zipe.bastpartner.dto.workflow.config.NodeConfigRegistry`。

> 注意：`runCatching` 會吞 `ServiceException` 以外的錯，此處 `parse` 只會拋 SerializationException 系列，直接包裝即可；不要讓 `onFailure` 內拋出的 ServiceException 再被外層 runCatching 捕捉（`onFailure` 內拋出即中斷，行為正確）。

**Step 5: 執行測試確認通過**

```bash
./gradlew test --tests "tw.zipe.bastpartner.service.WorkflowServiceTest" -Dorg.gradle.daemon=false
```
預期：全部 PASS（含既有測試——若既有測試的 config 含未知欄位而變紅，屬契約收緊的預期影響，修正該測試資料使用合法欄位）。

**Step 6: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/WorkflowService.kt \
        bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/enumerate/AppMessage.kt \
        bestpartner-service/src/main/resources/messages/ \
        bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/WorkflowServiceTest.kt
git commit -m "新增(workflow): 存檔驗節點 config 型別、啟用驗必填欄位"
```

---

## Task 3: ToolSchemaGenerator 反射產生 settingSchema

**Files:**
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/tool/config/ToolConfigField.kt`
- Create: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/util/ToolSchemaGenerator.kt`
- Modify: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/tool/config/Google.kt`
- Modify: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/tool/config/Tavily.kt`
- Test: `bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/util/ToolSchemaGeneratorTest.kt`

**Step 1: 寫失敗測試**

```kotlin
package tw.zipe.bastpartner.util

import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ToolSchemaGenerator 反射契約測試：以 Google config class 為對象，
 * 驗證型別映射、nullable -> required、@ToolConfigField -> sensitive/description。
 */
class ToolSchemaGeneratorTest {

    @Test
    fun `Google config 產生正確 schema`() {
        val schema = ToolSchemaGenerator.generate("tw.zipe.bastpartner.tool.config.Google")!!

        val apiKey = schema["apiKey"]!!.jsonObject
        assertEquals("string", apiKey["type"]!!.jsonPrimitive.content)
        assertEquals(true, apiKey["required"]!!.jsonPrimitive.booleanOrNull)
        assertEquals(true, apiKey["sensitive"]!!.jsonPrimitive.booleanOrNull)

        val siteRestrict = schema["siteRestrict"]!!.jsonObject
        assertEquals("boolean", siteRestrict["type"]!!.jsonPrimitive.content)
        assertEquals(false, siteRestrict["required"]!!.jsonPrimitive.booleanOrNull)

        val timeout = schema["timeout"]!!.jsonObject
        assertEquals("integer", timeout["type"]!!.jsonPrimitive.content)
        assertEquals(true, timeout["required"]!!.jsonPrimitive.booleanOrNull)

        // 欄位順序 = 建構子參數順序
        assertEquals(listOf("apiKey", "csi", "siteRestrict", "includeImages",
            "timeout", "maxRetries", "logRequests", "logResponses"), schema.keys.toList())
    }

    @Test
    fun `class 不存在回傳 null`() {
        assertNull(ToolSchemaGenerator.generate("tw.zipe.bastpartner.tool.config.NotExist"))
    }
}
```

**Step 2: 執行測試確認失敗**

```bash
./gradlew test --tests "tw.zipe.bastpartner.util.ToolSchemaGeneratorTest" -Dorg.gradle.daemon=false
```
預期：編譯失敗（`ToolSchemaGenerator` 不存在）。

**Step 3: 實作**

`ToolConfigField.kt`：

```kotlin
package tw.zipe.bastpartner.tool.config

/**
 * 標註於工具 config data class 的建構子參數，補充 settingSchema 的 UI 提示。
 * required 與型別由反射自動推導（nullable -> 選填），此 annotation 僅補人工無法推導的資訊。
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ToolConfigField(
    val description: String = "",
    val sensitive: Boolean = false
)
```

`ToolSchemaGenerator.kt`：

```kotlin
package tw.zipe.bastpartner.util

import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import tw.zipe.bastpartner.tool.config.ToolConfigField

/**
 * 由工具 configObjectPath 指向的 data class 反射產生結構化 settingSchema，
 * 供前端動態渲染設定表單。事實來源為 config class 本身，schema 不落地不手填。
 *
 * 輸出格式（每欄位）：{ "type": "...", "required": bool, "sensitive"?: true, "description"?: "..." }
 * 欄位順序 = primary constructor 參數順序。
 */
object ToolSchemaGenerator {

    /** class 不存在或無 primary constructor 時回傳 null（前端 fallback raw JSON editor） */
    fun generate(configObjectPath: String): JsonObject? {
        val kClass = runCatching { Class.forName(configObjectPath).kotlin }.getOrNull() ?: return null
        val ctor = kClass.primaryConstructor ?: return null
        return buildJsonObject {
            ctor.parameters.forEach { param ->
                val name = param.name ?: return@forEach
                val annotation = param.findAnnotation<ToolConfigField>()
                putJsonObject(name) {
                    put("type", jsonType(param.type))
                    put("required", !param.type.isMarkedNullable)
                    if (annotation?.sensitive == true) put("sensitive", true)
                    annotation?.description?.takeIf { it.isNotBlank() }?.let { put("description", it) }
                }
            }
        }
    }

    private fun jsonType(type: KType): String = when (type.classifier) {
        Int::class, Long::class, Short::class -> "integer"
        Double::class, Float::class -> "number"
        Boolean::class -> "boolean"
        List::class, Set::class -> "array"
        else -> "string"
    }
}
```

`Google.kt` 加註（Tavily.kt 比照，實作時先讀該檔欄位，API key 類欄位標 `sensitive = true`）：

```kotlin
package tw.zipe.bastpartner.tool.config

data class Google(
    @ToolConfigField(description = "Google Custom Search API 金鑰", sensitive = true)
    val apiKey: String,
    @ToolConfigField(description = "Custom Search Engine ID")
    val csi: String,
    val siteRestrict: Boolean?,
    val includeImages: Boolean?,
    @ToolConfigField(description = "逾時毫秒數")
    val timeout: Long,
    val maxRetries: Int?,
    val logRequests: Boolean?,
    val logResponses: Boolean?
)
```

**Step 4: 執行測試確認通過**

```bash
./gradlew test --tests "tw.zipe.bastpartner.util.ToolSchemaGeneratorTest" -Dorg.gradle.daemon=false
```
預期：PASS。

**Step 5: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/tool/config/ \
        bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/util/ToolSchemaGenerator.kt \
        bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/util/ToolSchemaGeneratorTest.kt
git commit -m "新增(tool): ToolSchemaGenerator 反射產生工具設定 schema"
```

---

## Task 4: `/llm/tool/list`、`/llm/tool/get` 回傳 settingSchema

**Files:**
- Modify: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/dto/ToolDTO.kt`
- Modify: `bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/ToolService.kt`（`getTools()` line 48、`getTool()` line 53）
- Test: `bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/ToolServiceTest.kt`（追加）

**Step 1: 寫失敗測試**

先閱讀 `ToolServiceTest.kt` 既有慣例（是否 `@QuarkusTest`、測試資料如何準備）。追加測試：對 DB 中已存在、`configObjectPath` 指向 `Google` 的內建工具（init-data 有 Google 搜尋工具；若無，測試內先 `registerTool` 建立並於 `@AfterEach` 清除）：

```kotlin
@Test
fun `getTool 回傳結構化 settingSchema`() {
    val tool = toolService.getTool(googleToolId)
    val schema = tool.settingSchema!!
    assertTrue(schema.containsKey("apiKey"))
    assertEquals("string", schema["apiKey"]!!.jsonObject["type"]!!.jsonPrimitive.content)
}

@Test
fun `getTools 清單每筆皆帶 settingSchema 或 null`() {
    val tools = toolService.getTools()
    val google = tools.first { it.name == "Google搜尋引擎" }   // 名稱以 init-data 實際值為準
    assertNotNull(google.settingSchema)
}
```

**Step 2: 執行測試確認失敗**

```bash
./gradlew test --tests "tw.zipe.bastpartner.service.ToolServiceTest" -Dorg.gradle.daemon=false
```
預期：編譯失敗（`settingSchema` 不存在於 ToolDTO）。

**Step 3: 實作**

`ToolDTO.kt` 追加欄位（`settingContent` 之後）：

```kotlin
var settingSchema: kotlinx.serialization.json.JsonObject? = null,
```

`ToolService.kt`：

```kotlin
/**
 * 取得所有工具清單（含反射產生的 settingSchema，無 config class 者為 null）
 */
fun getTools() = llmToolRepository.findByCondition(null).onEach { tool ->
    tool.settingSchema = tool.configObjectPath?.let { ToolSchemaGenerator.generate(it) }
}
```

`getTool()` 在既有 `settingArgs` 邏輯後追加同樣一行（保留 `settingArgs` 以維持既有相容，`settingSchema` 為新契約）：

```kotlin
tool.settingSchema = tool.configObjectPath?.let { path -> ToolSchemaGenerator.generate(path) }
```

> 前置檢查：確認 `llmToolRepository.findByCondition(null)` 的查詢投影包含 `configObjectPath`；若未包含，先補上該欄位再實作。

**Step 4: 執行測試確認通過**

```bash
./gradlew test --tests "tw.zipe.bastpartner.service.ToolServiceTest" -Dorg.gradle.daemon=false
```
預期：PASS。

**Step 5: Commit**

```bash
git add bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/dto/ToolDTO.kt \
        bestpartner-service/src/main/kotlin/tw/zipe/bastpartner/service/ToolService.kt \
        bestpartner-service/src/test/kotlin/tw/zipe/bastpartner/service/ToolServiceTest.kt
git commit -m "新增(tool): 工具查詢 API 回傳反射產生的 settingSchema"
```

---

## Task 5: 前端 schema 型別與 tool API 擴充

**Files:**
- Create: `bestpartner-ui/src/types/toolSchema.ts`
- Modify: `bestpartner-ui/src/api/tool.ts`
- Test: 型別層免測；API 函式由 Task 7 的元件測試覆蓋（mock http）

**Step 1: 實作 `types/toolSchema.ts`**

```typescript
/** 後端 ToolSchemaGenerator 反射產生的單一設定欄位描述 */
export interface ToolSettingFieldSchema {
  type: 'string' | 'integer' | 'number' | 'boolean' | 'array'
  required: boolean
  sensitive?: boolean
  description?: string
}

/** 工具 settingSchema：欄位名 -> 欄位描述；null 代表該工具無 config class */
export type ToolSettingSchema = Record<string, ToolSettingFieldSchema>
```

**Step 2: 擴充 `api/tool.ts`**

```typescript
import type { ToolSettingSchema } from '../types/toolSchema'

/** 後端工具 DTO（下拉與設定表單所需欄位） */
interface ToolDTO {
  id?: string
  name?: string
  settingSchema?: ToolSettingSchema | null
  settingId?: string
  alias?: string
  settingContent?: string
}

/** 取得單一工具詳情（含 settingSchema） */
export async function getTool(id: string): Promise<ToolDTO> {
  const res = await http.post<ApiResponse<ToolDTO>>('/llm/tool/get', { id })
  return res.data.data ?? {}
}

/** 建立工具設定（id + alias），回傳含 settingId 的 DTO */
export async function saveToolSetting(id: string, alias: string): Promise<ToolDTO> {
  const res = await http.post<ApiResponse<ToolDTO>>('/llm/tool/saveSetting', { id, alias })
  return res.data.data ?? {}
}

/** 更新工具設定內容（settingId + settingContent JSON 字串） */
export async function updateToolSetting(settingId: string, settingContent: string): Promise<void> {
  await http.post<ApiResponse<unknown>>('/llm/tool/updateSetting', { settingId, settingContent })
}
```

（既有 `listTools()` 保持不動。）

**Step 3: 型別檢查**

```bash
cd bestpartner-ui
npx vue-tsc -b
```
預期：無錯誤。

**Step 4: Commit**

```bash
git add bestpartner-ui/src/types/toolSchema.ts bestpartner-ui/src/api/tool.ts
git commit -m "新增(bestpartner-ui): 工具 settingSchema 型別與設定 API"
```

---

## Task 6: SettingSchemaForm 動態表單元件

**Files:**
- Create: `bestpartner-ui/src/components/inspector/forms/SettingSchemaForm.vue`
- Test: `bestpartner-ui/src/components/inspector/forms/__tests__/SettingSchemaForm.test.ts`

**Step 1: 寫失敗測試**

先讀 `__tests__/LlmAssistantForm.test.ts` 比照 mount / 斷言慣例。測試要點：

```typescript
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import SettingSchemaForm from '../SettingSchemaForm.vue'
import type { ToolSettingSchema } from '../../../../types/toolSchema'

const schema: ToolSettingSchema = {
  apiKey: { type: 'string', required: true, sensitive: true, description: 'API 金鑰' },
  timeout: { type: 'integer', required: true },
  siteRestrict: { type: 'boolean', required: false },
}

describe('SettingSchemaForm', () => {
  it('依 schema 渲染欄位：sensitive 為 password、integer 為 number、boolean 為 checkbox', () => {
    const wrapper = mount(SettingSchemaForm, { props: { schema, modelValue: {} } })
    expect(wrapper.find('[data-test="field-apiKey"]').attributes('type')).toBe('password')
    expect(wrapper.find('[data-test="field-timeout"]').attributes('type')).toBe('number')
    expect(wrapper.find('[data-test="field-siteRestrict"]').attributes('type')).toBe('checkbox')
  })

  it('必填欄位 label 顯示星號', () => {
    const wrapper = mount(SettingSchemaForm, { props: { schema, modelValue: {} } })
    expect(wrapper.find('[data-test="label-apiKey"]').text()).toContain('*')
    expect(wrapper.find('[data-test="label-siteRestrict"]').text()).not.toContain('*')
  })

  it('輸入時 emit 型別正確的 modelValue', async () => {
    const wrapper = mount(SettingSchemaForm, { props: { schema, modelValue: {} } })
    await wrapper.find('[data-test="field-apiKey"]').setValue('sk-xxx')
    await wrapper.find('[data-test="field-timeout"]').setValue('3000')
    await wrapper.find('[data-test="field-siteRestrict"]').setValue(true)
    const events = wrapper.emitted('update:modelValue')!
    const last = events[events.length - 1][0] as Record<string, unknown>
    expect(last.apiKey).toBe('sk-xxx')
    expect(last.timeout).toBe(3000)          // number，非字串
    expect(last.siteRestrict).toBe(true)
  })

  it('顯示 description 說明文字', () => {
    const wrapper = mount(SettingSchemaForm, { props: { schema, modelValue: {} } })
    expect(wrapper.text()).toContain('API 金鑰')
  })
})
```

**Step 2: 執行測試確認失敗**

```bash
npm run test -- SettingSchemaForm
```
預期：FAIL（元件不存在）。

**Step 3: 實作元件**

樣式沿用既有 forms 的 `.field` / `.text-input` CSS 變數慣例（照抄 `ToolForm.vue` 的 style 區塊基底）：

```vue
<script setup lang="ts">
import { computed } from 'vue'
import type { ToolSettingSchema } from '../../../types/toolSchema'

const props = defineProps<{
  schema: ToolSettingSchema
  modelValue: Record<string, unknown>
}>()
const emit = defineEmits<{ 'update:modelValue': [value: Record<string, unknown>] }>()

const fields = computed(() => Object.entries(props.schema))

function inputType(field: { type: string; sensitive?: boolean }): string {
  if (field.sensitive) return 'password'
  if (field.type === 'integer' || field.type === 'number') return 'number'
  if (field.type === 'boolean') return 'checkbox'
  return 'text'
}

function onInput(name: string, event: Event) {
  const el = event.target as HTMLInputElement
  const field = props.schema[name]
  let value: unknown
  if (field.type === 'boolean') value = el.checked
  else if (field.type === 'integer' || field.type === 'number') {
    value = el.value === '' ? undefined : Number(el.value)
  } else value = el.value
  const next = { ...props.modelValue }
  if (value === undefined || value === '') delete next[name]
  else next[name] = value
  emit('update:modelValue', next)
}
</script>

<template>
  <div class="form">
    <div v-for="[name, field] in fields" :key="name" class="field">
      <label :data-test="`label-${name}`">
        {{ name }}<span v-if="field.required" class="required">*</span>
        <span v-if="field.description" class="hint">{{ field.description }}</span>
      </label>
      <input
        :data-test="`field-${name}`"
        :type="inputType(field)"
        :checked="field.type === 'boolean' ? Boolean(modelValue[name]) : undefined"
        :value="field.type === 'boolean' ? undefined : ((modelValue[name] as string | number | undefined) ?? '')"
        class="text-input"
        :class="{ checkbox: field.type === 'boolean' }"
        @input="onInput(name, $event)"
        @change="field.type === 'boolean' && onInput(name, $event)"
      />
    </div>
  </div>
</template>

<style scoped>
.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field label {
  font-size: 11px;
  color: var(--wf-text-dim, #8a8a95);
}

.required {
  color: var(--wf-accent, #ff6a54);
  margin-left: 2px;
}

.hint {
  margin-left: 6px;
  opacity: 0.75;
}

.text-input {
  width: 100%;
  box-sizing: border-box;
  padding: 6px 8px;
  font-size: 12.5px;
  font-family: inherit;
  color: var(--wf-text, #e7e7ec);
  background: var(--wf-input, #0f0f13);
  border: 1px solid var(--wf-border, #29292f);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.15s;
}

.text-input.checkbox {
  width: auto;
  align-self: flex-start;
}

.text-input:focus {
  border-color: var(--wf-accent, #ff6a54);
}
</style>
```

**Step 4: 執行測試確認通過**

```bash
npm run test -- SettingSchemaForm
```
預期：PASS。

**Step 5: Commit**

```bash
git add bestpartner-ui/src/components/inspector/forms/SettingSchemaForm.vue \
        bestpartner-ui/src/components/inspector/forms/__tests__/SettingSchemaForm.test.ts
git commit -m "新增(bestpartner-ui): SettingSchemaForm 依 schema 動態渲染設定表單"
```

---

## Task 7: ToolForm 整合——選工具後以 schema 表單建立設定

**Files:**
- Modify: `bestpartner-ui/src/components/inspector/forms/ToolForm.vue`
- Test: `bestpartner-ui/src/components/inspector/forms/__tests__/ToolForm.test.ts`（若已存在則追加；先讀既有測試）

**行為規格：**
1. 選擇工具後呼叫 `getTool(toolId)` 取得 `settingSchema`。
2. `settingSchema` 非 null → 顯示「建立工具設定」摺疊區塊：alias 輸入框 + `SettingSchemaForm` + 「建立設定」按鈕。
3. 按下按鈕 → `saveToolSetting(toolId, alias)` 取得 `settingId` → `updateToolSetting(settingId, JSON.stringify(values))` → 將 `settingId` 自動填入既有 `toolSettingId` 欄位並 `emitConfig()`。
4. `settingSchema` 為 null → 區塊不顯示，維持現狀（手動輸入 toolSettingId）。
5. API 失敗 → 區塊內顯示錯誤文字，不清空使用者輸入。

**Step 1: 寫失敗測試**

mock `../../../api/tool`（`vi.mock`），要點：

```typescript
it('選擇有 schema 的工具後顯示設定建立區塊', async () => { /* getTool 回 settingSchema */ })
it('選擇無 schema 的工具不顯示設定區塊', async () => { /* getTool 回 settingSchema: null */ })
it('建立設定成功後 toolSettingId 自動帶入並 emit config', async () => {
  // saveToolSetting 回 { settingId: 'setting-1' }，斷言 update:config 事件的 toolSettingId
})
```

**Step 2: 執行測試確認失敗**

```bash
npm run test -- ToolForm
```

**Step 3: 實作**

在 `ToolForm.vue` 現有結構上加：`watch(toolId)` → `getTool`；區塊條件渲染 `v-if="settingSchema"`；`settingValues = ref<Record<string, unknown>>({})` 綁 `SettingSchemaForm`；建立流程如行為規格。錯誤處理用 `try/catch` 設 `errorMsg` ref。

**Step 4: 執行測試確認通過**

```bash
npm run test -- ToolForm
npx vue-tsc -b
```
預期：全 PASS、型別無錯。

**Step 5: 全前端測試回歸**

```bash
npm run test
```
預期：全 PASS。

**Step 6: Commit**

```bash
git add bestpartner-ui/src/components/inspector/forms/ToolForm.vue \
        bestpartner-ui/src/components/inspector/forms/__tests__/
git commit -m "新增(bestpartner-ui): ToolForm 依 settingSchema 建立工具設定"
```

---

## Task 8: 後端全量建置驗證

**Step 1: 後端建置（跳過測試的正式建法）**

```bash
cd bestpartner-service
./gradlew clean build -x test -Dquarkus.package.type=uber-jar -Dorg.gradle.daemon=false -Dquarkus.profile=dev
```
預期：BUILD SUCCESSFUL。

**Step 2: 相關測試全跑一次**

```bash
./gradlew test --tests "tw.zipe.bastpartner.dto.workflow.config.*" \
               --tests "tw.zipe.bastpartner.util.ToolSchemaGeneratorTest" \
               --tests "tw.zipe.bastpartner.service.WorkflowServiceTest" \
               --tests "tw.zipe.bastpartner.service.ToolServiceTest" \
               --tests "tw.zipe.bastpartner.resource.WorkflowResourceTest" \
               -Dorg.gradle.daemon=false
```
預期：全 PASS（WorkflowResourceTest 若因 config 收緊變紅，修正其測試資料為合法欄位）。

**Step 3:（無 commit，驗證性任務）**

---

## Task 9: 文件同步（強制）

**REQUIRED SUB-SKILL:** 呼叫 `documentation-sync` skill（依 `.claude/rules/documentation-update-policy.md`，未執行不得宣告完成）。

**必須涵蓋的更新清單：**

| 文件 | 更新內容 |
|------|---------|
| `.claude/rules/api-endpoints.md` | `/llm/tool/list`、`/llm/tool/get` 說明補「回傳 settingSchema」；workflow save/switchStatus 說明補 config 驗證 |
| `.claude/rules/architecture-and-packages.md` | 目錄樹補 `dto/workflow/config/`、`util/ToolSchemaGenerator.kt`、`tool/config/ToolConfigField.kt` |
| `docs/workflow-engine/system-design.md` | §2 開頭加註：「config schema 事實來源為 `dto/workflow/config/NodeConfig.kt`，本節為對照說明」 |
| `docs-site/docs/api/` 工具相關頁 | settingSchema 欄位說明與範例 |
| `docs-site/docs/features/tools.md` | 工具設定表單機制說明 |
| `docs/api-test-plan.md` | 新驗證行為的測試案例（save 400、啟用擋必填、settingSchema 回傳） |
| `docs/postman/basepartner.postman_collection.json` | tool get/list 範例 response 更新 |
| `docs/plans/2026-07-04-config-schema-contract-design.md` | 狀態改「已實作」 |

**Commit:**

```bash
git commit -m "文件: 同步節點 config 契約與 settingSchema 相關文件"
```

---

## 已知風險與注意事項

1. **契約收緊的相容性**：既有 DB 中若有帶未知欄位的 workflow node config，save（整張覆寫）時會被拒。前端各 form 的 `emitConfig` 以 spread 保留未知鍵值——若使用者曾用 `JsonConfigEditor` 塞入任意鍵，重新存檔會 400（訊息明確指出 nodeKey 與原因），屬預期行為。
2. **`@QuarkusTest` 需 PostgreSQL**：Task 2、4、8 的整合測試需本地 DB 運行；跑不起來時先啟動 `docs/docker` 的 mysql/postgres compose（實際為 PostgreSQL，見 configuration-and-profiles.md）。
3. **`findByCondition` 投影**：Task 4 前置檢查 repository 查詢是否含 `configObjectPath`，缺了 schema 會恆為 null 而測試會抓到。
4. **舊 `settingArgs` 不移除**：`getTool` 既有的 `generateFieldJson` 輸出保留（相容既有呼叫方），`settingSchema` 為新契約；日後確認無人使用 `settingArgs` 再另開移除任務。
5. **kotlinx 嚴格模式的錯誤訊息**：`SerializationException.message` 會包含內部路徑描述，直接放入 `{1}` 佔位符即可，前端原樣顯示。
