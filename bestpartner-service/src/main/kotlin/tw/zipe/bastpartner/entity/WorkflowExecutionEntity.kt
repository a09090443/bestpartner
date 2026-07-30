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

    /**
     * 本次執行由哪個 TRIGGER 節點發起；null = 未指定觸發點（所有 TRIGGER 皆執行）。
     *
     * 與 [triggerId] 不同：[triggerId] 指向 llm_workflow_trigger.id（webhook / cron 的觸發器設定），
     * 本欄位存的是 llm_workflow_node.node_key。兩者刻意分開，避免 webhook / cron 上線後語義撞名。
     */
    @Column(name = "trigger_node_key")
    var triggerNodeKey: String? = null

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
