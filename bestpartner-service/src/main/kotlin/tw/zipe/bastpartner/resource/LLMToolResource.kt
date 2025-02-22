package tw.zipe.bastpartner.resource

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.ToolDTO
import tw.zipe.bastpartner.enumerate.ToolsType
import tw.zipe.bastpartner.service.ToolService
import tw.zipe.bastpartner.util.DTOValidator

/**
 * @author Gary
 * @created 2024/10/14
 */
@Path("/llm/tool")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMToolResource(
    val toolService: ToolService,
    private val objectMapper: ObjectMapper
) : BaseLLMResource() {

    @GET
    @Path("/list")
    fun tools() = ApiResponse.success(toolService.getTools())

    @POST
    @Path("/get")
    fun get(toolDTO: ToolDTO): ApiResponse<ToolDTO> {
        DTOValidator.validate(toolDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        return ApiResponse.success(toolService.getTool(toolDTO.id.orEmpty()))
    }

    @POST
    @Path("/register")
    @RolesAllowed("admin")
    fun register(toolDTO: ToolDTO): ApiResponse<ToolDTO> {
        DTOValidator.validate(toolDTO) {
            requireNotEmpty("name", "classPath", "groupId", "type")
            if (toolDTO.type == ToolsType.CUSTOMIZE) {
                requireNotEmpty("functionName", "functionDescription")
            }
            toolDTO.functionParams?.let {
                val map: Map<String, List<Any>> =
                    objectMapper.readValue(it, object : TypeReference<Map<String, List<Any>>>() {})
                toolService.validateFunctionParams(map) }
            throwOnInvalid()
        }
        toolService.registerTool(toolDTO)
        return ApiResponse.success(toolDTO)
    }

    @POST
    @Path("/delete")
    @RolesAllowed("admin")
    fun delete(toolDTO: ToolDTO): ApiResponse<Boolean> {
        DTOValidator.validate(toolDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        return ApiResponse.success(toolService.deleteTool(toolDTO.id!!))
    }

    @POST
    @Path("/saveSetting")
    fun saveSetting(toolDTO: ToolDTO): ApiResponse<ToolDTO> {
        DTOValidator.validate(toolDTO) {
            requireNotEmpty("id", "alias")
            throwOnInvalid()
        }
        toolService.saveSetting(toolDTO)
        return ApiResponse.success(toolDTO)
    }

    @POST
    @Path("/updateSetting")
    fun updateSetting(toolDTO: ToolDTO): ApiResponse<ToolDTO> {
        DTOValidator.validate(toolDTO) {
            requireNotEmpty("settingId", "settingContent")
            throwOnInvalid()
        }
        toolService.updateSetting(toolDTO)
        return ApiResponse.success(toolDTO)
    }

    @POST
    @Path("/category/save")
    @RolesAllowed("admin")
    fun categorySave(toolDTO: ToolDTO): ApiResponse<ToolDTO> {
        DTOValidator.validate(toolDTO) {
            requireNotEmpty("group")
            throwOnInvalid()
        }
        toolService.saveCategory(toolDTO)
        return ApiResponse.success(toolDTO)
    }

    @POST
    @Path("/category/update")
    @RolesAllowed("admin")
    fun categoryUpdate(toolDTO: ToolDTO): ApiResponse<ToolDTO> {
        DTOValidator.validate(toolDTO) {
            requireNotEmpty("groupId", "group")
            throwOnInvalid()
        }
        toolService.updateCategory(toolDTO)
        return ApiResponse.success(toolDTO)
    }

    @POST
    @Path("/category/delete")
    @RolesAllowed("admin")
    fun categoryDelete(toolDTO: ToolDTO): ApiResponse<ToolDTO> {
        DTOValidator.validate(toolDTO) {
            requireNotEmpty("groupId")
            throwOnInvalid()
        }
        toolService.deleteCategory(toolDTO)
        return ApiResponse.success(toolDTO)
    }

}
