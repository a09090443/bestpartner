package tw.zipe.bastpartner.provider

import dev.langchain4j.service.tool.ToolProvider
import dev.langchain4j.service.tool.ToolProviderRequest
import dev.langchain4j.service.tool.ToolProviderResult

/**
 * 合併多個 [ToolProvider] 為單一 provider。
 *
 * langchain4j `AiServices.toolProvider(...)` 為**單一插槽**：多次呼叫會後者覆蓋前者。
 * 當同時需要掛載 Skill（`activate_skill` 工具）與 MCP server 工具集時，若各自呼叫
 * `toolProvider(...)` 會使先設定者被靜默丟棄。故以本類把多個 provider 的
 * [ToolProviderResult]（工具 map 與 immediateReturnToolNames）合併後一次掛上。
 */
class CompositeToolProvider(private val providers: List<ToolProvider>) : ToolProvider {

    override fun provideTools(request: ToolProviderRequest): ToolProviderResult {
        val builder = ToolProviderResult.builder()
        val immediateReturnToolNames = mutableSetOf<String>()
        providers.forEach { provider ->
            val result = provider.provideTools(request) ?: return@forEach
            builder.addAll(result.tools())
            immediateReturnToolNames.addAll(result.immediateReturnToolNames())
        }
        if (immediateReturnToolNames.isNotEmpty()) {
            builder.immediateReturnToolNames(immediateReturnToolNames)
        }
        return builder.build()
    }
}
