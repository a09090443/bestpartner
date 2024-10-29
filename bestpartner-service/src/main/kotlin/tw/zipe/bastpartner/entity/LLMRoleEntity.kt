package tw.zipe.bastpartner.entity

import io.netty.util.internal.StringUtil
import io.quarkus.security.jpa.Roles
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import tw.zipe.bastpartner.converter.PermissionSetConverter
import tw.zipe.bastpartner.enumerate.Permission

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
