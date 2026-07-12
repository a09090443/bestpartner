package tw.zipe.bastpartner.service.workflow.executor

import dev.langchain4j.mcp.client.McpClient
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.config.PersistentChatMemoryStore
import tw.zipe.bastpartner.dto.ChatRequestDTO
import tw.zipe.bastpartner.dto.Memory
import tw.zipe.bastpartner.dto.workflow.config.LlmAssistantNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.McpServerNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.dto.workflow.config.SkillNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.ToolNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.LLMService
import tw.zipe.bastpartner.service.McpServerService
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor
import tw.zipe.bastpartner.util.logger

/**
 * LLM 助手節點（Agent 模式）：只負責呼叫指定的 LLM。
 *
 * 工具、MCP、Skill 改為獨立節點（TOOL / MCP_SERVER / SKILL），透過連線掛載到本節點的
 * 工具輸入埠（`in:tool`）。引擎於執行前把這些能力節點解析後放入
 * [ExecutionContext.capabilitiesFor]，本 executor 於推論前聚合為工具集，重用
 * [LLMService.buildAIService]（Tool/RAG/Memory 掛載）與 [LLMService.applyToolProviders]
 * （Skill + MCP 合併掛載），由 LLM 自主決定何時呼叫。
 *
 * userPrompt 支援插值；未填 userPrompt 時視為設定錯誤（拋例外，由引擎轉為節點 FAILED）。
 * MCP client 於本節點執行期間存活、結束即關閉。
 */
@ApplicationScoped
class LlmAssistantExecutor(
    private val llmService: LLMService,
    private val mcpServerService: McpServerService
) : NodeExecutor {
    override val type = NodeType.LLM_ASSISTANT
    private val logger = logger()

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as LlmAssistantNodeConfig
        val message = cfg.userPrompt?.let { context.resolveTemplate(it) }
            ?: throw IllegalArgumentException("LLM 節點需要 userPrompt")

        // memory：enableMemory 且有 memoryId 用之（插值），否則以 executionId 隔離單次執行
        val memoryId = if (cfg.enableMemory == true && !cfg.memoryId.isNullOrBlank()) {
            context.resolveTemplate(cfg.memoryId)
        } else {
            context.executionId
        }

        // Agent 模式：從掛載到本節點工具埠（in:tool）的能力節點聚合工具來源
        val toolIds = mutableListOf<String>()
        val toolSettingIds = mutableListOf<String>()
        val mcpIds = mutableListOf<String>()
        val mcpSettingIds = mutableListOf<String>()
        val skillIds = mutableListOf<String>()
        context.capabilitiesFor(node.nodeKey).forEach { cap ->
            when (val c = cap.config) {
                is ToolNodeConfig ->
                    c.toolSettingId?.takeIf { it.isNotBlank() }?.let { toolSettingIds.add(it) }
                        ?: c.toolId?.takeIf { it.isNotBlank() }?.let { toolIds.add(it) }
                is McpServerNodeConfig ->
                    c.userSettingId?.takeIf { it.isNotBlank() }?.let { mcpSettingIds.add(it) }
                        ?: c.mcpId?.takeIf { it.isNotBlank() }?.let { mcpIds.add(it) }
                is SkillNodeConfig -> c.skillId?.takeIf { it.isNotBlank() }?.let { skillIds.add(it) }
                else -> Unit
            }
        }

        val dto = ChatRequestDTO(
            message = message,
            promptContent = cfg.systemPrompt?.let { context.resolveTemplate(it) } ?: "You are a helpful assistant.",
            memory = Memory(id = memoryId),
            toolIds = toolIds.takeIf { it.isNotEmpty() },
            toolSettingIds = toolSettingIds.takeIf { it.isNotEmpty() },
            skillIds = skillIds.takeIf { it.isNotEmpty() }
        )
        // llmId 為 BaseDTO 的建構後 var 設定
        dto.llmId = cfg.llmId

        val aiService = llmService.buildAIService(dto, ModelType.CHAT)

        val mcpClients = mutableListOf<McpClient>()
        mcpIds.takeIf { it.isNotEmpty() }?.let { mcpClients.addAll(mcpServerService.buildMcpServer(it)) }
        mcpSettingIds.takeIf { it.isNotEmpty() }
            ?.let { mcpClients.addAll(mcpServerService.buildMcpServer(it, context.userId)) }

        try {
            // Skill 與 MCP 的工具集統一組裝（避免 AiServices.toolProvider 單插槽互相覆蓋）
            llmService.applyToolProviders(aiService, dto, mcpClients)
            val reply = aiService.build().chat(dto.memory.id, message).content().text()
            return mapOf((cfg.outputKey ?: "reply") to reply)
        } finally {
            mcpClients.forEach { c -> runCatching { c.close() }.onFailure { logger.warn("MCP client 關閉失敗", it) } }
        }
    }

    companion object {
        /**
         * 清除以 executionId 註冊的 memory：未指定 memoryId 的 LLM 節點以 executionId 為 memoryId
         * 於 [PersistentChatMemoryStore]（單例 store）累積訊息，引擎於執行結束（成功/失敗/取消皆然）
         * 收尾呼叫本方法釋放。使用者自訂 memoryId（enableMemory）為跨執行記憶，刻意不清除。
         */
        fun clearMemory(executionId: String) {
            PersistentChatMemoryStore().deleteMessages(executionId)
        }
    }
}
