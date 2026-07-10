package tw.zipe.bastpartner.service.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.security.identity.CurrentIdentityAssociation
import io.quarkus.security.identity.SecurityIdentity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.control.ActivateRequestContext
import jakarta.enterprise.inject.Instance
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import tw.zipe.bastpartner.dto.workflow.config.NodeConfigRegistry
import tw.zipe.bastpartner.entity.WorkflowExecutionEntity
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.entity.WorkflowNodeExecutionEntity
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.ExecutionStatus
import tw.zipe.bastpartner.enumerate.NodeExecutionStatus
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.enumerate.TriggerType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.repository.WorkflowEdgeRepository
import tw.zipe.bastpartner.repository.WorkflowExecutionRepository
import tw.zipe.bastpartner.repository.WorkflowNodeExecutionRepository
import tw.zipe.bastpartner.repository.WorkflowNodeRepository
import tw.zipe.bastpartner.repository.WorkflowRepository
import tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor
import tw.zipe.bastpartner.util.MessageUtil
import tw.zipe.bastpartner.util.logger

/**
 * Workflow 執行引擎（Phase 1：線性節點）。
 * 事件經 [ExecutionEventSink] 即時發出，紀錄同步落庫；寫庫失敗不中斷執行。
 *
 * @author Gary
 * @created 2026/7/10
 */
@ApplicationScoped
class WorkflowEngine(
    private val workflowRepository: WorkflowRepository,
    private val workflowNodeRepository: WorkflowNodeRepository,
    private val workflowEdgeRepository: WorkflowEdgeRepository,
    private val executionRepository: WorkflowExecutionRepository,
    private val nodeExecutionRepository: WorkflowNodeExecutionRepository,
    private val executors: Instance<NodeExecutor>,
    private val currentIdentityAssociation: CurrentIdentityAssociation
) {
    private val logger = logger()
    private val objectMapper = ObjectMapper()
    private val json = Json { ignoreUnknownKeys = true }
    private val executorMap: Map<NodeType, NodeExecutor> by lazy { executors.associateBy { it.type } }

    /**
     * 執行整條 workflow。同步阻塞直到完成；事件經 sink 即時發出。
     * 呼叫端多在背景執行緒（如 CompletableFuture.runAsync）呼叫，故以 [ActivateRequestContext]
     * 自行啟用 CDI request context，避免 Panache 查詢拋 ContextNotActiveException。
     *
     * 新啟用的 request context 中 [SecurityIdentity] 預設為 anonymous，下游依賴登入身分的
     * 服務（如 LLM 節點）會因此誤判未登入。因此呼叫端須將 request scope 內取得的呼叫者
     * identity 一併傳入，本方法啟用 request context 後第一件事即以 [currentIdentityAssociation]
     * 還原該身分，確保背景執行緒內的服務能正確取得目前使用者。
     *
     * @param identity 呼叫端（request scope）取得的呼叫者身分，用於還原背景 request context 的登入狀態
     * @param cancelled 每節點執行前檢查；true 則中止並標 CANCELLED
     * @return 落庫後的 execution id
     */
    @ActivateRequestContext
    fun execute(
        workflowId: String,
        userId: String,
        input: Map<String, Any?>?,
        identity: SecurityIdentity,
        sink: ExecutionEventSink,
        cancelled: () -> Boolean
    ): String {
        currentIdentityAssociation.setIdentity(identity)
        val workflow = workflowRepository.findOptionalById(workflowId)
            ?: throw ServiceException(AppMessage.WORKFLOW_NOT_FOUND)
        val nodes = workflowNodeRepository.findByWorkflowId(workflowId)
        val edges = workflowEdgeRepository.findByWorkflowId(workflowId)

        validateNodes(nodes)

        val order = topologicalOrder(nodes, edges)
        val startedAt = LocalDateTime.now()

        val execution = WorkflowExecutionEntity().apply {
            this.workflowId = workflowId
            workflowVersion = workflow.version
            triggerType = TriggerType.MANUAL
            triggeredBy = userId
            status = ExecutionStatus.RUNNING
            inputPayload = input
            this.startedAt = startedAt
        }
        persist { executionRepository.saveOrUpdate(execution) }
        val executionId = execution.id ?: java.util.UUID.randomUUID().toString()

        sink.emit(ExecutionEvent("execution.started", executionId, ts = now()))

        val context = ExecutionContext(executionId, userId)
        context.putOutput(TriggerExecutor.INPUT_KEY, mapOf("input" to (input ?: emptyMap<String, Any?>())))

        var failedNode: WorkflowNodeEntity? = null
        var failureMessage: String? = null
        var wasCancelled = false
        val executedKeys = mutableSetOf<String>()

        for ((index, node) in order.withIndex()) {
            if (cancelled()) { wasCancelled = true; break }
            val seqNo = index + 1
            sink.emit(ExecutionEvent("node.started", executionId, node.nodeKey, seqNo, ts = now()))
            val nodeStart = System.currentTimeMillis()
            val record = WorkflowNodeExecutionEntity().apply {
                this.executionId = executionId
                this.workflowId = workflowId
                nodeKey = node.nodeKey
                nodeType = node.type
                this.seqNo = seqNo
                status = NodeExecutionStatus.RUNNING
                this.input = mapOf(
                    "config" to node.config,
                    "contextKeys" to context.allOutputs().keys.toList()
                )
                this.startedAt = LocalDateTime.now()
            }
            try {
                val executor = executorMap[node.type]
                    ?: throw ServiceException(AppMessage.WORKFLOW_NODE_TYPE_NOT_SUPPORTED, node.type.name)
                val configObj = mapToJsonObject(node.config) ?: JsonObject(emptyMap())
                val parsed = NodeConfigRegistry.parse(node.type, configObj)
                val output = executor.execute(node, parsed, context)
                context.putOutput(node.nodeKey, output)
                executedKeys.add(node.nodeKey)

                val duration = System.currentTimeMillis() - nodeStart
                record.apply {
                    status = NodeExecutionStatus.SUCCESS
                    this.output = output
                    finishedAt = LocalDateTime.now()
                    durationMs = duration
                }
                persist { nodeExecutionRepository.saveOrUpdate(record) }
                sink.emit(ExecutionEvent("node.completed", executionId, node.nodeKey, seqNo, "SUCCESS", output, durationMs = duration, ts = now()))
            } catch (e: Exception) {
                val message = if (e is VariableNotFoundException) {
                    MessageUtil.get(AppMessage.WORKFLOW_VARIABLE_NOT_FOUND, e.path)
                } else {
                    e.message ?: e.javaClass.simpleName
                }
                logger.error("節點 ${node.nodeKey} 執行失敗", e)
                record.apply {
                    status = NodeExecutionStatus.FAILED
                    errorMessage = message
                    finishedAt = LocalDateTime.now()
                    durationMs = System.currentTimeMillis() - nodeStart
                }
                persist { nodeExecutionRepository.saveOrUpdate(record) }
                sink.emit(ExecutionEvent("node.failed", executionId, node.nodeKey, seqNo, "FAILED", error = message, ts = now()))
                failedNode = node
                failureMessage = message
                break
            }
        }

        // 未執行節點標 SKIPPED（失敗或取消時）
        if (failedNode != null || wasCancelled) {
            order.filter { it.nodeKey !in executedKeys && it.nodeKey != failedNode?.nodeKey }.forEach { skipped ->
                persist {
                    nodeExecutionRepository.saveOrUpdate(WorkflowNodeExecutionEntity().apply {
                        this.executionId = executionId
                        this.workflowId = workflowId
                        nodeKey = skipped.nodeKey
                        nodeType = skipped.type
                        seqNo = order.indexOf(skipped) + 1
                        status = NodeExecutionStatus.SKIPPED
                    })
                }
            }
        }

        val finalStatus = when {
            wasCancelled -> ExecutionStatus.CANCELLED
            failedNode != null -> ExecutionStatus.FAILED
            else -> ExecutionStatus.SUCCESS
        }
        val finalOutput = if (finalStatus == ExecutionStatus.SUCCESS) collectFinalOutput(order, context) else null

        execution.apply {
            status = finalStatus
            outputResult = finalOutput
            errorNodeKey = failedNode?.nodeKey
            errorMessage = failureMessage
            finishedAt = LocalDateTime.now()
            durationMs = java.time.Duration.between(startedAt, LocalDateTime.now()).toMillis()
        }
        persist { executionRepository.update(execution) }
        sink.emit(
            ExecutionEvent(
                "execution.completed", executionId, status = finalStatus.name,
                output = finalOutput, error = failureMessage,
                durationMs = execution.durationMs, ts = now()
            )
        )
        return executionId
    }

    /**
     * 執行前驗證（載入 workflow 存在性、無 TRIGGER 節點檢查、逐節點 config 型別 + 必填），
     * 供 resource 於 request scope 內預檢——失敗即拋 [ServiceException]，由 GlobalExceptionMapper
     * 轉為 HTTP 400，避免先建立執行紀錄再失敗（spec §5）。
     */
    fun validateForExecution(workflowId: String) {
        workflowRepository.findOptionalById(workflowId)
            ?: throw ServiceException(AppMessage.WORKFLOW_NOT_FOUND)
        val nodes = workflowNodeRepository.findByWorkflowId(workflowId)
        validateNodes(nodes)
    }

    /** 無 TRIGGER 節點檢查 + 逐節點 parse/必填驗證（同啟用等級） */
    private fun validateNodes(nodes: List<WorkflowNodeEntity>) {
        if (nodes.none { it.type == NodeType.TRIGGER }) {
            throw ServiceException(AppMessage.WORKFLOW_TRIGGER_NODE_REQUIRED)
        }
        nodes.forEach { n ->
            val configObj = mapToJsonObject(n.config) ?: JsonObject(emptyMap())
            val parsed = runCatching { NodeConfigRegistry.parse(n.type, configObj) }
                .getOrElse { e -> throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_INVALID, n.nodeKey, e.message.orEmpty()) }
            val missing = parsed.missingRequiredFields()
            if (missing.isNotEmpty()) {
                throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_REQUIRED_MISSING, n.nodeKey, missing.joinToString(", "))
            }
        }
    }

    /** OUTPUT 節點輸出合併；恰一個直接用；零個取拓撲序最後節點輸出 */
    private fun collectFinalOutput(order: List<WorkflowNodeEntity>, context: ExecutionContext): Map<String, Any?> {
        val outputNodes = order.filter { it.type == NodeType.OUTPUT }
        return when {
            outputNodes.size == 1 -> context.getOutput(outputNodes[0].nodeKey) ?: emptyMap()
            outputNodes.size > 1 -> outputNodes.associate { it.nodeKey to context.getOutput(it.nodeKey) }
            else -> order.lastOrNull()?.let { context.getOutput(it.nodeKey) } ?: emptyMap()
        }
    }

    /** Kahn 拓撲排序（同層依 nodeKey 排序，結果 deterministic）；有環拋既有例外 */
    private fun topologicalOrder(
        nodes: List<WorkflowNodeEntity>,
        edges: List<tw.zipe.bastpartner.entity.WorkflowEdgeEntity>
    ): List<WorkflowNodeEntity> {
        val nodeMap = nodes.associateBy { it.nodeKey }
        val indegree = nodes.associateTo(HashMap()) { it.nodeKey to 0 }
        val adj = HashMap<String, MutableList<String>>()
        edges.forEach { e ->
            adj.getOrPut(e.sourceNodeKey) { mutableListOf() }.add(e.targetNodeKey)
            indegree[e.targetNodeKey] = (indegree[e.targetNodeKey] ?: 0) + 1
        }
        val queue = sortedSetOf<String>()
        indegree.filterValues { it == 0 }.keys.forEach { queue.add(it) }
        val order = mutableListOf<WorkflowNodeEntity>()
        while (queue.isNotEmpty()) {
            val key = queue.first().also { queue.remove(it) }
            nodeMap[key]?.let { order.add(it) }
            adj[key]?.forEach { next ->
                val deg = (indegree[next] ?: 0) - 1
                indegree[next] = deg
                if (deg == 0) queue.add(next)
            }
        }
        if (order.size != nodes.size) throw ServiceException(AppMessage.WORKFLOW_GRAPH_HAS_CYCLE)
        return order
    }

    /** 落庫失敗不中斷執行（spec §2.6） */
    private fun persist(block: () -> Unit) {
        runCatching(block).onFailure { logger.error("執行紀錄落庫失敗", it) }
    }

    private fun now(): String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    private fun mapToJsonObject(map: Map<String, Any?>?): JsonObject? {
        if (map == null) return null
        return json.parseToJsonElement(objectMapper.writeValueAsString(map)).jsonObject
    }
}
