package tw.zipe.bastpartner.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.security.identity.SecurityIdentity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.eclipse.microprofile.config.inject.ConfigProperty
import tw.zipe.bastpartner.config.security.SecurityValidator
import tw.zipe.bastpartner.converter.WorkflowSecretConverter
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
    private val workflowSecretConverter: WorkflowSecretConverter,
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
        normalizeLegacyNodeConfigs(req.nodes)
        validateGraph(req.nodes, req.edges)
        validateNodeConfigs(req.nodes)

        val entity: WorkflowEntity
        // nodeKey -> 既有 secretHeaders（密文）。整張覆寫會先清舊 node，故須在刪除前取得，
        // 供前端回傳遮罩值時沿用原密文（見 [WorkflowSecretConverter.SECRET_MASK]）。
        var existingSecrets: Map<String, Map<String, Any?>?> = emptyMap()
        if (!req.id.isNullOrEmpty()) {
            entity = workflowRepository.findOptionalById(req.id!!)
                ?: throw ServiceException(AppMessage.WORKFLOW_NOT_FOUND)
            checkAccess(entity)
            if (req.version != null && entity.version != req.version) {
                throw ServiceException(AppMessage.WORKFLOW_VERSION_CONFLICT)
            }
            // 維持「ACTIVE ⟺ 有效」不變式：對已啟用 workflow 重存時，套用與啟用相同的必填驗證，
            // 避免 re-save 破壞必填欄位卻仍停留在 ACTIVE，使狀態與保證脫鉤。新建（DRAFT）不驗必填。
            if (entity.status == WorkflowStatus.ACTIVE) {
                validateRequiredFields(req.nodes)
            }
            entity.name = req.name
            entity.description = req.description
            entity.canvasMeta = req.canvasMeta?.let { jsonObjectToMap(it) }
            existingSecrets = workflowNodeRepository.findByWorkflowId(entity.id!!)
                .associate { it.nodeKey to workflowSecretConverter.secretHeadersOf(it.config) }
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
            val nodeType = n.type ?: throw ServiceException(AppMessage.WORKFLOW_NODE_TYPE_REQUIRED, n.nodeKey)
            workflowNodeRepository.saveOrUpdate(WorkflowNodeEntity().apply {
                this.workflowId = workflowId
                nodeKey = n.nodeKey
                type = nodeType
                name = n.name
                positionX = n.positionX
                positionY = n.positionY
                config = workflowSecretConverter.encryptForStorage(
                    nodeType,
                    jsonObjectToMap(n.config),
                    existingSecrets[n.nodeKey]
                )
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
            // 啟用前逐節點驗證 config：型別須合法且必填欄位不得缺席（與 save 對 ACTIVE 重存共用底層 [validateNodeRequired]）
            nodes.forEach { n ->
                val configObj = mapToJsonObject(n.config) ?: JsonObject(emptyMap())
                validateNodeRequired(n.nodeKey, n.type, configObj)
            }
        }
        entity.status = if (active) WorkflowStatus.ACTIVE else WorkflowStatus.INACTIVE
        workflowRepository.update(entity)
        return get(id)
    }

    /**
     * 回傳各 NodeType 的「無條件必填欄位清單」，供前端載入時即時驗證節點設定。
     *
     * 事實來源：清單直接由 [NodeConfigRegistry.parse] 搭配空 config 產生——空 config 下所有
     * 必填欄位都會 missing，其 [NodeConfig.missingRequiredFields] 結果即等同「無條件必填欄位」。
     * 因此本清單與後端 save / switchStatus 的必填檢核共用同一份 NodeConfig 契約，前端無須重複硬編。
     *
     * 特例：DATA_TRANSFORM 的 mappings 與 template 為「擇一必填」，契約以複合字樣
     * `"mappings|template"` 表示，此處原樣回傳，交由前端解讀。
     *
     * 純函式，不涉及 DB 與登入狀態。
     *
     * @return NodeType.name 對應其必填欄位清單，涵蓋全部 [NodeType.entries]
     */
    fun getNodeRequiredFields(): Map<String, List<String>> =
        NodeType.entries.associate { type ->
            type.name to NodeConfigRegistry.parse(type, JsonObject(emptyMap())).missingRequiredFields()
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
     * v0.1.7 legacy 相容遷移：舊資料 / 舊前端 / 匯入來源可能於 MCP_SERVER 節點寫入舊鍵
     * `mcpSettingId`，而契約欄位已改為 `userSettingId`。此處就地將舊鍵改名，避免其被
     * [validateNodeConfigs] 當未知欄位以 400 拒絕。前端 emit 時雖已移除舊鍵，但後端仍需相容存量。
     *
     * 規則：僅處理 `mcpSettingId`；當 `userSettingId` 不存在或為空時才把舊值搬入，否則以現值為準、
     * 僅移除舊鍵。non-MCP 節點與不含該鍵的 MCP 節點不動。
     *
     * TODO(未來版本)：存量資料完成遷移後可移除本方法。
     */
    private fun normalizeLegacyNodeConfigs(nodes: List<WorkflowNodeDTO>) {
        nodes.forEach { n ->
            if (n.type != NodeType.MCP_SERVER) return@forEach
            if (!n.config.containsKey("mcpSettingId")) return@forEach
            val map = n.config.toMutableMap()
            val legacy = map.remove("mcpSettingId")
            if (legacy != null && isBlankJson(map["userSettingId"])) {
                map["userSettingId"] = legacy
            }
            n.config = JsonObject(map)
        }
    }

    /**
     * 判斷 JSON 值是否視為「空」：缺席（null）、JsonNull、或空白字串。
     */
    private fun isBlankJson(el: JsonElement?): Boolean {
        if (el == null || el is JsonNull) return true
        return el is JsonPrimitive && el.isString && el.content.isBlank()
    }

    /**
     * 逐節點必填欄位驗證（DTO 版）：save 對 ACTIVE workflow 重存時使用。
     * 與 [switchStatus] 啟用時的必填驗證共用底層 [validateNodeRequired]，避免兩處分叉。
     */
    private fun validateRequiredFields(nodes: List<WorkflowNodeDTO>) {
        nodes.forEach { n ->
            val type = n.type ?: return@forEach // type 缺席由既有 save 流程處理
            validateNodeRequired(n.nodeKey, type, n.config)
        }
    }

    /**
     * 必填驗證底層：parse config（型別錯誤包裝為 INVALID）→ 必填缺席則拋 REQUIRED_MISSING。
     * save（ACTIVE 重存）與 switchStatus（啟用）共用同一份必填邏輯。
     */
    private fun validateNodeRequired(nodeKey: String, type: NodeType, config: JsonObject) {
        val parsed = runCatching { NodeConfigRegistry.parse(type, config) }
            .getOrElse { e ->
                throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_INVALID, nodeKey, e.message.orEmpty())
            }
        val missing = parsed.missingRequiredFields()
        if (missing.isNotEmpty()) {
            throw ServiceException(AppMessage.WORKFLOW_NODE_CONFIG_REQUIRED_MISSING, nodeKey, missing.joinToString(", "))
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
                // secretHeaders 一律遮罩，明文與密文皆不外流；前端原值存回即代表沿用
                config = mapToJsonObject(workflowSecretConverter.maskForResponse(n.type, n.config))
                    ?: JsonObject(emptyMap())
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
     *
     * internal 可見性：供 [tw.zipe.bastpartner.resource.WorkflowResource] 的
     * `/execute` 端點轉換 `inputPayload` 使用，避免在 resource 層重複實作同款轉換。
     */
    @Suppress("UNCHECKED_CAST")
    internal fun jsonObjectToMap(obj: JsonObject): Map<String, Any?> {
        val str = json.encodeToString(JsonObject.serializer(), obj)
        return objectMapper.readValue(str, Map::class.java) as Map<String, Any?>
    }
}
