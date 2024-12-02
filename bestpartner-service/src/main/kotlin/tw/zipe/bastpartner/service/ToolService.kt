package tw.zipe.bastpartner.service

import io.quarkus.security.identity.SecurityIdentity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import tw.zipe.bastpartner.dto.ToolDTO
import tw.zipe.bastpartner.entity.LLMToolEntity
import tw.zipe.bastpartner.entity.LLMToolUserSettingEntity
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.repository.LLMToolRepository
import tw.zipe.bastpartner.repository.LLMToolUserSettingRepository
import tw.zipe.bastpartner.util.DTOValidator

/**
 * @author Gary
 * @created 2024/10/14
 */
@ApplicationScoped
class ToolService(
    val llmToolRepository: LLMToolRepository,
    val llmToolUserSettingRepository: LLMToolUserSettingRepository,
    val identity: SecurityIdentity
) {

    /**
     * 取得所有工具清單
     */
    fun getTools(): List<ToolDTO> {
        return llmToolRepository.findAll().list().map {
            ToolDTO(
                id = it.id,
                name = it.name
            )
        }.toList()
    }

    /**
     * 註冊工具
     */
    @Transactional
    fun registerTool(toolDTO: ToolDTO) {
        with(LLMToolEntity()) {
            name = toolDTO.name!!
            classPath = toolDTO.classPath
            settingFields = toolDTO.settingFields?.joinToString(",")
            llmToolRepository.persist(this).let { toolDTO.id = id }
        }
    }

    /**
     * 註銷工具
     */
    @Transactional
    fun deleteTool(id: String) = llmToolRepository.deleteById(id)

    /**
     * 透過名稱找尋工具
     */
    fun findToolByName(name: String) = llmToolRepository.findByName(name)

    /**
     * 移除工具
     */
    @Transactional
    fun removeTool(id: String) = llmToolRepository.deleteById(id)

    /**
     * 儲存使用者工具設定
     */
    @Transactional
    fun saveSetting(toolDTO: ToolDTO) {
        val user = identity.principal?.name?.takeIf { it.isNotEmpty() } ?: throw ServiceException("請確認已登入")
        verifySettingFields(toolDTO).let { tool ->
            LLMToolUserSettingEntity().apply {
                toolId = tool.id.orEmpty()
                userId = user
                settingContent = toolDTO.settingContent
                llmToolUserSettingRepository.persist(this)
                toolDTO.settingId = id
            }
        }
    }

    /**
     * 更新使用者工具設定
     */
    @Transactional
    fun updateSetting(toolDTO: ToolDTO) {
        verifySettingFields(toolDTO)
        llmToolUserSettingRepository.updateSettingsByNative(toolDTO.settingId!!, toolDTO.settingContent)
    }

    fun jsonStringToMap(json: String): Map<String, JsonElement> {
        return Json.parseToJsonElement(json).jsonObject
    }

    /**
     * 驗證設定欄位
     */
    private fun verifySettingFields(toolDTO: ToolDTO): LLMToolEntity {

        DTOValidator.validate(toolDTO) {
            validateJson("settingContent")
            throwOnInvalid()
        }

        return llmToolRepository.findByName(toolDTO.name!!)?.let { tool ->
            val validationFields = tool.settingFields?.split(",")?.toList()
            val settingJson = jsonStringToMap(toolDTO.settingContent)
            validationFields?.forEach {
                if (settingJson[it] == null) {
                    throw ServiceException("請設定欄位 $it 值")
                }
            }
            tool
        } ?: throw ServiceException("找不到工具")
    }
}
