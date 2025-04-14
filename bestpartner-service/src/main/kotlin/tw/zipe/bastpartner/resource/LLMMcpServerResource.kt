package tw.zipe.bastpartner.resource

import io.quarkus.security.Authenticated
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.McpDTO
import tw.zipe.bastpartner.enumerate.McpType
import tw.zipe.bastpartner.service.McpServerService
import tw.zipe.bastpartner.util.DTOValidator

/**
 * @author Gary
 * @created 2025/3/20
 */
@Path("/llm/mcpServer")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class LLMMcpServerResource(
    private val mcpServerService: McpServerService
) {

    @GET
    @Path("/list")
    fun mcpServers() = ApiResponse.success(mcpServerService.getMcpServers())

    @POST
    @Path("/get")
    fun getMcpServer(mcpDTO: McpDTO): ApiResponse<McpDTO?> {
        DTOValidator.validate(mcpDTO) {
            requireNotEmpty("mcpId")
            throwOnInvalid()
        }
        return ApiResponse.success(mcpServerService.getMcpServer(mcpDTO.mcpId.orEmpty()))
    }

    @POST
    @Path("/update")
    @RolesAllowed("admin")
    fun updateMcpServer(mcpDTO: McpDTO): ApiResponse<McpDTO> {
        DTOValidator.validate(mcpDTO) {
            DTOValidator.validate(mcpDTO) {
                requireNotEmpty("mcpId", "name", "type")
                if (mcpDTO.type == McpType.STDIO) {
                    requireNotEmpty("command", "args")
                } else if (mcpDTO.type == McpType.SSE) {
                    requireNotEmpty("server")
                }
                throwOnInvalid()
            }
        }
        mcpServerService.updateMcpServer(mcpDTO)
        return ApiResponse.success(mcpDTO)
    }

    @POST
    @Path("/register")
    @RolesAllowed("admin")
    fun register(mcpDTO: McpDTO): ApiResponse<McpDTO> {
        DTOValidator.validate(mcpDTO) {
            requireNotEmpty("name", "type")
            if (mcpDTO.type == McpType.STDIO) {
                requireNotEmpty("command", "args")
            } else if (mcpDTO.type == McpType.SSE) {
                requireNotEmpty("server")
            }
            throwOnInvalid()
        }
        mcpServerService.saveMcpServer(mcpDTO)
        return ApiResponse.success(mcpDTO)
    }

    @POST
    @Path("/delete")
    @RolesAllowed("admin")
    fun delete(mcpDTO: McpDTO): ApiResponse<Boolean> {
        DTOValidator.validate(mcpDTO) {
            requireNotEmpty("mcpId")
            throwOnInvalid()
        }
        return ApiResponse.success(mcpServerService.deleteMcpServer(mcpDTO.mcpId.orEmpty()))
    }

    @POST
    @Path("/saveSetting")
    fun saveSetting(mcpDTO: McpDTO): ApiResponse<McpDTO> {
        DTOValidator.validate(mcpDTO) {
            requireNotEmpty("mcpId", "settingContent")
            throwOnInvalid()
        }
        mcpServerService.saveUserSetting(mcpDTO)
        return ApiResponse.success(mcpDTO)
    }

    @POST
    @Path("/getSetting")
    fun getSetting(mcpDTO: McpDTO): ApiResponse<McpDTO> {
        DTOValidator.validate(mcpDTO) {
            requireNotEmpty("userSettingId")
            throwOnInvalid()
        }
        val data = mcpServerService.getUserSetting(mcpDTO)
        return ApiResponse.success(data)
    }

    @POST
    @Path("/updateSetting")
    fun updateSetting(mcpDTO: McpDTO): ApiResponse<McpDTO> {
        DTOValidator.validate(mcpDTO) {
            requireNotEmpty("userSettingId", "settingContent")
            throwOnInvalid()
        }
        mcpServerService.updateSetting(mcpDTO)
        return ApiResponse.success(mcpDTO)
    }

    @DELETE
    @Path("/deleteSetting")
    fun deleteSetting(mcpDTO: McpDTO): ApiResponse<Boolean> {
        DTOValidator.validate(mcpDTO) {
            requireNotEmpty("userSettingId")
            throwOnInvalid()
        }
        return ApiResponse.success(mcpServerService.deleteMcpUserSetting(mcpDTO.userSettingId.orEmpty()))
    }
}
