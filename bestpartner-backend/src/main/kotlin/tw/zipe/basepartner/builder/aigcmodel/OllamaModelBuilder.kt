package tw.zipe.basepartner.builder.aigcmodel

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import java.time.Duration
import tw.zipe.basepartner.model.LLMChatModel
import tw.zipe.basepartner.model.LLMEmbeddingModel
import tw.zipe.basepartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/8
 */
class OllamaModelBuilder : ModelProvider {
    override fun chatModel(chatModel: LLMChatModel): ChatLanguageModel =
        OllamaChatModel.builder()
            .baseUrl(chatModel.url)
            .modelName(chatModel.modelName)
            .temperature(chatModel.temperature)
            .timeout(chatModel.timeout.let { Duration.ofSeconds(it) })
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .build()

    override fun chatModelStreaming(chatModel: LLMChatModel): StreamingChatLanguageModel =
        OllamaStreamingChatModel.builder()
            .baseUrl(chatModel.url)
            .modelName(chatModel.modelName)
            .temperature(chatModel.temperature)
            .timeout(chatModel.timeout.let { Duration.ofSeconds(it) })
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .build()

    override fun embeddingModel(embeddingModel: LLMEmbeddingModel): EmbeddingModel =
        OllamaEmbeddingModel.builder()
            .baseUrl(embeddingModel.url)
            .modelName(embeddingModel.modelName)
            .timeout(embeddingModel.timeout.let { Duration.ofSeconds(it) })
            .build()
}
