package tw.zipe.bastpartner.service.workflow

import dev.langchain4j.agent.tool.Tool
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.service.tool.ToolExecutor
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.ToolNodeConfig
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.workflow.executor.ToolNodeExecutor
import tw.zipe.bastpartner.tool.DateTool

/**
 * ToolNodeExecutor 純單元測試：不觸網、不啟動 CDI。
 * 直接測 companion 的 invoke（工具實例由測試提供，形態比照 ToolService.buildTool 的回傳）。
 */
class ToolNodeExecutorTest {

    /** 測試用 @Tool 類：驗證反射呼叫與參數依方法簽名轉型 */
    class EchoTool {
        @Tool("重複輸出文字")
        fun echo(text: String, times: Int): String = text.repeat(times)
    }

    /** 測試用多 @Tool 方法類：驗證取第一個（依方法名排序） */
    class MultiTool {
        @Tool("甲")
        fun alpha(word: String): String = "alpha:$word"

        @Tool("乙")
        fun beta(word: String): String = "beta:$word"
    }

    /** 比照 ToolService.buildToolProvider 對 CUSTOMIZE ToolExecutor（如 DateTool）的回傳形態 */
    private fun dateToolInstance(): Map<ToolSpecification, ToolExecutor> {
        val spec = ToolSpecification.builder()
            .name("getCurrentTime")
            .description("以台灣時間為基準，會根據不同時區取得當地日期時間")
            .parameters(JsonObjectSchema.builder().addStringProperty("zoneId", "國家/城市").build())
            .build()
        return mapOf(spec to DateTool())
    }

    @Test
    fun `ToolExecutor 型工具（DateTool）呼叫成功並以預設 outputKey 輸出`() {
        val ctx = ExecutionContext("e1", "u1")
        val cfg = ToolNodeConfig(
            toolId = "t1",
            arguments = buildJsonObject { put("zoneId", JsonPrimitive("Asia/Taipei")) }
        )
        val output = ToolNodeExecutor.invoke(dateToolInstance(), cfg, ctx)
        val result = output["result"] as String
        assertTrue(result.isNotBlank(), "DateTool 應回傳格式化日期時間字串")
        assertTrue(Regex("""\d{4}""").containsMatchIn(result), "輸出應含年份：$result")
    }

    @Test
    fun `arguments 先經 resolveTemplate 插值再傳入工具`() {
        val ctx = ExecutionContext("e1", "u1")
        ctx.putOutput("trigger", mapOf("input" to mapOf("word" to "hi", "count" to 2)))
        val cfg = ToolNodeConfig(
            toolId = "t1",
            arguments = buildJsonObject {
                put("text", JsonPrimitive("{{trigger.input.word}}"))
                put("times", JsonPrimitive("{{trigger.input.count}}"))
            },
            outputKey = "echoed"
        )
        val output = ToolNodeExecutor.invoke(EchoTool(), cfg, ctx)
        assertEquals("hihi", output["echoed"])
    }

    @Test
    fun `@Tool 方法反射呼叫且參數依簽名做基本型別轉換`() {
        val ctx = ExecutionContext("e1", "u1")
        val cfg = ToolNodeConfig(
            toolId = "t1",
            arguments = buildJsonObject {
                put("text", JsonPrimitive("ab"))
                put("times", JsonPrimitive("3")) // 字串 → Int
            }
        )
        val output = ToolNodeExecutor.invoke(EchoTool(), cfg, ctx)
        assertEquals("ababab", output["result"])
    }

    @Test
    fun `多個 @Tool 方法且未指名時取第一個`() {
        val ctx = ExecutionContext("e1", "u1")
        val cfg = ToolNodeConfig(
            toolId = "t1",
            arguments = buildJsonObject { put("word", JsonPrimitive("x")) }
        )
        val output = ToolNodeExecutor.invoke(MultiTool(), cfg, ctx)
        assertEquals("alpha:x", output["result"])
    }

    @Test
    fun `工具不存在（實例為 null）拋 TOOL_NOT_FOUND`() {
        val ctx = ExecutionContext("e1", "u1")
        val cfg = ToolNodeConfig(toolId = "missing")
        assertThrows(ServiceException::class.java) {
            ToolNodeExecutor.invoke(null, cfg, ctx)
        }
    }

    @Test
    fun `arguments 插值變數不存在時拋 VariableNotFoundException`() {
        val ctx = ExecutionContext("e1", "u1")
        val cfg = ToolNodeConfig(
            toolId = "t1",
            arguments = buildJsonObject { put("zoneId", JsonPrimitive("{{no.such.path}}")) }
        )
        assertThrows(VariableNotFoundException::class.java) {
            ToolNodeExecutor.invoke(dateToolInstance(), cfg, ctx)
        }
    }
}
