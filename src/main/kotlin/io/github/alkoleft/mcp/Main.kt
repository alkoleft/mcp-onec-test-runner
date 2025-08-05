package io.github.alkoleft.mcp

import io.github.alkoleft.mcp.interfaces.cli.RunnerCli
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import kotlin.system.exitProcess

@EnableConfigurationProperties
@SpringBootApplication
class McpYaxUnitRunnerApplication

fun main(args: Array<String>) {
    // Check if we should run in CLI mode or Spring Boot mode
    if (args.isNotEmpty() && !isSpringBootArgs(args)) {
        // CLI mode - parse commands with PicoCLI
        val exitCode = RunnerCli.parseAndExecute(args)
        exitProcess(exitCode)
    } else {
        // Spring Boot mode - run as MCP server
        runApplication<McpYaxUnitRunnerApplication>(*args)
    }
}

/**
 * Determines if arguments are Spring Boot specific (profile, properties, etc.)
 * rather than CLI commands
 */
private fun isSpringBootArgs(args: Array<String>): Boolean =
    args.any { arg ->
        arg.startsWith("--spring.") ||
            arg.startsWith("--server.") ||
            arg.startsWith("--logging.") ||
            arg.startsWith("--management.") ||
            arg == "--debug" ||
            arg == "--trace"
    }
