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

    fun findAllList(): List<ToolDTO> {
        val sql = """
            SELECT lt.id             AS id,
                   lt.name           AS name,
                   lt.category_id    AS groupId,
                   lt.description    AS description,
                   lt.setting_fields AS settingFields,
                   ltc.name          AS `group`,
                   ltc.description   AS groupDescription
            FROM llm_tool lt
                     LEFT JOIN llm_tool_category ltc ON lt.category_id = ltc.id
                 """.trimIndent()
        return this.executeSelect(sql, emptyMap(), ToolDTO::class.java)
    }
}
