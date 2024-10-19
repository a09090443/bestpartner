package tw.zipe.basepartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * @author Gary
 * @created 2024/10/19
 */
@Entity
@Table(name = "system_setting")
class SystemSettingEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    /**
     * 設定名稱
     */
    @Column(name = "setting_key", nullable = false)
    var settingKey: String = StringUtil.EMPTY_STRING

    /**
     * 設定值
     */
    @Column(name = "setting_value", nullable = true)
    var settingValue: String? = null

    /**
     * 設定說明
     */
    @Column(name = "description", nullable = true)
    var description: String = StringUtil.EMPTY_STRING

}
