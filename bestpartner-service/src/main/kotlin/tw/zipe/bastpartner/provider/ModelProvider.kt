package tw.zipe.bastpartner.provider

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import tw.zipe.bastpartner.model.LLModel

/**
 * @author Gary
 * @created 2024/10/8
 */
interface ModelProvider {

    fun chatModel(llModel: LLModel): ChatModel

    fun chatModelStreaming(llModel: LLModel): StreamingChatModel

    fun embeddingModel(llModel: LLModel): EmbeddingModel

}
