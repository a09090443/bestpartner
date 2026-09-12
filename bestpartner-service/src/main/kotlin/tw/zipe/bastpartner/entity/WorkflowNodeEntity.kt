package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tw.zipe.bastpartner.enumerate.NodeType

/**
 * Workflow 節點
 *
 * @author Gary
 * @created 2026/6/28
 */
@Entity
@Table(name = "llm_workflow_node")
class WorkflowNodeEntity : BaseEntity() {
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
     * 節點識別鍵（workflow 內唯一）
     */
    @Column(name = "node_key", nullable = false)
    var nodeKey: String = StringUtil.EMPTY_STRING

    /**
     * 節點類型
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var type: NodeType

    /**
     * 名稱
     */
    @Column(name = "name")
    var name: String? = null

    /**
     * 畫布座標 X
     */
    @Column(name = "position_x", nullable = false)
    var positionX: Double = 0.0

    /**
     * 畫布座標 Y
     */
    @Column(name = "position_y", nullable = false)
    var positionY: Double = 0.0

    /**
     * 節點設定
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "json", nullable = false)
    var config: Map<String, Any?> = emptyMap()

}
