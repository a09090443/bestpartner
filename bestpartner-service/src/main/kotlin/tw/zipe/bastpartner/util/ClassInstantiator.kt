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
fun instantiate(className: String, constructorArgs: Map<String, Any?> = emptyMap()): Any? {
    val clazz: Class<*> = try {
        Class.forName(className)
    } catch (e: ClassNotFoundException) {
        logger().error("找不到指定的類別: $className")
        return null
    }
    val kClass = clazz.kotlin
    return instantiate(kClass, constructorArgs)
}

/**
 * 透過 KClass 實例化一個類
 *
 * @param clazz KClass 實例
 * @param constructorArgs 建構函數參數
 * @return 建立的實例，如果失敗則傳回null
 */
fun instantiate(clazz: KClass<*>, constructorArgs: Map<String, Any?> = emptyMap()): Any? {
    return try {
        val constructor = findConstructor(clazz, constructorArgs) ?: return null
        val args = prepareConstructorArgs(constructor, constructorArgs) ?: return null
        constructor.callBy(args)
    } catch (e: Exception) {
        logger().error("實例化過程中發生錯誤: ${e.message}", e)
        null
    }
}

private fun findConstructor(kClass: KClass<*>, constructorArgs: Map<String, Any?>): KFunction<Any>? {
    // 如果無參數，尋找無參數構造函數或主構造函數
    if (constructorArgs.isEmpty()) {
        return kClass.constructors.find { it.parameters.isEmpty() } ?: kClass.primaryConstructor
    }

    // 尋找符合參數數量的構造函數
    return kClass.constructors.find { constructor ->
        // 檢查參數數量
        if (constructor.parameters.size != constructorArgs.size) {
            return@find false
        }

        // 檢查所有必要參數都存在
        constructor.parameters.indices.all { index ->
            constructorArgs.containsKey("arg$index")
        }
    } ?: kClass.primaryConstructor
}

private fun prepareConstructorArgs(constructor: KFunction<Any>, constructorArgs: Map<String, Any?>): Map<KParameter, Any?> {
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
        param.type.classifier is KClass<*> && isEnum(param.type.classifier as KClass<*>) -> {
            try {
                val enumClass = (param.type.classifier as KClass<*>).java
                // 使用安全的方式取得 enum 值
                if (Enum::class.java.isAssignableFrom(enumClass)) {
                    @Suppress("UNCHECKED_CAST")
                    val enumType = enumClass as Class<out Enum<*>>
                    java.lang.Enum.valueOf(enumType, value.toString().trim('"'))
                } else {
                    null
                }
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

private fun isEnum(kClass: KClass<*>): Boolean {
    return kClass.java.isEnum
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
    if (value.isBlank()) {
        return null
    }

    try {
        // 如果是純數字，視為毫秒
        if (value.matches(Regex("^\\d+$"))) {
            return Duration.ofMillis(value.toLong())
        }

        // 解析帶單位的格式
        val pattern = Regex("^(\\d+)(ms|s|m|h|d)$")
        val matchResult = pattern.find(value) ?: return null
        val (amount, unit) = matchResult.destructured

        // 安全轉換數字
        val amountValue = try {
            amount.toLong()
        } catch (e: NumberFormatException) {
            logger().warn("無效的持續時間數值: $amount")
            return null
        }

        return when (unit) {
            "ms" -> Duration.ofMillis(amountValue)
            "s" -> Duration.ofSeconds(amountValue)
            "m" -> Duration.ofMinutes(amountValue)
            "h" -> Duration.ofHours(amountValue)
            "d" -> Duration.ofDays(amountValue)
            else -> null
        }
    } catch (e: Exception) {
        logger().error("解析 Duration 失敗: $value", e)
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
    if (fields.isBlank()) {
        return emptyMap()
    }

    // 以逗號分割欄位字串並移除多餘空格
    val fieldOrder = fields.split(",").map { it.trim() }.filter { it.isNotBlank() }

    // 根據字段順序重新排列並產生新的鍵值對
    return fieldOrder.mapIndexed { index, field ->
        "arg$index" to inputMap[field]
    }.toMap()
}

// 函數：將資料類別的欄位與型態轉成 JSON 格式
fun generateFieldJson(clazz: KClass<*>): String {
    if (clazz.memberProperties.isEmpty()) {
        return "{}"
    }

    val fields = clazz.memberProperties.map { property ->
        val fieldType = when (property.returnType.toString()) {
            "kotlin.String?", "kotlin.String" -> "String"
            "kotlin.Long", "kotlin.Long?" -> "Long"
            "kotlin.Boolean", "kotlin.Boolean?" -> "Boolean"
            "kotlin.collections.List<kotlin.String>?" -> "List<String>"
            else -> property.returnType.toString() // 回傳原始型態名稱
        }
        "\"${property.name}\": \"$fieldType\""
    }
    return "{ ${fields.joinToString(", ")} }"
}
