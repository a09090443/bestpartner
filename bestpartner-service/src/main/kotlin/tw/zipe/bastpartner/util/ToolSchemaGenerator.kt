package tw.zipe.bastpartner.util

import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import tw.zipe.bastpartner.tool.config.ToolConfigField

/**
 * 由工具 configObjectPath 指向的 data class 反射產生結構化 settingSchema，
 * 供前端動態渲染設定表單。事實來源為 config class 本身，schema 不落地不手填。
 *
 * 輸出格式（每欄位）：{ "type": "...", "required": bool, "sensitive"?: true, "description"?: "..." }
 * 欄位順序 = primary constructor 參數順序。
 */
object ToolSchemaGenerator {

    /** class 不存在或無 primary constructor 時回傳 null（前端 fallback raw JSON editor） */
    fun generate(configObjectPath: String): JsonObject? {
        val kClass = runCatching { Class.forName(configObjectPath).kotlin }.getOrNull() ?: return null
        val ctor = kClass.primaryConstructor ?: return null
        return buildJsonObject {
            ctor.parameters.forEach { param ->
                val name = param.name ?: return@forEach
                val annotation = param.findAnnotation<ToolConfigField>()
                putJsonObject(name) {
                    put("type", jsonType(param.type))
                    put("required", !param.type.isMarkedNullable)
                    if (annotation?.sensitive == true) put("sensitive", true)
                    annotation?.description?.takeIf { it.isNotBlank() }?.let { put("description", it) }
                }
            }
        }
    }

    private fun jsonType(type: KType): String = when (type.classifier) {
        Int::class, Long::class, Short::class -> "integer"
        Double::class, Float::class -> "number"
        Boolean::class -> "boolean"
        List::class, Set::class -> "array"
        else -> "string"
    }
}
