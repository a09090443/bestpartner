package tw.zipe.bastpartner.service

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import jakarta.inject.Inject
import java.util.UUID
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tw.zipe.bastpartner.dto.WorkflowEdgeDTO
import tw.zipe.bastpartner.dto.WorkflowNodeDTO
import tw.zipe.bastpartner.dto.WorkflowSaveRequestDTO
import tw.zipe.bastpartner.entity.WorkflowEntity
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.enumerate.WorkflowStatus
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.repository.WorkflowEdgeRepository
import tw.zipe.bastpartner.repository.WorkflowNodeRepository
import tw.zipe.bastpartner.repository.WorkflowRepository
import tw.zipe.bastpartner.util.MessageUtil

/**
 * WorkflowService 整合測試（@QuarkusTest，連真實 PostgreSQL）
 *
 * 注意（JSON 型別漂移）：config / canvasMeta / condition 三個欄位以 SqlTypes.JSON 儲存，
 * round-trip 後數值回為 Integer、巢狀回為 LinkedHashMap，故一律以「key 存在 + 值字串化」
 * 做寬鬆斷言。
 *
 * @author Gary
 * @created 2026/6/29
 */
@QuarkusTest
@TestSecurity(user = WorkflowServiceTest.TEST_USER, roles = ["user"])
class WorkflowServiceTest {

    companion object {
        // @TestSecurity 的 user 即為 principal name（= userId），必須為編譯期常數
        const val TEST_USER = "11111111-1111-1111-1111-111111111111"
        const val OTHER_USER = "22222222-2222-2222-2222-222222222222"
    }

    @Inject
    lateinit var workflowService: WorkflowService

    @Inject
    lateinit var workflowRepository: WorkflowRepository

    @Inject
    lateinit var workflowNodeRepository: WorkflowNodeRepository

    @Inject
    lateinit var workflowEdgeRepository: WorkflowEdgeRepository

    private val touchedWorkflowIds = mutableListOf<String>()

    @AfterEach
    fun cleanup() {
        touchedWorkflowIds.forEach { workflowId ->
            workflowNodeRepository.deleteByWorkflowId(workflowId)
            workflowEdgeRepository.deleteByWorkflowId(workflowId)
        }
        touchedWorkflowIds.clear()
        // 兩個測試用 userId 的 workflow 一律清除
        workflowRepository.deleteByUserId(TEST_USER)
        workflowRepository.deleteByUserId(OTHER_USER)
    }

    private fun node(key: String, type: NodeType, configBuilder: (kotlinx.serialization.json.JsonObjectBuilder.() -> Unit)? = null): WorkflowNodeDTO =
        WorkflowNodeDTO().apply {
            nodeKey = key
            this.type = type
            name = "節點-$key"
            positionX = 10.0
            positionY = 20.0
            configBuilder?.let { config = buildJsonObject(it) }
        }

    private fun edge(source: String, target: String): WorkflowEdgeDTO =
        WorkflowEdgeDTO().apply {
            sourceNodeKey = source
            targetNodeKey = target
        }

    /**
     * 案例 1：save 後 get 可無損取回 nodes 與 edges（含巢狀 config）
     */
    @Test
    fun testSaveThenGetRoundTrip() {
        val keyA = UUID.randomUUID().toString().substring(0, 8)
        val keyB = UUID.randomUUID().toString().substring(0, 8)
        val req = WorkflowSaveRequestDTO().apply {
            name = "RoundTrip 測試"
            description = "整合測試"
            nodes = listOf(
                node(keyA, NodeType.TRIGGER) {
                    put("triggerType", "MANUAL")
                    putJsonObject("inputSchema") { put("a", 1) }
                },
                node(keyB, NodeType.LLM_ASSISTANT) { put("llmId", "x") }
            )
            edges = listOf(edge(keyA, keyB))
        }

        val saved = workflowService.save(req)
        assertNotNull(saved.id)
        saved.id?.let { touchedWorkflowIds.add(it) }

        val got = workflowService.get(saved.id!!)
        assertEquals(2, got.nodes.size, "應取回 2 個節點")
        assertEquals(1, got.edges.size, "應取回 1 條連線")

        val nodeA = got.nodes.first { it.nodeKey == keyA }
        assertTrue(nodeA.config.containsKey("triggerType"))
        assertTrue(nodeA.config.containsKey("inputSchema"))
        // 巢狀數值 round-trip：inputSchema.a 應保值為 1
        assertEquals("1", (nodeA.config["inputSchema"] as JsonObject)["a"].toString())

        val gotEdge = got.edges.first()
        assertEquals(keyA, gotEdge.sourceNodeKey)
        assertEquals(keyB, gotEdge.targetNodeKey)
    }

    /**
     * 案例 2：edge 參考不存在節點 -> ServiceException，且整筆 rollback（DB 無殘留）
     */
    @Test
    fun testEdgeReferToMissingNodeRollback() {
        val keyA = UUID.randomUUID().toString().substring(0, 8)
        val req = WorkflowSaveRequestDTO().apply {
            name = "缺節點測試"
            nodes = listOf(node(keyA, NodeType.TRIGGER))
            edges = listOf(edge(keyA, "missing-node"))
        }

        val ex = assertThrows(ServiceException::class.java) { workflowService.save(req) }
        assertEquals(MessageUtil.get(AppMessage.WORKFLOW_EDGE_NODE_NOT_FOUND, "missing-node"), ex.message)
        // 整筆 rollback：該 user 名下不應有 workflow 殘留
        assertTrue(workflowRepository.findByUserId(TEST_USER).isEmpty(), "失敗後 DB 不應有殘留 workflow")
    }

    /**
     * 案例 3：nodeKey 重複 -> WORKFLOW_NODE_KEY_DUPLICATED
     */
    @Test
    fun testDuplicatedNodeKey() {
        val dupKey = UUID.randomUUID().toString().substring(0, 8)
        val req = WorkflowSaveRequestDTO().apply {
            name = "重複 key 測試"
            nodes = listOf(
                node(dupKey, NodeType.TRIGGER),
                node(dupKey, NodeType.TOOL)
            )
            edges = emptyList()
        }

        val ex = assertThrows(ServiceException::class.java) { workflowService.save(req) }
        assertEquals(MessageUtil.get(AppMessage.WORKFLOW_NODE_KEY_DUPLICATED, dupKey), ex.message)
    }

    /**
     * 案例 4：圖含非法環 -> WORKFLOW_GRAPH_HAS_CYCLE
     */
    @Test
    fun testGraphHasCycle() {
        val a = UUID.randomUUID().toString().substring(0, 8)
        val b = UUID.randomUUID().toString().substring(0, 8)
        val req = WorkflowSaveRequestDTO().apply {
            name = "環測試"
            nodes = listOf(node(a, NodeType.TRIGGER), node(b, NodeType.TOOL))
            edges = listOf(edge(a, b), edge(b, a))
        }

        val ex = assertThrows(ServiceException::class.java) { workflowService.save(req) }
        assertEquals(MessageUtil.get(AppMessage.WORKFLOW_GRAPH_HAS_CYCLE), ex.message)
    }

    /**
     * 案例 5：更新時 version 不符 -> WORKFLOW_VERSION_CONFLICT
     */
    @Test
    fun testVersionConflict() {
        val created = workflowService.create("版本衝突測試", null)
        created.id?.let { touchedWorkflowIds.add(it) }

        val req = WorkflowSaveRequestDTO().apply {
            id = created.id
            version = 999 // 與 DB 不符
            name = "更新"
            nodes = emptyList()
            edges = emptyList()
        }

        val ex = assertThrows(ServiceException::class.java) { workflowService.save(req) }
        assertEquals(MessageUtil.get(AppMessage.WORKFLOW_VERSION_CONFLICT), ex.message)
    }

    /**
     * 案例 6：switchStatus(active=true) 但無 TRIGGER 節點 -> WORKFLOW_TRIGGER_NODE_REQUIRED
     */
    @Test
    fun testSwitchStatusRequiresTrigger() {
        val keyA = UUID.randomUUID().toString().substring(0, 8)
        val req = WorkflowSaveRequestDTO().apply {
            name = "無觸發節點測試"
            nodes = listOf(node(keyA, NodeType.TOOL))
            edges = emptyList()
        }
        val saved = workflowService.save(req)
        saved.id?.let { touchedWorkflowIds.add(it) }

        val ex = assertThrows(ServiceException::class.java) { workflowService.switchStatus(saved.id!!, true) }
        assertEquals(MessageUtil.get(AppMessage.WORKFLOW_TRIGGER_NODE_REQUIRED), ex.message)
    }

    /**
     * 案例 7：非擁有者讀取他人 workflow -> WORKFLOW_FORBIDDEN
     */
    @Test
    fun testGetOthersWorkflowForbidden() {
        // 直接以另一位 user 建立 workflow
        val other = workflowRepository.saveOrUpdate(WorkflowEntity().apply {
            userId = OTHER_USER
            name = "他人的工作流"
        })
        other.id?.let { touchedWorkflowIds.add(it) }

        val ex = assertThrows(ServiceException::class.java) { workflowService.get(other.id!!) }
        assertEquals(MessageUtil.get(AppMessage.WORKFLOW_FORBIDDEN), ex.message)
    }

    /**
     * 案例 8：save 拒絕含未知欄位的節點 config（DRAFT 亦驗型別），訊息須指出節點 key
     */
    @Test
    fun testSaveRejectsUnknownConfigField() {
        val req = WorkflowSaveRequestDTO().apply {
            name = "config-驗證-未知欄位"
            nodes = listOf(node("n1", NodeType.LLM_ASSISTANT) {
                put("llmId", "llm-1")
                put("bogusField", "x")
            })
            edges = emptyList()
        }

        val ex = assertThrows(ServiceException::class.java) { workflowService.save(req) }
        assertTrue(ex.message!!.contains("n1"), "錯誤訊息應指出節點 key：${ex.message}")
    }

    /**
     * 案例 9：save 允許必填缺席的草稿 config（必填檢核延後至啟用時）
     */
    @Test
    fun testSaveAllowsDraftConfigWithMissingRequired() {
        val req = WorkflowSaveRequestDTO().apply {
            name = "config-驗證-草稿"
            nodes = listOf(node("n1", NodeType.LLM_ASSISTANT) { put("systemPrompt", "hi") })
            edges = emptyList()
        }

        val dto = workflowService.save(req) // 不應拋例外
        touchedWorkflowIds.add(dto.id!!)
    }

    /**
     * 案例 10：switchStatus 啟用時擋下缺必填欄位的節點，訊息須指出節點 key 與欄位名
     */
    @Test
    fun testSwitchStatusRejectsMissingRequiredConfig() {
        val req = WorkflowSaveRequestDTO().apply {
            name = "config-驗證-啟用"
            nodes = listOf(
                node("t1", NodeType.TRIGGER) { put("triggerType", "MANUAL") },
                node("n1", NodeType.LLM_ASSISTANT) { put("systemPrompt", "hi") } // 缺 llmId
            )
            edges = listOf(edge("t1", "n1"))
        }
        val dto = workflowService.save(req)
        touchedWorkflowIds.add(dto.id!!)

        val ex = assertThrows(ServiceException::class.java) { workflowService.switchStatus(dto.id!!, true) }
        assertTrue(ex.message!!.contains("n1"), "錯誤訊息應指出節點 key：${ex.message}")
        assertTrue(ex.message!!.contains("llmId"), "錯誤訊息應指出缺少的欄位：${ex.message}")
    }

    /**
     * 案例 11：switchStatus 啟用時所有節點 config 完整 -> 成功轉為 ACTIVE
     */
    @Test
    fun testSwitchStatusSucceedsWithCompleteConfigs() {
        val req = WorkflowSaveRequestDTO().apply {
            name = "config-驗證-啟用成功"
            nodes = listOf(
                node("t1", NodeType.TRIGGER) { put("triggerType", "MANUAL") },
                node("n1", NodeType.LLM_ASSISTANT) { put("llmId", "llm-1") }
            )
            edges = listOf(edge("t1", "n1"))
        }
        val dto = workflowService.save(req)
        touchedWorkflowIds.add(dto.id!!)

        val activated = workflowService.switchStatus(dto.id!!, true)
        assertEquals(WorkflowStatus.ACTIVE, activated.status)
    }
}
