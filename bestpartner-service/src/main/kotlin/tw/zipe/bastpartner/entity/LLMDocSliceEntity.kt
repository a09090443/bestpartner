package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * @author Gary
 * @created 2025/3/8
 */
@Entity
@Table(name = "llm_doc_slice")
class LLMDocSliceEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    var id: String? = null

    /**
     * 知識庫ID
     */
    @Column(name = "knowledge_id", nullable = false)
    var knowledgeId: String = StringUtil.EMPTY_STRING

    /**
     * 文件ID
     */
    @Column(name = "doc_id", nullable = false)
    var docId: String = StringUtil.EMPTY_STRING

    /**
     * 切片内容
     */
    @Column(name = "content", nullable = false)
    var content: String = StringUtil.EMPTY_STRING

}
