package io.github.alkoleft.mcp.interfaces.websocket

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

/**
 * WebSocket configuration for YAXUnit test execution endpoints.
 * Configures reactive WebSocket support using Spring WebFlux.
 */
@Configuration
class WebSocketConfiguration {
    @Bean
    fun handlerMapping(yaXUnitWebSocketHandler: YaXUnitWebSocketHandler): HandlerMapping {
        val mapping = SimpleUrlHandlerMapping()
        mapping.urlMap =
            mapOf(
                "/yaxunit" to yaXUnitWebSocketHandler,
            )
        mapping.order = 1
        return mapping
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter = WebSocketHandlerAdapter()
}
