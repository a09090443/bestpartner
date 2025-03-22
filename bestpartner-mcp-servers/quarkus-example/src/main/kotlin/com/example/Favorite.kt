package com.example

import io.quarkiverse.mcp.server.Tool
import io.quarkiverse.mcp.server.ToolArg

class Favorite {

    @Tool(description = "Get the favorite color of a user.")
    fun favoriteColor(@ToolArg(description = "Enter username") username: String) = when (username) {
        "Gary" -> "Blue"
        "Bob" -> "Green"
        "Charlie" -> "Red"
        else -> "Unknown"
    }
}
