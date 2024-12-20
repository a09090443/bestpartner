package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMUserRole
import tw.zipe.bastpartner.entity.LLMUserRoleId

/**
 * @author Gary
 * @created 2024/12/20
 */
@ApplicationScoped
class LLMUserRoleRepository : BaseRepository<LLMUserRole, LLMUserRoleId>(){
    fun deleteByUserId(userId: String) {
        delete("id.userId", userId)
    }
}
