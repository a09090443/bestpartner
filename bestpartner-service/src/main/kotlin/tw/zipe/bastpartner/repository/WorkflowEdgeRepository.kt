package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.entity.WorkflowEdgeEntity

/**
 * Workflow 連線（邊）資料存取
 *
 * @author Gary
 * @created 2026/6/28
 */
@ApplicationScoped
class WorkflowEdgeRepository : BaseRepository<WorkflowEdgeEntity, String>() {

    /**
     * 依所屬 workflowId 查詢連線清單
     */
    fun findByWorkflowId(workflowId: String): List<WorkflowEdgeEntity> = list("workflowId = ?1", workflowId)

    /**
     * 依所屬 workflowId 刪除全部連線
     */
    @Transactional
    fun deleteByWorkflowId(workflowId: String): Long = delete("workflowId = ?1", workflowId)
}
