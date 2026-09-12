package tw.zipe.bastpartner.service.workflow

import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType

/**
 * 單一節點執行器。實作以 @ApplicationScoped 註冊，引擎依 [type] 分派。
 * 失敗以拋例外表達（引擎統一轉為節點 FAILED）。
 */
interface NodeExecutor {
    val type: NodeType
    fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?>
}
