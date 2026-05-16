package tw.zipe.bastpartner.resource

import io.quarkus.security.Authenticated
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.SkillDTO
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.form.SkillUploadForm
import tw.zipe.bastpartner.service.SkillService
import tw.zipe.bastpartner.util.DTOValidator
import tw.zipe.bastpartner.util.MessageUtil

/**
 * @author Gary
 * @created 2025/04/18
 */
@Path("/llm/skill")
@ApplicationScoped
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMSkillResource(
    private val skillService: SkillService
) : BaseLLMResource() {

    @GET
    @Path("/list")
    fun list(): ApiResponse<List<SkillDTO>> = ApiResponse.success(skillService.getSkills())

    @POST
    @Path("/get")
    fun get(skillDTO: SkillDTO): ApiResponse<SkillDTO> {
        DTOValidator.validate(skillDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        return ApiResponse.success(skillService.getSkill(skillDTO.id.orEmpty()))
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun upload(@BeanParam form: SkillUploadForm): ApiResponse<SkillDTO> {
        if (form.file == null) throw ServiceException(AppMessage.SKILL_ZIP_NOT_PROVIDED)
        return ApiResponse.success(skillService.uploadSkill(form))
    }

    @POST
    @Path("/delete")
    fun delete(skillDTO: SkillDTO): ApiResponse<String> {
        DTOValidator.validate(skillDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        skillService.deleteSkill(skillDTO.id.orEmpty())
        return ApiResponse.success(MessageUtil.get(AppMessage.SUCCESS_DATA_DELETED))
    }

    // ── Global Skill 管理（僅限 admin）──

    @POST
    @Path("/global/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("admin")
    fun uploadGlobal(@BeanParam form: SkillUploadForm): ApiResponse<SkillDTO> {
        if (form.file == null) throw ServiceException(AppMessage.SKILL_ZIP_NOT_PROVIDED)
        return ApiResponse.success(skillService.uploadGlobalSkill(form))
    }

    @POST
    @Path("/global/delete")
    @RolesAllowed("admin")
    fun deleteGlobal(skillDTO: SkillDTO): ApiResponse<String> {
        DTOValidator.validate(skillDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        skillService.deleteGlobalSkill(skillDTO.id.orEmpty())
        return ApiResponse.success(MessageUtil.get(AppMessage.SUCCESS_DATA_DELETED))
    }
}
