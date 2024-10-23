package tw.zipe.bastpartner.config.chatmodel

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.builder.llm.OpenaiModelBuilder
import tw.zipe.bastpartner.properties.AIPlatformOpenaiConfig

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class OpenaiChatModelConfig(var aiPlatformOpenaiConfig: AIPlatformOpenaiConfig) : ChatModelConfig() {

    override fun buildChatModel(): ChatLanguageModel? {
        val llmConfig = aiPlatformOpenaiConfig.defaultConfig().map { convertChatModelSetting(it) }.orElse(null)
        return llmConfig?.let { OpenaiModelBuilder().chatModel(it) }
    }

    override fun buildStreamingChatModel(): StreamingChatLanguageModel? {
        val llmConfig = aiPlatformOpenaiConfig.defaultConfig().map { convertChatModelSetting(it) }.orElse(null)
        return llmConfig?.let { OpenaiModelBuilder().chatModelStreaming(it) }
    }

}

