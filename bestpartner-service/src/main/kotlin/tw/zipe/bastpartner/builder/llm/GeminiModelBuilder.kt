package tw.zipe.bastpartner.builder.llm

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel
import java.time.Duration
import tw.zipe.bastpartner.model.LLModel
import tw.zipe.bastpartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2025/3/23
 */
class GeminiModelBuilder : ModelProvider {
    override fun chatModel(llModel: LLModel): ChatModel =
        GoogleAiGeminiChatModel.builder()
            .apiKey(llModel.apiKey)
            .modelName(llModel.modelName)
            .temperature(llModel.temperature)
            .topP(llModel.topP)
            .maxOutputTokens(llModel.maxTokens)
            .timeout(llModel.timeout.let { Duration.ofSeconds(it) })
            .logRequestsAndResponses(llModel.logRequestsAndResponses)
            .build()

    override fun chatModelStreaming(llModel: LLModel): StreamingChatModel =
        GoogleAiGeminiStreamingChatModel.builder()
            .apiKey(llModel.apiKey)
            .modelName(llModel.modelName)
            .temperature(llModel.temperature)
            .topP(llModel.topP)
            .maxOutputTokens(llModel.maxTokens)
            .timeout(llModel.timeout.let { Duration.ofSeconds(it) })
            .logRequestsAndResponses(llModel.logRequestsAndResponses)
            .build()

    override fun embeddingModel(llModel: LLModel): EmbeddingModel  =
        GoogleAiEmbeddingModel.builder()
            .apiKey(llModel.apiKey)
            .modelName(llModel.modelName)
            .maxRetries(llModel.maxRetries)
            .timeout(llModel.timeout.let { Duration.ofSeconds(it) })
            .outputDimensionality(llModel.dimensions)
            .logRequestsAndResponses(llModel.logRequestsAndResponses)
            .build()
}
