package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import tw.zipe.bastpartner.enumerate.UserStatus

/**
 * @author Gary
 * @created 2024/10/22
 */
@Entity
@Table(name = "llm_user")
class LLMUserEntity : BaseEntity() {
    /**
     * 主鍵
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    /**
     * 使用者
     */
    @Column(name = "username", nullable = false)
    var username: String = StringUtil.EMPTY_STRING

    /**
     * 密碼
     */
    @Column(name = "password", nullable = false)
    var password: String = StringUtil.EMPTY_STRING

    /**
     * 暱稱
     */
    @Column(name = "nickname", nullable = true)
    var nickname: String = StringUtil.EMPTY_STRING

    /**
     * 手機
     */
    @Column(name = "phone", nullable = true)
    var phone: String = StringUtil.EMPTY_STRING

    /**
     * Email
     */
    @Column(name = "email", nullable = true)
    var email: String = StringUtil.EMPTY_STRING

    /**
     * 頭像
     */
    @Column(name = "avatar", nullable = true)
    var avatar: String = StringUtil.EMPTY_STRING

    /**
     * 狀態
     */
    @Column(name = "status", nullable = false)
    var status: String = UserStatus.INACTIVE.ordinal.toString()

}
