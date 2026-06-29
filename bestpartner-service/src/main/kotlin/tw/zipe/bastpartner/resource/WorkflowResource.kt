package tw.zipe.bastpartner.resource

import io.quarkus.security.Authenticated
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.WorkflowDTO
import tw.zipe.bastpartner.dto.WorkflowSaveRequestDTO
import tw.zipe.bastpartner.dto.WorkflowSummaryDTO
import tw.zipe.bastpartner.dto.WorkflowSwitchStatusRequestDTO
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.service.WorkflowService
import tw.zipe.bastpartner.util.DTOValidator
import tw.zipe.bastpartner.util.MessageUtil

/**
 * Workflow 定義 CRUD REST 端點（Phase 1）。
 *
 * 路徑前綴 /llm/workflow，全部需登入（@Authenticated），擁有權與 admin 例外於 service 層檢核。
 *
 * @author Gary
 * @created 2026/6/29
 */
@Path("/llm/workflow")
@ApplicationScoped
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class WorkflowResource(
    private val workflowService: WorkflowService
) {

    @POST
    @Path("/create")
    fun create(dto: WorkflowDTO): ApiResponse<WorkflowDTO> {
        DTOValidator.validate(dto) {
            requireNotEmpty("name")
            throwOnInvalid()
        }
        return ApiResponse.success(workflowService.create(dto.name, dto.description))
    }

    @POST
    @Path("/save")
    fun save(req: WorkflowSaveRequestDTO): ApiResponse<WorkflowDTO> {
        DTOValidator.validate(req) {
            requireNotEmpty("name")
            throwOnInvalid()
        }
        return ApiResponse.success(workflowService.save(req))
    }

    @POST
    @Path("/get")
    fun get(dto: WorkflowDTO): ApiResponse<WorkflowDTO> {
        DTOValidator.validate(dto) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        return ApiResponse.success(workflowService.get(dto.id.orEmpty()))
    }

    @GET
    @Path("/list")
    fun list(): ApiResponse<List<WorkflowSummaryDTO>> = ApiResponse.success(workflowService.list())

    @POST
    @Path("/update")
    fun update(dto: WorkflowDTO): ApiResponse<WorkflowDTO> {
        DTOValidator.validate(dto) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        return ApiResponse.success(
            workflowService.updateMeta(
                dto.id.orEmpty(),
                dto.name.takeIf { it.isNotBlank() },
                dto.description,
                dto.canvasMeta
            )
        )
    }

    @POST
    @Path("/delete")
    fun delete(dto: WorkflowDTO): ApiResponse<String> {
        DTOValidator.validate(dto) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        workflowService.delete(dto.id.orEmpty())
        return ApiResponse.success(MessageUtil.get(AppMessage.SUCCESS_DATA_DELETED))
    }

    @POST
    @Path("/switchStatus")
    fun switchStatus(dto: WorkflowSwitchStatusRequestDTO): ApiResponse<WorkflowDTO> {
        DTOValidator.validate(dto) {
            requireNotEmpty("id")
            throwOnInvalid()
        }
        return ApiResponse.success(workflowService.switchStatus(dto.id.orEmpty(), dto.active))
    }
}
