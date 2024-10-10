package tw.zipe.basepartner.util

import dev.langchain4j.memory.chat.MessageWindowChatMemory
import tw.zipe.basepartner.config.PersistentChatMemoryStore

/**
 * @author Gary
 * @created 2024/10/07
 */
class ChatModelBuilder {

    fun chatMemory(memoryId: String, size: Int) = MessageWindowChatMemory.builder()
        .id(memoryId)
        .chatMemoryStore(PersistentChatMemoryStore())
        .maxMessages(size)
        .build()
}
