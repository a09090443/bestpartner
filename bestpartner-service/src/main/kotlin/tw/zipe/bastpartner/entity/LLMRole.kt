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
data class LLMRole(
    @Id
    @Column(name = "name", nullable = false)
    var name: String = StringUtil.EMPTY_STRING,

    @Column(name = "num", nullable = false, unique = true)
    var num: Int? = null,

    @Column(name = "description", nullable = false)
    var description: String = StringUtil.EMPTY_STRING
) : BaseEntity()
