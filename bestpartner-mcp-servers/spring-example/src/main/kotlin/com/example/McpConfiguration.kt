package com.example

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpConfiguration {

    @Bean
    fun bitcoinTools(chineseName: ChineseName): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder().toolObjects(chineseName).build()
    }
}
