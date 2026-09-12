package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.LoopNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/**
 * 迴圈節點（薄殼）：迭代編排放在引擎（需執行子圖的其他節點），
 * 引擎不經 [execute] 分派 LOOP，僅呼叫 [resolveItems] 解析輸入陣列；
 * 常數（預設 alias / 彙集鍵 / 迭代上限 / handle 字串）集中於此供引擎引用。
 *
 * @author Gary
 * @created 2026/7/10
 */
@ApplicationScoped
class LoopExecutor : NodeExecutor {
    override val type = NodeType.LOOP

    companion object {
        const val DEFAULT_ITEM_ALIAS = "item"
        const val DEFAULT_COLLECT_KEY = "items"
        const val DEFAULT_MAX_ITERATIONS = 100

        /** LOOP → 子圖入口的邊 handle（僅界定子圖入口，不參與活化） */
        const val LOOP_HANDLE = "out:loop"

        /** 迭代完成後活化的出邊 handle */
        const val DONE_HANDLE = "out:done"

        /** inputArrayPath 允許 {{path}} 包裹或裸 path 兩種寫法 */
        private val PURE_PLACEHOLDER = Regex("""^\{\{\s*([\w.\-]+)\s*}}$""")
    }

    /** 引擎特判 LOOP 自行編排，不會呼叫此方法；保留最小實作僅回傳解析後的輸入陣列 */
    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> =
        mapOf(DEFAULT_COLLECT_KEY to resolveItems(config as LoopNodeConfig, context))

    /** inputArrayPath 經 resolvePath 取得 List；非 List 拋例外（訊息含原始 path 字串） */
    fun resolveItems(cfg: LoopNodeConfig, context: ExecutionContext): List<Any?> {
        val raw = cfg.inputArrayPath?.takeIf { it.isNotBlank() }
            ?: throw ServiceException(AppMessage.WORKFLOW_LOOP_INPUT_NOT_ARRAY, "inputArrayPath")
        val path = PURE_PLACEHOLDER.matchEntire(raw)?.groupValues?.get(1) ?: raw
        val value = context.resolvePath(path)
        return value as? List<Any?> ?: throw ServiceException(AppMessage.WORKFLOW_LOOP_INPUT_NOT_ARRAY, raw)
    }
}
