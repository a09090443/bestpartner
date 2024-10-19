package tw.zipe.basepartner.resource

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.service.AiServices
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.basepartner.assistant.AIAssistant
import tw.zipe.basepartner.dto.ChatRequestDTO
import tw.zipe.basepartner.exception.ServiceException
import tw.zipe.basepartner.util.logger

/**
 * @author Gary
 * @created 2024/10/17
 */
@Path("/llm/admin")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMAdminResource(
    @Named("chatModelMap") val chatModelMap: Map<String, ChatLanguageModel>
) {
    private val logger = logger()

    @POST
    @Path("/chat")
    fun chat(chatRequestDTO: ChatRequestDTO): String {
        logger.info("llmRequestDTO: ${chatRequestDTO.message}")
        val llm = chatModelMap[chatRequestDTO.platform.name] ?: throw ServiceException("Did not find the LLM model")

        val assistant = AiServices.builder(AIAssistant::class.java)
            .chatLanguageModel(llm)
            .build()
        val response: Response<AiMessage> = assistant.chat(chatRequestDTO.message)
        logger.info("token counting: ${response.tokenUsage()}")
        return response.content().text()
    }

}
