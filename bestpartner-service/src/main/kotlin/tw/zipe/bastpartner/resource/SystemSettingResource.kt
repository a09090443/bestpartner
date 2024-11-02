package tw.zipe.bastpartner.resource

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.SystemSettingDTO
import tw.zipe.bastpartner.service.SystemService
import tw.zipe.bastpartner.util.DTOValidator

/**
 * @author Gary
 * @created 2024/11/1
 */
@Path("/systemSetting")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class SystemSettingResource(
    val systemService: SystemService
) {

    @GET
    @Path("/list")
    fun list(): ApiResponse<List<SystemSettingDTO>> {
        val result = systemService.findAll()
        return ApiResponse.success(result)
    }

    @POST
    @Path("/get")
    fun get(systemSettingDTO: SystemSettingDTO): ApiResponse<String> {
        val result = systemService.getSystemSettingValue(systemSettingDTO.key)
        return ApiResponse.success(result.orEmpty())
    }

    @POST
    @Path("/add")
    fun add(systemSettingDTO: SystemSettingDTO): ApiResponse<SystemSettingDTO> {
        DTOValidator.validate(systemSettingDTO) {
            requireNotEmpty("value")
            throwOnInvalid()
        }
        val result = systemService.addSystemSetting(systemSettingDTO)
        return ApiResponse.success(result)
    }

    @POST
    @Path("/update")
    fun update(systemSettingDTO: SystemSettingDTO): ApiResponse<SystemSettingDTO> {
        DTOValidator.validate(systemSettingDTO) {
            requireNotEmpty("value")
            throwOnInvalid()
        }
        val result = systemService.updateSystemSetting(systemSettingDTO)
        return ApiResponse.success(result)
    }

    @DELETE
    @Path("/delete")
    fun delete(systemSettingDTO: SystemSettingDTO): ApiResponse<Long> {
        DTOValidator.validate(systemSettingDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        val result = systemService.deleteSystemSetting(systemSettingDTO.key)
        return ApiResponse.success(result)
    }

}
