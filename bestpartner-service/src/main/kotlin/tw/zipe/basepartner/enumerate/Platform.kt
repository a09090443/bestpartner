package tw.zipe.basepartner.enumerate

import tw.zipe.basepartner.builder.llm.OllamaModelBuilder
import tw.zipe.basepartner.builder.llm.OpenaiModelBuilder
import tw.zipe.basepartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/8
 */
enum class Platform(val builder: ModelProvider) {
    OPENAI(OpenaiModelBuilder()),
    OLLAMA(OllamaModelBuilder());

    fun getLLMBean(): ModelProvider {
        return builder
    }
}
