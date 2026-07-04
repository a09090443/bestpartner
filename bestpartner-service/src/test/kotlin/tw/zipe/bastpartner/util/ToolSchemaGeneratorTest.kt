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
    fun `Tavily config 產生正確 schema`() {
        val schema = ToolSchemaGenerator.generate("tw.zipe.bastpartner.tool.config.Tavily")!!

        // List 型別映射為 array，nullable -> 選填
        val includeDomains = schema["includeDomains"]!!.jsonObject
        assertEquals("array", includeDomains["type"]!!.jsonPrimitive.content)
        assertEquals(false, includeDomains["required"]!!.jsonPrimitive.booleanOrNull)

        // description 管線：annotation 有給值就要輸出
        val apiKey = schema["apiKey"]!!.jsonObject
        assertEquals(true, apiKey["sensitive"]!!.jsonPrimitive.booleanOrNull)
        assertEquals("Tavily API 金鑰", apiKey["description"]!!.jsonPrimitive.content)

        // 可選性契約：無 annotation 的欄位不得帶 sensitive / description 鍵
        val includeAnswer = schema["includeAnswer"]!!.jsonObject
        assertNull(includeAnswer["sensitive"])
        assertNull(includeAnswer["description"])
    }

    @Test
    fun `class 不存在回傳 null`() {
        assertNull(ToolSchemaGenerator.generate("tw.zipe.bastpartner.tool.config.NotExist"))
    }

    @Test
    fun `無 primary constructor 的 class 回傳 null`() {
        // Kotlin object 宣告沒有 primary constructor，以 generator 自身為穩定測試目標
        assertNull(ToolSchemaGenerator.generate("tw.zipe.bastpartner.util.ToolSchemaGenerator"))
    }
}
