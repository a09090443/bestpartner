package tw.zipe.bastpartner.entity

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * 角色與權限關聯表（llm_role_permission）
 * 用於 JPQL 多表 JOIN 查詢
 * @author Gary
 */
@Entity
@Table(name = "llm_role_permission")
data class LLMRolePermissionEntity(
    @EmbeddedId
    var id: LLMRolePermissionId = LLMRolePermissionId()
)
