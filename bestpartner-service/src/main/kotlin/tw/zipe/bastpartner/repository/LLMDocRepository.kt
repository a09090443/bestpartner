package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.PermissionDTO
import tw.zipe.bastpartner.entity.LLMDocEntity
import tw.zipe.bastpartner.enumerate.UserStatus

/**
 * @author Gary
 * @created 2024/10/22
 */
@ApplicationScoped
class LLMDocRepository : BaseRepository<LLMDocEntity, String>() {

    fun findByKnowledgeId(knowledgeId: String): List<LLMDocEntity>? = find("knowledgeId", knowledgeId).list()

    fun findByKnowledgeIdAndName(knowledgeId: String, name: String): LLMDocEntity? {
        val params = mapOf("knowledgeId" to knowledgeId, "name" to name)
        return find("knowledgeId = :knowledgeId AND name = :name", params).firstResult()
    }

    fun findByKnowledgeId(id: String, status: UserStatus): List<PermissionDTO> {
        val paramMap = mapOf("id" to id, "status" to status.ordinal)

        val sql = """
            SELECT lp.num, lp.name
            FROM llm_user lu
                     JOIN llm_user_role lur ON lu.id = lur.user_id
                     JOIN llm_role_permission lrp ON lur.ROLE_NUM = lrp.ROLE_NUM
                     JOIN llm_permission lp ON lrp.PERMISSION_NUM = lp.NUM
            WHERE lu.id = :id AND lu.status = :status
            ORDER BY lu.created_at DESC
        """.trimIndent()
        return this.executeSelect(sql, paramMap, PermissionDTO::class.java)
    }

    fun deleteByKnowledgeIdAndName(knowledgeId: String, fileName: String?) {
        // 建立基礎查詢和參數 Map
        val conditions = mutableListOf<String>()
        val parameters = mutableMapOf<String, Any>()
        conditions.add("knowledgeId = :knowledgeId")
        parameters["knowledgeId"] = knowledgeId

        fileName?.let {
            conditions.add("name = :name")
            parameters["name"] = it
        }

        // 構建最終查詢字串
        val query = if (conditions.isEmpty()) {
            "1=1"
        } else {
            conditions.joinToString(" and ")
        }
        delete(query, parameters)
    }
}
