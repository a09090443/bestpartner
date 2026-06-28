package tw.zipe.bastpartner.repository

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import java.util.UUID
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

    /**
     * (a) persist WorkflowEntity 後可由 findOptionalById 取回，id 非空
     */
    @Test
    fun testSaveAndFindWorkflow() {
        val workflow = WorkflowEntity().apply {
            userId = UUID.randomUUID().toString()
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
     * (b) persist WorkflowNodeEntity（config 帶巢狀物件與數值）後 round-trip
     */
    @Test
    fun testNodeConfigJsonRoundTrip() {
        val workflowId = UUID.randomUUID().toString()
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
        // 寬鬆斷言：key 可取回、值字串化比對（避免 Integer/Double 型別差異）
        assertTrue(cfg.containsKey("llmId"))
        assertEquals("x", cfg["llmId"])
        assertTrue(cfg.containsKey("retry"))
        assertEquals("3", cfg["retry"].toString())
        assertTrue(cfg.containsKey("nested"))
        @Suppress("UNCHECKED_CAST")
        val nested = cfg["nested"] as Map<String, Any?>
        assertEquals("1", nested["a"].toString())

        workflowNodeRepository.deleteByWorkflowId(workflowId)
    }

    /**
     * (c) persist WorkflowEdgeEntity（condition 帶非 null Map）後 round-trip
     * 同時驗證 condition 這個 SQL 保留字欄位 round-trip 無語法問題
     */
    @Test
    fun testEdgeConditionReservedWordRoundTrip() {
        val workflowId = UUID.randomUUID().toString()
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
        assertEquals("1", cond["value"].toString())

        workflowEdgeRepository.deleteByWorkflowId(workflowId)
    }

    /**
     * (d) findByWorkflowId 能依 workflowId 篩出 node/edge；deleteByWorkflowId 能刪除
     */
    @Test
    fun testFindAndDeleteByWorkflowId() {
        val workflowId = UUID.randomUUID().toString()

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
