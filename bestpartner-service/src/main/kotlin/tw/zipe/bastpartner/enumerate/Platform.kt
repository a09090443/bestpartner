package tw.zipe.bastpartner.enumerate

import tw.zipe.bastpartner.builder.llm.AnthropicModelBuilder
import tw.zipe.bastpartner.builder.llm.GeminiModelBuilder
import tw.zipe.bastpartner.builder.llm.GrokModelBuilder
import tw.zipe.bastpartner.builder.llm.OllamaModelBuilder
import tw.zipe.bastpartner.builder.llm.OpenaiModelBuilder
import tw.zipe.bastpartner.builder.llm.OpenrouterModelBuilder
import tw.zipe.bastpartner.provider.ModelProvider
import tw.zipe.bastpartner.util.MessageUtil
import tw.zipe.bastpartner.enumerate.AppMessage

/**
 * @author Gary
 * @created 2024/10/8
 */
enum class Platform(val builder: ModelProvider) {
    OPENAI(OpenaiModelBuilder()),
    OLLAMA(OllamaModelBuilder()),
    GEMINI(GeminiModelBuilder()),
    ANTHROPIC(AnthropicModelBuilder()),
    GROK(GrokModelBuilder()),
    OPENROUTER(OpenrouterModelBuilder());

    fun getLLMBean() = builder

    // 取得 platform 透過 name 的方式
    companion object {
        fun getPlatform(name: String): Platform {
            return entries.firstOrNull { it.name == name }
                ?: throw IllegalArgumentException(MessageUtil.get(AppMessage.SYSTEM_PLATFORM_NOT_FOUND))
        }
    }
}
