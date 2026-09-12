package tw.zipe.bastpartner.service.workflow.executor

import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.mcp.client.McpClient
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.McpServerNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.McpServerService
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor
import tw.zipe.bastpartner.util.logger

/**
 * MCP 節點：啟動指定 MCP server、呼叫 toolName、關閉 client。
 * arguments 值插值後以 JSON 傳入（dev.langchain4j ToolExecutionRequest）。
 */
@ApplicationScoped
class McpServerNodeExecutor(private val mcpServerService: McpServerService) : NodeExecutor {
    override val type = NodeType.MCP_SERVER
    private val logger = logger()

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as McpServerNodeConfig
        val clients: List<McpClient> = if (!cfg.userSettingId.isNullOrBlank()) {
            mcpServerService.buildMcpServer(listOf(cfg.userSettingId), context.userId)
        } else {
            mcpServerService.buildMcpServer(listOf(cfg.mcpId!!))
        }
        val client = clients.firstOrNull()
            ?: throw IllegalStateException("MCP client build failed: ${cfg.mcpId}")
        try {
            val argsJson = cfg.arguments?.let { args ->
                context.resolveTemplate(args.toString())
            } ?: "{}"
            val request = ToolExecutionRequest.builder()
                .name(cfg.toolName!!)
                .arguments(argsJson)
                .build()
            val result = client.executeTool(request)
            return mapOf((cfg.outputKey ?: "result") to result.resultText())
        } finally {
            runCatching { client.close() }.onFailure { logger.warn("MCP client 關閉失敗", it) }
        }
    }
}
