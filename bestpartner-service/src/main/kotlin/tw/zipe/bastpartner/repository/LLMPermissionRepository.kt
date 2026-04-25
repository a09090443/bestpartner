package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.dto.PermissionDTO
import tw.zipe.bastpartner.entity.LLMPermissionEntity
import tw.zipe.bastpartner.enumerate.UserStatus

/**
 * @author Gary
 * @created 2024/12/18
 */
@ApplicationScoped
class LLMPermissionRepository : BaseRepository<LLMPermissionEntity, String>() {

    fun findUserPermissionByStatus(id: String, status: UserStatus): List<PermissionDTO> {
        val jpql = """
            SELECT lp.num AS num, lp.name AS name
            FROM LLMUserEntity lu
            JOIN LLMUserRoleEntity lur ON lur.id.userId = lu.id
            JOIN LLMRolePermissionEntity lrp ON lrp.id.roleNum = lur.id.roleNum
            JOIN LLMPermissionEntity lp ON lp.num = lrp.id.permissionNum
            WHERE lu.id = :id AND lu.status = :status
            ORDER BY lu.createdAt DESC
        """.trimIndent()
        val paramMap = mapOf("id" to id, "status" to status.ordinal.toString())
        return executeJpqlSelect(jpql, paramMap, PermissionDTO::class.java)
    }

    @Transactional
    fun updatePermission(id: String, name: String, num: Int, description: String): Int {
        val entity = findById(id) ?: return 0
        entity.name = name
        entity.num = num
        entity.description = description
        // @PreUpdate 自動設定 updatedAt / updatedBy
        update(entity)
        return 1
    }
}
