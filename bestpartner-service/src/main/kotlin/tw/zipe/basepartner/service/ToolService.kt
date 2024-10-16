package tw.zipe.basepartner.service

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.util.UUID
import tw.zipe.basepartner.dto.ToolDTO
import tw.zipe.basepartner.entity.LLMToolEntity
import tw.zipe.basepartner.repository.LLMToolRepository

/**
 * @author Gary
 * @created 2024/10/14
 */
@ApplicationScoped
class ToolService(
    val llmToolRepository: LLMToolRepository
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
        val llmTool = LLMToolEntity()
        llmTool.id = UUID.randomUUID().toString()
        llmTool.name = toolDTO.name
        llmTool.classPath = toolDTO.classPath
        llmToolRepository.persist(llmTool).let { toolDTO.id = llmTool.id }
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
}
