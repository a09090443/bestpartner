package tw.zipe.bastpartner.builder.llm

import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import java.time.Duration
import tw.zipe.bastpartner.model.LLModel
import tw.zipe.bastpartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2025/3/23
 */
class AnthropicModelBuilder : ModelProvider {
    override fun chatModel(llModel: LLModel): ChatModel =
        AnthropicChatModel.builder()
            .apiKey(llModel.apiKey)
            .modelName(llModel.modelName)
            .temperature(llModel.temperature)
            .topP(llModel.topP)
            .maxTokens(llModel.maxTokens)
            .timeout(llModel.timeout.let { Duration.ofSeconds(it) })
            .logRequests(llModel.logRequests)
            .logResponses(llModel.logResponses)
            .build()

    override fun chatModelStreaming(llModel: LLModel): StreamingChatModel=
        AnthropicStreamingChatModel.builder()
            .apiKey(llModel.apiKey)
            .modelName(llModel.modelName)
            .temperature(llModel.temperature)
            .topP(llModel.topP)
            .maxTokens(llModel.maxTokens)
            .timeout(llModel.timeout.let { Duration.ofSeconds(it) })
            .logRequests(llModel.logRequests)
            .logResponses(llModel.logResponses)
            .build()

    override fun embeddingModel(llModel: LLModel): EmbeddingModel {
        TODO("Not yet implemented")
    }

}
