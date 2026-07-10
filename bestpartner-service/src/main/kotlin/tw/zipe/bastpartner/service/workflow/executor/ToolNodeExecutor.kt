package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/**
 * 工具節點。Phase 1 佔位：TOOL 動態呼叫於 Phase 2 實作。
 */
@ApplicationScoped
class ToolNodeExecutor : NodeExecutor {
    override val type = NodeType.TOOL

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        throw ServiceException(AppMessage.WORKFLOW_NODE_TYPE_NOT_SUPPORTED, type.name)
    }
}
