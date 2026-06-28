package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * Workflow 連線（邊）
 *
 * @author Gary
 * @created 2026/6/28
 */
@Entity
@Table(name = "llm_workflow_edge")
class WorkflowEdgeEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    /**
     * 所屬 workflow ID
     */
    @Column(name = "workflow_id", nullable = false)
    var workflowId: String = StringUtil.EMPTY_STRING

    /**
     * 來源節點 key
     */
    @Column(name = "source_node_key", nullable = false)
    var sourceNodeKey: String = StringUtil.EMPTY_STRING

    /**
     * 目標節點 key
     */
    @Column(name = "target_node_key", nullable = false)
    var targetNodeKey: String = StringUtil.EMPTY_STRING

    /**
     * 來源連接點
     */
    @Column(name = "source_handle")
    var sourceHandle: String? = null

    /**
     * 目標連接點
     */
    @Column(name = "target_handle")
    var targetHandle: String? = null

    /**
     * 連線標籤
     */
    @Column(name = "label")
    var label: String? = null

    /**
     * 連線條件
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition", columnDefinition = "json")
    var condition: Map<String, Any?>? = null

}
