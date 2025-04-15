package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.McpDTO
import tw.zipe.bastpartner.entity.LLMMcpServerEntity

/**
 * @author Gary
 * @created 2025/3/19
 */
@ApplicationScoped
class LLMMcpServerRepository : BaseRepository<LLMMcpServerEntity, String>() {

    fun update(mcpDto: McpDTO): Int {
        val paramMap = initParamsMap(
            "id" to mcpDto.mcpId.orEmpty(),
            "name" to mcpDto.name.orEmpty(),
            "command_setting" to objectMapper.writeValueAsString(mcpDto.commandSetting),
            "type" to mcpDto.type?.name.orEmpty(),
        )
        val sql = """
            UPDATE llm_mcp_server lms
            SET lms.name = :name, lms.command_setting = :command_setting, lms.type = :type, lms.updated_at = :updatedAt, lms.updated_by = :updatedBy
            WHERE lms.id = :id
        """.trimIndent()
        val executor = createSqlExecutor()
            .withSql(sql)
            .withParamMap(paramMap)
        return executeUpdateWithTransaction(executor)
    }
}
