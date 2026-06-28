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
import tw.zipe.bastpartner.enumerate.WorkflowStatus

/**
 * Workflow 定義
 *
 * @author Gary
 * @created 2026/6/28
 */
@Entity
@Table(name = "llm_workflow")
class WorkflowEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    /**
     * 擁有者用戶 ID
     */
    @Column(name = "user_id", nullable = false)
    var userId: String = StringUtil.EMPTY_STRING

    /**
     * 名稱
     */
    @Column(name = "name", nullable = false)
    var name: String = StringUtil.EMPTY_STRING

    /**
     * 描述
     */
    @Column(name = "description")
    var description: String? = null

    /**
     * 狀態
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: WorkflowStatus = WorkflowStatus.DRAFT

    /**
     * 版本
     */
    @Column(name = "version")
    var version: Int = 1

    /**
     * 畫布中繼資料
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canvas_meta", columnDefinition = "json")
    var canvasMeta: Map<String, Any?>? = null

}
