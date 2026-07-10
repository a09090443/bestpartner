package tw.zipe.bastpartner.service.workflow.executor

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.JsonPrimitive
import tw.zipe.bastpartner.dto.workflow.config.HttpRequestNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor
import tw.zipe.bastpartner.util.OkHttpUtil

/** HTTP 請求節點：url/body/headers 皆支援插值；Phase 1 支援 GET / POST(JSON) */
@ApplicationScoped
class HttpRequestExecutor : NodeExecutor {
    override val type = NodeType.HTTP_REQUEST

    data class ResolvedRequest(val method: String, val url: String, val body: String?)

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as HttpRequestNodeConfig
        val req = buildRequest(cfg, context)
        val responseText = when (req.method) {
            "GET" -> OkHttpUtil.getData(req.url)?.body?.string().orEmpty()
            "POST" -> OkHttpUtil.postJson(req.url, req.body ?: "{}")
            else -> throw IllegalArgumentException("Unsupported HTTP method: ${req.method}")
        }
        return mapOf((cfg.outputKey ?: "response") to responseText)
    }

    companion object {
        fun buildRequest(cfg: HttpRequestNodeConfig, context: ExecutionContext): ResolvedRequest {
            val method = cfg.method!!.uppercase()
            val url = context.resolveTemplate(cfg.url!!)
            val body = when (val b = cfg.body) {
                null -> null
                is JsonPrimitive -> context.resolveTemplate(b.content)
                else -> context.resolveTemplate(b.toString())
            }
            return ResolvedRequest(method, url, body)
        }
    }
}
