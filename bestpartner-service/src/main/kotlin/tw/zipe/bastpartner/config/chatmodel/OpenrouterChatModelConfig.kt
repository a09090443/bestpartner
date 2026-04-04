package tw.zipe.bastpartner.config.chatmodel

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.builder.llm.OpenrouterModelBuilder
import tw.zipe.bastpartner.properties.AIPlatformOpenrouterConfig

/**
 * @author Gary
 * @created 2026/04/04
 */
@ApplicationScoped
class OpenrouterChatModelConfig(var aiPlatformOpenrouterConfig: AIPlatformOpenrouterConfig) : ChatModelConfig() {

    override fun buildChatModel(): ChatModel? {
        val llmConfig = aiPlatformOpenrouterConfig.defaultConfig().map { convertChatModelSetting(it) }.orElse(null)
        return llmConfig?.let { OpenrouterModelBuilder().chatModel(it) }
    }

    override fun buildStreamingChatModel(): StreamingChatModel? {
        val llmConfig = aiPlatformOpenrouterConfig.defaultConfig().map { convertChatModelSetting(it) }.orElse(null)
        return llmConfig?.let { OpenrouterModelBuilder().chatModelStreaming(it) }
    }
}
