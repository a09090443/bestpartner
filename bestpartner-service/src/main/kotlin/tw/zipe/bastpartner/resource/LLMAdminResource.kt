package tw.zipe.bastpartner.resource

import io.smallrye.mutiny.Multi
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.ChatRequestDTO
import tw.zipe.bastpartner.exception.ServiceException

/**
 * @author Gary
 * @created 2024/10/17
 */
@Path("/llm/admin")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMAdminResource : BaseLLMResource() {

    @POST
    @Path("/chat")
    fun chat(chatRequestDTO: ChatRequestDTO): ApiResponse<String> {
        val llm = chatModelMap[chatRequestDTO.platform.name] ?: throw ServiceException("Did not find the LLM model")
        return ApiResponse.success(baseChat(llm, chatRequestDTO.message!!))
    }

    @POST
    @Path("/customAssistantChat")
    fun customAssistantChat(chatRequestDTO: ChatRequestDTO): Multi<String?> {
        val llm = streamingChatModelMap[chatRequestDTO.platform.name] ?: throw ServiceException("Did not find the LLM model")
        return baseStreamingChat(llm, chatRequestDTO.message!!)
    }
}
