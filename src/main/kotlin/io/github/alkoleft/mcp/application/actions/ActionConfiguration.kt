package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.alkoleft.mcp.infrastructure.process.EnhancedReportParser
import io.github.alkoleft.mcp.infrastructure.process.JsonYaXUnitConfigWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring configuration for Actions
 */
@Configuration
class ActionConfiguration {

    @Bean
    fun jsonYaXUnitConfigWriter(): JsonYaXUnitConfigWriter = JsonYaXUnitConfigWriter()

    @Bean
    fun enhancedReportParser(): EnhancedReportParser = EnhancedReportParser()

    @Bean
    fun actionFactory(
        platformUtilityDsl: PlatformUtilityDsl,
        utilLocator: CrossPlatformUtilLocator,
        configWriter: JsonYaXUnitConfigWriter,
        reportParser: EnhancedReportParser
    ): ActionFactory = ActionFactoryImpl(platformUtilityDsl, utilLocator, configWriter, reportParser)
} 