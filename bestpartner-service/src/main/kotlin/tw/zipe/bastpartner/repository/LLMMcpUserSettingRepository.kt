package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
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
}
