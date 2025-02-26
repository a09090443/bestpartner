package tw.zipe.bastpartner.resource

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.TokenStream
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import jakarta.inject.Inject
import java.time.Duration
import tw.zipe.bastpartner.assistant.AIAssistant
import tw.zipe.bastpartner.util.logger

/**
 * @author Gary
 * @created 2024/10/17
 */
abstract class BaseLLMResource {
    private val logger = logger()

    @Inject
    protected lateinit var identity: SecurityIdentity

    fun baseChat(llm: ChatLanguageModel, message: String): String {
        val assistant = AiServices.builder(AIAssistant::class.java)
            .chatLanguageModel(llm)
            .build()
        val response: Response<AiMessage> = assistant.chat(message)
        logger.info("token counting: ${response.tokenUsage()}")
        return response.content().text()
    }

    fun baseStreamingChat(llm: StreamingChatLanguageModel, message: String): Multi<String?> {
        // 使用 lazy 初始化 assistant，確保資源不會過早創建
        val assistant by lazy {
            AiServices.builder(AIAssistant::class.java)
                .streamingChatLanguageModel(llm)
                .build()
        }

        return Multi.createFrom().emitter<String?> { emitter: MultiEmitter<in String?> ->
            val streamingChat: TokenStream
            try {
                streamingChat = assistant.streamingChat(message)
                    .onPartialResponse { response ->
                        if (!emitter.isCancelled) {
                            try {
                                emitter.emit(response)
                            } catch (e: Exception) {
                                emitter.fail(e)
                            }
                        }
                    }
                    .onCompleteResponse {
                        if (!emitter.isCancelled) {
                            emitter.complete()
                        }
                    }
                    .onError { error ->
                        if (!emitter.isCancelled) {
                            emitter.fail(error)
                        }
                    }
                streamingChat.start()
            } catch (e: Exception) {
                logger.error("Error occurred while starting streaming chat", e)
                emitter.fail(e)
            }

        }.onItem()
            .transform { item -> item ?: "" }  // 處理空值
            .onOverflow()
            .buffer(128)  // 設置緩衝區
            .onFailure()
            .retry()
            .atMost(3)  // 最多重試3次
            .ifNoItem()
            .after(Duration.ofSeconds(30))  // 30秒超時
            .fail()
    }

}
