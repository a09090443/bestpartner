package tw.zipe.bastpartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMToolEntity

/**
 * @author Gary
 * @created 2024/10/14
 */
@ApplicationScoped
class LLMToolRepository : PanacheRepositoryBase<LLMToolEntity, String> {

    fun findByName(name: String) = find("name", name).firstResult()

}
