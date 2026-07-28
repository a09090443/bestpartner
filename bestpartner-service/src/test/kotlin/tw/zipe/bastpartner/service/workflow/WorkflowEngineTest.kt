package tw.zipe.bastpartner.service.workflow

import io.quarkus.security.runtime.QuarkusPrincipal
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.security.runtime.SecurityIdentityAssociation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.converter.PasswordEncryptConverter
import tw.zipe.bastpartner.converter.WorkflowSecretConverter
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

        // 測試共用 ManagedExecutor（smallrye-context-propagation 已在 test classpath；
        // 純單元測試無 CDI 容器，context 傳播為 no-op，僅作為節點逾時的執行緒池）
        private val managedExecutor: org.eclipse.microprofile.context.ManagedExecutor by lazy {
            io.smallrye.context.SmallRyeManagedExecutor.builder().build()
        }
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

    /** 提示邊：PROMPT 節點 out:main → LLM 節點 in:prompt（一般資料流邊，會參與活化） */
    private fun promptEdge(source: String, target: String) =
        edge(source, target).apply { targetHandle = WorkflowEngine.PROMPT_INPUT_HANDLE }

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
            SecurityIdentityAssociation(),
            managedExecutor,
            WorkflowSecretConverter(PasswordEncryptConverter().apply { secretKey = "engine-test-key" })
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
        // trigger 輸出 score=80（Task 8 扁平化後一層可取），cond 判斷 {{trigger.score}} gt 60 → 走 true 側
        val nodes = listOf(
            triggerNode(),
            conditionNode(left = "{{trigger.score}}", op = "gt", right = "60"),
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

    // ---------- LOOP（Task 2：LoopExecutor + 引擎子圖迭代） ----------

    /** 建立 LOOP 節點（config 由引擎經 NodeConfigRegistry 解析） */
    private fun loopNode(
        key: String = "loop",
        inputArrayPath: String = "{{trigger.list}}",
        entry: String = "B1",
        maxIterations: Int? = null,
        itemAlias: String? = null,
        collectOutputKey: String? = null
    ) = node(key, NodeType.LOOP, buildMap {
        put("inputArrayPath", inputArrayPath)
        put("loopBodyEntryNodeKey", entry)
        maxIterations?.let { put("maxIterations", it) }
        itemAlias?.let { put("itemAlias", it) }
        collectOutputKey?.let { put("collectOutputKey", it) }
    })

    private fun toolBodyNode(key: String = "B1") = node(key, NodeType.TOOL, mapOf("toolId" to "t1"))
    private fun codeBodyNode(key: String = "B2") = node(key, NodeType.CODE, mapOf("language" to "js", "source" to "x"))

    @Test
    fun `LOOP 三項陣列逐迭代執行子圖並彙集輸出給 done 下游`() {
        // trigger → loop；loop -(out:loop)→ B1 → B2；loop -(out:done)→ D
        val nodes = listOf(triggerNode(), loopNode(), toolBodyNode("B1"), codeBodyNode("B2"), outputNode("D"))
        val edges = listOf(
            edge("trigger", "loop"),
            edge("loop", "B1", "out:loop"),
            edge("B1", "B2"),
            edge("loop", "D", "out:done")
        )
        val executors = listOf(
            triggerExecutor(),
            tw.zipe.bastpartner.service.workflow.executor.LoopExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { ctx -> mapOf("r" to "b1-${ctx.resolvePath("item")}") },
            FakeNodeExecutor(NodeType.CODE) { ctx -> mapOf("r" to "b2-${ctx.resolvePath("item")}") },
            FakeNodeExecutor(NodeType.OUTPUT) { ctx -> mapOf("final" to ctx.resolvePath("loop.items")) }
        )
        val (engine, executionRepo, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, mapOf("list" to listOf("x", "y", "z")), testIdentity(), sink) { false }

        assertEquals("SUCCESS", sink.events.last().status)
        // 子圖節點每迭代各一筆紀錄：B1、B2 各 3 筆，loop_index 0..2 且皆 SUCCESS
        val b1Records = nodeExecutionRepo.saved.filter { it.nodeKey == "B1" }
        val b2Records = nodeExecutionRepo.saved.filter { it.nodeKey == "B2" }
        assertEquals(listOf(0, 1, 2), b1Records.map { it.loopIndex })
        assertEquals(listOf(0, 1, 2), b2Records.map { it.loopIndex })
        assertTrue((b1Records + b2Records).all { it.status == tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SUCCESS })
        // LOOP 節點輸出彙集各迭代「子圖拓撲序最後節點」（B2）的輸出於預設鍵 items
        val expectedItems = listOf(mapOf("r" to "b2-x"), mapOf("r" to "b2-y"), mapOf("r" to "b2-z"))
        assertEquals(mapOf("items" to expectedItems), nodeExecutionRepo.saved.single { it.nodeKey == "loop" }.output)
        // out:done 下游 D 插值取得彙集值，且為最終輸出
        assertEquals(mapOf<String, Any?>("final" to expectedItems), executionRepo.lastSaved?.outputResult)
        // SSE 事件每迭代照發
        assertEquals(3, sink.events.count { it.nodeKey == "B1" && it.event == "node.completed" })
        assertEquals(3, sink.events.count { it.nodeKey == "B2" && it.event == "node.completed" })
    }

    @Test
    fun `LOOP 輸入非陣列時節點 FAILED 且訊息含路徑`() {
        val nodes = listOf(triggerNode(), loopNode(), toolBodyNode("B1"), outputNode("D"))
        val edges = listOf(
            edge("trigger", "loop"),
            edge("loop", "B1", "out:loop"),
            edge("loop", "D", "out:done")
        )
        val executors = listOf(
            triggerExecutor(),
            tw.zipe.bastpartner.service.workflow.executor.LoopExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { mapOf("r" to "b1") },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("final" to "done") }
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, mapOf("list" to "not-a-list"), testIdentity(), sink) { false }

        assertEquals("FAILED", sink.events.last().status)
        val failed = sink.events.single { it.event == "node.failed" }
        assertEquals("loop", failed.nodeKey)
        assertTrue(failed.error!!.contains("{{trigger.list}}"), "訊息應含 path：${failed.error}")
        // 子圖節點未執行且不落紀錄；out:done 下游 D 落 SKIPPED
        assertTrue(nodeExecutionRepo.saved.none { it.nodeKey == "B1" })
        assertEquals(
            tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SKIPPED,
            nodeExecutionRepo.saved.single { it.nodeKey == "D" }.status
        )
    }

    @Test
    fun `LOOP 超出 maxIterations 以上限截斷`() {
        val nodes = listOf(triggerNode(), loopNode(maxIterations = 2), toolBodyNode("B1"))
        val edges = listOf(
            edge("trigger", "loop"),
            edge("loop", "B1", "out:loop")
        )
        val executors = listOf(
            triggerExecutor(),
            tw.zipe.bastpartner.service.workflow.executor.LoopExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { ctx -> mapOf("r" to ctx.resolvePath("item")) }
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, mapOf("list" to listOf(1, 2, 3, 4, 5)), testIdentity(), sink) { false }

        assertEquals("SUCCESS", sink.events.last().status)
        // 僅執行前 2 項
        assertEquals(listOf(0, 1), nodeExecutionRepo.saved.filter { it.nodeKey == "B1" }.map { it.loopIndex })
        @Suppress("UNCHECKED_CAST")
        val items = nodeExecutionRepo.saved.single { it.nodeKey == "loop" }.output?.get("items") as List<Any?>
        assertEquals(2, items.size)
    }

    @Test
    fun `LOOP 迭代中節點失敗則 LOOP FAILED 並中止整體`() {
        val nodes = listOf(triggerNode(), loopNode(), toolBodyNode("B1"), outputNode("D"))
        val edges = listOf(
            edge("trigger", "loop"),
            edge("loop", "B1", "out:loop"),
            edge("loop", "D", "out:done")
        )
        val executors = listOf(
            triggerExecutor(),
            tw.zipe.bastpartner.service.workflow.executor.LoopExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { ctx ->
                if (ctx.resolvePath("item") == "y") throw RuntimeException("boom") else mapOf("r" to "ok")
            },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("final" to "done") }
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, mapOf("list" to listOf("x", "y", "z")), testIdentity(), sink) { false }

        assertEquals("FAILED", sink.events.last().status)
        // 失敗事件序：先子圖節點 B1，再 LOOP 節點本身
        assertEquals(listOf("B1", "loop"), sink.events.filter { it.event == "node.failed" }.map { it.nodeKey })
        // B1 兩筆紀錄：迭代 0 成功、迭代 1 失敗；不再執行迭代 2
        val b1Records = nodeExecutionRepo.saved.filter { it.nodeKey == "B1" }
        assertEquals(listOf(0, 1), b1Records.map { it.loopIndex })
        assertEquals(tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SUCCESS, b1Records[0].status)
        assertEquals(tw.zipe.bastpartner.enumerate.NodeExecutionStatus.FAILED, b1Records[1].status)
        // LOOP 節點 FAILED，錯誤訊息指出失敗的子圖節點
        val loopRecord = nodeExecutionRepo.saved.single { it.nodeKey == "loop" }
        assertEquals(tw.zipe.bastpartner.enumerate.NodeExecutionStatus.FAILED, loopRecord.status)
        assertTrue(loopRecord.errorMessage!!.contains("B1"), "錯誤訊息應含子圖節點鍵：${loopRecord.errorMessage}")
        // out:done 下游 D 落 SKIPPED
        assertEquals(
            tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SKIPPED,
            nodeExecutionRepo.saved.single { it.nodeKey == "D" }.status
        )
    }

    @Test
    fun `LOOP 巢狀插值 item xxx 可解析且支援自訂 collectOutputKey`() {
        val nodes = listOf(triggerNode(), loopNode(collectOutputKey = "results"), toolBodyNode("B1"), outputNode("D"))
        val edges = listOf(
            edge("trigger", "loop"),
            edge("loop", "B1", "out:loop"),
            edge("loop", "D", "out:done")
        )
        val executors = listOf(
            triggerExecutor(),
            tw.zipe.bastpartner.service.workflow.executor.LoopExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { ctx -> mapOf("r" to ctx.resolveTemplate("hi {{item.name}}")) },
            FakeNodeExecutor(NodeType.OUTPUT) { ctx -> mapOf("final" to ctx.resolvePath("loop.results")) }
        )
        val (engine, executionRepo, _) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(
            WORKFLOW_ID, USER_ID,
            mapOf("list" to listOf(mapOf("name" to "a"), mapOf("name" to "b"))),
            testIdentity(), sink
        ) { false }

        assertEquals("SUCCESS", sink.events.last().status)
        assertEquals(
            mapOf<String, Any?>("final" to listOf(mapOf("r" to "hi a"), mapOf("r" to "hi b"))),
            executionRepo.lastSaved?.outputResult
        )
    }

    // ---------- 節點逾時（Task 5：通用逾時機制） ----------

    @Test
    fun `節點執行超過 config timeoutMs 時 FAILED 且訊息含逾時毫秒數與下游 SKIPPED`() {
        // CODE 節點 config 帶 timeoutMs=300，fake executor sleep 遠超過 → 逾時 FAILED
        val codeNode = node("C", NodeType.CODE, mapOf("language" to "js", "source" to "x", "timeoutMs" to 300))
        val nodes = listOf(triggerNode(), codeNode, outputNode("out"))
        val edges = listOf(edge("trigger", "C"), edge("C", "out"))
        val executors = listOf(
            triggerExecutor(),
            FakeNodeExecutor(NodeType.CODE) { Thread.sleep(10_000); mapOf("r" to "late") },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("final" to "done") }
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        val start = System.currentTimeMillis()
        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }
        val elapsed = System.currentTimeMillis() - start

        assertTrue(elapsed < 5_000, "逾時應在 timeoutMs 附近觸發而非等待 executor 完成（實際 ${elapsed}ms）")
        assertEquals("FAILED", sink.events.last().status)
        val failed = sink.events.single { it.event == "node.failed" }
        assertEquals("C", failed.nodeKey)
        assertTrue(failed.error!!.contains("300"), "訊息應含逾時毫秒數：${failed.error}")
        // C 落 FAILED 紀錄且訊息含逾時毫秒數；下游 out 落 SKIPPED
        val cRecord = nodeExecutionRepo.saved.single { it.nodeKey == "C" }
        assertEquals(tw.zipe.bastpartner.enumerate.NodeExecutionStatus.FAILED, cRecord.status)
        assertTrue(cRecord.errorMessage!!.contains("300"))
        assertEquals(
            tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SKIPPED,
            nodeExecutionRepo.saved.single { it.nodeKey == "out" }.status
        )
    }

    @Test
    fun `未逾時節點不受逾時機制影響正常完成`() {
        // 同樣帶 timeoutMs=300 但執行極快 → SUCCESS，輸出照常寫回 context
        val codeNode = node("C", NodeType.CODE, mapOf("language" to "js", "source" to "x", "timeoutMs" to 300))
        val nodes = listOf(triggerNode(), codeNode, outputNode("out"))
        val edges = listOf(edge("trigger", "C"), edge("C", "out"))
        val executors = listOf(
            triggerExecutor(),
            FakeNodeExecutor(NodeType.CODE) { mapOf("r" to "fast") },
            FakeNodeExecutor(NodeType.OUTPUT) { ctx -> mapOf("final" to ctx.resolvePath("C.r")) }
        )
        val (engine, executionRepo, _) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        assertEquals("SUCCESS", sink.events.last().status)
        assertEquals(mapOf<String, Any?>("final" to "fast"), executionRepo.lastSaved?.outputResult)
    }

    @Test
    fun `逾時機制下節點失敗例外訊息維持原樣不被包裝`() {
        // executor 拋一般例外 → 經逾時包裝層後訊息仍為原始例外訊息（ExecutionException 需解包）
        val nodes = listOf(triggerNode(), toolNode())
        val edges = listOf(edge("trigger", "A"))
        val executors = listOf(
            triggerExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { throw RuntimeException("boom") }
        )
        val (engine, _, _) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        val failed = sink.events.single { it.event == "node.failed" }
        assertEquals("boom", failed.error)
    }

    @Test
    fun `逾時機制下插值變數不存在仍轉為 i18n 訊息`() {
        // VariableNotFoundException 經 ExecutionException 解包後仍走 failureMessage 的 i18n 轉換
        val nodes = listOf(triggerNode(), toolNode())
        val edges = listOf(edge("trigger", "A"))
        val executors = listOf(
            triggerExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { ctx -> mapOf("r" to ctx.resolvePath("no.such.path")) }
        )
        val (engine, _, _) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        val failed = sink.events.single { it.event == "node.failed" }
        assertTrue(failed.error!!.contains("no.such.path"), "訊息應含變數路徑：${failed.error}")
    }

    // ---------- trigger 扁平化 + memory 清理（Task 8） ----------

    @Test
    fun `trigger 輸出扁平化後下游一層插值即可取得 input 欄位`() {
        // input {name: gary} → 下游以 {{trigger.name}} 直接取值，不再需要 .input. 一層
        val nodes = listOf(triggerNode(), toolNode())
        val edges = listOf(edge("trigger", "A"))
        val executors = listOf(
            tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { ctx -> mapOf("greet" to ctx.resolveTemplate("hi {{trigger.name}}")) }
        )
        val (engine, executionRepo, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, mapOf("name" to "gary"), testIdentity(), sink) { false }

        assertEquals("SUCCESS", sink.events.last().status)
        assertEquals(mapOf<String, Any?>("greet" to "hi gary"), executionRepo.lastSaved?.outputResult)
        // trigger 節點輸出即 input map 本身（無 input 包裝層）
        assertEquals(mapOf<String, Any?>("name" to "gary"), nodeExecutionRepo.saved.single { it.nodeKey == "trigger" }.output)
    }

    @Test
    fun `trigger input 為 null 時輸出空 map`() {
        val nodes = listOf(triggerNode())
        val executors = listOf<NodeExecutor>(tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor())
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, emptyList(), executors)
        val sink = CollectingSink()

        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        assertEquals("SUCCESS", sink.events.last().status)
        assertEquals(emptyMap<String, Any?>(), nodeExecutionRepo.saved.single { it.nodeKey == "trigger" }.output)
    }

    @Test
    fun `執行成功後以 executionId 註冊的 memory 被清除`() {
        // fake executor 模擬 LLM 節點：以 executionId 為 memoryId 於 PersistentChatMemoryStore 累積訊息
        val store = tw.zipe.bastpartner.config.PersistentChatMemoryStore()
        val nodes = listOf(triggerNode(), toolNode())
        val edges = listOf(edge("trigger", "A"))
        var memoryDuringExecution = 0
        val executors = listOf(
            tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { ctx ->
                store.updateMessages(ctx.executionId, mutableListOf(dev.langchain4j.data.message.UserMessage.from("hi")))
                memoryDuringExecution = store.getMessages(ctx.executionId).size
                mapOf("r" to "ok")
            }
        )
        val (engine, _, _) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        val executionId = engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        assertEquals("SUCCESS", sink.events.last().status)
        assertEquals(1, memoryDuringExecution, "執行中 memory 應有累積")
        assertTrue(store.getMessages(executionId).isEmpty(), "執行結束後 memory 應被清除")
    }

    @Test
    fun `執行失敗後以 executionId 註冊的 memory 亦被清除`() {
        val store = tw.zipe.bastpartner.config.PersistentChatMemoryStore()
        val nodes = listOf(triggerNode(), toolNode())
        val edges = listOf(edge("trigger", "A"))
        val executors = listOf(
            tw.zipe.bastpartner.service.workflow.executor.TriggerExecutor(),
            FakeNodeExecutor(NodeType.TOOL) { ctx ->
                store.updateMessages(ctx.executionId, mutableListOf(dev.langchain4j.data.message.UserMessage.from("hi")))
                throw RuntimeException("boom")
            }
        )
        val (engine, _, _) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()

        val executionId = engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        assertEquals("FAILED", sink.events.last().status)
        assertTrue(store.getMessages(executionId).isEmpty(), "失敗結束後 memory 應被清除")
    }

    @Test
    fun `子圖含巢狀 LOOP 於 validateForExecution 即報錯`() {
        val nodes = listOf(
            triggerNode(),
            loopNode(key = "loop1", entry = "loop2"),
            loopNode(key = "loop2", entry = "B1"),
            toolBodyNode("B1")
        )
        val edges = listOf(
            edge("trigger", "loop1"),
            edge("loop1", "loop2", "out:loop"),
            edge("loop2", "B1", "out:loop")
        )
        val (engine, _, _) = buildEngine(nodes, edges, emptyList())

        val ex = assertThrows(tw.zipe.bastpartner.exception.ServiceException::class.java) {
            engine.validateForExecution(WORKFLOW_ID)
        }
        assertTrue(ex.message!!.contains("loop2"), "訊息應指出巢狀 LOOP 節點：${ex.message}")
    }

    @Test
    fun `KNOWLEDGE_RAG 掛到 LLM 工具埠時作為能力被排除主遍歷且不落執行紀錄`() {
        val nodes = listOf(
            triggerNode(),
            node("llm", NodeType.LLM_ASSISTANT, mapOf("llmId" to "m1", "userPrompt" to "hi")),
            node("rag", NodeType.KNOWLEDGE_RAG, mapOf("knowledgeId" to "k1")),
            outputNode()
        )
        val edges = listOf(
            edge("trigger", "llm"),
            edge("llm", "out"),
            // 純能力掛載邊：targetHandle = in:tool（KNOWLEDGE_RAG 的唯一出邊）
            edge("rag", "llm").apply { targetHandle = "in:tool" }
        )
        var mountedConfigs: List<NodeConfig>? = null
        val executors = listOf(
            FakeNodeExecutor(NodeType.TRIGGER) { mapOf("input" to emptyMap<String, Any?>()) },
            FakeNodeExecutor(NodeType.LLM_ASSISTANT) { ctx ->
                mountedConfigs = ctx.capabilitiesFor("llm").map { it.config }
                mapOf("reply" to "ok")
            },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("output" to "done") }
            // 刻意不提供 KNOWLEDGE_RAG executor：純能力節點應被排除主遍歷，不會被分派
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()
        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        // rag 作為純能力節點：不落執行紀錄、不發節點事件
        assertTrue(nodeExecutionRepo.saved.none { it.nodeKey == "rag" }, "純能力 KNOWLEDGE_RAG 不應落執行紀錄")
        assertTrue(sink.events.none { it.nodeKey == "rag" }, "純能力 KNOWLEDGE_RAG 不應發節點事件")
        // LLM 節點可從 capabilitiesFor 取得掛載的知識庫能力
        assertTrue(
            mountedConfigs?.any { it is tw.zipe.bastpartner.dto.workflow.config.KnowledgeRagNodeConfig } == true,
            "LLM 應能從工具埠取得掛載的 KnowledgeRagNodeConfig"
        )
        // 整體仍成功完成
        assertTrue(sink.events.any { it.event == "execution.completed" })
    }

    // ---------- PROMPT 節點（提示埠 in:prompt）----------

    private fun promptNode(key: String, text: String, outputKey: String? = null) = node(
        key,
        NodeType.PROMPT,
        buildMap {
            put("prompt", text)
            outputKey?.let { put("outputKey", it) }
        }
    )

    /** 記錄 LLM 節點實際取得的提問（模擬 LlmAssistantExecutor.resolveMessage 的來源查找） */
    private fun capturingLlmExecutor(nodeKey: String, captured: MutableList<String?>) =
        FakeNodeExecutor(NodeType.LLM_ASSISTANT) { ctx ->
            captured.add(
                ctx.promptSourcesFor(nodeKey).firstNotNullOfOrNull { src ->
                    ctx.getOutput(src.nodeKey)?.get(src.outputKey) as? String
                }
            )
            mapOf("reply" to "ok")
        }

    @Test
    fun `PROMPT 節點經 in prompt 邊提供提問且照常執行落紀錄`() {
        val nodes = listOf(
            triggerNode(),
            promptNode("promptA", "以正式語氣回答"),
            node("llm", NodeType.LLM_ASSISTANT, mapOf("llmId" to "m1")),
            outputNode()
        )
        val edges = listOf(edge("trigger", "promptA"), promptEdge("promptA", "llm"), edge("llm", "out"))
        val captured = mutableListOf<String?>()
        val executors = listOf(
            FakeNodeExecutor(NodeType.TRIGGER) { emptyMap() },
            FakeNodeExecutor(NodeType.PROMPT) { mapOf("prompt" to "以正式語氣回答") },
            capturingLlmExecutor("llm", captured),
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("output" to "done") }
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        val sink = CollectingSink()
        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), sink) { false }

        assertEquals(listOf<String?>("以正式語氣回答"), captured)
        // 與 SKILL 等純能力節點相反：PROMPT 是資料流節點，照常落紀錄與發事件
        assertTrue(nodeExecutionRepo.saved.any { it.nodeKey == "promptA" }, "PROMPT 節點應落執行紀錄")
        assertTrue(sink.events.any { it.nodeKey == "promptA" && it.event == "node.completed" })
    }

    @Test
    fun `PROMPT 節點自訂 outputKey 時引擎解析為正確的來源鍵`() {
        val nodes = listOf(
            triggerNode(),
            promptNode("promptA", "自訂鍵提問", outputKey = "askText"),
            node("llm", NodeType.LLM_ASSISTANT, mapOf("llmId" to "m1")),
            outputNode()
        )
        val edges = listOf(edge("trigger", "promptA"), promptEdge("promptA", "llm"), edge("llm", "out"))
        val captured = mutableListOf<String?>()
        val executors = listOf(
            FakeNodeExecutor(NodeType.TRIGGER) { emptyMap() },
            FakeNodeExecutor(NodeType.PROMPT) { mapOf("askText" to "自訂鍵提問") },
            capturingLlmExecutor("llm", captured),
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("output" to "done") }
        )
        val (engine, _, _) = buildEngine(nodes, edges, executors)
        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), CollectingSink()) { false }

        assertEquals(listOf<String?>("自訂鍵提問"), captured)
    }

    @Test
    fun `唯一入邊為 in prompt 的 LLM 節點不被誤判 SKIPPED`() {
        // in:prompt 是資料流邊（與 in:tool 相反），故能單獨活化 LLM 節點
        val nodes = listOf(
            triggerNode(),
            promptNode("promptA", "提問"),
            node("llm", NodeType.LLM_ASSISTANT, mapOf("llmId" to "m1")),
            outputNode()
        )
        val edges = listOf(edge("trigger", "promptA"), promptEdge("promptA", "llm"), edge("llm", "out"))
        val executors = listOf(
            FakeNodeExecutor(NodeType.TRIGGER) { emptyMap() },
            FakeNodeExecutor(NodeType.PROMPT) { mapOf("prompt" to "提問") },
            FakeNodeExecutor(NodeType.LLM_ASSISTANT) { mapOf("reply" to "ok") },
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("output" to "done") }
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), CollectingSink()) { false }

        val llmRecord = nodeExecutionRepo.saved.first { it.nodeKey == "llm" }
        assertEquals(tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SUCCESS, llmRecord.status)
    }

    @Test
    fun `CONDITION 分支只活化其中一個 PROMPT 時 LLM 取該分支的提問`() {
        val nodes = listOf(
            triggerNode(),
            node("cond", NodeType.CONDITION, mapOf("conditions" to listOf(mapOf("left" to "1", "operator" to "eq", "right" to "1")))),
            promptNode("promptA", "正式語氣"),
            promptNode("promptB", "條列語氣"),
            node("llm", NodeType.LLM_ASSISTANT, mapOf("llmId" to "m1")),
            outputNode()
        )
        val edges = listOf(
            edge("trigger", "cond"),
            edge("cond", "promptA", "out:true"),
            edge("cond", "promptB", "out:false"),
            promptEdge("promptA", "llm"),
            promptEdge("promptB", "llm"),
            edge("llm", "out")
        )
        val captured = mutableListOf<String?>()
        val executors = listOf(
            FakeNodeExecutor(NodeType.TRIGGER) { emptyMap() },
            FakeNodeExecutor(NodeType.CONDITION) {
                mapOf(
                    tw.zipe.bastpartner.service.workflow.executor.ConditionExecutor.OUTPUT_RESULT to true,
                    tw.zipe.bastpartner.service.workflow.executor.ConditionExecutor.OUTPUT_BRANCH to "out:true"
                )
            },
            // 依 nodeKey 回傳不同提問，驗證取到的是被活化那條
            FakeNodeExecutor(NodeType.PROMPT) { ctx ->
                if (ctx.allOutputs().containsKey("promptA")) mapOf("prompt" to "條列語氣") else mapOf("prompt" to "正式語氣")
            },
            capturingLlmExecutor("llm", captured),
            FakeNodeExecutor(NodeType.OUTPUT) { mapOf("output" to "done") }
        )
        val (engine, _, nodeExecutionRepo) = buildEngine(nodes, edges, executors)
        engine.execute(WORKFLOW_ID, USER_ID, null, testIdentity(), CollectingSink()) { false }

        // true 分支的 promptA 執行、false 分支的 promptB 落 SKIPPED，LLM 取 promptA 的輸出
        assertEquals(listOf<String?>("正式語氣"), captured)
        assertEquals(
            tw.zipe.bastpartner.enumerate.NodeExecutionStatus.SKIPPED,
            nodeExecutionRepo.saved.first { it.nodeKey == "promptB" }.status
        )
    }

    @Test
    fun `孤兒 PROMPT 節點於 validateForExecution 即報錯`() {
        val nodes = listOf(
            triggerNode(),
            promptNode("promptA", "沒人取用的提問"),
            node("llm", NodeType.LLM_ASSISTANT, mapOf("llmId" to "m1", "userPrompt" to "hi")),
            outputNode()
        )
        // promptA 沒有連到任何 LLM 的 in:prompt 埠
        val edges = listOf(edge("trigger", "llm"), edge("llm", "out"))
        val (engine, _, _) = buildEngine(nodes, edges, emptyList())

        val ex = assertThrows(tw.zipe.bastpartner.exception.ServiceException::class.java) {
            engine.validateForExecution(WORKFLOW_ID)
        }
        assertTrue(ex.message!!.contains("promptA"), "訊息應指出孤兒 PROMPT 節點：${ex.message}")
    }

    @Test
    fun `LLM 既無 userPrompt 也無 PROMPT 來源時 validateForExecution 即報錯`() {
        val nodes = listOf(
            triggerNode(),
            node("llm", NodeType.LLM_ASSISTANT, mapOf("llmId" to "m1")),
            outputNode()
        )
        val edges = listOf(edge("trigger", "llm"), edge("llm", "out"))
        val (engine, _, _) = buildEngine(nodes, edges, emptyList())

        val ex = assertThrows(tw.zipe.bastpartner.exception.ServiceException::class.java) {
            engine.validateForExecution(WORKFLOW_ID)
        }
        assertTrue(ex.message!!.contains("llm"), "訊息應指出缺提問來源的 LLM 節點：${ex.message}")
    }

    @Test
    fun `LLM 無 userPrompt 但有 PROMPT 連入時通過驗證`() {
        val nodes = listOf(
            triggerNode(),
            promptNode("promptA", "提問"),
            node("llm", NodeType.LLM_ASSISTANT, mapOf("llmId" to "m1")),
            outputNode()
        )
        val edges = listOf(edge("trigger", "promptA"), promptEdge("promptA", "llm"), edge("llm", "out"))
        val (engine, _, _) = buildEngine(nodes, edges, emptyList())

        engine.validateForExecution(WORKFLOW_ID)
    }
}
