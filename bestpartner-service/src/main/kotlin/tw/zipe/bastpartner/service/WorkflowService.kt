package tw.zipe.bastpartner.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.security.identity.SecurityIdentity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.eclipse.microprofile.config.inject.ConfigProperty
import tw.zipe.bastpartner.config.security.SecurityValidator
import tw.zipe.bastpartner.dto.WorkflowDTO
import tw.zipe.bastpartner.dto.WorkflowEdgeDTO
import tw.zipe.bastpartner.dto.WorkflowNodeDTO
import tw.zipe.bastpartner.dto.WorkflowSaveRequestDTO
import tw.zipe.bastpartner.dto.WorkflowSummaryDTO
import tw.zipe.bastpartner.dto.workflow.config.NodeConfigRegistry
import tw.zipe.bastpartner.entity.WorkflowEdgeEntity
import tw.zipe.bastpartner.entity.WorkflowEntity
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.enumerate.WorkflowStatus
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.repository.WorkflowEdgeRepository
import tw.zipe.bastpartner.repository.WorkflowNodeRepository
import tw.zipe.bastpartner.repository.WorkflowRepository

/**
 * Workflow 定義之 CRUD 與畫布驗證服務。
 *
 * Phase 1 範圍：僅處理 workflow 主檔與其 node / edge 子集，不涉及執行（execution）與
 * 觸發器（trigger）—— 該兩者於 Phase 2 / Phase 4 實作。
 *
 * JSON 型別橋接：entity 端 config / canvasMeta / condition 為 [Map]，DTO 端為 kotlinx
 * [JsonObject]，兩者經 Jackson 與 kotlinx Json 以「JSON 字串」互轉，可無損承載任意巢狀 JSON。
 *
 * @author Gary
 * @created 2026/6/29
 */
@ApplicationScoped
class WorkflowService(
    private val workflowRepository: WorkflowRepository,
    private val workflowNodeRepository: WorkflowNodeRepository,
    private val workflowEdgeRepository: WorkflowEdgeRepository,
    private val securityValidator: SecurityValidator,
    private val securityIdentity: SecurityIdentity,
    @ConfigProperty(name = "workflow.max-nodes", defaultValue = "100") private val maxNodes: Int
) {

    private val objectMapper = ObjectMapper()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 建立新的空白 workflow（狀態 DRAFT、版本 1）。
     */
    @Transactional
    fun create(name: String, description: String?): WorkflowDTO {
        val userId = securityValidator.validateLoggedInUser()
        val entity = WorkflowEntity().apply {
            this.userId = userId
            this.name = name
            this.description = description
            status = WorkflowStatus.DRAFT
            version = 1
        }
        workflowRepository.saveOrUpdate(entity)
        return toDTO(entity, emptyList(), emptyList())
    }

    /**
     * 新增或整張覆寫 workflow（含 nodes / edges）。
     *
     * 流程：① 畫布驗證與節點 config 型別驗證 → ② 既有則檢核擁有權與樂觀鎖 → ③ 清舊 node/edge 後寫入新集合
     * → ④ version++ 後更新。全程 [Transactional]，任一步失敗整筆 rollback。
     */
    @Transactional
    fun save(req: WorkflowSaveRequestDTO): WorkflowDTO {
        val userId = securityValidator.validateLoggedInUser()
        validateGraph(req.nodes, req.edges)
        validateNodeConfigs(req.nodes)

        val entity: WorkflowEntity
        if (!req.id.isNullOrEmpty()) {
            entity = workflowRepository.findOptionalById(req.id!!)
                ?: throw ServiceException(AppMessage.WORKFLOW_NOT_FOUND)
            checkAccess(entity)
            if (req.version != null && entity.version != req.version) {
                throw ServiceException(AppMessage.WORKFLOW_VERSION_CONFLICT)
            }
            entity.name = req.name
            entity.description = req.description
            entity.canvasMeta = req.canvasMeta?.let { jsonObjectToMap(it) }
            workflowNodeRepository.deleteByWorkflowId(entity.id!!)
            workflowEdgeRepository.deleteByWorkflowId(entity.id!!)
            entity.version = entity.version + 1
            workflowRepository.update(entity)
        } else {
            entity = WorkflowEntity().apply {
                this.userId = userId
                name = req.name
                description = req.description
                status = WorkflowStatus.DRAFT
                version = 1
                canvasMeta = req.canvasMeta?.let { jsonObjectToMap(it) }
            }
            workflowRepository.saveOrUpdate(entity)
        }

        val workflowId = entity.id!!
        req.nodes.forEach { n ->
            workflowNodeRepository.saveOrUpdate(WorkflowNodeEntity().apply {
                this.workflowId = workflowId
                nodeKey = n.nodeKey
                type = n.type ?: throw ServiceException(AppMessage.WORKFLOW_NODE_KEY_DUPLICATED, n.nodeKey)
                name = n.name
                positionX = n.positionX
                positionY = n.positionY
                config = jsonObjectToMap(n.config)
            })
        }
        req.edges.forEach { e ->
            workflowEdgeRepository.saveOrUpdate(WorkflowEdgeEntity().apply {
                this.workflowId = workflowId
                sourceNodeKey = e.sourceNodeKey
                targetNodeKey = e.targetNodeKey
                sourceHandle = e.sourceHandle
                targetHandle = e.targetHandle
                label = e.label
                condition = e.condition?.let { jsonObjectToMap(it) }
            })
        }
        return get(workflowId)
    }

    /**
     * 取得單一 workflow 完整定義（含 nodes / edges）。
     */
    fun get(id: String): WorkflowDTO {
        val entity = workflowRepository.findOptionalById(id)
            ?: throw ServiceException(AppMessage.WORKFLOW_NOT_FOUND)
        checkAccess(entity)
        val nodes = workflowNodeRepository.findByWorkflowId(id)
        val edges = workflowEdgeRepository.findByWorkflowId(id)
        return toDTO(entity, nodes, edges)
    }

    /**
     * 列出當前登入者擁有的 workflow 摘要。
     */
    fun list(): List<WorkflowSummaryDTO> {
        val userId = securityValidator.validateLoggedInUser()
        return workflowRepository.findByUserId(userId).map { e ->
            WorkflowSummaryDTO().apply {
                id = e.id
                name = e.name
                status = e.status
                version = e.version
                updatedAt = e.updatedAt?.toString()
            }
        }
    }

    /**
     * 僅更新 workflow meta（name / description / canvasMeta），不動 node / edge。
     */
    @Transactional
    fun updateMeta(id: String, name: String?, description: String?, canvasMeta: JsonObject?): WorkflowDTO {
        val entity = workflowRepository.findOptionalById(id)
            ?: throw ServiceException(AppMessage.WORKFLOW_NOT_FOUND)
        checkAccess(entity)
        name?.let { entity.name = it }
        description?.let { entity.description = it }
        canvasMeta?.let { entity.canvasMeta = jsonObjectToMap(it) }
        workflowRepository.update(entity)
        return get(id)
    }

    /**
     * 刪除 workflow，連鎖刪除其 node / edge。
     *
     * Phase 1 僅處理 node / edge / workflow；execution / trigger 連鎖刪除待 Phase 2 / 4。
     */
    @Transactional
    fun delete(id: String) {
        val entity = workflowRepository.findOptionalById(id)
            ?: throw ServiceException(AppMessage.WORKFLOW_NOT_FOUND)
        checkAccess(entity)
        // TODO(Phase 2): 若存在 RUNNING execution 應拋 WORKFLOW_DELETE_WHILE_RUNNING（execution 表尚未建立）
        workflowNodeRepository.deleteByWorkflowId(id)
        workflowEdgeRepository.deleteByWorkflowId(id)
        workflowRepository.deleteById(id)
    }

    /**
     * 啟用 / 停用 workflow。啟用前須具備 Trigger 節點、圖無環，且各節點 config 型別合法、必填欄位齊備。
     */
    @Transactional
    fun switchStatus(id: String, active: Boolean): WorkflowDTO {
        val entity = workflowRepository.findOptionalById(id)
            ?: throw ServiceException(AppMessage.WORKFLOW_NOT_FOUND)
        checkAccess(entity)
        if (active) {
            val nodes = workflowNodeRepository.findByWorkflowId(id)
            if (nodes.none { it.type == NodeType.TRIGGER }) {
                throw ServiceException(AppMessage.WORKFLOW_TRIGGER_NODE_REQUIRED)
            }
            val edges = workflowEdgeRepository.findByWorkflowId(id)
            if (detectCycle(nodes.map { it.nodeKey }, edges.map { it.sourceNodeKey to it.targetNodeKey })) {
                throw ServiceException(AppMessage.WORKFLOW_GRAPH_HAS_CYCLE)
            }
            // 啟用前逐節點驗證 config：型別須合法且必填欄位不得缺席
            nodes.forEach { n ->
                val configObj = mapToJsonObject(n.config) ?: JsonObject(emptyMap())
                val parsed = runCatching { NodeConfigRegistry.parse(n.type, configObj) }
                    .getOrElse { e ->
                        throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_INVALID, n.nodeKey, e.message.orEmpty())
                    }
                val missing = parsed.missingRequiredFields()
                if (missing.isNotEmpty()) {
                    throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_REQUIRED_MISSING, n.nodeKey, missing.joinToString(", "))
                }
            }
        }
        entity.status = if (active) WorkflowStatus.ACTIVE else WorkflowStatus.INACTIVE
        workflowRepository.update(entity)
        return get(id)
    }

    /**
     * 畫布驗證：節點數上限、nodeKey 唯一、edge 端點存在、無非法環。
     */
    private fun validateGraph(nodes: List<WorkflowNodeDTO>, edges: List<WorkflowEdgeDTO>) {
        if (nodes.size > maxNodes) {
            throw ServiceException(AppMessage.WORKFLOW_NODE_LIMIT_EXCEEDED, maxNodes)
        }
        val keys = HashSet<String>()
        nodes.forEach { n ->
            if (!keys.add(n.nodeKey)) {
                throw ServiceException(AppMessage.WORKFLOW_NODE_KEY_DUPLICATED, n.nodeKey)
            }
        }
        edges.forEach { e ->
            if (!keys.contains(e.sourceNodeKey)) {
                throw ServiceException(AppMessage.WORKFLOW_EDGE_NODE_NOT_FOUND, e.sourceNodeKey)
            }
            if (!keys.contains(e.targetNodeKey)) {
                throw ServiceException(AppMessage.WORKFLOW_EDGE_NODE_NOT_FOUND, e.targetNodeKey)
            }
        }
        if (detectCycle(nodes.map { it.nodeKey }, edges.map { it.sourceNodeKey to it.targetNodeKey })) {
            throw ServiceException(AppMessage.WORKFLOW_GRAPH_HAS_CYCLE)
        }
    }

    /**
     * 節點 config 型別驗證（DRAFT 亦執行）：依 NodeType 嚴格反序列化，
     * 結構性型別錯誤或未知欄位一律拒絕；必填缺席放行（啟用時才驗，見 [switchStatus]）。
     */
    private fun validateNodeConfigs(nodes: List<WorkflowNodeDTO>) {
        nodes.forEach { n ->
            val type = n.type ?: return@forEach // type 缺席由既有 save 流程處理
            runCatching { NodeConfigRegistry.parse(type, n.config) }
                .onFailure { e ->
                    throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_INVALID, n.nodeKey, e.message.orEmpty())
                }
        }
    }

    /**
     * 以 Kahn 拓樸排序偵測有向環：若可排序節點數 < 總節點數，代表存在環。
     */
    private fun detectCycle(nodeKeys: Collection<String>, edges: List<Pair<String, String>>): Boolean {
        val indegree = HashMap<String, Int>()
        nodeKeys.forEach { indegree[it] = 0 }
        val adj = HashMap<String, MutableList<String>>()
        edges.forEach { (source, target) ->
            adj.getOrPut(source) { mutableListOf() }.add(target)
            indegree[target] = (indegree[target] ?: 0) + 1
        }
        val queue = ArrayDeque<String>()
        indegree.filterValues { it == 0 }.keys.forEach { queue.add(it) }
        var visited = 0
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            visited++
            adj[current]?.forEach { next ->
                val deg = (indegree[next] ?: 0) - 1
                indegree[next] = deg
                if (deg == 0) queue.add(next)
            }
        }
        return visited != nodeKeys.size
    }

    /**
     * 擁有權檢核：非擁有者且非 admin 角色則拒絕。
     */
    private fun checkAccess(entity: WorkflowEntity) {
        val userId = securityValidator.validateLoggedInUser()
        if (entity.userId != userId && !securityIdentity.roles.contains("admin")) {
            throw ServiceException(AppMessage.WORKFLOW_FORBIDDEN)
        }
    }

    private fun toDTO(
        entity: WorkflowEntity,
        nodes: List<WorkflowNodeEntity>,
        edges: List<WorkflowEdgeEntity>
    ): WorkflowDTO = WorkflowDTO().apply {
        id = entity.id
        name = entity.name
        description = entity.description
        status = entity.status
        version = entity.version
        canvasMeta = mapToJsonObject(entity.canvasMeta)
        this.nodes = nodes.map { n ->
            WorkflowNodeDTO().apply {
                nodeKey = n.nodeKey
                type = n.type
                name = n.name
                positionX = n.positionX
                positionY = n.positionY
                config = mapToJsonObject(n.config) ?: JsonObject(emptyMap())
            }
        }
        this.edges = edges.map { e ->
            WorkflowEdgeDTO().apply {
                sourceNodeKey = e.sourceNodeKey
                targetNodeKey = e.targetNodeKey
                sourceHandle = e.sourceHandle
                targetHandle = e.targetHandle
                label = e.label
                condition = mapToJsonObject(e.condition)
            }
        }
    }

    /**
     * Map -> JsonObject（經 Jackson 序列化為字串後以 kotlinx 解析）。
     */
    private fun mapToJsonObject(map: Map<String, Any?>?): JsonObject? {
        if (map == null) return null
        return json.parseToJsonElement(objectMapper.writeValueAsString(map)).jsonObject
    }

    /**
     * JsonObject -> Map（經 kotlinx 序列化為字串後以 Jackson 解析）。
     */
    @Suppress("UNCHECKED_CAST")
    private fun jsonObjectToMap(obj: JsonObject): Map<String, Any?> {
        val str = json.encodeToString(JsonObject.serializer(), obj)
        return objectMapper.readValue(str, Map::class.java) as Map<String, Any?>
    }
}
