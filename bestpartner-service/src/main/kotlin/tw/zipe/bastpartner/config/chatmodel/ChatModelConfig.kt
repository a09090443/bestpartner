package tw.zipe.bastpartner.config.chatmodel

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import io.netty.util.internal.StringUtil
import tw.zipe.bastpartner.model.LLModel
import tw.zipe.bastpartner.properties.BaseAIPlatform
import tw.zipe.bastpartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/9
 */
abstract class ChatModelConfig {

    fun convertChatModelSetting(baseAIPlatform: BaseAIPlatform) = with(LLModel()) {
        apiKey = baseAIPlatform.apiKey().orElse(StringUtil.EMPTY_STRING)
        url = baseAIPlatform.url().orElse(StringUtil.EMPTY_STRING)
        modelName = baseAIPlatform.modelName()
        temperature = baseAIPlatform.temperature()
        topP = baseAIPlatform.topP().orElse(Double.MIN_VALUE)
        topK = baseAIPlatform.topK().orElse(Int.MIN_VALUE)
        maxTokens = baseAIPlatform.maxTokens().orElse(null)
        timeout = baseAIPlatform.timeout().toMillis()
        logRequests = baseAIPlatform.logRequests().orElse(false)
        logResponses = baseAIPlatform.logResponses().orElse(false)
        this
    }

    fun buildChatModel(llmConfig: LLModel, modelProvider: ModelProvider): ChatModel {
        return modelProvider.chatModel(llmConfig)
    }

    fun buildStreamingChatModel(llmConfig: LLModel, modelProvider: ModelProvider): StreamingChatModel {
        return modelProvider.chatModelStreaming(llmConfig)
    }

    /**
     * 建立ChatModel
     */
    abstract fun buildChatModel(): ChatModel?

    /**
     * 建立StreamingChatModel
     */
    abstract fun buildStreamingChatModel(): StreamingChatModel?

}
