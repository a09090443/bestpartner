package tw.zipe.bastpartner.assistant

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.output.Response
import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.UserMessage

/**
 * @author Gary
 * @created 2024/10/7
 */
interface DynamicAssistant{
    fun chat(@MemoryId memoryId: String, @UserMessage userMessage: String): Response<AiMessage>
    fun streamingChat(@MemoryId memoryId: String, @UserMessage userMessage: String): TokenStream
}
