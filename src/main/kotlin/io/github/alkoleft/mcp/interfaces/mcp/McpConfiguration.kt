package io.github.alkoleft.mcp.interfaces.mcp

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpConfiguration {
    @Bean
    fun platformTools(platformMcp: YaXUnitMcpServer): ToolCallbackProvider =
        MethodToolCallbackProvider
            .builder()
            .toolObjects(platformMcp)
            .build()
}