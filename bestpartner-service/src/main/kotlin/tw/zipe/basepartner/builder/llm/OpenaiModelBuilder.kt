package tw.zipe.basepartner.builder.llm

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import java.time.Duration
import tw.zipe.basepartner.model.LLModel
import tw.zipe.basepartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/8
 */
class OpenaiModelBuilder : ModelProvider {
    override fun chatModel(chatModel: LLModel): ChatLanguageModel =
        OpenAiChatModel.builder()
            .apiKey(chatModel.apiKey)
            .modelName(chatModel.modelName)
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .maxTokens(chatModel.maxTokens)
            .timeout(chatModel.timeout.let { Duration.ofSeconds(it) })
            .build()

    override fun chatModelStreaming(chatModel: LLModel): StreamingChatLanguageModel =
        OpenAiStreamingChatModel.builder()
            .apiKey(chatModel.apiKey)
            .modelName(chatModel.modelName)
            .temperature(chatModel.temperature)
            .topP(chatModel.topP)
            .maxTokens(chatModel.maxTokens)
            .timeout(chatModel.timeout.let { Duration.ofSeconds(it) })
            .build()

    override fun embeddingModel(llModel: LLModel): EmbeddingModel =
        OpenAiEmbeddingModel.builder()
            .apiKey(llModel.apiKey)
            .modelName(llModel.modelName)
            .timeout(llModel.timeout.let { Duration.ofSeconds(it) })
            .dimensions(llModel.dimensions)
            .build()
}
