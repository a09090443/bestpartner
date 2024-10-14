package tw.zipe.basepartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.entity.LlmTool

/**
 * @author Gary
 * @created 2024/10/14
 */
@ApplicationScoped
class LlmToolRepository : PanacheRepositoryBase<LlmTool, String> {

    fun findByName(name: String) = find("name", name).firstResult()

    fun deleteByName(name: String) = delete("name", name)
}
