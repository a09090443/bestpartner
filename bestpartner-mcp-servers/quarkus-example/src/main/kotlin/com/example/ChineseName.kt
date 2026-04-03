package com.example

import io.quarkiverse.mcp.server.Tool
import io.quarkiverse.mcp.server.ToolArg

/**
 * @author Gary
 * @created 2025/5/1
 */
class ChineseName {
    @Tool(description = "Get the Chinese name of a user.")
    fun chineseName(@ToolArg(description = "Enter username") username: String) = when (username) {
        "Gary" -> "蓋瑞"
        "Bob" -> "鮑勃"
        "Charlie" -> "查理"
        else -> "Unknown"
    }
}
