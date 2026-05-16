package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMSkillResourceEntity

/**
 * @author Gary
 * @created 2025/04/18
 */
@ApplicationScoped
class LLMSkillResourceRepository : BaseRepository<LLMSkillResourceEntity, String>() {

    fun findBySkillId(skillId: String): List<LLMSkillResourceEntity> = find("skillId", skillId).list()
}
