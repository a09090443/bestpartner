package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import tw.zipe.bastpartner.converter.McpSettingEncryptConverter

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
     * 設定內容（整包 AES-GCM 加密，內含 MCP token/key 等機密；儲存為密文文字，非 JSON）
     */
    @Convert(converter = McpSettingEncryptConverter::class)
    @Column(name = "setting_content", columnDefinition = "text", nullable = false)
    var settingContent: Map<String, String> = mapOf()

}
