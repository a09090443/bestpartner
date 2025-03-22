package com.example

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpConfiguration {

    @Bean
    fun bitcoinTools(realName: RealName): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder().toolObjects(realName).build()
    }
}
