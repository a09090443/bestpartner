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

}
