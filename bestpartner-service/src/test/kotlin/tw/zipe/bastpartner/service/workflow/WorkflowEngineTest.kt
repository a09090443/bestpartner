package tw.zipe.bastpartner.service.workflow

import io.quarkus.security.runtime.QuarkusPrincipal
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.security.runtime.SecurityIdentityAssociation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowEdgeEntity
import tw.zipe.bastpartner.entity.WorkflowEntity
import tw.zipe.bastpartner.entity.WorkflowExecutionEntity
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.entity.WorkflowNodeExecutionEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.repository.WorkflowEdgeRepository
import tw.zipe.bastpartner.repository.WorkflowExecutionRepository
import tw.zipe.bastpartner.repository.WorkflowNodeExecutionRepository
import tw.zipe.bastpartner.repository.WorkflowNodeRepository
import tw.zipe.bastpartner.repository.WorkflowRepository

/**
 * WorkflowEngine 純編排單元測試。
 *
 * 測試以「純編排」為對象：以可控的 fake [NodeExecutor] 與攔截的 [ExecutionEventSink] 驗證事件序列。
 * 專案 src/test 現有測試無 mockito/mockk 依賴，repository 以手寫 fake 子類別替代
 * （WorkflowRepository 等皆標註 @ApplicationScoped，經 kotlin allopen 編譯器插件自動 open，
 * 可被子類別覆寫關鍵方法）。
 *
 * @author Gary
 * @created 2026/7/10
 */
class WorkflowEngineTest {

    companion object {
        private const val WORKFLOW_ID = "wf-1"
        private const val USER_ID = "user-1"
    }

    // ---------- fake NodeExecutor ----------

    private class FakeNodeExecutor(
        override val type: NodeType,
        private val fn: (ExecutionContext) -> Map<String, Any?>
    ) : NodeExecutor {
        override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> =
            fn(context)
    }

    // CDI Instance<NodeExecutor> 假實作見 FakeNodeExecutorInstance.java（同目錄，src/test/java）：
    // Kotlin 編譯器對 Instance 的三個多載 vararg select 方法有已知覆寫辨識限制，改以 Java 撰寫。

    // ---------- fake repositories ----------

    private class FakeWorkflowRepository(private val workflow: WorkflowEntity) : WorkflowRepository() {
        override fun findOptionalById(id: String): WorkflowEntity? = if (id == workflow.id) workflow else null
    }

    private class FakeWorkflowNodeRepository(private val nodes: List<WorkflowNodeEntity>) : WorkflowNodeRepository() {
        override fun findByWorkflowId(workflowId: String): List<WorkflowNodeEntity> = nodes
    }

    private class FakeWorkflowEdgeRepository(private val edges: List<WorkflowEdgeEntity>) : WorkflowEdgeRepository() {
        override fun findByWorkflowId(workflowId: String): List<WorkflowEdgeEntity> = edges
    }

    private class FakeWorkflowExecutionRepository : WorkflowExecutionRepository() {
        var lastSaved: WorkflowExecutionEntity? = null
        override fun saveOrUpdate(entity: WorkflowExecutionEntity): WorkflowExecutionEntity {
            if (entity.id == null) entity.id = "exec-fixed"
            lastSaved = entity
            return entity
        }
        override fun update(entity: WorkflowExecutionEntity): WorkflowExecutionEntity {
            lastSaved = entity
            return entity
        }
    }

    private class FakeWorkflowNodeExecutionRepository : WorkflowNodeExecutionRepository() {
        val saved = mutableListOf<WorkflowNodeExecutionEntity>()
        override fun saveOrUpdate(entity: WorkflowNodeExecutionEntity): WorkflowNodeExecutionEntity {
            saved.add(entity)
            return entity
        }
    }

    // ---------- fake event sink ----------

    private class CollectingSink : ExecutionEventSink {
        val events = mutableListOf<ExecutionEvent>()
        override fun emit(event: ExecutionEvent) { events.add(event) }
    }

    // ---------- fixtures ----------

    private fun node(key: String, type: NodeType, config: Map<String, Any?>) = WorkflowNodeEntity().apply {
        nodeKey = key
        this.type = type
        this.config = config
    }

    private fun edge(source: String, target: String) = WorkflowEdgeEntity().apply {
        sourceNodeKey = source
        targetNodeKey = target
    }

    private fun testIdentity() = QuarkusSecurityIdentity.builder()
        .setPrincipal(QuarkusPrincipal(USER_ID))
        .setAnonymous(false)
        .build()

    private fun triggerNode() = node("trigger", NodeType.TRIGGER, mapOf("triggerType" to "MANUAL"))
    private fun toolNode(key: String = "A") = node(key, NodeType.TOOL, mapOf("toolId" to "t1"))
    private fun outputNode(key: String = "out") = node(key, NodeType.OUTPUT, mapOf("template" to "x"))

    private fun buildEngine(
        nodes: List<WorkflowNodeEntity>,
        edges: List<WorkflowEdgeEntity>,
        executors: List<NodeExecutor>
    ): Triple<WorkflowEngine, FakeWorkflowExecutionRepository, FakeWorkflowNodeExecutionRepository> {
        val workflow = WorkflowEntity().apply { id = WORKFLOW_ID; userId = USER_ID; name = "測試 workflow"; version = 1 }
        val executionRepo = FakeWorkflowExecutionRepository()
        val nodeExecutionRepo = FakeWorkflowNodeExecutionRepository()
        val engine = WorkflowEngine(
            FakeWorkflowRepository(workflow),
            FakeWorkflowNodeRepository(nodes),
            FakeWorkflowEdgeRepository(edges),
            executionRepo,
            nodeExecutionRepo,
            FakeNodeExecutorInstance(executors),
            SecurityIdentityAssociation()
        )
        return Triple(engine, executionRepo, nodeExecutionRepo)
    }

    @Test
    fun `線性圖依拓撲序執行且事件序列正確`() {
        val nodes = listOf(triggerNode(), toolNode(), outputNode())
        val edges = listOf(edge("trigger", "A"), edge("A", "out"))
        val executors = listOf(
            FakeNodeExecutor(NodeType.TRIGGER) { ctx -> ctx.getOutput(tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor.INPUT_KEY) ?: emptyMap() },
            FakeNodeExecutor(NodeType.TOOL) { mapOf("result" to "A-done") },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("final" to "done") }
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        val executionId = engine.execute(WORKFLOW_ID, USER_ID, mapOf("q" to "hi"), testIdentity(), sink) { false }

        assertTrue(executionId.isNotBlank())
        val sequence = sink.events.map { it.event to it.nodeKey }
        assertEquals(
            listOf(
                "execution.started" to null,
                "node.started" to "trigger",
                "node.completed" to "trigger",
                "node.started" to "A",
                "node.completed" to "A",
                "node.started" to "out",
                "node.completed" to "out",
                "execution.completed" to null
            ),
            sequence
        )
        assertEquals("SUCCESS", sink.events.last().status)

        // 驗證第二個節點的執行紀錄 input 快照包含 contextKeys
        val secondNodeRecord = nodeExecutionRepo.saved.first { it.nodeKey == "A" }
        @Suppress("UNCHECKED_CAST")
        val inputSnapshot = secondNodeRecord.input as Map<String, Any?>
        assertTrue(inputSnapshot.containsKey("config"), "input 快照應含 config")
        assertTrue(inputSnapshot.containsKey("contextKeys"), "input 快照應含 contextKeys")
        @Suppress("UNCHECKED_CAST")
        val contextKeys = inputSnapshot["contextKeys"] as List<String>
        assertTrue(contextKeys.contains("trigger"), "contextKeys 應含 trigger 節點鍵")
    }

    @Test
    fun `節點失敗時下游標 SKIPPED 且整體 FAILED`() {
        val nodes = listOf(triggerNode(), toolNode(), outputNode())
        val edges = listOf(edge("trigger", "A"), edge("A", "out"))
        val executors = listOf(
            FakeNodeExecutor(NodeType.TRIGGER) { ctx -> ctx.getOutput(tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor.INPUT_KEY) ?: emptyMap() },
            FakeNodeExecutor(NodeType.TOOL) { throw RuntimeException("boom") },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("final" to "done") }
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        val sequence = sink.events.map { it.event to it.nodeKey }
        assertEquals(
            listOf(
                "execution.started" to null,
                "node.started" to "trigger",
                "node.completed" to "trigger",
                "node.started" to "A",
                "node.failed" to "A",
                "execution.completed" to null
            ),
            sequence
        )
        assertEquals("FAILED", sink.events.last().status)
        // out 無 node.started
        assertTrue(sink.events.none { it.nodeKey == "out" })
        // out 應被標為 SKIPPED 落庫
        val outRecord = nodeExecutionRepo.saved.first { it.nodeKey == "out" }
        assertEquals(tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SKIPPED, outRecord.status)
    }

    @Test
    fun `cancelled 回傳 true 時中止並標 CANCELLED`() {
        val nodes = listOf(triggerNode(), toolNode(), outputNode())
        val edges = listOf(edge("trigger", "A"), edge("A", "out"))
        val executors = listOf(
            FakeNodeExecutor(NodeType.TRIGGER) { ctx -> ctx.getOutput(tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor.INPUT_KEY) ?: emptyMap() },
            FakeNodeExecutor(NodeType.TOOL) { mapOf("result" to "A-done") },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("final" to "done") }
        )
        val (engine, _, _) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { true }

        val sequence = sink.events.map { it.event to it.nodeKey }
        assertEquals(
            listOf(
                "execution.started" to null,
                "execution.completed" to null
            ),
            sequence
        )
        assertEquals("CANCELLED", sink.events.last().status)
    }

    @Test
    fun `不支援的節點型別回報 FAILED 且訊息含型別名`() {
        val conditionNode = node(
            "cond",
            NodeType.CONDITION,
            mapOf("conditions" to listOf(mapOf("left" to "a", "operator" to "eq", "right" to "b")))
        )
        val nodes = listOf(triggerNode(), conditionNode)
        val edges = listOf(edge("trigger", "cond"))
        val executors = listOf(
            FakeNodeExecutor(NodeType.TRIGGER) { ctx -> ctx.getOutput(tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor.INPUT_KEY) ?: emptyMap() }
            // 故意不提供 CONDITION executor
        )
        val (engine, _, _) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        val failedEvent = sink.events.first { it.event == "node.failed" }
        assertEquals("cond", failedEvent.nodeKey)
        assertTrue(failedEvent.error!!.contains("CONDITION"), "訊息應含型別名：${failedEvent.error}")

        val completedEvent = sink.events.last()
        assertEquals("execution.completed", completedEvent.event)
        assertEquals("FAILED", completedEvent.status)
    }

    @Test
    fun `無 OUTPUT 節點時最終輸出取最後節點輸出`() {
        val nodes = listOf(triggerNode(), toolNode())
        val edges = listOf(edge("trigger", "A"))
        val executors = listOf(
            FakeNodeExecutor(NodeType.TRIGGER) { ctx -> ctx.getOutput(tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor.INPUT_KEY) ?: emptyMap() },
            FakeNodeExecutor(NodeType.TOOL) { mapOf("result" to "A-done") }
        )
        val (engine, executionRepo, _) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        val completedEvent = sink.events.last()
        assertEquals("execution.completed", completedEvent.event)
        assertEquals("SUCCESS", completedEvent.status)
        assertEquals(mapOf("result" to "A-done"), completedEvent.output)
        assertEquals(mapOf("result" to "A-done"), executionRepo.lastSaved?.outputResult)
    }
}
