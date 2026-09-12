package tw.zipe.bastpartner.resource

import io.quarkus.security.Authenticated
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.converter.SensitiveValueCodec
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.LLMDTO
import tw.zipe.bastpartner.dto.PlatformDTO
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.service.LLMService
import tw.zipe.bastpartner.util.DTOValidator
import tw.zipe.bastpartner.util.MessageUtil

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
):BaseLLMResource() {

    @POST
    @Path("/get")
    fun get(llmDTO: LLMDTO): ApiResponse<List<LLMDTO?>> {
        val result = llmService.getLLMSetting(
            identity.principal.name,
            llmDTO.platformId,
            null,
            llmDTO.llmId
        )
        return ApiResponse.success(result.map { it?.let(::maskApiKey) })
    }

    /**
     * 對外回應一律遮罩 apiKey，明文不跨越 HTTP 邊界。
     *
     * 遮罩僅施加於此處（resource 層）而非 [LLMService.getLLMSetting]：該方法另有
     * 非 REST 的呼叫端（`LLMStore`），service 層維持回傳明文，避免內部消費者靜默拿到遮罩值。
     * 未設定金鑰者維持空值，不遮罩，以免誤以為其中存有金鑰。
     *
     * 存回時客戶端可原樣傳回遮罩值，[LLMService.updateLLMSetting] 會沿用既有金鑰。
     */
    private fun maskApiKey(dto: LLMDTO): LLMDTO = dto.also { d ->
        if (!d.llmModel.apiKey.isNullOrEmpty()) {
            d.llmModel.apiKey = SensitiveValueCodec.SECRET_MASK
        }
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
        // saveLLMSetting 會還原明文 apiKey 供 caller 使用，回應前一併遮罩
        return ApiResponse.success(maskApiKey(llmDTO))
    }

    @POST
    @Path("/update")
    fun update(llmDTO: LLMDTO): ApiResponse<String> {
        DTOValidator.validate(llmDTO) {
            requireNotEmpty("id")
            requireNotEmpty("platformId")
            requireNotEmpty("modelType")
            requireNotEmpty("llmModel")
            throwOnInvalid()
        }

        llmDTO.llmModel.let {
            llmService.updateLLMSetting(llmDTO)
        }
        return ApiResponse.success(MessageUtil.get(AppMessage.SUCCESS_LLM_SETTING_UPDATED))
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
    @RolesAllowed("admin")
    fun deletePlatform(platformDTO: PlatformDTO): ApiResponse<Boolean> {
        DTOValidator.validate(platformDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }

        return ApiResponse.success(llmService.deletePlatform(platformDTO.id.orEmpty()))
    }
}
