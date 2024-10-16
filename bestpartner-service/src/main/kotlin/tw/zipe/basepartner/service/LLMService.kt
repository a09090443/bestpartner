package tw.zipe.basepartner.service

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
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
    fun saveLLM(llmDTO: LLMDTO) {
        val llmSettingEntity = LLMSettingEntity()
        llmSettingEntity.platform = llmDTO.platform
        llmSettingEntity.alias = llmDTO.alias
        llmSettingEntity.type = llmDTO.modelType
        llmSettingEntity.modelSetting = llmDTO.llmModel

        llmSettingRepository.persist(llmSettingEntity)
    }

    /**
     * 取得 LLM 設定
     */
    fun getLLMSetting(alias: String): LLMDTO? {
        val llmSettingEntity = llmSettingRepository.findByAlias(alias)

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
    fun updateLLMSetting(llmDTO: LLMDTO) {
        val llmSettingEntity = LLMSettingEntity()
        llmSettingEntity.platform = llmDTO.platform
        llmSettingEntity.type = llmDTO.modelType
        llmSettingEntity.modelSetting = llmDTO.llmModel

        llmSettingRepository.persist(llmSettingEntity)
    }
}
