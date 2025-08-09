package io.github.alkoleft.mcp.configuration

import io.github.alkoleft.mcp.application.actions.exceptions.ErrorHandler
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Конфигурация приложения с оптимизированным созданием бинов
 */
@Configuration
@EnableConfigurationProperties(ApplicationProperties::class)
class ApplicationConfiguration {
    /**
     * Создает ErrorHandler для централизованной обработки ошибок
     */
    @Bean
    fun errorHandler(): ErrorHandler = ErrorHandler
}
