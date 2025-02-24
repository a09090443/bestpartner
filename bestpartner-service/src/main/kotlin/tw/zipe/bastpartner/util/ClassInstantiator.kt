package tw.zipe.bastpartner.util

import io.quarkus.runtime.configuration.DurationConverter.parseDuration
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * 透過完整類路徑實例化一個類
 *
 * @param className 完整的類路徑
 * @param constructorArgs 建構函數參數
 * @return 建立的實例，如果失敗則傳回null
 */
@Suppress("UNCHECKED_CAST")
fun instantiate(className: String, constructorArgs: Map<String, Any?> = emptyMap()): Any? {
    return try {
        val clazz = Class.forName(className)
        val kClass = clazz.kotlin
        val constructor = findConstructor(kClass, constructorArgs) ?: return null
        val args = prepareConstructorArgs(constructor, constructorArgs) ?: return null
        constructor.callBy(args)
    } catch (e: ClassNotFoundException) {
        logger().error("找不到指定的類別: $className")
        null
    } catch (e: Exception) {
        logger().error("實例化過程中發生錯誤: ${e.message}", e)
        null
    }
}

private fun findConstructor(kClass: KClass<*>, constructorArgs: Map<String, Any?>): KFunction<Any>? {
    return if (constructorArgs.isEmpty()) {
        kClass.constructors.find { it.parameters.isEmpty() } ?: kClass.primaryConstructor
    } else {
        kClass.constructors.find { constructor ->
            // 檢查參數數量是否符合
            if (constructor.parameters.size != constructorArgs.size) {
                return@find false
            }

            // 檢查是否所有必要的參數都存在
            val hasAllRequiredArgs = List(constructor.parameters.size) { index ->
                constructorArgs.containsKey("arg$index")
            }.all { it }

            hasAllRequiredArgs
        } ?: kClass.primaryConstructor
    }
}

private fun prepareConstructorArgs(constructor: KFunction<Any>, constructorArgs: Map<String, Any?>): Map<KParameter, Any?>? {
    return constructor.parameters.mapIndexed { index, param ->
        val argKey = "arg$index"
        val value = constructorArgs[argKey]
        val convertedValue = convertValue(param, value)
        param to convertedValue
    }.toMap()
}

private fun convertValue(param: KParameter, value: Any?): Any? {
    return when {
        value == null -> null
        param.type.classifier == Int::class -> value.toString().trim('"').toIntOrNull()
        param.type.classifier == Long::class -> value.toString().trim('"').toLongOrNull()
        param.type.classifier == Double::class -> value.toString().trim('"').toDoubleOrNull()
        param.type.classifier == Float::class -> value.toString().trim('"').toFloatOrNull()
        param.type.classifier == Boolean::class -> when (value.toString().trim('"').lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
        param.type.classifier == String::class -> value.toString().trim('"')
        param.type.classifier == Duration::class -> parseDuration(value.toString().trim('"'))
        param.type.classifier is KClass<*> && (param.type.classifier as KClass<*>).java.isEnum -> {
            try {
                java.lang.Enum.valueOf(
                    (param.type.classifier as KClass<*>).java as Class<out Enum<*>>,
                    value.toString().trim('"')
                )
            } catch (e: Exception) {
                null
            }
        }
        param.type.classifier is KClass<*> && (param.type.classifier as KClass<*>).java.isInstance(value) -> value
        else -> {
            logger().warn("無法轉換參數 ${param.name} 的值 $value 到類型 ${param.type}")
            null
        }
    }
}

/**
 * 解析 Duration 字串
 * 支援的格式：
 * - 純數字：視為毫秒
 * - 帶單位：
 *   - ms: 毫秒
 *   - s: 秒
 *   - m: 分鐘
 *   - h: 小時
 *   - d: 天
 * 例如："1000"、"1000ms"、"1s"、"1m"、"1h"、"1d"
 */
private fun parseDuration(value: String): Duration? {
    try {
        // 如果是純數字，視為毫秒
        if (value.matches(Regex("^\\d+$"))) {
            return Duration.ofMillis(value.toLong())
        }

        // 解析帶單位的格式
        val pattern = Regex("^(\\d+)(ms|s|m|h|d)$")
        val matchResult = pattern.find(value) ?: return null
        val (amount, unit) = matchResult.destructured

        return when (unit) {
            "ms" -> Duration.ofMillis(amount.toLong())
            "s" -> Duration.ofSeconds(amount.toLong())
            "m" -> Duration.ofMinutes(amount.toLong())
            "h" -> Duration.ofHours(amount.toLong())
            "d" -> Duration.ofDays(amount.toLong())
            else -> null
        }
    } catch (e: Exception) {
        logger().error("解析 Duration 失敗: $value")
        return null
    }
}

/**
 * 列印類別的建構函數訊息
 */
fun printConstructorInfo(className: String) {
    try {
        val clazz = Class.forName(className)
        val kClass = clazz.kotlin
        val constructors = kClass.constructors

        logger().info("類 '$className' 的構造函數:")
        constructors.forEachIndexed { constructorIndex, constructor ->
            logger().info("  構造函數 $constructorIndex:")
            constructor.parameters.forEachIndexed { paramIndex, param ->
                logger().info("    參數 $paramIndex: ${param.name ?: "未命名"} (類型: ${param.type})")
            }
        }

    } catch (e: ClassNotFoundException) {
        logger().error("找不到指定的類別: $className")
    } catch (e: Exception) {
        logger().error("取得構造函數資訊時發生錯誤: ${e.message}")
    }
}

/**
 * 根據指定欄位順序重排並重新命名 Map 的鍵
 *
 * @param inputMap 原始參數的鍵值對
 * @param fields 指定的欄位順序，以逗號分隔的字串
 * @return 重新排序並重新命名後的鍵值對
 */
fun reorderAndRenameArguments(inputMap: Map<String, Any>, fields: String): Map<String, Any?> {
    // 以逗號分割欄位字串並移除多餘空格
    val fieldOrder = fields.split(",").map { it.trim() }

    // 根據字段順序重新排列並產生新的鍵值對
    return fieldOrder.mapIndexed { index, field ->
        "arg$index" to inputMap[field]
    }.toMap()
}

// 函數：將資料類別的欄位與型態轉成 JSON 格式
fun generateFieldJson(clazz: KClass<*>): String {
    val fields = clazz.memberProperties.map { property ->
        val fieldType = when (property.returnType.toString()) {
            "kotlin.String?" -> "String"
            "kotlin.String" -> "String"
            "kotlin.Long" -> "Long"
            "kotlin.Boolean" -> "Boolean"
            "kotlin.collections.List<kotlin.String>?" -> "List<String>"
            else -> property.returnType.toString() // 回傳原始型態名稱
        }
        "\"${property.name}\": \"$fieldType\""
    }
    return "{ ${fields.joinToString(", ")} }"
}
