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
 * [ToolProviderResult] 合併後一次掛上。
 *
 * 合併單位為 `AiServiceTool`：自 langchain4j 1.14.0 起，每個工具自帶
 * `ReturnBehavior`（`TO_LLM` / `IMMEDIATE` / `IMMEDIATE_IF_LAST`），取代了舊有
 * 「另外維護一份 immediateReturnToolNames 名稱集合」的作法，故此處無須再手動合併該集合。
 * 上游 provider 即使仍以已棄用的 `immediateReturnToolNames(...)` 建構結果亦相容——
 * 該設定會在其 `build()` 時就寫入對應工具的 `ReturnBehavior`。
 */
class CompositeToolProvider(private val providers: List<ToolProvider>) : ToolProvider {

    override fun provideTools(request: ToolProviderRequest): ToolProviderResult {
        val builder = ToolProviderResult.builder()
        providers.forEach { provider ->
            val result = provider.provideTools(request) ?: return@forEach
            builder.addAll(result.aiServiceTools())
        }
        return builder.build()
    }
}
