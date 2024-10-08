package tw.zipe.basepartner.service

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.store.memory.chat.ChatMemoryStore

/**
 * @author Gary
 * @created 2024/10/7
 */
class PersistentChatMemoryStore : ChatMemoryStore {
    private object ChatStore {
        val store = mutableMapOf<Any, MutableList<ChatMessage>>()
    }

    override fun getMessages(memoryId: Any) = ChatStore.store.getOrPut(memoryId) { mutableListOf() }

    override fun updateMessages(memoryId: Any, messages: MutableList<ChatMessage>) {
        ChatStore.store[memoryId] = messages
    }

    override fun deleteMessages(key: Any) {
        ChatStore.store.remove(key)
    }
}
