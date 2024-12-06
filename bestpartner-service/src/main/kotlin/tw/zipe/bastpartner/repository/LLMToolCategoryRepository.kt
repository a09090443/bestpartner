package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMToolCategoryEntity

/**
 * @author Gary
 * @created 2024/12/5
 */
@ApplicationScoped
class LLMToolCategoryRepository : BaseRepository<LLMToolCategoryEntity, String>() {

    fun updateByNative(id: String, name: String, description: String): Int {
        val paramMap = initParamsMap("id" to id, "name" to name, "description" to description)
        val sql = """
            UPDATE llm_tool_category ltc
            SET ltc.name = :name, ltc.description = :description, ltc.updated_at = :updatedAt, ltc.updated_by = :updatedBy
            WHERE ltc.id = :id
        """.trimIndent()
        val executor = createSqlExecutor()
            .withSql(sql)
            .withParamMap(paramMap)
        return executeUpdateWithTransaction(executor)
    }
}
