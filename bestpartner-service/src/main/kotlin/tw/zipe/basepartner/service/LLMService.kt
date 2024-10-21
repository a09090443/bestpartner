package tw.zipe.basepartner.service

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.basepartner.dto.LLMDTO
import tw.zipe.basepartner.entity.LLMSettingEntity
import tw.zipe.basepartner.enumerate.ModelType
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.model.LLModel
import tw.zipe.basepartner.repository.LLMSettingRepository

/**
 * @author Gary
 * @created 2024/10/16
 */
@ApplicationScoped
class LLMService(
    private val llmSettingRepository: LLMSettingRepository
) {

    /**
     * 儲存 LLM 設定
     */
    @Transactional
    fun saveLLMSetting(llmDTO: LLMDTO): LLMSettingEntity {
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
     * 取得 LLM 設定
     */
    fun getLLMSetting(platform: Platform, account: String): List<LLModel?> {
//        val llmSettingList = entityManager.createQuery(
//            "SELECT ls FROM LLMSettingEntity ls WHERE ls.platform = :platform AND ls.account = :account",
//            LLMSettingEntity::class.java
//        ).setParameter("platform", platform).setParameter("account", account).resultList
        val llmSettingList = llmSettingRepository.findByAccountAndPlatform(account, platform)
        return llmSettingList.map { it.modelSetting }.toList()
    }

    /**
     * 更新 LLM 設定
     */
    @Transactional
    fun updateLLMSetting(llmDTO: LLMDTO) {
        llmDTO.id ?: throw IllegalArgumentException("llmId is required")

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
            "id" to llmDTO.id!!
        ).let {
            llmSettingRepository.updateSetting(it)
        }
    }

    /**
     * 刪除 LLM 設定
     */
    @Transactional
    fun deleteLLMSetting(id: String) {
        llmSettingRepository.deleteById(id)
    }

    /**
     * 建立 LLM
     */
    fun buildLLM(id: String): Any {
        val llmSettingEntity = llmSettingRepository.findById(id)
        return llmSettingEntity?.let {
            when (it.type) {
                ModelType.EMBEDDING -> it.platform.getLLMBean().embeddingModel(it.modelSetting!!)
                ModelType.CHAT -> it.platform.getLLMBean().chatModel(it.modelSetting!!)
                ModelType.STREAMING_CHAT -> it.platform.getLLMBean().chatModelStreaming(it.modelSetting!!)
            }
        } ?: throw IllegalArgumentException("llmId didn't exist")
    }
}
