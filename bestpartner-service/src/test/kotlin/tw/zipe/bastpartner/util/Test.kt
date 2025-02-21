package tw.zipe.bastpartner.util

import com.fasterxml.jackson.databind.ObjectMapper

fun main() {
    val json = """
        {
          "param1": ["hello", "String"],
          "param2": [22, "Integer"],
          "param3": ["1234", "String"],
          "param4": [true, "Boolean"]
        }
    """.trimIndent()

    val objectMapper = ObjectMapper()
    val map: Map<String, List<Any>> = objectMapper.readValue(json, object : com.fasterxml.jackson.core.type.TypeReference<Map<String, List<Any>>>() {})

    map.forEach { (key, value) ->
        if (value.size == 2) {
            val actualValue = value[0]
            val declaredType = value[1].toString()

            // 透過 declaredType 檢查值的真實型態
            val detectedType = when (actualValue) {
                is String -> "String"
                is Int -> "Integer"
                is Long -> "Long"
                is Double -> "Double"
                is Boolean -> "Boolean"
                is List<*> -> "Array"
                is Map<*, *> -> "Object"
                else -> "Unknown"
            }

            // 輸出對比結果
            val status = if (declaredType == detectedType) "✅" else "❌ ($detectedType)"
            println("$key -> Value: $actualValue, Declared Type: $declaredType, Detected Type: $detectedType $status")
        } else {
            println("$key -> ❌ Invalid format (should be [value, type])")
        }
    }
}
