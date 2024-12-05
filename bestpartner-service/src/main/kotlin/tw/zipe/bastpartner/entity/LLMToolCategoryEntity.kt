package tw.zipe.bastpartner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * @author Gary
 * @created 2024/12/3
 */
@Entity
@Table(name = "llm_tool_category")
class LLMToolCategoryEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    /**
     * 群組名稱
     */
    @Column(name = "name", nullable = false)
    var name: String? = null

    /**
     * 群組描述
     */
    @Column(name = "description", nullable = true)
    var description: String? = null

}
