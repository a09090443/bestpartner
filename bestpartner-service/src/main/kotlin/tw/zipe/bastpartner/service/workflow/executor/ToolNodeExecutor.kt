package tw.zipe.bastpartner.service.workflow.executor

import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.agent.tool.Tool
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.service.tool.ToolExecutor
import jakarta.enterprise.context.ApplicationScoped
import java.lang.reflect.Method
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import tw.zipe.bastpartner.dto.workflow.config.NodeConfig
import tw.zipe.bastpartner.dto.workflow.config.ToolNodeConfig
import tw.zipe.bastpartner.entity.WorkflowNodeEntity
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.enumerate.NodeType
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.ToolService
import tw.zipe.bastpartner.service.workflow.ExecutionContext
import tw.zipe.bastpartner.service.workflow.NodeExecutor
import tw.zipe.bastpartner.util.logger

/**
 * 工具節點：重用 ToolService 的工具實例化路徑（與 customAssistantChat 掛載工具相同機制），
 * arguments 值插值後依工具形態呼叫：
 * - `Map<ToolSpecification, ToolExecutor>`（CUSTOMIZE ToolExecutor，如 DateTool）：以 ToolExecutionRequest 傳 JSON 參數
 * - 其他實例（如 WebSearchTool）：反射呼叫其 @Tool 方法（多個未指名 → 依方法名排序取第一個並 log warn），
 *   參數依方法簽名做基本型別轉換（String/Int/Long/Double/Boolean）
 * 輸出 `{outputKey(預設 "result") to 回傳值}`，非 scalar 以 Jackson 轉 Map/List。
 */
@ApplicationScoped
class ToolNodeExecutor(private val toolService: ToolService) : NodeExecutor {
    override val type = NodeType.TOOL

    override fun execute(node: WorkflowNodeEntity, config: NodeConfig, context: ExecutionContext): Map<String, Any?> {
        val cfg = config as ToolNodeConfig
        // 有 toolSettingId 走使用者設定實例化，否則以 toolId 實例化（不存在由 ToolService 拋 TOOL_NOT_FOUND）
        val instance = if (!cfg.toolSettingId.isNullOrBlank()) {
            toolService.buildToolWithSetting(cfg.toolSettingId)
        } else {
            toolService.buildTool(cfg.toolId!!)
        }
        return invoke(instance, cfg, context)
    }

    companion object {
        private val logger = logger()
        private val objectMapper = ObjectMapper()

        /** 以已實例化的工具執行節點呼叫（抽出供單元測試直接驗證，不需 CDI 與資料庫） */
        fun invoke(instance: Any?, cfg: ToolNodeConfig, context: ExecutionContext): Map<String, Any?> {
            instance ?: throw ServiceException(AppMessage.TOOL_NOT_FOUND)
            val args = resolveArguments(cfg.arguments, context)
            val result = when (instance) {
                is Map<*, *> -> invokeToolExecutor(instance, args, context)
                else -> invokeAnnotatedMethod(instance, args)
            }
            return mapOf((cfg.outputKey ?: "result") to normalize(result))
        }

        /** arguments 每個值先經 resolveTemplate 插值；非字串 primitive 保留原生型別 */
        private fun resolveArguments(arguments: JsonObject?, context: ExecutionContext): Map<String, Any?> =
            arguments?.mapValues { (_, v) -> resolveJsonValue(v, context) } ?: emptyMap()

        private fun resolveJsonValue(value: JsonElement, context: ExecutionContext): Any? = when {
            value is JsonNull -> null
            value is JsonPrimitive && value.isString -> context.resolveTemplate(value.content)
            value is JsonPrimitive -> value.booleanOrNull ?: value.longOrNull ?: value.doubleOrNull ?: value.content
            // 複合值（物件/陣列）以 JSON 字串插值後原樣傳遞
            else -> context.resolveTemplate(value.toString())
        }

        /** CUSTOMIZE ToolExecutor 形態：spec 內含 functionName，參數以 JSON 傳入 */
        private fun invokeToolExecutor(instance: Map<*, *>, args: Map<String, Any?>, context: ExecutionContext): Any? {
            if (instance.size > 1) logger.warn("工具含 ${instance.size} 個 function，未指名，取第一個")
            val (spec, executor) = instance.entries.first()
            check(spec is ToolSpecification && executor is ToolExecutor) { "工具實例形態不符：${instance.javaClass.name}" }
            val request = ToolExecutionRequest.builder()
                .name(spec.name())
                .arguments(objectMapper.writeValueAsString(args))
                .build()
            return executor.execute(request, context.executionId)
        }

        /** 反射呼叫 @Tool 方法：唯一者用之；多個依方法名排序取第一個並 log warn */
        private fun invokeAnnotatedMethod(instance: Any, args: Map<String, Any?>): Any? {
            val methods = instance.javaClass.methods
                .filter { it.isAnnotationPresent(Tool::class.java) }
                .sortedBy { it.name }
            check(methods.isNotEmpty()) { "工具 ${instance.javaClass.name} 無可呼叫的 @Tool 方法" }
            if (methods.size > 1) {
                logger.warn("工具 ${instance.javaClass.name} 含 ${methods.size} 個 @Tool 方法，未指名，取 ${methods.first().name}")
            }
            val method = methods.first()
            val params = method.parameters.map { p -> convert(args[p.name], p.type) }
            return method.invoke(instance, *params.toTypedArray())
        }

        /** 依方法簽名做基本型別轉換（編譯已開 -parameters / javaParameters，參數名可取得） */
        private fun convert(value: Any?, type: Class<*>): Any? = when {
            value == null -> null
            type.isInstance(value) -> value
            type == String::class.java -> value.toString()
            type == Int::class.javaPrimitiveType || type == Integer::class.java -> value.toString().toInt()
            type == Long::class.javaPrimitiveType || type == java.lang.Long::class.java -> value.toString().toLong()
            type == Double::class.javaPrimitiveType || type == java.lang.Double::class.java -> value.toString().toDouble()
            type == Boolean::class.javaPrimitiveType || type == java.lang.Boolean::class.java -> value.toString().toBoolean()
            else -> value
        }

        /** scalar 原樣輸出；集合轉 List、其餘物件以 Jackson 轉 Map，確保輸出可序列化與插值取值 */
        private fun normalize(result: Any?): Any? = when (result) {
            null, is String, is Number, is Boolean -> result
            is Collection<*> -> objectMapper.convertValue(result, List::class.java)
            else -> objectMapper.convertValue(result, Map::class.java)
        }
    }
}
