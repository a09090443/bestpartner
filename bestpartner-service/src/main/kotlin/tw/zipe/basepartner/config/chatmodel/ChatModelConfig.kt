package tw.zipe.basepartner.config.chatmodel

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import io.netty.util.internal.StringUtil
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.model.LLMChatModel
import tw.zipe.basepartner.properties.BaseAIPlatform
import tw.zipe.basepartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/9
 */
abstract class ChatModelConfig {

    fun convertChatModelSetting(baseAIPlatform: BaseAIPlatform, platform: Platform): LLMChatModel = run {
        LLMChatModel(
            platform = platform,
            url = baseAIPlatform.url().orElse(StringUtil.EMPTY_STRING),
            apiKey = baseAIPlatform.apiKey().orElse(null),
            modelName = baseAIPlatform.modelName(),
            temperature = baseAIPlatform.temperature(),
            timeout = baseAIPlatform.timeout().toMillis()
        )
    }

    fun buildChatModel(llmConfig: LLMChatModel, modelProvider: ModelProvider): ChatLanguageModel {
        return modelProvider.chatModel(llmConfig)
    }

    fun buildStreamingChatModel(llmConfig: LLMChatModel, modelProvider: ModelProvider): StreamingChatLanguageModel {
        return modelProvider.chatModelStreaming(llmConfig)
    }

    /**
     * 建立ChatModel
     */
    abstract fun buildChatModel(): ChatLanguageModel?

    /**
     * 建立StreamingChatModel
     */
    abstract fun buildStreamingChatModel(): StreamingChatLanguageModel?

}
