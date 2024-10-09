package tw.zipe.basepartner.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.builder.chatmodel.OpenaiBuilder
import tw.zipe.basepartner.properties.AIPlatformOpenaiConfig

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class OpenaiChatModelConfig(var aiPlatformOpenaiConfig: AIPlatformOpenaiConfig) : ChatModelConfig() {

    override fun buildChatModel(): ChatLanguageModel? {
        val llmConfig = aiPlatformOpenaiConfig.defaultConfig().map { convertChatModelSetting(it) }.orElse(null)
        return llmConfig?.let { OpenaiBuilder().chatModel(it) }
    }

    override fun buildStreamingChatModel(): StreamingChatLanguageModel? {
        val llmConfig = aiPlatformOpenaiConfig.defaultConfig().map { convertChatModelSetting(it) }.orElse(null)
        return llmConfig?.let { OpenaiBuilder().chatModelStreaming(it) }
    }

}

