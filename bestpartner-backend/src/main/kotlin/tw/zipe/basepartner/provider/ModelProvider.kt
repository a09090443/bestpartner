package tw.zipe.basepartner.provider

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import tw.zipe.basepartner.model.ChatModel

/**
 * @author Gary
 * @created 2024/10/8
 */
interface ModelProvider {

    fun chatModel(chatModel: ChatModel): ChatLanguageModel

    fun chatModelStreaming(chatModel: ChatModel): StreamingChatLanguageModel
}
