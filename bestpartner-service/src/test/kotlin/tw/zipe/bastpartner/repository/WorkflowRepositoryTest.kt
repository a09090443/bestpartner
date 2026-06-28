package tw.zipe.bastpartner.repository

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.entity.WorkflowEdgeEntity
import tw.zipe.bastpartner.entity.WorkflowEntity
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.enumerate.WorkflowStatus

/**
 * Workflow / Node / Edge repository 整合測試
 *
 * 注意（JSON 型別漂移）：canvasMeta / config / condition 三個欄位以 SqlTypes.JSON 儲存，
 * 經 JSON round-trip 後數值會回為 Integer（即使寫入時為 Int/Double），巢狀物件會回為
 * LinkedHashMap。因此本測試一律以「key 存在 + 值字串化」做寬鬆斷言；Task 7 service 層
 * 取值時亦勿假設為 Int/Double 或特定 Map 實作，應做型別轉換。
 *
 * @author Gary
 * @created 2026/6/28
 */
@QuarkusTest
class WorkflowRepositoryTest {

    @Inject
    lateinit var workflowRepository: WorkflowRepository

    @Inject
    lateinit var workflowNodeRepository: WorkflowNodeRepository

    @Inject
    lateinit var workflowEdgeRepository: WorkflowEdgeRepository

    // 追蹤本測試類建立的資料，於 @AfterEach 統一清理，避免整合測試殘留
    private val createdUserIds = mutableListOf<String>()
    private val touchedWorkflowIdsForNodeEdge = mutableListOf<String>()

    @AfterEach
    fun cleanup() {
        touchedWorkflowIdsForNodeEdge.forEach { workflowId ->
            workflowNodeRepository.deleteByWorkflowId(workflowId)
            workflowEdgeRepository.deleteByWorkflowId(workflowId)
        }
        touchedWorkflowIdsForNodeEdge.clear()

        // workflow 以唯一隨機 userId 建立，依 userId 精準刪除
        createdUserIds.forEach { userId -> workflowRepository.deleteByUserId(userId) }
        createdUserIds.clear()
    }

    /**
     * (a) persist WorkflowEntity 後可由 findOptionalById 取回，id 非空
     */
    @Test
    fun testSaveAndFindWorkflow() {
        val userId = UUID.randomUUID().toString()
        createdUserIds.add(userId)
        val workflow = WorkflowEntity().apply {
            this.userId = userId
            name = "測試工作流"
            description = "整合測試用"
            status = WorkflowStatus.DRAFT
            version = 1
            canvasMeta = mapOf("zoom" to 1.0, "viewport" to mapOf("x" to 10, "y" to 20))
        }

        val saved = workflowRepository.saveOrUpdate(workflow)
        assertNotNull(saved.id, "persist 後 id 應自動產生")

        val found = workflowRepository.findOptionalById(saved.id!!)
        assertNotNull(found, "應能依 id 取回 workflow")
        assertEquals("測試工作流", found!!.name)
        assertNotNull(found.canvasMeta)
        assertTrue(found.canvasMeta!!.containsKey("zoom"))
        assertTrue(found.canvasMeta!!.containsKey("viewport"))
    }

    /**
     * Minor #4：覆蓋 WorkflowRepository.findByUserId
     */
    @Test
    fun testFindByUserId() {
        val userId = UUID.randomUUID().toString()
        createdUserIds.add(userId)
        val workflow = WorkflowEntity().apply {
            this.userId = userId
            name = "依用戶查詢測試"
            status = WorkflowStatus.ACTIVE
        }
        val saved = workflowRepository.saveOrUpdate(workflow)
        assertNotNull(saved.id)

        val result = workflowRepository.findByUserId(userId)
        assertEquals(1, result.size, "應依 userId 查得 1 筆 workflow")
        assertEquals(saved.id, result.first().id)
        assertEquals(userId, result.first().userId)
    }

    /**
     * (b) persist WorkflowNodeEntity（config 帶巢狀物件與數值）後 round-trip
     */
    @Test
    fun testNodeConfigJsonRoundTrip() {
        val workflowId = UUID.randomUUID().toString()
        touchedWorkflowIdsForNodeEdge.add(workflowId)
        val node = WorkflowNodeEntity().apply {
            this.workflowId = workflowId
            nodeKey = "node-1"
            type = NodeType.LLM_ASSISTANT
            name = "LLM 節點"
            positionX = 100.0
            positionY = 200.0
            config = mapOf(
                "llmId" to "x",
                "retry" to 3,
                "nested" to mapOf("a" to 1)
            )
        }

        val saved = workflowNodeRepository.saveOrUpdate(node)
        assertNotNull(saved.id)

        val found = workflowNodeRepository.findOptionalById(saved.id!!)
        assertNotNull(found, "應能取回 node")
        val cfg = found!!.config
        // 寬鬆斷言：JSON round-trip 後數值回為 Integer、巢狀回為 LinkedHashMap，
        // 故逐鍵存在 + 值字串化比對（避免 Integer/Double 型別差異）
        assertTrue(cfg.containsKey("llmId"))
        assertEquals("x", cfg["llmId"])
        assertTrue(cfg.containsKey("retry"))
        assertEquals("3", cfg["retry"].toString())
        assertTrue(cfg.containsKey("nested"))
        @Suppress("UNCHECKED_CAST")
        val nested = cfg["nested"] as Map<String, Any?>
        assertEquals("1", nested["a"].toString())
    }

    /**
     * (c) persist WorkflowEdgeEntity（condition 帶非 null Map）後 round-trip
     * 同時驗證 condition 這個 SQL 保留字欄位 round-trip 無語法問題
     */
    @Test
    fun testEdgeConditionReservedWordRoundTrip() {
        val workflowId = UUID.randomUUID().toString()
        touchedWorkflowIdsForNodeEdge.add(workflowId)
        val edge = WorkflowEdgeEntity().apply {
            this.workflowId = workflowId
            sourceNodeKey = "node-1"
            targetNodeKey = "node-2"
            label = "成立時"
            condition = mapOf("op" to "eq", "value" to 1)
        }

        val saved = workflowEdgeRepository.saveOrUpdate(edge)
        assertNotNull(saved.id)

        val found = workflowEdgeRepository.findOptionalById(saved.id!!)
        assertNotNull(found, "應能取回 edge")
        val cond = found!!.condition
        assertNotNull(cond, "condition 應可正確讀回（保留字欄位 round-trip）")
        assertTrue(cond!!.containsKey("op"))
        assertEquals("eq", cond["op"])
        // value 寫入時為 Int 1，round-trip 後回為 Integer，故以字串化比對
        assertEquals("1", cond["value"].toString())
    }

    /**
     * (d) findByWorkflowId 能依 workflowId 篩出 node/edge；deleteByWorkflowId 能刪除
     */
    @Test
    fun testFindAndDeleteByWorkflowId() {
        val workflowId = UUID.randomUUID().toString()
        touchedWorkflowIdsForNodeEdge.add(workflowId)

        workflowNodeRepository.saveOrUpdate(WorkflowNodeEntity().apply {
            this.workflowId = workflowId
            nodeKey = "n1"
            type = NodeType.TRIGGER
            positionX = 0.0
            positionY = 0.0
            config = mapOf("k" to "v")
        })
        workflowNodeRepository.saveOrUpdate(WorkflowNodeEntity().apply {
            this.workflowId = workflowId
            nodeKey = "n2"
            type = NodeType.TOOL
            positionX = 50.0
            positionY = 50.0
            config = mapOf("k" to "v2")
        })
        workflowEdgeRepository.saveOrUpdate(WorkflowEdgeEntity().apply {
            this.workflowId = workflowId
            sourceNodeKey = "n1"
            targetNodeKey = "n2"
        })

        val nodes = workflowNodeRepository.findByWorkflowId(workflowId)
        assertEquals(2, nodes.size, "應依 workflowId 篩出 2 個 node")

        val edges = workflowEdgeRepository.findByWorkflowId(workflowId)
        assertEquals(1, edges.size, "應依 workflowId 篩出 1 個 edge")

        val deletedNodes = workflowNodeRepository.deleteByWorkflowId(workflowId)
        assertEquals(2L, deletedNodes, "應刪除 2 個 node")

        val deletedEdges = workflowEdgeRepository.deleteByWorkflowId(workflowId)
        assertEquals(1L, deletedEdges, "應刪除 1 個 edge")

        assertTrue(workflowNodeRepository.findByWorkflowId(workflowId).isEmpty())
        assertTrue(workflowEdgeRepository.findByWorkflowId(workflowId).isEmpty())
    }
}
