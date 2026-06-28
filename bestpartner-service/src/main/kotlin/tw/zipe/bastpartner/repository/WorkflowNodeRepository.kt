package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.entity.WorkflowNodeEntity

/**
 * Workflow 節點資料存取
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
