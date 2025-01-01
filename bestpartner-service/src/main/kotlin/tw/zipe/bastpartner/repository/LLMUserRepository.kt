package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID
import tw.zipe.bastpartner.dto.UserDTO
import tw.zipe.bastpartner.entity.LLMUserEntity

/**
 * @author Gary
 * @created 2024/10/14
 */
@ApplicationScoped
class LLMUserRepository : BaseRepository<LLMUserEntity, String>() {

    fun findUserByEmail(email: String) = find("email", email).firstResult()

    fun findUserByUsername(username: String) = find("username", username).firstResult()

    fun updateUserByNativeSQL(id: String, params: Map<String, Any>): Int {
        val paramsWithoutId = params.filterKeys { it != "id" }
        val setClause = paramsWithoutId.keys.joinToString(", ") { "$it = :$it" }
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

    fun findUserInfo(userId: String): UserDTO? {
        val sql = """
            SELECT lu.id AS id,
                   lu.username AS username,
                   lu.nickname AS nickname,
                   lu.phone AS phone,
                   lu.email AS email,
                   lu.avatar AS avatar,
                   lu.status AS status,
                   lur.role_num AS `role.roleNum`,
                   lr.name AS `role.roleName`
            FROM llm_user lu
                     inner join llm_user_role lur on lu.id = lur.user_id
                     inner join llm_role lr on lur.role_num = lr.num
            WHERE lu.id = :userId
        """.trimIndent()

        return executeSelectOne(sql, mapOf("userId" to userId), UserDTO::class.java)
    }
}
