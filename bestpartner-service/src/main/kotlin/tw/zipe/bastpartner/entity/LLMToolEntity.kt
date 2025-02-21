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
import tw.zipe.bastpartner.enumerate.ToolsType

/**
 * @author Gary
 * @created 2024/10/14
 */
@Entity
@Table(name = "llm_tool")
class LLMToolEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    /**
     * 工具名稱
     */
    @Column(name = "name", nullable = false)
    var name: String = StringUtil.EMPTY_STRING

    /**
     * Class path
     */
    @Column(name = "class_path", nullable = false)
    var classPath: String = StringUtil.EMPTY_STRING

    /**
     * 工具群組表 ID
     */
    @Column(name = "category_id", nullable = false)
    var categoryId: String? = null

    /**
     * 工具類型
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var type: ToolsType

    /**
     * Tool設定物件Path
     */
    @Column(name = "config_object_path", nullable = true)
    var configObjectPath: String? = null

    /**
     * 功能名稱
     */
    @Column(name = "function_name", nullable = true)
    var functionName: String? = null

    /**
     * 功能描述
     */
    @Column(name = "function_description", nullable = true)
    var functionDescription: String? = null

    /**
     * 功能描述
     */
    @Column(name = "function_params", nullable = true)
    var functionParams: String? = null

    /**
     * 工具描述
     */
    @Column(name = "description", nullable = true)
    var description: String? = null

}
