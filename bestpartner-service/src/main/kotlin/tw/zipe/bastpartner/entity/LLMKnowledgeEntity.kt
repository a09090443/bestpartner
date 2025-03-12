package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * @author Gary
 * @created 2025/3/12
 */
@Entity
@Table(name = "llm_knowledge")
class LLMKnowledgeEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    var id: String? = null

    /**
     * 使用者ID
     */
    @Column(name = "user_id", nullable = false)
    var userId: String = StringUtil.EMPTY_STRING

    /**
     * 向量資料庫ID
     */
    @Column(name = "vector_store_id", nullable = false)
    var vectorStoreId: String = StringUtil.EMPTY_STRING

    /**
     * 向量模型ID
     */
    @Column(name = "llm_embedding_id", nullable = false)
    var llmEmbeddingId: String = StringUtil.EMPTY_STRING

    /**
     * 名稱
     */
    @Column(name = "name", nullable = false)
    var name: String = StringUtil.EMPTY_STRING

    /**
     * 描述
     */
    @Column(name = "description", nullable = true)
    var description: String = StringUtil.EMPTY_STRING

}
