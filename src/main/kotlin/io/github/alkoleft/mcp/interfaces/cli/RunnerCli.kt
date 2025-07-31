package io.github.alkoleft.mcp.interfaces.cli

import io.github.alkoleft.mcp.interfaces.cli.commands.McpCommand
import io.github.alkoleft.mcp.interfaces.cli.commands.TestCommand
import io.github.alkoleft.mcp.interfaces.cli.config.CliConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.nio.file.Paths

private val logger = KotlinLogging.logger { }

/**
 * Main CLI entry point for MCP YaXUnit Runner.
 * Provides commands for running MCP server and executing tests.
 */
@Component
@Command(
    name = "mcp-yaxunit-runner",
    description = ["MCP Server для работы с модульными тестами решений на платформе 1С:Предприятие"],
    subcommands = [McpCommand::class, TestCommand::class],
    version = ["1.0-SNAPSHOT"],
    mixinStandardHelpOptions = true,
)
class RunnerCli {
    @Option(
        names = ["--project"],
        description = ["Путь к проекту 1С:Предприятие (обязательно)"],
        required = true,
    )
    lateinit var projectPath: String

    @Option(
        names = ["--tests"],
        description = ["Путь к директории тестов (по умолчанию: ./tests)"],
        defaultValue = "./tests",
    )
    lateinit var testsPath: String

    @Option(
        names = ["--ib-conn"],
        description = ["Строка подключения к информационной базе (Srvr=...;Ref=...; или /F...)"],
        required = true,
    )
    lateinit var ibConnection: String

    @Option(
        names = ["--ib-user"],
        description = ["Пользователь ИБ (переопределяет env IB_USER)"],
    )
    var ibUser: String? = null

    @Option(
        names = ["--ib-pwd"],
        description = ["Пароль ИБ (переопределяет env IB_PWD)"],
    )
    var ibPassword: String? = null

    @Option(
        names = ["--platform-ver"],
        description = ["Версия платформы 1С (8.3.24.1482 ...)"],
    )
    var platformVersion: String? = null

    @Option(
        names = ["--log-file"],
        description = ["Путь к файлу логов (опционально)"],
    )
    var logFile: String? = null

    @Option(
        names = ["--config"],
        description = ["Путь к файлу конфигурации YAML/JSON"],
    )
    var configFile: String? = null

    /**
     * Creates CLI configuration from command line arguments
     */
    fun createConfiguration(): CliConfiguration =
        CliConfiguration(
            projectPath = Paths.get(projectPath),
            testsPath = Paths.get(testsPath),
            ibConnection = ibConnection,
            ibUser = ibUser,
            ibPassword = ibPassword,
            platformVersion = platformVersion,
            logFile = logFile?.let { Paths.get(it) },
            configFile = configFile?.let { Paths.get(it) },
        )

    companion object {
        /**
         * Parse and execute CLI commands
         */
        fun parseAndExecute(args: Array<String>): Int {
            val cli = RunnerCli()
            val commandLine = CommandLine(cli)

            // Configure command line options
            commandLine.isCaseInsensitiveEnumValuesAllowed = true
            commandLine.isAbbreviatedSubcommandsAllowed = true
            commandLine.isAbbreviatedOptionsAllowed = true

            return try {
                commandLine.execute(*args)
            } catch (e: Exception) {
                logger.error("CLI execution failed", e)
                1
            }
        }
    }
}
