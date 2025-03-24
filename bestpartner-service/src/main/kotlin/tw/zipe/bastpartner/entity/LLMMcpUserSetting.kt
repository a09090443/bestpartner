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
import tw.zipe.bastpartner.enumerate.McpType
import tw.zipe.bastpartner.model.McpCommandSetting

/**
 * @author Gary
 * @created 2025/3/22
 */
@Entity
@Table(name = "llm_mcp_user_setting")
class LLMMcpUserSetting : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    /**
     * 別名
     */
    @Column(name = "alias", nullable = false)
    var alias: String = StringUtil.EMPTY_STRING

    /**
     * 使用者 ID
     */
    @Column(name = "user_id", nullable = false)
    var userId: String = StringUtil.EMPTY_STRING

    /**
     * MCP ID
     */
    @Column(name = "mcp_id", nullable = false)
    var mcpId: String = StringUtil.EMPTY_STRING

    /**
     * 設定內容
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "setting_content", columnDefinition = "json", nullable = false)
    var settingContent: Map<String, String> = mapOf()

}
