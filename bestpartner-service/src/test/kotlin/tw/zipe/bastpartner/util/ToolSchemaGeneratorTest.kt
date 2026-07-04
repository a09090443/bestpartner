package tw.zipe.bastpartner.util

import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
        assertEquals(
            listOf(
                "apiKey", "csi", "siteRestrict", "includeImages",
                "timeout", "maxRetries", "logRequests", "logResponses"
            ),
            schema.keys.toList()
        )
    }

    @Test
    fun `class 不存在回傳 null`() {
        assertNull(ToolSchemaGenerator.generate("tw.zipe.bastpartner.tool.config.NotExist"))
    }
}
