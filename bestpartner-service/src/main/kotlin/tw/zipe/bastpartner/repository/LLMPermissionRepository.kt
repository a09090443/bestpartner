package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
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
        val paramMap = mapOf("id" to id, "status" to status.ordinal)

        val sql = """
            SELECT lp.num, lp.name
            FROM llm_user lm
                     JOIN llm_user_role lur ON lm.id = lur.user_id
                     JOIN llm_role_permission lrp ON lur.ROLE_NUM = lrp.ROLE_NUM
                     JOIN llm_permission lp ON lrp.PERMISSION_NUM = lp.NUM
            WHERE lm.id = :id AND lm.status = :status
            ORDER BY lm.created_at DESC
        """.trimIndent()
        return this.executeSelect(sql, paramMap, PermissionDTO::class.java)
    }

    fun updatePermission(id: String, name: String, num: Int, description: String): Int {
        val paramMap = initParamsMap("id" to id, "name" to name, "num" to num, "description" to description)
        val sql = """
            UPDATE llm_permission lp
            SET lp.name = :name, lp.num = :num, lp.description = :description, lp.updated_at = :updatedAt, lp.updated_by = :updatedBy
            WHERE lp.id = :id
        """.trimIndent()
        val executor = createSqlExecutor()
            .withSql(sql)
            .withParamMap(paramMap)
        return executeUpdateWithTransaction(executor)
    }
}
