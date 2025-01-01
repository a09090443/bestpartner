package tw.zipe.bastpartner.resource

import io.quarkus.security.Authenticated
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.LLMDTO
import tw.zipe.bastpartner.dto.PlatformDTO
import tw.zipe.bastpartner.service.LLMService
import tw.zipe.bastpartner.util.DTOValidator

/**
 * @author Gary
 * @created 2024/10/20
 */
@Path("/llm/setting")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class LLMSettingResource(
    private val llmService: LLMService
) {

    @POST
    @Path("/get")
    fun get(llmDTO: LLMDTO): ApiResponse<LLMDTO?> {
        DTOValidator.validate(llmDTO) {
            requireNotEmpty("llmId")
            throwOnInvalid()
        }
        val result = llmService.getLLMSetting(llmDTO.llmId.orEmpty())
        return ApiResponse.success(result)
    }

    @POST
    @Path("/save")
    fun save(llmDTO: LLMDTO): ApiResponse<LLMDTO> {
        DTOValidator.validate(llmDTO) {
            requireNotEmpty("modelType")
            requireNotEmpty("platformId")
            validateNested("llmModel") {
                requireNotEmpty("modelName")
            }
            throwOnInvalid()
        }

        llmDTO.llmModel.let {
            llmService.saveLLMSetting(llmDTO)
        }
        return ApiResponse.success(llmDTO)
    }

    @POST
    @Path("/update")
    fun update(llmDTO: LLMDTO): ApiResponse<String> {
        DTOValidator.validate(llmDTO) {
            requireNotEmpty("id")
            validateNested("llmModel") {
                requireNotEmpty("id")
                requireNotEmpty("modelName")
            }
            throwOnInvalid()
        }

        llmDTO.llmModel?.let {
            llmService.updateLLMSetting(llmDTO)
        }
        return ApiResponse.success("成功更新 LLM 設定")
    }

    @POST
    @Path("/delete")
    fun delete(llmDTO: LLMDTO): ApiResponse<Boolean> {
        DTOValidator.validate(llmDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }

        return ApiResponse.success(llmService.deleteLLMSetting(llmDTO.id.orEmpty()))
    }

    @POST
    @Path("/platform/add")
    @RolesAllowed("admin")
    fun addPlatform(platformDTO: PlatformDTO): ApiResponse<PlatformDTO> {
        DTOValidator.validate(platformDTO) {
            requireNotEmpty("platform")
            throwOnInvalid()
        }

        llmService.addPlatform(platformDTO)
        return ApiResponse.success(platformDTO)
    }

    @POST
    @Path("/platform/delete")
    fun deletePlatform(platformDTO: PlatformDTO): ApiResponse<Boolean> {
        DTOValidator.validate(platformDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }

        return ApiResponse.success(llmService.deletePlatform(platformDTO.id.orEmpty()))
    }
}
