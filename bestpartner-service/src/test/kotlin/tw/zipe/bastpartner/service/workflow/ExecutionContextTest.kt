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
