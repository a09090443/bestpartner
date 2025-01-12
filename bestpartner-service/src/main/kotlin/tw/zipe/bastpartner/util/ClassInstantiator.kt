package tw.zipe.bastpartner.util

import io.quarkus.runtime.configuration.DurationConverter.parseDuration
import java.time.Duration
import java.util.Locale
import kotlin.reflect.KClass
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
    try {
        // 取得Class對象
        val clazz = Class.forName(className)
        val kClass = clazz.kotlin

        // 找出適合的構造函數
        val constructor = if (constructorArgs.isEmpty()) {
            // 如果沒有參數，優先使用無參數構造函數
            kClass.constructors.find { it.parameters.isEmpty() }
                ?: kClass.primaryConstructor
        } else {
            kClass.constructors.find { constructor ->
                // 檢查參數數量和名稱是否匹配
                constructor.parameters.all { param ->
                    param.name?.let { paramName ->
                        constructorArgs.containsKey(paramName) ||
                                constructorArgs.containsKey(paramName.replaceFirstChar { it.lowercase(Locale.getDefault()) })
                    } ?: false
                }
            } ?: kClass.primaryConstructor
        }

        // 檢查是否找到構造函數
        if (constructor == null) {
            logger().error("找不到匹配的構造函數")
            return null
        }

        // 處理無參數情況
        return if (constructor.parameters.isEmpty()) {
            constructor.call()
        } else {
            // 建立參數映射，並進行型態轉換
            val args = constructor.parameters.mapNotNull { param ->
                val paramName = param.name ?: return@mapNotNull null
                val value = constructorArgs[paramName]
                    ?: constructorArgs[paramName.replaceFirstChar { it.lowercase() }]
                    ?: return@mapNotNull null

                // 進行型態轉換
                val convertedValue = when {
                    false -> null
                    param.type.isMarkedNullable && value == null -> null
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
                    // 處理列舉類型
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
                    // 如果是其他類型且值已經符合目標類型，直接使用
                    param.type.classifier is KClass<*> &&
                            (param.type.classifier as KClass<*>).java.isInstance(value) -> value

                    else -> {
                        logger().warn("無法轉換參數 ${param.name} 的值 $value 到類型 ${param.type}")
                        null
                    }
                }

                if (convertedValue == null && !param.type.isMarkedNullable) {
                    logger().error("參數 ${param.name} 不可為 null，但轉換後得到 null 值")
                    return null
                }

                param to convertedValue
            }.toMap()

            // 使用轉換後的參數呼叫構造函數
            constructor.callBy(args)
        }

    } catch (e: ClassNotFoundException) {
        logger().error("找不到指定的類別: $className")
    } catch (e: Exception) {
        logger().error("實例化過程中發生錯誤: ${e.message}")
    }
    return null
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
fun reorderAndRenameArguments(inputMap: Map<String, Any>, fields: String): Map<String, Any> {
    // 以逗號分割欄位字串並移除多餘空格
    val fieldOrder = fields.split(",").map { it.trim() }

    // 根據字段順序重新排列並產生新的鍵值對
    return fieldOrder.mapIndexedNotNull { index, field ->
        inputMap[field]?.let { "arg$index" to it } // 找到對應值並產生新鍵
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
