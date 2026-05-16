package com.example

import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

@Component
class ChineseName {

    @Tool(description = "Get the Chinese name of a user.")
    fun getChineseName(@ToolParam(description = "Enter username") username: String): String {
        return when (username) {
            "Gary" -> "蓋瑞"
            "Bob" -> "鮑勃"
            "Charlie" -> "查理"
            else -> "Unknown"
        }
    }
}
