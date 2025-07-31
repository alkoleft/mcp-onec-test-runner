package io.github.alkoleft.mcp.interfaces.cli.commands

import io.github.alkoleft.mcp.McpYaxUnitRunnerApplication
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.SpringApplication
import picocli.CommandLine.Command
import picocli.CommandLine.ParentCommand
import java.util.concurrent.Callable

private val logger = KotlinLogging.logger { }

/**
 * MCP server command - launches the Spring Boot application as MCP server.
 */
@Command(
    name = "mcp",
    description = ["Запуск MCP сервера для интеграции с AI агентами"],
    mixinStandardHelpOptions = true,
)
class McpCommand : Callable<Int> {
    @ParentCommand
    private lateinit var parent: io.github.alkoleft.mcp.interfaces.cli.RunnerCli

    override fun call(): Int {
        return try {
            logger.info("Starting MCP YaXUnit Runner server...")

            val config = parent.createConfiguration()
            val errors = config.validate()

            if (errors.isNotEmpty()) {
                logger.error("Configuration validation failed:")
                errors.forEach { logger.error("  - $it") }
                return 1
            }

            logger.info("Configuration:\n${config.summary()}")

            // Set system properties for Spring Boot application
            System.setProperty("app.project.path", config.projectPath.toString())
            System.setProperty("app.tests.path", config.testsPath.toString())
            System.setProperty("app.ib.connection", config.ibConnection)
            config.resolveIbUser()?.let { System.setProperty("app.ib.user", it) }
            config.resolveIbPassword()?.let { System.setProperty("app.ib.password", it) }
            config.platformVersion?.let { System.setProperty("app.platform.version", it) }
            config.configFile?.let { System.setProperty("app.config.file", it.toString()) }

            // Launch Spring Boot application
            val app = SpringApplication(McpYaxUnitRunnerApplication::class.java)

            // Configure logging
            config.logFile?.let { logFile ->
                System.setProperty("logging.file.name", logFile.toString())
            }

            logger.info("MCP server starting on stdio transport...")
            val context = app.run()

            logger.info("MCP server started successfully. Waiting for MCP protocol messages...")

            // Keep the application running
            val shutdownHook =
                Thread {
                    logger.info("Shutting down MCP server...")
                    context.close()
                }
            Runtime.getRuntime().addShutdownHook(shutdownHook)

            // Wait for shutdown
            context.registerShutdownHook()

            0
        } catch (e: Exception) {
            logger.error("Failed to start MCP server", e)
            1
        }
    }
}
