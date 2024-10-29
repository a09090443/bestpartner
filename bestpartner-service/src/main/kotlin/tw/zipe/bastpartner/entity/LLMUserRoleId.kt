package tw.zipe.bastpartner.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * @author Gary
 * @created 2024/10/25
 */
@Embeddable
data class LLMUserRoleId(
    @Column(name = "user_id", nullable = false)
    var userId: String = "",

    @Column(name = "role_num", nullable = false)
    var roleNum: Int = 0
)
