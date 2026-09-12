package tw.zipe.bastpartner.dto.workflow.config

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.enumerate.TriggerType

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
        }
        val parsed = NodeConfigRegistry.parse(NodeType.LLM_ASSISTANT, config)
        assertTrue(parsed is LlmAssistantNodeConfig)
        assertEquals(emptyList<String>(), parsed.missingRequiredFields())
    }

    @Test
    fun `LLM_ASSISTANT 已廢欄位 toolIds 被視為未知欄位而被拒`() {
        // 工具/MCP/Skill 已改為獨立節點掛載，LLM 節點 config 不再接受這些欄位
        val config = buildJsonObject {
            put("llmId", "llm-uuid")
            putJsonArray("toolIds") { add(JsonPrimitive("tool-1")) }
        }
        assertThrows(SerializationException::class.java) {
            NodeConfigRegistry.parse(NodeType.LLM_ASSISTANT, config)
        }
    }

    @Test
    fun `SKILL 合法 config 解析成功且無缺必填`() {
        val config = buildJsonObject { put("skillId", "skill-1") }
        val parsed = NodeConfigRegistry.parse(NodeType.SKILL, config)
        assertTrue(parsed is SkillNodeConfig)
        assertEquals(emptyList<String>(), parsed.missingRequiredFields())
    }

    @Test
    fun `PROMPT 合法 config 解析成功且無缺必填`() {
        val config = buildJsonObject {
            put("prompt", "請用繁體中文回答：{{trigger.question}}")
            put("outputKey", "askText")
        }
        val parsed = NodeConfigRegistry.parse(NodeType.PROMPT, config)
        assertTrue(parsed is PromptNodeConfig)
        assertEquals(emptyList<String>(), parsed.missingRequiredFields())
    }

    @Test
    fun `PROMPT 缺 prompt 可解析但回報缺必填`() {
        val config = buildJsonObject { put("outputKey", "askText") }
        val parsed = NodeConfigRegistry.parse(NodeType.PROMPT, config)
        assertEquals(listOf("prompt"), parsed.missingRequiredFields())
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
    fun `型別錯誤被拒 - conditions 給字串而非陣列`() {
        val config = buildJsonObject {
            put("conditions", "not-an-array")
        }
        assertThrows(SerializationException::class.java) {
            NodeConfigRegistry.parse(NodeType.CONDITION, config)
        }
    }

    @Test
    fun `scalar 寬鬆轉型 - topK 給字串形式數字仍解析成功`() {
        // 釘住 kotlinx tree decoding 的寬鬆轉型行為：升級 kotlinx 若行為改變，此測試會失敗提醒
        val config = buildJsonObject {
            put("knowledgeId", "kb-1")
            put("embeddingModelId", "emb-1")
            put("query", "hi")
            put("topK", "5")
        }
        val parsed = NodeConfigRegistry.parse(NodeType.KNOWLEDGE_RAG, config)
        assertTrue(parsed is KnowledgeRagNodeConfig)
        assertEquals(5, (parsed as KnowledgeRagNodeConfig).topK)
    }

    @Test
    fun `TRIGGER triggerType 合法 enum 值解析成功`() {
        val config = buildJsonObject { put("triggerType", "MANUAL") }
        val parsed = NodeConfigRegistry.parse(NodeType.TRIGGER, config)
        assertTrue(parsed is TriggerNodeConfig)
        assertEquals(TriggerType.MANUAL, (parsed as TriggerNodeConfig).triggerType)
        assertEquals(emptyList<String>(), parsed.missingRequiredFields())
    }

    @Test
    fun `TRIGGER triggerType 非法 enum 值被拒`() {
        val config = buildJsonObject { put("triggerType", "BOGUS") }
        assertThrows(SerializationException::class.java) {
            NodeConfigRegistry.parse(NodeType.TRIGGER, config)
        }
    }

    @Test
    fun `DATA_TRANSFORM 僅 template 有值時無缺必填`() {
        val config = buildJsonObject { put("template", "{{ input }}") }
        val parsed = NodeConfigRegistry.parse(NodeType.DATA_TRANSFORM, config)
        assertEquals(emptyList<String>(), parsed.missingRequiredFields())
    }

    @Test
    fun `全部 NodeType 空 config 皆可解析`() {
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
        assertEquals(listOf("skillId"), NodeConfigRegistry.parse(NodeType.SKILL, empty).missingRequiredFields())
        assertEquals(listOf("prompt"), NodeConfigRegistry.parse(NodeType.PROMPT, empty).missingRequiredFields())
        // LLM 的提問來源可為連入的 PROMPT 節點（圖層級條件），故 userPrompt 不列為無條件必填
        assertEquals(listOf("llmId"), NodeConfigRegistry.parse(NodeType.LLM_ASSISTANT, empty).missingRequiredFields())
        // knowledgeId 為唯一無條件必填；query/embeddingModelId 僅 pipeline 檢索需要，
        // 作為 LLM 外掛（自動注入）時不需，故不列為無條件必填。
        assertEquals(
            listOf("knowledgeId"),
            NodeConfigRegistry.parse(NodeType.KNOWLEDGE_RAG, empty).missingRequiredFields()
        )
        assertEquals(listOf("conditions"), NodeConfigRegistry.parse(NodeType.CONDITION, empty).missingRequiredFields())
        assertEquals(
            listOf("inputArrayPath", "loopBodyEntryNodeKey"),
            NodeConfigRegistry.parse(NodeType.LOOP, empty).missingRequiredFields()
        )
        assertEquals(listOf("language", "source"), NodeConfigRegistry.parse(NodeType.CODE, empty).missingRequiredFields())
        assertEquals(listOf("method", "url"), NodeConfigRegistry.parse(NodeType.HTTP_REQUEST, empty).missingRequiredFields())
        assertEquals(listOf("mappings|template"), NodeConfigRegistry.parse(NodeType.DATA_TRANSFORM, empty).missingRequiredFields())
    }
}
