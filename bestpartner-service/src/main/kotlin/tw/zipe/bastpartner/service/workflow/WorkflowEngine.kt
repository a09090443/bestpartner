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
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import org.eclipse.microprofile.context.ManagedExecutor
import tw.zipe.bastpartner.dto.workflow.config.CodeNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.HttpRequestNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.LoopNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
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
import tw.zipe.bastpartner.service.workflow.executor.LoopExecutor
import tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor
import tw.zipe.bastpartner.util.MessageUtil
import tw.zipe.bastpartner.util.logger

/**
 * Workflow 執行引擎（Phase 2：邊活化遍歷，支援 CONDITION 分支）。
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
    private val currentIdentityAssociation: CurrentIdentityAssociation,
    private val managedExecutor: ManagedExecutor
) {
    companion object {
        /** 節點逾時預設值（spec §2.2）：config 未指定 timeoutMs 時套用 */
        const val DEFAULT_NODE_TIMEOUT_MS = 120_000L
    }

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

        validateNodes(nodes, edges)

        val order = topologicalOrder(nodes, edges)
        // LOOP 子圖：loopNodeKey → 子圖節點鍵集合；子圖節點不在主遍歷執行，由 LOOP 迭代驅動
        val loopSubgraphs = computeLoopSubgraphs(nodes, edges)
        val loopBodyKeys = loopSubgraphs.values.flatten().toSet()
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
        val skippedKeys = mutableSetOf<String>()

        // 活化遍歷（Phase 2）：仍依拓撲序處理，但每節點先判斷是否 active——
        // indegree 0 恆 active；其餘至少一條入邊被活化才 active。
        // 邊被活化 = 來源節點執行成功，且（來源非 CONDITION，或 sourceHandle 等於判定分支）。
        val incomingEdges = edges.groupBy { it.targetNodeKey }
        val outgoingEdges = edges.groupBy { it.sourceNodeKey }
        val activatedEdges = mutableSetOf<tw.zipe.bastpartner.entity.WorkflowEdgeEntity>()
        // seqNo 改為遞增計數器（Phase 2）：LOOP 每迭代的子圖節點紀錄各占一個序號
        val seq = AtomicInteger(0)

        for (node in order) {
            if (cancelled()) { wasCancelled = true; break }
            // LOOP 子圖節點由 LOOP 迭代驅動，主遍歷不執行也不落紀錄
            if (node.nodeKey in loopBodyKeys) continue
            val seqNo = seq.incrementAndGet()
            val incoming = incomingEdges[node.nodeKey].orEmpty()
            if (incoming.isNotEmpty() && incoming.none { it in activatedEdges }) {
                // 非 active：落 SKIPPED 紀錄（含 seqNo），不執行、不活化出邊、不發事件
                skippedKeys.add(node.nodeKey)
                persist {
                    nodeExecutionRepository.saveOrUpdate(WorkflowNodeExecutionEntity().apply {
                        this.executionId = executionId
                        this.workflowId = workflowId
                        nodeKey = node.nodeKey
                        nodeType = node.type
                        this.seqNo = seqNo
                        status = NodeExecutionStatus.SKIPPED
                    })
                }
                continue
            }
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
                val configObj = mapToJsonObject(node.config) ?: JsonObject(emptyMap())
                val parsed = NodeConfigRegistry.parse(node.type, configObj)
                val output = if (node.type == NodeType.LOOP) {
                    // LOOP 由引擎自行編排子圖迭代（需執行其他節點，不經 executor 分派）
                    val bodyOrder = order.filter { it.nodeKey in loopSubgraphs[node.nodeKey].orEmpty() }
                    executeLoopNode(node, parsed as LoopNodeConfig, bodyOrder, context, executionId, workflowId, sink, seq)
                } else {
                    val executor = executorMap[node.type]
                        ?: throw ServiceException(AppMessage.WORKFLOW_NODE_TYPE_NOT_SUPPORTED, node.type.name)
                    executeWithTimeout(executor, node, parsed, context)
                }
                context.putOutput(node.nodeKey, output)
                executedKeys.add(node.nodeKey)

                // 活化出邊：CONDITION 僅活化判定分支、LOOP 完成後僅活化 out:done，其餘節點全數活化
                val branch = when (node.type) {
                    NodeType.CONDITION ->
                        output[tw.zipe.bastpartner.service.workflow.executor.ConditionExecutor.OUTPUT_BRANCH] as? String
                    NodeType.LOOP -> LoopExecutor.DONE_HANDLE
                    else -> null
                }
                outgoingEdges[node.nodeKey].orEmpty().forEach { e ->
                    if (branch == null || e.sourceHandle == branch) activatedEdges.add(e)
                }

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
                val message = failureMessage(e)
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

        // 未執行節點標 SKIPPED（失敗或取消時）；排除主迴圈已落過 SKIPPED 紀錄者與
        // LOOP 子圖節點（由 LOOP 迭代驅動，已執行過的迭代各有紀錄），避免重複記錄
        if (failedNode != null || wasCancelled) {
            order.filter {
                it.nodeKey !in executedKeys && it.nodeKey !in skippedKeys &&
                    it.nodeKey !in loopBodyKeys && it.nodeKey != failedNode?.nodeKey
            }.forEach { skipped ->
                persist {
                    nodeExecutionRepository.saveOrUpdate(WorkflowNodeExecutionEntity().apply {
                        this.executionId = executionId
                        this.workflowId = workflowId
                        nodeKey = skipped.nodeKey
                        nodeType = skipped.type
                        seqNo = seq.incrementAndGet()
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
        val finalOutput = if (finalStatus == ExecutionStatus.SUCCESS) collectFinalOutput(order, context, executedKeys) else null

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
        val edges = workflowEdgeRepository.findByWorkflowId(workflowId)
        validateNodes(nodes, edges)
    }

    /** 無 TRIGGER 節點檢查 + 逐節點 parse/必填驗證（同啟用等級）+ 巢狀 LOOP 檢查 */
    private fun validateNodes(
        nodes: List<WorkflowNodeEntity>,
        edges: List<tw.zipe.bastpartner.entity.WorkflowEdgeEntity>
    ) {
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
        // 巢狀 LOOP 不支援：任一 LOOP 子圖內含另一 LOOP 節點即報錯
        val typeByKey = nodes.associate { it.nodeKey to it.type }
        computeLoopSubgraphs(nodes, edges).forEach { (_, body) ->
            body.firstOrNull { typeByKey[it] == NodeType.LOOP }?.let { nested ->
                throw ServiceException(AppMessage.WORKFLOW_LOOP_NESTED_NOT_SUPPORTED, nested)
            }
        }
    }

    /**
     * 計算各 LOOP 節點的迴圈子圖：自 loopBodyEntryNodeKey 起沿出邊可達、
     * 不含 LOOP 節點本身（亦不越過它續遍歷）的節點鍵集合。
     */
    private fun computeLoopSubgraphs(
        nodes: List<WorkflowNodeEntity>,
        edges: List<tw.zipe.bastpartner.entity.WorkflowEdgeEntity>
    ): Map<String, Set<String>> {
        val outgoing = edges.groupBy { it.sourceNodeKey }
        val nodeKeys = nodes.map { it.nodeKey }.toSet()
        return nodes.filter { it.type == NodeType.LOOP }.associate { loopNode ->
            val entry = loopNode.config?.get("loopBodyEntryNodeKey") as? String
            val body = mutableSetOf<String>()
            if (entry != null && entry in nodeKeys) {
                val queue = ArrayDeque<String>().apply { add(entry) }
                while (queue.isNotEmpty()) {
                    val key = queue.removeFirst()
                    if (key == loopNode.nodeKey || !body.add(key)) continue
                    outgoing[key].orEmpty().forEach { queue.add(it.targetNodeKey) }
                }
            }
            loopNode.nodeKey to body
        }
    }

    /**
     * LOOP 節點的迭代編排（spec §2.2）：解析輸入陣列後逐項迭代，每迭代以 itemAlias 注入
     * 當前項並依子圖拓撲序執行各節點（紀錄帶 loopIndex、SSE 事件照發）；
     * 各迭代取子圖拓撲序最後節點的輸出彙集為 List，放入 collectOutputKey 回傳。
     * 迭代中任一節點失敗 → 例外上拋，由主迴圈轉為 LOOP 節點 FAILED（整體 FAILED）。
     */
    private fun executeLoopNode(
        loopNode: WorkflowNodeEntity,
        cfg: LoopNodeConfig,
        bodyOrder: List<WorkflowNodeEntity>,
        context: ExecutionContext,
        executionId: String,
        workflowId: String,
        sink: ExecutionEventSink,
        seq: AtomicInteger
    ): Map<String, Any?> {
        val loopExecutor = executorMap[NodeType.LOOP] as? LoopExecutor
            ?: throw ServiceException(AppMessage.WORKFLOW_NODE_TYPE_NOT_SUPPORTED, NodeType.LOOP.name)
        val items = loopExecutor.resolveItems(cfg, context)
        val limit = cfg.maxIterations ?: LoopExecutor.DEFAULT_MAX_ITERATIONS
        val effective = if (items.size > limit) {
            logger.warn("LOOP ${loopNode.nodeKey} 輸入 ${items.size} 項超過迭代上限 $limit，僅執行前 $limit 項")
            items.take(limit)
        } else {
            items
        }
        val alias = cfg.itemAlias?.takeIf { it.isNotBlank() } ?: LoopExecutor.DEFAULT_ITEM_ALIAS
        val collected = mutableListOf<Any?>()
        try {
            effective.forEachIndexed { index, item ->
                context.putValue(alias, item)
                var lastOutput: Map<String, Any?> = emptyMap()
                bodyOrder.forEach { bodyNode ->
                    lastOutput = executeLoopBodyNode(bodyNode, index, context, executionId, workflowId, sink, seq)
                }
                collected.add(lastOutput)
            }
        } finally {
            context.removeValue(alias)
        }
        val collectKey = cfg.collectOutputKey?.takeIf { it.isNotBlank() } ?: LoopExecutor.DEFAULT_COLLECT_KEY
        return mapOf(collectKey to collected)
    }

    /** 執行單一迴圈子圖節點：紀錄帶 loopIndex、事件照發；失敗落 FAILED 紀錄後包 nodeKey 上拋 */
    private fun executeLoopBodyNode(
        node: WorkflowNodeEntity,
        loopIndex: Int,
        context: ExecutionContext,
        executionId: String,
        workflowId: String,
        sink: ExecutionEventSink,
        seq: AtomicInteger
    ): Map<String, Any?> {
        val seqNo = seq.incrementAndGet()
        sink.emit(ExecutionEvent("node.started", executionId, node.nodeKey, seqNo, ts = now()))
        val nodeStart = System.currentTimeMillis()
        val record = WorkflowNodeExecutionEntity().apply {
            this.executionId = executionId
            this.workflowId = workflowId
            nodeKey = node.nodeKey
            nodeType = node.type
            this.seqNo = seqNo
            this.loopIndex = loopIndex
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
            val output = executeWithTimeout(executor, node, parsed, context)
            context.putOutput(node.nodeKey, output)
            val duration = System.currentTimeMillis() - nodeStart
            record.apply {
                status = NodeExecutionStatus.SUCCESS
                this.output = output
                finishedAt = LocalDateTime.now()
                durationMs = duration
            }
            persist { nodeExecutionRepository.saveOrUpdate(record) }
            sink.emit(ExecutionEvent("node.completed", executionId, node.nodeKey, seqNo, "SUCCESS", output, durationMs = duration, ts = now()))
            return output
        } catch (e: Exception) {
            val message = failureMessage(e)
            logger.error("迴圈子圖節點 ${node.nodeKey}（迭代 $loopIndex）執行失敗", e)
            record.apply {
                status = NodeExecutionStatus.FAILED
                errorMessage = message
                finishedAt = LocalDateTime.now()
                durationMs = System.currentTimeMillis() - nodeStart
            }
            persist { nodeExecutionRepository.saveOrUpdate(record) }
            sink.emit(ExecutionEvent("node.failed", executionId, node.nodeKey, seqNo, "FAILED", error = message, ts = now()))
            throw ServiceException(AppMessage.WORKFLOW_NODE_EXEC_FAILED, node.nodeKey, message)
        }
    }

    /**
     * 節點逾時通用機制（spec §2.2 + 追記）：以 [managedExecutor] submit 後 `future.get` 套用逾時，
     * config 有 timeoutMs（HTTP / CODE 型別）用之，否則預設 [DEFAULT_NODE_TIMEOUT_MS]。
     * executor 內部依賴 CDI request context 與登入身分，故必須用 Quarkus ManagedExecutor
     * （自動傳播 CDI + security context）而非一般執行緒池。
     * 逾時 → `future.cancel(true)` 中斷工作執行緒，拋 [ServiceException]（訊息含逾時毫秒數），
     * 由主迴圈轉為節點 FAILED。CODE 節點自身已有內層腳本逾時，外層仍照套（取 config timeoutMs）。
     */
    private fun executeWithTimeout(
        executor: NodeExecutor,
        node: WorkflowNodeEntity,
        parsed: NodeConfig,
        context: ExecutionContext
    ): Map<String, Any?> {
        val timeoutMs = when (parsed) {
            is HttpRequestNodeConfig -> parsed.timeoutMs
            is CodeNodeConfig -> parsed.timeoutMs
            else -> null
        } ?: DEFAULT_NODE_TIMEOUT_MS
        val future = managedExecutor.submit(Callable { executor.execute(node, parsed, context) })
        return try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw ServiceException(AppMessage.WORKFLOW_NODE_TIMEOUT, node.nodeKey, timeoutMs.toString())
        } catch (e: ExecutionException) {
            // 解包實際例外，維持 failureMessage 對例外型別的判斷（如 VariableNotFoundException）
            throw (e.cause as? Exception ?: e)
        }
    }

    /** 節點失敗訊息：插值變數不存在轉 i18n 訊息，其餘取例外訊息 */
    private fun failureMessage(e: Exception): String = if (e is VariableNotFoundException) {
        MessageUtil.get(AppMessage.WORKFLOW_VARIABLE_NOT_FOUND, e.path)
    } else {
        e.message ?: e.javaClass.simpleName
    }

    /**
     * OUTPUT 節點輸出合併；恰一個直接用；零個取「實際執行成功的最後一個節點」輸出。
     * 分支情境下 OUTPUT 節點可能被 SKIPPED，故僅計入實際執行成功者。
     */
    private fun collectFinalOutput(
        order: List<WorkflowNodeEntity>,
        context: ExecutionContext,
        executedKeys: Set<String>
    ): Map<String, Any?> {
        val outputNodes = order.filter { it.type == NodeType.OUTPUT && it.nodeKey in executedKeys }
        return when {
            outputNodes.size == 1 -> context.getOutput(outputNodes[0].nodeKey) ?: emptyMap()
            outputNodes.size > 1 -> outputNodes.associate { it.nodeKey to context.getOutput(it.nodeKey) }
            else -> order.lastOrNull { it.nodeKey in executedKeys }?.let { context.getOutput(it.nodeKey) } ?: emptyMap()
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
