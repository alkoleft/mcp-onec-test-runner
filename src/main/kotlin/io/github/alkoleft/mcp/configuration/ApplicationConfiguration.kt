package io.github.alkoleft.mcp.configuration

import io.github.alkoleft.mcp.application.actions.BuildAction
import io.github.alkoleft.mcp.application.actions.build.DesignerBuildAction
import io.github.alkoleft.mcp.application.actions.build.IbcmdBuildAction
import io.github.alkoleft.mcp.application.actions.exceptions.ErrorHandler
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Конфигурация приложения с оптимизированным созданием бинов
 */
@Configuration
@EnableConfigurationProperties
class ApplicationConfiguration {

    /**
     * Создает DesignerBuildAction при использовании DESIGNER в качестве сборщика
     */
    @Bean
    @ConditionalOnProperty(name = ["app.tools.builder"], havingValue = "DESIGNER")
    fun designerBuildAction(dsl: PlatformUtilityDsl): BuildAction =
        DesignerBuildAction(dsl)

    /**
     * Создает IbcmdBuildAction при использовании IBMCMD в качестве сборщика
     */
    @Bean
    @ConditionalOnProperty(name = ["app.tools.builder"], havingValue = "IBMCMD")
    fun ibcmdBuildAction(dsl: PlatformUtilityDsl): BuildAction =
        IbcmdBuildAction(dsl)

    /**
     * Создает ErrorHandler для централизованной обработки ошибок
     */
    @Bean
    fun errorHandler(): ErrorHandler = ErrorHandler
} 