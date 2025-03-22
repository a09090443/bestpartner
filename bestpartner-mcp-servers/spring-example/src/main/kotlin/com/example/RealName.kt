package com.example

import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

@Component
class RealName {

    @Tool(description = "Get the real name of a user.")
    fun getRealName(@ToolParam(description = "Enter username") username: String): String {
        return when (username) {
            "Gary" -> "蓋瑞"
            "Bob" -> "鮑勃"
            "Charlie" -> "查理"
            else -> "Unknown"
        }
    }
}
