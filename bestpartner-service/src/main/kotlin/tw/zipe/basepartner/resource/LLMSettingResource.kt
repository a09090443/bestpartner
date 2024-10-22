package tw.zipe.basepartner.resource

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.basepartner.dto.ApiResponse
import tw.zipe.basepartner.dto.LLMDTO
import tw.zipe.basepartner.service.LLMService
import tw.zipe.basepartner.util.DTOValidator

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
    fun saveLlm(llmDTO: LLMDTO): ApiResponse<String> {
        DTOValidator.validate(llmDTO) {
            validateNested("llmModel") {
                requireNotEmpty("modelName")
            }
            throwOnInvalid()
        }

        llmDTO.llmModel?.let {
            lLMService.saveLLMSetting(llmDTO)
        }
        return ApiResponse.success("成功儲存 LLM 設定")
    }

    @POST
    @Path("/updateLLM")
    fun updateLlm(llmDTO: LLMDTO): ApiResponse<String> {
        DTOValidator.validate(llmDTO) {
            requireNotEmpty("id")
            validateNested("llmModel") {
                requireNotEmpty("id")
                requireNotEmpty("modelName")
            }
            throwOnInvalid()
        }

        llmDTO.llmModel?.let {
            lLMService.updateLLMSetting(llmDTO)
        }
        return ApiResponse.success("成功更新 LLM 設定")
    }
}
