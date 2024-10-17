package tw.zipe.basepartner.service

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.LocalDateTime
import tw.zipe.basepartner.dto.LLMDTO
import tw.zipe.basepartner.entity.LLMSettingEntity
import tw.zipe.basepartner.repository.LLMSettingRepository

/**
 * @author Gary
 * @created 2024/10/16
 */
@ApplicationScoped
class LLMService(
    val llmSettingRepository: LLMSettingRepository
) {

    /**
     * 儲存 LLM 設定
     */
    @Transactional
    fun saveLLM(llmDTO: LLMDTO): LLMSettingEntity {
        val llmSettingEntity = LLMSettingEntity()
        llmSettingEntity.platform = llmDTO.platform
        llmSettingEntity.alias = llmDTO.alias
        llmSettingEntity.type = llmDTO.modelType
        llmSettingEntity.modelSetting = llmDTO.llmModel

        llmSettingRepository.persist(llmSettingEntity)
        return llmSettingEntity
    }

    /**
     * 取得 LLM 設定
     */
    fun getLLMSetting(id: String): LLMDTO? {
//        val llmSettingEntity = entityManager.createQuery(
//            "SELECT ls FROM LLMSettingEntity ls WHERE ls.alias = :alias",
//            LLMDTO::class.java
//        ).setParameter("alias", alias).singleResult

        val llmSettingEntity = llmSettingRepository.findById(id)

        return llmSettingEntity?.let {
            val llmDTO = LLMDTO()
            llmDTO.id = it.id
            llmDTO.modelType = it.type
            llmDTO.alias = it.alias
            llmDTO.platform = it.platform
            llmDTO.llmModel = it.modelSetting
            return llmDTO
        }
    }

    /**
     * 更新 LLM 設定
     */
    @Transactional
    fun updateLLMSetting(llmDTO: LLMDTO) {
        val llmSettingEntity = LLMSettingEntity()
        llmSettingEntity.id = llmDTO.id
        llmSettingEntity.platform = llmDTO.platform
        llmSettingEntity.type = llmDTO.modelType
        llmSettingEntity.modelSetting = llmDTO.llmModel

        mapOf(
            "alias" to llmDTO.alias,
            "platform" to llmDTO.platform,
            "type" to llmDTO.modelType,
            "modelSetting" to llmDTO.llmModel!!,
            "id" to llmDTO.id!!,
            "updatedAt" to LocalDateTime.now()
        ).let {
            llmSettingRepository.updateSetting(llmSettingEntity, it)
        }
    }

    @Transactional
    fun deleteLLMSetting(id: String) {
        llmSettingRepository.deleteById(id)
    }
}
