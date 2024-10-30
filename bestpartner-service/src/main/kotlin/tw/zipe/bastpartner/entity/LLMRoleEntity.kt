package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * @author Gary
 * @created 2024/10/25
 */
@Entity
@Table(name = "llm_role")
class LLMRoleEntity : BaseEntity() {

    /**
     * 角色名稱
     */
    @Id
    @Column(name = "name", nullable = false)
    var name: String = StringUtil.EMPTY_STRING

    /**
     * 角色名稱
     */
    @Column(name = "num", nullable = false)
    var num: Int? = null

    /**
     * 密碼
     */
    @Column(name = "desription", nullable = true)
    var desription: String = StringUtil.EMPTY_STRING

}
