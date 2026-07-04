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
        // 整個方法體納入同一個 try/catch：本方法接進 /llm/tool/list 端點，
        // kotlin-reflect 對 metadata 異常的 class 可能拋 KotlinReflectionInternalError 等例外，
        // 任何反射失敗一律 warn + 回 null，絕不炸掉整個清單查詢
        return try {
            // initialize = false：僅載入 metadata，避免觸發目標 class 的 static initializer
            val kClass = Class.forName(configObjectPath, false, javaClass.classLoader).kotlin
            val ctor = kClass.primaryConstructor ?: return null
            buildJsonObject {
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
        } catch (e: Throwable) {
            logger().warn("無法反射工具設定類別: $configObjectPath", e)
            null
        }
    }

    /** 未列舉的型別（含 Map、自訂 class）一律 fallback 為 "string"，由前端以文字輸入呈現 */
    private fun jsonType(type: KType): String = when (type.classifier) {
        Int::class, Long::class, Short::class -> "integer"
        Double::class, Float::class -> "number"
        Boolean::class -> "boolean"
        List::class, Set::class -> "array"
        else -> "string"
    }
}
