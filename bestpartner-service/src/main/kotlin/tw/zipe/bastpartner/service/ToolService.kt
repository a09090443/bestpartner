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
import tw.zipe.bastpartner.enumerate.ToolsCategory
import tw.zipe.bastpartner.enumerate.ToolsType
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
                name = it.name,
                group = it.category,
                type = it.type,
                description = it.description.orEmpty(),
            )
        }.toList()
    }

    /**
     * 註冊工具
     */
    @Transactional
    fun registerTool(toolDTO: ToolDTO) {
        with(LLMToolEntity()) {
            name = toolDTO.name.orEmpty()
            classPath = toolDTO.classPath
            category = toolDTO.group
            type = toolDTO.type
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
     * 透過ID找尋工具
     */
    fun findToolById(id: String) = llmToolRepository.findById(id)

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
     * 執行工具
     */
    fun buildTool(toolDTO: ToolDTO) {
        val tool = llmToolRepository.findById(toolDTO.id.orEmpty()) ?: throw ServiceException("找不到工具")

        tool.settingFields.let { toolSettingFields ->
            val userSetting = llmToolUserSettingRepository.findSettingByUserIdAndToolId(tool.id!!, identity.principal?.name!!)
            tool.type?.let {
                if (it == ToolsType.BUILT_IN) {
                    when(toolDTO.group){
                        ToolsCategory.WEB_SEARCH -> {
                            val settingJson = userSetting?.let { setting -> jsonStringToMap(setting.settingContent) }
                            jsonStringToMap(toolSettingFields.orEmpty()).forEach { map ->
                                if (settingJson?.get(map.key) == null) {
                                    throw ServiceException("請設定欄位 ${map.key} 值")
                                }
                            }
                        }
                        ToolsCategory.DATE -> TODO()
                        ToolsCategory.OTHER -> TODO()
                        null -> TODO()
                    }
                }
            }
        }

        val settingJson = jsonStringToMap(toolDTO.settingContent)
        val settingFields = tool.settingFields?.split(",")?.toList()
        settingFields?.forEach {
            if (settingJson[it] == null) {
                throw ServiceException("請設定欄位 $it 值")
            }
        }
        val clazz = Class.forName(tool?.classPath)
        val instance = clazz.getDeclaredConstructor().newInstance()
        clazz.getDeclaredMethod("execute", Map::class.java).invoke(instance, settingJson)
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
