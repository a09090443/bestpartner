package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.WorkflowNodeExecutionEntity

@ApplicationScoped
class WorkflowNodeExecutionRepository : BaseRepository<WorkflowNodeExecutionEntity, String>() {

    fun findByExecutionId(executionId: String): List<WorkflowNodeExecutionEntity> =
        find("executionId = :executionId order by seqNo", mapOf("executionId" to executionId)).list()
}
