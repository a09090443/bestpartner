package tw.zipe.basepartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.entity.LLMSettingEntity

/**
 * @author Gary
 * @created 2024/10/11
 */
@ApplicationScoped
class LLMSettingRepository : PanacheRepositoryBase<LLMSettingEntity, String> {

    fun findByAlias(alias: String) = find("alias", alias).firstResult()

    fun updateSetting(llmSettingEntity: LLMSettingEntity, parasMap: Map<String, Any>) {
        val sql = """
            UPDATE LLMSettingEntity ls 
            SET ls.alias = :alias,
                ls.platform = :platform,
                ls.type = :type,
                ls.modelSetting = :modelSetting,
                ls.updatedAt = :updatedAt
            WHERE ls.id = :id
        """.trimIndent()
        update(sql, parasMap)
    }
}
