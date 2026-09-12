package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.WorkflowExecutionEntity

@ApplicationScoped
class WorkflowExecutionRepository : BaseRepository<WorkflowExecutionEntity, String>() {

    fun findByWorkflowId(workflowId: String): List<WorkflowExecutionEntity> =
        find("workflowId = :workflowId order by startedAt desc", mapOf("workflowId" to workflowId)).list()
}
