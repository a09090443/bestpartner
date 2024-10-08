package tw.zipe.basepartner.builder.chatmodel

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import java.time.Duration
import tw.zipe.basepartner.model.ChatModel
import tw.zipe.basepartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/8
 */
class OpenaiBuilder : ModelProvider {
    override fun chatModel(chatModel: ChatModel): ChatLanguageModel =
        OpenAiChatModel.builder()
            .baseUrl(chatModel.url)
            .apiKey(chatModel.apiKey)
            .modelName(chatModel.modelName)
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .maxTokens(chatModel.maxTokens)
            .timeout(chatModel.timeout.let { Duration.ofSeconds(it) })
            .build()

    override fun chatModelStreaming(chatModel: ChatModel): StreamingChatLanguageModel =
        OpenAiStreamingChatModel.builder()
            .baseUrl(chatModel.url)
            .modelName(chatModel.modelName)
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .maxTokens(chatModel.maxTokens)
            .timeout(chatModel.timeout.let { Duration.ofSeconds(it) })
            .build()
}
