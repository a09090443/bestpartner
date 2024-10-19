package tw.zipe.basepartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.entity.SystemSettingEntity

/**
 * @author Gary
 * @created 2024/10/19
 */
@ApplicationScoped
class SystemSettingRepository : PanacheRepositoryBase<SystemSettingEntity, String> {

    fun findBySettingKey(settingKey: String) = find("settingKey", settingKey).firstResult()
}
