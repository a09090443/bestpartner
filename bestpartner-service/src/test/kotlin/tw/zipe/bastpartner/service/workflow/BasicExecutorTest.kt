package tw.zipe.bastpartner.service.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.OutputNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.TriggerNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.TriggerType
import tw.zipe.bastpartner.service.workflow.executor.OutputExecutor
import tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor

class BasicExecutorTest {

    private fun node(key: String) = WorkflowNodeEntity().apply { nodeKey = key }

    @Test
    fun `TriggerExecutor 將 input payload 扁平化作為輸出`() {
        // Task 8：__input__ 即 input map 本身，trigger 輸出不再包 input 一層
        val ctx = ExecutionContext("exec-1", "user-1")
        ctx.putOutput("__input__", mapOf("q" to "hello"))
        val out = TriggerExecutor().execute(node("trigger"), TriggerNodeConfig(TriggerType.MANUAL), ctx)
        assertEquals(mapOf("q" to "hello"), out)
    }

    @Test
    fun `TriggerExecutor 無 input payload 時輸出空 map`() {
        val ctx = ExecutionContext("exec-1", "user-1")
        val out = TriggerExecutor().execute(node("trigger"), TriggerNodeConfig(TriggerType.MANUAL), ctx)
        assertEquals(emptyMap<String, Any?>(), out)
    }

    @Test
    fun `OutputExecutor template 模式`() {
        val ctx = ExecutionContext("exec-1", "user-1")
        ctx.putOutput("llm", mapOf("reply" to "OK"))
        val out = OutputExecutor().execute(node("out"), OutputNodeConfig(template = "答案：{{llm.reply}}"), ctx)
        assertEquals(mapOf("result" to "答案：OK"), out)
    }

    @Test
    fun `OutputExecutor mappings 模式`() {
        val ctx = ExecutionContext("exec-1", "user-1")
        ctx.putOutput("llm", mapOf("reply" to "OK"))
        val out = OutputExecutor().execute(
            node("out"),
            OutputNodeConfig(mappings = mapOf("answer" to "{{llm.reply}}", "static" to "v1")),
            ctx
        )
        assertEquals(mapOf("answer" to "OK", "static" to "v1"), out)
    }

    @Test
    fun `ExecutionEvent 序列化為單行 JSON`() {
        val json = ExecutionEvent(event = "node.started", executionId = "e1", nodeKey = "n1", seqNo = 1, ts = "t").toJson()
        assert(json.contains(""""event":"node.started"""") && !json.contains("\n"))
    }
}
