package tw.zipe.basepartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.entity.LLMSettingEntity
import tw.zipe.basepartner.enumerate.Platform

/**
 * @author Gary
 * @created 2024/10/11
 */
@ApplicationScoped
class LLMSettingRepository : PanacheRepositoryBase<LLMSettingEntity, String> {

    fun findByAlias(alias: String) = find("alias", alias).firstResult()

    fun findByAccountAndPlatform(account: String, platform: Platform?): List<LLMSettingEntity> {
        // 建立基礎查詢和參數 Map
        val conditions = mutableListOf<String>()
        val parameters = mutableMapOf<String, Any>()
        conditions.add("account = :account")
        parameters["account"] = account

        platform?.let {
            conditions.add("platform = :platform")
            parameters["platform"] = it
        }

        // 構建最終查詢字串
        val query = if (conditions.isEmpty()) {
            "1=1"
        } else {
            conditions.joinToString(" and ")
        }

        return list(query, parameters)
    }

    fun updateSetting(parasMap: Map<String, Any>) {
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
