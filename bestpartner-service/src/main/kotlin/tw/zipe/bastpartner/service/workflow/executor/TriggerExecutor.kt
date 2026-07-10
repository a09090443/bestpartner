package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/**
 * MANUAL 觸發：把引擎預放於 `__input__` 的啟動 payload 轉為本節點輸出，
 * 供下游以 {{<triggerNodeKey>.input.*}} 引用。
 */
@ApplicationScoped
class TriggerExecutor : NodeExecutor {
    override val type = NodeType.TRIGGER

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> =
        context.getOutput(INPUT_KEY) ?: mapOf("input" to emptyMap<String, Any?>())

    companion object {
        const val INPUT_KEY = "__input__"
    }
}
