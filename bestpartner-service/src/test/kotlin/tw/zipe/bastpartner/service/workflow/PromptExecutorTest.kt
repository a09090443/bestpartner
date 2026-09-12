package tw.zipe.bastpartner.service.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.PromptNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.service.workflow.executor.PromptExecutor

/**
 * PromptExecutor 純單元測試：提示詞插值、輸出鍵解析與缺 prompt 的錯誤情境。
 *
 * @author Gary
 * @created 2026/7/27
 */
class PromptExecutorTest {

    private val executor = PromptExecutor()

    private fun node(key: String = "promptA") = WorkflowNodeEntity().apply { nodeKey = key }

    private fun context() = ExecutionContext("exec-1", "user-1").apply {
        putOutput("trigger", mapOf("question" to "退貨流程是什麼", "count" to 3))
    }

    @Test
    fun `prompt 插值上游輸出後以預設鍵 prompt 輸出`() {
        val cfg = PromptNodeConfig(prompt = "請用繁體中文回答：{{trigger.question}}")
        val out = executor.execute(node(), cfg, context())
        assertEquals(mapOf<String, Any?>("prompt" to "請用繁體中文回答：退貨流程是什麼"), out)
    }

    @Test
    fun `輸出鍵可由 outputKey 覆蓋`() {
        val cfg = PromptNodeConfig(prompt = "共 {{trigger.count}} 筆", outputKey = "askText")
        val out = executor.execute(node(), cfg, context())
        assertEquals(mapOf<String, Any?>("askText" to "共 3 筆"), out)
    }

    @Test
    fun `outputKey 為空白時回退預設鍵`() {
        val cfg = PromptNodeConfig(prompt = "hi", outputKey = "  ")
        val out = executor.execute(node(), cfg, context())
        assertEquals(mapOf<String, Any?>("prompt" to "hi"), out)
    }

    @Test
    fun `prompt 為空白時拋例外`() {
        assertThrows(IllegalArgumentException::class.java) {
            executor.execute(node(), PromptNodeConfig(prompt = "   "), context())
        }
        assertThrows(IllegalArgumentException::class.java) {
            executor.execute(node(), PromptNodeConfig(), context())
        }
    }

    @Test
    fun `prompt 引用不存在變數拋 VariableNotFoundException`() {
        val cfg = PromptNodeConfig(prompt = "{{missing.path}}")
        assertThrows(VariableNotFoundException::class.java) {
            executor.execute(node(), cfg, context())
        }
    }
}
