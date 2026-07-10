package tw.zipe.bastpartner.service.workflow

import com.fasterxml.jackson.databind.ObjectMapper

/** 插值變數不存在（含完整 path），由引擎轉為該節點 FAILED */
class VariableNotFoundException(val path: String) : RuntimeException(path)

/**
 * 單次執行的變數上下文：nodeKey → 該節點輸出物件。
 * 下游以 `{{nodeKey.path}}` 插值引用上游輸出，path 支援巢狀（Map 逐層取值）。
 */
class ExecutionContext(
    val executionId: String,
    val userId: String
) {
    private val outputs = LinkedHashMap<String, Map<String, Any?>>()
    private val objectMapper = ObjectMapper()

    companion object {
        /** {{ path }}，path 允許英數、底線、連字號與點 */
        private val PLACEHOLDER = Regex("""\{\{\s*([\w.\-]+)\s*}}""")
    }

    fun putOutput(nodeKey: String, output: Map<String, Any?>) {
        outputs[nodeKey] = output
    }

    fun getOutput(nodeKey: String): Map<String, Any?>? = outputs[nodeKey]

    fun allOutputs(): Map<String, Map<String, Any?>> = outputs

    fun resolvePath(path: String): Any? {
        val segments = path.split(".")
        var current: Any? = outputs[segments.first()] ?: throw VariableNotFoundException(path)
        segments.drop(1).forEach { seg ->
            val map = current as? Map<*, *> ?: throw VariableNotFoundException(path)
            if (!map.containsKey(seg)) throw VariableNotFoundException(path)
            current = map[seg]
        }
        return current
    }

    fun resolveTemplate(text: String): String =
        PLACEHOLDER.replace(text) { match ->
            when (val value = resolvePath(match.groupValues[1])) {
                null -> ""
                is String -> value
                is Number, is Boolean -> value.toString()
                else -> objectMapper.writeValueAsString(value)
            }
        }
}
