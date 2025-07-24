package io.github.alkoleft.mcp.infrastructure.config

import io.github.alkoleft.mcp.application.services.BuildOrchestrationService
import io.github.alkoleft.mcp.application.services.TestLauncherService
import io.github.alkoleft.mcp.core.modules.BuildService
import io.github.alkoleft.mcp.core.modules.BuildStateManager
import io.github.alkoleft.mcp.core.modules.HashStorage
import io.github.alkoleft.mcp.core.modules.ReportParser
import io.github.alkoleft.mcp.core.modules.UtilLocator
import io.github.alkoleft.mcp.core.modules.YaXUnitConfigWriter
import io.github.alkoleft.mcp.core.modules.YaXUnitRunner
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.process.EnhancedReportParser
import io.github.alkoleft.mcp.infrastructure.process.JsonYaXUnitConfigWriter
import io.github.alkoleft.mcp.infrastructure.process.ProcessYaXUnitRunner
import io.github.alkoleft.mcp.infrastructure.storage.FileBuildStateManager
import io.github.alkoleft.mcp.infrastructure.storage.MapDbHashStorage
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.nio.file.Paths

/**
 * Spring configuration for MCP YaXUnit Runner application components.
 * Provides beans for dependency injection and proper service wiring.
 */
@Configuration
class ApplicationConfiguration {
    @Bean
    @Primary
    fun projectConfigurationManager(): ProjectConfigurationManager = ProjectConfigurationManager()

    @Bean
    @ConditionalOnMissingBean
    fun projectConfiguration(configManager: ProjectConfigurationManager): ProjectConfiguration {
        // Load configuration from system properties if set via CLI
        val configFilePath = System.getProperty("app.config.file")?.let { Paths.get(it) }
        return configManager.loadConfiguration(configFilePath)
    }

    @Bean
    @ConditionalOnMissingBean
    fun utilLocator(): UtilLocator = CrossPlatformUtilLocator()

    @Bean
    @ConditionalOnMissingBean
    fun hashStorage(config: ProjectConfiguration): HashStorage = MapDbHashStorage()

    @Bean
    @ConditionalOnMissingBean
    fun buildStateManager(hashStorage: HashStorage): BuildStateManager = FileBuildStateManager(hashStorage as MapDbHashStorage)

    @Bean
    @ConditionalOnMissingBean
    fun reportParser(): ReportParser = EnhancedReportParser()

    @Bean
    @ConditionalOnMissingBean
    fun yaXUnitRunner(utilLocator: UtilLocator): YaXUnitRunner = ProcessYaXUnitRunner()

    @Bean
    @ConditionalOnMissingBean
    fun yaXUnitConfigWriter(): YaXUnitConfigWriter = JsonYaXUnitConfigWriter()

    @Bean
    @ConditionalOnMissingBean
    fun buildService(
        buildStateManager: BuildStateManager,
        utilLocator: UtilLocator,
        config: ProjectConfiguration,
    ): BuildService =
        BuildOrchestrationService(
            buildStateManager = buildStateManager,
            utilLocator = utilLocator,
        )

    @Bean
    @ConditionalOnMissingBean
    fun testLauncher(
        buildService: BuildService,
        yaXUnitRunner: YaXUnitRunner,
        yaXUnitConfigWriter: YaXUnitConfigWriter,
        reportParser: ReportParser,
        utilLocator: UtilLocator,
        projectConfiguration: ProjectConfiguration,
    ): TestLauncherService =
        TestLauncherService(
            buildService = buildService,
            yaXUnitRunner = yaXUnitRunner,
            yaXUnitConfigWriter = yaXUnitConfigWriter,
            reportParser = reportParser,
            utilLocator = utilLocator,
            projectConfiguration = projectConfiguration,
        )
}
