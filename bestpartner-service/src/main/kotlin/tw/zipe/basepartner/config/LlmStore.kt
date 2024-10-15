package tw.zipe.basepartner.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Named
import tw.zipe.basepartner.builder.aigcmodel.OllamaModelBuilder
import tw.zipe.basepartner.builder.aigcmodel.OpenaiModelBuilder
import tw.zipe.basepartner.config.chatmodel.OllamaChatModelConfig
import tw.zipe.basepartner.config.chatmodel.OpenaiChatModelConfig
import tw.zipe.basepartner.config.embedding.OllamaEmbeddingModelConfig
import tw.zipe.basepartner.config.embedding.OpenaiEmbeddingModelConfig
import tw.zipe.basepartner.enumerate.Platform

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class LlmStore(
    private val ollamaChatModelConfig: OllamaChatModelConfig,
    private val openaiChatModelConfig: OpenaiChatModelConfig,
    private val ollamaEmbeddingModelConfig: OllamaEmbeddingModelConfig,
    private val openaiEmbeddingModelConfig: OpenaiEmbeddingModelConfig
) {

    @Produces
    @Named("chatModelMap")
    fun getChatModelMap(): Map<String, ChatLanguageModel> {

        val chatModelMap = mutableMapOf<String, ChatLanguageModel>()

        ollamaChatModelConfig.buildChatModel()?.let { chatModelMap[Platform.OLLAMA.name] = it }
        ollamaChatModelConfig.aiPlatformOllamaConfig.namedConfig().forEach { (key, value) ->
            chatModelMap[key] = ollamaChatModelConfig.buildChatModel(
                ollamaChatModelConfig.convertChatModelSetting(value, Platform.OLLAMA),
                OllamaModelBuilder()
            )
        }

        openaiChatModelConfig.buildChatModel()?.let { chatModelMap[Platform.OPENAI.name] = it }
        openaiChatModelConfig.aiPlatformOpenaiConfig.namedConfig().forEach { (key, value) ->
            chatModelMap[key] = openaiChatModelConfig.buildChatModel(
                openaiChatModelConfig.convertChatModelSetting(value, Platform.OPENAI),
                OpenaiModelBuilder()
            )
        }

        return chatModelMap
    }

    @Produces
    @Named("streamingChatModelMap")
    fun getStreamingChatModel(): Map<String, StreamingChatLanguageModel> {

        val streamingChatModelMap = mutableMapOf<String, StreamingChatLanguageModel>()

        ollamaChatModelConfig.buildStreamingChatModel()?.let { streamingChatModelMap[Platform.OLLAMA.name] = it }
        ollamaChatModelConfig.aiPlatformOllamaConfig.namedConfig().forEach { (key, value) ->
            streamingChatModelMap[key] = ollamaChatModelConfig.buildStreamingChatModel(
                ollamaChatModelConfig.convertChatModelSetting(value, Platform.OLLAMA),
                OllamaModelBuilder()
            )
        }

        openaiChatModelConfig.buildStreamingChatModel()?.let { streamingChatModelMap[Platform.OPENAI.name] = it }
        openaiChatModelConfig.aiPlatformOpenaiConfig.namedConfig().forEach { (key, value) ->
            streamingChatModelMap[key] = openaiChatModelConfig.buildStreamingChatModel(
                openaiChatModelConfig.convertChatModelSetting(value, Platform.OPENAI),
                OpenaiModelBuilder()
            )
        }

        return streamingChatModelMap
    }

    @Produces
    @Named("embeddingModelMap")
    fun getEmbeddingModel(): Map<String, EmbeddingModel> {

        val embeddingChatModelMap = mutableMapOf<String, EmbeddingModel>()

        embeddingChatModelMap["default"] = BgeSmallEnV15QuantizedEmbeddingModel()

        ollamaEmbeddingModelConfig.buildEmbeddingModel()?.let { embeddingChatModelMap[Platform.OLLAMA.name] = it }
        ollamaEmbeddingModelConfig.aiPlatformOllamaConfig.namedConfig().forEach { (key, value) ->
            embeddingChatModelMap[key] = ollamaEmbeddingModelConfig.buildEmbeddingModel(
                ollamaEmbeddingModelConfig.convertEmbeddingModelSetting(value, Platform.OLLAMA),
                OllamaModelBuilder()
            )
        }

        openaiEmbeddingModelConfig.buildEmbeddingModel()?.let { embeddingChatModelMap[Platform.OPENAI.name] = it }
        openaiEmbeddingModelConfig.aiPlatformOpenaiConfig.namedConfig().forEach { (key, value) ->
            embeddingChatModelMap[key] = openaiEmbeddingModelConfig.buildEmbeddingModel(
                openaiEmbeddingModelConfig.convertEmbeddingModelSetting(value, Platform.OPENAI),
                OpenaiModelBuilder()
            )
        }

        return embeddingChatModelMap
    }
}
