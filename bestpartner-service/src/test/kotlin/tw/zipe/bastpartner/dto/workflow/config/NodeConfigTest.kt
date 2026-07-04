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
