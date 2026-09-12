package tw.zipe.bastpartner.service.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import tw.zipe.bastpartner.dto.workflow.config.CodeNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.workflow.executor.CodeExecutor

/**
 * CodeExecutor 純單元測試：GraalJS sandbox 求值、input 注入、逾時中斷、輸出上限與語言檢核。
 *
 * @author Gary
 * @created 2026/7/10
 */
class CodeExecutorTest {

    private val executor = CodeExecutor()

    private fun node(key: String = "code") = WorkflowNodeEntity().apply { nodeKey = key }

    private fun context() = ExecutionContext("exec-1", "user-1")

    private fun cfg(
        source: String,
        language: String? = "js",
        timeoutMs: Long? = null,
        outputKey: String? = null
    ) = CodeNodeConfig(language, source, timeoutMs, outputKey)

    @Test
    fun `回傳物件時輸出反序列化為 Map`() {
        val out = executor.execute(node(), cfg("""({ name: "gary", count: 3 })"""), context())
        assertEquals(mapOf("result" to mapOf("name" to "gary", "count" to 3)), out)
    }

    @Test
    fun `回傳 scalar 時輸出原生型別`() {
        assertEquals(mapOf("result" to 42), executor.execute(node(), cfg("6 * 7"), context()))
        assertEquals(mapOf("result" to "hi"), executor.execute(node(), cfg(""""h" + "i""""), context()))
    }

    @Test
    fun `回傳陣列時輸出反序列化為 List`() {
        val out = executor.execute(node(), cfg("[1, 2, 3].map(n => n * 2)"), context())
        assertEquals(mapOf("result" to listOf(2, 4, 6)), out)
    }

    @Test
    fun `outputKey 覆蓋預設輸出鍵`() {
        val out = executor.execute(node(), cfg("1 + 1", outputKey = "sum"), context())
        assertEquals(mapOf("sum" to 2), out)
    }

    @Test
    fun `input 全域變數可讀取上游輸出`() {
        val ctx = context()
        ctx.putOutput("A", mapOf("count" to 10, "name" to "gary"))
        val out = executor.execute(node(), cfg("input.A.count + input.A.name.length"), ctx)
        assertEquals(mapOf("result" to 14), out)
    }

    @Test
    fun `上游輸出含單引號與反斜線時 input 注入不破壞腳本`() {
        val ctx = context()
        ctx.putOutput("A", mapOf("text" to """it's a "quote" with \ backslash"""))
        val out = executor.execute(node(), cfg("input.A.text"), ctx)
        assertEquals(mapOf("result" to """it's a "quote" with \ backslash"""), out)
    }

    @Test
    fun `語法錯誤時拋 ServiceException 且訊息含錯誤摘要`() {
        val e = assertThrows(ServiceException::class.java) {
            executor.execute(node(), cfg("this is not js;;;"), context())
        }
        assertTrue(e.message!!.contains("SyntaxError"), "訊息應含 JS 錯誤摘要：${e.message}")
    }

    @Test
    fun `腳本執行期拋錯時拋 ServiceException 且訊息含錯誤摘要`() {
        val e = assertThrows(ServiceException::class.java) {
            executor.execute(node(), cfg("""throw new Error("boom")"""), context())
        }
        assertTrue(e.message!!.contains("boom"), "訊息應含 JS 錯誤摘要：${e.message}")
    }

    @Test
    @Timeout(30)
    fun `無窮迴圈於 timeoutMs 後強制中斷並失敗`() {
        val e = assertThrows(ServiceException::class.java) {
            executor.execute(node(), cfg("while (true) {}", timeoutMs = 500), context())
        }
        assertTrue(e.message!!.contains("500"), "訊息應含逾時毫秒數：${e.message}")
    }

    @Test
    fun `輸出序列化後超過 256KB 上限時失敗`() {
        val e = assertThrows(ServiceException::class.java) {
            executor.execute(node(), cfg(""""x".repeat(300 * 1024)"""), context())
        }
        assertTrue(e.message!!.contains("256"), "訊息應含輸出上限：${e.message}")
    }

    @Test
    fun `非 js 語言拒絕執行`() {
        val e = assertThrows(ServiceException::class.java) {
            executor.execute(node(), cfg("print(1)", language = "python"), context())
        }
        assertTrue(e.message!!.contains("python"), "訊息應含語言名：${e.message}")
    }

    @Test
    fun `sandbox 禁止 host class 存取`() {
        val e = assertThrows(ServiceException::class.java) {
            executor.execute(node(), cfg("""Java.type("java.lang.System").exit(1)"""), context())
        }
        assertTrue(e.message != null)
    }
}
