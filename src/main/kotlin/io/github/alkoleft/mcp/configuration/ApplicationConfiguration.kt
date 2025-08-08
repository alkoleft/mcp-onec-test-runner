package io.github.alkoleft.mcp.configuration

import io.github.alkoleft.mcp.application.actions.exceptions.ErrorHandler
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.config.ProjectConfiguration
import io.github.alkoleft.mcp.infrastructure.config.ProjectConfigurationManager
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

    /**
     * Создает ProjectConfigurationManager для управления конфигурацией проекта
     */
    @Bean
    fun projectConfigurationManager(): ProjectConfigurationManager = ProjectConfigurationManager()

    /**
     * Создает ProjectConfiguration с настройками по умолчанию
     */
    @Bean
    fun projectConfiguration(projectConfigurationManager: ProjectConfigurationManager): ProjectConfiguration =
        projectConfigurationManager.loadConfiguration()
} 