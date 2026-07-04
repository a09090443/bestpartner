package tw.zipe.bastpartner.dto.workflow.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.enumerate.TriggerType

/**
 * Workflow 節點 config 的強型別契約。
 *
 * 事實來源：本檔案即各 NodeType config schema 的唯一事實來源，
 * `docs/workflow-engine/system-design.md` §2 為對照說明文件。
 *
 * 欄位一律 nullable 以支援兩段式驗證：save（DRAFT）僅驗型別與未知欄位，
 * 必填檢核（[NodeConfig.missingRequiredFields]）於 switchStatus 啟用時執行。
 *
 * @author Gary
 * @created 2026/7/4
 */
sealed interface NodeConfig {
    /** 回傳缺席的必填欄位名稱；空清單代表滿足啟用條件 */
    fun missingRequiredFields(): List<String>
}

@Serializable
data class TriggerNodeConfig(
    val triggerType: TriggerType? = null,
    val inputSchema: JsonObject? = null,
    val webhook: JsonObject? = null,
    val cron: JsonObject? = null
) : NodeConfig {
    override fun missingRequiredFields() = buildList {
        if (triggerType == null) add("triggerType")
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
        if (mappings.isNullOrEmpty() && template.isNullOrBlank()) add("mappings|template")
    }
}

/**
 * NodeType -> config 反序列化的映射入口。
 *
 * 嚴格度說明（ignoreUnknownKeys = false）：
 * - 拒絕未知欄位與結構性型別錯誤（如字串給 List 欄位、非法 enum 值），拋 [kotlinx.serialization.SerializationException]。
 * - 注意：kotlinx tree decoding 對 scalar 有寬鬆轉型——數字/布林形式的字串
 *   （如 `topK: "5"`、`enableMemory: "true"`）會被轉型接受，不會拋例外。
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
