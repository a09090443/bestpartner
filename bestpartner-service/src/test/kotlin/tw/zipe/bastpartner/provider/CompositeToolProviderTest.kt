package tw.zipe.bastpartner.provider

import dev.langchain4j.agent.tool.ReturnBehavior
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.service.tool.ToolExecutor
import dev.langchain4j.service.tool.ToolProvider
import dev.langchain4j.service.tool.ToolProviderRequest
import dev.langchain4j.service.tool.ToolProviderResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 驗證 [CompositeToolProvider] 正確合併多個 ToolProvider 的工具——
 * 對應 Skill + MCP 同時掛載到 LLM 節點時，兩者工具皆須可見（修正 toolProvider 單插槽覆蓋缺陷）。
 */
class CompositeToolProviderTest {

    private fun providerOf(vararg toolNames: String): ToolProvider = ToolProvider { _ ->
        val builder = ToolProviderResult.builder()
        toolNames.forEach { name ->
            builder.add(
                ToolSpecification.builder().name(name).build(),
                ToolExecutor { _, _ -> "result-of-$name" }
            )
        }
        builder.build()
    }

    private fun request() = ToolProviderRequest(null, UserMessage.from("hi"))

    private fun ToolProviderResult.toolNames() = aiServiceTools().map { it.name() }.toSet()

    private fun ToolProviderResult.behaviorOf(name: String) =
        aiServiceTools().first { it.name() == name }.returnBehavior()

    @Test
    fun `合併 Skill 與 MCP 兩個 provider 的工具皆可見`() {
        val skillProvider = providerOf("activate_skill")
        val mcpProvider = providerOf("mcp_tool_a", "mcp_tool_b")
        val composite = CompositeToolProvider(listOf(skillProvider, mcpProvider))

        val names = composite.provideTools(request()).toolNames()

        assertEquals(setOf("activate_skill", "mcp_tool_a", "mcp_tool_b"), names)
    }

    @Test
    fun `各工具的 ReturnBehavior 於合併後保留`() {
        val immediateProvider = ToolProvider { _ ->
            ToolProviderResult.builder()
                .add(
                    ToolSpecification.builder().name("t1").build(),
                    ToolExecutor { _, _ -> "x" },
                    ReturnBehavior.IMMEDIATE
                )
                .build()
        }
        val composite = CompositeToolProvider(listOf(immediateProvider, providerOf("t2")))

        val result = composite.provideTools(request())

        assertEquals(setOf("t1", "t2"), result.toolNames())
        assertEquals(ReturnBehavior.IMMEDIATE, result.behaviorOf("t1"))
        assertEquals(ReturnBehavior.TO_LLM, result.behaviorOf("t2"), "未指定者應維持預設 TO_LLM")
    }

    /**
     * 上游 provider（MCP / Skills）若仍以已棄用的 `immediateReturnToolNames(...)` 建構結果，
     * 合併後仍須保留 IMMEDIATE 語意。故意保留此路徑作為**相容性覆蓋**：
     * 若上游哪天改變該設定與 `ReturnBehavior` 的對應方式，此測試會先紅燈。
     */
    @Suppress("DEPRECATION")
    @Test
    fun `以舊版 immediateReturnToolNames 建構的 provider 仍相容`() {
        val legacyProvider = ToolProvider { _ ->
            ToolProviderResult.builder()
                .add(ToolSpecification.builder().name("t1").build(), ToolExecutor { _, _ -> "x" })
                .immediateReturnToolNames(setOf("t1"))
                .build()
        }
        val composite = CompositeToolProvider(listOf(legacyProvider, providerOf("t2")))

        val result = composite.provideTools(request())

        assertEquals(setOf("t1", "t2"), result.toolNames())
        assertEquals(ReturnBehavior.IMMEDIATE, result.behaviorOf("t1"))
    }

    @Test
    fun `單一 provider 直接透傳`() {
        val composite = CompositeToolProvider(listOf(providerOf("only")))
        assertEquals(setOf("only"), composite.provideTools(request()).toolNames())
    }
}
