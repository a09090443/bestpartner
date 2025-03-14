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
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.model.LLModel

/**
 * @author Gary
 * @created 2024/10/12
 */
@Entity
@Table(name = "llm_setting")
class LLMSettingEntity : BaseEntity() {
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
     * LLM 平台ID
     */
    @Column(name = "platform_id", nullable = false)
    var platformId: String = StringUtil.EMPTY_STRING

    /**
     * 類型
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    var type: ModelType? = null

    /**
     * 自定義別名
     */
    @Column(name = "alias", nullable = true)
    var alias: String = StringUtil.EMPTY_STRING

    /**
     * 模型設定
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_setting", columnDefinition = "json", nullable = true)
    var modelSetting: LLModel = LLModel()

}
