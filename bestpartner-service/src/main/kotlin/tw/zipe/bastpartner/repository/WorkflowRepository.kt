package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.entity.WorkflowEntity

/**
 * Workflow 定義資料存取
 *
 * 注意：WorkflowEntity.canvasMeta 以 SqlTypes.JSON 儲存，JSON round-trip 後數值回為
 * Integer、巢狀物件回為 LinkedHashMap，service 層取值勿假設為 Int/Double 或特定 Map 實作。
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

    /**
     * 依擁有者 userId 刪除其所有 workflow
     */
    @Transactional
    fun deleteByUserId(userId: String): Long = delete("userId = ?1", userId)
}
