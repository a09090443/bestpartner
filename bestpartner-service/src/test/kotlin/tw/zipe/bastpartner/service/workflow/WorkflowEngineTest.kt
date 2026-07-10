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

    private fun edge(source: String, target: String, handle: String? = null) = WorkflowEdgeEntity().apply {
        sourceNodeKey = source
        targetNodeKey = target
        sourceHandle = handle
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

    // ---------- 條件分支（Task 1：活化遍歷 + ConditionExecutor + SKIPPED 可達性） ----------

    /** 建立 CONDITION 節點：單一條件 left op right（config 由引擎經 NodeConfigRegistry 解析） */
    private fun conditionNode(key: String = "cond", left: String = "1", op: String = "eq", right: String = "1") =
        node(key, NodeType.CONDITION, mapOf("conditions" to listOf(mapOf("left" to left, "operator" to op, "right" to right))))

    private fun triggerExecutor() = FakeNodeExecutor(NodeType.TRIGGER) { ctx ->
        ctx.getOutput(tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor.INPUT_KEY) ?: emptyMap()
    }

    @Test
    fun `CONDITION true 分支活化 true 側且 false 側落 SKIPPED 不發事件`() {
        // trigger → cond；cond -(out:true)→ A(TOOL)、-(out:false)→ B(OUTPUT)
        val nodes = listOf(triggerNode(), conditionNode(left = "1", op = "eq", right = "1"), toolNode("A"), outputNode("B"))
        val edges = listOf(
            edge("trigger", "cond"),
            edge("cond", "A", "out:true"),
            edge("cond", "B", "out:false")
        )
        val executors = listOf(
            triggerExecutor(),
            tw.zipe.bastpartner.service.workflow.executor.ConditionExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { mapOf("result" to "A-done") },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("final" to "B-done") }
        )
        val (engine, executionRepo, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        // A 有事件、B 完全沒有事件（SKIPPED 只落庫）
        assertTrue(sink.events.any { it.nodeKey == "A" && it.event == "node.completed" })
        assertTrue(sink.events.none { it.nodeKey == "B" })
        assertEquals("SUCCESS", sink.events.last().status)
        // B 落一筆 SKIPPED 紀錄且含 seqNo
        val bRecord = nodeExecutionRepo.saved.single { it.nodeKey == "B" }
        assertEquals(tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SKIPPED, bRecord.status)
        assertTrue(bRecord.seqNo > 0, "SKIPPED 紀錄應含 seqNo")
        // 唯一 OUTPUT 節點 B 被跳過 → 最終輸出取實際執行成功的最後節點 A
        assertEquals(mapOf("result" to "A-done"), executionRepo.lastSaved?.outputResult)
    }

    @Test
    fun `CONDITION false 分支活化 false 側且 true 側落 SKIPPED`() {
        val nodes = listOf(triggerNode(), conditionNode(left = "1", op = "eq", right = "2"), toolNode("A"), outputNode("B"))
        val edges = listOf(
            edge("trigger", "cond"),
            edge("cond", "A", "out:true"),
            edge("cond", "B", "out:false")
        )
        val executors = listOf(
            triggerExecutor(),
            tw.zipe.bastpartner.service.workflow.executor.ConditionExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { mapOf("result" to "A-done") },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("final" to "B-done") }
        )
        val (engine, executionRepo, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        assertTrue(sink.events.any { it.nodeKey == "B" && it.event == "node.completed" })
        assertTrue(sink.events.none { it.nodeKey == "A" })
        assertEquals("SUCCESS", sink.events.last().status)
        val aRecord = nodeExecutionRepo.saved.single { it.nodeKey == "A" }
        assertEquals(tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SKIPPED, aRecord.status)
        // 執行到的 OUTPUT 節點 B 為最終輸出
        assertEquals(mapOf("final" to "B-done"), executionRepo.lastSaved?.outputResult)
    }

    @Test
    fun `分支下游失敗時其餘節點補 SKIPPED 且不重複記錄`() {
        // cond true → Z(TOOL，執行失敗)；false → B(OUTPUT)。拓撲序 B 在 Z 之前，
        // B 於主迴圈即落 SKIPPED，收尾迴圈不得重複記錄。
        val nodes = listOf(triggerNode(), conditionNode(left = "1", op = "eq", right = "1"), outputNode("B"), toolNode("Z"))
        val edges = listOf(
            edge("trigger", "cond"),
            edge("cond", "Z", "out:true"),
            edge("cond", "B", "out:false")
        )
        val executors = listOf(
            triggerExecutor(),
            tw.zipe.bastpartner.service.workflow.executor.ConditionExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { throw RuntimeException("boom") },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("final" to "B-done") }
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        assertEquals("FAILED", sink.events.last().status)
        assertTrue(sink.events.any { it.nodeKey == "Z" && it.event == "node.failed" })
        // B 僅一筆 SKIPPED 紀錄（主迴圈落過，收尾迴圈不重複）
        val bRecords = nodeExecutionRepo.saved.filter { it.nodeKey == "B" }
        assertEquals(1, bRecords.size, "B 應僅一筆紀錄")
        assertEquals(tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SKIPPED, bRecords[0].status)
    }

    @Test
    fun `CONDITION 條件插值引用上游輸出決定分支`() {
        // trigger 輸出 score=80，cond 判斷 {{trigger.input.score}} gt 60 → 走 true 側
        val nodes = listOf(
            triggerNode(),
            conditionNode(left = "{{trigger.input.score}}", op = "gt", right = "60"),
            toolNode("A"),
            outputNode("B")
        )
        val edges = listOf(
            edge("trigger", "cond"),
            edge("cond", "A", "out:true"),
            edge("cond", "B", "out:false")
        )
        val executors = listOf(
            triggerExecutor(),
            tw.zipe.bastpartner.service.workflow.executor.ConditionExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { mapOf("result" to "A-done") },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("final" to "B-done") }
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, mapOf("score" to 80), testIdentity(), sink) { false }

        assertEquals("SUCCESS", sink.events.last().status)
        assertTrue(sink.events.any { it.nodeKey == "A" && it.event == "node.completed" })
        assertEquals(
            tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SKIPPED,
            nodeExecutionRepo.saved.single { it.nodeKey == "B" }.status
        )
    }
}
