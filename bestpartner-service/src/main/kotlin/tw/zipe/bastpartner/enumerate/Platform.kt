package tw.zipe.bastpartner.enumerate

import tw.zipe.bastpartner.builder.llm.OllamaModelBuilder
import tw.zipe.bastpartner.builder.llm.OpenaiModelBuilder
import tw.zipe.bastpartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/8
 */
enum class Platform(val builder: ModelProvider) {
    OPENAI(OpenaiModelBuilder()),
    OLLAMA(OllamaModelBuilder());

    fun getLLMBean() = builder

    // 取得 platform 透過 name 的方式
    companion object {
        fun getPlatform(name: String): Platform {
            return entries.firstOrNull { it.name == name }
                ?: throw IllegalArgumentException("找不到對應的平台")
        }
    }
}
