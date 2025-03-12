package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMDocSliceEntity

/**
 * @author Gary
 * @created 2025/3/8
 */
@ApplicationScoped
class LLMDocSliceRepository : BaseRepository<LLMDocSliceEntity, String>() {

    fun findByKnowledgeIdAndDocId(knowledgeId: String, docId: String): List<LLMDocSliceEntity>? {
        val params = mapOf("knowledgeId" to knowledgeId, "docId" to docId)
        return find("knowledgeId = :knowledgeId AND docId = :docId", params).list()
    }

    fun deleteByKnowledgeIdAndDocId(knowledgeId: String, docId: String?) {
        // 建立基礎查詢和參數 Map
        val conditions = mutableListOf<String>()
        val parameters = mutableMapOf<String, Any>()
        conditions.add("knowledgeId = :knowledgeId")
        parameters["knowledgeId"] = knowledgeId

        docId?.let {
            conditions.add("docId = :docId")
            parameters["docId"] = it
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
