package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * @author Gary
 * @created 2024/10/22
 */
@Entity
@Table(name = "llm_doc")
class LLMDocEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    /**
     * 知識庫ID
     */
    @Column(name = "knowledge_id", nullable = false)
    var knowledgeId: String = StringUtil.EMPTY_STRING

    /**
     * 名稱
     */
    @Column(name = "name", nullable = true)
    var name: String = StringUtil.EMPTY_STRING

    /**
     * 類型
     */
    @Column(name = "type", nullable = true)
    var type: String = StringUtil.EMPTY_STRING

    /**
     * 網址
     */
    @Column(name = "url", nullable = true)
    var url: String = StringUtil.EMPTY_STRING

    /**
     * 描述
     */
    @Column(name = "description", nullable = true)
    var description: String = StringUtil.EMPTY_STRING

    /**
     * 文件大小
     */
    @Column(name = "size", nullable = true)
    var size: Long = 0

}
