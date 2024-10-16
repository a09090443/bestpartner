package tw.zipe.basepartner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * @author Gary
 * @created 2024/10/14
 */
@Entity
@Table(name = "llm_tool")
class LLMToolEntity {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    var id: String? = null

    /**
     * 工具名稱
     */
    @Column(name = "name", nullable = false)
    var name: String? = null

    /**
     * Class path
     */
    @Column(name = "class_path",nullable = true)
    var classPath: String? = null

}
