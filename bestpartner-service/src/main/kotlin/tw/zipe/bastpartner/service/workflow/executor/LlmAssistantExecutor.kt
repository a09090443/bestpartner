package tw.zipe.bastpartner.service.workflow.executor

import dev.langchain4j.mcp.McpToolProvider
import dev.langchain4j.mcp.client.McpClient
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.ChatRequestDTO
import tw.zipe.bastpartner.dto.Memory
import tw.zipe.bastpartner.dto.workflow.config.LlmAssistantNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.LLMService
import tw.zipe.bastpartner.service.McpServerService
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor
import tw.zipe.bastpartner.util.logger

/**
 * LLM 助手節點：重用 LLMService.buildAIService（含 Tool/RAG/Memory 掛載），
 * MCP client 於本節點執行期間存活、結束即關閉。
 * userPrompt 支援插值；未填 userPrompt 時視為設定錯誤（拋例外，由引擎轉為節點 FAILED）。
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

        val dto = ChatRequestDTO(
            message = message,
            promptContent = cfg.systemPrompt?.let { context.resolveTemplate(it) } ?: "You are a helpful assistant.",
            memory = Memory(id = memoryId),
            toolIds = cfg.toolIds,
            toolSettingIds = cfg.toolSettingIds,
            knowledgeId = cfg.knowledgeId
        )
        // llmId 為 BaseDTO 的建構參數，ChatRequestDTO 未於自身建構子轉發，僅能於建構後設定（BaseDTO.llmId 已由 val 改為 var）
        dto.llmId = cfg.llmId

        val aiService = llmService.buildAIService(dto, ModelType.CHAT)

        val mcpClients = mutableListOf<McpClient>()
        cfg.mcpIds?.takeIf { it.isNotEmpty() }?.let { mcpClients.addAll(mcpServerService.buildMcpServer(it)) }
        cfg.mcpSettingIds?.takeIf { it.isNotEmpty() }
            ?.let { mcpClients.addAll(mcpServerService.buildMcpServer(it, context.userId)) }

        try {
            if (mcpClients.isNotEmpty()) {
                aiService.toolProvider(McpToolProvider.builder().mcpClients(mcpClients).build())
            }
            val reply = aiService.build().chat(dto.memory.id, message).content().text()
            return mapOf((cfg.outputKey ?: "reply") to reply)
        } finally {
            mcpClients.forEach { c -> runCatching { c.close() }.onFailure { logger.warn("MCP client 關閉失敗", it) } }
        }
    }
}
