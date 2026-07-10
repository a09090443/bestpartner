package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tw.zipe.bastpartner.dto.workflow.config.HttpRequestNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/**
 * HTTP 請求節點：url/body/headers/secretHeaders 皆支援插值；支援 GET / POST(JSON)。
 * timeoutMs 有值時作為該次呼叫的 call timeout。
 * 注意：secretHeaders 的值不得寫入任何 log 或錯誤訊息（本類別一律不輸出 header 值）。
 */
@ApplicationScoped
class HttpRequestExecutor : NodeExecutor {
    override val type = NodeType.HTTP_REQUEST

    data class ResolvedRequest(
        val method: String,
        val url: String,
        val body: String?,
        val headers: Map<String, String> = emptyMap()
    )

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as HttpRequestNodeConfig
        val req = buildRequest(cfg, context)
        val responseText = call(req, cfg.timeoutMs)
        return mapOf((cfg.outputKey ?: "response") to responseText)
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        /** 共用 base client；per-node timeout 以 newBuilder 衍生（共享連線池與執行緒） */
        private val baseClient = OkHttpClient()

        fun buildRequest(cfg: HttpRequestNodeConfig, context: ExecutionContext): ResolvedRequest {
            val method = cfg.method!!.uppercase()
            val url = context.resolveTemplate(cfg.url!!)
            val body = when (val b = cfg.body) {
                null -> null
                is JsonPrimitive -> context.resolveTemplate(b.content)
                else -> context.resolveTemplate(b.toString())
            }
            // headers 與 secretHeaders 逐值插值後合併套用（同名時 secretHeaders 優先）
            val headers = (cfg.headers.orEmpty() + cfg.secretHeaders.orEmpty())
                .mapValues { (_, v) -> context.resolveTemplate(v) }
            return ResolvedRequest(method, url, body, headers)
        }

        /** 執行請求並回傳 body 字串；非 2xx 或逾時拋 IOException（訊息不含 header 值） */
        fun call(req: ResolvedRequest, timeoutMs: Long?): String {
            val client = timeoutMs
                ?.let { baseClient.newBuilder().callTimeout(it, TimeUnit.MILLISECONDS).build() }
                ?: baseClient
            val builder = Request.Builder().url(req.url)
            req.headers.forEach { (name, value) -> builder.header(name, value) }
            when (req.method) {
                "GET" -> builder.get()
                "POST" -> builder.post((req.body ?: "{}").toRequestBody(JSON))
                else -> throw IllegalArgumentException("Unsupported HTTP method: ${req.method}")
            }
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code ${response.code}")
                }
                return response.body?.string().orEmpty()
            }
        }
    }
}
