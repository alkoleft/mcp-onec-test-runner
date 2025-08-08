package io.github.alkoleft.mcp

import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import io.github.alkoleft.mcp.interfaces.cli.BootstrapCli
import io.github.alkoleft.mcp.interfaces.cli.RunnerCli
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import picocli.CommandLine
import picocli.CommandLine.IFactory


@EnableConfigurationProperties
@SpringBootApplication
class McpYaxUnitRunnerApplication

fun main(args: Array<String>) {
    if (PlatformDetector.isWindows) {
        System.setProperty("file.encoding", "UTF-8")
    }
    val profile = BootstrapCli.detectProfile(args)
    runApplication<McpYaxUnitRunnerApplication>(*args) {
        setAdditionalProfiles(profile)
    }
}

@Profile("cli")
@Component
class ApplicationRunner(private val runner: RunnerCli, private val factory: IFactory) : CommandLineRunner,
    ExitCodeGenerator {
    private var exitCode = 0

    override fun run(vararg args: String) {
        exitCode = CommandLine(runner, factory).execute(*args)
    }

    override fun getExitCode(): Int {
        return exitCode
    }
}

@Profile("mcp")
@Component
class McpServerRunner : CommandLineRunner {
    private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

    override fun run(vararg args: String) {
        logger.info { "Starting MCP server on STDIO transport..." }
        logger.info { "MCP server ready. Waiting for protocol messages on STDIN..." }
        
        // Keep the application alive by waiting for shutdown signal
        // The actual MCP protocol handling is done by Spring AI MCP framework
        try {
            // Use a shutdown hook to handle graceful shutdown
            Runtime.getRuntime().addShutdownHook(Thread {
                logger.info { "Received shutdown signal, stopping MCP server..." }
            })
            
            // Keep the main thread alive by waiting indefinitely
            // The MCP framework will handle STDIN/STDOUT communication
            val lock = Object()
            synchronized(lock) {
                lock.wait()
            }
        } catch (e: InterruptedException) {
            logger.info { "MCP server interrupted, shutting down..." }
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            logger.error(e) { "Error in MCP server runner" }
        }
    }
}

@Primary
@Profile("cli")
@Component
class SpringPicocliFactory(private val applicationContext: ApplicationContext) : IFactory {
    override fun <K> create(cls: Class<K?>): K? {
        return applicationContext.getBean<K?>(cls)
    }
}