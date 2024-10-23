package tw.zipe.bastpartner.util

import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

/**
 * 透過完整類路徑實例化一個類
 *
 * @param className 完整的類路徑
 * @param constructorArgs 建構函數參數
 * @return 建立的實例，如果失敗則傳回null
 */
@Suppress("UNCHECKED_CAST")
fun instantiate(className: String, vararg constructorArgs: Any?): Any? {
    try {
        // 取得Class對象
        val clazz = Class.forName(className)
        val kClass = clazz.kotlin

        // 取得主構造函數
        val constructor = kClass.primaryConstructor ?: return null

        // 檢查參數數量是否匹配
        if (constructor.parameters.size != constructorArgs.size) {
            logger().error("參數數量不匹配")
            return null
        }

        //建立參數映射
        val args = constructor.parameters.zip(constructorArgs.toList()).toMap()

        // 建立參數映射
        return constructor.callBy(args)
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
        val constructor = kClass.primaryConstructor ?: throw Exception("找不到主構造函數")

        logger().info("類別 '$className' 的主建構子:")
        constructor.parameters.forEachIndexed { index, param ->
            logger().info("  參數 $index: ${param.name} (類型: ${param.type.javaType})")
        }

    } catch (e: ClassNotFoundException) {
        logger().error("找不到指定的類別: $className")
    } catch (e: Exception) {
        logger().error("取得構造函數資訊時發生錯誤: ${e.message}")
    }
}
