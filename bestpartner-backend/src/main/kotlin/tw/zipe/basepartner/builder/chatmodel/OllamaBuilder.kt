package tw.zipe.basepartner.builder.chatmodel

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import java.time.Duration
import tw.zipe.basepartner.model.ChatModel
import tw.zipe.basepartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/8
 */
class OllamaBuilder : ModelProvider {
    override fun chatModel(chatModel: ChatModel): ChatLanguageModel =
        OllamaChatModel.builder()
            .baseUrl(chatModel.url)
            .modelName(chatModel.modelName)
            .temperature(chatModel.temperature)
            .timeout(chatModel.timeout.let { Duration.ofSeconds(it) })
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .build()

    override fun chatModelStreaming(chatModel: ChatModel): StreamingChatLanguageModel =
        OllamaStreamingChatModel.builder()
            .baseUrl(chatModel.url)
            .modelName(chatModel.modelName)
            .temperature(chatModel.temperature)
            .timeout(chatModel.timeout.let { Duration.ofSeconds(it) })
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .build()
}
