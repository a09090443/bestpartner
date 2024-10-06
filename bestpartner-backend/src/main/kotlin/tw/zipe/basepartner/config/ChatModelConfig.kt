package tw.zipe.basepartner.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Produces
import tw.zipe.basepartner.properties.AIPlatformOllamaConfig
import tw.zipe.basepartner.properties.AIPlatformOpenaiConfig
import tw.zipe.basepartner.properties.OllamaProp
import tw.zipe.basepartner.properties.OpenaiProp

@ApplicationScoped
class ChatModelConfig(
    var aiPlatformOllamaConfig: AIPlatformOllamaConfig,
    var aiPlatformOpenaiConfig: AIPlatformOpenaiConfig
) {

    @Produces
    fun getChatModel(): Map<String, ChatLanguageModel> {
        val chatModelMap = mutableMapOf<String, ChatLanguageModel>()
        aiPlatformOllamaConfig.defaultConfig().map { chatModelMap["ollama"] = ollamaChatModel(it) }
        aiPlatformOpenaiConfig.defaultConfig().map { chatModelMap["openai"] = openaiChatModel(it) }

        aiPlatformOllamaConfig.namedConfig().forEach { (key, value) ->
            chatModelMap[key] = ollamaChatModel(value)
        }

        aiPlatformOpenaiConfig.namedConfig().forEach { (key, value) ->
            chatModelMap[key] = openaiChatModel(value)
        }
        return chatModelMap
    }

    @Produces
    fun getStreamingChatModel(): Map<String, StreamingChatLanguageModel> {
        val streamingChatModelMap = mutableMapOf<String, StreamingChatLanguageModel>()
        aiPlatformOllamaConfig.defaultConfig().map { streamingChatModelMap["ollama"] = ollamaStreamingChatModel(it) }
        aiPlatformOpenaiConfig.defaultConfig().map { streamingChatModelMap["openai"] = openaiStreamingChatModel(it) }

        aiPlatformOllamaConfig.namedConfig().forEach { (key, value) ->
            streamingChatModelMap[key] = ollamaStreamingChatModel(value)
        }

        aiPlatformOpenaiConfig.namedConfig().forEach { (key, value) ->
            streamingChatModelMap[key] = openaiStreamingChatModel(value)
        }
        return streamingChatModelMap
    }

    fun ollamaChatModel(ollama: OllamaProp): ChatLanguageModel {
        return OllamaChatModel.builder()
            .baseUrl(ollama.url())
            .modelName(ollama.modelName())
            .temperature(ollama.temperature())
            .timeout(ollama.timeout())
            .build()
    }

    fun ollamaStreamingChatModel(ollama: OllamaProp): StreamingChatLanguageModel {
        return OllamaStreamingChatModel.builder()
            .baseUrl(ollama.url())
            .modelName(ollama.modelName())
            .temperature(ollama.temperature())
            .timeout(ollama.timeout())
            .build()
    }

    fun openaiChatModel(openai: OpenaiProp): ChatLanguageModel {
        return OpenAiChatModel.builder()
            .baseUrl(openai.url())
            .apiKey(openai.apiKey())
            .modelName(openai.modelName())
            .temperature(openai.temperature())
            .timeout(openai.timeout())
            .build()
    }

    fun openaiStreamingChatModel(openai: OpenaiProp): StreamingChatLanguageModel {
        return OpenAiStreamingChatModel.builder()
            .baseUrl(openai.url())
            .apiKey(openai.apiKey())
            .modelName(openai.modelName())
            .temperature(openai.temperature())
            .timeout(openai.timeout())
            .build()
    }
}
