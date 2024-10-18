package tw.zipe.basepartner.resource

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.memory.chat.MessageWindowChatMemory
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
import org.jboss.resteasy.reactive.RestStreamElementType
import tw.zipe.basepartner.assistant.AIAssistant
import tw.zipe.basepartner.assistant.DynamicAssistant
import tw.zipe.basepartner.config.PersistentChatMemoryStore
import tw.zipe.basepartner.dto.ChatRequestDTO
import tw.zipe.basepartner.dto.LLMDTO
import tw.zipe.basepartner.service.LLMService
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
    @Named("chatModelMap") val chatModelMap: Map<String, ChatLanguageModel>,
    @Named("streamingChatModelMap") val streamingChatModelMap: Map<String, StreamingChatLanguageModel>,
    private val lLMService: LLMService
) {
    private val logger = logger()

    @POST
    @Path("/chat")
    fun chat(chatRequestDTO: ChatRequestDTO): String {
        logger.info("llmRequestDTO: ${chatRequestDTO.message}")
        val llm = chatRequestDTO.llmId?.let {
            lLMService.buildLLM(it)
        }?.let {
            it as ChatLanguageModel
        } ?: throw IllegalArgumentException("llmId is required")

        val assistant = AiServices.builder(AIAssistant::class.java)
            .chatLanguageModel(llm)
            .build()
        val response: Response<AiMessage> = assistant.chat(chatRequestDTO.message)
        logger.info("token counting: ${response.tokenUsage()}")
        return response.content().text()
    }

    @POST
    @Path("/chatStreaming")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    fun chatStreamingTest(chatRequestDTO: ChatRequestDTO): Multi<String?> {
        val assistant = AiServices.builder(AIAssistant::class.java)
            .streamingChatLanguageModel(streamingChatModelMap[chatRequestDTO.platform.name])
            .build()

        return Multi.createFrom().emitter<String?> { emitter: MultiEmitter<in String?> ->
            assistant.streamingChat(chatRequestDTO.message)
                .onNext { emitter.emit(it) }
                .onComplete { emitter.complete() }
                .onError { emitter.fail(it) }.start()
        }
    }

    @POST
    @Path("/customAssistantChat")
    fun customAssistantChat(chatRequestDTO: ChatRequestDTO): String {

        val aiService = AiServices.builder(DynamicAssistant::class.java)
            .chatLanguageModel(chatModelMap[chatRequestDTO.platform.name])
            .systemMessageProvider { _ -> chatRequestDTO.promptContent }

        val tools = chatRequestDTO.tools?.map {
            instantiate(it.classPath)
        } ?: emptyList()

        tools.isNotEmpty().let {
            aiService.tools(tools)
        }

        if (chatRequestDTO.isRemember) {
            val chatMemoryProvider = chatRequestDTO.memory?.let {
                ChatMemoryProvider { _: Any? ->
                    MessageWindowChatMemory.builder()
                        .id(it.id)
                        .maxMessages(it.maxSize)
                        .chatMemoryStore(PersistentChatMemoryStore())
                        .build()
                }
            }
            aiService.chatMemoryProvider(chatMemoryProvider)
        }

        return aiService.build().chat(chatRequestDTO.message).content().text()
    }

    @POST
    @Path("/saveLLM")
    fun saveLlmChatModel(llmDTO: LLMDTO) {
        llmDTO.llmModel?.let {
            lLMService.saveLLM(llmDTO)
        }
    }
}
