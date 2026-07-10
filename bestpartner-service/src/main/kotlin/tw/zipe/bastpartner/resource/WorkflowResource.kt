package tw.zipe.bastpartner.resource

import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.common.annotation.Blocking
import io.smallrye.mutiny.Multi
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import java.util.concurrent.atomic.AtomicBoolean
import org.jboss.resteasy.reactive.RestStreamElementType
import tw.zipe.bastpartner.config.security.SecurityValidator
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.WorkflowDTO
import tw.zipe.bastpartner.dto.WorkflowSaveRequestDTO
import tw.zipe.bastpartner.dto.WorkflowSummaryDTO
import tw.zipe.bastpartner.dto.WorkflowSwitchStatusRequestDTO
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.WorkflowService
import tw.zipe.bastpartner.service.workflow.ExecutionEvent
import tw.zipe.bastpartner.service.workflow.WorkflowEngine
import tw.zipe.bastpartner.util.DTOValidator
import tw.zipe.bastpartner.util.MessageUtil
import tw.zipe.bastpartner.util.logger

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
    private val workflowService: WorkflowService,
    private val workflowEngine: WorkflowEngine,
    private val securityValidator: SecurityValidator,
    private val securityIdentity: SecurityIdentity
) {

    private val logger = logger()

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

    /**
     * 取得各 NodeType 的必填欄位清單，供前端載入時即時驗證節點設定。
     *
     * 清單源自 NodeConfig 契約（missingRequiredFields），非硬編；前端據此在存檔前即時提示缺漏欄位。
     * 純查詢設定契約、不涉個資，class 層級 @Authenticated 已足夠，不另加權限標註。
     */
    @GET
    @Path("/nodeRequiredFields")
    fun nodeRequiredFields(): ApiResponse<Map<String, List<String>>> =
        ApiResponse.success(workflowService.getNodeRequiredFields())

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

    /**
     * 手動執行 workflow，SSE 逐節點吐出執行事件（execution.started / node.started /
     * node.completed / node.failed / execution.completed）。
     *
     * 擁有權檢核與 userId 解析須在 request scope 內先完成（[workflowService.get] 內含
     * checkAccess；[SecurityIdentity] 為 request scope，無法帶入 async 執行緒），
     * 解析完成後才透過 Mutiny 的 [io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool]
     * 交由 [WorkflowEngine] 背景執行；相較 [CompletableFuture.runAsync]（預設落在
     * ForkJoinPool.commonPool，其執行緒 TCCL 為 system classloader 而非 Quarkus runtime
     * classloader，背景執行緒首次觸發 langchain4j/OkHttp 類別初始化時可能拋出
     * LinkageError/NoClassDefFoundError 等 [Throwable] 而非 [Exception]，導致 future 靜默失敗），
     * Quarkus 管理的 worker pool 執行緒具備正確的 TCCL。
     *
     * [WorkflowEngine.execute] 以 `@ActivateRequestContext` 另啟一個 request context，
     * 其中的 [SecurityIdentity] 預設為 anonymous，會導致下游依賴登入身分的邏輯（如
     * LLM 節點的 [tw.zipe.bastpartner.service.LLMService.buildAIService]）誤判未登入。
     * 因此在此 request scope 內先取出呼叫端 identity，隨呼叫傳入引擎，由引擎於其
     * 背景 request context 啟用後還原該身分。
     */
    @POST
    @Path("/execute")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @Blocking
    fun execute(dto: WorkflowDTO): Multi<String> {
        val id = dto.id ?: throw ServiceException(AppMessage.WORKFLOW_NOT_FOUND)
        // request scope 內先完成擁有權檢核與 userId 解析，再進背景執行
        workflowService.get(id)
        val userId = securityValidator.validateLoggedInUser()
        val input: Map<String, Any?>? = dto.inputPayload?.let { workflowService.jsonObjectToMap(it) }
        val callerIdentity = securityIdentity

        return Multi.createFrom().emitter<String> { emitter ->
            val cancelled = AtomicBoolean(false)
            emitter.onTermination { cancelled.set(true) }
            try {
                workflowEngine.execute(id, userId, input, callerIdentity, { event -> emitter.emit(event.toJson()) }) { cancelled.get() }
                emitter.complete()
            } catch (e: Throwable) {
                logger.error("workflow 執行失敗: $id", e)
                runCatching {
                    emitter.emit(
                        ExecutionEvent(
                            event = "execution.completed",
                            executionId = "",
                            status = "FAILED",
                            error = e.message ?: e.javaClass.simpleName,
                            ts = java.time.LocalDateTime.now().toString()
                        ).toJson()
                    )
                }
                emitter.complete()
            }
        }.runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool())
    }
}
