package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMUserRoleEntity
import tw.zipe.bastpartner.entity.LLMUserRoleId

/**
 * @author Gary
 * @created 2024/12/20
 */
@ApplicationScoped
class LLMUserRoleRepository : BaseRepository<LLMUserRoleEntity, LLMUserRoleId>(){
    fun deleteByUserId(userId: String) {
        delete("id.userId", userId)
    }
}
