package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.dto.workflow.config.DataTransformNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/**
 * 資料轉換節點：以 mappings / template 將上游輸出重組為新的輸出 map。
 * - mappings：逐筆求值，expression 為純 {{path}} 時以 resolvePath 保留原生型別，否則插值為字串
 * - template：整段插值為字串，輸出至 outputKey（預設 "result"）
 * - 兩者皆有時 mappings 優先，template 結果僅在鍵未被 mappings 佔用時併入
 *
 * @author Gary
 * @created 2026/7/11
 */
@ApplicationScoped
class DataTransformExecutor : NodeExecutor {
    override val type = NodeType.DATA_TRANSFORM

    companion object {
        const val DEFAULT_OUTPUT_KEY = "result"

        /** 整串恰為單一 {{path}} 時走 resolvePath 取原生型別，保留數值/布林/集合結構 */
        private val PURE_PLACEHOLDER = Regex("""^\{\{\s*([\w.\-]+)\s*}}$""")
    }

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as DataTransformNodeConfig
        val output = LinkedHashMap<String, Any?>()

        // mappings 優先寫入；targetKey / expression 為 null 的項目跳過
        cfg.mappings.orEmpty().forEach { mapping ->
            val targetKey = mapping.targetKey ?: return@forEach
            val expression = mapping.expression ?: return@forEach
            output[targetKey] = resolveExpression(expression, context)
        }

        // template 結果併入 outputKey 鍵，與 mappings 衝突時讓位（mappings 優先）
        cfg.template?.takeIf { it.isNotBlank() }?.let { template ->
            val key = cfg.outputKey?.takeIf { it.isNotBlank() } ?: DEFAULT_OUTPUT_KEY
            output.putIfAbsent(key, context.resolveTemplate(template))
        }

        return output
    }

    /** 純 {{path}} 以 resolvePath 取原生型別；其餘經 resolveTemplate 插值為字串 */
    private fun resolveExpression(expression: String, context: ExecutionContext): Any? {
        val pure = PURE_PLACEHOLDER.matchEntire(expression)
        return if (pure != null) context.resolvePath(pure.groupValues[1]) else context.resolveTemplate(expression)
    }
}
