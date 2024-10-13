package tw.zipe.basepartner.builder.aigcmodel

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import java.time.Duration
import tw.zipe.basepartner.model.LLMChatModel
import tw.zipe.basepartner.model.LLMEmbeddingModel
import tw.zipe.basepartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/8
 */
class OpenaiModelBuilder : ModelProvider {
    override fun chatModel(chatModel: LLMChatModel): ChatLanguageModel =
        OpenAiChatModel.builder()
            .baseUrl(chatModel.url)
            .apiKey(chatModel.apiKey)
            .modelName(chatModel.modelName)
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .maxTokens(chatModel.maxTokens)
            .timeout(chatModel.timeout.let { Duration.ofSeconds(it) })
            .build()

    override fun chatModelStreaming(chatModel: LLMChatModel): StreamingChatLanguageModel =
        OpenAiStreamingChatModel.builder()
            .baseUrl(chatModel.url)
            .modelName(chatModel.modelName)
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .maxTokens(chatModel.maxTokens)
            .timeout(chatModel.timeout.let { Duration.ofSeconds(it) })
            .build()

    override fun embeddingModel(embeddingModel: LLMEmbeddingModel): EmbeddingModel =
        OpenAiEmbeddingModel.builder()
            .baseUrl(embeddingModel.url)
            .apiKey(embeddingModel.apiKey)
            .modelName(embeddingModel.modelName)
            .timeout(embeddingModel.timeout.let { Duration.ofSeconds(it) })
            .dimensions(embeddingModel.dimensions)
            .build()
}
