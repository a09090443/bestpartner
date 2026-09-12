package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.dto.workflow.config.OutputNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/** 最終輸出組裝：mappings 逐值插值；template 插值結果放 result 鍵 */
@ApplicationScoped
class OutputExecutor : NodeExecutor {
    override val type = NodeType.OUTPUT

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as OutputNodeConfig
        val result = LinkedHashMap<String, Any?>()
        cfg.mappings?.forEach { (key, value) -> result[key] = context.resolveTemplate(value) }
        cfg.template?.takeIf { it.isNotBlank() }?.let { result["result"] = context.resolveTemplate(it) }
        return result
    }
}
