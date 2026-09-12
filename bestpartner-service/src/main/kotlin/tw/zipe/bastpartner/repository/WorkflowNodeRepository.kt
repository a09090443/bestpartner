package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.entity.WorkflowNodeEntity

/**
 * Workflow 節點資料存取
 *
 * 注意：WorkflowNodeEntity.config 以 SqlTypes.JSON 儲存，JSON round-trip 後數值回為
 * Integer、巢狀物件回為 LinkedHashMap，service 層取值勿假設為 Int/Double 或特定 Map 實作。
 *
 * @author Gary
 * @created 2026/6/28
 */
@ApplicationScoped
class WorkflowNodeRepository : BaseRepository<WorkflowNodeEntity, String>() {

    /**
     * 依所屬 workflowId 查詢節點清單
     */
    fun findByWorkflowId(workflowId: String): List<WorkflowNodeEntity> = list("workflowId = ?1", workflowId)

    /**
     * 依所屬 workflowId 刪除全部節點
     */
    @Transactional
    fun deleteByWorkflowId(workflowId: String): Long = delete("workflowId = ?1", workflowId)
}
