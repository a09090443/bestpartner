package tw.zipe.bastpartner.resource

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.UserDTO
import tw.zipe.bastpartner.service.LLMUserService

/**
 * @author Gary
 * @created 2024/10/22
 */
@Path("/llm/user")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMUserResource(
    val llmUserService: LLMUserService
) {

    @POST
    @Path("/register")
    fun register(userDTO: UserDTO): ApiResponse<String> {
        return ApiResponse.success("success")
    }

}
