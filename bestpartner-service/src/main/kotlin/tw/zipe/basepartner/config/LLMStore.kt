package tw.zipe.basepartner.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Named
import tw.zipe.basepartner.config.LLMStore.LLMStore.SYSTEM_DEFAULT_MODEL
import tw.zipe.basepartner.config.LLMStore.LLMStore.SYSTEM_DEFAULT_SETTING_PREFIX
import tw.zipe.basepartner.config.chatmodel.OllamaChatModelConfig
import tw.zipe.basepartner.config.chatmodel.OpenaiChatModelConfig
import tw.zipe.basepartner.config.embedding.OllamaEmbeddingModelConfig
import tw.zipe.basepartner.config.embedding.OpenaiEmbeddingModelConfig
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.service.LLMService
import tw.zipe.basepartner.service.SystemService

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class LLMStore(
    private val ollamaChatModelConfig: OllamaChatModelConfig,
    private val openaiChatModelConfig: OpenaiChatModelConfig,
    private val ollamaEmbeddingModelConfig: OllamaEmbeddingModelConfig,
    private val openaiEmbeddingModelConfig: OpenaiEmbeddingModelConfig,
    private val lLMService: LLMService,
    private val systemService: SystemService
) {

    object LLMStore {
        const val SYSTEM_DEFAULT_SETTING_PREFIX = "SYSTEM-"
        const val SYSTEM_DEFAULT_MODEL = "default_model"
    }

    @Produces
    @Named("chatModelMap")
    fun getChatModelMap(): Map<String, ChatLanguageModel> {

        val chatModelMap = mutableMapOf<String, ChatLanguageModel>()

        ollamaChatModelConfig.buildChatModel()?.let { chatModelMap[Platform.OLLAMA.name] = it }
        openaiChatModelConfig.buildChatModel()?.let { chatModelMap[Platform.OPENAI.name] = it }
        systemService.getSystemSettingValue(SYSTEM_DEFAULT_MODEL)?.let {
            Platform.valueOf(it).let { platform ->
                lLMService.getLLMSetting(platform, "SYSTEM").forEach { llModel ->
                    if (llModel != null) {
                        when (llModel.platform) {
                            Platform.OLLAMA -> {
                                ollamaChatModelConfig.buildChatModel()
                                    ?.let { model ->
                                        chatModelMap[SYSTEM_DEFAULT_SETTING_PREFIX + Platform.OLLAMA.name] = model
                                    }
                            }

                            Platform.OPENAI -> {
                                openaiChatModelConfig.buildChatModel()
                                    ?.let { model ->
                                        chatModelMap[SYSTEM_DEFAULT_SETTING_PREFIX + Platform.OPENAI.name] = model
                                    }
                            }
                        }
                    }
                }
            }
        }
        return chatModelMap
    }

    @Produces
    @Named("streamingChatModelMap")
    fun getStreamingChatModel(): Map<String, StreamingChatLanguageModel> {

        val streamingChatModelMap = mutableMapOf<String, StreamingChatLanguageModel>()

        ollamaChatModelConfig.buildStreamingChatModel()?.let { streamingChatModelMap[Platform.OLLAMA.name] = it }
        openaiChatModelConfig.buildStreamingChatModel()?.let { streamingChatModelMap[Platform.OPENAI.name] = it }

        systemService.getSystemSettingValue(SYSTEM_DEFAULT_MODEL)?.let {

            Platform.valueOf(it).let { platform ->
                {
                    lLMService.getLLMSetting(platform, "SYSTEM").forEach { llModel ->
                        if (llModel != null) {
                            when (llModel.platform) {
                                Platform.OLLAMA -> {
                                    ollamaChatModelConfig.buildStreamingChatModel()
                                        ?.let { model ->
                                            streamingChatModelMap[SYSTEM_DEFAULT_SETTING_PREFIX + Platform.OLLAMA.name] =
                                                model
                                        }
                                }

                                Platform.OPENAI -> {
                                    openaiChatModelConfig.buildStreamingChatModel()
                                        ?.let { model ->
                                            streamingChatModelMap[SYSTEM_DEFAULT_SETTING_PREFIX + Platform.OPENAI.name] =
                                                model
                                        }
                                }
                            }
                        }
                    }
                }

            }
        }

        return streamingChatModelMap
    }

    @Produces
    @Named("embeddingModelMap")
    fun getEmbeddingModel(): Map<String, EmbeddingModel> {

        val embeddingChatModelMap = mutableMapOf<String, EmbeddingModel>()

        embeddingChatModelMap["default"] = BgeSmallEnV15QuantizedEmbeddingModel()

        ollamaEmbeddingModelConfig.buildEmbeddingModel()?.let { embeddingChatModelMap[Platform.OLLAMA.name] = it }
        openaiEmbeddingModelConfig.buildEmbeddingModel()?.let { embeddingChatModelMap[Platform.OPENAI.name] = it }

        lLMService.getLLMSetting(Platform.OLLAMA, "SYSTEM").forEach { llModel ->
            if (llModel != null) {
                when (llModel.platform) {
                    Platform.OLLAMA -> {
                        ollamaEmbeddingModelConfig.buildEmbeddingModel()
                            ?.let { embeddingChatModelMap[SYSTEM_DEFAULT_SETTING_PREFIX + Platform.OLLAMA.name] = it }
                    }

                    Platform.OPENAI -> {
                        openaiEmbeddingModelConfig.buildEmbeddingModel()
                            ?.let { embeddingChatModelMap[SYSTEM_DEFAULT_SETTING_PREFIX + Platform.OPENAI.name] = it }
                    }
                }
            }
        }
        return embeddingChatModelMap
    }
}
