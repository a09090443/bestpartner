package tw.zipe.bastpartner.builder.llm

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import java.time.Duration
import tw.zipe.bastpartner.model.LLModel
import tw.zipe.bastpartner.provider.ModelProvider

const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"

/**
 * @author Gary
 * @created 2026/04/04
 */
class OpenrouterModelBuilder : ModelProvider {

    override fun chatModel(llModel: LLModel): ChatModel =
        OpenAiChatModel.builder()
            .baseUrl(OPENROUTER_BASE_URL)
            .apiKey(llModel.apiKey)
            .modelName(llModel.modelName)
            .temperature(llModel.temperature)
            .topP(llModel.topP)
            .maxTokens(llModel.maxTokens)
            .timeout(llModel.timeout.let { Duration.ofSeconds(it) })
            .logRequests(llModel.logRequests)
            .logResponses(llModel.logResponses)
            .build()

    override fun chatModelStreaming(llModel: LLModel): StreamingChatModel =
        OpenAiStreamingChatModel.builder()
            .baseUrl(OPENROUTER_BASE_URL)
            .apiKey(llModel.apiKey)
            .modelName(llModel.modelName)
            .temperature(llModel.temperature)
            .topP(llModel.topP)
            .maxTokens(llModel.maxTokens)
            .timeout(llModel.timeout.let { Duration.ofSeconds(it) })
            .logRequests(llModel.logRequests)
            .logResponses(llModel.logResponses)
            .build()

    override fun embeddingModel(llModel: LLModel): EmbeddingModel =
        OpenAiEmbeddingModel.builder()
            .baseUrl(OPENROUTER_BASE_URL)
            .apiKey(llModel.apiKey)
            .modelName(llModel.modelName)
            .timeout(llModel.timeout.let { Duration.ofSeconds(it) })
            .dimensions(llModel.dimensions)
            .logRequests(llModel.logRequests)
            .logResponses(llModel.logResponses)
            .build()
}
