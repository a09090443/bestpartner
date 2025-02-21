package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.ToolDTO
import tw.zipe.bastpartner.entity.LLMToolEntity

/**
 * @author Gary
 * @created 2024/10/14
 */
@ApplicationScoped
class LLMToolRepository : BaseRepository<LLMToolEntity, String>() {

    fun findByName(name: String) = find("name", name).firstResult()

    fun findByCategoryId(categoryId: String) = find("categoryId", categoryId).list()

    fun findByToolId(id: String): ToolDTO? = findByCondition(id).firstOrNull()

    fun findByCondition(toolId: String?): List<ToolDTO> {
        var sql = """
            SELECT lt.id                   AS id,
                   lt.name                 AS name,
                   lt.class_path           AS classPath,
                   lt.category_id          AS groupId,
                   lt.type                 AS type,
                   lt.config_object_path   AS configObjectPath,
                   lt.function_name        AS functionName,
                   lt.function_description AS functionDescription,
                   lt.function_params      AS functionParams,
                   lt.description          AS description,
                   ltc.name                AS `group`,
                   ltc.description         AS groupDescription
            FROM llm_tool lt
                     LEFT JOIN llm_tool_category ltc ON lt.category_id = ltc.id
            WHERE 1 = 1
        """.trimIndent()
        val parameters = mutableMapOf<String, Any>()

        toolId?.let {
            sql = sql.plus(" AND lt.id = :toolId")
            parameters["toolId"] = it
        }
        return this.executeSelect(sql, parameters, ToolDTO::class.java)
    }
}
