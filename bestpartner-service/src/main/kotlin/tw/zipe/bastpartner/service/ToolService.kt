package tw.zipe.bastpartner.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.service.tool.ToolExecutor
import dev.langchain4j.web.search.WebSearchEngine
import dev.langchain4j.web.search.WebSearchTool
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import tw.zipe.bastpartner.config.security.SecurityValidator
import tw.zipe.bastpartner.converter.SensitiveValueCodec
import tw.zipe.bastpartner.dto.ToolDTO
import tw.zipe.bastpartner.entity.LLMToolCategoryEntity
import tw.zipe.bastpartner.entity.LLMToolEntity
import tw.zipe.bastpartner.entity.LLMToolUserSettingEntity
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.ToolsType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.repository.LLMToolCategoryRepository
import tw.zipe.bastpartner.repository.LLMToolRepository
import tw.zipe.bastpartner.repository.LLMToolUserSettingRepository
import tw.zipe.bastpartner.util.ToolSchemaGenerator
import tw.zipe.bastpartner.util.generateFieldJson
import tw.zipe.bastpartner.util.instantiate
import tw.zipe.bastpartner.util.logger
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
    private val objectMapper: ObjectMapper,
    private val sensitiveValueCodec: SensitiveValueCodec
) {
    private val logger = logger()

    /**
     * 取得所有工具清單（含反射產生的 settingSchema，無 config class 者為 null）
     */
    fun getTools() = llmToolRepository.findByCondition(null).onEach { tool ->
        tool.settingSchema = tool.configObjectPath?.let { ToolSchemaGenerator.generate(it) }
    }

    /**
     * 取得工具
     */
    fun getTool(toolId: String): ToolDTO {
        val tool = llmToolRepository.findByToolId(toolId) ?: throw ServiceException(AppMessage.TOOL_NOT_FOUND)
        tool.configObjectPath?.let {
            val clazz = Class.forName(it)
            val kClass = clazz.kotlin
            tool.settingArgs = generateFieldJson(kClass)
        }
        // settingArgs 為既有相容格式；settingSchema 為新契約（結構化、含 required/sensitive/description）
        tool.settingSchema = tool.configObjectPath?.let { path -> ToolSchemaGenerator.generate(path) }
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
        llmToolRepository.findByToolId(toolDTO.id.orEmpty())?.let { tool ->
            with(LLMToolUserSettingEntity()) {
                alias = toolDTO.alias.orEmpty()
                toolId = toolDTO.id.orEmpty()
                userId = securityValidator.validateLoggedInUser()
                // 敏感欄位（@ToolConfigField(sensitive=true)，如 apiKey）加密後落地；新增無既有值
                settingContent = encryptSettingContent(toolDTO.settingContent, tool.configObjectPath, existing = null)
                llmToolUserSettingRepository.persist(this)
                toolDTO.settingId = id
            }
        } ?: throw ServiceException(AppMessage.TOOL_NOT_FOUND)
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
    @Transactional
    fun deleteCategory(toolDTO: ToolDTO): Boolean {
        llmToolRepository.findByCategoryId(toolDTO.groupId.orEmpty()).takeIf { it.isEmpty() }
            ?: throw ServiceException(AppMessage.TOOL_CATEGORY_HAS_TOOLS)
        return llmToolCategoryRepository.deleteById(toolDTO.groupId.orEmpty())
    }

    /**
     * 更新使用者工具設定
     */
    @Transactional
    fun updateSetting(toolDTO: ToolDTO) {
        val existingEntity = llmToolUserSettingRepository.findById(toolDTO.settingId.orEmpty())
            ?: throw ServiceException(AppMessage.TOOL_SETTING_NOT_FOUND)
        // 讀既有 content（密文狀態）供未更動的敏感值沿用；configObjectPath 決定哪些 key 敏感
        val configObjectPath = getTool(existingEntity.toolId).configObjectPath
        val existingMap = parseSettingContent(existingEntity.settingContent)
        val encrypted = encryptSettingContent(toolDTO.settingContent, configObjectPath, existingMap)
        llmToolUserSettingRepository.updateSettingsByNative(
            toolDTO.settingId.orEmpty(),
            encrypted.orEmpty()
        )
    }

    /**
     * 加密 settingContent（JSON 字串）中的敏感欄位。無 configObjectPath 或無敏感欄位時原樣返回。
     * [existing]：既有 content 解析出的 map（可能含密文），供值為遮罩時沿用。
     */
    private fun encryptSettingContent(
        content: String?,
        configObjectPath: String?,
        existing: Map<String, Any?>?
    ): String? {
        if (content.isNullOrBlank() || configObjectPath == null) return content
        val sensitiveKeys = ToolSchemaGenerator.sensitiveFields(configObjectPath)
        if (sensitiveKeys.isEmpty()) return content
        val map = objectMapper.readValue(content, object : TypeReference<Map<String, Any?>>() {})
        return objectMapper.writeValueAsString(sensitiveValueCodec.encryptMap(map, sensitiveKeys, existing))
    }

    /**
     * 解密 settingContent 中的敏感欄位供工具實例化使用。
     */
    private fun decryptSettingContent(content: String?, configObjectPath: String?): String {
        if (content.isNullOrBlank() || configObjectPath == null) return content.orEmpty()
        val sensitiveKeys = ToolSchemaGenerator.sensitiveFields(configObjectPath)
        if (sensitiveKeys.isEmpty()) return content
        val map = objectMapper.readValue(content, object : TypeReference<Map<String, Any?>>() {})
        return objectMapper.writeValueAsString(sensitiveValueCodec.decryptMap(map, sensitiveKeys))
    }

    private fun parseSettingContent(content: String?): Map<String, Any?>? {
        if (content.isNullOrBlank()) return null
        return objectMapper.readValue(content, object : TypeReference<Map<String, Any?>>() {})
    }

    /**
     * 建立工具-需自定義設定值的工具
     */
    fun buildToolWithSetting(toolSettingId: String): Any? {
        // ⚠️ 這裡拿到的是「設定 id」，必須以 id 查（並帶擁有者做權限檢核）。
        // 舊版誤用 findSettingByUserIdAndToolId(userId, toolSettingId) 把設定 id 當 tool_id 比對，
        // 條件永不成立，導致 TOOL 節點只要填 toolSettingId 就必定 TOOL_SETTING_NOT_FOUND。
        val userSetting = llmToolUserSettingRepository.findSettingByIdAndUserId(
            toolSettingId,
            securityValidator.validateLoggedInUser()
        ) ?: throw ServiceException(AppMessage.TOOL_SETTING_NOT_FOUND)

        // 以「指定的那一筆設定」建構；不可退回 buildTool(toolId)，
        // 否則同一工具有多筆設定時會取到別筆（等同忽略使用者的選擇）。
        return buildToolFromSetting(getTool(userSetting.toolId), userSetting)
    }

    /**
     * 建立工具
     */
    fun buildTool(toolDTOId: String): Any? {
        val tool = getTool(toolDTOId)

        val userSetting = tool.configObjectPath?.let {
            llmToolUserSettingRepository.findSettingByUserIdAndToolId(
                securityValidator.validateLoggedInUser(),
                toolDTOId
            )
        }

        return buildToolFromSetting(tool, userSetting)
    }

    /**
     * 以指定的使用者設定實例化工具；無設定（或該工具不需設定）時以無參數建構。
     *
     * 供 [buildTool]（依 toolId 找設定）與 [buildToolWithSetting]（依 settingId 指定設定）共用，
     * 確保兩條路徑的建構行為一致。
     */
    private fun buildToolFromSetting(tool: ToolDTO, userSetting: LLMToolUserSettingEntity?): Any? {
        val instance = userSetting?.let {
            // 使用前解密敏感欄位（settingContent 落地為密文）
            val decrypted = decryptSettingContent(userSetting.settingContent, tool.configObjectPath)
            val settingJson = Json.parseToJsonElement(decrypted).jsonObject
            // 需使用 java 反射才能取得有順序性的 fields
            val configClazz = Class.forName(tool.configObjectPath)
            val fields = configClazz.declaredFields.joinToString(", ") { it.name }
            val sortFields = reorderAndRenameArguments(settingJson, fields)

            instantiateTool(tool, sortFields)
        } ?: instantiateTool(tool, emptyMap())

        // 實例化失敗時工具會被靜默丟棄，模型看不到任何工具卻毫無線索（曾導致 E2E J10-06 誤判為模型不呼叫工具）。
        // 這裡不拋例外（避免單一工具設定壞掉就讓整個對話失敗），但一定要留下可追查的紀錄。
        if (instance == null) {
            logger.error(
                "工具 ${tool.name} 實例化失敗（classPath=${tool.classPath}, configObjectPath=${tool.configObjectPath}），" +
                    "本次對話將不會掛載此工具；請檢查設定欄位是否與建構子相符"
            )
        }
        return instance
    }

    /**
     * 實例化工具
     */
    private fun instantiateTool(tool: ToolDTO, sortFields: Map<String, Any?>): Any? {
        return when(val instance = instantiate(tool.classPath, sortFields)){
            is WebSearchEngine -> WebSearchTool.from(instance)
            is ToolExecutor -> buildToolProvider(tool, instance)
            else -> instance
        }
    }

    /**
     * 建立工具提供者
     */
    fun buildToolProvider(toolDTO: ToolDTO, toolExecutor: ToolExecutor): Map<ToolSpecification, ToolExecutor> {

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
                    else -> throw ServiceException(AppMessage.TOOL_PARAM_TYPE_INVALID, key)
                }
            }
        }
        val toolSpecification = ToolSpecification.builder()
            .name(toolDTO.functionName)
            .description(toolDTO.functionDescription)
            .parameters(
                jsonObjectSchema.build()
            )
            .build()

        return mapOf(toolSpecification to toolExecutor)
    }

    /**
     * 驗證 function params
     */
    fun validateFunctionParams(paramsMap: Map<String, List<Any>>) {
        paramsMap.map { (_, value) ->
            if (value.size == 2) {
                val actualValue = value[0]

                if (actualValue !is String) {
                    throw ServiceException(AppMessage.TOOL_FUNCTION_PARAM_FORMAT_INVALID_VALUE)
                }

            } else {
                throw ServiceException(AppMessage.TOOL_FUNCTION_PARAM_FORMAT_INVALID)
            }
        }
    }
}
