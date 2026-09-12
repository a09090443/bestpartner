package tw.zipe.bastpartner.service.workflow.executor

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.PolyglotException
import tw.zipe.bastpartner.dto.workflow.config.CodeNodeConfig
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor

/**
 * 程式碼節點：以 GraalJS sandbox 執行使用者 JS 腳本（spec §2.5）。
 * 沙箱不開 host access / IO / 執行緒；`input` 全域變數為上游所有輸出（JSON 序列化後於
 * JS 內 JSON.parse 還原，避免 host object 穿透）；取腳本最後表達式為回傳值，
 * 經 JS JSON.stringify 轉字串後由 Jackson 反序列化為 Map/List/scalar。
 * 逾時（預設 10 秒）以獨立執行緒 + context.close(true) 強制中斷；輸出上限 256KB。
 *
 * @author Gary
 * @created 2026/7/10
 */
@ApplicationScoped
class CodeExecutor : NodeExecutor {
    override val type = NodeType.CODE

    companion object {
        const val DEFAULT_OUTPUT_KEY = "result"
        const val DEFAULT_TIMEOUT_MS = 10_000L

        /** 輸出上限：序列化後字串長度 256KB */
        const val MAX_OUTPUT_KB = 256
        const val MAX_OUTPUT_LENGTH = MAX_OUTPUT_KB * 1024

        private const val LANGUAGE_JS = "js"
    }

    private val objectMapper = ObjectMapper()

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as CodeNodeConfig
        if (!cfg.language.equals(LANGUAGE_JS, ignoreCase = true)) {
            throw ServiceException(AppMessage.WORKFLOW_CODE_LANGUAGE_NOT_SUPPORTED, cfg.language ?: "null")
        }
        val timeoutMs = cfg.timeoutMs ?: DEFAULT_TIMEOUT_MS
        val inputJson = objectMapper.writeValueAsString(context.allOutputs())
        val script = "const input = JSON.parse(${toJsStringLiteral(inputJson)});\n${cfg.source}"
        val resultJson = evalWithTimeout(script, timeoutMs)
        return mapOf((cfg.outputKey ?: DEFAULT_OUTPUT_KEY) to deserialize(resultJson))
    }

    /** 於獨立執行緒 eval，逾時以 close(true) 強制中斷；回傳 JSON.stringify 後的結果字串（undefined → null） */
    private fun evalWithTimeout(script: String, timeoutMs: Long): String? {
        val sandbox = Context.newBuilder(LANGUAGE_JS)
            .allowAllAccess(false)
            .build()
        val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "workflow-code-node").apply { isDaemon = true } }
        try {
            val future = executor.submit<String?> {
                val result = sandbox.eval(LANGUAGE_JS, script)
                val json = sandbox.eval(LANGUAGE_JS, "JSON.stringify").execute(result)
                if (json.isNull || !json.isString) null else json.asString()
            }
            return try {
                future.get(timeoutMs, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                sandbox.close(true)
                future.cancel(true)
                throw ServiceException(AppMessage.WORKFLOW_CODE_TIMEOUT, timeoutMs)
            } catch (e: ExecutionException) {
                throw when (val cause = e.cause) {
                    is PolyglotException -> ServiceException(AppMessage.WORKFLOW_CODE_SCRIPT_ERROR, cause.message ?: cause.toString())
                    is ServiceException -> cause
                    else -> ServiceException(AppMessage.WORKFLOW_CODE_SCRIPT_ERROR, cause?.message ?: e.toString())
                }
            }
        } finally {
            executor.shutdownNow()
            runCatching { sandbox.close(true) }
        }
    }

    /** 反序列化 JS 輸出：先檢核大小上限，再交 Jackson 還原為 Map/List/scalar */
    private fun deserialize(resultJson: String?): Any? {
        if (resultJson == null) return null
        if (resultJson.length > MAX_OUTPUT_LENGTH) {
            throw ServiceException(AppMessage.WORKFLOW_CODE_OUTPUT_TOO_LARGE, MAX_OUTPUT_KB)
        }
        return objectMapper.readValue(resultJson, Any::class.java)
    }

    /** 將任意字串轉為安全的 JS 單引號字面值（escape 反斜線、引號、換行與行分隔符） */
    private fun toJsStringLiteral(text: String): String {
        val sb = StringBuilder(text.length + 16).append('\'')
        text.forEach { c ->
            when (c) {
                '\\' -> sb.append("\\\\")
                '\'' -> sb.append("\\'")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\u2028' -> sb.append("\\u2028")
                '\u2029' -> sb.append("\\u2029")
                else -> sb.append(c)
            }
        }
        return sb.append('\'').toString()
    }
}
