package tw.zipe.basepartner.resource

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.output.Response
import dev.langchain4j.service.AiServices
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestStreamElementType
import tw.zipe.basepartner.assistant.AIAssistant
import tw.zipe.basepartner.assistant.DynamicAssistant
import tw.zipe.basepartner.config.ChatModelConfig
import tw.zipe.basepartner.dto.ChatRequestDTO
import tw.zipe.basepartner.service.PersistentChatMemoryStore
import tw.zipe.basepartner.util.logger

/**
 * @author Gary
 * @created 2024/10/07
 */
@Path("/llm")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMResource(
    var chatModelConfig: ChatModelConfig
) {
    private val logger = logger()

    @POST
    @Path("/chat")
    fun chat(llmRequestDTO: ChatRequestDTO): String {
        logger.info("llmRequestDTO: ${llmRequestDTO.message}")
        return AiServices.create(AIAssistant::class.java, chatModelConfig.getChatModel()[llmRequestDTO.model])
            .chat(llmRequestDTO.message)
    }

    @POST
    @Path("/chatStreaming")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    fun chatStreamingTest(llmRequestDTO: ChatRequestDTO): Multi<String?> {

        return Multi.createFrom().emitter<String?> { emitter: MultiEmitter<in String?> ->
            chatModelConfig.getStreamingChatModel()["ollama"]?.generate(
                llmRequestDTO.message,
                object : StreamingResponseHandler<AiMessage> {
                    override fun onNext(token: String) {
                        emitter.emit(token)
                    }

                    override fun onError(error: Throwable) {
                        emitter.fail(error)
                    }

                    override fun onComplete(response: Response<AiMessage?>?) {
                        emitter.complete()
                    }
                }) ?: emitter.fail(RuntimeException("No model found"))
        }
    }

    @POST
    @Path("/customAssistantChat")
    fun customAssistantChat(chatRequestDTO: ChatRequestDTO): String {
        val chatMemoryProvider = ChatMemoryProvider { memoryId: Any? ->
            MessageWindowChatMemory.builder()
                .id(chatRequestDTO.memoryId)
                .maxMessages(10)
                .chatMemoryStore(PersistentChatMemoryStore())
                .build()
        }

        return AiServices.builder(DynamicAssistant::class.java)
            .chatLanguageModel(chatModelConfig.getChatModel()[chatRequestDTO.model])
            .systemMessageProvider { _ -> chatRequestDTO.promptContent }
            .chatMemoryProvider(chatMemoryProvider)
            .build().chat(chatRequestDTO.message)
    }

}
