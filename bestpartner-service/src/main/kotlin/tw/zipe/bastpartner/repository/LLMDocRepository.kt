package tw.zipe.bastpartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMDocEntity

/**
 * @author Gary
 * @created 2024/10/22
 */
@ApplicationScoped
class LLMDocRepository : BaseRepository<LLMDocEntity, String>() {

    fun deleteByKnowledgeIdAndFileName(knowledgeId: String, fileName: String?) {
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
