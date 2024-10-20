package tw.zipe.basepartner.resource

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.basepartner.dto.LLMDTO
import tw.zipe.basepartner.service.LLMService

/**
 * @author Gary
 * @created 2024/10/20
 */
@Path("/llm/setting")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMSettingResource(
    private val lLMService: LLMService
) {

    @POST
    @Path("/saveLLM")
    fun saveLlmChatModel(llmDTO: LLMDTO) {
        llmDTO.llmModel?.let {
            lLMService.saveLLM(llmDTO)
        }
    }
}
