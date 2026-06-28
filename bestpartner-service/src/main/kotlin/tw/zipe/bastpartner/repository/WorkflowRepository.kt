package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.WorkflowEntity

/**
 * Workflow 定義資料存取
 *
 * @author Gary
 * @created 2026/6/28
 */
@ApplicationScoped
class WorkflowRepository : BaseRepository<WorkflowEntity, String>() {

    /**
     * 依擁有者 userId 查詢 workflow 清單
     */
    fun findByUserId(userId: String): List<WorkflowEntity> = list("userId = ?1", userId)
}
