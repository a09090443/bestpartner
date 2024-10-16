package tw.zipe.basepartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.entity.LLMSettingEntity

/**
 * @author Gary
 * @created 2024/10/11
 */
@ApplicationScoped
class LLMSettingRepository : PanacheRepositoryBase<LLMSettingEntity, String> {

    fun findByAlias(alias: String) = find("alias", alias).firstResult()
}
