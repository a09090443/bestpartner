package tw.zipe.bastpartner.resource

import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.PermissionDTO
import tw.zipe.bastpartner.service.LLMPermissionService
import tw.zipe.bastpartner.util.DTOValidator

/**
 * @author Gary
 * @created 2024/12/18
 */
@Path("/llm/permission")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("all")
class LLMPermissionResource(
    private val llmPermissionService: LLMPermissionService
) {

    @POST
    @Path("/add")
    fun addPermission(permissionDTO: PermissionDTO): ApiResponse<PermissionDTO> {
        DTOValidator.validate(permissionDTO) {
            requireNotEmpty("name", "num")
            throwOnInvalid()
        }
        llmPermissionService.add(permissionDTO)
        return ApiResponse.success(permissionDTO)
    }

    @POST
    @Path("/update")
    fun updatePermission(permissionDTO: PermissionDTO): ApiResponse<PermissionDTO> {
        DTOValidator.validate(permissionDTO) {
            requireNotEmpty("id", "name", "num")
            throwOnInvalid()
        }
        llmPermissionService.update(permissionDTO)
        return ApiResponse.success(permissionDTO)
    }

    @DELETE
    @Path("/delete")
    fun deletePermission(permissionDTO: PermissionDTO): ApiResponse<Boolean> {
        DTOValidator.validate(permissionDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        return ApiResponse.success(llmPermissionService.delete(permissionDTO.id.orEmpty()))
    }
}
