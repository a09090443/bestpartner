package tw.zipe.bastpartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.SystemSettingEntity

/**
 * @author Gary
 * @created 2024/10/19
 */
@ApplicationScoped
class SystemSettingRepository : PanacheRepositoryBase<SystemSettingEntity, String> {

    fun findBySettingKey(settingKey: String) = find("settingKey", settingKey).firstResult()
}
