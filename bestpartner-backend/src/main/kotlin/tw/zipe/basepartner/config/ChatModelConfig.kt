package tw.zipe.basepartner.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.builder.chatmodel.OllamaBuilder
import tw.zipe.basepartner.builder.chatmodel.OpenaiBuilder
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.model.ChatModel
import tw.zipe.basepartner.properties.AIPlatformOllamaConfig
import tw.zipe.basepartner.properties.AIPlatformOpenaiConfig
import tw.zipe.basepartner.properties.OllamaProp
import tw.zipe.basepartner.properties.OpenaiProp
import tw.zipe.basepartner.util.logger

/**
 * @author Gary
 * @created 2024/10/07
 */
@ApplicationScoped
class ChatModelConfig(
    var aiPlatformOllamaConfig: AIPlatformOllamaConfig,
    var aiPlatformOpenaiConfig: AIPlatformOpenaiConfig
) {

    private val logger = logger()
    private val chatModelMap = mutableMapOf<String, ChatLanguageModel>()

    fun getChatModel(): Map<String, ChatLanguageModel> {
        logger.info("根據設定檔建立llm連線: $aiPlatformOllamaConfig")
        chatModelMap.ifEmpty {
            aiPlatformOllamaConfig.defaultConfig().map { chatModelMap[Platform.OLLAMA.name] = ollamaChatModel(it) }
            aiPlatformOpenaiConfig.defaultConfig().map { chatModelMap[Platform.OPENAI.name] = openaiChatModel(it) }

            aiPlatformOllamaConfig.namedConfig().forEach { (key, value) ->
                chatModelMap[key] = ollamaChatModel(value)
            }

            aiPlatformOpenaiConfig.namedConfig().forEach { (key, value) ->
                chatModelMap[key] = openaiChatModel(value)
            }
        }

        return chatModelMap
    }

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
        return run {
            ChatModel(
                platform = Platform.OLLAMA,
                url = ollama.url(),
                modelName = ollama.modelName(),
                temperature = ollama.temperature(),
                timeout = ollama.timeout().toMillis()
            )
        }.run {
            OllamaBuilder().chatModel(this)
        }
    }

    fun ollamaStreamingChatModel(ollama: OllamaProp): StreamingChatLanguageModel {
        return run {
            ChatModel(
                platform = Platform.OLLAMA,
                url = ollama.url(),
                modelName = ollama.modelName(),
                temperature = ollama.temperature(),
                timeout = ollama.timeout().toMillis()
            )
        }.run {
            OllamaBuilder().chatModelStreaming(this)
        }
    }

    fun openaiChatModel(openai: OpenaiProp): ChatLanguageModel {
        return run {
            ChatModel(
                platform = Platform.OPENAI,
                url = openai.url(),
                apiKey = openai.apiKey(),
                modelName = openai.modelName(),
                temperature = openai.temperature(),
                timeout = openai.timeout().toMillis()
            )
        }.run {
            OpenaiBuilder().chatModel(this)
        }
    }

    fun openaiStreamingChatModel(openai: OpenaiProp): StreamingChatLanguageModel {
        return run {
            ChatModel(
                platform = Platform.OPENAI,
                url = openai.url(),
                apiKey = openai.apiKey(),
                modelName = openai.modelName(),
                temperature = openai.temperature(),
                timeout = openai.timeout().toMillis()
            )
        }.run {
            OpenaiBuilder().chatModelStreaming(this)
        }
    }
}
