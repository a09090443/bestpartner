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
