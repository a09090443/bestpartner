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

    /**
     * 依「設定 id ＋ 擁有者」取設定。
     *
     * ⚠️ 勿改用 [findSettingByUserIdAndToolId]：那支比對的是 `tool_id` 欄位，
     * 傳入設定 id 永遠不會相符（TOOL 節點填 toolSettingId 必失敗的成因）。
     * 帶 userId 是擁有權檢核，避免以他人的 settingId 取用設定。
     */
    fun findSettingByIdAndUserId(settingId: String, userId: String): LLMToolUserSettingEntity? {
        val params = mapOf("id" to settingId, "userId" to userId)
        return find("id = :id AND userId = :userId", params).firstResult()
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
