package tw.zipe.bastpartner.entity

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * @author Gary
 * @created 2024/10/25
 */
@Entity
@Table(name = "llm_user_role")
data class LLMUserRole(
    @EmbeddedId
    var id: LLMUserRoleId = LLMUserRoleId()  // 提供預設值
)
