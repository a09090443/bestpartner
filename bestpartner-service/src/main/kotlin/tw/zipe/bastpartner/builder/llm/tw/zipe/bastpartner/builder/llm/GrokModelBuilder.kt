package tw.zipe.bastpartner.tw.zipe.bastpartner.builder.llm.tw.zipe.bastpartner.builder.llm

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import java.time.Duration
import tw.zipe.bastpartner.model.LLModel
import tw.zipe.bastpartner.provider.ModelProvider

const val URL = "https://api.x.ai/v1"

/**
 * @author Gary
 * @created 2024/10/8
 */
class GrokModelBuilder : ModelProvider {

    override fun chatModel(llModel: LLModel): ChatModel =
        OpenAiChatModel.builder()
            .baseUrl(URL)
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
            .baseUrl(URL)
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
            .baseUrl(URL)
            .apiKey(llModel.apiKey)
            .modelName(llModel.modelName)
            .timeout(llModel.timeout.let { Duration.ofSeconds(it) })
            .dimensions(llModel.dimensions)
            .logRequests(llModel.logRequests)
            .logResponses(llModel.logResponses)
            .build()
}
