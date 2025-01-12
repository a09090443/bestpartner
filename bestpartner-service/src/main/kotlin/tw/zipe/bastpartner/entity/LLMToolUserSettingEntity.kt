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
 * @created 2024/11/27
 */
@Entity
@Table(name = "llm_tool_user_setting")
class LLMToolUserSettingEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    /**
     * 使用者ID
     */
    @Column(name = "user_id", nullable = false)
    var userId: String = StringUtil.EMPTY_STRING

    /**
     * 工具ID
     */
    @Column(name = "tool_id", nullable = false)
    var toolId: String = StringUtil.EMPTY_STRING

    /**
     * 設定值內容
     */
    @Column(name = "setting_content", nullable = true)
    var settingContent: String? = null

}
