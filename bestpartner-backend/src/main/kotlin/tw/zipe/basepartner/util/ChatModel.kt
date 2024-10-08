package tw.zipe.basepartner.util

import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import java.time.Duration
import tw.zipe.basepartner.service.PersistentChatMemoryStore

/**
 * @author Gary
 * @created 2024/10/07
 */
class ChatModel(
    val url: String,
    val modelName: String,
    val temperature: Double,
    val timeout: Duration
) {

    fun chatModel(): ChatLanguageModel {
        return OllamaChatModel.builder()
            .baseUrl(url)
            .modelName(modelName)
            .temperature(temperature)
            .timeout(timeout)
            .build()
    }

    fun chatModelStreaming(): StreamingChatLanguageModel {
        return OllamaStreamingChatModel.builder()
            .baseUrl(url)
            .modelName(modelName)
            .temperature(temperature)
            .timeout(timeout)
            .build()
    }

    fun chatMemory(memoryId: String, size: Int) = MessageWindowChatMemory.builder()
        .id(memoryId)
        .chatMemoryStore(PersistentChatMemoryStore())
        .maxMessages(size)
        .build()
}
