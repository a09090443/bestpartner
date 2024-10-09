package tw.zipe.basepartner.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Named
import tw.zipe.basepartner.builder.chatmodel.OllamaBuilder
import tw.zipe.basepartner.builder.chatmodel.OpenaiBuilder
import tw.zipe.basepartner.enumerate.Platform

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class AigcModelStore(
    private val ollamaChatModelConfig: OllamaChatModelConfig,
    private val openaiChatModelConfig: OpenaiChatModelConfig
) {

    @Produces
    @Named("chatModelMap")
    fun getChatModelMap(): Map<String, ChatLanguageModel> {

        val chatModelMap = mutableMapOf<String, ChatLanguageModel>()
        ollamaChatModelConfig.buildChatModel()?.let { chatModelMap[Platform.OLLAMA.name] = it }
        ollamaChatModelConfig.aiPlatformOllamaConfig.namedConfig().forEach { (key, value) ->
            chatModelMap[key] = ollamaChatModelConfig.buildChatModel(
                ollamaChatModelConfig.convertChatModelSetting(value),
                OllamaBuilder()
            )
        }

        openaiChatModelConfig.buildChatModel()?.let { chatModelMap[Platform.OPENAI.name] = it }
        openaiChatModelConfig.aiPlatformOpenaiConfig.namedConfig().forEach { (key, value) ->
            chatModelMap[key] = openaiChatModelConfig.buildChatModel(
                openaiChatModelConfig.convertChatModelSetting(value),
                OpenaiBuilder()
            )
        }

        return chatModelMap
    }

    @Produces
    @Named("streamingChatModelMap")
    fun getStreamingChatModel(): Map<String, StreamingChatLanguageModel> {
        val streamingChatModelMap = mutableMapOf<String, StreamingChatLanguageModel>()
        streamingChatModelMap.ifEmpty {
            ollamaChatModelConfig.buildStreamingChatModel()?.let { streamingChatModelMap[Platform.OLLAMA.name] = it }
            openaiChatModelConfig.buildStreamingChatModel()?.let { streamingChatModelMap[Platform.OPENAI.name] = it }
        }
        return streamingChatModelMap
    }
}
