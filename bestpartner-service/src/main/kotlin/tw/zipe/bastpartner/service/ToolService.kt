package tw.zipe.bastpartner.service

import dev.langchain4j.web.search.WebSearchEngine
import dev.langchain4j.web.search.WebSearchTool
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import tw.zipe.bastpartner.config.SecurityValidator
import tw.zipe.bastpartner.dto.ToolDTO
import tw.zipe.bastpartner.entity.LLMToolCategoryEntity
import tw.zipe.bastpartner.entity.LLMToolEntity
import tw.zipe.bastpartner.entity.LLMToolUserSettingEntity
import tw.zipe.bastpartner.enumerate.ToolsType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.repository.LLMToolCategoryRepository
import tw.zipe.bastpartner.repository.LLMToolRepository
import tw.zipe.bastpartner.repository.LLMToolUserSettingRepository
import tw.zipe.bastpartner.util.DTOValidator
import tw.zipe.bastpartner.util.instantiate
import tw.zipe.bastpartner.util.reorderAndRenameArguments

/**
 * @author Gary
 * @created 2024/10/14
 */
@ApplicationScoped
class ToolService(
    private val llmToolRepository: LLMToolRepository,
    private val llmToolCategoryRepository: LLMToolCategoryRepository,
    private val llmToolUserSettingRepository: LLMToolUserSettingRepository,
    private val securityValidator: SecurityValidator
) {

    /**
     * 取得所有工具清單
     */
    fun getTools() = llmToolRepository.findAllList()

    /**
     * 註冊工具
     */
    fun registerTool(toolDTO: ToolDTO) {
        with(LLMToolEntity()) {
            name = toolDTO.name.orEmpty()
            classPath = toolDTO.classPath
            categoryId = toolDTO.groupId
            type = toolDTO.type
            settingFields = toolDTO.settingFields?.joinToString(",")
            description = toolDTO.description
            llmToolRepository.persist(this).let { toolDTO.id = id }
        }
    }

    /**
     * 移除工具
     */
    @Transactional
    fun deleteTool(id: String) = llmToolRepository.deleteById(id)

    /**
     * 透過ID找尋工具
     */
    fun findToolById(id: String) = llmToolRepository.findById(id)

    /**
     * 儲存使用者工具設定
     */
    @Transactional
    fun saveSetting(toolDTO: ToolDTO) {
        verifySettingFields(toolDTO).let { tool ->
            LLMToolUserSettingEntity().apply {
                toolId = tool.id.orEmpty()
                userId = securityValidator.validateLoggedInUser()
                settingContent = toolDTO.settingContent
                llmToolUserSettingRepository.persist(this)
                toolDTO.settingId = id
            }
        }
    }

    /**
     * 儲存工具群組
     */
    fun saveCategory(toolDTO: ToolDTO) {

        with(LLMToolCategoryEntity()) {
            name = toolDTO.group
            description = toolDTO.groupDescription.orEmpty()
            llmToolCategoryRepository.persist(this).let { toolDTO.groupId = id }
        }
    }

    /**
     * 更新工具群組
     */
    fun updateCategory(toolDTO: ToolDTO) =
        llmToolCategoryRepository.updateByNative(toolDTO.groupId!!, toolDTO.group!!, toolDTO.groupDescription.orEmpty())

    /**
     * 刪除工具群組
     */
    fun deleteCategory(toolDTO: ToolDTO): Boolean {
        llmToolRepository.findByCategoryId(toolDTO.groupId.orEmpty()).takeIf { it.isEmpty() }
            ?: throw ServiceException("請先移除群組下的工具")
        return llmToolCategoryRepository.deleteById(toolDTO.groupId.orEmpty())
    }

    /**
     * 更新使用者工具設定
     */
    @Transactional
    fun updateSetting(toolDTO: ToolDTO) {
        verifySettingFields(toolDTO)
        llmToolUserSettingRepository.updateSettingsByNative(toolDTO.settingId!!, toolDTO.settingContent)
    }

    fun getSettingByUserIdAndToolId(toolId: String) =
        llmToolUserSettingRepository.findSettingByUserIdAndToolId(toolId, securityValidator.validateLoggedInUser())

    fun jsonStringToMap(json: String): Map<String, JsonElement> {
        return Json.parseToJsonElement(json).jsonObject
    }

    /**
     * 建立工具
     */
    fun buildTool(toolDTO: ToolDTO): Any? {
        val tool = llmToolRepository.findById(toolDTO.id.orEmpty()) ?: throw ServiceException("找不到工具")

        val toolSettingFields = tool.settingFields.orEmpty()
        val userSetting = llmToolUserSettingRepository.findSettingByUserIdAndToolId(
            securityValidator.validateLoggedInUser(),
            tool.id!!
        )
        val settingJson = userSetting?.let { jsonStringToMap(it.settingContent) } ?: emptyMap()

        validateSettings(toolSettingFields, settingJson)

        val sortFields = reorderAndRenameArguments(settingJson, toolSettingFields)

        return instantiateTool(tool, sortFields, toolDTO.group!!)
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

    private fun validateSettings(toolSettingFields: String, settingJson: Map<String, JsonElement>) {
        jsonStringToMap(toolSettingFields).forEach { (key, _) ->
            if (settingJson[key] == null) {
                throw ServiceException("請設定欄位 $key 值")
            }
        }
    }

    private fun instantiateTool(tool: LLMToolEntity, sortFields: Map<String, Any>, group: String): Any? {
        val instance = instantiate(tool.classPath, sortFields)
        return when (tool.type) {
            ToolsType.BUILT_IN -> when (group) {
                "WEB_SEARCH" -> WebSearchTool.from(instance as WebSearchEngine)
                else -> instance
            }

            else -> instance
        }
    }

}
