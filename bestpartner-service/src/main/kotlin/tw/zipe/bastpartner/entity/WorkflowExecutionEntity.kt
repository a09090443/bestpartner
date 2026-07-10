package tw.zipe.bastpartner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tw.zipe.bastpartner.enumerate.ExecutionStatus
import tw.zipe.bastpartner.enumerate.TriggerType

/**
 * Workflow 單次執行主檔，對應資料表 llm_workflow_execution。
 */
@Entity
@Table(name = "llm_workflow_execution")
class WorkflowExecutionEntity : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    @Column(name = "workflow_id", nullable = false)
    var workflowId: String = ""

    @Column(name = "workflow_version", nullable = false)
    var workflowVersion: Int = 0

    @Column(name = "trigger_id")
    var triggerId: String? = null

    @Column(name = "trigger_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var triggerType: TriggerType = TriggerType.MANUAL

    @Column(name = "triggered_by", nullable = false)
    var triggeredBy: String = ""

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: ExecutionStatus = ExecutionStatus.PENDING

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_payload", columnDefinition = "json")
    var inputPayload: Map<String, Any?>? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_result", columnDefinition = "json")
    var outputResult: Map<String, Any?>? = null

    @Column(name = "error_node_key")
    var errorNodeKey: String? = null

    @Column(name = "error_message")
    var errorMessage: String? = null

    @Column(name = "started_at", nullable = false)
    var startedAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null

    @Column(name = "duration_ms")
    var durationMs: Long? = null
}
