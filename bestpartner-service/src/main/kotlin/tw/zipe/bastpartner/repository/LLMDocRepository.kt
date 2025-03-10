package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMDocEntity

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
