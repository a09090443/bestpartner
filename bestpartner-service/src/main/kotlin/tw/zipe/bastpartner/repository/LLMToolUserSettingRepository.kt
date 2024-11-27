package tw.zipe.bastpartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMToolUserSettingEntity

/**
 * @author Gary
 * @created 2024/11/27
 */
@ApplicationScoped
class LLMToolUserSettingRepository : PanacheRepositoryBase<LLMToolUserSettingEntity, String> {
}
