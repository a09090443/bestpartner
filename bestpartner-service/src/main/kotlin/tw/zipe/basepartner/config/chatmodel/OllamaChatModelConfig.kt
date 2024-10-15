package tw.zipe.basepartner.config.chatmodel

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.builder.llm.OllamaModelBuilder
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.properties.AIPlatformOllamaConfig

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class OllamaChatModelConfig(var aiPlatformOllamaConfig: AIPlatformOllamaConfig) : ChatModelConfig() {

    override fun buildChatModel(): ChatLanguageModel? {
        val llmConfig = aiPlatformOllamaConfig.defaultConfig().map { convertChatModelSetting(it, Platform.OLLAMA) }.orElse(null)
        return llmConfig?.let { OllamaModelBuilder().chatModel(it) }
    }

    override fun buildStreamingChatModel(): StreamingChatLanguageModel? {
        val llmConfig = aiPlatformOllamaConfig.defaultConfig().map { convertChatModelSetting(it, Platform.OLLAMA) }.orElse(null)
        return llmConfig?.let { OllamaModelBuilder().chatModelStreaming(it) }
    }

}

