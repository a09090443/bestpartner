package tw.zipe.basepartner.provider

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import tw.zipe.basepartner.model.LLMChatModel
import tw.zipe.basepartner.model.LLMEmbeddingModel

/**
 * @author Gary
 * @created 2024/10/8
 */
interface ModelProvider {

    fun chatModel(chatModel: LLMChatModel): ChatLanguageModel

    fun chatModelStreaming(chatModel: LLMChatModel): StreamingChatLanguageModel

    fun embeddingModel(embeddingModel: LLMEmbeddingModel): EmbeddingModel

}
