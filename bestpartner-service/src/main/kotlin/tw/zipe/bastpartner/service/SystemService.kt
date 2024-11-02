package tw.zipe.bastpartner.service

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.dto.SystemSettingDTO
import tw.zipe.bastpartner.entity.SystemSettingEntity
import tw.zipe.bastpartner.repository.SystemSettingRepository

/**
 * @author Gary
 * @created 2024/10/19
 */
@ApplicationScoped
class SystemService(
    private val systemSettingRepository: SystemSettingRepository
) {

    fun findAll(): List<SystemSettingDTO> {
        return systemSettingRepository.findAll().list().map {
            SystemSettingDTO(
                id = it.id,
                key = it.settingKey,
                value = it.settingValue.orEmpty(),
                description = it.description
            )
        }.toList()
    }

    fun getSystemSettingValue(key: String): String? = systemSettingRepository.findBySettingKey(key)?.settingValue

    @Transactional
    fun addSystemSetting(systemSettingDTO: SystemSettingDTO): SystemSettingDTO {
        with(SystemSettingEntity()) {
            this.settingKey = systemSettingDTO.key
            this.settingValue = systemSettingDTO.value
            this.description = systemSettingDTO.description.orEmpty()
            systemSettingRepository.persist(this)
        }
        return systemSettingDTO
    }

    @Transactional
    fun updateSystemSetting(systemSettingDTO: SystemSettingDTO): SystemSettingDTO {
        with(SystemSettingEntity()) {
            this.id = systemSettingDTO.id!!
            this.settingKey = systemSettingDTO.key
            this.settingValue = systemSettingDTO.value.orEmpty()
            this.description = systemSettingDTO.description.orEmpty()
            systemSettingRepository.updateSystemSettingByNative(
                this.id,
                this.settingKey,
                this.settingValue!!,
                this.description
            )
        }
        return systemSettingDTO
    }

    @Transactional
    fun deleteSystemSetting(key: String) = systemSettingRepository.delete(key)
}
