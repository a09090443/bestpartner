package tw.zipe.basepartner.resource

import dev.langchain4j.memory.chat.ChatMemoryProvider
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.service.AiServices
import io.smallrye.mutiny.Multi
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestStreamElementType
import tw.zipe.basepartner.assistant.DynamicAssistant
import tw.zipe.basepartner.config.PersistentChatMemoryStore
import tw.zipe.basepartner.dto.ApiResponse
import tw.zipe.basepartner.dto.ChatRequestDTO
import tw.zipe.basepartner.enumerate.ModelType
import tw.zipe.basepartner.service.LLMService
import tw.zipe.basepartner.util.DTOValidator
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
    private val lLMService: LLMService
) : BaseLLMResource() {
    private val logger = logger()

    @POST
    @Path("/chat")
    fun chat(chatRequestDTO: ChatRequestDTO): ApiResponse<String> {
        DTOValidator.validate(chatRequestDTO) {
            requireNotEmpty("llmId", "message")
            throwOnInvalid()
        }

        val llm = chatRequestDTO.llmId.let {
            lLMService.buildLLM(it!!, ModelType.CHAT)
        }.let {
            it as ChatLanguageModel
        }

        return ApiResponse.success(baseChat(llm, chatRequestDTO.message!!))
    }

    @POST
    @Path("/chatStreaming")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    fun chatStreamingTest(chatRequestDTO: ChatRequestDTO): Multi<String?> {
        DTOValidator.validate(chatRequestDTO) {
            requireNotEmpty("llmId", "message")
            throwOnInvalid()
        }

        val llm = chatRequestDTO.llmId.let {
            lLMService.buildLLM(it!!, ModelType.STREAMING_CHAT)
        }.let {
            it as StreamingChatLanguageModel
        }

        return baseStreamingChat(llm, chatRequestDTO.message!!)
    }

    @POST
    @Path("/customAssistantChat")
    fun customAssistantChat(chatRequestDTO: ChatRequestDTO): ApiResponse<String> {
        DTOValidator.validate(chatRequestDTO) {
            requireNotEmpty("llmId", "message")
            throwOnInvalid()
        }

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

        return ApiResponse.success(aiService.build().chat(chatRequestDTO.message!!).content().text())
    }
}
