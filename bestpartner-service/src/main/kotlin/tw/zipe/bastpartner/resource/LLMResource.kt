package tw.zipe.bastpartner.resource

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestStreamElementType
import org.jetbrains.annotations.Blocking
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.ChatRequestDTO
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.service.LLMService
import tw.zipe.bastpartner.util.DTOValidator

/**
 * @author Gary
 * @created 2024/10/07
 */
@Path("/llm")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class LLMResource(
    private val llmService: LLMService
) : BaseLLMResource() {

    @POST
    @Path("/chat")
    fun chat(chatRequestDTO: ChatRequestDTO): ApiResponse<String> {
        DTOValidator.validate(chatRequestDTO) {
            requireNotEmpty("llmId", "message")
            throwOnInvalid()
        }
        val llm = llmService.buildLLM(chatRequestDTO.llmId.orEmpty(), ModelType.CHAT).let {
            it as ChatLanguageModel
        }
        return ApiResponse.success(baseChat(llm, chatRequestDTO.message.orEmpty()))
    }

    @POST
    @Path("/chatStreaming")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @Blocking
    @Transactional
    fun chatStreaming(chatRequestDTO: ChatRequestDTO): Multi<String?> {
        DTOValidator.validate(chatRequestDTO) {
            requireNotEmpty("llmId", "message")
            throwOnInvalid()
        }
        val llm = llmService.buildLLM(chatRequestDTO.llmId.orEmpty(), ModelType.STREAMING_CHAT).let {
            it as StreamingChatLanguageModel
        }
        return baseStreamingChat(llm, chatRequestDTO.message!!)
    }

    @POST
    @Path("/customAssistantChatStreaming")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @Blocking
    @Transactional
    fun customAssistantChatStreaming(chatRequestDTO: ChatRequestDTO): Multi<String?> {
        DTOValidator.validate(chatRequestDTO) {
            requireNotEmpty("llmId", "message")
            throwOnInvalid()
        }
        val aiService = llmService.buildAIService(chatRequestDTO, ModelType.STREAMING_CHAT)
        return Multi.createFrom().emitter<String?> { emitter: MultiEmitter<in String?> ->
            aiService.build().streamingChat(chatRequestDTO.memory.id, chatRequestDTO.message.orEmpty())
                .onPartialResponse { emitter.emit(it) }
                .onCompleteResponse { emitter.complete() }
                .onError { emitter.fail(it) }.start()
        }
    }

    @POST
    @Path("/customAssistantChat")
    fun customAssistantChat(chatRequestDTO: ChatRequestDTO): ApiResponse<String> {
        val aiService = llmService.buildAIService(chatRequestDTO, ModelType.CHAT)
        return ApiResponse.success(
            aiService.build().chat(chatRequestDTO.memory.id, chatRequestDTO.message.orEmpty()).content().text()
        )
    }

}
