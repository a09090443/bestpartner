package tw.zipe.bastpartner.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Named
import tw.zipe.bastpartner.config.LLMStore.LLMStore.SYSTEM_DEFAULT_MODEL
import tw.zipe.bastpartner.config.LLMStore.LLMStore.SYSTEM_DEFAULT_SETTING_PREFIX
import tw.zipe.bastpartner.config.chatmodel.OllamaChatModelConfig
import tw.zipe.bastpartner.config.chatmodel.OpenaiChatModelConfig
import tw.zipe.bastpartner.config.embedding.OllamaEmbeddingModelConfig
import tw.zipe.bastpartner.config.embedding.OpenaiEmbeddingModelConfig
import tw.zipe.bastpartner.enumerate.Platform
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.LLMService
import tw.zipe.bastpartner.service.LLMUserService
import tw.zipe.bastpartner.service.SystemService

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
    private val llmService: LLMService,
    private val llmUserService: LLMUserService,
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

//        ollamaChatModelConfig.buildChatModel()?.let { chatModelMap[Platform.OLLAMA.name] = it }
//        openaiChatModelConfig.buildChatModel()?.let { chatModelMap[Platform.OPENAI.name] = it }
        systemService.getSystemSettingValue(SYSTEM_DEFAULT_MODEL)?.let {
            llmUserService.findUserByName("admin")?.let {
                llmService.getLLMSetting(it.id.orEmpty(), null, null ).forEach { llModel ->
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

                            else -> {
                                throw ServiceException("Platform not found")
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

//        ollamaChatModelConfig.buildStreamingChatModel()?.let { streamingChatModelMap[Platform.OLLAMA.name] = it }
//        openaiChatModelConfig.buildStreamingChatModel()?.let { streamingChatModelMap[Platform.OPENAI.name] = it }

        systemService.getSystemSettingValue(SYSTEM_DEFAULT_MODEL)?.let {
            val user = llmUserService.findUserByName("admin") ?: throw ServiceException("User not found")
            llmService.getLLMSetting(user.id.orEmpty(), null, null).forEach { llModel ->
                if (llModel != null) {
                    when (llModel.platform) {
                        Platform.OLLAMA -> {
                            ollamaChatModelConfig.buildStreamingChatModel()
                                ?.let { model ->
                                    streamingChatModelMap[SYSTEM_DEFAULT_SETTING_PREFIX + Platform.OLLAMA.name] = model
                                }
                        }

                        Platform.OPENAI -> {
                            openaiChatModelConfig.buildStreamingChatModel()
                                ?.let { model ->
                                    streamingChatModelMap[SYSTEM_DEFAULT_SETTING_PREFIX + Platform.OPENAI.name] = model
                                }
                        }

                        else -> {
                            throw ServiceException("Platform not found")
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

//        ollamaEmbeddingModelConfig.buildEmbeddingModel()?.let { embeddingChatModelMap[Platform.OLLAMA.name] = it }
//        openaiEmbeddingModelConfig.buildEmbeddingModel()?.let { embeddingChatModelMap[Platform.OPENAI.name] = it }

        return embeddingChatModelMap
    }
}
