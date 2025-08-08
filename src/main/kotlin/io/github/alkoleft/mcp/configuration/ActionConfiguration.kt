package io.github.alkoleft.mcp.configuration

import io.github.alkoleft.mcp.application.actions.ActionFactory
import io.github.alkoleft.mcp.application.actions.ActionFactoryImpl
import io.github.alkoleft.mcp.application.actions.BuildAction
import io.github.alkoleft.mcp.application.actions.build.DesignerBuildAction
import io.github.alkoleft.mcp.application.actions.build.IbcmdBuildAction
import io.github.alkoleft.mcp.infrastructure.platform.locator.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.process.EnhancedReportParser
import io.github.alkoleft.mcp.infrastructure.process.JsonYaXUnitConfigWriter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
        platformDsl: PlatformDsl,
        utilLocator: CrossPlatformUtilLocator,
        configWriter: JsonYaXUnitConfigWriter,
        reportParser: EnhancedReportParser
    ): ActionFactory = ActionFactoryImpl(platformDsl, utilLocator, configWriter, reportParser)

    /**
     * Создает DesignerBuildAction при использовании DESIGNER в качестве сборщика
     */
    @Bean
    @ConditionalOnProperty(name = ["app.tools.builder"], havingValue = "DESIGNER")
    fun designerBuildAction(dsl: PlatformDsl): BuildAction =
        DesignerBuildAction(dsl)

    /**
     * Создает IbcmdBuildAction при использовании IBMCMD в качестве сборщика
     */
    @Bean
    @ConditionalOnProperty(name = ["app.tools.builder"], havingValue = "IBMCMD")
    fun ibcmdBuildAction(dsl: PlatformDsl): BuildAction =
        IbcmdBuildAction(dsl)
}