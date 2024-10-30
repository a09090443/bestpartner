package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID
import tw.zipe.bastpartner.dto.PermissionDTO
import tw.zipe.bastpartner.dto.UserDTO
import tw.zipe.bastpartner.entity.LLMUserEntity
import tw.zipe.bastpartner.enumerate.UserStatus

/**
 * @author Gary
 * @created 2024/10/14
 */
@ApplicationScoped
class LLMUserRepository : BaseRepository<LLMUserEntity, String>() {

    fun findUserByEmail(email: String) = find("email", email).firstResult()

    fun updateUserByNativeSQL(id: String, params: Map<String, Any>): Int {
        val setClause = params.keys.joinToString(", ") { "$it = :$it" }
        val sql = """
            UPDATE llm_user
            SET $setClause
            WHERE id = :id
        """.trimIndent()
        val executor = createSqlExecutor()
            .withSql(sql)
            .withParamMap(params)
        return executeUpdateWithTransaction(executor)
    }

    fun insertUserByNative(username: String, email: String): Int {
        return createSqlExecutor()
            .withSql(
                """
                INSERT INTO llm_user (id, username, email)
                VALUES (:id, :username, :email)
            """
            )
            .withParams(UUID.randomUUID().toString(), username, email)
            .executeUpdate()
    }

    fun updateUserStatusByNative(id: String, status: UserStatus): Int {
        val paramMap = mapOf("id" to id, "status" to status.ordinal)
        val sql = """
            UPDATE llm_user lm
            SET lm.status = :status
            WHERE lm.id = :id
        """.trimIndent()
        val executor = createSqlExecutor()
            .withSql(sql)
            .withParamMap(paramMap)
        return executeUpdateWithTransaction(executor)
    }

    fun findUserPermissionByStatus(id: String, status: UserStatus): List<PermissionDTO> {
        val paramMap = mapOf("id" to id, "status" to status.ordinal)

        val sql = """
            SELECT lp.num, lp.name
            FROM llm_user lm
                     JOIN llm_user_role lur ON lm.id = lur.user_id
                     JOIN llm_role_permission lrp ON lur.ROLE_NUM = lrp.ROLE_NUM
                     JOIN llm_permission lp ON lrp.PERMISSION_NUM = lp.NUM
            WHERE lm.id = :id AND lm.id = :id AND lm.status = :status
            ORDER BY lm.created_at DESC
        """.trimIndent()
        return this.executeSelect(sql, paramMap, PermissionDTO::class.java)
    }
}
