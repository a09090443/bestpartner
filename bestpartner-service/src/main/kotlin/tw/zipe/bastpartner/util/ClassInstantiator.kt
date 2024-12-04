package tw.zipe.bastpartner.util

import java.util.Locale
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
                                constructorArgs.containsKey(paramName.replaceFirstChar { it.lowercase(Locale.getDefault()) }) // 處理駝峰命名
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
            // 建立參數映射
            val args = constructor.parameters.mapNotNull { param ->
                val paramName = param.name ?: return@mapNotNull null
                val value = constructorArgs[paramName]
                    ?: constructorArgs[paramName.replaceFirstChar { it.lowercase() }]
                    ?: return@mapNotNull null

                // 類型轉換和驗證
                param to value
            }.toMap()

            // 使用參數呼叫構造函數
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
