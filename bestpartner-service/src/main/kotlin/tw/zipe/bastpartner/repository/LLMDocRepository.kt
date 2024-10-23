package tw.zipe.bastpartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMDocEntity

/**
 * @author Gary
 * @created 2024/10/22
 */
@ApplicationScoped
class LLMDocRepository : PanacheRepositoryBase<LLMDocEntity, String> {
}
