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
 * @created 2025/3/19
 */
@Entity
@Table(name = "llm_mcp_server")
class LLMMcpServerEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    @Column(name = "name", nullable = false)
    var name: String = StringUtil.EMPTY_STRING

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "command_setting", columnDefinition = "json", nullable = false)
    var commandSetting: McpCommandSetting = McpCommandSetting()

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var type: McpType

}
