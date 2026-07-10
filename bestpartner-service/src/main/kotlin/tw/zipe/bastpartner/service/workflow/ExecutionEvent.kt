package tw.zipe.bastpartner.service.workflow

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * SSE 事件（每事件一行 JSON）。event 值域：
 * execution.started / node.started / node.completed / node.failed / execution.completed
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExecutionEvent(
    val event: String,
    val executionId: String,
    val nodeKey: String? = null,
    val seqNo: Int? = null,
    val status: String? = null,
    val output: Map<String, Any?>? = null,
    val error: String? = null,
    val durationMs: Long? = null,
    val ts: String
) {
    fun toJson(): String = MAPPER.writeValueAsString(this)

    companion object {
        private val MAPPER = ObjectMapper()
    }
}

/** 事件出口：SSE emitter 實作；未來 CRON/WEBHOOK 觸發可另接 */
fun interface ExecutionEventSink {
    fun emit(event: ExecutionEvent)
}
