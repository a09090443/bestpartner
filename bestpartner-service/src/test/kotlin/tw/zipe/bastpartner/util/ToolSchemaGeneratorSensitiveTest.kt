package tw.zipe.bastpartner.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 驗證 sensitiveFields 反射：事實來源為 config class 的 @ToolConfigField(sensitive=true)。
 */
class ToolSchemaGeneratorSensitiveTest {

    @Test
    fun `Google config 僅 apiKey 為敏感`() {
        assertEquals(setOf("apiKey"), ToolSchemaGenerator.sensitiveFields("tw.zipe.bastpartner.tool.config.Google"))
    }

    @Test
    fun `Tavily config 僅 apiKey 為敏感`() {
        assertEquals(setOf("apiKey"), ToolSchemaGenerator.sensitiveFields("tw.zipe.bastpartner.tool.config.Tavily"))
    }

    @Test
    fun `不存在的 class 回空集合，不拋例外`() {
        assertTrue(ToolSchemaGenerator.sensitiveFields("tw.zipe.bastpartner.tool.config.NoSuchConfig").isEmpty())
    }
}
