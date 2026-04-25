package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.entity.LLMToolUserSettingEntity

/**
 * @author Gary
 * @created 2024/11/27
 */
@ApplicationScoped
class LLMToolUserSettingRepository : BaseRepository<LLMToolUserSettingEntity, String>() {

    fun findSettingByUserIdAndToolId(userId: String, toolId: String): LLMToolUserSettingEntity? {
        val params = mapOf("userId" to userId, "toolId" to toolId)
        return find("userId = :userId AND toolId = :toolId", params).firstResult()
    }

    @Transactional
    fun updateSettingsByNative(id: String, settingContent: String): Int {
        val entity = findById(id) ?: return 0
        entity.settingContent = settingContent
        // @PreUpdate 自動設定 updatedAt / updatedBy
        update(entity)
        return 1
    }
}
