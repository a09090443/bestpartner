package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.LLMSettingDTO
import tw.zipe.bastpartner.entity.LLMSettingEntity

/**
 * @author Gary
 * @created 2024/10/11
 */
@ApplicationScoped
class LLMSettingRepository : BaseRepository<LLMSettingEntity, String>() {

    fun findByConditions(userId: String, platformId: String?, platformName: String?, llmId: String?): List<LLMSettingDTO> {
        var sql = """
            SELECT ls.id            AS id,
                   ls.user_id       AS userId,
                   ls.platform_id   AS platformId,
                   ls.type          AS type,
                   ls.alias         AS alias,
                   ls.model_setting AS modelSetting,
                   lp.name          AS platformName
            FROM llm_setting ls
                     LEFT JOIN llm_platform lp ON ls.platform_id = lp.id
            WHERE ls.user_id = :userId
            """.trimIndent()
        // 建立基礎查詢參數 Map
        val parameters = mutableMapOf<String, Any>()
        parameters["userId"] = userId

        platformId?.let {
            sql = sql.plus(" AND ls.platform_id = :platformId")
            parameters["platformId"] = it
        }
        platformName?.let {
            sql = sql.plus(" AND lp.name = :platformName")
            parameters["platformName"] = it
        }
        llmId?.let {
            sql = sql.plus(" AND ls.id = :llmId")
            parameters["llmId"] = it
        }
        return executeSelect(sql, parameters, LLMSettingDTO::class.java)
    }

    fun updateSetting(parasMap: Map<String, Any>): Int {
        val params = parasMap.plus(initParamsMap())
        val sql = """
            UPDATE llm_setting ls 
            SET ls.alias = :alias,
                ls.platform_id = :platformId,
                ls.type = :type,
                ls.model_setting = :modelSetting,
                ls.updated_at = :updatedAt,
                ls.updated_by = :updatedBy
            WHERE ls.id = :id
        """.trimIndent()
        val executor = createSqlExecutor()
            .withSql(sql)
            .withParamMap(params)
        return executeUpdateWithTransaction(executor)
    }
}
