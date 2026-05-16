package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.dto.McpDTO
import tw.zipe.bastpartner.entity.LLMMcpUserSetting

/**
 * @author Gary
 * @created 2025/3/22
 */
@ApplicationScoped
class LLMMcpUserSettingRepository : BaseRepository<LLMMcpUserSetting, String>() {

    fun findSettingByUserIdAndMcpId(userId: String, mcpId: String): LLMMcpUserSetting? {
        val params = mapOf("userId" to userId, "mcpId" to mcpId)
        return find("userId = :userId AND mcpId = :mcpId", params).firstResult()
    }

    fun findSettingByUserIdAndSettingId(settingId: String, userId: String = getCurrentUsername()): LLMMcpUserSetting? {
        val params = mapOf("id" to settingId, "userId" to userId)
        return find("id = :id AND userId = :userId", params).firstResult()
    }

    fun findByCondition(settingId: String?, userId: String?): List<McpDTO>? {
        var jpql = """
            SELECT lmus.id AS id, lmus.alias AS alias, lmus.userId AS userId,
                   lmus.mcpId AS mcpId, lmus.settingContent AS settingContent,
                   lms.commandSetting AS commandSetting, lms.type AS type, lms.description AS description
            FROM LLMMcpUserSetting lmus JOIN LLMMcpServerEntity lms ON lmus.mcpId = lms.id
            WHERE 1 = 1
        """.trimIndent()
        val parameters = mutableMapOf<String, Any>()

        settingId?.let { jpql += " AND lmus.id = :settingId"; parameters["settingId"] = it }
        userId?.let { jpql += " AND lmus.userId = :userId"; parameters["userId"] = it }

        return executeJpqlSelect(jpql, parameters, McpDTO::class.java)
    }

    @Transactional
    fun updateSettingsByNative(id: String, settingContent: Map<String, Any>): Int {
        val entity = findById(id) ?: return 0
        @Suppress("UNCHECKED_CAST")
        entity.settingContent = settingContent as Map<String, String>
        // @PreUpdate 自動設定 updatedAt / updatedBy
        update(entity)
        return 1
    }
}
