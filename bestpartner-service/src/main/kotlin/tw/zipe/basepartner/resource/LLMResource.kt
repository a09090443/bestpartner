package tw.zipe.basepartner.resource

import dev.langchain4j.agent.tool.JsonSchemaProperty.description
import dev.langchain4j.agent.tool.JsonSchemaProperty.type
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.service.AiServices
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlin.reflect.full.primaryConstructor
import org.jboss.resteasy.reactive.RestStreamElementType
import tw.zipe.basepartner.assistant.AIAssistant
import tw.zipe.basepartner.assistant.DynamicAssistant
import tw.zipe.basepartner.config.PersistentChatMemoryStore
import tw.zipe.basepartner.dto.ChatRequestDTO
import tw.zipe.basepartner.util.instantiate
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
    @Named("chatModelMap") var chatModelMap: Map<String, ChatLanguageModel>,
    @Named("streamingChatModelMap") var streamingChatModelMap: Map<String, StreamingChatLanguageModel>
) {
    private val logger = logger()

    @POST
    @Path("/chat")
    fun chat(chatRequestDTO: ChatRequestDTO): String {
        logger.info("llmRequestDTO: ${chatRequestDTO.message}")

        val assistant = AiServices.builder(AIAssistant::class.java)
            .chatLanguageModel(chatModelMap[chatRequestDTO.defaultPlatform.name])
            .build()
        val response: Response<AiMessage> = assistant.chat(chatRequestDTO.message)
        logger.info("token counting: ${response.tokenUsage()}")
        return response.content().text()
    }

    @POST
    @Path("/chatStreaming")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    fun chatStreamingTest(llmRequestDTO: ChatRequestDTO): Multi<String?> {

        return Multi.createFrom().emitter<String?> { emitter: MultiEmitter<in String?> ->
            streamingChatModelMap["ollama"]?.generate(
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
        ToolSpecification.builder().name("get_booking_details")
            .description("Returns booking details")
            .addParameter("bookingNumber", type("string"), description("Booking number in B-12345 format"))
            .build()
        val chatMemoryProvider = chatRequestDTO.memoryId?.let {
            ChatMemoryProvider { _: Any? ->
                MessageWindowChatMemory.builder()
                    .id(chatRequestDTO.memoryId)
                    .maxMessages(10)
                    .chatMemoryStore(PersistentChatMemoryStore())
                    .build()
            }
        }

        return AiServices.builder(DynamicAssistant::class.java)
            .chatLanguageModel(chatModelMap[chatRequestDTO.defaultPlatform.name])
            .systemMessageProvider { _ -> chatRequestDTO.promptContent }
            .chatMemoryProvider(chatMemoryProvider)
            .build().chat(chatRequestDTO.message).content().text()
    }

    @POST
    @Path("/agentChat")
    fun agentChat(chatRequestDTO: ChatRequestDTO): String {

        val test = instantiate("tw.zipe.basepartner.tool.DateTool")
        val toolClass = Class.forName("tw.zipe.basepartner.tool.DateTool")
        val kClass = toolClass.kotlin
        val constructor = kClass.primaryConstructor
        val instance = constructor?.call()

        return "Agent chat"
    }

}
