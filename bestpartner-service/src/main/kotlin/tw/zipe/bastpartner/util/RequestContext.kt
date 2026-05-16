package tw.zipe.bastpartner.util

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Gary
 * @created 2025/4/25
 * 請求上下文工具類，用於追蹤請求
 */
object RequestContext {
    private val contextMap = ConcurrentHashMap<String, Map<String, Any>>()

    fun generateRequestId(): String {
        return "req-${UUID.randomUUID().toString().substring(0, 8)}"
    }

    fun setRequestContext(requestId: String, contextData: Map<String, Any>) {
        contextMap[requestId] = contextData
    }

    fun getRequestContext(requestId: String): Map<String, Any>? {
        return contextMap[requestId]
    }

    fun clearRequestContext(requestId: String) {
        contextMap.remove(requestId)
    }
}
