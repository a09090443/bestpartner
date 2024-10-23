package tw.zipe.bastpartner.config.chatmodel

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.builder.llm.OllamaModelBuilder
import tw.zipe.bastpartner.properties.AIPlatformOllamaConfig

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class OllamaChatModelConfig(var aiPlatformOllamaConfig: AIPlatformOllamaConfig) : ChatModelConfig() {

    override fun buildChatModel(): ChatLanguageModel? {
        val llmConfig = aiPlatformOllamaConfig.defaultConfig().map { convertChatModelSetting(it) }.orElse(null)
        return llmConfig?.let { OllamaModelBuilder().chatModel(it) }
    }

    override fun buildStreamingChatModel(): StreamingChatLanguageModel? {
        val llmConfig = aiPlatformOllamaConfig.defaultConfig().map { convertChatModelSetting(it) }.orElse(null)
        return llmConfig?.let { OllamaModelBuilder().chatModelStreaming(it) }
    }

}

