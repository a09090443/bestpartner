package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.McpDTO
import tw.zipe.bastpartner.entity.LLMMcpUserSetting

/**
 * @author Gary
 * @created 2025/3/22
 */
@ApplicationScoped
class LLMMcpUserSettingRepository: BaseRepository<LLMMcpUserSetting, String>() {

    fun findSettingByUserIdAndMcpId(userId: String, mcpId: String): LLMMcpUserSetting? {
        val params = mapOf("userId" to userId, "mcpId" to mcpId)
        return find("userId = :userId AND mcpId = :mcpId", params).firstResult()
    }

    fun findByCondition(settingId: String?, userId: String?): List<McpDTO>? {
        var sql = """
            SELECT lmus.id              AS id,
                   lmus.alias           AS alias,
                   lmus.user_id         AS userId,
                   lmus.mcp_id          AS mcpId,
                   lmus.setting_content AS settingContent,
                   lms.command_setting  AS commandSetting,
                   lms.type             AS type,
                   lms.description      AS description
            FROM llm_mcp_server lms,
                 llm_mcp_user_setting lmus
            WHERE lms.id = lmus.mcp_id
        """.trimIndent()
        val parameters = mutableMapOf<String, Any>()

        settingId?.let {
            sql = sql.plus(" AND lmus.id = :settingId")
            parameters["settingId"] = it
        }
        userId?.let {
            sql = sql.plus(" AND lmus.user_id = :userId")
            parameters["userId"] = it
        }
        return this.executeSelect(sql, parameters, McpDTO::class.java)
    }
}
