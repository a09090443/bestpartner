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
import tw.zipe.bastpartner.enumerate.NodeExecutionStatus
import tw.zipe.bastpartner.enumerate.NodeType

/**
 * Workflow 單一節點執行紀錄，對應資料表 llm_workflow_node_execution。
 */
@Entity
@Table(name = "llm_workflow_node_execution")
class WorkflowNodeExecutionEntity : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    @Column(name = "execution_id", nullable = false)
    var executionId: String = ""

    @Column(name = "workflow_id", nullable = false)
    var workflowId: String = ""

    @Column(name = "node_key", nullable = false)
    var nodeKey: String = ""

    @Column(name = "node_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var nodeType: NodeType = NodeType.TRIGGER

    @Column(name = "seq_no", nullable = false)
    var seqNo: Int = 0

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: NodeExecutionStatus = NodeExecutionStatus.PENDING

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input", columnDefinition = "json")
    var input: Map<String, Any?>? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output", columnDefinition = "json")
    var output: Map<String, Any?>? = null

    @Column(name = "error_message")
    var errorMessage: String? = null

    @Column(name = "loop_index")
    var loopIndex: Int? = null

    @Column(name = "started_at")
    var startedAt: LocalDateTime? = null

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null

    @Column(name = "duration_ms")
    var durationMs: Long? = null
}
