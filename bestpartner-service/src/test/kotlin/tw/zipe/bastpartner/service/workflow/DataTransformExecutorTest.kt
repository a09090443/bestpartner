package tw.zipe.bastpartner.service.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.DataTransformMappingConfig
import tw.zipe.bastpartner.dto.workflow.config.DataTransformNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.service.workflow.executor.DataTransformExecutor

/**
 * DataTransformExecutor 純單元測試：template 插值、mappings 原生型別保留、混合輸出與錯誤情境。
 *
 * @author Gary
 * @created 2026/7/11
 */
class DataTransformExecutorTest {

    private val executor = DataTransformExecutor()

    private fun node(key: String = "transform") = WorkflowNodeEntity().apply { nodeKey = key }

    private fun context() = ExecutionContext("exec-1", "user-1").apply {
        putOutput("A", mapOf("name" to "gary", "count" to 10, "flag" to true, "items" to listOf(1, 2, 3)))
    }

    private fun mapping(targetKey: String?, expression: String?) =
        DataTransformMappingConfig(targetKey, expression)

    @Test
    fun `template 插值輸出至預設 result 鍵`() {
        val cfg = DataTransformNodeConfig(template = "hi-{{A.name}}")
        val out = executor.execute(node(), cfg, context())
        assertEquals(mapOf<String, Any?>("result" to "hi-gary"), out)
    }

    @Test
    fun `template 輸出鍵可由 outputKey 覆蓋`() {
        val cfg = DataTransformNodeConfig(template = "count={{A.count}}", outputKey = "summary")
        val out = executor.execute(node(), cfg, context())
        assertEquals(mapOf<String, Any?>("summary" to "count=10"), out)
    }

    @Test
    fun `mappings 純插值表達式保留原生型別`() {
        val cfg = DataTransformNodeConfig(
            mappings = listOf(
                mapping("num", "{{A.count}}"),
                mapping("bool", "{{A.flag}}"),
                mapping("list", "{{A.items}}")
            )
        )
        val out = executor.execute(node(), cfg, context())
        assertEquals(mapOf<String, Any?>("num" to 10, "bool" to true, "list" to listOf(1, 2, 3)), out)
    }

    @Test
    fun `mappings 混合模板表達式插值為字串`() {
        val cfg = DataTransformNodeConfig(mappings = listOf(mapping("greeting", "hi-{{A.name}}")))
        val out = executor.execute(node(), cfg, context())
        assertEquals(mapOf<String, Any?>("greeting" to "hi-gary"), out)
    }

    @Test
    fun `mappings 中 targetKey 或 expression 為 null 的項目跳過`() {
        val cfg = DataTransformNodeConfig(
            mappings = listOf(
                mapping(null, "{{A.count}}"),
                mapping("skipped", null),
                mapping("kept", "{{A.name}}")
            )
        )
        val out = executor.execute(node(), cfg, context())
        assertEquals(mapOf<String, Any?>("kept" to "gary"), out)
    }

    @Test
    fun `mappings 與 template 皆有時 template 併入 outputKey 鍵`() {
        val cfg = DataTransformNodeConfig(
            mappings = listOf(mapping("num", "{{A.count}}")),
            template = "hi-{{A.name}}"
        )
        val out = executor.execute(node(), cfg, context())
        assertEquals(mapOf<String, Any?>("num" to 10, "result" to "hi-gary"), out)
    }

    @Test
    fun `mappings 與 template 目標鍵衝突時 mappings 優先`() {
        val cfg = DataTransformNodeConfig(
            mappings = listOf(mapping("result", "{{A.count}}")),
            template = "hi-{{A.name}}",
            outputKey = "result"
        )
        val out = executor.execute(node(), cfg, context())
        assertEquals(mapOf<String, Any?>("result" to 10), out)
    }

    @Test
    fun `template 引用不存在變數拋 VariableNotFoundException`() {
        val cfg = DataTransformNodeConfig(template = "{{missing.path}}")
        assertThrows(VariableNotFoundException::class.java) {
            executor.execute(node(), cfg, context())
        }
    }

    @Test
    fun `mappings 引用不存在變數拋 VariableNotFoundException`() {
        val cfg = DataTransformNodeConfig(mappings = listOf(mapping("x", "{{missing.path}}")))
        assertThrows(VariableNotFoundException::class.java) {
            executor.execute(node(), cfg, context())
        }
    }
}
