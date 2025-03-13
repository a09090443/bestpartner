package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.LLMDocDTO
import tw.zipe.bastpartner.dto.PermissionDTO
import tw.zipe.bastpartner.entity.LLMDocEntity

/**
 * @author Gary
 * @created 2024/10/22
 */
@ApplicationScoped
class LLMDocRepository : BaseRepository<LLMDocEntity, String>() {

    fun findByKnowledgeIdAndName(knowledgeId: String, name: String): LLMDocEntity? {
        val params = mapOf("knowledgeId" to knowledgeId, "name" to name)
        return find("knowledgeId = :knowledgeId AND name = :name", params).firstResult()
    }

    fun findByKnowledgeId(userId: String, knowledgeId: String?): List<LLMDocDTO> {
        var sql = """
            SELECT lk.id AS knowledgeId,
                   lk.llm_embedding_id AS embeddingModelId,
                   lk.vector_store_id AS embeddingStoreId,
                   lk.name AS knowledgeName,
                   lk.description AS knowledgeDescription,
                   ld.id AS docId,
                   ld.name AS docName,
                   ld.url AS fileName,
                   ld.description AS description,
                   ld.type AS type,
                   ld.size AS size
            FROM llm_knowledge lk,
                 llm_doc ld
            WHERE lk.id = ld.knowledge_id AND lk.user_id = :userId
        """.trimIndent()
        // 建立基礎查詢參數 Map
        val parameters = mutableMapOf<String, Any>()
        parameters["userId"] = userId

        knowledgeId?.let {
            sql = sql.plus(" AND lk.id = :knowledgeId")
            parameters["knowledgeId"] = it
        }
        return this.executeSelect(sql, parameters, LLMDocDTO::class.java)
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
