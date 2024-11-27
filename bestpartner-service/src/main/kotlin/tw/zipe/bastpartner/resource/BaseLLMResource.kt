package tw.zipe.bastpartner.resource

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.service.AiServices
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import jakarta.inject.Inject
import jakarta.inject.Named
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

    @Inject
    @Named("chatModelMap")
    protected lateinit var chatModelMap: Map<String, ChatLanguageModel>

    @Inject
    @Named("streamingChatModelMap")
    protected lateinit var streamingChatModelMap: Map<String, StreamingChatLanguageModel>

    fun baseChat(llm: ChatLanguageModel, message: String): String {
        val assistant = AiServices.builder(AIAssistant::class.java)
            .chatLanguageModel(llm)
            .build()
        val response: Response<AiMessage> = assistant.chat(message)
        logger.info("token counting: ${response.tokenUsage()}")
        return response.content().text()
    }

    fun baseStreamingChat(llm: StreamingChatLanguageModel, message: String): Multi<String?> {
        val assistant = AiServices.builder(AIAssistant::class.java)
            .streamingChatLanguageModel(llm)
            .build()

        return Multi.createFrom().emitter<String?> { emitter: MultiEmitter<in String?> ->
            assistant.streamingChat(message)
                .onNext { emitter.emit(it) }
                .onComplete { emitter.complete() }
                .onError { emitter.fail(it) }.start()
        }
    }

}
