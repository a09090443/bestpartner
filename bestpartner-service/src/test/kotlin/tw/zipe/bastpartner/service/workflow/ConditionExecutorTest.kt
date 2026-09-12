package tw.zipe.bastpartner.service.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.ConditionExpressionConfig
import tw.zipe.bastpartner.dto.workflow.config.ConditionNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.workflow.executor.ConditionExecutor

/**
 * ConditionExecutor 純單元測試：運算子求值、logic 聚合、插值、handle 覆蓋與錯誤情境。
 *
 * @author Gary
 * @created 2026/7/10
 */
class ConditionExecutorTest {

    private val executor = ConditionExecutor()

    private fun node(key: String = "cond") = WorkflowNodeEntity().apply { nodeKey = key }

    private fun context() = ExecutionContext("exec-1", "user-1")

    private fun cfg(
        vararg conditions: ConditionExpressionConfig,
        logic: String? = null,
        trueHandle: String? = null,
        falseHandle: String? = null
    ) = ConditionNodeConfig(conditions.toList(), logic, trueHandle, falseHandle)

    private fun expr(left: String?, operator: String?, right: String? = null) =
        ConditionExpressionConfig(left, operator, right)

    @Test
    fun `eq 字串相等回傳 true 分支`() {
        val out = executor.execute(node(), cfg(expr("apple", "eq", "apple")), context())
        assertEquals(mapOf("result" to true, "branch" to "out:true"), out)
    }

    @Test
    fun `ne 不相等回傳 true`() {
        val out = executor.execute(node(), cfg(expr("apple", "ne", "banana")), context())
        assertEquals(true, out["result"])
    }

    @Test
    fun `gt 兩側可轉數值時以數值比較`() {
        // 字串比較 "10" < "9"，數值比較 10 > 9；驗證走數值比較
        val out = executor.execute(node(), cfg(expr("10", "gt", "9")), context())
        assertEquals(true, out["result"])
    }

    @Test
    fun `gte 與 lte 邊界相等成立`() {
        assertEquals(true, executor.execute(node(), cfg(expr("5", "gte", "5")), context())["result"])
        assertEquals(true, executor.execute(node(), cfg(expr("5", "lte", "5")), context())["result"])
    }

    @Test
    fun `lt 不可轉數值時以字串比較`() {
        val out = executor.execute(node(), cfg(expr("abc", "lt", "abd")), context())
        assertEquals(true, out["result"])
    }

    @Test
    fun `contains 與 notContains`() {
        assertEquals(true, executor.execute(node(), cfg(expr("hello world", "contains", "world")), context())["result"])
        assertEquals(true, executor.execute(node(), cfg(expr("hello world", "notContains", "mars")), context())["result"])
    }

    @Test
    fun `isEmpty 與 isNotEmpty 忽略 right`() {
        assertEquals(true, executor.execute(node(), cfg(expr("", "isEmpty", "ignored")), context())["result"])
        assertEquals(true, executor.execute(node(), cfg(expr("x", "isNotEmpty", "ignored")), context())["result"])
    }

    @Test
    fun `運算子忽略大小寫`() {
        val out = executor.execute(node(), cfg(expr("a", "EQ", "a")), context())
        assertEquals(true, out["result"])
    }

    @Test
    fun `and 邏輯任一為 false 即 false 分支`() {
        val out = executor.execute(
            node(),
            cfg(expr("1", "eq", "1"), expr("1", "eq", "2"), logic = "and"),
            context()
        )
        assertEquals(mapOf("result" to false, "branch" to "out:false"), out)
    }

    @Test
    fun `or 邏輯任一為 true 即 true 分支`() {
        val out = executor.execute(
            node(),
            cfg(expr("1", "eq", "2"), expr("1", "eq", "1"), logic = "or"),
            context()
        )
        assertEquals(mapOf("result" to true, "branch" to "out:true"), out)
    }

    @Test
    fun `left 為純插值時以原生型別比較`() {
        val ctx = context()
        ctx.putOutput("A", mapOf("count" to 10))
        val out = executor.execute(node(), cfg(expr("{{A.count}}", "gt", "9")), ctx)
        assertEquals(true, out["result"])
    }

    @Test
    fun `left 為混合模板時先插值再比較`() {
        val ctx = context()
        ctx.putOutput("A", mapOf("name" to "gary"))
        val out = executor.execute(node(), cfg(expr("hi-{{A.name}}", "eq", "hi-gary")), ctx)
        assertEquals(true, out["result"])
    }

    @Test
    fun `插值路徑不存在時拋 VariableNotFoundException`() {
        assertThrows(VariableNotFoundException::class.java) {
            executor.execute(node(), cfg(expr("{{missing.path}}", "eq", "x")), context())
        }
    }

    @Test
    fun `trueHandle 與 falseHandle 覆蓋預設 handle`() {
        val t = executor.execute(node(), cfg(expr("1", "eq", "1"), trueHandle = "out:yes"), context())
        assertEquals("out:yes", t["branch"])
        val f = executor.execute(node(), cfg(expr("1", "eq", "2"), falseHandle = "out:no"), context())
        assertEquals("out:no", f["branch"])
    }

    @Test
    fun `不支援的運算子拋 ServiceException 且訊息含運算子名`() {
        val e = assertThrows(ServiceException::class.java) {
            executor.execute(node(), cfg(expr("a", "like", "b")), context())
        }
        assertTrue(e.message!!.contains("like"), "訊息應含運算子名：${e.message}")
    }
}
