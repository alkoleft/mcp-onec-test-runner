package io.github.alkoleft.mcp.interfaces.cli.commands

import io.github.alkoleft.mcp.McpYaxUnitRunnerApplication
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.stereotype.Component
import picocli.CommandLine.Command
import java.util.concurrent.Callable

private val logger = KotlinLogging.logger { }

/**
 * MCP server command - launches the Spring Boot application as MCP server.
 */
@Component
@Command(
    name = "mcp",
    description = ["Запуск MCP сервера для интеграции с AI агентами"],
    mixinStandardHelpOptions = true,
)
class McpCommand : Callable<Int> {
    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    override fun call(): Int {
        return try {
            logger.info { "Starting MCP YaXUnit Runner server..." }

            logger.info { "Using centralized ApplicationProperties for configuration" }
            logger.info { "  Base Path: ${applicationProperties.basePath}" }
            logger.info { "  Tests Path: ${applicationProperties.testsPath ?: applicationProperties.basePath.resolve("tests")}" }
            logger.info { "  IB Connection: ${applicationProperties.connection.connectionString.take(20)}..." }
            logger.info { "  Platform Version: ${applicationProperties.platformVersion.ifBlank { "auto-detect" }}" }

            // Launch Spring Boot application
            val app = SpringApplication(McpYaxUnitRunnerApplication::class.java)

            // Logging configuration is expected to be provided via standard Spring mechanisms

            logger.info { "MCP server starting on stdio transport..." }
            val context = app.run()

            logger.info { "MCP server started successfully. Waiting for MCP protocol messages..." }

            // Keep the application running
            val shutdownHook =
                Thread {
                    logger.info { "Shutting down MCP server..." }
                    context.close()
                }
            Runtime.getRuntime().addShutdownHook(shutdownHook)

            // Wait for shutdown
            context.registerShutdownHook()

            0
        } catch (e: Exception) {
            logger.error(e) { "Failed to start MCP server" }
            1
        }
    }
}
