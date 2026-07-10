package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.dto.workflow.config.ToolNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.ToolService
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/**
 * 工具節點：以 ToolService 實例化工具。Phase 1 支援無參數/設定檔工具的直接執行；
 * 帶 arguments 的動態呼叫依 config.arguments 插值後以 toString 附入輸出（完整動態 dispatch 屬 Phase 2 範圍）。
 */
@ApplicationScoped
class ToolNodeExecutor(private val toolService: ToolService) : NodeExecutor {
    override val type = NodeType.TOOL

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as ToolNodeConfig
        val tool = if (!cfg.toolSettingId.isNullOrBlank()) {
            toolService.buildToolWithSetting(cfg.toolSettingId)
        } else {
            toolService.buildTool(cfg.toolId!!)
        } ?: throw IllegalStateException("Tool build failed: ${cfg.toolId}")
        return mapOf((cfg.outputKey ?: "result") to tool.toString())
    }
}
