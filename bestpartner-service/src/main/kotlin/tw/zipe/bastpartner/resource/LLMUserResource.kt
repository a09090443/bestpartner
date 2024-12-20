package tw.zipe.bastpartner.resource

import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.UserDTO
import tw.zipe.bastpartner.enumerate.UserStatus
import tw.zipe.bastpartner.service.LLMUserService
import tw.zipe.bastpartner.util.DTOValidator

/**
 * @author Gary
 * @created 2024/10/22
 */
@Path("/llm/user")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMUserResource(
    val llmUserService: LLMUserService,
    val identity: SecurityIdentity
) {

    @POST
    @Path("/register")
    fun register(userDTO: UserDTO): ApiResponse<UserDTO> {
        DTOValidator.validate(userDTO) {
            requireNotEmpty("username")
            requireNotEmpty("password")
            requireNotEmpty("email")
            userDTO.status?.let {
                UserStatus.fromOrdinal(it)
            }
            throwOnInvalid()
        }
        llmUserService.register(userDTO)
        return ApiResponse.success(userDTO)
    }

    @POST
    @Path("/get")
    @Authenticated
    fun get(userDTO: UserDTO): ApiResponse<UserDTO> {
        return ApiResponse.success(llmUserService.findUserById(identity.principal.name))
    }

    @POST
    @Path("/switchStatus")
    @RolesAllowed("all")
    fun switchStatus(userDTO: UserDTO): ApiResponse<UserDTO> {
        DTOValidator.validate(userDTO) {
            requireNotEmpty("id")
            requireNotEmpty("status")
            throwOnInvalid()
        }
        val user = llmUserService.findUserById(userDTO.id!!).let {
            it.status = userDTO.status
            llmUserService.updateUser(it)
            it
        }
        return ApiResponse.success(user)
    }

    @POST
    @Path("/update")
    @Authenticated
    fun update(userDTO: UserDTO): ApiResponse<UserDTO> {
        DTOValidator.validate(userDTO) {
            requireNotEmpty("id")
            requireNotEmpty("email")
            requireNotEmpty("status")
            throwOnInvalid()
        }
        llmUserService.updateUser(userDTO)
        return ApiResponse.success(userDTO)
    }

    @DELETE
    @Path("/delete")
    @RolesAllowed("all")
    fun delete(userDTO: UserDTO): ApiResponse<Boolean> {
        DTOValidator.validate(userDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        return ApiResponse.success(llmUserService.deleteUser(userDTO.id!!))
    }
}
