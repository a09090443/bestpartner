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
 * @created 2024/10/25
 */
@Entity
@Table(name = "llm_permission")
data class LLMPermission(

    /**
     * 識別碼
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(name = "name", nullable = false)
    var name: String = StringUtil.EMPTY_STRING,

    @Column(name = "num", nullable = false, unique = true)
    var num: Int? = null,

    @Column(name = "description")
    var description: String? = null
) : BaseEntity()
