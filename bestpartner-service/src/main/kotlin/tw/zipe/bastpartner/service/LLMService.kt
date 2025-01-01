package tw.zipe.bastpartner.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.config.SecurityValidator
import tw.zipe.bastpartner.dto.LLMDTO
import tw.zipe.bastpartner.dto.PlatformDTO
import tw.zipe.bastpartner.entity.LLMPlatformEntity
import tw.zipe.bastpartner.entity.LLMSettingEntity
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.enumerate.Platform
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.model.LLModel
import tw.zipe.bastpartner.repository.LLMPlatformRepository
import tw.zipe.bastpartner.repository.LLMSettingRepository
import tw.zipe.bastpartner.util.DTOValidator

/**
 * @author Gary
 * @created 2024/10/16
 */
@ApplicationScoped
class LLMService(
    private val llmSettingRepository: LLMSettingRepository,
    private val llmPlatformRepository: LLMPlatformRepository,
    private val securityValidator: SecurityValidator,
    private val objectMapper: ObjectMapper
) {

    /**
     * 儲存 LLM 設定
     */
    fun saveLLMSetting(llmDTO: LLMDTO): LLMSettingEntity {
        val platform = llmPlatformRepository.findById(llmDTO.platformId.orEmpty())
            ?: throw ServiceException("請確認存取的平台是否存在")
        llmDTO.llmModel.platform = platform.name

        with(LLMSettingEntity()) {
            userId = securityValidator.validateLoggedInUser()
            platformId = llmDTO.platformId.orEmpty()
            type = llmDTO.modelType
            alias = llmDTO.alias
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
    fun getLLMSetting(userId: String, platformId: String?, llmId: String?): List<LLMDTO?> {
//        val llmSettingList = entityManager.createQuery(
//            "SELECT ls FROM LLMSettingEntity ls WHERE ls.platform = :platform AND ls.account = :account",
//            LLMSettingEntity::class.java
//        ).setParameter("platform", platform).setParameter("account", account).resultList
        return llmSettingRepository.findByUserIdAndPlatformIdAndLLMId(userId, platformId, llmId)
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
        mapOf(
            "alias" to llmDTO.alias,
            "platformId" to llmDTO.platformId.orEmpty(),
            "type" to llmDTO.modelType?.name.orEmpty(),
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
            val platform = llmPlatformRepository.findById(it.platformId).let {
                if (it == null) {
                    throw ServiceException("請確認存取的平台是否存在")
                }
                it
            }
            when (type) {
                ModelType.EMBEDDING -> platform.name?.getLLMBean()?.embeddingModel(it.modelSetting)
                ModelType.CHAT -> platform.name?.getLLMBean()?.chatModel(it.modelSetting)
                ModelType.STREAMING_CHAT -> platform.name?.getLLMBean()?.chatModelStreaming(it.modelSetting)
            }
        } ?: throw ServiceException("請確認存取的 LLM 設定是否存在")
    }

    /**
     * 新增平台
     */
    fun addPlatform(platformDTO: PlatformDTO) {
        with(LLMPlatformEntity()) {
            name = platformDTO.platform
            llmPlatformRepository.saveOrUpdate(this).also { platformDTO.id = this.id }
        }
    }

    /**
     * 刪除平台
     */
    @Transactional
    fun deletePlatform(id: String) = llmPlatformRepository.deleteById(id)
}
