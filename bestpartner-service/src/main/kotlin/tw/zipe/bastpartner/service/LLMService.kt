package tw.zipe.bastpartner.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.config.SecurityValidator
import tw.zipe.bastpartner.dto.LLMDTO
import tw.zipe.bastpartner.entity.LLMSettingEntity
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.enumerate.Platform
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.model.LLModel
import tw.zipe.bastpartner.repository.LLMSettingRepository
import tw.zipe.bastpartner.util.DTOValidator

/**
 * @author Gary
 * @created 2024/10/16
 */
@ApplicationScoped
class LLMService(
    private val llmSettingRepository: LLMSettingRepository,
    private val securityValidator: SecurityValidator,
    private val objectMapper: ObjectMapper
) {

    /**
     * 儲存 LLM 設定
     */
    fun saveLLMSetting(llmDTO: LLMDTO) {
        with(LLMSettingEntity()) {
            id = llmDTO.id
            userId = securityValidator.validateLoggedInUser()
            platformId = llmDTO.platformId.orEmpty()
            alias = llmDTO.alias
            type = llmDTO.modelType
            modelSetting = llmDTO.llmModel
            llmSettingRepository.saveOrUpdate(this).also { llmDTO.id = this.id }
        }
    }

    /**
     * 取得 LLM 設定
     */
    fun getLLMSetting(id: String): LLMDTO? {
        val llmSettingEntity = llmSettingRepository.find(id).firstResult()

        return llmSettingEntity?.let {
            val llmDTO = LLMDTO()
            llmDTO.modelType = it.type ?: ModelType.CHAT
            llmDTO.alias = it.alias
            llmDTO.platformId = it.platformId
            llmDTO.llmModel = it.modelSetting
            llmDTO
        }
    }

    /**
     * 取得 LLM 設定
     */
    fun getLLMSetting(userId: String, platformId: String): List<LLModel?> {
//        val llmSettingList = entityManager.createQuery(
//            "SELECT ls FROM LLMSettingEntity ls WHERE ls.platform = :platform AND ls.account = :account",
//            LLMSettingEntity::class.java
//        ).setParameter("platform", platform).setParameter("account", account).resultList
        val llmSettingList = llmSettingRepository.findByUserIdAndPlatformId(userId, platformId)
            .map { objectMapper.readValue(it.modelSetting, LLModel::class.java) }.toList()
        return llmSettingList
    }

    /**
     * 更新 LLM 設定
     */
    fun updateLLMSetting(llmDTO: LLMDTO) {
        DTOValidator.validate(llmDTO) {
            requireNotEmpty("id")
            throwOnInvalid()
        }

        mapOf(
            "alias" to llmDTO.alias,
            "platformId" to llmDTO.platformId.orEmpty(),
            "type" to llmDTO.modelType,
            "modelSetting" to llmDTO.llmModel,
            "id" to llmDTO.id.orEmpty()
        ).let {
            llmSettingRepository.updateSetting(it)
        }
    }

    /**
     * 刪除 LLM 設定
     */
    @Transactional
    fun deleteLLMSetting(id: String) = llmSettingRepository.deleteById(id)

    /**
     * 建立 LLM
     */
    fun buildLLM(id: String, type: ModelType): Any {
        val llmSettingEntity = llmSettingRepository.findById(id)
        return llmSettingEntity?.let {
            when (it.type) {
                ModelType.EMBEDDING -> it.platform.getLLMBean().embeddingModel(it.modelSetting!!)
                ModelType.CHAT -> it.platform.getLLMBean().chatModel(it.modelSetting!!)
                ModelType.STREAMING_CHAT -> it.platform.getLLMBean().chatModelStreaming(it.modelSetting!!)
            }
        } ?: throw ServiceException("請確認存取的 LLM 設定是否存在")
    }
}
