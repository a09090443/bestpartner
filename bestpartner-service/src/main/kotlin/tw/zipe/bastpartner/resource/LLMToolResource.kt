package tw.zipe.bastpartner.resource

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ToolDTO
import tw.zipe.bastpartner.service.ToolService

/**
 * @author Gary
 * @created 2024/10/14
 */
@Path("/llm/tool")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMToolResource(
    val toolService: ToolService
) {

    @GET
    @Path("/list")
    fun tools() = toolService.getTools()

    @GET
    @Path("/register")
    fun registerTool(toolDTO: ToolDTO) {
        toolService.registerTool(toolDTO)
    }
}
