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
     * 設定值欄位名稱, 以逗號分隔
     */
    @Column(name = "setting_fields", nullable = true)
    var settingFields: String? = null

}
