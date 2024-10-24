package tw.zipe.bastpartner.service

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.repository.SystemSettingRepository

/**
 * @author Gary
 * @created 2024/10/19
 */
@ApplicationScoped
class SystemService(
    private val systemSettingRepository: SystemSettingRepository
) {

    fun getSystemSettingValue(key: String): String? = systemSettingRepository.findBySettingKey(key)?.settingValue
}
