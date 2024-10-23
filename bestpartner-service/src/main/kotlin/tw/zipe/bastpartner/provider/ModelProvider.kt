package tw.zipe.bastpartner.provider

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import tw.zipe.bastpartner.model.LLModel

/**
 * @author Gary
 * @created 2024/10/8
 */
interface ModelProvider {

    fun chatModel(llModel: LLModel): ChatLanguageModel

    fun chatModelStreaming(llModel: LLModel): StreamingChatLanguageModel

    fun embeddingModel(llModel: LLModel): EmbeddingModel

}
