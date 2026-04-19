package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMSkillEntity
import tw.zipe.bastpartner.enumerate.SkillScope

/**
 * @author Gary
 * @created 2025/04/18
 */
@ApplicationScoped
class LLMSkillRepository : BaseRepository<LLMSkillEntity, String>() {

    fun findByUserIdAndName(userId: String, name: String): LLMSkillEntity? =
        find("userId = ?1 and name = ?2", userId, name).firstResult()

    fun findAllByUserId(userId: String): List<LLMSkillEntity> = find("userId", userId).list()

    fun findAllByScope(scope: SkillScope): List<LLMSkillEntity> = find("scope", scope).list()

    fun findByScopeAndName(scope: SkillScope, name: String): LLMSkillEntity? =
        find("scope = ?1 and name = ?2", scope, name).firstResult()
}
