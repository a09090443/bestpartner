package tw.zipe.bastpartner.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.service.tool.ToolExecutor
import dev.langchain4j.service.tool.ToolProvider
import dev.langchain4j.service.tool.ToolProviderRequest
import dev.langchain4j.service.tool.ToolProviderResult
import dev.langchain4j.web.search.WebSearchEngine
import dev.langchain4j.web.search.WebSearchTool
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import tw.zipe.bastpartner.config.security.SecurityValidator
import tw.zipe.bastpartner.dto.ToolDTO
import tw.zipe.bastpartner.entity.LLMToolCategoryEntity
import tw.zipe.bastpartner.entity.LLMToolEntity
import tw.zipe.bastpartner.entity.LLMToolUserSettingEntity
import tw.zipe.bastpartner.enumerate.ToolsType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.repository.LLMToolCategoryRepository
import tw.zipe.bastpartner.repository.LLMToolRepository
import tw.zipe.bastpartner.repository.LLMToolUserSettingRepository
import tw.zipe.bastpartner.util.generateFieldJson
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
    private val securityValidator: SecurityValidator,
    private val objectMapper: ObjectMapper
) {

    /**
     * 取得所有工具清單
     */
    fun getTools() = llmToolRepository.findByCondition(null)

    /**
     * 取得工具
     */
    fun getTool(toolId: String): ToolDTO {
        val tool = llmToolRepository.findByToolId(toolId) ?: throw ServiceException("找不到工具")
        tool.configObjectPath?.let {
            val clazz = Class.forName(it)
            val kClass = clazz.kotlin
            tool.settingArgs = generateFieldJson(kClass)
        }
        return tool
    }

    /**
     * 註冊工具
     */
    fun registerTool(toolDTO: ToolDTO) {
        with(LLMToolEntity()) {
            name = toolDTO.name.orEmpty()
            classPath = toolDTO.classPath
            categoryId = toolDTO.groupId
            type = toolDTO.type ?: ToolsType.BUILT_IN
            description = toolDTO.description
            configObjectPath = toolDTO.configObjectPath
            functionName = toolDTO.functionName
            functionDescription = toolDTO.functionDescription
            functionParams = toolDTO.functionParams
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
        llmToolRepository.findByToolId(toolDTO.id.orEmpty())?.let {
            with(LLMToolUserSettingEntity()) {
                toolId = toolDTO.id.orEmpty()
                userId = securityValidator.validateLoggedInUser()
                settingContent = toolDTO.settingContent
                llmToolUserSettingRepository.persist(this)
                toolDTO.settingId = id
            }
        } ?: throw ServiceException("找不到工具")
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
        llmToolUserSettingRepository.updateSettingsByNative(
            toolDTO.settingId.orEmpty(),
            toolDTO.settingContent.orEmpty()
        )
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
        val tool = toolDTO.id?.let {
            getTool(it)
        } ?: throw ServiceException("請正確填寫 tool id")

        val userSetting = tool.configObjectPath?.let {
            llmToolUserSettingRepository.findSettingByUserIdAndToolId(
                securityValidator.validateLoggedInUser(),
                toolDTO.id.orEmpty()
            )
        }
        return userSetting?.let {
            val settingJson = jsonStringToMap(userSetting.settingContent.orEmpty())
            // 需使用 java 反射才能取得有順序性的 fields
            val clazz = Class.forName(tool.configObjectPath)
            val fields = clazz.declaredFields.joinToString(", ") { it.name }
            val sortFields = reorderAndRenameArguments(settingJson, fields)

            instantiateTool(tool, sortFields)
        } ?: return instantiateTool(tool, emptyMap())
    }

    private fun instantiateTool(tool: ToolDTO, sortFields: Map<String, Any?>): Any? {
        val instance = instantiate(tool.classPath, sortFields)
        return when (tool.type) {
            ToolsType.BUILT_IN -> when (tool.group) {
                "WEB_SEARCH" -> WebSearchTool.from(instance as WebSearchEngine)
                else -> instance
            }

            else -> instance
        }
    }

    fun buildToolProvider(toolDTO: ToolDTO): ToolProvider {
        val tool = toolDTO.id?.let {
            getTool(it)
        } ?: throw ServiceException("請正確填寫 tool id")

        val toolExecutor: ToolExecutor = instantiate(tool.classPath) as ToolExecutor

        var jsonObjectSchema = JsonObjectSchema.builder()
        toolDTO.functionParams?.let {
            val map: Map<String, List<Any>> =
                objectMapper.readValue(it, object : TypeReference<Map<String, List<Any>>>() {})
            validateFunctionParams(map)
            var description:String
            map.map { (key, value) ->
                description = value[0].toString()
                jsonObjectSchema = when (value[1]) {
                    "String" -> jsonObjectSchema.addStringProperty(key, description)
                    "Int" -> jsonObjectSchema.addIntegerProperty(key, description)
                    "Long" -> jsonObjectSchema.addIntegerProperty(key, description)
                    "Double" -> jsonObjectSchema.addNumberProperty(key, description)
                    "Boolean" -> jsonObjectSchema.addBooleanProperty(key, description)
                    else -> throw ServiceException("型態不正確: $key")
                }
            }
        }

        return ToolProvider { _ ->
            val toolSpecification = ToolSpecification.builder()
                .name(toolDTO.functionName)
                .description(toolDTO.functionDescription)
                .parameters(
                    jsonObjectSchema.build()
                )
                .build()

            ToolProviderResult.builder()
                .add(toolSpecification, toolExecutor)
                .build()
        }
    }

    fun validateFunctionParams(paramsMap: Map<String, List<Any>>) {
        paramsMap.map { (_, value) ->
            if (value.size == 2) {
                val actualValue = value[0]

                if (actualValue !is String) {
                    throw ServiceException("function params 欄位格式錯誤 (應為 [value, type])，value 應為 String")
                }

            } else {
                throw ServiceException("function params 欄位格式錯誤 (應為 [value, type])")
            }
        }
    }
}
