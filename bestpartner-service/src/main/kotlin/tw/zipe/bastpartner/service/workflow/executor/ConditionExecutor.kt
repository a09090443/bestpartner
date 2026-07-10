package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.ConditionExpressionConfig
import tw.zipe.bastpartner.dto.workflow.config.ConditionNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/**
 * 條件節點：逐條求值 conditions 後依 logic（and/or）聚合，
 * 輸出 result（Boolean）與 branch（活化哪一側出邊的 handle），由引擎據以活化出邊。
 *
 * @author Gary
 * @created 2026/7/10
 */
@ApplicationScoped
class ConditionExecutor : NodeExecutor {
    override val type = NodeType.CONDITION

    companion object {
        const val OUTPUT_RESULT = "result"
        const val OUTPUT_BRANCH = "branch"
        const val DEFAULT_TRUE_HANDLE = "out:true"
        const val DEFAULT_FALSE_HANDLE = "out:false"

        /** 整串恰為單一 {{path}} 時走 resolvePath 取原生型別，保留數值/布林比較能力 */
        private val PURE_PLACEHOLDER = Regex("""^\{\{\s*([\w.\-]+)\s*}}$""")
    }

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as ConditionNodeConfig
        val results = cfg.conditions.orEmpty().map { evaluate(it, context) }
        val result = if (cfg.logic.equals("or", ignoreCase = true)) results.any { it } else results.all { it }
        val branch = if (result) {
            cfg.trueHandle?.takeIf { it.isNotBlank() } ?: DEFAULT_TRUE_HANDLE
        } else {
            cfg.falseHandle?.takeIf { it.isNotBlank() } ?: DEFAULT_FALSE_HANDLE
        }
        return mapOf(OUTPUT_RESULT to result, OUTPUT_BRANCH to branch)
    }

    /** 單一條件求值：left/right 先插值，再依 operator（忽略大小寫）比較 */
    private fun evaluate(expr: ConditionExpressionConfig, context: ExecutionContext): Boolean {
        val left = resolveOperand(expr.left, context)
        return when (expr.operator?.trim()?.lowercase()) {
            "isempty" -> isEmptyValue(left)
            "isnotempty" -> !isEmptyValue(left)
            "eq" -> compare(left, resolveOperand(expr.right, context)) == 0
            "ne" -> compare(left, resolveOperand(expr.right, context)) != 0
            "gt" -> compare(left, resolveOperand(expr.right, context)) > 0
            "gte" -> compare(left, resolveOperand(expr.right, context)) >= 0
            "lt" -> compare(left, resolveOperand(expr.right, context)) < 0
            "lte" -> compare(left, resolveOperand(expr.right, context)) <= 0
            "contains" -> asString(left).contains(asString(resolveOperand(expr.right, context)))
            "notcontains" -> !asString(left).contains(asString(resolveOperand(expr.right, context)))
            else -> throw ServiceException(AppMessage.WORKFLOW_CONDITION_OPERATOR_NOT_SUPPORTED, expr.operator ?: "null")
        }
    }

    /** 純 {{path}} 以 resolvePath 取原生型別；其餘經 resolveTemplate 插值為字串 */
    private fun resolveOperand(raw: String?, context: ExecutionContext): Any? {
        if (raw == null) return null
        val pure = PURE_PLACEHOLDER.matchEntire(raw)
        return if (pure != null) context.resolvePath(pure.groupValues[1]) else context.resolveTemplate(raw)
    }

    /** 兩側可轉 Double 時以數值比較，否則以字串比較 */
    private fun compare(left: Any?, right: Any?): Int {
        val ld = left?.toString()?.toDoubleOrNull()
        val rd = right?.toString()?.toDoubleOrNull()
        return if (ld != null && rd != null) ld.compareTo(rd) else asString(left).compareTo(asString(right))
    }

    private fun asString(value: Any?): String = value?.toString() ?: ""

    private fun isEmptyValue(value: Any?): Boolean = when (value) {
        null -> true
        is String -> value.isEmpty()
        is Collection<*> -> value.isEmpty()
        is Map<*, *> -> value.isEmpty()
        else -> false
    }
}
