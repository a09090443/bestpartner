package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMToolUserSettingEntity

/**
 * @author Gary
 * @created 2024/11/27
 */
@ApplicationScoped
class LLMToolUserSettingRepository : BaseRepository<LLMToolUserSettingEntity, String>() {

    fun updateSettingsByNative(id: String, settingContent: String): Int {
        val paramMap = initParamsMap("id" to id, "settingContent" to settingContent)
        val sql = """
            UPDATE llm_tool_user_setting ltus
            SET ltus.setting_content = :settingContent, ltus.updated_at = :updatedAt, ltus.updated_by = :updatedBy
            WHERE ltus.id = :id
        """.trimIndent()
        val executor = createSqlExecutor()
            .withSql(sql)
            .withParamMap(paramMap)
        return executeUpdateWithTransaction(executor)
    }
}
