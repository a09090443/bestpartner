package tw.zipe.bastpartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMUserEntity

/**
 * @author Gary
 * @created 2024/10/14
 */
@ApplicationScoped
class LLMUserRepository : PanacheRepositoryBase<LLMUserEntity, String> {

    fun findByUsername(username: String) = find("username", username).firstResult()

    fun deleteByUsername(username: String) = delete("username", username)
}
