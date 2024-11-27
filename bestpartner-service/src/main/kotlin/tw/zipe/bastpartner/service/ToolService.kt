package tw.zipe.bastpartner.service

import io.quarkus.security.identity.SecurityIdentity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import tw.zipe.bastpartner.dto.ToolDTO
import tw.zipe.bastpartner.entity.LLMToolEntity
import tw.zipe.bastpartner.entity.LLMToolUserSettingEntity
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.repository.LLMToolRepository
import tw.zipe.bastpartner.repository.LLMToolUserSettingRepository

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
     * 取得所有工具
     */
    fun getTools() = llmToolRepository.findAll()

    /**
     * 註冊工具
     */
    @Transactional
    fun registerTool(toolDTO: ToolDTO) {
        with(LLMToolEntity()) {
            name = toolDTO.name
            classPath = toolDTO.classPath
            llmToolRepository.persist(this).let { toolDTO.id = id }
        }
    }

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

        jsonStringToMap(toolDTO.settingContent)

        llmToolRepository.findByName(toolDTO.name)?.let {
            LLMToolUserSettingEntity().apply {
                toolId = it.id.orEmpty()
                userId = user
                settingContent = toolDTO.settingContent
                llmToolUserSettingRepository.persist(this)
            }
        }
    }

    fun jsonStringToMap(json: String): Map<String, JsonElement> {
        val data = Json.parseToJsonElement(json)
        require(data is JsonObject) { "settingContent 請確認為 Json 格式" }
        return data
    }
}
