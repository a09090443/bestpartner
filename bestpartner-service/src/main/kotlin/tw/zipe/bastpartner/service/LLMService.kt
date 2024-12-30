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
    fun saveLLMSetting(llmDTO: LLMDTO): LLMSettingEntity {
        with(LLMSettingEntity()) {
            id = llmDTO.id
            userId = securityValidator.validateLoggedInUser()
            platformId = llmDTO.platformId.orEmpty()
            alias = llmDTO.alias
            type = llmDTO.modelType
            modelSetting = llmDTO.llmModel
            llmSettingRepository.saveOrUpdate(this).also { llmDTO.id = this.id }
            return this
        }
    }

    /**
     * 取得 LLM 設定
     */
    fun getLLMSetting(llmId: String): LLMDTO? {
        return llmSettingRepository.findById(llmId)?.let { llmSetting ->
            LLMDTO().apply {
                id = llmSetting.id
                alias = llmSetting.alias
                platformId = llmSetting.platformId
                modelType = llmSetting.type ?: ModelType.CHAT
                llmModel = llmSetting.modelSetting
            }
        }
    }

    /**
     * 取得 LLM 設定
     */
    fun getLLMSetting(userId: String, platformId: String): List<LLMDTO?> {
//        val llmSettingList = entityManager.createQuery(
//            "SELECT ls FROM LLMSettingEntity ls WHERE ls.platform = :platform AND ls.account = :account",
//            LLMSettingEntity::class.java
//        ).setParameter("platform", platform).setParameter("account", account).resultList
        return llmSettingRepository.findByUserIdAndPlatformId(userId, platformId)
            .map { llmSetting ->
                LLMDTO().apply {
                    id = llmSetting.id
                    alias = llmSetting.alias
                    modelType = ModelType.valueOf(llmSetting.type)
                    llmModel = objectMapper.readValue(llmSetting.modelSetting, LLModel::class.java)
                    platform = Platform.valueOf(llmSetting.platformName)
                }
            }.toList()
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
        val llmSettingEntity = llmSettingRepository.findByUserIdAndPlatformId(id, null).firstOrNull()
        return llmSettingEntity?.let {
            when (type) {
                ModelType.EMBEDDING -> Platform.valueOf(it.platformName).getLLMBean()
                    .embeddingModel(objectMapper.readValue(it.modelSetting, LLModel::class.java))

                ModelType.CHAT -> Platform.valueOf(it.platformName).getLLMBean()
                    .chatModel(objectMapper.readValue(it.modelSetting, LLModel::class.java))

                ModelType.STREAMING_CHAT -> Platform.valueOf(it.platformName).getLLMBean()
                    .chatModelStreaming(objectMapper.readValue(it.modelSetting, LLModel::class.java))
            }
        } ?: throw ServiceException("請確認存取的 LLM 設定是否存在")
    }
}
